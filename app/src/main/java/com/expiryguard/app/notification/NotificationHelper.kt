package com.expiryguard.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.expiryguard.app.MainActivity
import com.expiryguard.app.R

/**
 * 通知助手，管理通知渠道和通知发送
 *
 * 提供测试通知、待办提醒通知等功能。
 * 通知渠道在应用启动时通过 [createNotificationChannel] 创建。
 */
object NotificationHelper {

    /** 通知渠道 ID */
    const val CHANNEL_ID = "expiry_reminder"

    /** 通知渠道名称 */
    const val CHANNEL_NAME = "到期提醒"

    /** 通知渠道描述 */
    const val CHANNEL_DESC = "清单到期提醒通知"

    /** 测试通知 ID */
    private const val TEST_NOTIFICATION_ID = 1001

    /** 待办提醒通知 ID */
    const val PENDING_NOTIFICATION_ID = 1002

    /**
     * 创建通知渠道（在 Application.onCreate 中调用）
     *
     * Android 8.0+ 需要创建通知渠道才能发送通知
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 发送测试通知
     *
     * 用于验证通知功能是否正常工作，点击通知打开应用主页
     *
     * @param context 上下文
     */
    fun sendTestNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("通知测试")
            .setContentText("这是一条测试通知，通知功能正常工作！")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("这是一条测试通知，通知功能正常工作！\n\n通知设置已生效，您将收到清单到期提醒。"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // 没有通知权限，忽略
        }
    }

    /**
     * 发送待办提醒通知
     *
     * 在应用打开时显示待处理清单数量，点击通知跳转到清单列表
     *
     * @param context 上下文
     * @param pendingCount 待处理清单数量
     * @param returnableCount 可退货清单数量
     * @param takeDownCount 可下架清单数量
     * @param expiredCount 已过期清单数量
     */
    fun sendPendingNotification(
        context: Context,
        pendingCount: Int,
        returnableCount: Int = 0,
        takeDownCount: Int = 0,
        expiredCount: Int = 0
    ) {
        if (pendingCount <= 0) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = buildString {
            append("有 $pendingCount 个清单待处理")
            if (takeDownCount > 0) {
                append("，其中 $takeDownCount 个可下架")
            }
            if (returnableCount > 0) {
                append("，其中 $returnableCount 个可退货")
            }
            if (expiredCount > 0) {
                append("，$expiredCount 个已过期")
            }
        }

        val bigText = buildString {
            appendLine("今日待处理：$pendingCount 个清单")
            if (takeDownCount > 0) {
                appendLine("可下架：$takeDownCount 个")
            }
            if (returnableCount > 0) {
                appendLine("可退货：$returnableCount 个")
            }
            if (expiredCount > 0) {
                appendLine("已过期：$expiredCount 个")
            }
            append("\n点击查看详情")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("清单到期提醒")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(PENDING_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // 没有通知权限，忽略
        }
    }
}