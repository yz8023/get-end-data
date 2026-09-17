package com.expiryguard.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.ui.components.GlassCard
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.theme.Blue500
import com.expiryguard.app.ui.theme.Gray500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.ui.theme.Yellow500
import com.expiryguard.app.util.DateUtils

/** 已下架标记颜色（深红） */
private val TakeDownColor = Color(0xFFB91C1C)

/** 已处理标记颜色（深绿） */
private val ProcessedColor = Color(0xFF15803D)

/**
 * 获取产品状态对应的颜色
 */
private fun getStatusColor(status: ProductStatus): Color {
    return when (status) {
        is ProductStatus.Safe -> Green500
        is ProductStatus.ExpiringSoon -> Yellow500
        is ProductStatus.Returnable -> Blue500
        is ProductStatus.TakeDown -> Red500
        is ProductStatus.Expired -> Red500
    }
}

/**
 * 获取产品状态对应的中文标签
 */
private fun getStatusLabel(status: ProductStatus): String {
    return when (status) {
        is ProductStatus.Safe -> "安全"
        is ProductStatus.ExpiringSoon -> "即将到期"
        is ProductStatus.Returnable -> "可退货"
        is ProductStatus.TakeDown -> "可下架"
        is ProductStatus.Expired -> "已过期"
    }
}

/**
 * 统计页面，展示产品状态统计数据和图表
 *
 * 毛玻璃风格，增强的 GlassCard 组件，包含概览卡片、状态分布横条、饼图、柱状图和清单详情列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigate: (String) -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            GradientBackground {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 概览区域：4 个 StatCard 横向排列，每行 2 个
                    item {
                        Text(
                            text = "概览",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassStatCard(
                                label = "总数",
                                value = uiState.totalCount.toString(),
                                color = Blue500,
                                statusColor = Blue500,
                                modifier = Modifier.weight(1f)
                            )
                            GlassStatCard(
                                label = "即将到期",
                                value = uiState.expiringCount.toString(),
                                color = Yellow500,
                                statusColor = Yellow500,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassStatCard(
                                label = "可退货",
                                value = uiState.returnableCount.toString(),
                                color = Blue500,
                                statusColor = Blue500,
                                modifier = Modifier.weight(1f)
                            )
                            GlassStatCard(
                                label = "已过期",
                                value = uiState.expiredCount.toString(),
                                color = Red500,
                                statusColor = Red500,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassStatCard(
                                label = "已下架",
                                value = uiState.takeDownCount.toString(),
                                color = Red500,
                                statusColor = Red500,
                                modifier = Modifier.weight(1f)
                            )
                            GlassStatCard(
                                label = "已处理",
                                value = uiState.processedCount.toString(),
                                color = Green500,
                                statusColor = Green500,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 状态分布横条
                    item {
                        Text(
                            text = "状态分布",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        StatusDistributionBar(
                            safeCount = uiState.safeCount,
                            expiringCount = uiState.expiringCount,
                            returnableCount = uiState.returnableCount,
                            expiredCount = uiState.expiredCount,
                            takeDownCount = uiState.takeDownCount,
                            processedCount = uiState.processedCount,
                            totalCount = uiState.totalCount
                        )
                    }

                    // 饼图：环形图（Donut Chart）
                    item {
                        Text(
                            text = "状态占比",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        DonutChart(
                            safeCount = uiState.safeCount,
                            expiringCount = uiState.expiringCount,
                            returnableCount = uiState.returnableCount,
                            expiredCount = uiState.expiredCount,
                            takeDownCount = uiState.takeDownCount,
                            processedCount = uiState.processedCount,
                            totalCount = uiState.totalCount
                        )
                    }

                    // 柱状图：展示每月到期分布
                    item {
                        Text(
                            text = "每月到期分布",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        BarChart(
                            products = uiState.products
                        )
                    }

                    // 清单详情列表
                    item {
                        Text(
                            text = "清单详情",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(uiState.products) { product ->
                        ProductDetailRow(
                            product = product,
                            status = ExpiryRuleEngine.calculateStatus(
                                product.shelfLifeDays, product.expiryDate
                            )
                        )
                    }

                    // 底部留白
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * 毛玻璃风格的统计卡片（增强版）
 *
 * 使用 GlassCard 带 statusColor 参数，显示彩色边框光晕
 */
@Composable
private fun GlassStatCard(
    label: String,
    value: String,
    color: Color,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        statusColor = statusColor
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 状态分布横条
 *
 * 用彩色横条直观展示各状态的比例，使用 GlassCard 带 statusColor
 */
@Composable
private fun StatusDistributionBar(
    safeCount: Int,
    expiringCount: Int,
    returnableCount: Int,
    expiredCount: Int,
    takeDownCount: Int,
    processedCount: Int,
    totalCount: Int
) {
    val total = totalCount.coerceAtLeast(1)

    GlassCard(
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        statusColor = Blue500.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 彩色横条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val safeRatio = safeCount.toFloat() / total
                    val expiringRatio = expiringCount.toFloat() / total
                    val returnableRatio = returnableCount.toFloat() / total
                    val expiredRatio = expiredCount.toFloat() / total
                    val takeDownRatio = takeDownCount.toFloat() / total
                    val processedRatio = processedCount.toFloat() / total

                    if (safeRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(safeRatio)
                                .fillMaxSize()
                                .background(Green500)
                        )
                    }
                    if (expiringRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(expiringRatio)
                                .fillMaxSize()
                                .background(Yellow500)
                        )
                    }
                    if (returnableRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(returnableRatio)
                                .fillMaxSize()
                                .background(Blue500)
                        )
                    }
                    if (expiredRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(expiredRatio)
                                .fillMaxSize()
                                .background(Red500)
                        )
                    }
                    if (takeDownRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(takeDownRatio)
                                .fillMaxSize()
                                .background(TakeDownColor)
                        )
                    }
                    if (processedRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(processedRatio)
                                .fillMaxSize()
                                .background(ProcessedColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 图例（两行，每行 3 个）
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = Green500, label = "安全", count = safeCount)
                    LegendItem(color = Yellow500, label = "即将到期", count = expiringCount)
                    LegendItem(color = Blue500, label = "可退货", count = returnableCount)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = Red500, label = "已过期", count = expiredCount)
                    LegendItem(color = TakeDownColor, label = "已下架", count = takeDownCount)
                    LegendItem(color = ProcessedColor, label = "已处理", count = processedCount)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

/**
 * 环形图（Donut Chart）组件
 *
 * 使用 Canvas drawArc 绘制各状态占比，中间显示总数
 */
@Composable
private fun DonutChart(
    safeCount: Int,
    expiringCount: Int,
    returnableCount: Int,
    expiredCount: Int,
    takeDownCount: Int,
    processedCount: Int,
    totalCount: Int
) {
    val total = totalCount.coerceAtLeast(1)

    val pieData = listOf(
        PieSlice("安全", safeCount, Green500),
        PieSlice("即将到期", expiringCount, Yellow500),
        PieSlice("可退货", returnableCount, Blue500),
        PieSlice("已过期", expiredCount, Red500),
        PieSlice("已下架", takeDownCount, TakeDownColor),
        PieSlice("已处理", processedCount, ProcessedColor)
    )

    GlassCard(
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        statusColor = Blue500.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 环形图
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val diameter = minOf(canvasWidth, canvasHeight) * 0.85f
                    val topLeft = Offset(
                        (canvasWidth - diameter) / 2f,
                        (canvasHeight - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)
                    val strokeWidth = diameter * 0.22f
                    var startAngle = -90f

                    pieData.forEach { slice ->
                        val sweepAngle = (slice.count.toFloat() / total) * 360f
                        if (sweepAngle > 0f) {
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    if (total == 0) {
                        drawArc(
                            color = Color(0xFFE5E7EB),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                    }
                }

                // 中间显示总数
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalCount.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "总计",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 图例（垂直排列）
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pieData.forEach { slice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${slice.label}: ${slice.count}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * 饼图数据条目
 */
private data class PieSlice(
    val label: String,
    val count: Int,
    val color: Color
)

/**
 * 柱状图组件，使用 Canvas drawRect 绘制每月到期分布
 */
@Composable
private fun BarChart(
    products: List<ProductEntity>
) {
    // 按月份分组统计到期数量
    val monthCountMap = mutableMapOf<String, Int>()
    products.forEach { product ->
        val month = DateUtils.formatDate(product.expiryDate).substring(0, 7)
        monthCountMap[month] = (monthCountMap[month] ?: 0) + 1
    }

    val sortedMonths = monthCountMap.entries.sortedBy { it.key }
    val maxCount = sortedMonths.maxOfOrNull { it.value } ?: 1

    GlassCard(
        shape = RoundedCornerShape(16.dp),
        elevation = 2.dp,
        statusColor = Blue500.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (sortedMonths.isEmpty()) {
                Text(
                    text = "暂无到期数据",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barCount = sortedMonths.size
                    val barSpacing = 12.dp.toPx()
                    val totalSpacing = barSpacing * (barCount + 1)
                    val barWidth = (canvasWidth - totalSpacing) / barCount.coerceAtLeast(1)
                    val chartHeight = canvasHeight - 40.dp.toPx()
                    val cornerRadius = 4.dp.toPx()

                    sortedMonths.forEachIndexed { index, entry ->
                        val barHeight = (entry.value.toFloat() / maxCount) * chartHeight
                        val x = barSpacing + index * (barWidth + barSpacing)
                        val y = canvasHeight - 40.dp.toPx() - barHeight

                        // 绘制圆角柱子（渐变色彩）
                        drawRoundRect(
                            color = Blue500.copy(alpha = 0.8f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )

                        // 在柱子上方显示数值
                        drawContext.canvas.nativeCanvas.drawText(
                            entry.value.toString(),
                            x + barWidth / 2f,
                            y - 6.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 24f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )

                        // 在底部显示月份标签
                        val monthLabel = entry.key.substring(5)
                        drawContext.canvas.nativeCanvas.drawText(
                            monthLabel,
                            x + barWidth / 2f,
                            canvasHeight - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 22f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 清单详情行，毛玻璃风格（增强版）
 *
 * 使用 GlassCard 带 statusColor 参数，显示状态色边框光晕
 * 可退货清单额外显示"剩余退货时间"和"距离到期还有"
 */
@Composable
private fun ProductDetailRow(
    product: ProductEntity,
    status: ProductStatus
) {
    // 已完成清单显示已下架/已处理标记
    val isCompleted = product.isCompleted
    val completedColor = when {
        isCompleted && product.completedType == ProductEntity.COMPLETED_TYPE_TAKE_DOWN -> TakeDownColor
        isCompleted -> ProcessedColor
        else -> getStatusColor(status)
    }
    val statusColor = completedColor

    GlassCard(
        shape = RoundedCornerShape(12.dp),
        elevation = 1.dp,
        statusColor = statusColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态颜色标记（带发光效果）
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 清单名称
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 到期日期
            Text(
                text = DateUtils.formatDate(product.expiryDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 状态标签（已完成显示已下架/已处理，可退货显示退货时间信息）
            when {
                isCompleted -> {
                    val label = if (product.completedType == ProductEntity.COMPLETED_TYPE_TAKE_DOWN) {
                        "已下架"
                    } else {
                        "已处理"
                    }
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                status is ProductStatus.Returnable -> {
                    val returnDeadlineDays = (status.remainingDays - status.threshold).coerceAtLeast(0)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "可退货",
                            fontSize = 12.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "剩余退货${returnDeadlineDays}天",
                            fontSize = 10.sp,
                            color = statusColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "到期还有${status.remainingDays}天",
                            fontSize = 10.sp,
                            color = statusColor.copy(alpha = 0.7f)
                        )
                    }
                }
                else -> {
                    Text(
                        text = getStatusLabel(status),
                        fontSize = 12.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}