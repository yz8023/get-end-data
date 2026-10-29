package com.expiryguard.app.domain.engine

import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.util.DateUtils

/** 首页与通知共用的待办分组；不同分组可能覆盖同一产品，总数按数据库 ID 去重。 */
data class PendingGroups(
    val todayExpiry: List<ProductEntity>,
    val takeDown: List<ProductEntity>,
    val returnable: List<ProductEntity>,
    val expired: List<ProductEntity>,
    val warning: List<ProductEntity>
) {
    val totalCount: Int
        get() = (todayExpiry.map { it.id } + takeDown.map { it.id } +
            returnable.map { it.id } + expired.map { it.id } + warning.map { it.id }).toSet().size
}

/** 纯日期规则引擎；所有业务调用都应传入同一 [ExpiryPolicy] 快照和明确的今天。 */
object ExpiryRuleEngine {
    private const val EXPIRING_SOON_WINDOW_DAYS = 30

    fun calculateStatus(
        shelfLifeDays: Int,
        expiryDate: Long,
        policy: ExpiryPolicy,
        today: Long
    ): ProductStatus {
        val remainingDays = DateUtils.daysBetween(today, expiryDate)
        if (remainingDays < 0) return ProductStatus.Expired(daysOverdue = -remainingDays)

        if (remainingDays <= policy.takeDownDays) {
            return ProductStatus.TakeDown(remainingDays)
        }
        val returnThreshold = policy.returnThresholdFor(shelfLifeDays)
        if (returnThreshold > 0 && remainingDays <= returnThreshold) {
            return ProductStatus.Returnable(remainingDays, returnThreshold)
        }
        if (returnThreshold == 0 && remainingDays <= EXPIRING_SOON_WINDOW_DAYS) {
            return ProductStatus.ExpiringSoon(remainingDays)
        }
        return ProductStatus.Safe(remainingDays)
    }

    fun calculateStatus(shelfLifeDays: Int, expiryDate: Long): ProductStatus {
        return calculateStatus(shelfLifeDays, expiryDate, null, null)
    }

    /** 兼容旧 UI 调用；新代码应使用 policy + today 版本。 */
    fun calculateStatus(
        shelfLifeDays: Int,
        expiryDate: Long,
        takeDownThreshold: Int?
    ): ProductStatus = calculateStatus(shelfLifeDays, expiryDate, null, takeDownThreshold)

    fun calculateStatus(
        shelfLifeDays: Int,
        expiryDate: Long,
        returnThreshold: Int?,
        takeDownThreshold: Int?
    ): ProductStatus = calculateStatus(
        shelfLifeDays,
        expiryDate,
        returnThreshold,
        takeDownThreshold,
        DateUtils.todayTimestamp()
    )

    fun calculateStatus(
        shelfLifeDays: Int,
        expiryDate: Long,
        returnThreshold: Int?,
        takeDownThreshold: Int?,
        today: Long
    ): ProductStatus {
        val policy = ExpiryPolicy(takeDownDays = (takeDownThreshold ?: 2).coerceAtLeast(0))
        if (returnThreshold == null) return calculateStatus(shelfLifeDays, expiryDate, policy, today)
        val remainingDays = DateUtils.daysBetween(today, expiryDate)
        if (remainingDays < 0) return ProductStatus.Expired(-remainingDays)
        if (remainingDays <= policy.takeDownDays) return ProductStatus.TakeDown(remainingDays)
        val threshold = returnThreshold.coerceAtLeast(0)
        return when {
            threshold > 0 && remainingDays <= threshold -> ProductStatus.Returnable(remainingDays, threshold)
            threshold == 0 && remainingDays <= EXPIRING_SOON_WINDOW_DAYS -> ProductStatus.ExpiringSoon(remainingDays)
            else -> ProductStatus.Safe(remainingDays)
        }
    }

    fun getReturnThreshold(shelfLifeDays: Int): Int = ExpiryPolicy.legacyReturnThreshold(shelfLifeDays)

    fun computePendingGroups(
        products: List<ProductEntity>,
        today: Long,
        policy: ExpiryPolicy
    ): PendingGroups = computePendingGroupsInternal(products, today, policy.reminderDays) { product ->
        calculateStatus(product.shelfLifeDays, product.expiryDate, policy, today)
    }

    /** 兼容旧调用；新代码使用 policy 版本。 */
    fun computePendingGroups(
        products: List<ProductEntity>,
        today: Long,
        takeDownThreshold: Int? = null,
        returnThreshold: Int? = null,
        reminderDays: Int = 1
    ): PendingGroups = computePendingGroupsInternal(products, today, reminderDays.coerceAtLeast(1)) { product ->
        calculateStatus(
            product.shelfLifeDays,
            product.expiryDate,
            returnThreshold,
            takeDownThreshold,
            today
        )
    }

    private inline fun computePendingGroupsInternal(
        products: List<ProductEntity>,
        today: Long,
        reminderDays: Int,
        statusOf: (ProductEntity) -> ProductStatus
    ): PendingGroups {
        val todayExpiry = mutableListOf<ProductEntity>()
        val takeDown = mutableListOf<ProductEntity>()
        val returnable = mutableListOf<ProductEntity>()
        val expired = mutableListOf<ProductEntity>()
        val warning = mutableListOf<ProductEntity>()
        products.forEach { product ->
            if (product.isCompleted) return@forEach
            val days = DateUtils.daysBetween(today, product.expiryDate)
            var alreadyPending = false
            if (days == 0) {
                todayExpiry += product
                alreadyPending = true
            }
            when (statusOf(product)) {
                is ProductStatus.Expired -> {
                    expired += product
                    alreadyPending = true
                }
                is ProductStatus.TakeDown -> if (days != 0) {
                    takeDown += product
                    alreadyPending = true
                }
                is ProductStatus.Returnable -> {
                    returnable += product
                    alreadyPending = true
                }
                else -> Unit
            }
            if (days in 1..reminderDays && !alreadyPending) warning += product
        }
        return PendingGroups(todayExpiry, takeDown, returnable, expired, warning)
    }
}
