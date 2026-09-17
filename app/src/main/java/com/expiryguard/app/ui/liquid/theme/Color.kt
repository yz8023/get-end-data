package com.expiryguard.app.ui.liquid.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens — colour.
 *
 * The neutral / cool-blue structure follows the reference UI (`kd64i/dyparse`,
 * which itself borrows the KernelSU + Miuix visual language). The scheme below
 * expands that reference into a complete, self-contained token set so the
 * template can be re-skinned by editing this one file.
 */

// ---------------------------------------------------------------------------
// Core neutrals
// ---------------------------------------------------------------------------
internal val Ink = Color(0xFF1A1C1E)
internal val InkMuted = Color(0xFF5A6068)
internal val Paper = Color(0xFFF8F9FC)
internal val PaperContainer = Color(0xFFEFF3F8)
internal val PaperContainerLow = Color(0xFFF2F5F9)

internal val Night = Color(0xFF0C1118)
internal val NightContainer = Color(0xFF1A222C)
internal val NightContainerLow = Color(0xFF141B24)
internal val NightText = Color(0xFFE6EAF1)

// ---------------------------------------------------------------------------
// Accents
// ---------------------------------------------------------------------------
internal val BlueLight = Color(0xFF00639A)
internal val BlueLightContainer = Color(0xFFCBE6FF)
internal val OnBlueLightContainer = Color(0xFF001D32)

internal val BlueDark = Color(0xFF9DCCFF)
internal val BlueDarkContainer = Color(0xFF164A70)
internal val OnBlueDarkContainer = Color(0xFFD1E8FF)

internal val SlateLight = Color(0xFF4F6072)
internal val SlateDark = Color(0xFFB8C8DA)

// Semantic accents used by the component showcase.
val AuroraMint = Color(0xFF4FD1A5)
val AuroraAmber = Color(0xFFFFB65C)
val AuroraRose = Color(0xFFFF7D93)
val AuroraViolet = Color(0xFF9D8CFF)

// ---------------------------------------------------------------------------
// Brand palette seeds — the four colour themes
// ---------------------------------------------------------------------------
internal val MintLightPrimary = Color(0xFF006B4E)
internal val MintLightContainer = Color(0xFFB9F2D7)
internal val MintLightSecondary = Color(0xFF4C6358)
internal val MintLightTertiary = Color(0xFF386574)
internal val MintDarkPrimary = Color(0xFF5FDBA8)
internal val MintDarkContainer = Color(0xFF004D37)
internal val MintDarkSecondary = Color(0xFFB3CCBF)
internal val MintDarkTertiary = Color(0xFFA7CCDD)

internal val SunsetLightPrimary = Color(0xFF8F4C00)
internal val SunsetLightContainer = Color(0xFFFFDCC4)
internal val SunsetLightSecondary = Color(0xFF77574A)
internal val SunsetLightTertiary = Color(0xFF6B5D2F)
internal val SunsetDarkPrimary = Color(0xFFFFB77A)
internal val SunsetDarkContainer = Color(0xFF6C3600)
internal val SunsetDarkSecondary = Color(0xFFE7BEAC)
internal val SunsetDarkTertiary = Color(0xFFD7C28B)

internal val MonoLightPrimary = Color(0xFF425F73)
internal val MonoLightContainer = Color(0xFFCBE7FF)
internal val MonoLightSecondary = Color(0xFF52606F)
internal val MonoLightTertiary = Color(0xFF6B7280)
internal val MonoDarkPrimary = Color(0xFFA5C8DD)
internal val MonoDarkContainer = Color(0xFF293E4D)
internal val MonoDarkSecondary = Color(0xFFB7C8DA)
internal val MonoDarkTertiary = Color(0xFFCBD2DC)

internal val CoralLightPrimary = Color(0xFF9C4337)
internal val CoralLightContainer = Color(0xFFFFDAD3)
internal val CoralLightSecondary = Color(0xFF77574E)
internal val CoralLightTertiary = Color(0xFF715C3F)
internal val CoralDarkPrimary = Color(0xFFFFB4A5)
internal val CoralDarkContainer = Color(0xFF7B2F25)
internal val CoralDarkSecondary = Color(0xFFE7BDB4)
internal val CoralDarkTertiary = Color(0xFFD7C092)

internal val SakuraLightPrimary = Color(0xFF8E4A63)
internal val SakuraLightContainer = Color(0xFFFFD9E2)
internal val SakuraLightSecondary = Color(0xFF74565F)
internal val SakuraLightTertiary = Color(0xFF815D36)
internal val SakuraDarkPrimary = Color(0xFFFFB1C7)
internal val SakuraDarkContainer = Color(0xFF723349)
internal val SakuraDarkSecondary = Color(0xFFE1BDC6)
internal val SakuraDarkTertiary = Color(0xFFD7C08F)

internal val GalaxyLightPrimary = Color(0xFF4053A8)
internal val GalaxyLightContainer = Color(0xFFDDE1FF)
internal val GalaxyLightSecondary = Color(0xFF5B5C72)
internal val GalaxyLightTertiary = Color(0xFF7A5382)
internal val GalaxyDarkPrimary = Color(0xFFB4C5FF)
internal val GalaxyDarkContainer = Color(0xFF303D8C)
internal val GalaxyDarkSecondary = Color(0xFFC2C4DD)
internal val GalaxyDarkTertiary = Color(0xFFE2B4EB)

// ---------------------------------------------------------------------------
// Decorative background (the layer the liquid glass refracts)
// ---------------------------------------------------------------------------
internal val AuroraLight = listOf(
    Color(0xFFFFFFFF),
    Color(0xFFE8F1FF),
    Color(0xFFF4ECFF),
    Color(0xFFE6FBF5),
)

internal val AuroraDark = listOf(
    Color(0xFF0A0F16),
    Color(0xFF102438),
    Color(0xFF1A1536),
    Color(0xFF0B2822),
)
