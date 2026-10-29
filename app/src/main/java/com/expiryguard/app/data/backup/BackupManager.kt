package com.expiryguard.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.expiryguard.app.data.db.AppDatabase
import com.expiryguard.app.data.db.dao.CategoryDao
import com.expiryguard.app.data.db.dao.ProductDao
import com.expiryguard.app.data.db.dao.ShelfLifeGroupDao
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** 当前备份格式；v1 没有 uuid，恢复时按记录位置生成稳定的兼容身份。 */
data class BackupData(
    val version: Int = 2,
    val products: List<ProductEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val shelfLifeGroups: List<ShelfLifeGroupEntity> = emptyList()
)

enum class RestoreMode(val label: String, val description: String) {
    INCREMENTAL("增量恢复", "按完整业务指纹跳过重复数据，保留现有内容"),
    OVERWRITE("覆盖恢复", "在单一事务中替换当前全部数据，失败自动回滚")
}

data class BackupResult(
    val uri: Uri,
    val productCount: Int,
    val imageCount: Int,
    val destinationLabel: String,
    val encrypted: Boolean = false,
    /** 仅在本次加密备份成功时返回；调用方负责一次性展示并随后清除。 */
    val recoveryCode: String? = null
)

data class RestoreResult(
    val insertedProducts: Int,
    val skippedProducts: Int,
    val insertedCategories: Int,
    val insertedGroups: Int,
    val missingImages: Int
)

/** 对 ZIP 条目做白名单校验，供恢复和 JVM 单元测试共用。 */
object BackupEntryValidator {
    private val imageEntry = Regex("images/[A-Za-z0-9][A-Za-z0-9._-]{0,127}")

    fun isValid(entryName: String): Boolean {
        return entryName == "data.json" || imageEntry.matches(entryName)
    }

    fun isSafeChild(root: File, target: File): Boolean {
        val canonicalRoot = root.canonicalFile
        val canonicalTarget = target.canonicalFile
        return canonicalTarget.path.startsWith(canonicalRoot.path + File.separator)
    }
}

/**
 * 负责备份快照、受限解压、图片暂存和事务恢复。
 *
 * 数据库与文件系统无法组成一个真正跨资源事务，因此采用“新图片先复制为唯一文件、
 * 数据库后提交”的方案：任何失败只删除本次新增文件，绝不覆盖或删除旧文件。
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val groupDao: ShelfLifeGroupDao
) {
    private val gson = Gson()
    private val operationMutex = Mutex()

    private suspend fun <T> ioSerialized(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        operationMutex.withLock { block() }
    }

    suspend fun createBackup(
        destinationUri: Uri,
        password: CharArray? = null,
        onProgress: (Float) -> Unit = {}
    ): BackupResult = ioSerialized {
        onProgress(0.05f)
        val snapshot = database.withTransaction {
            BackupData(
                version = BACKUP_VERSION,
                products = productDao.getAllProductsSnapshot(),
                categories = categoryDao.getAllCategoriesSnapshot(),
                shelfLifeGroups = groupDao.getAllGroupsSnapshot()
            )
        }
        onProgress(0.2f)

        val imageRoot = File(context.filesDir, IMAGE_DIR_NAME).canonicalFile
        val rootPrefix = imageRoot.path + File.separator
        val images = snapshot.products.mapNotNull { product ->
            val path = product.photoPath ?: return@mapNotNull null
            val file = File(path).canonicalFile
            // 不信任数据库/旧备份中的任意外部路径，防止把私有文件带入公开备份。
            if (file.isFile && file.path.startsWith(rootPrefix)) file else null
        }.distinctBy { it.absolutePath }
        onProgress(0.3f)

        val destination = openBackupDestination(destinationUri)
        val encrypted = password != null
        var temporaryZip: File? = null
        var recoveryCode: String? = null
        var completed = false
        try {
            if (encrypted) {
                // 先完成 ZIP，再流式加密到 SAF；不会把整个图片备份载入内存。
                val zipFile = File.createTempFile("backup_", ".zip", context.cacheDir)
                temporaryZip = zipFile
                zipFile.outputStream().buffered().use { output ->
                    writeBackupZip(snapshot, images, output, onProgress)
                }
                onProgress(0.94f)
                destination.output.use { output ->
                    recoveryCode = BackupCrypto.encrypt(zipFile, output, password).recoveryCode
                }
            } else {
                destination.output.use { output ->
                    writeBackupZip(snapshot, images, output, onProgress)
                }
            }
            destination.onSuccess()
            completed = true
            onProgress(1f)
            BackupResult(
                uri = destination.uri,
                productCount = snapshot.products.size,
                imageCount = images.size,
                destinationLabel = destination.destinationLabel,
                encrypted = encrypted,
                recoveryCode = recoveryCode
            )
        } finally {
            temporaryZip?.delete()
            if (!completed) destination.onFailure()
        }
    }

    private fun writeBackupZip(
        snapshot: BackupData,
        images: List<File>,
        output: OutputStream,
        onProgress: (Float) -> Unit
    ) {
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_DATA_FILE))
            // 不关闭 writer，否则会提前关闭整个 ZIP；这里只刷新当前条目。
            val writer = OutputStreamWriter(zip, Charsets.UTF_8)
            gson.toJson(snapshot, writer)
            writer.flush()
            zip.closeEntry()
            onProgress(0.45f)

            images.forEachIndexed { index, image ->
                zip.putNextEntry(ZipEntry("$BACKUP_IMAGES_DIR/${image.name}"))
                image.inputStream().buffered().use { input ->
                    input.copyTo(zip, DEFAULT_BUFFER_SIZE)
                }
                zip.closeEntry()
                onProgress(0.45f + 0.45f * (index + 1) / images.size.coerceAtLeast(1))
            }
        }
    }

    private data class BackupDestination(
        val uri: Uri,
        val output: OutputStream,
        val destinationLabel: String,
        val onSuccess: () -> Unit,
        val onFailure: () -> Unit
    )

    /** 由 SAF 提供目的地，避免隐式写入公共 Download，也兼容 API 26–28。 */
    private fun openBackupDestination(uri: Uri): BackupDestination {
        val resolver = context.contentResolver
        val output = resolver.openOutputStream(uri, "w")
            ?: throw BackupException("无法写入用户选择的备份位置")
        return BackupDestination(
            uri = uri,
            output = output,
            destinationLabel = "用户选择的位置",
            onSuccess = {},
            onFailure = {
                runCatching { output.close() }
                runCatching { resolver.delete(uri, null, null) }
            }
        )
    }

    suspend fun restoreBackup(
        uri: Uri,
        mode: RestoreMode,
        password: CharArray? = null,
        recoveryCode: String? = null,
        onProgress: (Float) -> Unit = {}
    ): RestoreResult = ioSerialized {
        val tempDir = File(context.cacheDir, "restore_${UUID.randomUUID()}")
        if (!tempDir.mkdirs()) throw BackupException("无法创建恢复暂存目录")
        val newImageFiles = mutableListOf<File>()

        try {
            onProgress(0.05f)
            val extractDir = File(tempDir, "extracted")
            if (!extractDir.mkdirs()) throw BackupException("无法创建解压目录")
            val resolver = context.contentResolver
            val encrypted = resolver.openInputStream(uri)?.use { input ->
                BackupCrypto.hasEncryptedHeader(input)
            } ?: throw BackupException("无法读取所选备份文件")
            if (encrypted) {
                val decryptedZip = File(tempDir, "payload.zip")
                resolver.openInputStream(uri)?.use { input ->
                    BackupCrypto.decryptToFile(input, decryptedZip, password, recoveryCode)
                } ?: throw BackupException("无法读取所选备份文件")
                decryptedZip.inputStream().buffered().use { input ->
                    extractBackupSafely(input, extractDir)
                }
            } else {
                // v1/v2 明文 ZIP 仅用于兼容旧备份；新备份由界面强制加密。
                resolver.openInputStream(uri)?.use { input ->
                    extractBackupSafely(input, extractDir)
                } ?: throw BackupException("无法读取所选备份文件")
            }
            onProgress(0.25f)

            val dataFile = File(extractDir, BACKUP_DATA_FILE)
            if (!dataFile.isFile || dataFile.length() > MAX_JSON_BYTES) {
                throw BackupException("备份缺少有效的 data.json 或文件过大")
            }
            val parsedBackup = try {
                gson.fromJson(dataFile.readText(Charsets.UTF_8), BackupData::class.java)
                    ?: throw BackupException("备份内容为空")
            } catch (e: BackupException) {
                throw e
            } catch (e: Exception) {
                throw BackupException("备份 JSON 格式无效或字段不兼容", e)
            }
            val backup = normalizeBackup(parsedBackup)
            validateBackup(backup)
            onProgress(0.4f)

            val appImageDir = File(context.filesDir, IMAGE_DIR_NAME)
            if (!appImageDir.exists() && !appImageDir.mkdirs()) {
                throw BackupException("无法创建图片目录")
            }
            val extractedImages = File(extractDir, BACKUP_IMAGES_DIR)
            val referencedNames = backup.products.mapNotNull { product ->
                product.photoPath?.let { File(it).name }
            }.toSet()
            val imageNameToNewPath = mutableMapOf<String, String>()

            referencedNames.sorted().forEach { name ->
                val source = File(extractedImages, name)
                if (source.isFile) {
                    val extension = source.extension.lowercase(Locale.US)
                        .takeIf { it in setOf("jpg", "jpeg", "png", "webp") }
                        ?: "jpg"
                    val target = File(appImageDir, "${UUID.randomUUID()}.$extension")
                    source.copyTo(target, overwrite = false)
                    newImageFiles += target
                    imageNameToNewPath[name] = target.absolutePath
                }
            }
            onProgress(0.55f)

            val transactionResult = database.withTransaction {
                restoreDatabase(backup, mode, imageNameToNewPath)
            }

            // 被增量去重跳过的记录不占用本次新复制的图片。
            val usedPaths = transactionResult.insertedProducts
                .mapNotNull { it.photoPath }
                .toSet()
            newImageFiles.filter { it.absolutePath !in usedPaths }.forEach { it.delete() }

            onProgress(1f)
            transactionResult.report
        } catch (e: CancellationException) {
            newImageFiles.forEach { it.delete() }
            throw e
        } catch (e: Exception) {
            newImageFiles.forEach { it.delete() }
            if (e is BackupException) throw e
            throw BackupException(e.message ?: "恢复失败", e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private data class DatabaseRestoreResult(
        val report: RestoreResult,
        val insertedProducts: List<ProductEntity>
    )

    private suspend fun restoreDatabase(
        backup: BackupData,
        mode: RestoreMode,
        imageNameToNewPath: Map<String, String>
    ): DatabaseRestoreResult {
        if (mode == RestoreMode.OVERWRITE) {
            productDao.permanentDeleteAll()
            categoryDao.deleteAll()
            groupDao.deleteAll()
        }

        val categoriesByName = categoryDao.getAllCategoriesSnapshot()
            .associateBy { it.name }
            .toMutableMap()
        val categoryIdMap = mutableMapOf<Long, Long>()
        var insertedCategories = 0
        backup.categories.forEach { category ->
            val existing = categoriesByName[category.name]
            val newId = if (existing != null) {
                existing.id
            } else {
                categoryDao.insert(category.copy(id = 0)).also {
                    insertedCategories++
                    categoriesByName[category.name] = category.copy(id = it)
                }
            }
            categoryIdMap[category.id] = newId
        }

        val groupsByName = groupDao.getAllGroupsSnapshot()
            .associateBy { it.name }
            .toMutableMap()
        var insertedGroups = 0
        backup.shelfLifeGroups.forEach { group ->
            if (groupsByName[group.name] == null) {
                val newId = groupDao.insert(group.copy(id = 0))
                insertedGroups++
                groupsByName[group.name] = group.copy(id = newId)
            }
        }

        val updatedProducts = backup.products.map { product ->
            val oldImageName = product.photoPath?.let { File(it).name }
            product.copy(
                id = 0,
                photoPath = oldImageName?.let { imageNameToNewPath[it] },
                // 未找到明确映射时不得沿用另一台设备上的数字 ID。
                categoryId = product.categoryId?.let { categoryIdMap[it] }
            )
        }

        val identityOf: (ProductEntity) -> String = if (backup.version == 1) {
            { product -> "legacy:${productFingerprint(product)}" }
        } else {
            { product -> "uuid:${product.uuid}" }
        }
        val existingIdentities = if (mode == RestoreMode.INCREMENTAL) {
            productDao.getAllProductsSnapshot().map(identityOf).toMutableSet()
        } else {
            mutableSetOf()
        }
        val productsToInsert = if (mode == RestoreMode.INCREMENTAL) {
            // v2 使用稳定 UUID；v1 仅为旧备份保留完整业务指纹兼容。
            updatedProducts.filter { existingIdentities.add(identityOf(it)) }
        } else {
            updatedProducts
        }
        if (productsToInsert.isNotEmpty()) {
            productDao.insertAll(productsToInsert)
        }

        val missingImages = backup.products.count { product ->
            val oldName = product.photoPath?.let { File(it).name }
            oldName != null && imageNameToNewPath[oldName] == null
        }
        return DatabaseRestoreResult(
            report = RestoreResult(
                insertedProducts = productsToInsert.size,
                skippedProducts = updatedProducts.size - productsToInsert.size,
                insertedCategories = insertedCategories,
                insertedGroups = insertedGroups,
                missingImages = missingImages
            ),
            insertedProducts = productsToInsert
        )
    }

    private fun extractBackupSafely(input: InputStream, extractDir: File) {
        val seenEntries = mutableSetOf<String>()
        var totalBytes = 0L
        var entryCount = 0

        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw BackupException("备份条目过多")
                }
                val name = entry.name
                if (!BackupEntryValidator.isValid(name) || !seenEntries.add(name)) {
                    throw BackupException("备份包含非法或重复条目：$name")
                }
                val target = File(extractDir, name)
                if (!BackupEntryValidator.isSafeChild(extractDir, target)) {
                    throw BackupException("备份包含越界路径")
                }
                target.parentFile?.mkdirs()
                val entryLimit = if (name == BACKUP_DATA_FILE) MAX_JSON_BYTES else MAX_IMAGE_BYTES
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var entryBytes = 0L
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        entryBytes += read
                        totalBytes += read
                        if (entryBytes > entryLimit || totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                            throw BackupException("备份解压后大小超过安全限制")
                        }
                        output.write(buffer, 0, read)
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun normalizeBackup(backup: BackupData): BackupData {
        val normalizedProducts = backup.products.mapIndexed { index, product ->
            val rawUuid: String? = runCatching { product.uuid }.getOrNull()
            val uuid = rawUuid?.takeIf { it.isNotBlank() } ?: if (backup.version == 1) {
                UUID.nameUUIDFromBytes(
                    "legacy:$index:${productFingerprint(product)}".toByteArray(Charsets.UTF_8)
                ).toString()
            } else {
                ""
            }
            product.copy(uuid = uuid)
        }
        return backup.copy(products = normalizedProducts)
    }

    private fun validateBackup(backup: BackupData) {
        if (backup.version !in setOf(1, BACKUP_VERSION)) {
            throw BackupException("不支持的备份格式版本：${backup.version}")
        }
        if (backup.products.size > MAX_RECORDS ||
            backup.categories.size > MAX_RECORDS ||
            backup.shelfLifeGroups.size > MAX_RECORDS
        ) {
            throw BackupException("备份记录数超过安全限制")
        }
        try {
            backup.products.forEach { product ->
                require(product.name.length <= 500)
                require((product.notes?.length ?: 0) <= 20_000)
                require((product.photoPath?.length ?: 0) <= 4096)
                require(product.shelfLifeDays >= 0)
                require(runCatching { UUID.fromString(product.uuid) }.isSuccess)
            }
            require(backup.products.map { it.uuid }.toSet().size == backup.products.size) {
                "产品 uuid 必须唯一"
            }
            backup.categories.forEach { category ->
                require(category.name.isNotBlank() && category.name.length <= 200)
            }
            backup.shelfLifeGroups.forEach { group ->
                require(group.name.isNotBlank() && group.name.length <= 200)
                require(group.minDays >= 0 && group.maxDays > group.minDays)
                require(group.reminderThreshold >= 0)
                require(COLOR_REGEX.matches(group.colorHex))
            }
        } catch (e: Exception) {
            throw BackupException("备份字段校验失败", e)
        }
    }

    /** 排除本地自增 ID、图片绝对路径和时间戳，避免原实现的同名同日期误去重。 */
    private fun productFingerprint(product: ProductEntity): String {
        val input = listOf(
            product.name,
            product.barcode.orEmpty(),
            product.categoryId?.toString().orEmpty(),
            product.productionDate?.toString().orEmpty(),
            product.expiryDate.toString(),
            product.shelfLifeDays.toString(),
            product.notes.orEmpty(),
            product.deletedAt?.toString().orEmpty(),
            product.isCompleted.toString(),
            product.completedAt?.toString().orEmpty(),
            product.completedType.orEmpty()
        ).joinToString(separator = "\u001f")
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val BACKUP_VERSION = 2
        const val IMAGE_DIR_NAME = "product_images"
        const val BACKUP_DATA_FILE = "data.json"
        const val BACKUP_IMAGES_DIR = "images"
        const val MAX_JSON_BYTES = 8L * 1024 * 1024
        const val MAX_IMAGE_BYTES = 32L * 1024 * 1024
        const val MAX_TOTAL_UNCOMPRESSED_BYTES = 1024L * 1024 * 1024
        const val MAX_ZIP_ENTRIES = 10_000
        const val MAX_RECORDS = 50_000
        val COLOR_REGEX = Regex("#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?")
    }
}

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
