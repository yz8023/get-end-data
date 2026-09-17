package com.expiryguard.app.ui.product

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.expiryguard.app.ui.theme.Gray500
import com.expiryguard.app.ui.theme.Red500
import com.expiryguard.app.util.DateUtils
import java.io.File

/**
 * 编辑清单页面
 *
 * 支持编辑清单名称、图片、到期日期、保质期分组、备注等字段
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    productId: Long,
    onNavigateBack: () -> Unit = {},
    viewModel: EditProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 加载清单数据
    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    // 保存成功后返回
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.pickImage(it) }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑清单") },
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            GradientBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // ========== 清单名称 ==========
                    SectionHeader(step = "1", title = "清单名称", isCompleted = uiState.name.isNotBlank())
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = { viewModel.updateName(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            placeholder = { Text("输入清单名称") },
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

                    // ========== 清单图片 ==========
                    SectionHeader(
                        step = "2",
                        title = "清单图片",
                        isCompleted = uiState.photoPath != null
                    )
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        PhotoEditSection(
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
                            onPickFromGallery = { galleryLauncher.launch("image/*") },
                            onRemovePhoto = { viewModel.pickImage(Uri.EMPTY) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ========== 到期日期 ==========
                    SectionHeader(
                        step = "3",
                        title = "到期日期",
                        isCompleted = uiState.expiryDate != null
                    )
                    DateInputField(
                        label = "到期日期",
                        hint = "如 2019.1.1 或 2019年1月1日",
                        value = uiState.expiryDate,
                        accentColor = Red500,
                        onDateSelected = { date -> viewModel.updateExpiryDate(date) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ========== 保质期分组 ==========
                    SectionHeader(
                        step = "4",
                        title = "保质期分组",
                        isCompleted = uiState.shelfLifeGroup != null
                    )
                    Text(
                        text = "选择清单对应的保质期区间",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    ShelfLifeGroupGrid(
                        groups = uiState.shelfLifeGroups,
                        selectedGroup = uiState.shelfLifeGroup,
                        onGroupSelected = { viewModel.selectShelfLifeGroup(it) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ========== 分类选择 ==========
                    if (uiState.categories.isNotEmpty()) {
                        SectionHeader(
                            step = "5",
                            title = "分类",
                            isCompleted = uiState.categoryId != null
                        )
                        CategorySelectionGrid(
                            categories = uiState.categories,
                            selectedCategoryId = uiState.categoryId,
                            onCategorySelected = { viewModel.updateCategory(it) }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ========== 备注 ==========
                    SectionHeader(
                        step = "6",
                        title = "备注（可选）",
                        isCompleted = uiState.notes.isNotBlank()
                    )
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.notes,
                            onValueChange = { viewModel.updateNotes(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            placeholder = { Text("添加备注信息...") },
                            minLines = 3,
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 保存按钮
                    Button(
                        onClick = { viewModel.saveProduct() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isSaving && uiState.expiryDate != null && uiState.name.isNotBlank(),
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
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "保存修改",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
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
 * 图片编辑区域
 */
@Composable
private fun PhotoEditSection(
    photoPath: String?,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        if (photoPath != null && photoPath.isNotEmpty()) {
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
                    .clip(CircleShape)
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

/**
 * 分类选择网格
 */
@Composable
private fun CategorySelectionGrid(
    categories: List<com.expiryguard.app.data.db.entity.CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val chunked = categories.chunked(3)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { category ->
                    val isSelected = category.id == selectedCategoryId
                    Card(
                        onClick = { onCategorySelected(category.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 4.dp else 2.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}