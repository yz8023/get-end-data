package com.expiryguard.app.domain.engine

/** 一次规则计算所需的不可变配置快照，禁止依赖进程级可变阈值。 */
data class ExpiryPolicy(
    val takeDownDays: Int = 2,
    val reminderDays: Int = 7,
    val groupRules: List<ShelfLifeRule> = DEFAULT_GROUP_RULES
) {
    fun normalized(): ExpiryPolicy = copy(
        takeDownDays = takeDownDays.coerceIn(0, 365),
        reminderDays = reminderDays.coerceIn(1, 365),
        groupRules = groupRules.filter { it.minDays >= 0 && it.maxDays > it.minDays }
            .sortedWith(compareBy<ShelfLifeRule> { it.minDays }.thenBy { it.maxDays })
            .ifEmpty { DEFAULT_GROUP_RULES }
    )

    fun returnThresholdFor(shelfLifeDays: Int): Int {
        val days = shelfLifeDays.coerceAtLeast(0)
        return groupRules.firstOrNull { days >= it.minDays && days < it.maxDays }
            ?.reminderThreshold
            ?.coerceAtLeast(0)
            ?: legacyReturnThreshold(days)
    }

    companion object {
        val DEFAULT_GROUP_RULES = listOf(
            ShelfLifeRule(0, 1, 0),
            ShelfLifeRule(1, 90, 0),
            ShelfLifeRule(90, 183, 15),
            ShelfLifeRule(183, 365, 20),
            ShelfLifeRule(365, Int.MAX_VALUE, 45)
        )

        fun legacyReturnThreshold(shelfLifeDays: Int): Int = when {
            shelfLifeDays >= 365 -> 45
            shelfLifeDays > 183 -> 20
            shelfLifeDays >= 90 -> 15
            else -> 0
        }
    }
}

data class ShelfLifeRule(
    val minDays: Int,
    val maxDays: Int,
    val reminderThreshold: Int
)
