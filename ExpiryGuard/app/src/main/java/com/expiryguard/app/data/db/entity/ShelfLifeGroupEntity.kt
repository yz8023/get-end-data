package com.expiryguard.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 保质期分组实体类，对应数据库中的 shelf_life_groups 表
 *
 * 每个分组代表一个保质期区间，用于对清单进行分类和设置提醒阈值。
 * 分组可自定义，但默认分组不可删除。
 */
@Entity(tableName = "shelf_life_groups")
data class ShelfLifeGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                              // 分组名称，如"3个月以下"
    val description: String,                       // 描述，如"今日到期提醒"
    val minDays: Int,                              // 最小天数
    val maxDays: Int,                              // 最大天数（Int.MAX_VALUE表示无上限）
    val reminderThreshold: Int,                    // 提前提醒天数
    val colorHex: String,                          // 颜色十六进制值，如"#F97316"
    val sortOrder: Int = 0,                        // 排序顺序
    val isDefault: Boolean = false,                // 是否为默认分组（不可删除）
    val createdAt: Long = System.currentTimeMillis()
)