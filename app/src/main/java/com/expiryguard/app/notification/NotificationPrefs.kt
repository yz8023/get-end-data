package com.expiryguard.app.notification

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

/**
 * 通知相关偏好设置键，供 Worker 与设置页共享使用
 */
object NotificationPrefs {

    /** 通知总开关 */
    val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")

    /** 提前提醒天数 */
    val KEY_REMINDER_DAYS = intPreferencesKey("reminder_days")
}
