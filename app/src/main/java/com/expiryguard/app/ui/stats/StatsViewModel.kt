package com.expiryguard.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.db.dao.CategoryCount
import com.expiryguard.app.data.db.dao.ExpiryCount
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.repository.ExpiryPolicyRepository
import com.expiryguard.app.data.repository.ProductRepository
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.expiryguard.app.util.LocalDateTicker
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

data class StatsUiState(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val safeCount: Int = 0,
    val expiringCount: Int = 0,
    val returnableCount: Int = 0,
    val expiredCount: Int = 0,
    val takeDownCount: Int = 0,
    val processedCount: Int = 0,
    val categoryDistribution: List<CategoryCount> = emptyList(),
    val expiryDistribution: List<ExpiryCount> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val policyRepository: ExpiryPolicyRepository,
    private val dateTicker: LocalDateTicker
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            combine(
                repository.getAllActiveProducts(),
                repository.getCategoryStats(),
                repository.getExpiryDistribution(),
                policyRepository.policy,
                dateTicker.observeToday()
            ) { allProducts, categoryStats, expiryStats, policy, today ->
                val completed = allProducts.filter { it.isCompleted }
                val products = allProducts.filterNot { it.isCompleted }
                var safe = 0
                var expiring = 0
                var returnable = 0
                var expired = 0
                products.forEach { product ->
                    when (
                        ExpiryRuleEngine.calculateStatus(
                            product.shelfLifeDays,
                            product.expiryDate,
                            policy,
                            today
                        )
                    ) {
                        is ProductStatus.Safe -> safe++
                        is ProductStatus.ExpiringSoon,
                        is ProductStatus.TakeDown -> expiring++
                        is ProductStatus.Returnable -> returnable++
                        is ProductStatus.Expired -> expired++
                    }
                }
                StatsUiState(
                    totalCount = allProducts.size,
                    activeCount = products.size,
                    safeCount = safe,
                    expiringCount = expiring,
                    returnableCount = returnable,
                    expiredCount = expired,
                    takeDownCount = completed.count {
                        it.completedType == ProductEntity.COMPLETED_TYPE_TAKE_DOWN
                    },
                    processedCount = completed.count {
                        it.completedType != ProductEntity.COMPLETED_TYPE_TAKE_DOWN
                    },
                    categoryDistribution = categoryStats,
                    expiryDistribution = expiryStats,
                    products = allProducts,
                    isLoading = false
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { _uiState.value = it }
        }
    }
}
