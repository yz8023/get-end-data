package com.expiryguard.app.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity
import com.expiryguard.app.ui.components.EmptyState
import com.expiryguard.app.ui.components.GlassCard
import com.expiryguard.app.ui.components.GlassTopAppBar
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.liquid.components.GlassDialog
import com.expiryguard.app.ui.liquid.components.GlassIconButton
import com.expiryguard.app.ui.liquid.components.GlassTextField
import com.expiryguard.app.ui.theme.Gray500
import com.expiryguard.app.ui.theme.Red500

/**
 * 保质期分组管理页面
 *
 * 提供自定义分组的 CRUD 功能：
 * - 列表显示所有分组（名称、描述、天数范围、提醒阈值）
 * - 添加分组（FAB）
 * - 点击编辑
 * - 长按删除（默认分组不可删除）
 * - 添加/编辑对话框
 * - 毛玻璃风格
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShelfLifeGroupManagementScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ShelfLifeGroupManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 删除确认对话框
    var showDeleteConfirm by remember { mutableStateOf<ShelfLifeGroupEntity?>(null) }

    // 错误提示
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            GlassTopAppBar(
                title = { Text("保质期分组管理") },
                navigationIcon = {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        onClick = onNavigateBack
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            GlassIconButton(
                icon = Icons.Default.Add,
                contentDescription = "添加分组",
                onClick = { viewModel.showAddDialog() },
                tint = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        GradientBackground {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = "暂无分组，点击右下角 + 添加",
                        icon = Icons.Default.Add
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 88.dp
                    )
                ) {
                    items(uiState.groups, key = { it.id }) { group ->
                        GroupCard(
                            group = group,
                            onClick = { viewModel.showEditDialog(group) },
                            onLongClick = {
                                if (group.isDefault) {
                                    Toast.makeText(context, "默认分组不可删除", Toast.LENGTH_SHORT).show()
                                } else {
                                    showDeleteConfirm = group
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (uiState.showDialog) {
        GroupEditDialog(
            isEditing = uiState.editingGroup != null,
            name = uiState.dialogName,
            description = uiState.dialogDescription,
            minDays = uiState.dialogMinDays,
            maxDays = uiState.dialogMaxDays,
            reminderThreshold = uiState.dialogReminderThreshold,
            colorHex = uiState.dialogColorHex,
            onNameChange = { viewModel.updateDialogName(it) },
            onDescriptionChange = { viewModel.updateDialogDescription(it) },
            onMinDaysChange = { viewModel.updateDialogMinDays(it) },
            onMaxDaysChange = { viewModel.updateDialogMaxDays(it) },
            onReminderThresholdChange = { viewModel.updateDialogReminderThreshold(it) },
            onColorHexChange = { viewModel.updateDialogColorHex(it) },
            onSave = { viewModel.saveGroup() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }

    // 删除确认对话框
    showDeleteConfirm?.let { group ->
        GlassDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = "确认删除",
            onConfirm = {
                viewModel.deleteGroup(group)
                showDeleteConfirm = null
            },
            confirmText = "删除"
        ) {
            Text("确定要删除分组「${group.name}」吗？此操作不可恢复。")
        }
    }
}

/**
 * 分组卡片组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(
    group: ShelfLifeGroupEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val groupColor = try {
        Color(android.graphics.Color.parseColor(group.colorHex))
    } catch (e: Exception) {
        Color(0xFFF97316)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        statusColor = groupColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 颜色指示器
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(groupColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 分组信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (group.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "默认分组",
                            modifier = Modifier.size(14.dp),
                            tint = Gray500
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("天数范围: ")
                        if (group.minDays == 0 && group.maxDays == 1) {
                            append("当天")
                        } else {
                            append("${group.minDays} ~ ")
                            append(if (group.maxDays == Int.MAX_VALUE) "无上限" else "${group.maxDays} 天")
                        }
                        append("  |  提醒: ${group.reminderThreshold} 天前")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray500
                )
            }

            // 操作按钮
            GlassIconButton(
                icon = Icons.Default.Edit,
                contentDescription = "编辑",
                onClick = onClick,
                tint = MaterialTheme.colorScheme.primary
            )
            if (!group.isDefault) {
                GlassIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "删除",
                    onClick = onLongClick,
                    tint = Red500
                )
            }
        }
    }
}

/**
 * 分组添加/编辑对话框
 */
@Composable
private fun GroupEditDialog(
    isEditing: Boolean,
    name: String,
    description: String,
    minDays: String,
    maxDays: String,
    reminderThreshold: String,
    colorHex: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMinDaysChange: (String) -> Unit,
    onMaxDaysChange: (String) -> Unit,
    onReminderThresholdChange: (String) -> Unit,
    onColorHexChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    // 预设颜色选项
    val colorOptions = listOf(
        "#EF4444", "#F97316", "#F59E0B", "#10B981",
        "#2563EB", "#7C3AED", "#EC4899", "#6B7280"
    )

    val minDaysValid = minDays.toIntOrNull()?.let { it >= 0 } ?: false
    val maxDaysValid = maxDays.isBlank() || (maxDays.toIntOrNull()?.let { it > 0 } ?: false)
    val rangeValid = maxDays.isBlank() ||
        (minDaysValid && (maxDays.toIntOrNull()?.let { it > (minDays.toIntOrNull() ?: Int.MIN_VALUE) } ?: false))
    val reminderValid = reminderThreshold.toIntOrNull()?.let { it >= 0 } ?: false

    GlassDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "编辑分组" else "添加分组",
        onConfirm = onSave,
        confirmText = "保存",
        confirmEnabled = name.isNotBlank() && minDaysValid && maxDaysValid && rangeValid && reminderValid
    ) {
        GlassTextField(
            value = name,
            onValueChange = onNameChange,
            label = "分组名称",
            placeholder = "如：3个月以下",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        GlassTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = "描述",
            placeholder = "如：保质期短，不提醒",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassTextField(
                value = minDays,
                onValueChange = onMinDaysChange,
                label = "最小天数",
                placeholder = "0",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            GlassTextField(
                value = maxDays,
                onValueChange = onMaxDaysChange,
                label = "最大天数",
                placeholder = "空=无上限",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        GlassTextField(
            value = reminderThreshold,
            onValueChange = onReminderThresholdChange,
            label = "提前提醒天数",
            placeholder = "0",
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // 颜色选择
        Text(
            text = "选择颜色",
            style = MaterialTheme.typography.labelMedium,
            color = Gray500
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colorOptions.forEach { hex ->
                val isSelected = hex == colorHex
                val color = try {
                    Color(android.graphics.Color.parseColor(hex))
                } catch (e: Exception) {
                    Color.Gray
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorHexChange(hex) }
                )
            }
        }
    }
}
