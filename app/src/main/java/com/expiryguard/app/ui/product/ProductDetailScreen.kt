package com.expiryguard.app.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.ui.components.StatusBadge
import com.expiryguard.app.ui.theme.Blue500
import com.expiryguard.app.ui.theme.Gray500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.ui.theme.Yellow500
import com.expiryguard.app.util.DateUtils

/**
 * 清单详情页面
 *
 * 展示清单完整信息，包括图片、名称、分类、到期日期、状态标签、
 * 退货窗口/过期天数、备注内容，以及删除和标记已处理操作按钮。
 *
 * @param productId 清单 ID
 * @param onNavigateBack 返回上一页回调
 * @param onEdit 编辑清单回调
 * @param onDelete 删除清单回调
 * @param viewModel 清单详情 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onNavigateBack: () -> Unit = {},
    onEdit: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    // 删除成功后自动返回
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    // 删除确认对话框
    if (showDeleteDialog && pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteId = null
            },
            title = { Text("确认删除") },
            text = { Text("确定要删除此清单吗？删除后可在回收站中恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteProduct()
                        pendingDeleteId?.let { onDelete(it) }
                        pendingDeleteId = null
                    }
                ) {
                    Text("删除", color = Red500)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    pendingDeleteId = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("清单详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 编辑按钮
                    IconButton(
                        onClick = { uiState.product?.let { onEdit(it.id) } }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            // 加载中
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.product == null) {
            // 清单不存在
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "清单不存在",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Gray500
                )
            }
        } else {
            val product = uiState.product!!
            val status = uiState.status
            val category = uiState.category

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ========== 图片区域 ==========
                ProductImageSection(
                    photoPath = product.photoPath,
                    productName = product.name
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ========== 信息区域 ==========
                ProductInfoSection(
                    product = product,
                    status = status,
                    categoryName = category?.name ?: ""
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ========== 状态区域 ==========
                status?.let { productStatus ->
                    StatusSection(
                        status = productStatus,
                        expiryDate = product.expiryDate
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ========== 备注区域 ==========
                if (!product.notes.isNullOrBlank()) {
                    NotesSection(notes = product.notes)

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ========== 操作按钮区域 ==========
                ActionButtons(
                    onDelete = {
                        pendingDeleteId = product.id
                        showDeleteDialog = true
                    },
                    onMarkAsHandled = { viewModel.markAsHandled() }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 清单图片区域
 *
 * 使用 Coil AsyncImage 加载大图，点击可全屏查看（占位，实际应实现全屏查看）
 *
 * @param photoPath 图片路径
 * @param productName 清单名称
 */
@Composable
private fun ProductImageSection(
    photoPath: String?,
    productName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoPath)
                    .size(1080)
                    .build(),
                contentDescription = productName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            // 无图片时显示占位符
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无图片",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Gray500
                )
            }
        }
    }
}

/**
 * 清单信息区域
 *
 * 显示清单名称、分类、到期日期、剩余天数、状态标签
 *
 * @param product 清单实体
 * @param status 清单状态
 * @param categoryName 分类名称
 */
@Composable
private fun ProductInfoSection(
    product: ProductEntity,
    status: ProductStatus?,
    categoryName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 清单名称 + 状态标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 状态标签
                if (status != null) {
                    StatusBadge(status = status)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(12.dp))

            // 分类
            InfoRow(
                label = "分类",
                value = categoryName.ifEmpty { "未分类" }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 到期日期
            InfoRow(
                label = "到期日期",
                value = DateUtils.formatDate(product.expiryDate)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 剩余天数
            val today = DateUtils.todayTimestamp()
            val daysLeft = DateUtils.daysBetween(today, product.expiryDate)
            val daysText = if (daysLeft < 0) {
                "已过期 ${-daysLeft}天"
            } else {
                "剩余 ${daysLeft}天"
            }
            val daysColor = when {
                daysLeft < 0 -> Red500
                daysLeft <= 3 -> Yellow500
                daysLeft <= 7 -> Blue500
                else -> Green500
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "状态",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.width(80.dp)
                )
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = daysColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 生产日期（如果有）
            if (product.productionDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    label = "生产日期",
                    value = DateUtils.formatDate(product.productionDate!!)
                )
            }

            // 保质期天数（如果有）
            if (product.shelfLifeDays > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    label = "保质期",
                    value = "${product.shelfLifeDays}天"
                )
            }
        }
    }
}

/**
 * 信息行组件
 *
 * @param label 标签文字
 * @param value 值文字
 */
@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 状态区域
 *
 * 根据清单状态显示对应的提示信息：
 * - 可退货：显示退货窗口天数
 * - 已过期：显示过期天数
 * - 可下架：提醒尽快取下架
 *
 * @param status 清单状态
 * @param expiryDate 到期日期
 */
@Composable
private fun StatusSection(
    status: ProductStatus,
    expiryDate: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                is ProductStatus.Safe -> Green500.copy(alpha = 0.08f)
                is ProductStatus.ExpiringSoon -> Yellow500.copy(alpha = 0.08f)
                is ProductStatus.Returnable -> Blue500.copy(alpha = 0.08f)
                is ProductStatus.TakeDown -> Red500.copy(alpha = 0.08f)
                is ProductStatus.Expired -> Gray500.copy(alpha = 0.08f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            when (status) {
                is ProductStatus.Safe -> {
                    Text(
                        text = "清单状态良好",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Green500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "剩余 ${status.remainingDays} 天，暂不需要处理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                is ProductStatus.ExpiringSoon -> {
                    Text(
                        text = "即将到期",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Yellow500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "剩余 ${status.remainingDays} 天，建议尽快使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                is ProductStatus.Returnable -> {
                    Text(
                        text = "可退货",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Blue500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "剩余 ${status.remainingDays} 天，退货窗口为 ${status.threshold} 天，请尽快处理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                is ProductStatus.TakeDown -> {
                    Text(
                        text = "可下架",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Red500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "仅剩 ${status.remainingDays} 天，请取下架处理！",
                        style = MaterialTheme.typography.bodySmall,
                        color = Red500
                    )
                }

                is ProductStatus.Expired -> {
                    Text(
                        text = "已过期",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "已过期 ${status.daysOverdue} 天，请及时清理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * 备注区域
 *
 * @param notes 备注内容
 */
@Composable
private fun NotesSection(notes: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "备注",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 操作按钮区域
 *
 * 包含删除和标记已处理两个操作按钮
 *
 * @param onDelete 删除回调
 * @param onMarkAsHandled 标记已处理回调
 */
@Composable
private fun ActionButtons(
    onDelete: () -> Unit,
    onMarkAsHandled: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标记已处理按钮
        Button(
            onClick = onMarkAsHandled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green500
            )
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "标记已处理",
                style = MaterialTheme.typography.titleSmall
            )
        }

        // 删除按钮
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Red500
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "删除清单",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}