package com.expiryguard.app.ui.liquid.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.expiryguard.app.ui.liquid.motion.idleBreathing

/**
 * Empty state — the default content for the "blank feature" screens.
 *
 * The illustration is drawn, not imported: concentric arcs plus a dot grid,
 * so the template carries no binary assets and the whole thing re-colours from
 * the theme. It breathes at idle so the screen never reads as broken.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(132.dp),
            contentAlignment = Alignment.Center
        ) {
            val primary = MaterialTheme.colorScheme.primary
            val onSurface = MaterialTheme.colorScheme.onSurface
            Canvas(
                modifier = Modifier
                    .size(132.dp)
                    .idleBreathing(
                        periodMs = 6400,
                        amplitudeX = 0.018f,
                        amplitudeY = 0.026f
                    )
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension / 2f
                listOf(
                    0.98f to 0.10f,
                    0.74f to 0.16f,
                    0.50f to 0.28f
                ).forEach { (scale, alpha) ->
                    drawCircle(
                        color = primary.copy(alpha = alpha),
                        radius = maxR * scale,
                        center = center,
                        style = Stroke(width = 1.5f.dp.toPx())
                    )
                }
                // Orbit dot — deliberately off-centre so the still frame has intent.
                drawCircle(
                    color = primary.copy(alpha = 0.9f),
                    radius = maxR * 0.11f,
                    center = Offset(center.x, center.y - maxR * 0.74f)
                )
                // Two ticks, asymmetric.
                drawLine(
                    color = onSurface.copy(alpha = 0.25f),
                    start = Offset(center.x - maxR * 0.5f, center.y + maxR * 0.62f),
                    end = Offset(center.x + maxR * 0.2f, center.y + maxR * 0.62f),
                    strokeWidth = 2f.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = onSurface.copy(alpha = 0.15f),
                    start = Offset(center.x - maxR * 0.5f, center.y + maxR * 0.78f),
                    end = Offset(center.x - maxR * 0.1f, center.y + maxR * 0.78f),
                    strokeWidth = 2f.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(2.dp))
            action()
        }
    }
}

/** Inline informational banner used by the component gallery. */
@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = MaterialTheme.shapes.medium, elevation = 0.dp, borderAlpha = 0.3f)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(accent, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Compact labelled metric tile, used on the home screen. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier
            .liquidGlass(shape = MaterialTheme.shapes.medium, elevation = 0.dp, borderAlpha = 0.32f)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
