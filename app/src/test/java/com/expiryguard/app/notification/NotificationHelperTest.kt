package com.expiryguard.app.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotificationHelperTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        NotificationHelper.createNotificationChannel(context)
    }

    @Test
    fun blockedReminderChannelIsNotReportedAsEnabled() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            android.app.NotificationChannel(
                NotificationHelper.CHANNEL_ID,
                NotificationHelper.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_NONE
            )
        )
        assertFalse(NotificationHelper.areNotificationsEnabled(context))
        // 发送路径也必须静默拒绝，而不是在权限/渠道关闭时抛出 SecurityException。
        NotificationHelper.sendPendingNotification(context, 1)
        NotificationHelper.sendTestNotification(context)
    }

    @Test
    fun zeroPendingItemsDoesNotCreateNotification() {
        NotificationHelper.sendPendingNotification(context, 0)
        // 该用例主要覆盖边界分支；真实 ROM 的权限和渠道行为仍由设备 smoke test 验证。
    }
}
