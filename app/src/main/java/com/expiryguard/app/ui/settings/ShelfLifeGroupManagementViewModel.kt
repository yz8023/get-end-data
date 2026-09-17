package com.expiryguard.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity
import com.expiryguard.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 保质期分组管理页面状态
 */
data class ShelfLifeGroupManagementUiState(
    val groups: List<ShelfLifeGroupEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showDialog: Boolean = false,
    val editingGroup: ShelfLifeGroupEntity? = null,  // null 表示新增，非 null 表示编辑
    val dialogName: String = "",
    val dialogDescription: String = "",
    val dialogMinDays: String = "",
    val dialogMaxDays: String = "",
    val dialogReminderThreshold: String = "",
    val dialogColorHex: String = "#F97316",
    val errorMessage: String? = null
)

/**
 * 保质期分组管理 ViewModel
 *
 * 提供分组的 CRUD 操作，包括添加、编辑、删除分组。
 * 默认分组（isDefault = true）不可删除。
 * 首次进入时如果数据库没有分组，自动插入默认的5个分组。
 */
@HiltViewModel
class ShelfLifeGroupManagementViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShelfLifeGroupManagementUiState())
    val uiState: StateFlow<ShelfLifeGroupManagementUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    private fun loadGroups() {
        viewModelScope.launch {
            // 先检查数据库是否已有分组，没有则插入默认的5个分组
            val existing = repository.getAllGroups().first()
            if (existing.isEmpty()) {
                insertDefaultGroups()
            }
            // 收集分组数据
            repository.getAllGroups().collect { groups ->
                _uiState.update {
                    it.copy(groups = groups, isLoading = false)
                }
            }
        }
    }

    /**
     * 插入默认的5个保质期分组
     */
    private suspend fun insertDefaultGroups() {
        val defaultGroups = listOf(
            ShelfLifeGroupEntity(
                name = "当天到期",
                description = "今日到期提醒",
                minDays = 0, maxDays = 1,
                reminderThreshold = 0,
                colorHex = "#EF4444",
                sortOrder = 0, isDefault = true
            ),
            ShelfLifeGroupEntity(
                name = "3个月以下",
                description = "今日到期提醒",
                minDays = 1, maxDays = 90,
                reminderThreshold = 0,
                colorHex = "#F97316",
                sortOrder = 1, isDefault = true
            ),
            ShelfLifeGroupEntity(
                name = "3~6个月",
                description = "提前15天提醒",
                minDays = 90, maxDays = 183,
                reminderThreshold = 15,
                colorHex = "#F59E0B",
                sortOrder = 2, isDefault = true
            ),
            ShelfLifeGroupEntity(
                name = "6个月~1年",
                description = "提前20天提醒",
                minDays = 183, maxDays = 365,
                reminderThreshold = 20,
                colorHex = "#10B981",
                sortOrder = 3, isDefault = true
            ),
            ShelfLifeGroupEntity(
                name = "1年以上",
                description = "提前45天提醒",
                minDays = 365, maxDays = Int.MAX_VALUE,
                reminderThreshold = 45,
                colorHex = "#6B7280",
                sortOrder = 4, isDefault = true
            )
        )
        defaultGroups.forEach { repository.insertGroup(it) }
    }

    /**
     * 打开添加分组对话框
     */
    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingGroup = null,
                dialogName = "",
                dialogDescription = "",
                dialogMinDays = "",
                dialogMaxDays = "",
                dialogReminderThreshold = "",
                dialogColorHex = "#F97316"
            )
        }
    }

    /**
     * 打开编辑分组对话框
     */
    fun showEditDialog(group: ShelfLifeGroupEntity) {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingGroup = group,
                dialogName = group.name,
                dialogDescription = group.description,
                dialogMinDays = group.minDays.toString(),
                dialogMaxDays = if (group.maxDays == Int.MAX_VALUE) "" else group.maxDays.toString(),
                dialogReminderThreshold = group.reminderThreshold.toString(),
                dialogColorHex = group.colorHex
            )
        }
    }

    /**
     * 关闭对话框
     */
    fun dismissDialog() {
        _uiState.update {
            it.copy(showDialog = false, editingGroup = null)
        }
    }

    fun updateDialogName(name: String) {
        _uiState.update { it.copy(dialogName = name) }
    }

    fun updateDialogDescription(description: String) {
        _uiState.update { it.copy(dialogDescription = description) }
    }

    fun updateDialogMinDays(minDays: String) {
        _uiState.update { it.copy(dialogMinDays = minDays) }
    }

    fun updateDialogMaxDays(maxDays: String) {
        _uiState.update { it.copy(dialogMaxDays = maxDays) }
    }

    fun updateDialogReminderThreshold(threshold: String) {
        _uiState.update { it.copy(dialogReminderThreshold = threshold) }
    }

    fun updateDialogColorHex(colorHex: String) {
        _uiState.update { it.copy(dialogColorHex = colorHex) }
    }

    /**
     * 保存分组（新增或更新）
     */
    fun saveGroup() {
        val state = _uiState.value

        // 验证
        if (state.dialogName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入分组名称") }
            return
        }

        val minDays = state.dialogMinDays.toIntOrNull()
        if (minDays == null || minDays < 0) {
            _uiState.update { it.copy(errorMessage = "最小天数必须为非负整数") }
            return
        }

        val maxDays = if (state.dialogMaxDays.isBlank()) {
            Int.MAX_VALUE
        } else {
            state.dialogMaxDays.toIntOrNull()
        }
        if (maxDays == null || maxDays <= 0) {
            _uiState.update { it.copy(errorMessage = "最大天数必须为正整数") }
            return
        }

        if (minDays >= maxDays && maxDays != Int.MAX_VALUE) {
            _uiState.update { it.copy(errorMessage = "最大天数必须大于最小天数") }
            return
        }

        val reminderThreshold = state.dialogReminderThreshold.toIntOrNull()
        if (reminderThreshold == null || reminderThreshold < 0) {
            _uiState.update { it.copy(errorMessage = "提醒天数必须为非负整数") }
            return
        }

        val editingGroup = state.editingGroup
        val group = ShelfLifeGroupEntity(
            id = editingGroup?.id ?: 0,
            name = state.dialogName.trim(),
            description = state.dialogDescription.trim(),
            minDays = minDays,
            maxDays = maxDays,
            reminderThreshold = reminderThreshold,
            colorHex = state.dialogColorHex,
            sortOrder = editingGroup?.sortOrder ?: (state.groups.size),
            isDefault = editingGroup?.isDefault ?: false,
            createdAt = editingGroup?.createdAt ?: System.currentTimeMillis()
        )

        viewModelScope.launch {
            val result = if (editingGroup != null) {
                repository.updateGroup(group)
            } else {
                repository.insertGroup(group)
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(showDialog = false, editingGroup = null, errorMessage = null) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = "保存失败: ${e.localizedMessage ?: "未知错误"}") }
                }
            )
        }
    }

    /**
     * 删除分组（默认分组不可删除）
     */
    fun deleteGroup(group: ShelfLifeGroupEntity) {
        if (group.isDefault) {
            _uiState.update { it.copy(errorMessage = "默认分组不可删除") }
            return
        }
        viewModelScope.launch {
            val result = repository.deleteGroup(group)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(errorMessage = null) }
                },
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