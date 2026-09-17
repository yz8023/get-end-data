package com.expiryguard.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity
import kotlinx.coroutines.flow.Flow

/**
 * 保质期分组数据访问对象
 */
@Dao
interface ShelfLifeGroupDao {

    /**
     * 获取所有分组，按排序顺序升序排列
     */
    @Query("SELECT * FROM shelf_life_groups ORDER BY sortOrder ASC")
    fun getAllGroups(): Flow<List<ShelfLifeGroupEntity>>

    /**
     * 根据 ID 获取单个分组
     */
    @Query("SELECT * FROM shelf_life_groups WHERE id = :id")
    fun getGroupById(id: Long): Flow<ShelfLifeGroupEntity?>

    /**
     * 插入分组，返回自增 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: ShelfLifeGroupEntity): Long

    /**
 * 批量插入分组
 */
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertAll(groups: List<ShelfLifeGroupEntity>)

/**
 * 更新分组
 */
@Update
suspend fun update(group: ShelfLifeGroupEntity)

    /**
 * 删除所有分组
 */
@Query("DELETE FROM shelf_life_groups")
suspend fun deleteAll()

/**
 * 删除分组
 */
@Delete
suspend fun delete(group: ShelfLifeGroupEntity)

    /**
     * 插入默认的 5 个分组（仅在首次创建数据库时调用）
     *
     * 默认分组：
     * - 当天到期（红色，提醒 0 天）
     * - 3个月以下（橙色，提醒 0 天）
     * - 3~6个月（黄色，提醒 15 天）
     * - 6个月~1年（绿色，提醒 20 天）
     * - 1年以上（灰色，提醒 45 天）
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultGroups(groups: List<ShelfLifeGroupEntity>)
}