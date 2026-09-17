package com.expiryguard.app.ui.product

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.expiryguard.app.ui.components.DateInputField
import com.expiryguard.app.ui.components.GlassCard
import com.expiryguard.app.ui.components.GradientBackground
import com.expiryguard.app.ui.theme.Blue500
import com.expiryguard.app.ui.theme.Gray500
import com.expiryguard.app.ui.theme.Green500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.util.DateUtils
import java.io.File

/**
 * 添加清单页面
 *
 * 三步流程：拍照 → 选到期日 → 选分组
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    photoPath: String? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToProductDetail: (Long) -> Unit = {},
    onNavigateToGroupManagement: (() -> Unit)? = null,
    viewModel: AddProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 设置初始图片路径
    LaunchedEffect(photoPath) {
        if (photoPath != null) {
            viewModel.setPhotoPath(photoPath)
        }
    }

    // 保存成功后跳转
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            onNavigateBack()
        }
    }

    // 错误提示
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // 相机拍照 Launcher
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.pickImage(cameraImageUri!!)
        }
    }

    // 相册选择 Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.pickImage(it) }
    }

    // 相机权限 Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", photoFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    // 自定义天数输入
    var customDaysText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加清单") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("取消")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // ========== 第一步：清单名称（可选） ==========
                SectionHeader(
                    step = "1",
                    title = "清单名称",
                    isCompleted = false
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.updateName(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        placeholder = { Text("可选，如「酸奶」「洗发水」") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ========== 第二步：拍照 ==========
                SectionHeader(
                    step = "2",
                    title = "拍照",
                    isCompleted = uiState.photoPath != null
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    PhotoSection(
                        photoPath = uiState.photoPath,
                        onTakePhoto = {
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCameraPermission) {
                                val photoFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
                                val uri = FileProvider.getUriForFile(
                                    context, "${context.packageName}.fileprovider", photoFile
                                )
                                cameraImageUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onPickFromGallery = {
                            galleryLauncher.launch("image/*")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ========== 第二步：选到期日期 ==========
                SectionHeader(
                    step = "3",
                    title = "选择到期日",
                    isCompleted = uiState.expiryDate != null
                )

                DateInputField(
                    label = "到期日期",
                    hint = "如 1.1 或 1月1日（年份默认今年）",
                    value = uiState.expiryDate,
                    accentColor = Red500,
                    onDateSelected = { date -> viewModel.updateExpiryDate(date) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ========== 第三步：到期计算（生产日期 + 保质期） ==========
                SectionHeader(
                    step = "4",
                    title = "到期计算（可选）",
                    isCompleted = uiState.productionDate != null && uiState.expiryDate != null
                )

                Text(
                    text = "选择生产日期和保质期时长，自动计算到期日",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 生产日期选择
                DateInputField(
                    label = "生产日期",
                    hint = "如 1.1 或 1月1日（年份默认今年）",
                    value = uiState.productionDate,
                    accentColor = Blue500,
                    onDateSelected = { date -> viewModel.updateProductionDate(date) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 保质期预设选择
                Text(
                    text = "选择保质期时长",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ShelfLifePresetGrid(
                    presets = defaultShelfLifePresets,
                    selectedPreset = uiState.shelfLifeDays.takeIf { it > 0 }?.let { days ->
                        defaultShelfLifePresets.find { it.days == days }
                    },
                    onPresetSelected = { preset ->
                        viewModel.calculateExpiryFromPreset(preset)
                        customDaysText = ""
                    }
                )

                // 自定义天数输入
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customDaysText,
                        onValueChange = { newValue ->
                            // 只允许输入数字
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                customDaysText = newValue
                            }
                        },
                        label = { Text("自定义天数") },
                        placeholder = { Text("例如：21") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            val days = customDaysText.toIntOrNull()
                            if (days != null && days > 0) {
                                viewModel.calculateExpiryFromCustomDays(days)
                            }
                        },
                        enabled = customDaysText.isNotEmpty() && customDaysText.toIntOrNull()?.let { it > 0 } == true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "确认",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("确认")
                    }
                }

                // 计算结果显示
                if (uiState.productionDate != null && uiState.expiryDate != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        selected = true
                    ) {
                        val today = DateUtils.todayTimestamp()
                        val daysLeft = DateUtils.daysBetween(today, uiState.expiryDate!!)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Green500,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (daysLeft >= 0) {
                                        "到期日：${DateUtils.formatDate(uiState.expiryDate!!)}（剩余${daysLeft}天）"
                                    } else {
                                        "到期日：${DateUtils.formatDate(uiState.expiryDate!!)}（已过期${-daysLeft}天）"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (daysLeft >= 0) Green500 else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "保质期 ${uiState.shelfLifeDays} 天",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ========== 第四步：选保质期分组 ==========
                SectionHeader(
                    step = "5",
                    title = "选择保质期分组",
                    isCompleted = uiState.shelfLifeGroup != null
                )

                Text(
                    text = "选择清单对应的保质期区间，自动计算到期日期",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 分组选择网格 - 使用数据库动态分组
                ShelfLifeGroupGrid(
                    groups = uiState.shelfLifeGroups,
                    selectedGroup = uiState.shelfLifeGroup,
                    onGroupSelected = { group ->
                        viewModel.selectShelfLifeGroup(group)
                    }
                )

                // 管理分组链接
                if (onNavigateToGroupManagement != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onNavigateToGroupManagement
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("管理分组", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 保存按钮
                Button(
                    onClick = { viewModel.saveProduct() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !uiState.isSaving && uiState.expiryDate != null,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "保存清单",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 保质期预设网格
 *
 * 每行 4 个预设选项，以 FilterChip 形式展示。
 */
@Composable
private fun ShelfLifePresetGrid(
    presets: List<ShelfLifePreset>,
    selectedPreset: ShelfLifePreset?,
    onPresetSelected: (ShelfLifePreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val chunked = presets.chunked(4)
        chunked.forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPresets.forEach { preset ->
                    val isSelected = selectedPreset?.days == preset.days
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPresetSelected(preset) },
                        label = {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Blue500.copy(alpha = 0.15f),
                            selectedLabelColor = Blue500
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                // 如果当前行不足4个，用空白占位
                repeat(4 - rowPresets.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 步骤标题组件
 */
@Composable
private fun SectionHeader(
    step: String,
    title: String,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已完成",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isCompleted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 保质期分组选择网格
 *
 * 使用数据库动态分组列表，动态渲染每行最多3个分组
 */
@Composable
fun ShelfLifeGroupGrid(
    groups: List<ShelfLifeGroupUi>,
    selectedGroup: ShelfLifeGroupUi?,
    onGroupSelected: (ShelfLifeGroupUi) -> Unit
) {
    if (groups.isEmpty()) {
        Text(
            text = "暂无分组",
            style = MaterialTheme.typography.bodyMedium,
            color = Gray500,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 每行最多显示3个分组，动态计算行数
        val chunked = groups.chunked(3)
        chunked.forEach { rowGroups ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowGroups.forEach { group ->
                    GroupChip(
                        group = group,
                        isSelected = selectedGroup?.id == group.id,
                        onClick = { onGroupSelected(group) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 如果当前行不足3个，用空白占位
                repeat(3 - rowGroups.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GroupChip(
    group: ShelfLifeGroupUi,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupColor = try {
        Color(android.graphics.Color.parseColor(group.colorHex))
    } catch (e: Exception) {
        Color(0xFFF97316)
    }

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) groupColor.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) groupColor
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "border"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray500
                )
            }
        }
    }
}

/**
 * 拍照区域组件
 */
@Composable
private fun PhotoSection(
    photoPath: String?,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoPath)
                    .size(1080)
                    .build(),
                contentDescription = "清单图片预览",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "拍照",
                    modifier = Modifier.size(48.dp),
                    tint = Gray500
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击拍照或从相册选择",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 操作按钮（右下角）
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onPickFromGallery,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "从相册选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onTakePhoto,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "拍照",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}