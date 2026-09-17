package com.expiryguard.app.ui.product

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.ui.components.EmptyState
import com.expiryguard.app.ui.components.GlassCard
import com.expiryguard.app.ui.components.GlassCardSmall
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.components.ProductCard
import com.expiryguard.app.ui.theme.Blue500
import com.expiryguard.app.ui.theme.Gray500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Orange500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.ui.theme.Yellow500
import com.expiryguard.app.util.DateUtils

/**
 * 清单列表页面
 *
 * 支持筛选标签切换、列表/网格视图切换、多选批量操作、滑动删除、下拉刷新。
 * 毛玻璃风格设计。
 *
 * @param initialSearchQuery 初始搜索关键词（从首页传递过来）
 * @param initialFilter 初始筛选条件（从首页统计卡片传递过来）
 * @param onNavigateToAddProduct 跳转到添加清单页面
 * @param onNavigateToProductDetail 跳转到清单详情页面
 * @param onNavigateToTrash 跳转到回收站页面
 * @param onNavigateToCategoryManagement 跳转到分类管理页面
 * @param viewModel 清单列表 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductListScreen(
    initialSearchQuery: String = "",
    initialFilter: String = "",
    onNavigateToAddProduct: () -> Unit = {},
    onNavigateToProductDetail: (Long) -> Unit = {},
    onNavigateToEditProduct: (Long) -> Unit = {},
    onNavigateToTrash: () -> Unit = {},
    onNavigateToCategoryManagement: () -> Unit = {},
    viewModel: ProductListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 底部弹出层状态
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // 批量备注对话框状态
    var showBatchNotesDialog by remember { mutableStateOf(false) }
    var batchNotesText by remember { mutableStateOf("") }

    // 接收初始搜索关键词
    LaunchedEffect(initialSearchQuery) {
        if (initialSearchQuery.isNotBlank()) {
            viewModel.setSearchQuery(initialSearchQuery)
        }
    }

    // 接收初始筛选条件
    LaunchedEffect(initialFilter) {
        if (initialFilter.isNotBlank()) {
            val filter = try {
                ProductFilter.valueOf(initialFilter)
            } catch (e: Exception) {
                null
            }
            if (filter != null && filter != ProductFilter.ALL) {
                viewModel.setInitialFilter(filter)
            }
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isMultiSelectMode) {
                // 多选模式顶部栏
                MultiSelectTopBar(
                    selectedCount = uiState.selectedIds.size,
                    totalCount = uiState.filteredProducts.size,
                    onDismiss = { viewModel.exitMultiSelectMode() },
                    onSelectAll = { viewModel.toggleSelectAll() },
                    onBatchDelete = { viewModel.batchDelete() },
                    onBatchNotes = { showBatchNotesDialog = true; batchNotesText = "" }
                )
            } else {
                // 正常模式顶部栏
                TopAppBar(
                    title = { Text("清单列表") },
                    actions = {
                        // 切换视图模式按钮
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (uiState.viewMode == ViewMode.LIST) {
                                    Icons.Default.GridView
                                } else {
                                    Icons.Default.List
                                },
                                contentDescription = if (uiState.viewMode == ViewMode.LIST) {
                                    "网格视图"
                                } else {
                                    "列表视图"
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            // 添加清单 FAB
            FloatingActionButton(
                onClick = onNavigateToAddProduct,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加清单"
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 搜索栏
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // 筛选标签行
                FilterChipsRow(
                    currentFilter = uiState.currentFilter,
                    onFilterSelected = { viewModel.setFilter(it) }
                )

                // 清单列表主体
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.filteredProducts.isEmpty() && !uiState.isLoading) {
                        // 空状态（居中显示）
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                message = when {
                                    uiState.searchQuery.isNotBlank() -> "没有找到匹配的清单"
                                    uiState.currentFilter != ProductFilter.ALL -> "该分类下没有清单"
                                    else -> "还没有清单，点击右下角按钮添加"
                                },
                                actionText = if (uiState.currentFilter == ProductFilter.ALL) "添加清单" else null,
                                onAction = if (uiState.currentFilter == ProductFilter.ALL) {
                                    onNavigateToAddProduct
                                } else {
                                    null
                                }
                            )
                        }
                    } else {
                        // 根据视图模式选择布局
                        if (uiState.viewMode == ViewMode.GRID) {
                            ProductGrid(
                                products = uiState.filteredProducts,
                                selectedIds = uiState.selectedIds,
                                isMultiSelectMode = uiState.isMultiSelectMode,
                                onProductClick = { productId ->
                                    if (uiState.isMultiSelectMode) {
                                        viewModel.toggleSelection(productId)
                                    } else {
                                        selectedProduct = uiState.products.find { it.id == productId }
                                    }
                                },
                                onProductLongClick = { productId ->
                                    if (!uiState.isMultiSelectMode) {
                                        viewModel.enterMultiSelectMode()
                                        viewModel.toggleSelection(productId)
                                    }
                                }
                            )
                        } else {
                            ProductList(
                                products = uiState.filteredProducts,
                                selectedIds = uiState.selectedIds,
                                isMultiSelectMode = uiState.isMultiSelectMode,
                                onProductClick = { productId ->
                                    if (uiState.isMultiSelectMode) {
                                        viewModel.toggleSelection(productId)
                                    } else {
                                        selectedProduct = uiState.products.find { it.id == productId }
                                    }
                                },
                                onProductLongClick = { productId ->
                                    if (!uiState.isMultiSelectMode) {
                                        viewModel.enterMultiSelectMode()
                                        viewModel.toggleSelection(productId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 删除确认对话框
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    // 选中清单时展开底部弹出层
    LaunchedEffect(selectedProduct) {
        if (selectedProduct != null) {
            sheetState.expand()
        }
    }

    if (showDeleteDialog && pendingDeleteId != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDeleteId = null
            },
            title = { androidx.compose.material3.Text("确认删除") },
            text = { androidx.compose.material3.Text("确定要删除此清单吗？删除后可在回收站中恢复。") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteDialog = false
                        pendingDeleteId?.let { id ->
                            viewModel.swipeDelete(id)
                        }
                        pendingDeleteId = null
                        selectedProduct = null
                    }
                ) {
                    Text("删除", color = Red500)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteDialog = false
                    pendingDeleteId = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 底部弹出层：清单详情
    if (selectedProduct != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val product = selectedProduct!!
            val status = remember { ExpiryRuleEngine.calculateStatus(product.shelfLifeDays, product.expiryDate) }
            val today = remember { DateUtils.todayTimestamp() }
            val daysLeft = remember { DateUtils.daysBetween(today, product.expiryDate) }
            val daysText = when {
                daysLeft < 0 -> "已过期 ${-daysLeft}天"
                daysLeft == 0 -> "今天到期"
                daysLeft == 1 -> "明天到期（预警）"
                else -> "剩余 ${daysLeft}天"
            }
            val daysColor = when {
                daysLeft < 0 -> Red500
                daysLeft <= 1 -> Orange500
                daysLeft <= 3 -> Yellow500
                else -> Green500
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                // 拖拽指示条
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 清单名称 + 状态标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (product.isCompleted) {
                        // 已处理标签：已下架 / 已退货处理 / 已处理
                        com.expiryguard.app.ui.components.StatusBadgeCompleted(
                            label = completedLabelFor(product)
                        )
                    } else {
                        com.expiryguard.app.ui.components.StatusBadge(status = status)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // 到期日期
                DetailInfoRow("到期日期", DateUtils.formatDate(product.expiryDate))
                Spacer(modifier = Modifier.height(10.dp))
                DetailInfoRow("状态", daysText, daysColor)

                if (product.productionDate != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailInfoRow("生产日期", DateUtils.formatDate(product.productionDate!!))
                }
                if (product.shelfLifeDays > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailInfoRow("保质期", "${product.shelfLifeDays}天")
                }

                // 备注
                if (!product.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "备注",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 标记完成/取消完成
                Button(
                    onClick = {
                        viewModel.swipeComplete(product.id)
                        selectedProduct = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (product.isCompleted) Gray500 else Green500
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (product.isCompleted) "取消完成" else "标记已处理",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 操作按钮行：编辑 | 删除 | 查看详情
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 编辑按钮
                    OutlinedButton(
                        onClick = {
                            val id = product.id
                            selectedProduct = null
                            onNavigateToEditProduct(id)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Blue500
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("编辑", style = MaterialTheme.typography.labelMedium)
                    }

                    // 删除按钮
                    OutlinedButton(
                        onClick = {
                            pendingDeleteId = product.id
                            showDeleteDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Red500
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 查看完整详情按钮
                TextButton(
                    onClick = {
                        val id = product.id
                        selectedProduct = null
                        onNavigateToProductDetail(id)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "查看完整详情",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // 批量备注对话框
    if (showBatchNotesDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBatchNotesDialog = false },
            title = { androidx.compose.material3.Text("批量备注") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = batchNotesText,
                    onValueChange = { batchNotesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { androidx.compose.material3.Text("输入备注内容...") },
                    minLines = 3,
                    maxLines = 5
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (batchNotesText.isNotBlank()) {
                            viewModel.batchUpdateNotes(batchNotesText)
                        }
                        showBatchNotesDialog = false
                    }
                ) {
                    androidx.compose.material3.Text("保存")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showBatchNotesDialog = false }
                ) {
                    androidx.compose.material3.Text("取消")
                }
            }
        )
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

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {    Row(
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
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (valueColor != MaterialTheme.colorScheme.onSurface) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * 搜索栏组件
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(22.dp)
            )
            // 内嵌搜索输入框
            androidx.compose.material3.TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "搜索清单名称",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                singleLine = true,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清除搜索",
                        tint = Gray500
                    )
                }
            }
        }
    }
}

/**
 * 筛选标签行
 */
@Composable
private fun FilterChipsRow(
    currentFilter: ProductFilter,
    onFilterSelected: (ProductFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ProductFilter.values()) { filter ->
            val isSelected = filter == currentFilter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.label,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (filter) {
                        ProductFilter.ALL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ProductFilter.SAFE -> Green500.copy(alpha = 0.15f)
                        ProductFilter.EXPIRING_SOON -> Yellow500.copy(alpha = 0.15f)
                        ProductFilter.RETURNABLE -> Blue500.copy(alpha = 0.15f)
                        ProductFilter.TAKEDOWN -> Red500.copy(alpha = 0.15f)
                        ProductFilter.EXPIRED -> Gray500.copy(alpha = 0.15f)
                    }
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = Color.Transparent,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

/**
 * 多选模式顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectTopBar(
    selectedCount: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onBatchDelete: () -> Unit,
    onBatchNotes: () -> Unit
) {
    TopAppBar(
        title = {
            Text("已选 $selectedCount 项")
        },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消多选"
                )
            }
        },
        actions = {
            TextButton(onClick = onSelectAll) {
                Text(
                    text = if (selectedCount == totalCount) "取消全选" else "全选",
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onBatchDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "批量删除",
                    tint = Red500
                )
            }
            IconButton(onClick = onBatchNotes) {
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = "批量备注"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    )
}

/**
 * 列表模式
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductList(
    products: List<ProductEntity>,
    selectedIds: Set<Long>,
    isMultiSelectMode: Boolean,
    onProductClick: (Long) -> Unit,
    onProductLongClick: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(products, key = { it.id }) { product ->
            val status = ExpiryRuleEngine.calculateStatus(
                product.shelfLifeDays,
                product.expiryDate
            )
            val isSelected = product.id in selectedIds

            if (isMultiSelectMode) {
                ProductCardWithSelection(
                    product = product,
                    status = status,
                    isSelected = isSelected,
                    isCompleted = product.isCompleted,
                    onClick = { onProductClick(product.id) },
                    onLongClick = { onProductLongClick(product.id) }
                )
            } else {
                ProductCard(
                    product = product,
                    status = status,
                    isCompleted = product.isCompleted,
                    onClick = { onProductClick(product.id) }
                )
            }
        }
    }
}

/**
 * 网格模式
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductGrid(
    products: List<ProductEntity>,
    selectedIds: Set<Long>,
    isMultiSelectMode: Boolean,
    onProductClick: (Long) -> Unit,
    onProductLongClick: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(products, key = { it.id }) { product ->
            val status = ExpiryRuleEngine.calculateStatus(
                product.shelfLifeDays,
                product.expiryDate
            )
            val isSelected = product.id in selectedIds

            GridProductCard(
                product = product,
                status = status,
                isSelected = isSelected,
                isMultiSelectMode = isMultiSelectMode,
                onClick = { onProductClick(product.id) },
                onLongClick = { onProductLongClick(product.id) }
            )
        }
    }
}

/**
 * 带选中指示器的清单卡片（列表模式）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductCardWithSelection(
    product: ProductEntity,
    status: ProductStatus,
    isSelected: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        ProductCard(
            product = product,
            status = status,
            isCompleted = isCompleted,
            onClick = onClick
        )

        // 选中状态指示器（右上角）
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp, top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 网格清单卡片
 *
 * 毛玻璃风格，适合网格布局，带有状态色条和选中指示器
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridProductCard(
    product: ProductEntity,
    status: ProductStatus,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val today = remember { DateUtils.todayTimestamp() }
    val daysLeft = remember(today, product.expiryDate) {
        DateUtils.daysBetween(today, product.expiryDate)
    }
    val daysText = remember(daysLeft) {
        if (daysLeft < 0) "已过期 ${-daysLeft}天" else "剩余 ${daysLeft}天"
    }
    val onSurfaceHint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val daysColor = remember(daysLeft, onSurfaceHint) {
        when {
            daysLeft < 0 -> Red500
            daysLeft <= 3 -> Yellow500
            else -> onSurfaceHint
        }
    }

    val statusColor = remember(status, product.isCompleted) {
        if (product.isCompleted) Green500 else when (status) {
            is ProductStatus.Safe -> Green500
            is ProductStatus.ExpiringSoon -> Yellow500
            is ProductStatus.Returnable -> Blue500
            is ProductStatus.TakeDown -> Red500
            is ProductStatus.Expired -> Gray500
        }
    }

    GlassCardSmall(
        onClick = onClick,
        selected = isSelected,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 状态色条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(statusColor)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 清单名称
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 到期日期
                Text(
                    text = DateUtils.formatDate(product.expiryDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 剩余天数
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.labelSmall,
                    color = daysColor,
                    fontWeight = FontWeight.SemiBold
                )

                // 已处理标记
                if (product.isCompleted) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "✓ 已处理",
                        style = MaterialTheme.typography.labelSmall,
                        color = Green500,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 选中指示器
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "已选中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                )
            }
        }
    }
}