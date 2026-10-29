package com.expiryguard.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.expiryguard.app.data.db.dao.ShelfLifeGroupDao
import com.expiryguard.app.domain.engine.ExpiryPolicy
import com.expiryguard.app.domain.engine.ShelfLifeRule
import com.expiryguard.app.notification.NotificationPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** 首页、列表、详情、统计和通知共用的策略事实源。 */
@Singleton
class ExpiryPolicyRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    groupDao: ShelfLifeGroupDao
) {
    val policy: Flow<ExpiryPolicy> = combine(
        dataStore.data,
        groupDao.getAllGroups()
    ) { preferences, groups ->
        ExpiryPolicy(
            takeDownDays = preferences[KEY_TAKE_DOWN_DAYS] ?: 2,
            reminderDays = preferences[NotificationPrefs.KEY_REMINDER_DAYS] ?: 7,
            groupRules = groups.map {
                ShelfLifeRule(it.minDays, it.maxDays, it.reminderThreshold)
            }
        ).normalized()
    }

    suspend fun current(): ExpiryPolicy = policy.first()

    private companion object {
        val KEY_TAKE_DOWN_DAYS = intPreferencesKey("take_down_days")
    }
}
