package com.expiryguard.app.ui.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.repository.ProductRepository
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.expiryguard.app.domain.model.ProductStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 清单详情页面状态
 *
 * @property product 清单实体
 * @property category 清单所属分类
 * @property status 计算后的清单状态
 * @property isLoading 是否正在加载
 * @property isDeleted 是否已删除（用于触发返回）
 */
data class ProductDetailUiState(
    val product: ProductEntity? = null,
    val category: CategoryEntity? = null,
    val status: ProductStatus? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

/**
 * 清单详情 ViewModel，通过 SavedStateHandle 接收 productId
 *
 * 提供加载详情、删除清单、标记已处理等功能
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository
) : ViewModel() {

    /** 从导航参数中获取清单 ID */
    private val productId: Long = savedStateHandle.get<Long>("productId") ?: 0L

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        if (productId > 0) {
            loadProductDetail()
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 加载清单详情，包括清单信息和分类信息
     */
    private fun loadProductDetail() {
        viewModelScope.launch {
            repository.getProductById(productId).collect { product ->
                if (product != null) {
                    // 计算清单状态
                    val status = ExpiryRuleEngine.calculateStatus(
                        product.shelfLifeDays,
                        product.expiryDate
                    )

                    // 加载分类信息
                    var category: CategoryEntity? = null
                    if (product.categoryId != null) {
                        repository.getAllCategories().collect { categories ->
                            category = categories.find { it.id == product.categoryId }
                        }
                    }

                    _uiState.value = ProductDetailUiState(
                        product = product,
                        category = category,
                        status = status,
                        isLoading = false
                    )
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * 删除当前清单（软删除）
     */
    fun deleteProduct() {
        viewModelScope.launch {
            val result = repository.softDelete(listOf(productId))
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isDeleted = true) }
                },
                onFailure = { /* 删除失败，可考虑显示错误提示 */ }
            )
        }
    }

    /**
     * 标记清单为已处理
     *
     * 已处理逻辑：将清单软删除（相当于从待办列表中移除）
     */
    fun markAsHandled() {
        // 标记已处理 = 删除清单（从活跃列表中移除）
        deleteProduct()
    }
}