package com.expiryguard.app.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReminderSchedulerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun dailyReminderSchedulingIsUniqueAndIdempotent() {
        ReminderScheduler.scheduleDailyReminder(context)
        ReminderScheduler.scheduleDailyReminder(context)
        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("expiry_daily_reminder")
            .get()
        assertEquals(1, work.size)
    }
}
