package com.expiryguard.app.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 定时提醒调度器，注册每日周期检查任务。
 */
object ReminderScheduler {

    private const val WORK_NAME = "expiry_daily_reminder"

    /**
     * 注册每日到期提醒任务（幂等，重复调用不会叠加）。
     *
     * WorkManager 周期任务最小间隔 15 分钟，此处按天注册，
     * 每次执行会检查待处理清单并在需要时发送通知。
     */
    fun scheduleDailyReminder(context: Context) {
        val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
