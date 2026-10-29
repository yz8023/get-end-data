package com.expiryguard.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.export.DataExporter
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 设置页面状态
 */
data class SettingsUiState(
    val isNotificationEnabled: Boolean = true,
    val reminderDays: Int = 7,
    val autoCleanupDays: Int = 30,
    val takeDownDays: Int = 2,
    val appVersion: String = "1.0.0"
)

/**
 * 设置页面 ViewModel，管理应用偏好设置
 *
 * 使用 DataStore 存储偏好设置，支持通知开关、提醒天数、自动清理、深色模式等配置
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataExporter: DataExporter,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // DataStore 键定义
    companion object {
        private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val KEY_REMINDER_DAYS = intPreferencesKey("reminder_days")
        private val KEY_AUTO_CLEANUP_DAYS = intPreferencesKey("auto_cleanup_days")
        private val KEY_TAKE_DOWN_DAYS = intPreferencesKey("take_down_days")
    }

    init {
        val versionName = runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
        // 从 DataStore 加载偏好设置
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                _uiState.value = SettingsUiState(
                    isNotificationEnabled = preferences[KEY_NOTIFICATION_ENABLED] ?: true,
                    reminderDays = preferences[KEY_REMINDER_DAYS] ?: 7,
                    autoCleanupDays = preferences[KEY_AUTO_CLEANUP_DAYS] ?: 30,
                    takeDownDays = preferences[KEY_TAKE_DOWN_DAYS] ?: 2,
                    appVersion = versionName
                )
            }
        }
    }

    /**
     * 切换通知开关
     */
    fun toggleNotification() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                val current = preferences[KEY_NOTIFICATION_ENABLED] ?: true
                preferences[KEY_NOTIFICATION_ENABLED] = !current
            }
        }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[KEY_NOTIFICATION_ENABLED] = enabled }
        }
    }

    /**
     * 更改提醒提前天数
     *
     * @param days 提醒天数
     */
    fun changeReminderDays(days: Int) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_REMINDER_DAYS] = days
            }
        }
    }

    /**
     * 更新取件天数
     */
    fun changeTakeDownDays(days: Int) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_TAKE_DOWN_DAYS] = days.coerceAtLeast(1)
            }
        }
    }

    /**
     * 更改自动清理天数
     *
     * @param days 清理天数，0 表示从不清理
     */
    fun changeAutoCleanupDays(days: Int) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[KEY_AUTO_CLEANUP_DAYS] = days
            }
        }
    }

    suspend fun exportDataAsCsv(uri: Uri): Result<Int> = runCatching {
        dataExporter.exportCsv(uri)
    }

    suspend fun exportDataAsJson(uri: Uri): Result<Int> = runCatching {
        dataExporter.exportJson(uri)
    }
}