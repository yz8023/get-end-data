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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.expiryguard.app.ui.components.GlassCard
import com.expiryguard.app.ui.components.GradientBackground
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
            TopAppBar(
                title = { Text("保质期分组管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加分组",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
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
                    Text(
                        text = "暂无分组，点击右下角 + 添加",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Gray500
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除分组「${group.name}」吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(group)
                        showDeleteConfirm = null
                    }
                ) {
                    Text("删除", color = Red500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消")
                }
            }
        )
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
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (!group.isDefault) {
                IconButton(
                    onClick = onLongClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp),
                        tint = Red500
                    )
                }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "编辑分组" else "添加分组",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("分组名称") },
                    placeholder = { Text("如：3个月以下") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("描述") },
                    placeholder = { Text("如：保质期短，不提醒") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minDays,
                        onValueChange = onMinDaysChange,
                        label = { Text("最小天数") },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxDays,
                        onValueChange = onMaxDaysChange,
                        label = { Text("最大天数") },
                        placeholder = { Text("空=无上限") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = reminderThreshold,
                    onValueChange = onReminderThresholdChange,
                    label = { Text("提前提醒天数") },
                    placeholder = { Text("0") },
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
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}