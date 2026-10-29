package com.expiryguard.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expiryguard.app.data.db.dao.CategoryDao
import com.expiryguard.app.data.db.dao.ProductDao
import com.expiryguard.app.data.db.dao.ShelfLifeGroupDao
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity

/**
 * 应用唯一的 Room 数据库定义。
 *
 * 迁移和初始化必须由 DI 中的数据库 Builder 显式注册。禁止在生产环境使用
 * fallbackToDestructiveMigration，避免版本升级时清空用户数据。
 */
@Database(
    entities = [ProductEntity::class, CategoryEntity::class, ShelfLifeGroupEntity::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shelfLifeGroupDao(): ShelfLifeGroupDao

    companion object {
        const val DATABASE_NAME = "expiry_guard_db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE products ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createShelfLifeGroupsTable(db)
                insertDefaultGroups(db)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN completedAt INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN completedType TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 先修复历史悬空引用，再启用外键，避免旧数据导致升级失败。
                db.execSQL(
                    "UPDATE products SET categoryId = NULL WHERE categoryId IS NOT NULL " +
                        "AND categoryId NOT IN (SELECT id FROM categories)"
                )
                db.execSQL(
                    """
                    CREATE TABLE products_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL,
                        barcode TEXT,
                        categoryId INTEGER,
                        photoPath TEXT,
                        productionDate INTEGER,
                        expiryDate INTEGER NOT NULL,
                        shelfLifeDays INTEGER NOT NULL,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER,
                        isCompleted INTEGER NOT NULL,
                        completedAt INTEGER,
                        completedType TEXT,
                        FOREIGN KEY(categoryId) REFERENCES categories(id)
                            ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                val uuidExpression = """
                    lower(
                        substr(hex(randomblob(4)), 1, 8) || '-' ||
                        substr(hex(randomblob(2)), 1, 4) || '-' ||
                        substr(hex(randomblob(2)), 1, 4) || '-' ||
                        substr(hex(randomblob(2)), 1, 4) || '-' ||
                        substr(hex(randomblob(6)), 1, 12)
                    )
                """.trimIndent().replace('\n', ' ')
                db.execSQL(
                    """
                    INSERT INTO products_new (
                        id, uuid, name, barcode, categoryId, photoPath, productionDate,
                        expiryDate, shelfLifeDays, notes, createdAt, updatedAt, deletedAt,
                        isCompleted, completedAt, completedType
                    )
                    SELECT id, $uuidExpression, name, barcode, categoryId, photoPath,
                        productionDate, expiryDate, shelfLifeDays, notes, createdAt, updatedAt,
                        deletedAt, isCompleted, completedAt, completedType
                    FROM products
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE products")
                db.execSQL("ALTER TABLE products_new RENAME TO products")
                db.execSQL("CREATE UNIQUE INDEX index_products_uuid ON products(uuid)")
                db.execSQL("CREATE INDEX index_products_expiryDate ON products(expiryDate)")
                db.execSQL("CREATE INDEX index_products_deletedAt ON products(deletedAt)")
                db.execSQL("CREATE INDEX index_products_categoryId ON products(categoryId)")
                db.execSQL(
                    "CREATE INDEX index_products_deletedAt_isCompleted_expiryDate " +
                        "ON products(deletedAt, isCompleted, expiryDate)"
                )
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )

        /** 新数据库的同步种子数据，避免依赖管理页面首次打开。 */
        fun seedDefaultData(db: SupportSQLiteDatabase) {
            insertDefaultCategories(db)
            insertDefaultGroups(db)
        }

        private fun createShelfLifeGroupsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS shelf_life_groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    minDays INTEGER NOT NULL,
                    maxDays INTEGER NOT NULL,
                    reminderThreshold INTEGER NOT NULL,
                    colorHex TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    isDefault INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        private fun insertDefaultCategories(db: SupportSQLiteDatabase) {
            val categories = listOf(
                Triple("食品", 0xFF10B981.toInt(), 0),
                Triple("饮料", 0xFF2563EB.toInt(), 1),
                Triple("日用品", 0xFFF59E0B.toInt(), 2),
                Triple("调味品", 0xFFF97316.toInt(), 3),
                Triple("冷冻食品", 0xFF7C3AED.toInt(), 4),
                Triple("其他", 0xFF6B7280.toInt(), 5)
            )
            categories.forEach { (name, color, _) ->
                db.execSQL(
                    "INSERT INTO categories (name, color) VALUES (?, ?)",
                    arrayOf<Any>(name, color)
                )
            }
        }

        private fun insertDefaultGroups(db: SupportSQLiteDatabase) {
            data class GroupSeed(
                val name: String,
                val description: String,
                val minDays: Int,
                val maxDays: Int,
                val reminderThreshold: Int,
                val colorHex: String,
                val sortOrder: Int
            )

            val now = System.currentTimeMillis()
            val groups = listOf(
                GroupSeed("当天到期", "今日到期提醒", 0, 1, 0, "#EF4444", 0),
                GroupSeed("3个月以下", "今日到期提醒", 1, 90, 0, "#F97316", 1),
                GroupSeed("3~6个月", "提前15天提醒", 90, 183, 15, "#F59E0B", 2),
                GroupSeed("6个月~1年", "提前20天提醒", 183, 365, 20, "#10B981", 3),
                GroupSeed("1年以上", "提前45天提醒", 365, Int.MAX_VALUE, 45, "#6B7280", 4)
            )
            groups.forEach { group ->
                db.execSQL(
                    """
                    INSERT INTO shelf_life_groups (
                        name, description, minDays, maxDays, reminderThreshold,
                        colorHex, sortOrder, isDefault, createdAt
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?)
                    """.trimIndent(),
                    arrayOf<Any>(
                        group.name,
                        group.description,
                        group.minDays,
                        group.maxDays,
                        group.reminderThreshold,
                        group.colorHex,
                        group.sortOrder,
                        now
                    )
                )
            }
        }
    }
}
