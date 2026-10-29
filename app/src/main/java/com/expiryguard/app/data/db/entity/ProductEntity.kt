package com.expiryguard.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["expiryDate"]),
        Index(value = ["deletedAt"]),
        Index(value = ["categoryId"]),
        Index(value = ["deletedAt", "isCompleted", "expiryDate"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 跨设备稳定身份；本地自增 ID 不再用于增量恢复去重。 */
    @ColumnInfo(defaultValue = "''") val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val barcode: String? = null,
    val categoryId: Long? = null,
    val photoPath: String? = null,
    val productionDate: Long? = null,
    val expiryDate: Long,
    val shelfLifeDays: Int = 0,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val completedType: String? = null
) {
    companion object {
        const val COMPLETED_TYPE_TAKE_DOWN = "TAKE_DOWN"
        const val COMPLETED_TYPE_RETURN = "RETURN"
    }
}
