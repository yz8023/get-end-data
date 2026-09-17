package com.expiryguard.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expiryguard.app.ui.components.GlassTopAppBar
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.liquid.components.CompactSwitch
import com.expiryguard.app.ui.liquid.components.GlassDialog
import com.expiryguard.app.ui.liquid.components.GlassIconButton
import com.expiryguard.app.ui.liquid.components.PreferenceCard
import com.expiryguard.app.ui.liquid.components.PreferenceRow
import com.expiryguard.app.ui.liquid.components.SectionTitle
import com.expiryguard.app.ui.liquid.components.SegmentedTabs
import com.expiryguard.app.ui.liquid.screens.AppSettingsState
import com.expiryguard.app.ui.liquid.screens.LocalAppSettings
import com.expiryguard.app.ui.liquid.screens.rememberAppSettingsState
import com.expiryguard.app.ui.liquid.theme.PaletteId
import com.expiryguard.app.ui.liquid.theme.VisualMode
import com.expiryguard.app.ui.liquid.theme.palettePrimaryLight
import kotlinx.coroutines.launch

/**
 * 设置页面，提供应用偏好设置和操作入口
 * 液态玻璃设计风格，与其他页面风格统一。
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
    // 与主题桥接层共享同一个实例，外观旋钮改动即时生效。
    val settings = LocalAppSettings.current ?: rememberAppSettingsState()

    // 自动清理天数选择对话框
    var showAutoCleanupDialog by remember { mutableStateOf(false) }
    // 取件天数选择对话框
    var showTakeDownDaysDialog by remember { mutableStateOf(false) }
    // 导出格式选择对话框
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassTopAppBar(title = { Text("设置") })
        }
    ) { padding ->
        GradientBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 通知设置
                item(key = "section_notification") {
                    SectionTitle("通知")
                }
                item(key = "card_notification") {
                    PreferenceCard {
                        PreferenceRow(
                            icon = Icons.Default.Notifications,
                            title = "通知设置",
                            subtitle = if (uiState.isNotificationEnabled) "已开启，提前 ${uiState.reminderDays} 天提醒" else "已关闭",
                            onClick = { onNavigate("notification_settings") },
                            trailing = {
                                GlassIconButton(
                                    icon = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    onClick = { onNavigate("notification_settings") }
                                )
                            }
                        )
                    }
                }

                // 数据管理
                item(key = "section_data") {
                    SectionTitle("数据管理")
                }
                item(key = "card_category") {
                    PreferenceCard {
                        PreferenceRow(
                            icon = Icons.Default.Settings,
                            title = "分类管理",
                            subtitle = "管理产品分类",
                            onClick = { onNavigate("category_management") },
                            trailing = {
                                GlassIconButton(
                                    icon = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    onClick = { onNavigate("category_management") }
                                )
                            }
                        )
                    }
                }
                item(key = "card_shelf_life") {
                    PreferenceCard {
                        PreferenceRow(
                            icon = Icons.Default.Settings,
                            title = "保质期分组管理",
                            subtitle = "自定义保质期分组",
                            onClick = { onNavigate("shelf_life_group_management") },
                            trailing = {
                                GlassIconButton(
                                    icon = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    onClick = { onNavigate("shelf_life_group_management") }
                                )
                            }
                        )
                    }
                }
                item(key = "card_backup") {
                    PreferenceCard {
                        PreferenceRow(
                            icon = Icons.Default.Backup,
                            title = "备份与恢复",
                            subtitle = "备份数据到文件或从文件恢复",
                            onClick = { onNavigate("backup_restore") },
                            trailing = {
                                GlassIconButton(
                                    icon = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    onClick = { onNavigate("backup_restore") }
                                )
                            }
                        )
                    }
                }
                item(key = "card_export") {
                    PreferenceCard {
                        PreferenceRow(
                            icon = Icons.Default.FileDownload,
                            title = "数据导出 (CSV/JSON)",
                            subtitle = "导出产品数据",
                            onClick = { showExportDialog = true },
                            trailing = {
                                GlassIconButton(
                                    icon = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    onClick = { showExportDialog = true }
                                )
                            }
                        )
                    }
                }

                // 外观
                item(key = "section_appearance") {
                    SectionTitle("外观")
                }
                item(key = "switch_dark_mode") {
                    PreferenceCard {
                        PreferenceRow(
                            icon = Icons.Default.DarkMode,
                            title = "深色模式",
                            subtitle = if (uiState.isDarkModeEnabled) "已开启" else "已关闭",
                            trailing = {
                                CompactSwitch(
                                    checked = uiState.isDarkModeEnabled,
                                    onCheckedChange = { viewModel.toggleDarkMode() }
                                )
                            }
                        )
                    }
                }
                item(key = "switch_monet") {
                    PreferenceCard {
                        PreferenceRow(
                            icon = Icons.Default.ColorLens,
                            title = "莫奈取色",
                            subtitle = if (uiState.isMonetEnabled) "已开启（使用系统壁纸色调）" else "已关闭",
                            trailing = {
                                CompactSwitch(
                                    checked = uiState.isMonetEnabled,
                                    onCheckedChange = { viewModel.toggleMonet() }
                                )
                            }
                        )
                    }
                }
                item(key = "card_glass_style") {
                    GlassStyleCard(settings = settings)
                }

                // 清理
                item(key = "section_cleanup") {
                    SectionTitle("清理")
                }
                item(key = "card_auto_cleanup") {
                    PreferenceCard {
                        PreferenceRow(
                            icon = Icons.Default.AutoDelete,
                            title = "自动清理",
                            subtitle = if (uiState.autoCleanupDays > 0) "${uiState.autoCleanupDays} 天后自动清理回收站" else "从不自动清理",
                            onClick = { showAutoCleanupDialog = true },
                            trailing = {
                                GlassIconButton(
                                    icon = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    onClick = { showAutoCleanupDialog = true }
                                )
                            }
                        )
                    }
                }
                item(key = "card_take_down_days") {
                    PreferenceCard {
                        PreferenceRow(
                            icon = Icons.Default.Settings,
                            title = "取件天数",
                            subtitle = "到期前 ${uiState.takeDownDays} 天内提醒取件下架",
                            onClick = { showTakeDownDaysDialog = true },
                            trailing = {
                                GlassIconButton(
                                    icon = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    onClick = { showTakeDownDaysDialog = true }
                                )
                            }
                        )
                    }
                }

                // 关于
                item(key = "section_about") {
                    SectionTitle("关于")
                }
                item(key = "card_about") {
                    PreferenceCard {
                        PreferenceRow(
                            icon = Icons.Default.Info,
                            title = "关于",
                            subtitle = "版本 ${uiState.appVersion}",
                            onClick = { onNavigate("about") },
                            trailing = {
                                GlassIconButton(
                                    icon = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    onClick = { onNavigate("about") }
                                )
                            }
                        )
                    }
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

    // 取件天数选择对话框
    if (showTakeDownDaysDialog) {
        TakeDownDaysDialog(
            currentDays = uiState.takeDownDays,
            onSelect = { days ->
                viewModel.changeTakeDownDays(days)
                showTakeDownDaysDialog = false
            },
            onDismiss = { showTakeDownDaysDialog = false }
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
    var selectedDays by remember(currentDays) { mutableStateOf(currentDays) }

    GlassDialog(
        onDismissRequest = onDismiss,
        title = "自动清理",
        onConfirm = { onSelect(selectedDays) },
        confirmText = "确定",
        dismissText = "取消"
    ) {
        Text(
            text = "选择回收站中的清单保留天数，到期后自动清理",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SegmentedTabs(
            labels = options.map { it.second },
            selectedIndex = options.indexOfFirst { it.first == selectedDays }.coerceAtLeast(0),
            onSelected = { index -> selectedDays = options[index].first },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 取件天数选择对话框
 */
@Composable
private fun TakeDownDaysDialog(
    currentDays: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(1, 2, 3, 5, 7)
    var selectedDays by remember(currentDays) { mutableStateOf(currentDays) }

    GlassDialog(
        onDismissRequest = onDismiss,
        title = "取件天数",
        onConfirm = { onSelect(selectedDays) },
        confirmText = "确定",
        dismissText = "取消"
    ) {
        Text(
            text = "选择到期前多少天进入「可下架」状态（提醒取件）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SegmentedTabs(
            labels = options.map { if (it == 1) "1 天" else "$it 天内" },
            selectedIndex = options.indexOfFirst { it == selectedDays }.coerceAtLeast(0),
            onSelected = { index -> selectedDays = options[index] },
            modifier = Modifier.fillMaxWidth()
        )
    }
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
    val actions = listOf("CSV" to onExportCsv, "JSON" to onExportJson)
    var selectedIndex by remember { mutableIntStateOf(0) }

    GlassDialog(
        onDismissRequest = onDismiss,
        title = "选择导出格式",
        onConfirm = { actions[selectedIndex].second() },
        confirmText = "导出",
        dismissText = "取消"
    ) {
        Text(
            text = "选择要导出的数据格式",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SegmentedTabs(
            labels = listOf("CSV", "JSON"),
            selectedIndex = selectedIndex,
            onSelected = { selectedIndex = it },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 液态玻璃外观设置卡片
 * 提供视觉风格切换、主题配色选择与玻璃质感参数调节，
 * 全部写入 [AppSettingsState]，由主题桥接层即时生效。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlassStyleCard(settings: AppSettingsState) {
    PreferenceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "液态玻璃外观",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text = "视觉风格",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            SegmentedTabs(
                labels = VisualMode.entries.map { it.label },
                selectedIndex = VisualMode.entries.indexOf(settings.visualMode),
                onSelected = { index ->
                    settings.updateVisualMode(VisualMode.entries[index])
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "主题配色",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaletteId.entries.forEach { palette ->
                    val selected = settings.palette == palette
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(palettePrimaryLight(palette))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape
                            )
                            .clickable { settings.updatePalette(palette) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            GlassSliderRow(
                label = "玻璃透明度",
                value = settings.glassOpacity,
                valueRange = 0.3f..0.95f,
                onValueChangeFinished = settings::updateGlassOpacity
            )
            GlassSliderRow(
                label = "折射强度",
                value = settings.glassRefraction,
                valueRange = 0.3f..2f,
                onValueChangeFinished = settings::updateGlassRefraction
            )
            GlassSliderRow(
                label = "动效阻尼",
                value = settings.motionDamping,
                valueRange = 0.1f..0.9f,
                onValueChangeFinished = settings::updateMotionDamping
            )
        }
    }
}

/**
 * 玻璃质感参数滑块行
 */
@Composable
private fun GlassSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (Float) -> Unit
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("%.2f", localValue),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChangeFinished(localValue) },
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
