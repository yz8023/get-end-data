package com.expiryguard.app.ui.liquid.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.expiryguard.app.ui.liquid.theme.AppShape
import com.expiryguard.app.ui.liquid.theme.AuroraDark
import com.expiryguard.app.ui.liquid.theme.AuroraLight
import com.expiryguard.app.ui.liquid.theme.LocalAppChrome
import com.expiryguard.app.ui.liquid.theme.VisualMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The decorative layer the glass refracts.
 *
 * Soft radial blobs are drawn directly (no `RenderEffect`), so this stays cheap
 * and works on every API level. The palette is intentionally small — the
 * reference guidance measures award-tier pages at a median of four dominant
 * colours, and gradients-as-decoration are a recognisable "generated" tell.
 * Keep it subtle: the glass panels are the subject, not the background.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(28_000, easing = LinearEasing)),
        label = "auroraPhase"
    )
    val palette = if (dark) AuroraDark else AuroraLight
    val base = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(base, base, palette.first()))
        )
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            drawAurora(phase, palette)
        }
        content()
    }
}

private fun DrawScope.drawAurora(phase: Float, palette: List<Color>) {
    // Four blobs, each with its own orbit and period so nothing pulses in unison.
    val blobs = listOf(
        Blob(cx = 0.20f, cy = 0.16f, r = 0.62f, orbit = 0.10f, speed = 1.0f, color = palette[1]),
        Blob(cx = 0.86f, cy = 0.28f, r = 0.52f, orbit = 0.12f, speed = -1.35f, color = palette[2]),
        Blob(cx = 0.34f, cy = 0.82f, r = 0.66f, orbit = 0.09f, speed = 0.78f, color = palette[3]),
        Blob(cx = 0.92f, cy = 0.88f, r = 0.48f, orbit = 0.08f, speed = -0.62f, color = palette[1]),
    )
    blobs.forEach { blob ->
        val x = (blob.cx + cos(phase * blob.speed) * blob.orbit) * size.width
        val y = (blob.cy + sin(phase * blob.speed * 0.8f) * blob.orbit) * size.height
        val radius = blob.r * size.minDimension
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    blob.color.copy(alpha = 0.55f),
                    blob.color.copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = Offset(x, y),
                radius = radius
            ),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private data class Blob(
    val cx: Float,
    val cy: Float,
    val r: Float,
    val orbit: Float,
    val speed: Float,
    val color: Color
)

/**
 * Backdrop layer the glass surfaces refract.
 *
 * Provided at the app root ([AppRoot]): the aurora is recorded into its own
 * graphics layer and every glass panel samples it, so cards genuinely refract
 * the decoration behind them. Null inside separate dialog windows — glass then
 * degrades to a translucent tint (a backdrop cannot be sampled cross-window).
 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Full scene backdrop (aurora + page content combined) for same-window overlays.
 *
 * Dialog panels sample this instead of [LocalGlassBackdrop] so they refract the
 * live pages beneath them; a scrim dims everything else, and the panel reads as
 * a bright glass patch on the dimmed scene. Null when the shell is in normal
 * (non-glass) layout.
 */
val LocalGlassContentBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * The liquid-glass surface used by every panel in the template.
 *
 * Real backdrop sampling on top of [LocalGlassBackdrop]:
 *  1. specular [Highlight] edge
 *  2. sampled backdrop refracted through a lens (`vibrancy` + `blur` + `lens`)
 *  3. a translucent [tint] fill whose density follows `glassOpacity`
 *  4. refraction strength follows `glassRefraction`
 *
 * On devices where the platform effects are unavailable the library no-ops the
 * blur/lens chain internally and the panel reads as a soft translucent tint.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = MaterialTheme.shapes.large,
    tint: Color? = null,
    elevation: Dp = 1.dp,
    borderWidth: Dp = 1.dp,
    borderAlpha: Float = 0.42f
): Modifier {
    val chrome = LocalAppChrome.current
    val opacity = chrome.glassOpacity.coerceIn(0f, 1f)
    val refraction = chrome.glassRefraction.coerceIn(0f, 1f)
    val backdrop = LocalGlassBackdrop.current
    val baseTint = tint ?: MaterialTheme.colorScheme.surfaceContainer
    val isDark = isSystemInDarkTheme()

    // Normal / MD3 modes: plain Material surface, no backdrop sampling at all.
    if (chrome.visualMode != VisualMode.Glass) {
        val solidTint = baseTint.copy(alpha = 1f)
        return this
            .shadow(elevation, shape, clip = false)
            .clip(shape)
            .background(solidTint)
    }

    if (backdrop != null) {
        val resolvedTint = baseTint.copy(alpha = baseTint.alpha * (0.10f + opacity * 0.55f))
        return this
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                    if (refraction > 0.01f) {
                        lens(
                            refractionHeight = 26f.dp.toPx() * refraction,
                            refractionAmount = 26f.dp.toPx() * refraction
                        )
                    }
                },
                highlight = {
                    Highlight.Default.copy(alpha = borderAlpha.coerceIn(0f, 1f) * 0.9f)
                },
                shadow = {
                    if (elevation > 0.dp) {
                        Shadow(
                            radius = elevation * 6f,
                            color = Color.Black,
                            alpha = if (isDark) 0.28f else 0.08f
                        )
                    } else {
                        null
                    }
                },
                onDrawSurface = { drawRect(resolvedTint) }
            )
    }

    // Fallback: no backdrop layer available (e.g. dialog window) — translucent tint.
    val resolvedTint = baseTint.copy(alpha = baseTint.alpha * (0.28f + opacity * 0.45f))
    return this
        .shadow(elevation, shape, clip = false)
        .clip(shape)
        .background(resolvedTint)
}

/** A glass panel with standard inner padding. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    tint: Color? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.liquidGlass(shape = shape, tint = tint),
        content = {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    )
}

/** A small glass badge / chip surface. */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .liquidGlass(shape = AppShape.pill, tint = tint, elevation = 0.dp, borderAlpha = 0.30f),
        content = { content() }
    )
}
