package com.expiryguard.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.ui.components.EmptyState
import com.expiryguard.app.ui.components.GlassCard
import com.expiryguard.app.ui.components.GlassTopAppBar
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.liquid.components.GlassDialog
import com.expiryguard.app.ui.liquid.components.GlassIconButton
import com.expiryguard.app.ui.liquid.components.GlassTextField
import com.expiryguard.app.ui.theme.Red500

/**
 * 分类管理页面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryManagementScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showDeleteConfirm by remember { mutableStateOf<CategoryEntity?>(null) }

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
                title = { Text("分类管理") },
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
                contentDescription = "添加分类",
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
            } else if (uiState.categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = "暂无分类，点击右下角 + 添加",
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
                    items(uiState.categories, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            onClick = { viewModel.showEditDialog(category) },
                            onDelete = { showDeleteConfirm = category }
                        )
                    }
                }
            }
        }
    }

    // 添加/编辑对话框
    if (uiState.showDialog) {
        CategoryEditDialog(
            isEditing = uiState.editingCategory != null,
            name = uiState.dialogName,
            onNameChange = { viewModel.updateDialogName(it) },
            onSave = { viewModel.saveCategory() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }

    // 删除确认对话框
    showDeleteConfirm?.let { category ->
        GlassDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = "确认删除",
            onConfirm = {
                viewModel.deleteCategory(category)
                showDeleteConfirm = null
            },
            confirmText = "删除"
        ) {
            Text("确定要删除分类「${category.name}」吗？此操作不可恢复。")
        }
    }
}

/**
 * 分类卡片
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryCard(
    category: CategoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = Color(category.color)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        statusColor = categoryColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onDelete
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 颜色指示器
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 分类名称
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            // 编辑按钮
            GlassIconButton(
                icon = Icons.Default.Edit,
                contentDescription = "编辑",
                onClick = onClick,
                tint = MaterialTheme.colorScheme.primary
            )

            // 删除按钮
            GlassIconButton(
                icon = Icons.Default.Delete,
                contentDescription = "删除",
                onClick = onDelete,
                tint = Red500
            )
        }
    }
}

/**
 * 分类添加/编辑对话框
 */
@Composable
private fun CategoryEditDialog(
    isEditing: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialog(
        onDismissRequest = onDismiss,
        title = if (isEditing) "编辑分类" else "添加分类",
        onConfirm = onSave,
        confirmText = "保存",
        confirmEnabled = name.isNotBlank()
    ) {
        GlassTextField(
            value = name,
            onValueChange = onNameChange,
            label = "分类名称",
            placeholder = "输入分类名称",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
