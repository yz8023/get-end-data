package com.expiryguard.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.CompositionLocalProvider
import com.expiryguard.app.ui.liquid.motion.Motion
import com.expiryguard.app.ui.liquid.screens.LocalAppSettings
import com.expiryguard.app.ui.liquid.screens.rememberAppSettingsState
import com.expiryguard.app.ui.liquid.theme.AppChrome
import com.expiryguard.app.ui.liquid.theme.LiquidUITheme
import com.expiryguard.app.ui.liquid.theme.ThemeMode

/**
 * ExpiryGuard 主题。
 *
 * 在 Liquid Motion UI（液态玻璃设计系统）之上做一层业务桥接：
 * - 业务侧只关心「深色模式」开关；
 * - 玻璃透明度、折射率、圆角缩放、弹簧阻尼、主题配色等外观旋钮由
 *   [com.expiryguard.app.ui.liquid.screens.AppSettingsState] 持久化驱动。
 *
 * 主题模式由 AppSettingsState 的 System/Light/Dark 单一状态驱动。
 */
@Composable
fun ExpiryGuardTheme(content: @Composable () -> Unit) {
    val settings = rememberAppSettingsState()

    val visualMode = settings.visualMode

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

    CompositionLocalProvider(LocalAppSettings provides settings) {
        LiquidUITheme(
            themeMode = settings.themeMode,
            visualMode = visualMode,
            palette = settings.palette,
            customSeed = Color(settings.customSeed),
            customShade = settings.customShade,
            chrome = chrome,
            content = content
        )
    }
}
