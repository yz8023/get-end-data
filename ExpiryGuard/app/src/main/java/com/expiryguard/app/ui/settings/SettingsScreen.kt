package com.expiryguard.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expiryguard.app.ui.components.GlassCard
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.theme.Blue500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Orange500
import com.expiryguard.app.ui.theme.Purple500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.ui.theme.Yellow500
import kotlinx.coroutines.launch

/**
 * 设置页面，提供应用偏好设置和操作入口
 * 毛玻璃风格设计，与其他页面风格统一。
 *
 * @param onNavigate 导航回调，用于跳转到子页面
 * @param viewModel 设置页面 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 自动清理天数选择对话框
    var showAutoCleanupDialog by remember { mutableStateOf(false) }
    // 导出格式选择对话框
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        GradientBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 通知设置
                item(key = "section_notification") {
                    SectionTitle("通知")
                }
                item(key = "card_notification") {
                    SettingsCard(
                        icon = Icons.Default.Notifications,
                        iconTint = Blue500,
                        title = "通知设置",
                        subtitle = if (uiState.isNotificationEnabled) "已开启，提前 ${uiState.reminderDays} 天提醒" else "已关闭",
                        onClick = { onNavigate("notification_settings") }
                    )
                }

                // 数据管理
                item(key = "section_data") {
                    SectionTitle("数据管理")
                }
                item(key = "card_category") {
                    SettingsCard(
                        icon = Icons.Default.Settings,
                        iconTint = Purple500,
                        title = "分类管理",
                        subtitle = "管理产品分类",
                        onClick = { onNavigate("category_management") }
                    )
                }
                item(key = "card_shelf_life") {
                    SettingsCard(
                        icon = Icons.Default.Settings,
                        iconTint = Orange500,
                        title = "保质期分组管理",
                        subtitle = "自定义保质期分组",
                        onClick = { onNavigate("shelf_life_group_management") }
                    )
                }
                item(key = "card_backup") {
                    SettingsCard(
                        icon = Icons.Default.Backup,
                        iconTint = Green500,
                        title = "备份与恢复",
                        subtitle = "备份数据到文件或从文件恢复",
                        onClick = { onNavigate("backup_restore") }
                    )
                }
                item(key = "card_export") {
                    SettingsCard(
                        icon = Icons.Default.FileDownload,
                        iconTint = Blue500,
                        title = "数据导出 (CSV/JSON)",
                        subtitle = "导出产品数据",
                        onClick = { showExportDialog = true }
                    )
                }

                // 外观
                item(key = "section_appearance") {
                    SectionTitle("外观")
                }
                item(key = "switch_dark_mode") {
                    SettingsSwitchCard(
                        icon = Icons.Default.DarkMode,
                        iconTint = Purple500,
                        title = "深色模式",
                        subtitle = if (uiState.isDarkModeEnabled) "已开启" else "已关闭",
                        checked = uiState.isDarkModeEnabled,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }
                item(key = "switch_monet") {
                    SettingsSwitchCard(
                        icon = Icons.Default.ColorLens,
                        iconTint = Blue500,
                        title = "莫奈取色",
                        subtitle = if (uiState.isMonetEnabled) "已开启（使用系统壁纸色调）" else "已关闭",
                        checked = uiState.isMonetEnabled,
                        onCheckedChange = { viewModel.toggleMonet() }
                    )
                }

                // 清理
                item(key = "section_cleanup") {
                    SectionTitle("清理")
                }
                item(key = "card_auto_cleanup") {
                    SettingsCard(
                        icon = Icons.Default.AutoDelete,
                        iconTint = Red500,
                        title = "自动清理",
                        subtitle = if (uiState.autoCleanupDays > 0) "${uiState.autoCleanupDays} 天后自动清理回收站" else "从不自动清理",
                        onClick = { showAutoCleanupDialog = true }
                    )
                }

                // 关于
                item(key = "section_about") {
                    SectionTitle("关于")
                }
                item(key = "card_about") {
                    SettingsCard(
                        icon = Icons.Default.Info,
                        iconTint = Blue500,
                        title = "关于",
                        subtitle = "版本 ${uiState.appVersion}",
                        onClick = { onNavigate("about") }
                    )
                }

                // 底部留白
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // 自动清理天数选择对话框
    if (showAutoCleanupDialog) {
        AutoCleanupDialog(
            currentDays = uiState.autoCleanupDays,
            onSelect = { days ->
                viewModel.changeAutoCleanupDays(days)
                showAutoCleanupDialog = false
            },
            onDismiss = { showAutoCleanupDialog = false }
        )
    }

    // 导出格式选择对话框
    if (showExportDialog) {
        ExportDialog(
            onExportCsv = {
                showExportDialog = false
                scope.launch {
                    val path = viewModel.exportDataAsCsv()
                    if (path != null) {
                        Toast.makeText(context, "CSV 已导出到: $path", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onExportJson = {
                showExportDialog = false
                scope.launch {
                    val path = viewModel.exportDataAsJson()
                    if (path != null) {
                        Toast.makeText(context, "JSON 已导出到: $path", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

/**
 * 分区标题
 */
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

/**
 * 设置项卡片（可点击跳转）
 * 使用 GlassCard 毛玻璃风格替换普通 Material3 Card。
 */
@Composable
private fun SettingsCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标（带毛玻璃背景）
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 标题和副标题
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // 右箭头
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * 设置项开关卡片（带 Switch 开关）
 * 使用 GlassCard 毛玻璃风格替换普通 Material3 Card。
 */
@Composable
private fun SettingsSwitchCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(
        shape = RoundedCornerShape(12.dp),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标（带毛玻璃背景）
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 标题和副标题
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // 开关
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * 自动清理天数选择对话框
 */
@Composable
private fun AutoCleanupDialog(
    currentDays: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        7 to "7 天",
        15 to "15 天",
        30 to "30 天",
        60 to "60 天",
        0 to "从不"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自动清理") },
        text = {
            Column {
                options.forEach { (days, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(days) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            color = if (days == currentDays) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (days == currentDays) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 导出格式选择对话框
 */
@Composable
private fun ExportDialog(
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择导出格式") },
        text = {
            Column {
                // CSV 导出选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onExportCsv)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Green500,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = "导出为 CSV",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // JSON 导出选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onExportJson)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = Blue500,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = "导出为 JSON",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}