package com.expiryguard.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 分类管理页面状态
 */
data class CategoryManagementUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showDialog: Boolean = false,
    val editingCategory: CategoryEntity? = null,
    val dialogName: String = "",
    val dialogColor: Int = 0xFF2563EB.toInt(),
    val errorMessage: String? = null
)

/**
 * 分类管理 ViewModel
 */
@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            // 先检查是否已有分类，没有则插入默认分类
            val existing = repository.getAllCategories().first()
            if (existing.isEmpty()) {
                insertDefaultCategories()
            }
            // 收集分类数据
            repository.getAllCategories().collect { categories ->
                _uiState.update {
                    it.copy(categories = categories, isLoading = false)
                }
            }
        }
    }

    /**
     * 插入默认分类
     */
    private suspend fun insertDefaultCategories() {
        val defaultCategories = listOf(
            CategoryEntity(name = "食品", color = 0xFF10B981.toInt()),
            CategoryEntity(name = "饮料", color = 0xFF2563EB.toInt()),
            CategoryEntity(name = "日用品", color = 0xFFF59E0B.toInt()),
            CategoryEntity(name = "调味品", color = 0xFFF97316.toInt()),
            CategoryEntity(name = "冷冻食品", color = 0xFF7C3AED.toInt()),
            CategoryEntity(name = "其他", color = 0xFF6B7280.toInt())
        )
        defaultCategories.forEach { repository.insertCategory(it) }
    }

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingCategory = null,
                dialogName = "",
                dialogColor = 0xFF2563EB.toInt()
            )
        }
    }

    fun showEditDialog(category: CategoryEntity) {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingCategory = category,
                dialogName = category.name,
                dialogColor = category.color
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showDialog = false, editingCategory = null) }
    }

    fun updateDialogName(name: String) {
        _uiState.update { it.copy(dialogName = name) }
    }

    fun updateDialogColor(color: Int) {
        _uiState.update { it.copy(dialogColor = color) }
    }

    fun saveCategory() {
        val state = _uiState.value
        if (state.dialogName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入分类名称") }
            return
        }

        val category = CategoryEntity(
            id = state.editingCategory?.id ?: 0,
            name = state.dialogName.trim(),
            color = state.dialogColor
        )

        viewModelScope.launch {
            val result = repository.insertCategory(category)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(showDialog = false, editingCategory = null, errorMessage = null) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = "保存失败: ${e.localizedMessage ?: "未知错误"}") }
                }
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val result = repository.deleteCategory(category)
            result.fold(
                onSuccess = { _uiState.update { it.copy(errorMessage = null) } },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = "删除失败: ${e.localizedMessage ?: "未知错误"}") }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}