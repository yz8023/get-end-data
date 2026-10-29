package com.expiryguard.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.expiryguard.app.notification.NotificationHelper
import com.expiryguard.app.util.ImageUtils
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ExpiryGuardApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // 创建通知渠道并清理异常退出留下的相机临时原图
        NotificationHelper.createNotificationChannel(this)
        ImageUtils.cleanupStaleCameraImages(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
