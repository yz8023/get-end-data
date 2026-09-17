package com.expiryguard.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.expiryguard.app.data.db.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * 状态统计结果
 */
data class StatusCount(
    val status: String,  // 状态名称
    val count: Int       // 数量
)

/**
 * 月度到期统计结果
 */
data class ExpiryCount(
    val month: String,   // 月份（如 "2025-01"）
    val count: Int       // 到期数量
)

/**
 * 分类统计结果
 */
data class CategoryCount(
    val categoryName: String,  // 分类名称
    val count: Int             // 产品数量
)

/**
 * 产品数据访问对象
 */
@Dao
interface ProductDao {

    /**
     * 获取所有未删除的产品，按到期日期升序排列
     */
    @Query("SELECT * FROM products WHERE deletedAt IS NULL ORDER BY expiryDate ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    /**
     * 根据分类 ID 获取未删除的产品
     */
    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND deletedAt IS NULL ORDER BY expiryDate ASC")
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>>

    /**
     * 搜索产品，按名称或备注模糊匹配
     */
    @Query("SELECT * FROM products WHERE deletedAt IS NULL AND (name LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY expiryDate ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    /**
     * 获取指定天数内即将到期的产品
     */
    @Query("SELECT * FROM products WHERE deletedAt IS NULL AND expiryDate > 0 AND expiryDate <= :endTime ORDER BY expiryDate ASC")
    fun getExpiringProducts(endTime: Long): Flow<List<ProductEntity>>

    /**
     * 获取回收站中的产品（已软删除），按删除时间降序排列
     */
    @Query("SELECT * FROM products WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getTrashedProducts(): Flow<List<ProductEntity>>

    /**
     * 按状态统计产品数量
     */
    @Query("SELECT 'active' AS status, COUNT(*) AS count FROM products WHERE deletedAt IS NULL")
    fun getStatsByStatus(): Flow<List<StatusCount>>

    /**
     * 按月统计到期数量
     */
    @Query("SELECT strftime('%Y-%m', expiryDate / 1000, 'unixepoch') AS month, COUNT(*) AS count FROM products WHERE deletedAt IS NULL AND expiryDate > 0 GROUP BY month ORDER BY month ASC")
    fun getExpiryDistribution(): Flow<List<ExpiryCount>>

    /**
     * 按分类统计产品数量
     */
    @Query("SELECT c.name AS categoryName, COUNT(p.id) AS count FROM categories c LEFT JOIN products p ON c.id = p.categoryId AND p.deletedAt IS NULL GROUP BY c.id, c.name ORDER BY count DESC")
    fun getCategoryStats(): Flow<List<CategoryCount>>

    /**
     * 根据 ID 获取单个产品
     */
    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductById(id: Long): Flow<ProductEntity?>

    /**
     * 插入产品，返回自增 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    /**
     * 批量插入产品，用于恢复数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    /**
     * 更新产品
     */
    @Update
    suspend fun update(product: ProductEntity)

    /**
     * 软删除：将指定 ID 的产品标记为已删除
     */
    @Query("UPDATE products SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id IN (:ids)")
    suspend fun softDelete(ids: List<Long>, deletedAt: Long = System.currentTimeMillis())

    /**
     * 恢复：清除指定 ID 产品的删除标记
     */
    @Query("UPDATE products SET deletedAt = NULL, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun restore(ids: List<Long>, updatedAt: Long = System.currentTimeMillis())

    /**
 * 永久删除指定 ID 的产品
 */
@Query("DELETE FROM products WHERE id IN (:ids)")
suspend fun permanentDelete(ids: List<Long>)

/**
 * 永久删除所有活跃产品（用于覆盖恢复）
 */
@Query("DELETE FROM products WHERE deletedAt IS NULL")
suspend fun permanentDeleteAllActive()

    /**
     * 批量更新产品备注
     */
    @Query("UPDATE products SET notes = :notes, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateNotes(ids: List<Long>, notes: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 批量更新产品分类
     */
    @Query("UPDATE products SET categoryId = :categoryId, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateCategory(ids: List<Long>, categoryId: Long, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新单个产品的完成状态
     * 标记完成时记录完成时间，取消完成时清空完成时间
     * completedType 用于区分处理方式：已下架 / 已退货处理 / null 未完成
     */
    @Query("UPDATE products SET isCompleted = :isCompleted, completedAt = :completedAt, completedType = :completedType, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCompletionStatus(
        id: Long,
        isCompleted: Boolean,
        completedType: String? = null,
        completedAt: Long? = if (isCompleted) System.currentTimeMillis() else null,
        updatedAt: Long = System.currentTimeMillis()
    )
}