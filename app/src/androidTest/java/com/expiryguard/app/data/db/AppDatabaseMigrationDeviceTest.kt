package com.expiryguard.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.expiryguard.app.data.db.entity.ProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID

/** 使用真实 Room 打开器逐版本验证迁移和最终 schema；不是只执行几条孤立 SQL。 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationDeviceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun allHistoricalVersionsPreserveProductsAndGroups() = runBlocking {
        for (version in 1..5) {
            val name = "migration_$version.db"
            createHistoricalDatabase(version, name)
            val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(*AppDatabase.ALL_MIGRATIONS)
                .allowMainThreadQueries()
                .build()
            try {
                // 触发 RoomOpenHelper 的迁移和最终 schema 校验。
                db.openHelper.writableDatabase
                val product = db.productDao().getProductById(42).first()
                assertNotNull("v$version 产品丢失", product)
                product!!
                assertEquals("迁移产品", product.name)
                assertEquals(7L, product.categoryId)
                assertEquals(900L, product.expiryDate)
                assertTrue(product.uuid.isNotBlank())
                UUID.fromString(product.uuid)
                val groups = db.shelfLifeGroupDao().getAllGroups().first()
                if (version < 3) {
                    assertEquals("v$version 默认分组", 5, groups.size)
                } else {
                    assertTrue("v$version 自定义分组丢失", groups.any { it.name == "自定义组" })
                }
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }
    }

    private fun createHistoricalDatabase(version: Int, name: String) {
        context.deleteDatabase(name)
        val file: File = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, color INTEGER NOT NULL)")
        val completionColumns = buildString {
            if (version >= 2) append(", isCompleted INTEGER NOT NULL DEFAULT 0")
            if (version >= 4) append(", completedAt INTEGER")
            if (version >= 5) append(", completedType TEXT")
        }
        db.execSQL(
            """CREATE TABLE products (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
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
                deletedAt INTEGER
                $completionColumns
            )""".trimIndent()
        )
        if (version >= 3) {
            db.execSQL(
                """CREATE TABLE shelf_life_groups (
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
                )""".trimIndent()
            )
            db.execSQL(
                "INSERT INTO shelf_life_groups (id,name,description,minDays,maxDays,reminderThreshold,colorHex,sortOrder,isDefault,createdAt) VALUES (9,'自定义组','测试',0,100,8,'#112233',0,0,1)"
            )
        }
        db.execSQL("INSERT INTO categories (id,name,color) VALUES (7,'测试分类',123)")
        val completionValues = buildString {
            if (version >= 2) append(",0")
            if (version >= 4) append(",NULL")
            if (version >= 5) append(",NULL")
        }
        db.execSQL(
            """INSERT INTO products (
                id,name,barcode,categoryId,photoPath,productionDate,expiryDate,shelfLifeDays,
                notes,createdAt,updatedAt,deletedAt
                ${if (version >= 2) ",isCompleted" else ""}
                ${if (version >= 4) ",completedAt" else ""}
                ${if (version >= 5) ",completedType" else ""}
            ) VALUES (42,'迁移产品',NULL,7,NULL,NULL,900,100,'保留',10,10,NULL$completionValues)""".trimIndent()
        )
        db.version = version
        db.close()
    }
}
