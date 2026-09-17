package com.expiryguard.app.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expiryguard.app.data.repository.ProductRepository
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 设置页面状态
 */
data class SettingsUiState(
    val isNotificationEnabled: Boolean = true,
    val reminderDays: Int = 7,
    val autoCleanupDays: Int = 30,
    val isDarkModeEnabled: Boolean = false,
    val isMonetEnabled: Boolean = false,
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
    private val repository: ProductRepository,
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
        private val KEY_DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        private val KEY_MONET_ENABLED = booleanPreferencesKey("monet_enabled")
        private val KEY_TAKE_DOWN_DAYS = intPreferencesKey("take_down_days")
    }

    init {
        // 从 DataStore 加载偏好设置
        viewModelScope.launch {
            dataStore.data.collect { preferences ->
                _uiState.value = SettingsUiState(
                    isNotificationEnabled = preferences[KEY_NOTIFICATION_ENABLED] ?: true,
                    reminderDays = preferences[KEY_REMINDER_DAYS] ?: 7,
                    autoCleanupDays = preferences[KEY_AUTO_CLEANUP_DAYS] ?: 30,
                    isDarkModeEnabled = preferences[KEY_DARK_MODE_ENABLED] ?: false,
                    isMonetEnabled = preferences[KEY_MONET_ENABLED] ?: false,
                    takeDownDays = preferences[KEY_TAKE_DOWN_DAYS] ?: 2,
                    appVersion = "1.0.0"
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
            ExpiryRuleEngine.updateTakeDownThreshold(days)
        }
    }

    /**
     * 切换深色模式
     */
    fun toggleDarkMode() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                val current = preferences[KEY_DARK_MODE_ENABLED] ?: false
                preferences[KEY_DARK_MODE_ENABLED] = !current
            }
        }
    }

    /**
     * 切换莫奈取色
     */
    fun toggleMonet() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                val current = preferences[KEY_MONET_ENABLED] ?: false
                preferences[KEY_MONET_ENABLED] = !current
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

    /**
     * 导出数据为 CSV 格式
     *
     * @return 导出文件路径，失败返回 null
     */
    suspend fun exportDataAsCsv(): String? {
        return try {
            val products = repository.getAllActiveProducts().first()
            val csvContent = buildString {
                appendLine("名称,条形码,到期日期,保质期(天),备注,创建时间")
                products.forEach { product ->
                    appendLine(
                        "${product.name}," +
                        "${product.barcode ?: ""}," +
                        "${product.expiryDate}," +
                        "${product.shelfLifeDays}," +
                        "${product.notes?.replace(",", "，") ?: ""}," +
                        "${product.createdAt}"
                    )
                }
            }
            val file = File(appContext.cacheDir, "expiryguard_export.csv")
            file.writeText(csvContent)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 导出数据为 JSON 格式
     *
     * @return 导出文件路径，失败返回 null
     */
    suspend fun exportDataAsJson(): String? {
        return try {
            val products = repository.getAllActiveProducts().first()
            val gson = Gson()
            val jsonContent = gson.toJson(products)
            val file = File(appContext.cacheDir, "expiryguard_export.json")
            file.writeText(jsonContent)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 备份数据到指定目录
     *
     * @param backupDir 备份目录路径
     * @return 是否成功
     */
    suspend fun backupData(backupDir: String): Boolean {
        return try {
            val products = repository.getAllActiveProducts().first()
            val gson = Gson()
            val jsonContent = gson.toJson(products)
            val backupFile = File(backupDir, "expiryguard_backup_${System.currentTimeMillis()}.json")
            backupFile.parentFile?.mkdirs()
            backupFile.writeText(jsonContent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从备份文件恢复数据
     *
     * @param backupFilePath 备份文件路径
     * @return 是否成功
     */
    suspend fun restoreData(backupFilePath: String): Boolean {
        return try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) return false
            val jsonContent = backupFile.readText()
            val gson = Gson()
            val type = object : TypeToken<List<com.expiryguard.app.data.db.entity.ProductEntity>>() {}.type
            val products: List<com.expiryguard.app.data.db.entity.ProductEntity> = gson.fromJson(jsonContent, type)
            // 逐个插入恢复的产品
            products.forEach { product ->
                repository.insert(product)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}