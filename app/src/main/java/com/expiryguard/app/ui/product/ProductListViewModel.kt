package com.expiryguard.app.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.repository.ProductRepository
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 筛选条件枚举
 */
enum class ProductFilter(val label: String) {
    ALL("全部"),
    SAFE("安全"),
    EXPIRING_SOON("即将到期"),
    RETURNABLE("可退货"),
    TAKEDOWN("可下架"),
    EXPIRED("已过期")
}

/**
 * 视图模式枚举
 */
enum class ViewMode {
    LIST,   // 列表模式
    GRID    // 网格模式
}

/**
 * 清单列表页面状态
 *
 * @property products 当前展示的清单列表
 * @property filteredProducts 经过筛选和搜索后的清单列表
 * @property currentFilter 当前选中的筛选条件
 * @property searchQuery 搜索关键词
 * @property viewMode 当前视图模式（列表/网格）
 * @property isMultiSelectMode 是否处于多选模式
 * @property selectedIds 多选模式下选中的清单 ID 集合
 * @property isLoading 是否正在加载
 */
data class ProductListUiState(
    val products: List<ProductEntity> = emptyList(),
    val filteredProducts: List<ProductEntity> = emptyList(),
    val currentFilter: ProductFilter = ProductFilter.ALL,
    val searchQuery: String = "",
    val viewMode: ViewMode = ViewMode.LIST,
    val isMultiSelectMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
)

/**
 * 清单列表 ViewModel，支持筛选、搜索、批量操作和滑动删除
 */
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    /**
     * 加载所有活跃清单，并在数据变化时重新应用筛选和搜索条件
     * 过滤计算在 Default 线程执行，避免阻塞主线程
     */
    private fun loadProducts() {
        viewModelScope.launch {
            repository.getAllActiveProducts()
                .map { products ->
                    val state = _uiState.value
                    val today = DateUtils.todayTimestamp()
                    state.copy(
                        products = products,
                        filteredProducts = applyFilter(products, state.currentFilter, state.searchQuery, today),
                        isLoading = false
                    )
                }
                .flowOn(Dispatchers.Default)
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    /**
     * 根据筛选条件和搜索关键词过滤清单列表
     */
    private fun applyFilter(
        products: List<ProductEntity>,
        filter: ProductFilter,
        query: String,
        today: Long
    ): List<ProductEntity> {
        // 先按筛选条件过滤
        val filtered = when (filter) {
            ProductFilter.ALL -> products
            ProductFilter.SAFE -> products.filter {
                val status = ExpiryRuleEngine.calculateStatus(it.shelfLifeDays, it.expiryDate)
                status is ProductStatus.Safe
            }
            ProductFilter.EXPIRING_SOON -> products.filter {
                val status = ExpiryRuleEngine.calculateStatus(it.shelfLifeDays, it.expiryDate)
                status is ProductStatus.ExpiringSoon
            }
            ProductFilter.RETURNABLE -> products.filter {
                val status = ExpiryRuleEngine.calculateStatus(it.shelfLifeDays, it.expiryDate)
                status is ProductStatus.Returnable
            }
            ProductFilter.TAKEDOWN -> products.filter {
                val status = ExpiryRuleEngine.calculateStatus(it.shelfLifeDays, it.expiryDate)
                status is ProductStatus.TakeDown
            }
            ProductFilter.EXPIRED -> products.filter {
                DateUtils.daysBetween(today, it.expiryDate) <= 0
            }
        }

        // 再按搜索关键词过滤
        return if (query.isBlank()) {
            filtered
        } else {
            val q = query.trim()
            filtered.filter { product ->
                val nameMatch = product.name.contains(q, ignoreCase = true)
                val notesMatch = product.notes?.contains(q, ignoreCase = true) == true
                val dateMatch = dateMatchesQuery(product.expiryDate, q)
                nameMatch || notesMatch || dateMatch
            }
        }
    }

    /**
     * 检查到期日期是否匹配搜索关键词
     * 支持格式：26（日）、6月26、6.26、0626、06-26、2026-06-26 等
     */
    private fun dateMatchesQuery(timestamp: Long, query: String): Boolean {
        val date = DateUtils.toLocalDate(timestamp)
        val month = date.monthValue
        val day = date.dayOfMonth
        val year = date.year

        // 生成多种日期格式
        val dateFormats = listOf(
            "${year}年${month}月${day}日",
            "${month}月${day}日",
            "${month}月${day}",
            String.format("%d.%d", month, day),
            String.format("%d.%02d", month, day),
            String.format("%02d%02d", month, day),
            String.format("%d-%d", month, day),
            String.format("%02d-%02d", month, day),
            String.format("%04d-%02d-%02d", year, month, day),
            String.format("%02d/%02d", month, day),
            String.format("%d/%d", month, day),
            day.toString(),
            String.format("%02d", day)
        )

        return dateFormats.any { it.contains(query) }
    }

    /**
     * 设置初始筛选条件（页面第一次加载时使用）
     */
    fun setInitialFilter(filter: ProductFilter) {
        _uiState.update { state ->
            val today = DateUtils.todayTimestamp()
            state.copy(
                currentFilter = filter,
                filteredProducts = applyFilter(state.products, filter, state.searchQuery, today)
            )
        }
    }

    /**
     * 切换筛选条件
     */
    fun setFilter(filter: ProductFilter) {
        _uiState.update { state ->
            val today = DateUtils.todayTimestamp()
            state.copy(
                currentFilter = filter,
                filteredProducts = applyFilter(state.products, filter, state.searchQuery, today)
            )
        }
    }

    /**
     * 更新搜索关键词（在 Default 线程执行过滤，避免输入时阻塞主线程）
     */
    fun setSearchQuery(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val today = DateUtils.todayTimestamp()
            val filtered = applyFilter(state.products, state.currentFilter, query, today)
            _uiState.update {
                it.copy(searchQuery = query, filteredProducts = filtered)
            }
        }
    }

    /**
     * 切换视图模式（列表/网格）
     */
    fun toggleViewMode() {
        _uiState.update { state ->
            state.copy(
                viewMode = if (state.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            )
        }
    }

    // ==================== 多选模式 ====================

    /**
     * 进入多选模式
     */
    fun enterMultiSelectMode() {
        _uiState.update { it.copy(isMultiSelectMode = true, selectedIds = emptySet()) }
    }

    /**
     * 退出多选模式
     */
    fun exitMultiSelectMode() {
        _uiState.update { it.copy(isMultiSelectMode = false, selectedIds = emptySet()) }
    }

    /**
     * 切换单个清单的选中状态
     */
    fun toggleSelection(productId: Long) {
        _uiState.update { state ->
            val newSelected = if (productId in state.selectedIds) {
                state.selectedIds - productId
            } else {
                state.selectedIds + productId
            }
            // 如果没有选中项，自动退出多选模式
            state.copy(
                selectedIds = newSelected,
                isMultiSelectMode = newSelected.isNotEmpty()
            )
        }
    }

    /**
     * 全选/取消全选
     */
    fun toggleSelectAll() {
        _uiState.update { state ->
            val allIds = state.filteredProducts.map { it.id }.toSet()
            val isAllSelected = state.selectedIds == allIds
            state.copy(
                selectedIds = if (isAllSelected) emptySet() else allIds,
                isMultiSelectMode = !isAllSelected
            )
        }
    }

    // ==================== 批量操作 ====================

    /**
     * 批量删除选中的清单（软删除）
     */
    fun batchDelete() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            repository.softDelete(ids)
            exitMultiSelectMode()
        }
    }

    /**
     * 批量更新选中清单的备注
     */
    fun batchUpdateNotes(notes: String) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            repository.updateNotes(ids, notes)
            exitMultiSelectMode()
        }
    }

    /**
     * 切换清单完成状态
     * 清单页按钮统一标记为「已处理」；取消时清空类型
     */
    fun swipeComplete(productId: Long) {
        viewModelScope.launch {
            val product = _uiState.value.products.firstOrNull { it.id == productId }
            product?.let {
                val completed = !it.isCompleted
                repository.toggleCompletion(productId, completed, null)
            }
        }
    }

    /**
     * 滑动删除单个清单
     */
    fun swipeDelete(productId: Long) {
        viewModelScope.launch {
            repository.softDelete(listOf(productId))
        }
    }

    /**
     * 手动触发刷新
     */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadProducts()
    }
}