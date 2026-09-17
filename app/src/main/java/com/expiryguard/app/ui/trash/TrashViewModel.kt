package com.expiryguard.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 回收站页面 ViewModel，管理已删除清单的恢复和永久删除操作
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _trashedProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
    val trashedProducts: StateFlow<List<ProductEntity>> = _trashedProducts.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    init {
        loadTrashed()
    }

    /**
     * 从 repository 获取回收站清单
     */
    private fun loadTrashed() {
        viewModelScope.launch {
            repository.getTrashedProducts().collect { products ->
                _trashedProducts.value = products
            }
        }
    }

    /**
     * 切换选中状态
     *
     * @param id 清单 ID
     */
    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedIds.value = current
        // 如果没有选中项，退出多选模式
        if (current.isEmpty()) {
            _isMultiSelectMode.value = false
        }
    }

    /**
     * 全选
     */
    fun selectAll() {
        _selectedIds.value = _trashedProducts.value.map { it.id }.toSet()
        _isMultiSelectMode.value = true
    }

    /**
     * 取消全选
     */
    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isMultiSelectMode.value = false
    }

    /**
     * 进入多选模式
     */
    fun enterMultiSelectMode(id: Long) {
        _isMultiSelectMode.value = true
        _selectedIds.value = setOf(id)
    }

    /**
     * 批量恢复
     */
    fun restoreSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            if (ids.isNotEmpty()) {
                repository.restore(ids)
                _selectedIds.value = emptySet()
                _isMultiSelectMode.value = false
            }
        }
    }

    /**
     * 批量永久删除
     */
    fun permanentDeleteSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value.toList()
            if (ids.isNotEmpty()) {
                repository.permanentDelete(ids)
                _selectedIds.value = emptySet()
                _isMultiSelectMode.value = false
            }
        }
    }
}