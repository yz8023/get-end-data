package com.expiryguard.app.ui.liquid.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.expiryguard.app.ui.liquid.motion.Motion
import com.expiryguard.app.ui.liquid.motion.pressScale
import com.expiryguard.app.ui.liquid.theme.AppShape
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Primary action. Snappy press weight, no ripple. */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(interaction, 0.96f, enabled),
        enabled = enabled,
        shape = AppShape.control,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        content = content
    )
}

/** Secondary action. */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.pressScale(interaction, 0.96f, enabled),
        enabled = enabled,
        shape = AppShape.control,
        interactionSource = interaction,
        content = content
    )
}

/** Circular glass icon button with the same press weight. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(44.dp)
            .pressScale(interaction, 0.90f)
            .liquidGlass(shape = CircleShape, elevation = 1.dp, borderAlpha = 0.5f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/**
 * Compact switch sized to a text line (~20dp). The thumb is a draggable pip: it
 * follows the finger, and on release snaps to whichever side is nearer while
 * the track colour cross-fades — so toggling reads as one physical motion.
 */
@Composable
fun CompactSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth = 44.dp
    val trackHeight = 26.dp
    val thumbSize = 20.dp
    val inset = 3.dp
    val travelPx = with(LocalDensity.current) { (trackWidth - thumbSize - inset).toPx() }
    val fraction = remember { Animatable(if (checked) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val dragging = remember { mutableStateOf(false) }

    LaunchedEffect(checked) {
        if (!dragging.value) fraction.animateTo(if (checked) 1f else 0f, Motion.snappy(0.01f))
    }

    val thumbOffset = offsetDp(fraction.value, travelPx, trackWidth, thumbSize, inset)

    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            checked -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = androidx.compose.animation.core.tween(Motion.Duration.Fast),
        label = "switchTrack"
    )
    val trackBorder by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = androidx.compose.animation.core.tween(Motion.Duration.Fast),
        label = "switchTrackBorder"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.94f,
        animationSpec = Motion.snappy(0.001f),
        label = "switchThumbScale"
    )

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, trackBorder, CircleShape)
            .pointerInput(enabled) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        if (enabled) {
                            dragging.value = true
                            scope.launch { fraction.stop() }
                        }
                    },
                    onDragEnd = {
                        if (enabled) {
                            dragging.value = false
                            val newState = fraction.value > 0.5f
                            scope.launch {
                                fraction.animateTo(if (newState) 1f else 0f, Motion.snappy(0.01f))
                            }
                            if (newState != checked) onCheckedChange(newState)
                        }
                    },
                    onDragCancel = {
                        if (enabled) {
                            dragging.value = false
                            scope.launch {
                                fraction.animateTo(if (checked) 1f else 0f, Motion.snappy(0.01f))
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (enabled) {
                            change.consume()
                            scope.launch { fraction.snapTo((fraction.value + dragAmount / travelPx).coerceIn(0f, 1f)) }
                        }
                    }
                )
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun offsetDp(fraction: Float, travelPx: Float, trackWidth: Dp, thumbSize: Dp, inset: Dp): Dp =
    with(LocalDensity.current) {
        (inset.toPx() + travelPx * fraction).toDp()
    }

/**
 * A bare colour/scale picker track: a horizontal gradient bar with a draggable
 * thumb. Tap anywhere or drag the thumb; both translate to a value in 0..1.
 */
@Composable
fun GradientBar(
    colors: List<Color>,
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbSize = 20.dp
    val thumbPx = with(density) { thumbSize.toPx() }
    var barWidthPx by remember { mutableIntStateOf(0) }

    fun fractionFromPosition(x: Float): Float =
        ((x - thumbPx) / (barWidthPx - 2 * thumbPx).toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .onSizeChanged { barWidthPx = it.width }
            .pointerInput(colors, barWidthPx, thumbPx) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        onFractionChange(fractionFromPosition(offset.x))
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onFractionChange(fractionFromPosition(change.position.x))
                    }
                )
            }
            .pointerInput(colors, barWidthPx, thumbPx) {
                detectTapGestures { offset ->
                    onFractionChange(fractionFromPosition(offset.x))
                }
            }
            .padding(horizontal = thumbSize)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(colors))
                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(x = with(density) { ((barWidthPx - 2 * thumbPx) * fraction).toDp() })
                .size(thumbSize)
                .align(Alignment.CenterStart)
                .shadow(2.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
    }
}

/**
 * Segmented control with a sliding glass thumb. The thumb tracks a draggable
 * float position: tap a segment to jump, or drag horizontally across the whole
 * track and release — the thumb snaps to the nearest segment with a spring.
 * Uses a single animated float so the thumb never overshoots past the last
 * segment.
 */
@Composable
fun SegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var widthPx by remember { mutableIntStateOf(0) }
    val fraction = remember { Animatable(selectedIndex.toFloat()) }
    val dragging = remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex) {
        if (!dragging.value) fraction.animateTo(selectedIndex.toFloat(), Motion.snappy(0.001f))
    }

    Row(
        modifier = modifier
            .onSizeChanged { widthPx = it.width }
            .liquidGlass(shape = AppShape.control, elevation = 0.dp, borderAlpha = 0.28f)
            .pointerInput(labels.size, selectedIndex) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragging.value = true
                        scope.launch { fraction.stop() }
                    },
                    onDragEnd = {
                        dragging.value = false
                        val target = fraction.value.roundToInt().coerceIn(0, labels.lastIndex)
                        scope.launch {
                            fraction.animateTo(target.toFloat(), Motion.snappy(0.001f))
                        }
                        if (target != selectedIndex) onSelected(target)
                    },
                    onDragCancel = {
                        dragging.value = false
                        scope.launch {
                            fraction.animateTo(selectedIndex.toFloat(), Motion.snappy(0.001f))
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val step = if (widthPx > 0) widthPx / labels.size.toFloat() else 1f
                        scope.launch {
                            fraction.snapTo(
                                (fraction.value + dragAmount / step).coerceIn(0f, (labels.size - 1).toFloat())
                            )
                        }
                    }
                )
            }
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val distance = kotlin.math.abs(fraction.value - index).coerceIn(0f, 1f)
            val selectedness = 1f - distance
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(AppShape.pill)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = selectedness)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelected(index) }
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selectedness > 0.5f) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedness > 0.5f) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/** Small labelled chip. */
@Composable
fun Tag(
    text: String,
    accent: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .liquidGlass(shape = AppShape.pill, elevation = 0.dp, borderAlpha = 0.3f)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(accent, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A thin progress meter with a spring-animated fill. */
@Composable
fun Meter(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = Motion.gentle(0.001f),
        label = "meter"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(6.dp)
                .background(accent, CircleShape)
        )
    }
}

/** Volume-preserving squash demo helper used by the motion playground. */
internal fun volumePreservingScale(amount: Float): Pair<Float, Float> =
    lerp(1f, 1f + amount, 0.5f) to lerp(1f, 1f - amount, 0.5f)
