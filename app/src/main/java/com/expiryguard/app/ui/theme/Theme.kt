package com.expiryguard.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.expiryguard.app.ui.liquid.motion.Motion
import com.expiryguard.app.ui.liquid.screens.rememberAppSettingsState
import com.expiryguard.app.ui.liquid.theme.AppChrome
import com.expiryguard.app.ui.liquid.theme.LiquidUITheme
import com.expiryguard.app.ui.liquid.theme.ThemeMode
import com.expiryguard.app.ui.liquid.theme.VisualMode

/**
 * ExpiryGuard 主题。
 *
 * 在 Liquid Motion UI（液态玻璃设计系统）之上做一层业务桥接：
 * - 业务侧继续只关心「深色 / 莫奈取色」两个开关；
 * - 玻璃透明度、折射率、圆角缩放、弹簧阻尼、主题配色等外观旋钮由
 *   [com.expiryguard.app.ui.liquid.screens.AppSettingsState] 持久化驱动。
 *
 * @param darkTheme 是否使用深色主题，默认跟随系统
 * @param monetEnabled 是否启用 Material You 动态取色（映射到 MD3 视觉模式）
 */
@Composable
fun ExpiryGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    monetEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val settings = rememberAppSettingsState()

    // 动态取色优先级最高；否则使用用户选择的三态视觉风格。
    val visualMode = if (monetEnabled) VisualMode.Md3 else settings.visualMode

    val chrome = AppChrome(
        visualMode = visualMode,
        glassOpacity = settings.glassOpacity,
        glassRefraction = settings.glassRefraction,
        cornerScale = settings.cornerScale,
        reducedMotion = settings.reducedMotion,
        motionDamping = settings.motionDamping,
        wallpaperBackground = settings.wallpaperBackground
    )

    // 全局弹簧阻尼旋钮：一个值重调全应用动效手感。
    SideEffect {
        Motion.motionDamping.floatValue = settings.motionDamping
    }

    LiquidUITheme(
        themeMode = if (darkTheme) ThemeMode.Dark else ThemeMode.Light,
        visualMode = visualMode,
        palette = settings.palette,
        customSeed = Color(settings.customSeed),
        customShade = settings.customShade,
        chrome = chrome,
        content = content
    )
}
