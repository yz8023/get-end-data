package com.expiryguard.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 产品实体类，对应数据库中的 products 表
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                                   // 产品名称
    val barcode: String? = null,                        // 条形码
    val categoryId: Long? = null,                       // 分类 ID
    val photoPath: String? = null,                      // 照片路径
    val productionDate: Long? = null,                   // 生产日期（时间戳）
    val expiryDate: Long,                               // 到期日期（时间戳）
    val shelfLifeDays: Int = 0,                         // 保质期天数
    val notes: String? = null,                          // 备注
    val createdAt: Long = System.currentTimeMillis(),   // 创建时间
    val updatedAt: Long = System.currentTimeMillis(),   // 更新时间
    val deletedAt: Long? = null,                        // 删除时间（软删除标记）
    val isCompleted: Boolean = false,                   // 是否已完成
    val completedAt: Long? = null                       // 完成时间（isCompleted 为 true 时记录）
)