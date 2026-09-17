package com.expiryguard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.expiryguard.app.data.repository.ProductRepository
import com.expiryguard.app.domain.engine.ExpiryRuleEngine
import com.expiryguard.app.navigation.AppNavigation
import com.expiryguard.app.notification.NotificationHelper
import com.expiryguard.app.notification.ReminderScheduler
import com.expiryguard.app.ui.theme.ExpiryGuardTheme
import com.expiryguard.app.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var repository: ProductRepository

    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
    private val KEY_MONET = booleanPreferencesKey("monet_enabled")
    private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 注册每日定时到期提醒（幂等）
        ReminderScheduler.scheduleDailyReminder(this)

        // 启动时检查待处理清单，发送通知
        // 查询与过滤在 IO 线程执行，避免阻塞首帧渲染
        lifecycleScope.launch {
            try {
                val notificationEnabled = withContext(Dispatchers.IO) {
                    dataStore.data.first()[KEY_NOTIFICATION_ENABLED] ?: true
                }
                if (notificationEnabled) {
                    withContext(Dispatchers.IO) {
                        val products = repository.getAllActiveProducts().first()
                        val today = DateUtils.todayTimestamp()

                        // 统一口径计算待办分组（与首页今日待办一致）
                        val groups = ExpiryRuleEngine.computePendingGroups(products, today)

                        // 待处理总数量（今日到期 + 可退货 + 已过期 + 预警，已去重）
                        val pendingCount = groups.totalCount
                        val returnableCount = groups.returnable.size
                        val expiredCount = groups.expired.size

                        if (pendingCount > 0) {
                            NotificationHelper.sendPendingNotification(
                                this@MainActivity,
                                pendingCount,
                                returnableCount,
                                expiredCount
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // 忽略启动时的通知错误
            }
        }

        val darkModeFlow = dataStore.data.map { preferences ->
            preferences[KEY_DARK_MODE] ?: false
        }

        val monetFlow = dataStore.data.map { preferences ->
            preferences[KEY_MONET] ?: false
        }

        setContent {
            val isDarkMode by darkModeFlow.collectAsState(initial = false)
            val isMonetEnabled by monetFlow.collectAsState(initial = false)

            ExpiryGuardTheme(
                darkTheme = isDarkMode,
                monetEnabled = isMonetEnabled
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}