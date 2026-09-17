package com.expiryguard.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expiryguard.app.data.db.dao.CategoryDao
import com.expiryguard.app.data.db.dao.ProductDao
import com.expiryguard.app.data.db.dao.ShelfLifeGroupDao
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Room 数据库，version=4，包含产品和分类、保质期分组三张表
 *
 * 版本历史：
 * v1 - 初始版本
 * v2 - 新增 isCompleted 字段（ProductEntity）
 * v3 - 新增 shelf_life_groups 表（ShelfLifeGroupEntity）
 * v4 - 新增 completedAt 字段（ProductEntity），用于区分今日/历史已完成
 */
@Database(
    entities = [ProductEntity::class, CategoryEntity::class, ShelfLifeGroupEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shelfLifeGroupDao(): ShelfLifeGroupDao

    companion object {
        private const val DATABASE_NAME = "expiry_guard.db"

        /**
         * v3 -> v4 迁移：products 表新增 completedAt 列，记录完成时间
         */
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN completedAt INTEGER")
            }
        }

        /**
         * 单例实例，防止同时创建多个数据库实例
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库实例，使用双重检查锁定确保线程安全
         *
         * @param context 应用上下文
         * @return AppDatabase 实例
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * 构建数据库
         */
        private fun buildDatabase(context: Context): AppDatabase {
            // 破坏性迁移兜底前先自动备份旧数据库，避免升级丢数据
            backupLegacyDatabase(context)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // 在数据库创建时插入默认分组数据
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                insertDefaultGroups(database.shelfLifeGroupDao())
                            }
                        }
                    }
                })
                .build()
        }

        /**
         * 备份旧版本数据库文件。
         *
         * 仓库历史从 v3 开始，v1/v2 无迁移链可还原；若用户从未发布版本升级而来，
         * `fallbackToDestructiveMigration` 会清空数据。此处先将旧库文件复制到应用文件目录，
         * 并导出到公共 Download 目录，供用户手动找回数据。
         */
        private fun backupLegacyDatabase(context: Context) {
            try {
                val dbFile = context.getDatabasePath(DATABASE_NAME)
                if (!dbFile.exists()) return

                // 读取现有数据库版本，仅当版本 < 3（无迁移链可覆盖）时才需要备份
                val currentVersion = SQLiteDatabase.openDatabase(
                    dbFile.path, null, SQLiteDatabase.OPEN_READONLY
                ).use { db ->
                    db.version
                }
                if (currentVersion >= 3) return

                val backupDir = File(context.filesDir, "legacy_db_backup")
                if (!backupDir.exists()) backupDir.mkdirs()
                val backupFile = File(
                    backupDir,
                    "expiry_guard_v${currentVersion}_${System.currentTimeMillis()}.db"
                )
                dbFile.copyTo(backupFile, overwrite = true)

                // 同时导出到公共 Download 目录，方便用户通过文件管理器取回
                exportToDownloads(context, backupFile)
            } catch (_: Exception) {
                // 备份失败不阻塞数据库初始化
            }
        }

        /**
         * 将备份文件通过 MediaStore 导出到公共 Download 目录（Android 8.0+ 无需存储权限）
         */
        private fun exportToDownloads(context: Context, sourceFile: File) {
            try {
                val fileName = sourceFile.name
                val contentValues = android.content.ContentValues().apply {
                    put(
                        android.provider.MediaStore.Downloads.DISPLAY_NAME,
                        fileName
                    )
                    put(
                        android.provider.MediaStore.Downloads.MIME_TYPE,
                        "application/octet-stream"
                    )
                    put(
                        android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS + "/ExpiryGuard"
                    )
                    put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    val updateValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                    }
                    resolver.update(uri, updateValues, null, null)
                }
            } catch (_: Exception) {
                // 导出失败不影响本地备份
            }
        }

        /**
         * 插入默认的 5 个保质期分组
         */
        private suspend fun insertDefaultGroups(dao: ShelfLifeGroupDao) {
            val defaultGroups = listOf(
                ShelfLifeGroupEntity(
                    name = "当天到期",
                    description = "今日到期提醒",
                    minDays = 0,
                    maxDays = 1,
                    reminderThreshold = 0,
                    colorHex = "#EF4444",
                    sortOrder = 0,
                    isDefault = true
                ),
                ShelfLifeGroupEntity(
                    name = "3个月以下",
                    description = "今日到期提醒",
                    minDays = 1,
                    maxDays = 90,
                    reminderThreshold = 0,
                    colorHex = "#F97316",
                    sortOrder = 1,
                    isDefault = true
                ),
                ShelfLifeGroupEntity(
                    name = "3~6个月",
                    description = "提前15天提醒",
                    minDays = 90,
                    maxDays = 183,
                    reminderThreshold = 15,
                    colorHex = "#F59E0B",
                    sortOrder = 2,
                    isDefault = true
                ),
                ShelfLifeGroupEntity(
                    name = "6个月~1年",
                    description = "提前20天提醒",
                    minDays = 183,
                    maxDays = 365,
                    reminderThreshold = 20,
                    colorHex = "#10B981",
                    sortOrder = 3,
                    isDefault = true
                ),
                ShelfLifeGroupEntity(
                    name = "1年以上",
                    description = "提前45天提醒",
                    minDays = 365,
                    maxDays = Int.MAX_VALUE,
                    reminderThreshold = 45,
                    colorHex = "#6B7280",
                    sortOrder = 4,
                    isDefault = true
                )
            )
            dao.insertDefaultGroups(defaultGroups)
        }
    }
}