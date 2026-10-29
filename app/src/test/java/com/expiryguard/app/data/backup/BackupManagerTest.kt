package com.expiryguard.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.expiryguard.app.data.db.AppDatabase
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** SAF/file URI、加密容器、图片和数据库事务的集成测试（Robolectric）。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupManagerTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var manager: BackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        manager = BackupManager(
            context,
            database,
            database.productDao(),
            database.categoryDao(),
            database.shelfLifeGroupDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun encryptedSafRoundTripPreservesUuidCategoryAndImage() = runBlocking {
        val categoryId = database.categoryDao().insert(CategoryEntity(name = "测试分类"))
        database.shelfLifeGroupDao().insert(
            ShelfLifeGroupEntity(
                name = "测试分组", description = "测试", minDays = 0, maxDays = 30,
                reminderThreshold = 3, colorHex = "#112233"
            )
        )
        val imageDir = File(context.filesDir, "product_images").apply { mkdirs() }
        val originalImage = File(imageDir, "original.jpg").apply { writeBytes(byteArrayOf(1, 3, 3, 7)) }
        val uuid = UUID.randomUUID().toString()
        database.productDao().insert(
            ProductEntity(
                uuid = uuid, name = "加密备份产品", categoryId = categoryId,
                photoPath = originalImage.absolutePath, expiryDate = 900L, shelfLifeDays = 12,
                notes = "含中文和备注"
            )
        )
        val backupFile = File(context.cacheDir, "roundtrip.expiryguard")
        val password = "a test password".toCharArray()
        val result = manager.createBackup(Uri.fromFile(backupFile), password)
        assertTrue(result.encrypted)
        assertNotNull(result.recoveryCode)
        backupFile.inputStream().use { assertTrue(BackupCrypto.hasEncryptedHeader(it)) }

        database.productDao().permanentDeleteAll()
        database.categoryDao().deleteAll()
        database.shelfLifeGroupDao().deleteAll()
        val restored = manager.restoreBackup(
            Uri.fromFile(backupFile), RestoreMode.OVERWRITE, password = password
        )
        assertEquals(1, restored.insertedProducts)
        val product = database.productDao().getAllProductsSnapshot().single()
        assertEquals(uuid, product.uuid)
        assertEquals("加密备份产品", product.name)
        assertEquals("测试分类", database.categoryDao().getAllCategoriesSnapshot().single().name)
        assertTrue(product.photoPath != originalImage.absolutePath)
        assertTrue(File(product.photoPath!!).isFile)
        assertEquals(1, database.shelfLifeGroupDao().getAllGroupsSnapshot().size)

        // 用恢复密钥再次增量恢复：UUID 去重，且不留下重复产品。
        val incremental = manager.restoreBackup(
            Uri.fromFile(backupFile), RestoreMode.INCREMENTAL,
            recoveryCode = result.recoveryCode
        )
        assertEquals(0, incremental.insertedProducts)
        assertEquals(1, incremental.skippedProducts)
        assertEquals(1, database.productDao().getAllProductsSnapshot().size)
    }

    @Test
    fun wrongPasswordFailsBeforeChangingExistingData() = runBlocking {
        val existingUuid = UUID.randomUUID().toString()
        database.productDao().insert(ProductEntity(uuid = existingUuid, name = "保留", expiryDate = 100L))
        val source = File(context.cacheDir, "source.expiryguard")
        manager.createBackup(Uri.fromFile(source), "correct password".toCharArray())

        assertBackupFailure {
            runBlocking {
                manager.restoreBackup(
                    Uri.fromFile(source), RestoreMode.OVERWRITE,
                    password = "incorrect password".toCharArray()
                )
            }
        }
        val products = database.productDao().getAllProductsSnapshot()
        assertEquals(1, products.size)
        assertEquals(existingUuid, products.single().uuid)
    }

    @Test
    fun legacyPlainZipAndZipSlipBehaviorRemainSafe() = runBlocking {
        database.productDao().insert(ProductEntity(name = "旧备份", expiryDate = 100L))
        val plain = File(context.cacheDir, "legacy.expiryguard")
        manager.createBackup(Uri.fromFile(plain))
        plain.inputStream().use { assertFalse(BackupCrypto.hasEncryptedHeader(it)) }
        database.productDao().permanentDeleteAll()
        manager.restoreBackup(Uri.fromFile(plain), RestoreMode.OVERWRITE)
        assertEquals("旧备份", database.productDao().getAllProductsSnapshot().single().name)

        val malicious = File(context.cacheDir, "zip-slip.expiryguard")
        ZipOutputStream(malicious.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("../outside.txt"))
            zip.write("not allowed".toByteArray())
            zip.closeEntry()
        }
        assertBackupFailure {
            runBlocking { manager.restoreBackup(Uri.fromFile(malicious), RestoreMode.INCREMENTAL) }
        }
        assertFalse(File(context.cacheDir.parentFile, "outside.txt").exists())
    }

    private fun assertBackupFailure(block: () -> Unit) {
        try {
            block()
            fail("Expected BackupException")
        } catch (_: BackupException) {
            // Expected.
        }
    }
}
