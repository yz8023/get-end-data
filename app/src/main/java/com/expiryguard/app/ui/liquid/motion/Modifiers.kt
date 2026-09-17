package com.expiryguard.app.ui.liquid.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Reusable motion modifiers.
 *
 * These are the "handfeel" primitives — press weight, impact, idle life and
 * staggered entrance. They compose: a hero card typically uses [pressScale] +
 * [StaggeredReveal] + [idleBreathing] at the same time.
 */

/**
 * Squash on press with the snappy spring. Replaces a plain `clickable`
 * ripple for controls that should read as physical.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.94f,
    enabled: Boolean = true
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = Motion.snappy(0.001f),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Impact shake (motion-web §8).
 *
 * Two rules make the difference between "有冲击力" and "抖成一团糊":
 * different frequency per axis, and amplitude decaying linearly to zero.
 * Sources are never summed — call this once per impact.
 *
 * @param trigger change this value to fire the shake; `null` never fires.
 */
fun Modifier.impactShake(
    trigger: Any?,
    amplitude: Float = 10f,
    durationMs: Int = 300
): Modifier = composed {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
    }
    graphicsLayer {
        val p = progress.value
        val envelope = (1f - p).coerceAtLeast(0f)
        val seconds = p * durationMs / 1000f
        translationX = sin(seconds * 47f) * amplitude * envelope
        translationY = sin(seconds * 31f + 1.3f) * amplitude * envelope
    }
}

/**
 * Ambient idle life (motion-web §6). Nothing on screen should be perfectly
 * still; two slightly different frequencies keep it from pulsing in unison.
 */
@Composable
fun Modifier.idleBreathing(
    periodMs: Int = 5600,
    amplitudeX: Float = 0.012f,
    amplitudeY: Float = 0.020f
): Modifier {
    val transition = rememberInfiniteTransition(label = "idle")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing)),
        label = "idlePhase"
    )
    return this.graphicsLayer {
        scaleX = 1f + sin(phase) * amplitudeX
        scaleY = 1f + sin(phase * 0.84f + 0.7f) * amplitudeY
    }
}

/**
 * Entrance with a stagger delay. The finished state is the default when
 * [Motion] is disabled, so reduced-motion users never get stuck invisible
 * content.
 */
@Composable
fun StaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    staggerMs: Int = Motion.Stagger.StandardList,
    startDelayMs: Int = 60,
    translateYDp: Int = 18,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(!enabled) }
    LaunchedEffect(enabled, index) {
        if (enabled) {
            delay((startDelayMs + index * staggerMs).toLong())
            visible = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = Motion.gentle(0.01f),
        label = "staggerReveal"
    )
    val offsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { translateYDp.dp.toPx() }
    androidx.compose.foundation.layout.Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * offsetPx
        }
    ) {
        content()
    }
}
