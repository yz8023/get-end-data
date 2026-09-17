package com.expiryguard.app.domain.engine

import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.util.DateUtils

/**
 * 待办清单分组结果，供首页与启动通知统一使用同一口径
 *
 * @property todayExpiry 今日到期清单
 * @property returnable 可退货清单
 * @property expired 已过期清单
 * @property warning 预警清单（还剩1天，且不在上述三组中）
 */
data class PendingGroups(
    val todayExpiry: List<ProductEntity>,
    val takeDown: List<ProductEntity>,
    val returnable: List<ProductEntity>,
    val expired: List<ProductEntity>,
    val warning: List<ProductEntity>
) {
    /** 今日待办总数量（今日到期 + 可下架 + 可退货 + 已过期 + 预警，已去重） */
    val totalCount: Int
        get() = (todayExpiry.map { it.id } + takeDown.map { it.id } +
                returnable.map { it.id } +
                expired.map { it.id } + warning.map { it.id }).toSet().size
}

/**
 * 到期规则引擎，根据产品保质期和到期日期计算状态
 *
 * 规则很简单：分组是分类，阈值决定到期前多少天显示"可退货"
 */
object ExpiryRuleEngine {

    /**
     * 取件天数配置（距到期前多少天进入「可下架」状态），默认 2 天
     * SettingsViewModel 写入，所有计算调用方无需改动自动读取
     */
    @Volatile
    var takeDownThreshold: Int = 2
        private set

    /**
     * 更新取件天数，由 SettingsViewModel 调用
     */
    fun updateTakeDownThreshold(days: Int) {
        takeDownThreshold = days.coerceAtLeast(1)
    }

    /**
     * 「即将到期」预警窗口天数：短保质期产品（退货阈值=0）在到期前这段天数内
     * 标记为即将到期，填补「安全」到「可下架」之间的渐进预警
     */
    private const val EXPIRING_SOON_WINDOW_DAYS = 30

    /**
     * 计算产品当前状态（使用硬编码默认阈值）
     *
     * @param shelfLifeDays 保质期天数（用于计算默认阈值）
     * @param expiryDate 到期日期时间戳（毫秒）
     * @return 对应的 ProductStatus
     */
    fun calculateStatus(shelfLifeDays: Int, expiryDate: Long): ProductStatus {
        return calculateStatus(shelfLifeDays, expiryDate, null, null)
    }

    /**
     * 计算产品当前状态（支持自定义退货阈值与取件阈值）
     *
     * @param shelfLifeDays 保质期天数（用于计算默认阈值）
     * @param expiryDate 到期日期时间戳（毫秒）
     * @param returnThreshold 自定义退货阈值天数，为 null 时使用硬编码默认值
     * @param takeDownThreshold 自定义取件阈值天数（距到期前多少天进入「可下架」状态），为 null 时使用硬编码默认值 2
     * @return 对应的 ProductStatus
     */
    fun calculateStatus(
        shelfLifeDays: Int,
        expiryDate: Long,
        returnThreshold: Int?,
        takeDownThreshold: Int?
    ): ProductStatus {
        val now = DateUtils.todayTimestamp()
        val remainingDays = DateUtils.daysBetween(now, expiryDate)
        // 已过期
        if (remainingDays <= 0) {
            return ProductStatus.Expired(daysOverdue = -remainingDays)
        }
        // 可下架状态：距到期不超过取件阈值天，需取下架
        val effectiveTakeDownThreshold = takeDownThreshold ?: this@ExpiryRuleEngine.takeDownThreshold
        if (remainingDays <= effectiveTakeDownThreshold) {
            return ProductStatus.TakeDown(remainingDays = remainingDays)
        }

        val effectiveThreshold = returnThreshold ?: getReturnThreshold(shelfLifeDays)

        // 可退货状态：剩余天数 <= 退货阈值
        // 例如：阈值15天，则到期前15天内为"可退货"
        if (effectiveThreshold > 0 && remainingDays <= effectiveThreshold) {
            return ProductStatus.Returnable(
                remainingDays = remainingDays,
                threshold = effectiveThreshold
            )
        }

        // 即将到期：短保质期产品（退货阈值=0，如保质期<3个月）无退货窗口，
        // 用该状态填补「安全」到「可下架」之间的渐进预警（剩余 3~30 天）
        if (effectiveThreshold == 0 && remainingDays <= EXPIRING_SOON_WINDOW_DAYS) {
            return ProductStatus.ExpiringSoon(remainingDays = remainingDays)
        }

        // 安全状态
        return ProductStatus.Safe(remainingDays = remainingDays)
    }

    /**
     * 计算产品当前状态（使用硬编码默认阈值）
     *
     * @param shelfLifeDays 保质期天数（用于计算默认阈值）
     * @param expiryDate 到期日期时间戳（毫秒）
     * @param takeDownThreshold 自定义取件阈值天数，为 null 时使用硬编码默认值 2
     * @return 对应的 ProductStatus
     */
    fun calculateStatus(
        shelfLifeDays: Int,
        expiryDate: Long,
        takeDownThreshold: Int?
    ): ProductStatus {
        return calculateStatus(shelfLifeDays, expiryDate, null, takeDownThreshold)
    }

    /**
     * 根据保质期天数获取默认退货阈值天数
     *
     * 规则1：
     * - 保质期 >1年（365天）：剩余0-45天可退货
     * - 保质期 6个月~1年（183~365天）：剩余0-20天可退货
     * - 保质期 3个月~6个月（90~183天）：剩余0-15天可退货
     * - 保质期 <3个月（<90天）：不可退货
     *
     * @param shelfLifeDays 保质期天数
     * @return 退货阈值天数
     */
    fun getReturnThreshold(shelfLifeDays: Int): Int {
        return when {
            shelfLifeDays >= 365 -> 45   // 1 年以上
            shelfLifeDays > 183 -> 20    // 6 个月 ~ 1 年（>183，因为183是3~6个月上限）
            shelfLifeDays >= 90 -> 15    // 3 个月 ~ 6 个月（90~183）
            else -> 0                    // 低于 3 个月，不可退货
        }
    }

    /**
     * 计算待办清单分组（今日到期 / 可下架 / 可退货 / 已过期 / 预警），
     * 首页今日待办与启动通知使用同一口径，保证数量一致。
     *
     * 口径：
     * - 今日到期：到期日期是今天
     * - 可下架：状态为 TakeDown（剩余 1~2 天，需取下架）
     * - 可退货：状态为 Returnable
     * - 已过期：到期日期已过
     * - 预警：还剩1天到期，且不在上述分组中
     *
     * @param products 全部活跃清单
     * @param today 今天零点时间戳
     */
    fun computePendingGroups(products: List<ProductEntity>, today: Long, takeDownThreshold: Int? = null): PendingGroups {
        // 今日到期：到期日期是今天
        val todayExpiry = products.filter {
            DateUtils.isToday(it.expiryDate) && !it.isCompleted
        }

        // 可下架清单：剩余天数 <= 取件阈值，需取下架
        val takeDown = products.filter { product ->
            val status = calculateStatus(product.shelfLifeDays, product.expiryDate, takeDownThreshold)
            status is ProductStatus.TakeDown && !product.isCompleted
        }

        // 可退货清单：根据规则1，到期前阈值天时放入待办
        val returnable = products.filter { product ->
            val status = calculateStatus(product.shelfLifeDays, product.expiryDate, takeDownThreshold)
            status is ProductStatus.Returnable && !product.isCompleted
        }

        // 已过期：到期日期已过，需要立即处理
        val expired = products.filter {
            val days = DateUtils.daysBetween(today, it.expiryDate)
            days < 0 && !it.isCompleted
        }

        // 今日到期（含可下架、可退货、已过期）—— 合并去重
        val todayWithReturnableIds = (todayExpiry.map { it.id } +
                takeDown.map { it.id } +
                returnable.map { it.id } +
                expired.map { it.id }).toSet()

        // 预警：还有1天到期（排除已在今日待办中的清单）
        val warning = products.filter {
            val days = DateUtils.daysBetween(today, it.expiryDate)
            days == 1 && !it.isCompleted && it.id !in todayWithReturnableIds
        }

        return PendingGroups(
            todayExpiry = todayExpiry,
            takeDown = takeDown,
            returnable = returnable,
            expired = expired,
            warning = warning
        )
    }
}