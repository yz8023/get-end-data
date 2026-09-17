package com.expiryguard.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体类，对应数据库中的 categories 表
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                                   // 分类名称
    val color: Int = 0xFF2563EB.toInt()                 // 分类颜色（默认蓝色）
)