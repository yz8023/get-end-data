package com.expiryguard.app.ui.trash

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.ui.components.EmptyState
import com.expiryguard.app.ui.theme.Gray500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.ui.theme.Yellow500
import com.expiryguard.app.util.DateUtils

/**
 * 回收站页面，展示已删除清单并支持批量恢复和永久删除
 *
 * @param onNavigateBack 返回导航回调
 * @param viewModel 回收站页面 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val trashedProducts by viewModel.trashedProducts.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("回收站")
                        Text(
                            text = "清单将在30天后自动永久删除",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // 底部批量操作按钮，仅多选模式显示
            if (isMultiSelectMode && selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 批量恢复按钮
                    OutlinedButton(
                        onClick = { viewModel.restoreSelected() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("恢复 (${selectedIds.size})")
                    }

                    // 批量永久删除按钮
                    Button(
                        onClick = { viewModel.permanentDeleteSelected() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red500
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("永久删除 (${selectedIds.size})")
                    }
                }
            }
        }
    ) { padding ->
        if (trashedProducts.isEmpty()) {
            // 空状态显示
            EmptyState(
                message = "回收站为空",
                icon = Icons.Outlined.Delete,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            // 回收站清单列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 全选/取消全选操作栏
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (isMultiSelectMode) {
                            TextButton(
                                onClick = {
                                    if (selectedIds.size == trashedProducts.size) {
                                        viewModel.clearSelection()
                                    } else {
                                        viewModel.selectAll()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (selectedIds.size == trashedProducts.size) "取消全选" else "全选",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 清单列表
                items(
                    items = trashedProducts,
                    key = { it.id }
                ) { product ->
                    TrashProductCard(
                        product = product,
                        isSelected = selectedIds.contains(product.id),
                        isMultiSelectMode = isMultiSelectMode,
                        onClick = {
                            if (isMultiSelectMode) {
                                viewModel.toggleSelection(product.id)
                            }
                        },
                        onLongClick = {
                            if (!isMultiSelectMode) {
                                viewModel.enterMultiSelectMode(product.id)
                            }
                        }
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

/**
 * 回收站清单卡片，灰色卡片样式，显示清单名、到期日期、删除时间
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashProductCard(
    product: ProductEntity,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "card_background_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 多选模式的复选框
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // 状态颜色圆点
            val status = ExpiryRuleEngine.calculateStatus(
                product.shelfLifeDays, product.expiryDate
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(getTrashStatusColor(status))
            )
            Spacer(modifier = Modifier.width(8.dp))

            // 清单信息
            Column(modifier = Modifier.weight(1f)) {
                // 清单名称
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 到期日期
                Text(
                    text = "到期: ${DateUtils.formatDate(product.expiryDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // 删除时间
                val deleteTime = product.deletedAt?.let {
                    DateUtils.formatDate(it)
                } ?: "未知"
                Text(
                    text = "删除时间: $deleteTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // 状态标签
            Text(
                text = getTrashStatusLabel(status),
                fontSize = 12.sp,
                color = getTrashStatusColor(status),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 获取已删除清单的状态颜色（使用较淡的颜色）
 */
private fun getTrashStatusColor(status: ProductStatus): Color {
    return when (status) {
        is ProductStatus.Safe -> Green500.copy(alpha = 0.6f)
        is ProductStatus.ExpiringSoon -> Yellow500.copy(alpha = 0.6f)
        is ProductStatus.Returnable -> Color(0xFF2563EB).copy(alpha = 0.6f)
        is ProductStatus.TakeDown -> Red500.copy(alpha = 0.6f)
        is ProductStatus.Expired -> Gray500.copy(alpha = 0.6f)
    }
}

/**
 * 获取已删除清单的状态标签
 */
private fun getTrashStatusLabel(status: ProductStatus): String {
    return when (status) {
        is ProductStatus.Safe -> "安全"
        is ProductStatus.ExpiringSoon -> "即将到期"
        is ProductStatus.Returnable -> "可退货"
        is ProductStatus.TakeDown -> "可下架"
        is ProductStatus.Expired -> "已过期"
    }
}