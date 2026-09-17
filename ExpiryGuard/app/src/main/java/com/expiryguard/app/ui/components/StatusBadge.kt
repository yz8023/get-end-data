package com.expiryguard.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.ui.theme.Blue500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.ui.theme.Red700
import com.expiryguard.app.ui.theme.Yellow500

/**
 * 状态标签组件，根据 ProductStatus 显示对应的颜色和文字。
 *
 * 设计要点：左侧 2dp 色条 + 彩色圆点 + 文字，色彩适度增强但不刺眼。
 *
 * @param status 产品状态
 */
@Composable
fun StatusBadge(status: ProductStatus) {
    when (status) {
        is ProductStatus.Safe -> {
            StatusBadgeContent(
                text = "安全",
                color = Green500,
                bgAlpha = 0.15f,
                borderAlpha = 0.35f,
                dotAlpha = 0.85f
            )
        }

        is ProductStatus.ExpiringSoon -> {
            StatusBadgeContent(
                text = "即将到期",
                color = Yellow500,
                bgAlpha = 0.18f,
                borderAlpha = 0.40f,
                dotAlpha = 0.85f
            )
        }

        is ProductStatus.Returnable -> {
            StatusBadgeContent(
                text = "可退货",
                color = Blue500,
                bgAlpha = 0.15f,
                borderAlpha = 0.35f,
                dotAlpha = 0.85f
            )
        }

        is ProductStatus.Urgent -> {
            // 红色闪烁标签
            val infiniteTransition = rememberInfiniteTransition(label = "urgent_blink")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Red500.copy(alpha = 0.18f * alpha))
                    .border(
                        width = 1.dp,
                        color = Red500.copy(alpha = 0.45f * alpha),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                // 左侧色条
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Red500.copy(alpha = 0.8f * alpha))
                )
                Spacer(modifier = Modifier.width(6.dp))
                // 发光圆点
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Red500.copy(alpha = 0.4f * alpha))
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Red500.copy(alpha = 0.9f * alpha))
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "紧急 ${status.remainingDays}天",
                    color = Red500.copy(alpha = alpha),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        is ProductStatus.Expired -> {
            StatusBadgeContent(
                text = "已过期 ${status.daysOverdue}天",
                color = Red700,
                bgAlpha = 0.18f,
                borderAlpha = 0.40f,
                dotAlpha = 0.90f
            )
        }
    }
}

/**
 * 通用状态标签内容
 */
@Composable
private fun StatusBadgeContent(
    text: String,
    color: Color,
    bgAlpha: Float,
    borderAlpha: Float,
    dotAlpha: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = bgAlpha))
            .border(
                width = 1.dp,
                color = color.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        // 左侧色条 — 一眼辨别状态
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color.copy(alpha = 0.8f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        // 发光圆点（外圈 + 实心）
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.35f))
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = dotAlpha))
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            color = color.copy(alpha = 0.95f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}