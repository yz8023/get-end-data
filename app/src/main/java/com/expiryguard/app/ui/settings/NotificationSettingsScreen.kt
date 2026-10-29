package com.expiryguard.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.expiryguard.app.notification.NotificationHelper
import com.expiryguard.app.ui.components.GlassTopAppBar
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.liquid.components.CompactSwitch
import com.expiryguard.app.ui.liquid.components.PreferenceCard
import com.expiryguard.app.ui.liquid.components.PreferenceRow
import com.expiryguard.app.ui.liquid.components.PrimaryButton
import com.expiryguard.app.ui.liquid.components.SectionTitle
import com.expiryguard.app.ui.liquid.components.SegmentedTabs

/**
 * 通知设置页面
 *
 * 管理通知开关、提醒天数设置，并提供测试通知功能。
 *
 * 通知设置说明：
 * - 开启通知后，应用会监控清单到期情况
 * - 在清单到期前指定天数时，会在通知栏显示提醒
 * - 打开应用时，会自动显示待处理清单的数量通知
 * - 可退货清单和已过期清单会单独列出
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var systemNotificationsEnabled by remember {
        mutableStateOf(NotificationHelper.areNotificationsEnabled(context))
    }
    var requestForTest by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemNotificationsEnabled = NotificationHelper.areNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 通知权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        systemNotificationsEnabled = NotificationHelper.areNotificationsEnabled(context)
        if (granted) {
            if (requestForTest) {
                NotificationHelper.sendTestNotification(context)
                Toast.makeText(context, "测试通知已发送，请查看通知栏", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "通知权限已授予", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "需要通知权限才能发送通知", Toast.LENGTH_SHORT).show()
        }
        requestForTest = false
    }

    Scaffold(
        topBar = {
            GlassTopAppBar(
                title = { Text("通知设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ========== 说明区域 ==========
                SectionTitle("什么是通知设置？")

                PreferenceCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NotificationInfoRow(
                            icon = Icons.Default.Info,
                            text = "开启通知后，应用会在清单到期前指定天数时，通过系统通知栏提醒您"
                        )
                        NotificationInfoRow(
                            icon = Icons.Default.Info,
                            text = "打开应用时，会自动显示今日待处理清单的数量通知（含可退货和已过期）"
                        )
                        NotificationInfoRow(
                            icon = Icons.Default.Info,
                            text = "可退货清单根据保质期规则自动判断：1年以上提前45天、6个月~1年提前20天、3~6个月提前15天"
                        )
                    }
                }

                // ========== 通知开关 ==========
                SectionTitle("通知开关")

                PreferenceCard {
                    PreferenceRow(
                        icon = if (uiState.isNotificationEnabled)
                            Icons.Default.NotificationsActive
                        else
                            Icons.Default.NotificationsOff,
                        title = "到期提醒通知",
                        subtitle = when {
                            !uiState.isNotificationEnabled -> "已关闭"
                            !systemNotificationsEnabled -> "应用已开启，但系统通知被禁用"
                            else -> "已开启"
                        },
                        trailing = {
                            CompactSwitch(
                                checked = uiState.isNotificationEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setNotificationEnabled(enabled)
                                    if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!granted) {
                                            requestForTest = false
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                }
                            )
                        }
                    )
                }
                if (uiState.isNotificationEnabled && !systemNotificationsEnabled) {
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("打开系统通知设置")
                    }
                }

                // ========== 提醒天数设置 ==========
                SectionTitle("提前提醒天数")

                PreferenceCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "选择清单到期前多少天开始提醒您",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val reminderOptions = listOf(
                            1 to "1 天",
                            3 to "3 天",
                            7 to "7 天",
                            14 to "14 天",
                            30 to "30 天"
                        )
                        SegmentedTabs(
                            labels = reminderOptions.map { it.second },
                            selectedIndex = reminderOptions.indexOfFirst { it.first == uiState.reminderDays }.coerceAtLeast(0),
                            onSelected = { index ->
                                viewModel.changeReminderDays(reminderOptions[index].first)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ========== 测试通知按钮 ==========
                SectionTitle("测试通知")

                PreferenceCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "点击下方按钮发送一条测试通知，验证通知功能是否正常工作",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryButton(
                            onClick = {
                                val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                } else {
                                    true
                                }
                                if (hasPermission && systemNotificationsEnabled) {
                                    NotificationHelper.sendTestNotification(context)
                                    Toast.makeText(context, "测试通知已发送，请查看通知栏", Toast.LENGTH_SHORT).show()
                                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                                    requestForTest = true
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    Toast.makeText(context, "请先在系统设置中允许通知", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "发送测试通知",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NotificationInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
