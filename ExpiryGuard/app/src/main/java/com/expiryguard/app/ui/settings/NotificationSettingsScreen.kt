package com.expiryguard.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.expiryguard.app.notification.NotificationHelper
import com.expiryguard.app.ui.components.GlassCard
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.theme.Blue500
import com.expiryguard.app.ui.theme.Gray500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Orange500

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

    // 通知权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            NotificationHelper.sendTestNotification(context)
            Toast.makeText(context, "测试通知已发送，请查看通知栏", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "需要通知权限才能发送通知", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ========== 说明区域 ==========
                Text(
                    text = "什么是通知设置？",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                GlassCard(
                    shape = RoundedCornerShape(12.dp),
                    elevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 说明项 1
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Blue500,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "开启通知后，应用会在清单到期前指定天数时，通过系统通知栏提醒您",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        // 说明项 2
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Blue500,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "打开应用时，会自动显示今日待处理清单的数量通知（含可退货和已过期）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        // 说明项 3
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Blue500,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "可退货清单根据保质期规则自动判断：1年以上提前45天、6个月~1年提前20天、3~6个月提前15天",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ========== 通知开关 ==========
                Text(
                    text = "通知开关",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (uiState.isNotificationEnabled) Blue500.copy(alpha = 0.1f)
                                    else Gray500.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isNotificationEnabled)
                                    Icons.Default.NotificationsActive
                                else
                                    Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (uiState.isNotificationEnabled) Blue500 else Gray500,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "到期提醒通知",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (uiState.isNotificationEnabled) "已开启" else "已关闭",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.isNotificationEnabled)
                                    Blue500
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        Switch(
                            checked = uiState.isNotificationEnabled,
                            onCheckedChange = { viewModel.toggleNotification() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ========== 提醒天数设置 ==========
                Text(
                    text = "提前提醒天数",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Text(
                    text = "选择清单到期前多少天开始提醒您",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val reminderOptions = listOf(
                    1 to "1 天",
                    3 to "3 天",
                    7 to "7 天",
                    14 to "14 天",
                    30 to "30 天"
                )

                reminderOptions.forEach { (days, label) ->
                    val isSelected = uiState.reminderDays == days
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.changeReminderDays(days) },
                        selected = isSelected,
                        shape = RoundedCornerShape(12.dp),
                        elevation = if (isSelected) 2.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) Orange500.copy(alpha = 0.1f)
                                        else Gray500.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (isSelected) Orange500 else Gray500,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Text(
                                        text = "当前选择",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Orange500
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ========== 测试通知按钮 ==========
                Text(
                    text = "测试通知",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Text(
                    text = "点击下方按钮发送一条测试通知，验证通知功能是否正常工作",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                        if (hasPermission) {
                            NotificationHelper.sendTestNotification(context)
                            Toast.makeText(context, "测试通知已发送，请查看通知栏", Toast.LENGTH_SHORT).show()
                        } else {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green500
                    )
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}