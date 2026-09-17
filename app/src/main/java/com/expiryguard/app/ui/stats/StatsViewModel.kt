package com.expiryguard.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 统计数据状态
 */
data class StatsUiState(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val safeCount: Int = 0,
    val expiringCount: Int = 0,
    val returnableCount: Int = 0,
    val expiredCount: Int = 0,
    val takeDownCount: Int = 0,
    val processedCount: Int = 0,
    val categoryDistribution: List<com.expiryguard.app.data.db.dao.CategoryCount> = emptyList(),
    val expiryDistribution: List<com.expiryguard.app.data.db.dao.ExpiryCount> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * 统计页面 ViewModel，负责计算产品状态统计数据
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            // 收集所有清单数据并计算统计（含已下架/已处理的已标记清单）
            repository.getAllActiveProducts().collect { allProducts ->
                // 已标记的清单（已下架 / 已处理）
                val completed = allProducts.filter { it.isCompleted }
                val takeDownCount = completed.count {
                    it.completedType == ProductEntity.COMPLETED_TYPE_TAKE_DOWN
                }
                val processedCount = completed.count {
                    it.completedType != ProductEntity.COMPLETED_TYPE_TAKE_DOWN
                }

                // 未处理的清单（用于状态分布统计）
                val products = allProducts.filter { !it.isCompleted }
                var safe = 0
                var expiring = 0
                var returnable = 0
                var expired = 0
                products.forEach { product ->
                    when (com.expiryguard.app.domain.engine.ExpiryRuleEngine.calculateStatus(
                        product.shelfLifeDays, product.expiryDate
                    )) {
                        is com.expiryguard.app.domain.model.ProductStatus.Safe -> safe++
                        is com.expiryguard.app.domain.model.ProductStatus.ExpiringSoon,
                        is com.expiryguard.app.domain.model.ProductStatus.TakeDown -> expiring++
                        is com.expiryguard.app.domain.model.ProductStatus.Returnable -> returnable++
                        is com.expiryguard.app.domain.model.ProductStatus.Expired -> expired++
                        else -> {}
                    }
                }

                _uiState.value = StatsUiState(
                    totalCount = allProducts.size,
                    activeCount = products.size,
                    safeCount = safe,
                    expiringCount = expiring,
                    returnableCount = returnable,
                    expiredCount = expired,
                    takeDownCount = takeDownCount,
                    processedCount = processedCount,
                    categoryDistribution = emptyList(),
                    expiryDistribution = emptyList(),
                    products = allProducts,
                    isLoading = false
                )
            }
        }
    }
}