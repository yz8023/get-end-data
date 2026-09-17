package com.expiryguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expiryguard.app.ui.theme.GlassBorderDark
import com.expiryguard.app.ui.theme.GlassBorderLight
import com.expiryguard.app.ui.theme.GlassCardDark
import com.expiryguard.app.ui.theme.GlassCardLight
import com.expiryguard.app.ui.theme.GlassGlowLight
import com.expiryguard.app.ui.theme.GlassShadowDark
import com.expiryguard.app.ui.theme.GlassShadowDeepDark
import com.expiryguard.app.ui.theme.GlassShadowDeepLight
import com.expiryguard.app.ui.theme.GlassShadowLight
import com.expiryguard.app.ui.theme.GradientEnd
import com.expiryguard.app.ui.theme.GradientEndDark
import com.expiryguard.app.ui.theme.GradientMid
import com.expiryguard.app.ui.theme.GradientMidDark
import com.expiryguard.app.ui.theme.GradientStart
import com.expiryguard.app.ui.theme.GradientStartDark

/**
 * 检测当前是否为深色主题
 * 使用 MaterialTheme 的 background 颜色判断，而非 isSystemInDarkTheme()，
 * 这样才能正确响应应用内自定义的夜间模式切换。
 */
@Composable
private fun isDarkTheme(): Boolean {
    val bg = MaterialTheme.colorScheme.background
    val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return luminance < 0.5f
}

/**
 * 增强渐变背景容器
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dark = isDarkTheme()
    val colors = remember(dark) {
        if (dark) {
            listOf(GradientStartDark, GradientMidDark, GradientEndDark)
        } else {
            listOf(GradientStart, GradientMid, GradientEnd)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = colors)
            )
    ) {
        content()
    }
}

/**
 * 增强版毛玻璃卡片
 *
 * 性能优化：合并 background 层，减少 Clip 次数，使用 remember 缓存主题色值。
 *
 * @param modifier 修饰符
 * @param selected 是否选中
 * @param onClick 点击回调，为 null 时不可点击
 * @param shape 圆角形状，默认 16.dp
 * @param elevation 阴影高度，默认 4.dp
 * @param statusColor 状态色（可选），为卡片添加彩色边框光晕
 * @param content 卡片内容
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
    val dark = isDarkTheme()
    val glassCardColor = if (dark) GlassCardDark else GlassCardLight
    val glassBorderColor = if (dark) GlassBorderDark else GlassBorderLight
    val shadowAmbientColor = if (dark) GlassShadowDark else GlassShadowLight
    val shadowSpotColor = if (dark) GlassShadowDeepDark else GlassShadowDeepLight

    val borderColor = when {
        statusColor != null && selected -> statusColor.copy(alpha = 0.5f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> glassBorderColor
    }

    val cardModifier = modifier
        .clip(shape)
        .background(
            color = if (selected) glassCardColor.copy(alpha = 0.95f) else glassCardColor,
            shape = shape
        )
        .border(
            width = 1.dp,
            color = borderColor,
            shape = shape
        )
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = shadowAmbientColor,
            spotColor = shadowSpotColor
        )

    Box(
        modifier = if (onClick != null) {
            cardModifier.clickable(onClick = onClick)
        } else {
            cardModifier
        }
    ) {
        content()
    }
}

/**
 * 小型毛玻璃卡片（用于网格布局）
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
        statusColor = statusColor
    ) {
        content()
    }
}

/**
 * 内发光玻璃卡片（用于清单详情页的主图区域）
 */
@Composable
fun GlassCardGlow(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    glowColor: Color = GlassGlowLight,
    content: @Composable () -> Unit
) {
    val dark = isDarkTheme()
    val glassCardColor = if (dark) GlassCardDark else GlassCardLight
    val glassBorderColor = if (dark) GlassBorderDark else GlassBorderLight
    val shadowAmbientColor = if (dark) GlassShadowDark else GlassShadowLight
    val shadowSpotColor = if (dark) GlassShadowDeepDark else GlassShadowDeepLight

    Box(
        modifier = modifier
            .shadow(8.dp, shape, ambientColor = shadowAmbientColor, spotColor = shadowSpotColor)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(glowColor, Color.Transparent)
                ),
                shape = shape
            )
            .background(glassCardColor, shape)
            .border(1.dp, glassBorderColor, shape)
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
    val dark = isDarkTheme()
    val colors = if (dark) {
        listOf(GradientStartDark, GradientMidDark, GradientEndDark)
    } else {
        listOf(GradientStart, GradientMid, GradientEnd)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(colors = colors)
            )
    )
}