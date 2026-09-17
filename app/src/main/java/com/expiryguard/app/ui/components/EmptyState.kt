package com.expiryguard.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.expiryguard.app.ui.liquid.components.PrimaryButton

/**
 * 空状态组件，当列表无数据时显示大图标、提示文字和操作按钮。
 *
 * 内部委托液态玻璃 [com.expiryguard.app.ui.liquid.components.EmptyState]，
 * 保持既有业务调用签名不变。
 *
 * @param message 提示文字
 * @param icon 图标
 * @param actionText 操作按钮文字（为 null 时不显示按钮）
 * @param onAction 操作按钮点击回调（为 null 时不显示按钮）
 * @param modifier 修饰符（可选）
 */
@Composable
fun EmptyState(
    message: String,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    com.expiryguard.app.ui.liquid.components.EmptyState(
        title = message,
        message = "",
        icon = icon,
        modifier = modifier,
        action = if (actionText != null && onAction != null) {
            {
                PrimaryButton(onClick = onAction) {
                    androidx.compose.material3.Text(actionText)
                }
            }
        } else null
    )
}
