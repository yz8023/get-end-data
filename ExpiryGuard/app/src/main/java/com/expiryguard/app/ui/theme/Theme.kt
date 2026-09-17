package com.expiryguard.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 浅色主题配色方案
 */
private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue400.copy(alpha = 0.2f),
    onPrimaryContainer = Blue700,
    secondary = Purple500,
    onSecondary = Color.White,
    secondaryContainer = Purple400.copy(alpha = 0.15f),
    onSecondaryContainer = Purple500,
    tertiary = Green500,
    onTertiary = Color.White,
    tertiaryContainer = Green400.copy(alpha = 0.15f),
    background = Background,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,
    error = Red500,
    onError = Color.White,
    errorContainer = Red400.copy(alpha = 0.15f),
    onErrorContainer = Red600,
    outline = Gray200,
    outlineVariant = Gray100
)

/**
 * 深色主题配色方案
 */
private val DarkColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = Color(0xFF003258),
    primaryContainer = Blue700,
    onPrimaryContainer = Blue400,
    secondary = Purple400,
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Purple500.copy(alpha = 0.2f),
    onSecondaryContainer = Purple400,
    tertiary = Green400,
    onTertiary = Color(0xFF00391C),
    tertiaryContainer = Green600.copy(alpha = 0.2f),
    background = DarkBackground,
    onBackground = OnBackgroundDark,
    surface = DarkSurface,
    onSurface = OnSurfaceDark,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = Gray500,
    error = Red400,
    onError = Color(0xFF601410),
    errorContainer = Red600.copy(alpha = 0.2f),
    onErrorContainer = Red400,
    outline = Gray500,
    outlineVariant = DarkSurface
)

/**
 * ExpiryGuard 主题，统一管理颜色和排版。
 *
 * @param darkTheme 是否使用深色主题，默认跟随系统设置
 * @param monetEnabled 是否启用莫奈动态取色
 * @param content 可组合内容
 */
@Composable
fun ExpiryGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    monetEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        monetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}