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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.expiryguard.app.ui.liquid.motion.Motion

/** The colour themes the settings page switches between. */
enum class PaletteId(val label: String) {
    Aurora("冰蓝"),
    Mint("薄荷"),
    Sunset("落日"),
    Mono("素灰"),
    Coral("珊瑚"),
    Sakura("樱花"),
    Galaxy("星夜"),
    Custom("自定义")
}

/** The palette switched to when the user picks a free seed colour. */
val CustomPalette = PaletteId.Custom

/** Light-mode primary of a palette — used for swatch previews in settings. */
fun palettePrimaryLight(palette: PaletteId): Color =
    if (palette == PaletteId.Custom) Color(0xFF3E8FE0)
    else PaletteSeeds.getValue(palette).light.primary

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

private fun schemeFor(
    palette: PaletteId,
    dark: Boolean,
    customSeed: Color = Color(0xFF3E8FE0),
    customShade: Float = 0.5f
): ColorScheme {
    if (palette == PaletteId.Custom) {
        return seedSchemeFor(customSeed, dark, customShade)
    }
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

/**
 * Builds a tonal Material scheme from an arbitrary seed colour. A simple HSB
 * pivot: the seed becomes the primary, its hue rotated ±40° drives secondary /
 * tertiary, and the surfaces shift toward the seed by [shade] (0..1) so the
 * custom background actually looks custom. Good contrast approximation: on
 * colours are derived from luminance rather than hand-picked pairs.
 */
private fun seedSchemeFor(seed: Color, dark: Boolean, shade: Float): ColorScheme {
    val hsb = FloatArray(3)
    android.graphics.Color.colorToHSV(seed.toArgb(), hsb)
    val hue = hsb[0]
    val saturation = hsb[1]
    val shadeClamped = shade.coerceIn(0f, 1f)

    fun withHue(deg: Float, satScale: Float = 1f, valueScale: Float = 1f): Color {
        val h = (hue + deg + 360f) % 360f
        val s = (saturation * satScale).coerceIn(0f, 1f)
        val v = (hsb[2] * valueScale).coerceIn(0f, 1f)
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    }

    val primary = seed
    val secondary = withHue(40f, 1.1f, 0.9f)
    val tertiary = withHue(-40f, 0.9f, 1.05f)
    val background = when {
        dark -> Night.lerp(primary, 0.12f + shadeClamped * 0.12f)
        else -> Color.White.lerp(primary, 0.03f + shadeClamped * 0.05f)
    }
    val onBackground = if (dark) NightText else Ink
    val surfaceContainer = when {
        dark -> NightContainer.lerp(primary, 0.16f + shadeClamped * 0.10f)
        else -> PaperContainer.lerp(primary, 0.10f + shadeClamped * 0.10f)
    }

    val lightPrimary = primary.luminance() > 0.5f
    val onPrimary = if (lightPrimary) Color(0xFF101418) else Color.White
    val primaryContainer = if (dark) {
        primary.lerp(Color.White, 0.18f)
    } else {
        primary.lerp(Color.White, 0.78f)
    }
    val onPrimaryContainer = if (dark) {
        primary.lerp(Color.White, 0.75f)
    } else {
        primary.lerp(Color.Black, 0.72f)
    }

    if (dark) {
        return darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            tertiary = tertiary,
            background = background,
            onBackground = onBackground,
            surface = background,
            onSurface = onBackground,
            surfaceContainer = surfaceContainer,
            surfaceContainerLow = background,
            surfaceContainerLowest = background.lerp(Color.Black, 0.25f),
            surfaceContainerHigh = surfaceContainer.lerp(Color.White, 0.05f),
            surfaceContainerHighest = surfaceContainer.lerp(Color.White, 0.09f),
            surfaceVariant = surfaceContainer.lerp(Color.White, 0.12f),
            onSurfaceVariant = Color(0xFFC1C7CE),
            outline = Color(0xFF8B929A),
            outlineVariant = surfaceContainer.lerp(Color.White, 0.14f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005)
        )
    }
    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        tertiary = tertiary,
        background = background,
        onBackground = onBackground,
        surface = background,
        onSurface = onBackground,
        surfaceContainer = surfaceContainer,
        surfaceContainerLow = background,
        surfaceContainerLowest = Color.White,
        surfaceContainerHigh = surfaceContainer.lerp(Color.White, 0.35f),
        surfaceContainerHighest = surfaceContainer.lerp(Color.White, 0.25f),
        surfaceVariant = surfaceContainer.lerp(Color.White, 0.30f),
        onSurfaceVariant = InkMuted,
        outline = Color(0xFF71787F),
        outlineVariant = Color(0xFFC1C7CE),
        error = Color(0xFFBA1A1A),
        onError = Color.White
    )
}

private fun Color.lerp(other: Color, fraction: Float): Color =
    androidx.compose.ui.graphics.lerp(this, other, fraction.coerceIn(0f, 1f))

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
    customSeed: Color = Color(0xFF3E8FE0),
    customShade: Float = 0.5f,
    useDynamicColors: Boolean = false,
    chrome: AppChrome = AppChrome(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    // 动态取色只影响配色方案，与视觉风格解耦：
    // - useDynamicColors 为 true（莫奈取色）或显式选了 MD3 视觉风格时使用
    //   系统动态色作为主色系；
    // - 玻璃渲染由 chrome.visualMode（即下方 visualMode）决定，不受影响。
    val colorScheme = when {
        (visualMode == VisualMode.Md3 || useDynamicColors) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> schemeFor(palette, darkTheme, customSeed, customShade)
    }

    val animatedScheme = if (chrome.reducedMotion) {
        colorScheme
    } else {
        rememberAnimatedColorScheme(colorScheme, reducedMotion = false)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = animatedScheme.background.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppChrome provides chrome) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = Typography,
            shapes = if (chrome.cornerScale == 1f) Shapes else scaledShapes(chrome.cornerScale),
            content = content
        )
    }
}

/**
 * Animates every [ColorScheme] role with one tween so a theme switch
 * cross-fades. [animateColorAsState] remembers each role's previous value, so
 * mid-transition colors lerp from the old scheme to the new instead of snapping.
 */
@Composable
private fun rememberAnimatedColorScheme(target: ColorScheme, reducedMotion: Boolean): ColorScheme {
    val spec = if (reducedMotion) {
        androidx.compose.animation.core.tween<Color>(0)
    } else {
        androidx.compose.animation.core.tween<Color>(Motion.Duration.Standard)
    }
    @Composable
    fun animated(role: Color, label: String): Color =
        androidx.compose.animation.animateColorAsState(
            role,
            animationSpec = spec,
            label = label
        ).value
    return ColorScheme(
        primary = animated(target.primary, "primary"),
        onPrimary = animated(target.onPrimary, "onPrimary"),
        primaryContainer = animated(target.primaryContainer, "primaryContainer"),
        onPrimaryContainer = animated(target.onPrimaryContainer, "onPrimaryContainer"),
        inversePrimary = animated(target.inversePrimary, "inversePrimary"),
        secondary = animated(target.secondary, "secondary"),
        onSecondary = animated(target.onSecondary, "onSecondary"),
        secondaryContainer = animated(target.secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = animated(target.onSecondaryContainer, "onSecondaryContainer"),
        tertiary = animated(target.tertiary, "tertiary"),
        onTertiary = animated(target.onTertiary, "onTertiary"),
        tertiaryContainer = animated(target.tertiaryContainer, "tertiaryContainer"),
        onTertiaryContainer = animated(target.onTertiaryContainer, "onTertiaryContainer"),
        background = animated(target.background, "background"),
        onBackground = animated(target.onBackground, "onBackground"),
        surface = animated(target.surface, "surface"),
        onSurface = animated(target.onSurface, "onSurface"),
        surfaceVariant = animated(target.surfaceVariant, "surfaceVariant"),
        onSurfaceVariant = animated(target.onSurfaceVariant, "onSurfaceVariant"),
        surfaceTint = animated(target.surfaceTint, "surfaceTint"),
        inverseSurface = animated(target.inverseSurface, "inverseSurface"),
        inverseOnSurface = animated(target.inverseOnSurface, "inverseOnSurface"),
        error = animated(target.error, "error"),
        onError = animated(target.onError, "onError"),
        errorContainer = animated(target.errorContainer, "errorContainer"),
        onErrorContainer = animated(target.onErrorContainer, "onErrorContainer"),
        outline = animated(target.outline, "outline"),
        outlineVariant = animated(target.outlineVariant, "outlineVariant"),
        scrim = animated(target.scrim, "scrim"),
        surfaceBright = animated(target.surfaceBright, "surfaceBright"),
        surfaceDim = animated(target.surfaceDim, "surfaceDim"),
        surfaceContainer = animated(target.surfaceContainer, "surfaceContainer"),
        surfaceContainerHigh = animated(target.surfaceContainerHigh, "surfaceContainerHigh"),
        surfaceContainerHighest = animated(target.surfaceContainerHighest, "surfaceContainerHighest"),
        surfaceContainerLow = animated(target.surfaceContainerLow, "surfaceContainerLow"),
        surfaceContainerLowest = animated(target.surfaceContainerLowest, "surfaceContainerLowest")
    )
}

/** Rebuilds the shape scale with every radius multiplied by [scale]. */
private fun scaledShapes(scale: Float) = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp * scale),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp * scale),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp * scale),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp * scale),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp * scale)
)
