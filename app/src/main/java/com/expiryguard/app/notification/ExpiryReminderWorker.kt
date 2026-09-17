package com.expiryguard.app.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expiryguard.app.data.repository.ProductRepository
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.expiryguard.app.util.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * 定时到期提醒 Worker。
 *
 * 周期检查待处理清单（今日到期 + 可退货 + 已过期 + 预警），
 * 在通知开关开启时发送提醒通知；同时按自动清理设置清理回收站。
 */
@HiltWorker
class ExpiryReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ProductRepository,
    private val dataStore: DataStore<Preferences>
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = dataStore.data.first()

            // 自动清理：回收站中删除超过 auto_cleanup_days 天的清单永久删除
            val cleanupDays = prefs[intPreferencesKey("auto_cleanup_days")] ?: 30
            if (cleanupDays > 0) {
                autoCleanupTrash(cleanupDays)
            }

            // 通知开关未开启则跳过
            val enabled = prefs[NotificationPrefs.KEY_NOTIFICATION_ENABLED] ?: true
            if (!enabled) return Result.success()

            val products = repository.getAllActiveProducts().first()
            val today = DateUtils.todayTimestamp()
            val groups = ExpiryRuleEngine.computePendingGroups(products, today)

            val pendingCount = groups.totalCount
            if (pendingCount > 0) {
                NotificationHelper.sendPendingNotification(
                    applicationContext,
                    pendingCount,
                    groups.returnable.size,
                    groups.takeDown.size,
                    groups.expired.size
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    /**
     * 清理回收站中删除时间超过指定天数的清单
     */
    private suspend fun autoCleanupTrash(cleanupDays: Int) {
        val threshold = System.currentTimeMillis() - cleanupDays * 24L * 60 * 60 * 1000
        val trashed = repository.getTrashedProducts().first()
        val expiredIds = trashed.filter { it.deletedAt != null && it.deletedAt!! < threshold }
            .map { it.id }
        if (expiredIds.isNotEmpty()) {
            repository.permanentDelete(expiredIds)
        }
    }
}
