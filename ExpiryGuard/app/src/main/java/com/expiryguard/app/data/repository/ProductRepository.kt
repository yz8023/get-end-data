package com.expiryguard.app.data.repository

import com.expiryguard.app.data.db.dao.CategoryCount
import com.expiryguard.app.data.db.dao.CategoryDao
import com.expiryguard.app.data.db.dao.ExpiryCount
import com.expiryguard.app.data.db.dao.ProductDao
import com.expiryguard.app.data.db.dao.ShelfLifeGroupDao
import com.expiryguard.app.data.db.dao.StatusCount
import com.expiryguard.app.data.db.entity.CategoryEntity
import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.data.db.entity.ShelfLifeGroupEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * 产品仓库，封装 DAO 操作并提供错误处理
 *
 * @param productDao 产品数据访问对象
 * @param categoryDao 分类数据访问对象
 * @param shelfLifeGroupDao 保质期分组数据访问对象
 */
class ProductRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val shelfLifeGroupDao: ShelfLifeGroupDao
) {

    // ==================== 产品操作 ====================

    /**
     * 获取所有未删除的产品，按到期日期升序排列
     */
    fun getAllActiveProducts(): Flow<List<ProductEntity>> {
        return productDao.getAllActiveProducts()
    }

    /**
     * 根据分类 ID 获取未删除的产品
     */
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>> {
        return productDao.getProductsByCategory(categoryId)
    }

    /**
     * 搜索产品，按名称或备注模糊匹配
     */
    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProducts(query)
    }

    /**
     * 获取指定天数内即将到期的产品
     *
     * @param days 距离到期的天数范围
     */
    fun getExpiringProducts(days: Int): Flow<List<ProductEntity>> {
        val endTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        return productDao.getExpiringProducts(endTime)
    }

    /**
     * 获取回收站中的产品（已软删除），按删除时间降序排列
     */
    fun getTrashedProducts(): Flow<List<ProductEntity>> {
        return productDao.getTrashedProducts()
    }

    /**
     * 按状态统计产品数量
     */
    fun getStatsByStatus(): Flow<List<StatusCount>> {
        return productDao.getStatsByStatus()
    }

    /**
     * 按月统计到期数量
     */
    fun getExpiryDistribution(): Flow<List<ExpiryCount>> {
        return productDao.getExpiryDistribution()
    }

    /**
     * 按分类统计产品数量
     */
    fun getCategoryStats(): Flow<List<CategoryCount>> {
        return productDao.getCategoryStats()
    }

    /**
     * 根据 ID 获取单个产品
     */
    fun getProductById(id: Long): Flow<ProductEntity?> {
        return productDao.getProductById(id)
    }

    /**
     * 插入产品，返回 Result 包装结果
     *
     * @return Result.success(insertedId) 或 Result.failure(exception)
     */
    suspend fun insert(product: ProductEntity): Result<Long> {
        return try {
            val id = productDao.insert(product)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 批量插入产品，用于恢复数据
     *
     * @return Result.success(Unit) 或 Result.failure(exception)
     */
    suspend fun insertAll(products: List<ProductEntity>): Result<Unit> {
        return try {
            productDao.insertAll(products)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新产品，返回 Result 包装结果
     */
    suspend fun update(product: ProductEntity): Result<Unit> {
        return try {
            productDao.update(product)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 软删除指定 ID 的产品，返回 Result 包装结果
     */
    suspend fun softDelete(ids: List<Long>): Result<Unit> {
        return try {
            productDao.softDelete(ids)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 恢复指定 ID 的产品（取消软删除），返回 Result 包装结果
     */
    suspend fun restore(ids: List<Long>): Result<Unit> {
        return try {
            productDao.restore(ids)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 永久删除所有活跃产品（用于覆盖恢复），返回 Result 包装结果
     */
    suspend fun permanentDeleteAllActive(): Result<Unit> {
        return try {
            productDao.permanentDeleteAllActive()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 永久删除指定 ID 的产品，返回 Result 包装结果
     */
    suspend fun permanentDelete(ids: List<Long>): Result<Unit> {
        return try {
            productDao.permanentDelete(ids)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 批量更新产品备注，返回 Result 包装结果
     */
    suspend fun updateNotes(ids: List<Long>, notes: String): Result<Unit> {
        return try {
            productDao.updateNotes(ids, notes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 批量更新产品分类，返回 Result 包装结果
     */
    suspend fun updateCategory(ids: List<Long>, categoryId: Long): Result<Unit> {
        return try {
            productDao.updateCategory(ids, categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 切换产品的完成状态，返回 Result 包装结果
     */
    suspend fun toggleCompletion(id: Long, isCompleted: Boolean): Result<Unit> {
        return try {
            productDao.updateCompletionStatus(id, isCompleted)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 分类操作 ====================

    /**
     * 获取所有分类
     */
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    /**
     * 插入分类，返回 Result 包装结果
     */
    suspend fun insertCategory(category: CategoryEntity): Result<Long> {
        return try {
            val id = categoryDao.insert(category)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 批量插入分类，用于恢复数据
     */
    suspend fun insertAllCategories(categories: List<CategoryEntity>): Result<Unit> {
        return try {
            categoryDao.insertAll(categories)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除所有分类（用于覆盖恢复）
     */
    suspend fun deleteAllCategories(): Result<Unit> {
        return try {
            categoryDao.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除分类，返回 Result 包装结果
     */
    suspend fun deleteCategory(category: CategoryEntity): Result<Unit> {
        return try {
            categoryDao.delete(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== 保质期分组操作 ====================

    /**
     * 获取所有保质期分组
     */
    fun getAllGroups(): Flow<List<ShelfLifeGroupEntity>> {
        return shelfLifeGroupDao.getAllGroups()
    }

    /**
     * 根据 ID 获取单个分组
     */
    fun getGroupById(id: Long): Flow<ShelfLifeGroupEntity?> {
        return shelfLifeGroupDao.getGroupById(id)
    }

    /**
     * 插入分组，返回 Result 包装结果
     */
    suspend fun insertGroup(group: ShelfLifeGroupEntity): Result<Long> {
        return try {
            val id = shelfLifeGroupDao.insert(group)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新分组，返回 Result 包装结果
     */
    suspend fun updateGroup(group: ShelfLifeGroupEntity): Result<Unit> {
        return try {
            shelfLifeGroupDao.update(group)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 批量插入分组，用于恢复数据
     */
    suspend fun insertAllGroups(groups: List<ShelfLifeGroupEntity>): Result<Unit> {
        return try {
            shelfLifeGroupDao.insertAll(groups)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除全部分组（用于覆盖恢复）
     */
    suspend fun deleteAllGroups(): Result<Unit> {
        return try {
            shelfLifeGroupDao.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除分组，返回 Result 包装结果
     */
    suspend fun deleteGroup(group: ShelfLifeGroupEntity): Result<Unit> {
        return try {
            shelfLifeGroupDao.delete(group)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}