package com.expiryguard.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.ui.components.EmptyState
import com.expiryguard.app.ui.components.GlassCard
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.components.StatusBadge
import com.expiryguard.app.ui.theme.Blue500
import com.expiryguard.app.ui.theme.Gray500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Orange500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.ui.theme.Red700
import com.expiryguard.app.ui.theme.Yellow500
import com.expiryguard.app.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * 获取产品状态对应的颜色
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
 * 根据 ProductEntity 计算状态颜色
 */
private fun productStatusColor(product: ProductEntity): Color {
    val status = ExpiryRuleEngine.calculateStatus(product.shelfLifeDays, product.expiryDate)
    return statusToColor(status)
}

/**
 * 首页可组合函数
 *
 * 简洁版：仅展示日期问候语、搜索栏和今日待办任务列表。
 * 点击任务卡片通过底部弹出层查看详情，不再跳转独立页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddProduct: () -> Unit = {},
    onNavigateToProductDetail: (Long) -> Unit = {},
    onNavigateToProductList: (String, String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Snackbar 用于标记完成/取消完成后提示，并提供"撤销"避免误触误标
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 底部弹出层状态
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // 选中清单时展开底部弹出层
    LaunchedEffect(selectedProduct) {
        if (selectedProduct != null) {
            sheetState.expand()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing.collectAsState().value,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoading && uiState.totalCount == 0) {
                // 首次加载：显示加载指示器，避免白屏/空状态闪烁
                GradientBackground {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Green500)
                    }
                }
            } else if (uiState.totalCount == 0 && !uiState.isLoading) {
                GradientBackground {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            message = "还没有清单，点击下方按钮添加第一个清单",
                            icon = Icons.Outlined.AddCircleOutline,
                            actionText = "添加清单",
                            onAction = onNavigateToAddProduct
                        )
                    }
                }
            } else {
                GradientBackground {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 顶部：日期 + 问候语
                        item { GreetingSection() }

                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        // 搜索栏
                        item {
                            SearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onSearch = {
                                    onNavigateToProductList(searchQuery, "ALL")
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(12.dp)) }

                        // 今日待办任务列表 —— 今天到期 + 还剩1天的预警清单
                        todayTasksSection(
                            todayExpiry = uiState.todayExpiry,
                            warning = uiState.warning,
                            takeDownCompleted = uiState.takeDownCompleted,
                            processedCompleted = uiState.processedCompleted,
                             onTaskCompleted = { product, isCompleted ->
                                 val type = if (isCompleted) ProductEntity.COMPLETED_TYPE_TAKE_DOWN else null
                                 viewModel.toggleProductCompletion(product.id, isCompleted, type)
                                 // 滑动标记静默完成，不再弹出底部提示（避免频繁弹窗碍眼）
                             },
                             onTaskLongPress = { product ->
                                 val wasCompleted = product.isCompleted
                                 val wasType = product.completedType
                                 val newType = toggledCompletionType(product)
                                 viewModel.toggleProductCompletion(product.id, true, newType)
                                 showCompletionSnackbar(
                                     scope = scope,
                                     snackbarHostState = snackbarHostState,
                                     product = product,
                                     isCompleted = true,
                                     markLabel = completionLabelFor(newType),
                                     onUndo = {
                                         viewModel.toggleProductCompletion(product.id, wasCompleted, wasType)
                                     },
                                     isLongPress = true
                                 )
                             },
                             onTaskClick = { product ->
                                 selectedProduct = product
                             }
                        )
                    }
                }
            }
        }

        // Snackbar 提示层，覆盖在界面顶部，不干扰列表布局
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
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
            ProductDetailSheet(
                product = selectedProduct!!,
                onMarkComplete = {
                    val target = selectedProduct!!
                    val targetCompleted = !target.isCompleted
                    viewModel.toggleProductCompletion(target.id, targetCompleted, null)
                    showCompletionSnackbar(
                        scope = scope,
                        snackbarHostState = snackbarHostState,
                        product = target,
                        isCompleted = targetCompleted,
                        markLabel = if (targetCompleted) "已处理" else null,
                        onUndo = {
                            viewModel.toggleProductCompletion(target.id, false, null)
                        }
                    )
                    selectedProduct = null
                },
                onNavigateToDetail = {
                    onNavigateToProductDetail(selectedProduct!!.id)
                    selectedProduct = null
                },
                onDismiss = { selectedProduct = null }
            )
        }
    }
}

/**
 * 展示标记完成/取消完成的提示，并提供撤销入口，防止误触
 *
 * 长按与详情按钮标记「已处理」，滑动标记「已下架」
 */
private fun showCompletionSnackbar(
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    product: ProductEntity,
    isCompleted: Boolean,
    onUndo: () -> Unit,
    isLongPress: Boolean = false,
    markLabel: String? = null
) {
    scope.launch {
        val actionLabel = markLabel ?: "已处理"
        val triggerText = if (isLongPress) "长按" else "滑动"
        val message = if (isCompleted) {
            "已「$triggerText」标记「${product.name}」$actionLabel"
        } else {
            "已取消「${product.name}」的标记"
        }
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (isCompleted) "撤销" else null,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            onUndo()
        }
    }
}

/**
 * 长按切换后的 completedType：
 * 长按标记为「已处理」；已标记「下架」→「已处理」；已标记「已处理」→「下架」
 */
private fun toggledCompletionType(product: ProductEntity): String? {
    if (product.isCompleted) {
        // 长按只在「已下架」与「已处理」两种标记间切换
        return when (product.completedType) {
            ProductEntity.COMPLETED_TYPE_TAKE_DOWN -> null
            else -> ProductEntity.COMPLETED_TYPE_TAKE_DOWN
        }
    }
    // 未标记时长按直接标记为「已处理」
    return null
}

/**
 * completedType 对应的已处理标签文字
 */
private fun completionLabelFor(type: String?): String {
    return when (type) {
        ProductEntity.COMPLETED_TYPE_TAKE_DOWN -> "已下架"
        ProductEntity.COMPLETED_TYPE_RETURN -> "已退货处理"
        else -> "已处理"
    }
}

/**
 * 已完成标签文字（用于绿色已处理徽标）
 * 优先读取 completedType：已下架 / 已退货处理 / 已处理
 */
private fun completedLabelFor(product: ProductEntity): String {
    return completionLabelFor(product.completedType)
}

/**
 * 顶部问候语区域
 */
@Composable
private fun GreetingSection() {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
    val dateString = today.format(formatter)

    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..5 -> "夜深了"
        in 6..11 -> "早上好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        else -> "晚上好"
    }

    Column {
        // 装饰性顶部渐变条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Blue500.copy(alpha = 0.6f),
                            Green500.copy(alpha = 0.6f),
                            Yellow500.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 装饰性圆形指示器
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Blue500.copy(alpha = 0.15f),
                                Green500.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = Blue500.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Green500.copy(alpha = 0.8f))
                )
            }
        }
    }
}

/**
 * 搜索栏组件
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    GlassCard(
        onClick = onSearch,
        shape = RoundedCornerShape(14.dp),
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Blue500.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = Blue500.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (query.isNotBlank()) query else "搜索清单名称或备注",
                style = MaterialTheme.typography.bodyLarge,
                color = if (query.isNotBlank())
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * 已完成分组最多渲染的条数。
 * 首页仅展示最近完成的若干条，避免大量历史已完成卡片（SwipeToDismissBox + GlassCard + AsyncImage）
 * 同时渲染导致卡顿；完整历史仍可在统计页/清单页查看。
 */
private const val MAX_COMPLETED_ITEMS_PER_GROUP = 20

/**
 * 今日待办区域 —— 显示今天到期 + 还剩1天的预警清单
 *
 * 支持左右滑动切换完成状态（左右方向均可标记/取消标记）。
 * 已处理区域按标记类型拆分为「已下架」与「已处理」两组，便于快速查看。
 * 作为 LazyListScope 扩展函数在 LazyColumn 中懒加载，避免一次性渲染全部卡片。
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.todayTasksSection(
    todayExpiry: List<ProductEntity>,
    warning: List<ProductEntity>,
    takeDownCompleted: List<ProductEntity>,
    processedCompleted: List<ProductEntity>,
    onTaskCompleted: (ProductEntity, Boolean) -> Unit,
    onTaskClick: (ProductEntity) -> Unit,
    onTaskLongPress: (ProductEntity) -> Unit
) {
    // 合并今日到期 + 预警（还剩1天），按到期日期排序
    val todayTasks = (todayExpiry + warning)
        .distinctBy { it.id }
        .sortedBy { it.expiryDate }

    // 今日待办标题行
    item(key = "today_header") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日待办",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (todayTasks.isNotEmpty()) {
                val unfinishedCount = todayTasks.count { !it.isCompleted }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (unfinishedCount > 0) Red500.copy(alpha = 0.12f) else Green500.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$unfinishedCount 项未处理",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unfinishedCount > 0) Red500 else Green500,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (todayTasks.isEmpty() && takeDownCompleted.isEmpty() && processedCompleted.isEmpty()) {
        // 空状态
        item(key = "today_empty") {
            GlassCard(
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp
            ) {
                Text(
                    text = "今天没有到期清单",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    } else {
        // 今天到期的未处理清单 + 预警（懒加载，仅渲染可见项）
        val unfinished = todayTasks.filter { !it.isCompleted }
        items(
            items = unfinished,
            key = { it.id }
        ) { product ->
            TaskCard(
                product = product,
                isCompleted = false,
                onSwipeComplete = {
                    onTaskCompleted(product, true)
                },
                onLongPress = { onTaskLongPress(product) },
                onClick = { onTaskClick(product) }
            )
        }

        // 已下架分组
        if (takeDownCompleted.isNotEmpty()) {
            item(key = "take_down_completed_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已下架",
                        style = MaterialTheme.typography.titleSmall,
                        color = Red500,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${takeDownCompleted.size} 项",
                        style = MaterialTheme.typography.labelSmall,
                        color = Red500.copy(alpha = 0.7f)
                    )
                }
            }
            items(
                items = takeDownCompleted.take(MAX_COMPLETED_ITEMS_PER_GROUP),
                key = { it.id }
            ) { product ->
                TaskCard(
                    product = product,
                    isCompleted = true,
                    onSwipeComplete = {
                        onTaskCompleted(product, false)
                    },
                    onLongPress = { onTaskLongPress(product) },
                    onClick = { onTaskClick(product) }
                )
            }
        }

        // 已处理分组
        if (processedCompleted.isNotEmpty()) {
            item(key = "processed_completed_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已处理",
                        style = MaterialTheme.typography.titleSmall,
                        color = Green500,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${processedCompleted.size} 项",
                        style = MaterialTheme.typography.labelSmall,
                        color = Green500.copy(alpha = 0.7f)
                    )
                }
            }
            items(
                items = processedCompleted.take(MAX_COMPLETED_ITEMS_PER_GROUP),
                key = { it.id }
            ) { product ->
                TaskCard(
                    product = product,
                    isCompleted = true,
                    onSwipeComplete = {
                        onTaskCompleted(product, false)
                    },
                    onLongPress = { onTaskLongPress(product) },
                    onClick = { onTaskClick(product) }
                )
            }
        }
    }
}

/**
 * 待办任务卡片
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(
    product: ProductEntity,
    isCompleted: Boolean,
    onSwipeComplete: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val productStatus = remember { ExpiryRuleEngine.calculateStatus(product.shelfLifeDays, product.expiryDate) }
    // 已处理时用绿色压过原始状态色
    val statusColor = if (isCompleted) Green500 else statusToColor(productStatus)

    // 滑动动作提示：滑动一律标记下架
    val swipeLabel = "滑动标记下架"

    // 滑动阈值：需滑动超过卡片宽度的 85% 才触发，增加阻尼感，避免轻滑误触
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                // 返回 false 防止卡片完全滑出（只触发动作，卡片回弹原位）
                onSwipeComplete()
            }
            false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.85f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val backgroundBrush = if (isCompleted) {
                Brush.linearGradient(listOf(Gray500.copy(alpha = 0.5f), Gray500.copy(alpha = 0.3f)))
            } else {
                Brush.linearGradient(listOf(Green500.copy(alpha = 0.5f), Green500.copy(alpha = 0.3f)))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundBrush)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isCompleted) Arrangement.End else Arrangement.Start
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.Close else Icons.Default.CheckCircle,
                        contentDescription = if (isCompleted) "取消完成" else "标记完成",
                        tint = if (isCompleted) Gray500 else Green500,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCompleted) "滑动取消完成" else swipeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCompleted) Gray500 else Green500,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (!isCompleted) {
                        Text(
                            text = "· 长按切换标记",
                            style = MaterialTheme.typography.labelSmall,
                            color = Green500.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongPress
                    )
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
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

                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 清单图片
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

                            Spacer(modifier = Modifier.width(12.dp))

                            // 清单信息
                            Column(modifier = Modifier.weight(1f)) {
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

                                Spacer(modifier = Modifier.height(4.dp))

                                val today = remember { DateUtils.todayTimestamp() }
                                val daysLeft = remember(today, product.expiryDate) {
                                    DateUtils.daysBetween(today, product.expiryDate)
                                }
                                val daysText = when {
                                    productStatus is ProductStatus.TakeDown -> {
                                        val t = productStatus as ProductStatus.TakeDown
                                        "可下架 · 到期还有${t.remainingDays}天"
                                    }
                                    productStatus is ProductStatus.Returnable -> {
                                        val r = productStatus as ProductStatus.Returnable
                                        val returnDeadlineDays = (r.remainingDays - r.threshold).coerceAtLeast(0)
                                        if (returnDeadlineDays > 0) {
                                            "可退货 · 剩余退货${returnDeadlineDays}天 · 到期还有${r.remainingDays}天"
                                        } else {
                                            "可退货 · 到期还有${r.remainingDays}天"
                                        }
                                    }
                                    daysLeft < 0 -> "已过期 ${-daysLeft}天"
                                    daysLeft == 0 -> "今天到期"
                                    daysLeft == 1 -> "明天到期（预警）"
                                    else -> "剩余 ${daysLeft}天"
                                }
                                val daysColor = when {
                                    productStatus is ProductStatus.TakeDown -> Red500
                                    productStatus is ProductStatus.Returnable -> Blue500
                                    daysLeft < 0 -> Red500
                                    daysLeft <= 1 -> Orange500
                                    daysLeft <= 3 -> Yellow500
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }

                                Text(
                                    text = "到期: ${DateUtils.formatDate(product.expiryDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = daysText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = daysColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // 状态标签：已处理时显示绿色处理徽标（下架/退货处理/已处理），否则显示原始状态
                            if (isCompleted) {
                                StatusBadgeCompleted(label = completedLabelFor(product))
                            } else {
                                StatusBadge(status = productStatus)
                            }
                        }

                        // 完成标记
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "已完成",
                                tint = Green500,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
)
}

/**
 * 底部弹出层：清单详情
 *
 * 展示清单完整信息，支持标记完成/取消完成、查看完整详情、删除操作。
 * 不跳转新页面，在当前页面以弹出层形式呈现。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailSheet(
    product: ProductEntity,
    onMarkComplete: () -> Unit,
    onNavigateToDetail: () -> Unit,
    onDismiss: () -> Unit
) {
    val status = remember { ExpiryRuleEngine.calculateStatus(product.shelfLifeDays, product.expiryDate) }
    val today = remember { DateUtils.todayTimestamp() }
    val daysLeft = remember { DateUtils.daysBetween(today, product.expiryDate) }

    var showFullImage by remember { mutableStateOf(false) }

    val daysText = when {
        daysLeft < 0 -> "已过期 ${-daysLeft}天"
        daysLeft == 0 -> "今天到期"
        status is ProductStatus.TakeDown -> "可下架 · 到期还有${status.remainingDays}天"
        daysLeft == 1 -> "明天到期（预警）"
        else -> "剩余 ${daysLeft}天"
    }
    val daysColor = when {
        daysLeft < 0 -> Red500
        status is ProductStatus.TakeDown -> Red500
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

        // 清单图片（如果有）
        if (product.photoPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.photoPath)
                    .size(720)
                    .build(),
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showFullImage = true },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 全屏查看大图
        if (showFullImage && product.photoPath != null) {
            FullScreenImageViewer(
                photoPath = product.photoPath,
                name = product.name,
                onDismiss = { showFullImage = false }
            )
        }

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
            StatusBadge(status = status)
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        // 到期日期
        DetailInfoRow(
            label = "到期日期",
            value = DateUtils.formatDate(product.expiryDate)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 剩余天数
        DetailInfoRow(
            label = "状态",
            value = daysText,
            valueColor = daysColor
        )

        // 可退货信息
        if (status is ProductStatus.Returnable) {
            Spacer(modifier = Modifier.height(10.dp))
            val returnDeadlineDays = (status.remainingDays - status.threshold).coerceAtLeast(0)
            if (returnDeadlineDays > 0) {
                DetailInfoRow(
                    label = "剩余退货",
                    value = "${returnDeadlineDays}天",
                    valueColor = Blue500
                )
            } else {
                DetailInfoRow(
                    label = "可退货",
                    value = "退货期内，到期还有${status.remainingDays}天",
                    valueColor = Blue500
                )
            }
        }

        // 可下架信息
        if (status is ProductStatus.TakeDown) {
            Spacer(modifier = Modifier.height(10.dp))
            DetailInfoRow(
                label = "可下架",
                value = "到期前两天内，请取下架处理",
                valueColor = Red500
            )
        }

        // 生产日期
        if (product.productionDate != null) {
            Spacer(modifier = Modifier.height(10.dp))
            DetailInfoRow(
                label = "生产日期",
                value = DateUtils.formatDate(product.productionDate!!)
            )
        }

        // 保质期
        if (product.shelfLifeDays > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            DetailInfoRow(
                label = "保质期",
                value = "${product.shelfLifeDays}天"
            )
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

        // 操作按钮
        // 标记完成/取消完成
        Button(
            onClick = onMarkComplete,
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

        }
}

/**
 * 详情信息行
 */
@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
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
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (valueColor != MaterialTheme.colorScheme.onSurface) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * 已处理状态标签 — 绿色处理徽标（已处理 / 已下架 / 已退货处理）
 */
@Composable
private fun StatusBadgeCompleted(label: String = "已处理") {
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
        // 发光圆点
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

/**
 * 全屏大图查看器 — 点击任意位置或图片关闭
 */
@Composable
private fun FullScreenImageViewer(
    photoPath: String,
    name: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoPath)
                    .size(1080)
                    .build(),
                contentDescription = name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }
    }
}