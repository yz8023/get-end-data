package com.expiryguard.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.domain.engine.ExpiryPolicy
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.util.DateUtils

/** 根节点注入的统一策略和本地日期，避免卡片各自使用不同的隐式默认值。 */
val LocalExpiryPolicy = staticCompositionLocalOf { ExpiryPolicy() }
val LocalExpiryToday = staticCompositionLocalOf { DateUtils.todayTimestamp() }

@Composable
fun rememberExpiryStatus(product: ProductEntity): ProductStatus {
    val policy = LocalExpiryPolicy.current
    val today = LocalExpiryToday.current
    return remember(product.id, product.shelfLifeDays, product.expiryDate, policy, today) {
        ExpiryRuleEngine.calculateStatus(product.shelfLifeDays, product.expiryDate, policy, today)
    }
}
