package com.expiryguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expiryguard.app.ui.liquid.components.liquidGlass

/**
 * 页面背景容器。
 *
 * 在液态玻璃布局下，真正的背景（Aurora 极光层）由 [com.expiryguard.app.navigation.AppNavigation]
 * 的根节点统一绘制并被玻璃面板采样，因此这里保持透明，让极光层透出；
 * 在普通 / MD3 模式下，根节点会绘制纯色背景，同样透出。
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
    }
}

/**
 * 液态玻璃卡片（真实 backdrop 采样 + 折射）。
 *
 * 保留原有业务调用签名，内部替换为 [liquidGlass]：
 * - [tint] 决定玻璃底色调，[statusColor] 用于表达状态色；
 * - [elevation] 映射玻璃软投影；
 * - 选中态提高边框高光并混入主色。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    elevation: Dp = 4.dp,
    statusColor: Color? = null,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val tint = when {
        selected && statusColor != null -> statusColor.copy(alpha = 0.35f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        statusColor != null -> statusColor.copy(alpha = 0.16f)
        else -> null
    }

    val glass = modifier
        .liquidGlass(
            shape = shape,
            tint = tint,
            elevation = elevation,
            borderAlpha = if (selected) 0.62f else 0.42f
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )

    Box(modifier = glass) {
        content()
    }
}

/**
 * 小型玻璃卡片（用于网格布局）
 */
@Composable
fun GlassCardSmall(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    statusColor: Color? = null,
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        statusColor = statusColor,
        content = content
    )
}

/**
 * 内发光玻璃卡片（用于清单详情页的主图区域）
 */
@Composable
fun GlassCardGlow(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    glowColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(
            shape = shape,
            tint = glowColor.copy(alpha = 0.18f),
            elevation = 8.dp,
            borderAlpha = 0.55f
        )
    ) {
        content()
    }
}

/**
 * 顶部装饰渐变条
 */
@Composable
fun GradientTopBar(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    )
}
