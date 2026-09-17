package com.expiryguard.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
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
 * 首页数据状态
 *
 * @property todayExpiry 今日到期的清单列表（含可退货）
 * @property warning 预警清单列表（还有1天到期）
 * @property takeDownCompleted 已下架的清单列表
 * @property processedCompleted 已处理的清单列表
 * @property pendingProducts 待处理的清单列表（剩余未完成）
 * @property todayCount 今日到期数量
 * @property warningCount 预警数量
 * @property takeDownCompletedCount 已下架数量
 * @property processedCompletedCount 已处理数量
 * @property pendingCount 待处理数量
 * @property recentProducts 最近添加的5个清单
 * @property totalCount 清单总数
 * @property isLoading 是否正在加载
 */
data class HomeUiState(
    val todayExpiry: List<ProductEntity> = emptyList(),
    val warning: List<ProductEntity> = emptyList(),
    val takeDownCompleted: List<ProductEntity> = emptyList(),
    val processedCompleted: List<ProductEntity> = emptyList(),
    val pendingProducts: List<ProductEntity> = emptyList(),
    val todayCount: Int = 0,
    val warningCount: Int = 0,
    val takeDownCompletedCount: Int = 0,
    val processedCompletedCount: Int = 0,
    val pendingCount: Int = 0,
    val recentProducts: List<ProductEntity> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

/**
 * 首页 ViewModel，负责加载首页所需的各项数据
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    companion object {
        private val KEY_TAKE_DOWN_DAYS = intPreferencesKey("take_down_days")
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadData()
    }

    /**
     * 加载所有活跃清单数据，并按状态分类统计
     * 归类规则：
     * - 今日到期：到期日期是今天
     * - 可退货：根据规则1，到期前阈值天内放入待办
     * - 预警：还有1天到期
     * - 已处理：isCompleted == true
     * - 待处理：剩下的未完成清单
     */
    private fun loadData() {
        viewModelScope.launch {
            // combine 监听产品数据 与 取件天数设置，任一变化时重新计算
            // 这样修改「取件天数」后首页会立即重算「可下架」范围
            combine(
                repository.getAllActiveProducts(),
                dataStore.data.map { prefs -> prefs[KEY_TAKE_DOWN_DAYS] ?: 2 }
            ) { products, takeDownDays ->
                // 同步取件阈值到引擎，确保分组、状态计算口径一致
                ExpiryRuleEngine.updateTakeDownThreshold(takeDownDays)
                val today = DateUtils.todayTimestamp()
                processProducts(products, today)
            }
                .flowOn(Dispatchers.Default)
                .collect { state ->
                    _uiState.value = state
                    _isRefreshing.value = false
                }
        }
    }

    /**
     * 处理清单列表并更新 UI 状态
     * 在 Default 线程执行，主线程只负责接收结果
     */
    private fun processProducts(products: List<ProductEntity>, today: Long): HomeUiState {
        // 统一口径分组（与启动通知一致）—— 使用引擎静态阈值，确保与设置页同步
        val takeDownThreshold = ExpiryRuleEngine.takeDownThreshold
        val groups = ExpiryRuleEngine.computePendingGroups(products, today, takeDownThreshold)

        // 今日到期（含可下架、可退货、已过期）—— 合并去重
        val todayWithReturnable = (groups.todayExpiry + groups.takeDown + groups.returnable + groups.expired).distinctBy { it.id }

        // 预警（还剩1天，已排除今日待办）
        val warning = groups.warning

        // 已处理：仅保留今日到期、可下架、预警、已过期或可退货的已完成清单
        // 按 completedType 拆分为「已下架」与「已处理」两个分组
        val completed = products.filter { product ->
            if (!product.isCompleted) return@filter false
            val days = DateUtils.daysBetween(today, product.expiryDate)
            if (days <= 1) return@filter true
            val status = ExpiryRuleEngine.calculateStatus(product.shelfLifeDays, product.expiryDate)
            status is ProductStatus.Returnable || status is ProductStatus.TakeDown
        }.sortedByDescending { it.completedAt ?: 0L }
        val takeDownCompleted = completed.filter {
            it.completedType == ProductEntity.COMPLETED_TYPE_TAKE_DOWN
        }
        val processedCompleted = completed.filter {
            it.completedType != ProductEntity.COMPLETED_TYPE_TAKE_DOWN
        }

        // 待处理：剩下的未完成清单（排除今日到期、可退货、预警）
        val todayAndWarningIds = (todayWithReturnable.map { it.id } + warning.map { it.id }).toSet()
        val pending = products.filter {
            !it.isCompleted && it.id !in todayAndWarningIds
        }

        // 最近添加：按创建时间降序取前5个
        val recent = products.sortedByDescending { it.createdAt }.take(5)

        return HomeUiState(
            todayExpiry = todayWithReturnable,
            warning = warning,
            takeDownCompleted = takeDownCompleted,
            processedCompleted = processedCompleted,
            pendingProducts = pending,
            todayCount = todayWithReturnable.size,
            warningCount = warning.size,
            takeDownCompletedCount = takeDownCompleted.size,
            processedCompletedCount = processedCompleted.size,
            pendingCount = pending.size,
            recentProducts = recent,
            totalCount = products.size,
            isLoading = false
        )
    }

    /**
     * 手动刷新首页数据
     * 触发下拉刷新指示器显示，并通过 Room Flow 自动响应数据变化
     */
    fun refresh() {
        _isRefreshing.value = true
    }

    /**
     * 切换清单的完成状态
     * @param completedType 完成类型：TAKE_DOWN 已下架 / RETURN 已退货处理 / null 未完成或旧数据
     */
    fun toggleProductCompletion(productId: Long, isCompleted: Boolean, completedType: String? = null) {
        viewModelScope.launch {
            repository.toggleCompletion(productId, isCompleted, completedType)
        }
    }
}