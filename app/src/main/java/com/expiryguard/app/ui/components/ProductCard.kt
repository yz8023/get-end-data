package com.expiryguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.ui.theme.Blue500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.ui.theme.Red700
import com.expiryguard.app.ui.theme.Yellow500
import com.expiryguard.app.util.DateUtils

/**
 * 获取状态对应的颜色
 */
private fun statusToColor(status: ProductStatus): Color {
    return when (status) {
        is ProductStatus.Safe -> Green500
        is ProductStatus.ExpiringSoon -> Yellow500
        is ProductStatus.Returnable -> Blue500
        is ProductStatus.TakeDown -> Red500
        is ProductStatus.Expired -> Red700
    }
}

/**
 * 清单卡片组件，展示清单缩略图、名称、分类、到期日期、剩余天数及状态标记。
 * 左侧 4dp 色条 + StatusBadge 标签，一眼识别状态。
 *
 * @param product 清单实体
 * @param status 清单状态
 * @param categoryName 分类名称（可选）
 * @param isCompleted 是否已完成
 * @param onClick 点击卡片回调
 */
@Composable
fun ProductCard(
    product: ProductEntity,
    status: ProductStatus,
    categoryName: String = "",
    isCompleted: Boolean = false,
    onClick: () -> Unit
) {
    val statusColor = remember(status, isCompleted) {
        if (isCompleted) Green500 else statusToColor(status)
    }
    val daysLeft = remember(product.expiryDate) {
        DateUtils.daysBetween(DateUtils.todayTimestamp(), product.expiryDate)
    }

    GlassCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        statusColor = statusColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧状态色条 — 4dp 宽，与卡片等高
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(statusColor.copy(alpha = 0.8f))
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 清单缩略图
                if (product.photoPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.photoPath)
                            .size(128)
                            .build(),
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = product.name.take(1),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 清单信息区域
                Column(modifier = Modifier.weight(1f)) {
                    // 清单名称
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // 分类名称
                    Text(
                        text = categoryName.ifEmpty { "未分类" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // 到期日期
                    Text(
                        text = "到期: ${DateUtils.formatDate(product.expiryDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    // 剩余天数
                    Text(
                        text = if (daysLeft < 0) {
                            "已过期 ${-daysLeft}天"
                        } else {
                            "剩余 ${daysLeft}天"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (daysLeft < 0) Red500 else Green500
                    )
                }

                // 状态标签：已处理时显示绿色处理徽标（已下架/已退货处理/已处理）
                if (isCompleted) {
                    StatusBadgeCompleted(label = completedLabelFor(product))
                } else {
                    StatusBadge(status = status)
                }
            }
        }
    }
}

/**
 * completedType 对应的已处理标签文字
 */
private fun completedLabelFor(product: ProductEntity): String {
    return when (product.completedType) {
        ProductEntity.COMPLETED_TYPE_TAKE_DOWN -> "已下架"
        ProductEntity.COMPLETED_TYPE_RETURN -> "已退货处理"
        else -> "已处理"
    }
}

/**
 * 已处理状态标签 — 绿色"已处理/已下架/已退货处理"压过过期/可下架标记
 *
 * @param label 处理方式文字（已处理 / 已下架 / 已退货处理）
 */
@Composable
fun StatusBadgeCompleted(label: String = "已处理") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Green500.copy(alpha = 0.18f))
            .border(
                width = 1.dp,
                color = Green500.copy(alpha = 0.45f),
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
                .background(Green500.copy(alpha = 0.85f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        // 发光圆点（外圈 + 实心）
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Green500.copy(alpha = 0.4f))
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Green500.copy(alpha = 0.9f))
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = Green500.copy(alpha = 0.95f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}