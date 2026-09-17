package com.expiryguard.app.ui.settings

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity
import com.expiryguard.app.data.repository.ProductRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * 备份数据包装类，包含产品、分类和保质期分组
 */
data class BackupData(
    val version: Int = 1,
    val products: List<ProductEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val shelfLifeGroups: List<ShelfLifeGroupEntity> = emptyList()
)

/**
 * 恢复模式枚举
 */
enum class RestoreMode(val label: String, val description: String) {
    INCREMENTAL("增量恢复", "验证哈希值，跳过已存在的重复数据"),
    OVERWRITE("覆盖恢复", "清空所有现有数据后恢复备份")
}

/**
 * 备份与恢复页面 ViewModel
 *
 * 支持将产品数据（含图片、分类、分组）打包为 .expiryguard 文件备份到 Download 目录，
 * 以及从 .expiryguard 文件恢复数据（支持增量/覆盖两种模式）。
 */
@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val repository: ProductRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /** 是否正在备份 */
    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    /** 是否正在恢复 */
    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    /** 备份进度 (0.0 ~ 1.0) */
    private val _backupProgress = MutableStateFlow(0f)
    val backupProgress: StateFlow<Float> = _backupProgress.asStateFlow()

    /** 恢复进度 (0.0 ~ 1.0) */
    private val _restoreProgress = MutableStateFlow(0f)
    val restoreProgress: StateFlow<Float> = _restoreProgress.asStateFlow()

    /** 错误消息 */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 成功消息 */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /** 上次备份时间 */
    private val _lastBackupTime = MutableStateFlow<String?>(null)
    val lastBackupTime: StateFlow<String?> = _lastBackupTime.asStateFlow()

    /** 当前选择的恢复模式 */
    private val _restoreMode = MutableStateFlow(RestoreMode.INCREMENTAL)
    val restoreMode: StateFlow<RestoreMode> = _restoreMode.asStateFlow()

    /** 备份文件路径（用于返回给调用方） */
    private val _backupFilePath = MutableStateFlow<String?>(null)
    val backupFilePath: StateFlow<String?> = _backupFilePath.asStateFlow()

    private val gson = Gson()
    private val prefs = appContext.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val BACKUP_EXTENSION = ".expiryguard"
        private const val IMAGE_DIR_NAME = "product_images"
        private const val BACKUP_DATA_FILE = "data.json"
        private const val BACKUP_IMAGES_DIR = "images"
        private const val PREF_LAST_BACKUP_TIME = "last_backup_time"
    }

    init {
        // 恢复上次备份时间
        val savedTime = prefs.getString(PREF_LAST_BACKUP_TIME, null)
        _lastBackupTime.value = savedTime
    }

    /**
     * 设置恢复模式
     */
    fun setRestoreMode(mode: RestoreMode) {
        _restoreMode.value = mode
    }

    /**
     * 计算产品指纹（用于增量恢复去重）
     * 基于名称 + 到期日期生成哈希值
     */
    private fun productFingerprint(product: ProductEntity): String {
        val input = "${product.name}|${product.expiryDate}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * 执行备份操作
     *
     * 流程：获取产品+分类+分组数据 -> 序列化为JSON -> 收集图片 -> 打包为ZIP -> 保存到Download
     */
    fun backupData() {
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupProgress.value = 0f
            _errorMessage.value = null
            _successMessage.value = null
            _backupFilePath.value = null

            var tempDir: File? = null

            try {
                // 1. 获取所有产品、分类、分组数据
                val products = repository.getAllActiveProducts().first()
                _backupProgress.value = 0.1f

                val categories = repository.getAllCategories().first()
                _backupProgress.value = 0.15f

                val groups = repository.getAllGroups().first()
                _backupProgress.value = 0.2f

                // 2. 构建备份数据对象并序列化
                val backupData = BackupData(
                    version = 1,
                    products = products,
                    categories = categories,
                    shelfLifeGroups = groups
                )
                val jsonContent = gson.toJson(backupData)
                _backupProgress.value = 0.3f

                // 3. 收集所有存在的图片文件路径
                val imagePaths = products.mapNotNull { it.photoPath }
                    .filter { path -> File(path).exists() }
                _backupProgress.value = 0.35f

                // 4. 创建临时目录
                val timestamp = System.currentTimeMillis()
                tempDir = File(appContext.cacheDir, "backup_temp_$timestamp")
                tempDir.mkdirs()
                val imagesDir = File(tempDir, BACKUP_IMAGES_DIR)
                imagesDir.mkdirs()

                // 5. 写入 data.json
                File(tempDir, BACKUP_DATA_FILE).writeText(jsonContent)
                _backupProgress.value = 0.45f

                // 6. 复制图片文件
                if (imagePaths.isNotEmpty()) {
                    imagePaths.forEachIndexed { index, path ->
                        val imageFile = File(path)
                        if (imageFile.exists()) {
                            imageFile.copyTo(
                                File(imagesDir, imageFile.name),
                                overwrite = true
                            )
                        }
                        _backupProgress.value = 0.45f + (0.25f * (index + 1) / imagePaths.size)
                    }
                }
                _backupProgress.value = 0.7f

                // 7. 创建备份文件名并打包为 zip
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date())
                val backupFileName = "ExpiryGuard_Backup_$dateStr$BACKUP_EXTENSION"
                val zipFile = File(appContext.cacheDir, backupFileName)

                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    tempDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val entryName = file.relativeTo(tempDir).path
                            zos.putNextEntry(ZipEntry(entryName))
                            file.inputStream().use { input ->
                                input.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
                _backupProgress.value = 0.85f

                // 8. 通过 MediaStore 保存到 Download 目录
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, backupFileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = appContext.contentResolver
                val downloadUri = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                if (downloadUri != null) {
                    resolver.openOutputStream(downloadUri)?.use { outputStream ->
                        zipFile.inputStream().use { input ->
                            input.copyTo(outputStream)
                        }
                    }

                    // 清除 IS_PENDING 标记，使文件对其他应用可见
                    val updateValues = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    resolver.update(downloadUri, updateValues, null, null)

                    _backupFilePath.value = downloadUri.toString()
                } else {
                    // 降级：直接保存到缓存目录
                    _backupFilePath.value = zipFile.absolutePath
                }

                _backupProgress.value = 1f

                // 记录备份时间
                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date())
                prefs.edit().putString(PREF_LAST_BACKUP_TIME, nowStr).apply()
                _lastBackupTime.value = nowStr

                _successMessage.value = "备份完成，已保存到 Download 目录"
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "备份失败: ${e.message ?: "未知错误"}"
            } finally {
                // 清理临时目录
                try {
                    tempDir?.deleteRecursively()
                } catch (_: Exception) {}

                _isBackingUp.value = false
            }
        }
    }

    /**
     * 执行恢复操作
     *
     * @param uri 备份文件 Uri（通过 SAF 选择）
     *
     * 流程：
     * 1. 读取 .expiryguard 备份文件并解压
     * 2. 解析 data.json 获取备份数据（产品+分类+分组）
     * 3. 根据恢复模式处理：
     *    - 覆盖模式：清空所有现有数据后恢复
     *    - 增量模式：验证哈希值，跳过已存在的重复数据
     * 4. 恢复图片文件
     * 5. 批量插入数据
     */
    fun restoreData(uri: Uri) {
        viewModelScope.launch {
            _isRestoring.value = true
            _restoreProgress.value = 0f
            _errorMessage.value = null
            _successMessage.value = null

            var tempDir: File? = null

            try {
                _restoreProgress.value = 0.05f

                // 1. 创建临时目录
                val timestamp = System.currentTimeMillis()
                tempDir = File(appContext.cacheDir, "restore_temp_$timestamp")
                tempDir.mkdirs()

                // 2. 将 SAF Uri 内容复制到临时文件
                val tempZip = File(tempDir, "backup$BACKUP_EXTENSION")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    tempZip.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("无法读取备份文件")
                _restoreProgress.value = 0.1f

                // 3. 解压到临时目录
                val extractDir = File(tempDir, "extracted")
                extractDir.mkdirs()
                ZipInputStream(tempZip.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val targetFile = File(extractDir, entry.name)
                        if (entry.isDirectory) {
                            targetFile.mkdirs()
                        } else {
                            targetFile.parentFile?.mkdirs()
                            targetFile.outputStream().use { output ->
                                zis.copyTo(output)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                _restoreProgress.value = 0.2f

                // 4. 解析 data.json
                val dataFile = File(extractDir, BACKUP_DATA_FILE)
                if (!dataFile.exists()) {
                    throw Exception("备份文件格式无效：缺少 data.json")
                }
                val dataJson = dataFile.readText()
                val backupDataType = object : TypeToken<BackupData>() {}.type
                val backupData: BackupData = gson.fromJson(dataJson, backupDataType)
                if (backupData == null) {
                    throw Exception("备份文件数据解析失败")
                }
                _restoreProgress.value = 0.3f

                val mode = _restoreMode.value

                // 5. 根据恢复模式处理数据
                when (mode) {
                    RestoreMode.OVERWRITE -> {
                        // 覆盖模式：清空所有现有数据
                        repository.permanentDeleteAllActive()
                        repository.deleteAllCategories()
                        repository.deleteAllGroups()
                        _restoreProgress.value = 0.35f
                    }
                    RestoreMode.INCREMENTAL -> {
                        // 增量模式：不需要清空，后面会去重
                        _restoreProgress.value = 0.35f
                    }
                }

                // 6. 恢复分类和分组（覆盖模式下已清空，增量模式下直接插入）
                if (backupData.categories.isNotEmpty()) {
                    repository.insertAllCategories(
                        backupData.categories.map { it.copy(id = 0) }
                    )
                }
                if (backupData.shelfLifeGroups.isNotEmpty()) {
                    repository.insertAllGroups(
                        backupData.shelfLifeGroups.map { it.copy(id = 0) }
                    )
                }
                _restoreProgress.value = 0.4f

                // 7. 恢复图片文件
                val imagesDir = File(extractDir, BACKUP_IMAGES_DIR)
                val appImageDir = File(appContext.filesDir, IMAGE_DIR_NAME)
                appImageDir.mkdirs()

                val imageNameToNewPath = mutableMapOf<String, String>()
                if (imagesDir.exists()) {
                    val imageFiles = imagesDir.listFiles() ?: emptyArray()
                    imageFiles.forEachIndexed { index, imageFile ->
                        if (imageFile.isFile) {
                            val newFile = File(appImageDir, imageFile.name)
                            imageFile.copyTo(newFile, overwrite = true)
                            imageNameToNewPath[imageFile.name] = newFile.absolutePath
                        }
                        _restoreProgress.value = 0.4f + (0.1f * (index + 1) / (imageFiles.size.coerceAtLeast(1)))
                    }
                }
                _restoreProgress.value = 0.5f

                // 8. 增量模式：计算现有产品哈希值用于去重
                val existingFingerprints = if (mode == RestoreMode.INCREMENTAL) {
                    val existingProducts = repository.getAllActiveProducts().first()
                    existingProducts.map { productFingerprint(it) }.toSet()
                } else {
                    emptySet()
                }
                _restoreProgress.value = 0.55f

                // 9. 更新产品数据中的图片路径，重置 ID
                val updatedProducts = backupData.products.map { product ->
                    val newPhotoPath = if (product.photoPath != null) {
                        val oldFile = File(product.photoPath)
                        imageNameToNewPath[oldFile.name] ?: product.photoPath
                    } else {
                        null
                    }
                    product.copy(
                        id = 0,
                        photoPath = newPhotoPath
                    )
                }

                // 10. 过滤并插入产品数据
                val productsToInsert = if (mode == RestoreMode.INCREMENTAL) {
                    val before = updatedProducts.size
                    val filtered = updatedProducts.filter {
                        !existingFingerprints.contains(productFingerprint(it))
                    }
                    val skipped = before - filtered.size
                    if (skipped > 0) {
                        _successMessage.value = "增量恢复完成：新增 ${filtered.size} 条，跳过 ${skipped} 条重复数据"
                    }
                    filtered
                } else {
                    updatedProducts
                }

                if (productsToInsert.isNotEmpty()) {
                    repository.insertAll(productsToInsert)
                }
                _restoreProgress.value = 1f

                // 设置成功消息（增量模式可能已经设置了）
                if (mode == RestoreMode.OVERWRITE) {
                    _successMessage.value = "覆盖恢复完成，共恢复 ${productsToInsert.size} 条记录"
                } else if (_successMessage.value == null) {
                    _successMessage.value = "恢复完成，共恢复 ${productsToInsert.size} 条记录"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "恢复失败: ${e.message ?: "未知错误"}"
            } finally {
                // 清理临时目录
                try {
                    tempDir?.deleteRecursively()
                } catch (_: Exception) {}

                _isRestoring.value = false
            }
        }
    }

    /**
     * 导出数据为 CSV 格式
     *
     * @return 导出文件路径，失败返回 null
     */
    suspend fun exportDataAsCsv(): String? {
        return try {
            val products = repository.getAllActiveProducts().first()
            val csvContent = buildString {
                appendLine("名称,条形码,到期日期,保质期(天),备注,创建时间")
                products.forEach { product ->
                    appendLine(
                        "${product.name}," +
                        "${product.barcode ?: ""}," +
                        "${product.expiryDate}," +
                        "${product.shelfLifeDays}," +
                        "${product.notes?.replace(",", "，") ?: ""}," +
                        "${product.createdAt}"
                    )
                }
            }
            val file = File(appContext.cacheDir, "expiryguard_export.csv")
            file.writeText(csvContent)
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 导出数据为 JSON 格式
     *
     * @return 导出文件路径，失败返回 null
     */
    suspend fun exportDataAsJson(): String? {
        return try {
            val products = repository.getAllActiveProducts().first()
            val jsonContent = gson.toJson(products)
            val file = File(appContext.cacheDir, "expiryguard_export.json")
            file.writeText(jsonContent)
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 清除消息（成功/错误）
     */
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}