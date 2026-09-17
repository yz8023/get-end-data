package com.expiryguard.app.ui.liquid.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.expiryguard.app.ui.liquid.motion.Motion
import com.expiryguard.app.ui.liquid.theme.AppShape
import kotlinx.coroutines.launch

/**
 * Hoists full-window overlays (dialogs) to the app root.
 *
 * A dialog must live in the SAME window as the glass backdrop it refracts —
 * Compose `Dialog` windows cannot sample layers recorded in the main window, so
 * glass would degrade to a tint. Screens push their dialog via [GlassDialog],
 * and [com.expiryguard.app.ui.liquid.navigation.AppRoot] renders the stack on top
 * of the tab bar.
 */
class GlassDialogHost {
    val items = mutableStateListOf<@Composable () -> Unit>()

    fun push(overlay: @Composable () -> Unit) {
        items.add(overlay)
    }

    fun remove(overlay: @Composable () -> Unit) {
        items.remove(overlay)
    }
}

val LocalGlassDialogHost = staticCompositionLocalOf { GlassDialogHost() }

/**
 * Glass dialog rendered as a same-window overlay.
 *
 * A recorded scrim layer dims the app underneath; the glass card samples the
 * aurora backdrop combined with that scrim, so it genuinely refracts and
 * frosts instead of falling back to a tint. When no backdrop is available
 * (normal-layout mode) it degrades to a solid scrim + Material card.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "确定",
    dismissText: String? = "取消",
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val host = LocalGlassDialogHost.current
    val overlay = @Composable {
        DialogOverlay(
            onDismissRequest = onDismissRequest,
            title = title,
            onConfirm = onConfirm,
            modifier = modifier,
            confirmText = confirmText,
            dismissText = dismissText,
            confirmEnabled = confirmEnabled,
            content = content
        )
    }
    DisposableEffect(host) {
        host.push(overlay)
        onDispose { host.remove(overlay) }
    }
}

@Composable
private fun DialogOverlay(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirm: () -> Unit,
    modifier: Modifier,
    confirmText: String,
    dismissText: String?,
    confirmEnabled: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onDismissRequest)

    val fullScene = LocalGlassContentBackdrop.current

    val cardScale = remember { Animatable(0.88f) }
    val cardAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { cardAlpha.animateTo(1f, tween(Motion.Duration.Fast)) }
        launch { cardScale.animateTo(1f, spring(Motion.DampedRatio(0.7f), 320f, 0.63f)) }
    }

    val scrimBackdrop = rememberLayerBackdrop { drawRect(Color.Black.copy(alpha = 0.42f)) }

    Box(Modifier.fillMaxSize()) {
        val dismissModifier = Modifier
            .matchParentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            )
        if (fullScene != null) {
            Box(dismissModifier.layerBackdrop(scrimBackdrop))
        } else {
            Box(dismissModifier.background(Color.Black.copy(alpha = 0.45f)))
        }

        val card = @Composable {
            Column(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 520.dp)
                    .graphicsLayer {
                        scaleX = cardScale.value
                        scaleY = cardScale.value
                        alpha = cardAlpha.value
                    }
                    .liquidGlass(shape = AppShape.cardLarge, elevation = 12.dp, borderAlpha = 0.5f)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                content()
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissText != null) {
                        SecondaryButton(onClick = onDismissRequest) { Text(dismissText) }
                    }
                    PrimaryButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmText) }
                }
            }
        }

        if (fullScene != null) {
            // Sample the live pages (aurora + content), NOT the scrim, so the
            // panel reads as a bright glass patch over the dimmed scene.
            CompositionLocalProvider(LocalGlassBackdrop provides fullScene) {
                Box(Modifier.align(Alignment.Center)) { card() }
            }
        } else {
            Box(Modifier.align(Alignment.Center)) { card() }
        }
    }
}
