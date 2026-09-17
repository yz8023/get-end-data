package com.expiryguard.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expiryguard.app.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据访问对象
 */
@Dao
interface CategoryDao {

    /**
     * 获取所有分类
     */
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    /**
     * 插入分类，返回自增 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    /**
 * 批量插入分类
 */
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertAll(categories: List<CategoryEntity>)

/**
 * 删除所有分类
 */
@Query("DELETE FROM categories")
suspend fun deleteAll()

/**
 * 删除分类
 */
@Delete
suspend fun delete(category: CategoryEntity)
}