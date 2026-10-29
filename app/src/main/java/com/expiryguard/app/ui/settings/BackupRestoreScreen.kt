package com.expiryguard.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expiryguard.app.data.backup.RestoreMode
import com.expiryguard.app.ui.components.GlassTopAppBar
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.liquid.components.GlassDialog
import com.expiryguard.app.ui.liquid.components.Meter
import com.expiryguard.app.ui.liquid.components.PreferenceCard
import com.expiryguard.app.ui.liquid.components.PrimaryButton
import com.expiryguard.app.ui.liquid.components.SecondaryButton
import com.expiryguard.app.ui.theme.Blue500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Purple500
import com.expiryguard.app.ui.theme.Red500

/**
 * 备份与恢复页面
 *
 * 使用液态玻璃风格，支持：
 * - 备份数据（含图片）到 .expiryguard 文件
 * - 从 .expiryguard 文件恢复数据
 *
 * @param onNavigateBack 返回上一页回调
 * @param viewModel 备份与恢复 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()
    val restoreProgress by viewModel.restoreProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val pendingRecoveryCode by viewModel.pendingRecoveryCode.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showOverwriteConfirm by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var backupPasswordConfirm by remember { mutableStateOf("") }
    var backupCredentialError by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var restoreRecoveryCode by remember { mutableStateOf("") }

    // 处理成功/错误消息显示
    LaunchedEffect(successMessage, errorMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // SAF 创建文档：备份目的地由用户明确选择，支持 API 26–28。
    val backupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.backupData(it, backupPassword.toCharArray())
            backupPassword = ""
            backupPasswordConfirm = ""
        }
    }

    // SAF 文件选择器 - 用于选择备份文件进行恢复
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassTopAppBar(
                title = {
                    Text(
                        "备份与恢复",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (errorMessage != null) Red500 else Green500,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { padding ->
        GradientBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ==================== 备份区域 ====================
                PreferenceCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 图标
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = Purple500,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "备份数据",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "将产品、回收站、分类、分组和图片打包；点击后通过系统文件选择器明确选择保存位置",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )

                        // 显示上次备份时间
                        AnimatedVisibility(
                            visible = lastBackupTime != null
                        ) {
                            Text(
                                text = "上次备份: $lastBackupTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 备份进度条
                        AnimatedVisibility(
                            visible = isBackingUp,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Meter(
                                    progress = backupProgress,
                                    accent = Purple500,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "正在备份... ${(backupProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Purple500
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        Text(
                            text = "新备份强制使用 AES-GCM 加密。密码至少 8 位；成功后还会生成只显示一次的恢复密钥，密码或恢复密钥均可跨设备恢复。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = backupPassword,
                            onValueChange = { backupPassword = it; backupCredentialError = null },
                            label = { Text("备份密码（至少 8 位）") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            enabled = !isBackingUp && !isRestoring,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = backupPasswordConfirm,
                            onValueChange = { backupPasswordConfirm = it; backupCredentialError = null },
                            label = { Text("确认备份密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            enabled = !isBackingUp && !isRestoring,
                            modifier = Modifier.fillMaxWidth()
                        )
                        backupCredentialError?.let {
                            Text(
                                text = it,
                                color = Red500,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // 备份按钮
                        PrimaryButton(
                            onClick = {
                                when {
                                    backupPassword.length < 8 -> {
                                        backupCredentialError = "请设置至少 8 位的备份密码"
                                    }
                                    backupPassword != backupPasswordConfirm -> {
                                        backupCredentialError = "两次输入的备份密码不一致"
                                    }
                                    else -> {
                                        backupCredentialError = null
                                        val timestamp = java.text.SimpleDateFormat(
                                            "yyyyMMdd_HHmmss",
                                            java.util.Locale.US
                                        ).format(java.util.Date())
                                        backupCreateLauncher.launch("ExpiryGuard_Backup_$timestamp.expiryguard")
                                    }
                                }
                            },
                            enabled = !isBackingUp && !isRestoring,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBackingUp) "备份中..." else "开始备份",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ==================== 恢复区域 ====================
                PreferenceCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 图标
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            tint = Blue500,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "恢复数据",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "从之前备份的 .expiryguard 文件恢复所有产品数据和图片",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 恢复模式选择
                        RestoreModeSelector(
                            selected = viewModel.restoreMode.collectAsState().value,
                            enabled = !isBackingUp && !isRestoring,
                            onSelect = viewModel::setRestoreMode
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 恢复进度条
                        AnimatedVisibility(
                            visible = isRestoring,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Meter(
                                    progress = restoreProgress,
                                    accent = Blue500,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "正在恢复... ${(restoreProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Blue500
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // 选择备份文件按钮
                        SecondaryButton(
                            onClick = {
                                if (viewModel.restoreMode.value == RestoreMode.OVERWRITE) {
                                    showOverwriteConfirm = true
                                } else {
                                    filePickerLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                                }
                            },
                            enabled = !isBackingUp && !isRestoring,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRestoring) "恢复中..." else "选择备份文件",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ==================== 警告提示 ====================
                PreferenceCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Red500.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "增量恢复按稳定 UUID 跳过重复记录并保留现有数据；覆盖恢复会在确认后替换全部记录。恢复在数据库事务中执行，失败不会留下半份数据。新备份使用密码和恢复密钥双重保护的加密容器，仍请勿分享给不可信对象。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                        )
                    }
                }

                // 底部留白
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showOverwriteConfirm) {
        GlassDialog(
            onDismissRequest = { showOverwriteConfirm = false },
            title = "确认覆盖恢复？",
            onConfirm = {
                showOverwriteConfirm = false
                filePickerLauncher.launch(arrayOf("application/octet-stream", "*/*"))
            },
            confirmText = "选择文件并覆盖",
            dismissText = "取消"
        ) {
            Text(
                text = "覆盖恢复将替换当前全部产品、分类、分组和回收站记录。建议先创建备份；恢复失败会回滚数据库。",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    pendingRestoreUri?.let { uri ->
        GlassDialog(
            onDismissRequest = {
                pendingRestoreUri = null
                restorePassword = ""
                restoreRecoveryCode = ""
            },
            title = "输入备份凭据",
            onConfirm = {
                val password = if (restorePassword.isEmpty()) null else restorePassword.toCharArray()
                viewModel.restoreData(
                    uri = uri,
                    password = password,
                    recoveryCode = restoreRecoveryCode.ifBlank { null }
                )
                pendingRestoreUri = null
                restorePassword = ""
                restoreRecoveryCode = ""
            },
            confirmText = "开始恢复",
            dismissText = "取消"
        ) {
            Text(
                text = "加密备份请输入备份密码或恢复密钥（任选其一）；旧版明文备份可全部留空。覆盖恢复仍会在凭据验证成功后执行。",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = restorePassword,
                onValueChange = { restorePassword = it },
                label = { Text("备份密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = restoreRecoveryCode,
                onValueChange = { restoreRecoveryCode = it },
                label = { Text("恢复密钥（可带连字符）") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    pendingRecoveryCode?.let { code ->
        GlassDialog(
            // 必须明确确认已保存，避免用户误触返回后永久失去恢复凭据。
            onDismissRequest = {},
            title = "立即保存恢复密钥",
            onConfirm = viewModel::acknowledgeRecoveryCode,
            confirmText = "我已安全保存",
            dismissText = null
        ) {
            Text(
                text = "此密钥只显示一次，不会写入应用设置或日志。忘记备份密码时可用它恢复；请离线保存，不要上传到聊天或云笔记。",
                style = MaterialTheme.typography.bodyMedium
            )
            SelectionContainer {
                Text(
                    text = formatRecoveryCode(code),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatRecoveryCode(code: String): String = code.chunked(4).joinToString("-")

/**
 * 恢复模式单选选择器
 *
 * 在增量恢复与覆盖恢复之间选择，使用选中态高亮边框 + 圆点指示。
 */
@Composable
private fun RestoreModeSelector(
    selected: RestoreMode,
    enabled: Boolean,
    onSelect: (RestoreMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RestoreMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isSelected) {
                            Blue500.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) Blue500 else Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = enabled) { onSelect(mode) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            color = if (isSelected) Blue500 else Color.Transparent,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .border(
                            border = BorderStroke(2.dp, if (isSelected) Blue500 else Blue500.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(9.dp)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = mode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
