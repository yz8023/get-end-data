package com.expiryguard.app.ui.product

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity
import com.expiryguard.app.data.repository.ProductRepository
import com.expiryguard.app.util.DateUtils
import com.expiryguard.app.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * 编辑清单页面状态
 */
data class EditProductUiState(
    val product: ProductEntity? = null,
    val name: String = "",
    val notes: String = "",
    val expiryDate: Long? = null,
    val shelfLifeGroup: ShelfLifeGroupUi? = null,
    val shelfLifeDays: Int = 0,
    val categoryId: Long? = null,
    val photoPath: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val shelfLifeGroups: List<ShelfLifeGroupUi> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * 编辑清单 ViewModel
 *
 * 加载已有清单数据，支持修改名称、备注、到期日期、分类、图片等字段
 */
@HiltViewModel
class EditProductViewModel @Inject constructor(
    application: Application,
    private val repository: ProductRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EditProductUiState())
    val uiState: StateFlow<EditProductUiState> = _uiState.asStateFlow()

    fun loadProduct(productId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val product = repository.getProductById(productId).first()
            if (product != null) {
                _uiState.update {
                    it.copy(
                        product = product,
                        name = product.name,
                        notes = product.notes ?: "",
                        expiryDate = product.expiryDate,
                        shelfLifeDays = product.shelfLifeDays,
                        categoryId = product.categoryId,
                        photoPath = product.photoPath,
                        isLoading = false
                    )
                }
                // 获取到期日期后匹配分组
                if (product.expiryDate > 0) {
                    val today = DateUtils.todayTimestamp()
                    val daysBetween = DateUtils.daysBetween(today, product.expiryDate).coerceAtLeast(0)
                    val matchedGroup = matchGroupByDays(daysBetween)
                    _uiState.update { it.copy(shelfLifeGroup = matchedGroup) }
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "清单不存在")
                }
            }
        }
    }

    init {
        loadCategories()
        loadShelfLifeGroups()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getAllCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun loadShelfLifeGroups() {
        viewModelScope.launch {
            repository.getAllGroups().collect { entities ->
                val groups = if (entities.isNotEmpty()) {
                    entities.map { ShelfLifeGroupUi.fromEntity(it) }
                } else {
                    ShelfLifeGroup.entries.mapIndexed { index, group ->
                        ShelfLifeGroupUi.fromEnum(group, index)
                    }
                }
                _uiState.update { it.copy(shelfLifeGroups = groups) }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    /**
     * 根据分组范围估算保质期天数
     * 使用分组中点值，对于无上限分组（如1年以上）使用 minDays + 1
     */
    private fun estimateShelfLifeDays(minDays: Int, maxDays: Int): Int {
        return if (maxDays == Int.MAX_VALUE || maxDays > 10000) {
            minDays + 1
        } else {
            (minDays + maxDays) / 2
        }
    }

    fun updateExpiryDate(date: LocalDate) {
        val today = DateUtils.todayTimestamp()
        val expiryTs = DateUtils.toTimestamp(date)
        val daysBetween = DateUtils.daysBetween(today, expiryTs).coerceAtLeast(0)

        val matchedGroup = matchGroupByDays(daysBetween)

        _uiState.update {
            it.copy(
                expiryDate = expiryTs,
                shelfLifeGroup = matchedGroup,
                shelfLifeDays = matchedGroup?.let { estimateShelfLifeDays(it.minDays, it.maxDays) } ?: daysBetween
            )
        }
    }

    fun selectShelfLifeGroup(group: ShelfLifeGroupUi) {
        _uiState.update { state ->
            state.copy(
                shelfLifeGroup = group,
                shelfLifeDays = estimateShelfLifeDays(group.minDays, group.maxDays)
            )
        }
    }

    fun updateCategory(categoryId: Long) {
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun pickImage(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                ImageUtils.saveImage(context, uri)
            }
            if (savedPath != null) {
                _uiState.update { it.copy(photoPath = savedPath) }
            } else {
                _uiState.update { it.copy(errorMessage = "图片保存失败") }
            }
        }
    }

    fun saveProduct() {
        val state = _uiState.value
        val product = state.product ?: return

        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入清单名称") }
            return
        }
        if (state.expiryDate == null) {
            _uiState.update { it.copy(errorMessage = "请选择到期日期") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val updatedProduct = product.copy(
                name = state.name,
                categoryId = state.categoryId,
                photoPath = state.photoPath,
                expiryDate = state.expiryDate,
                shelfLifeDays = state.shelfLifeDays,
                notes = state.notes.ifBlank { null }
            )

            val result = repository.update(updatedProduct)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "保存失败: ${e.localizedMessage ?: "未知错误"}"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun matchGroupByDays(days: Int): ShelfLifeGroupUi? {
        val groups = _uiState.value.shelfLifeGroups
        val dbMatch = groups.firstOrNull { days in it.minDays until it.maxDays }
        if (dbMatch != null) return dbMatch
        val enumMatch = ShelfLifeGroup.matchByDays(days)
        val enumIndex = ShelfLifeGroup.entries.indexOf(enumMatch)
        return ShelfLifeGroupUi.fromEnum(enumMatch, enumIndex)
    }
}