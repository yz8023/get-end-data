package com.expiryguard.app.ui.liquid.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** The colour themes the settings page switches between. */
enum class PaletteId(val label: String) {
    Aurora("冰蓝"),
    Mint("薄荷"),
    Sunset("落日"),
    Mono("素灰"),
    Coral("珊瑚"),
    Sakura("樱花"),
    Galaxy("星夜")
}

/** Light-mode primary of a palette — used for swatch previews in settings. */
fun palettePrimaryLight(palette: PaletteId): Color = PaletteSeeds.getValue(palette).light.primary

private data class PaletteSeed(
    val light: ColorSchemeSeed,
    val dark: ColorSchemeSeed
)

private data class ColorSchemeSeed(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val tertiary: Color
)

private val PaletteSeeds = mapOf(
    PaletteId.Aurora to PaletteSeed(
        light = ColorSchemeSeed(
            primary = BlueLight,
            onPrimary = Color.White,
            primaryContainer = BlueLightContainer,
            onPrimaryContainer = OnBlueLightContainer,
            secondary = SlateLight,
            tertiary = Color(0xFF5B4DCC)
        ),
        dark = ColorSchemeSeed(
            primary = BlueDark,
            onPrimary = Color(0xFF003354),
            primaryContainer = BlueDarkContainer,
            onPrimaryContainer = OnBlueDarkContainer,
            secondary = SlateDark,
            tertiary = Color(0xFFC8BFFF)
        )
    ),
    PaletteId.Mint to PaletteSeed(
        light = ColorSchemeSeed(
            primary = MintLightPrimary,
            onPrimary = Color.White,
            primaryContainer = MintLightContainer,
            onPrimaryContainer = Color(0xFF002114),
            secondary = MintLightSecondary,
            tertiary = MintLightTertiary
        ),
        dark = ColorSchemeSeed(
            primary = MintDarkPrimary,
            onPrimary = Color(0xFF00351F),
            primaryContainer = MintDarkContainer,
            onPrimaryContainer = Color(0xFF9AF3C6),
            secondary = MintDarkSecondary,
            tertiary = MintDarkTertiary
        )
    ),
    PaletteId.Sunset to PaletteSeed(
        light = ColorSchemeSeed(
            primary = SunsetLightPrimary,
            onPrimary = Color.White,
            primaryContainer = SunsetLightContainer,
            onPrimaryContainer = Color(0xFF2E1600),
            secondary = SunsetLightSecondary,
            tertiary = SunsetLightTertiary
        ),
        dark = ColorSchemeSeed(
            primary = SunsetDarkPrimary,
            onPrimary = Color(0xFF4A2800),
            primaryContainer = SunsetDarkContainer,
            onPrimaryContainer = Color(0xFFFFDCC4),
            secondary = SunsetDarkSecondary,
            tertiary = SunsetDarkTertiary
        )
    ),
    PaletteId.Mono to PaletteSeed(
        light = ColorSchemeSeed(
            primary = MonoLightPrimary,
            onPrimary = Color.White,
            primaryContainer = MonoLightContainer,
            onPrimaryContainer = Color(0xFF081F2E),
            secondary = MonoLightSecondary,
            tertiary = MonoLightTertiary
        ),
        dark = ColorSchemeSeed(
            primary = MonoDarkPrimary,
            onPrimary = Color(0xFF0A283A),
            primaryContainer = MonoDarkContainer,
            onPrimaryContainer = Color(0xFFC1E3F8),
            secondary = MonoDarkSecondary,
            tertiary = MonoDarkTertiary
        )
    ),
    PaletteId.Coral to PaletteSeed(
        light = ColorSchemeSeed(
            primary = CoralLightPrimary,
            onPrimary = Color.White,
            primaryContainer = CoralLightContainer,
            onPrimaryContainer = Color(0xFF3B0800),
            secondary = CoralLightSecondary,
            tertiary = CoralLightTertiary
        ),
        dark = ColorSchemeSeed(
            primary = CoralDarkPrimary,
            onPrimary = Color(0xFF581007),
            primaryContainer = CoralDarkContainer,
            onPrimaryContainer = Color(0xFFFFDAD3),
            secondary = CoralDarkSecondary,
            tertiary = CoralDarkTertiary
        )
    ),
    PaletteId.Sakura to PaletteSeed(
        light = ColorSchemeSeed(
            primary = SakuraLightPrimary,
            onPrimary = Color.White,
            primaryContainer = SakuraLightContainer,
            onPrimaryContainer = Color(0xFF3A001F),
            secondary = SakuraLightSecondary,
            tertiary = SakuraLightTertiary
        ),
        dark = ColorSchemeSeed(
            primary = SakuraDarkPrimary,
            onPrimary = Color(0xFF551A2D),
            primaryContainer = SakuraDarkContainer,
            onPrimaryContainer = Color(0xFFFFD9E2),
            secondary = SakuraDarkSecondary,
            tertiary = SakuraDarkTertiary
        )
    ),
    PaletteId.Galaxy to PaletteSeed(
        light = ColorSchemeSeed(
            primary = GalaxyLightPrimary,
            onPrimary = Color.White,
            primaryContainer = GalaxyLightContainer,
            onPrimaryContainer = Color(0xFF00196B),
            secondary = GalaxyLightSecondary,
            tertiary = GalaxyLightTertiary
        ),
        dark = ColorSchemeSeed(
            primary = GalaxyDarkPrimary,
            onPrimary = Color(0xFF14237A),
            primaryContainer = GalaxyDarkContainer,
            onPrimaryContainer = Color(0xFFDDE1FF),
            secondary = GalaxyDarkSecondary,
            tertiary = GalaxyDarkTertiary
        )
    )
)

private fun schemeFor(palette: PaletteId, dark: Boolean): ColorScheme {
    val seed = PaletteSeeds.getValue(palette)
    return if (dark) {
        darkColorScheme(
            primary = seed.dark.primary,
            onPrimary = seed.dark.onPrimary,
            primaryContainer = seed.dark.primaryContainer,
            onPrimaryContainer = seed.dark.onPrimaryContainer,
            secondary = seed.dark.secondary,
            tertiary = seed.dark.tertiary,
            background = Night,
            onBackground = NightText,
            surface = Night,
            onSurface = NightText,
            surfaceContainer = NightContainer,
            surfaceContainerLow = NightContainerLow,
            surfaceContainerLowest = Color(0xFF0A0E14),
            surfaceContainerHigh = Color(0xFF202934),
            surfaceContainerHighest = Color(0xFF26313E),
            surfaceVariant = Color(0xFF41484F),
            onSurfaceVariant = Color(0xFFC1C7CE),
            outline = Color(0xFF8B929A),
            outlineVariant = Color(0xFF41484F),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005)
        )
    } else {
        lightColorScheme(
            primary = seed.light.primary,
            onPrimary = seed.light.onPrimary,
            primaryContainer = seed.light.primaryContainer,
            onPrimaryContainer = seed.light.onPrimaryContainer,
            secondary = seed.light.secondary,
            tertiary = seed.light.tertiary,
            background = Paper,
            onBackground = Ink,
            surface = Paper,
            onSurface = Ink,
            surfaceContainer = PaperContainer,
            surfaceContainerLow = PaperContainerLow,
            surfaceContainerLowest = Color.White,
            surfaceContainerHigh = Color(0xFFE7EDF4),
            surfaceContainerHighest = Color(0xFFDFE6EE),
            surfaceVariant = Color(0xFFDDE3EA),
            onSurfaceVariant = InkMuted,
            outline = Color(0xFF71787F),
            outlineVariant = Color(0xFFC1C7CE),
            error = Color(0xFFBA1A1A),
            onError = Color.White
        )
    }
}

/** How the app resolves light / dark. */
enum class ThemeMode { System, Light, Dark }

/**
 * The three mutually-exclusive visual styles the settings page offers.
 * Replaces the old independent `glassEnabled` / `dynamicColor` booleans.
 */
enum class VisualMode(val label: String) {
    /** Liquid-glass look: sampled-backdrop panels, glass bottom bar. */
    Glass("玻璃"),
    /** Plain Material surfaces, solid bottom bar, no refraction. */
    Normal("普通"),
    /** Material Design 3 dynamic colour scheme sampled from the wallpaper. */
    Md3("MD3")
}

/**
 * Presentation knobs that sit on top of the Material colour scheme.
 * Exposed through [LocalAppChrome] so components read one source of truth.
 */
@Immutable
data class AppChrome(
    /**
     * Master style switch. `Glass` is the liquid-glass look; `Normal` falls
     * back to plain Material surfaces, a solid bottom bar and no refraction;
     * `Md3` uses the system dynamic colour scheme (Material You).
     */
    val visualMode: VisualMode = VisualMode.Glass,
    /**
     * Glass opacity, 0f..1f. Drives how dense the tint over the refracted
     * backdrop reads — 0 is a barely-there window, 1 is a dense frosted panel.
     * Read by [com.expiryguard.app.ui.liquid.components.liquidGlass].
     */
    val glassOpacity: Float = 0.55f,
    /**
     * Glass refraction, 0f..1f. Scales the lens distortion applied to the
     * sampled backdrop — 0 is a flat window, 1 is the full liquid lens.
     * Read by [com.expiryguard.app.ui.liquid.components.liquidGlass].
     */
    val glassRefraction: Float = 1f,
    /** Scales every decorative radius; the component API is untouched. */
    val cornerScale: Float = 1f,
    /** Global escape hatch — motion demos honour this per animation. */
    val reducedMotion: Boolean = false,
    /**
     * Spring damping, 0f..1f. Pushed into [com.expiryguard.app.ui.liquid.motion.Motion.motionDamping]
     * so one slider retunes every spring token in the app.
     */
    val motionDamping: Float = 0.5f,
    /** Use the device's home-screen wallpaper as the background layer. */
    val wallpaperBackground: Boolean = false
)

val LocalAppChrome = staticCompositionLocalOf { AppChrome() }

/** True when the platform can actually run a backdrop blur. */
val supportsBackdropBlur: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun LiquidUITheme(
    themeMode: ThemeMode = ThemeMode.System,
    visualMode: VisualMode = VisualMode.Glass,
    palette: PaletteId = PaletteId.Aurora,
    chrome: AppChrome = AppChrome(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val colorScheme = when {
        visualMode == VisualMode.Md3 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> schemeFor(palette, darkTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppChrome provides chrome) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = if (chrome.cornerScale == 1f) Shapes else scaledShapes(chrome.cornerScale),
            content = content
        )
    }
}

/** Rebuilds the shape scale with every radius multiplied by [scale]. */
private fun scaledShapes(scale: Float) = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp * scale),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp * scale),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp * scale),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp * scale),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp * scale)
)
