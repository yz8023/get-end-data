package com.expiryguard.app.domain.model

/**
 * 产品状态密封类，表示产品当前的安全/过期状态
 */
sealed class ProductStatus {

    /**
     * 安全状态：剩余天数充足
     *
     * @param remainingDays 距离到期剩余天数
     */
    data class Safe(val remainingDays: Int) : ProductStatus()

    /**
     * 即将到期状态：已进入预警范围但尚未达到退货阈值
     *
     * @param remainingDays 距离到期剩余天数
     */
    data class ExpiringSoon(val remainingDays: Int) : ProductStatus()

    /**
     * 可退货状态：在退货期限内，建议尽快处理
     *
     * @param remainingDays 距离到期剩余天数
     * @param threshold 退货阈值天数
     */
    data class Returnable(val remainingDays: Int, val threshold: Int) : ProductStatus()

    /**
     * 可下架状态：到期前两天内，需将商品从货架取下
     *
     * @param remainingDays 距离到期剩余天数
     */
    data class TakeDown(val remainingDays: Int) : ProductStatus()

    /**
     * 已过期状态
     *
     * @param daysOverdue 超期天数
     */
    data class Expired(val daysOverdue: Int) : ProductStatus()
}