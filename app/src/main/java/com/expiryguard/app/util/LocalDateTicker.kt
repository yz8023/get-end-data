package com.expiryguard.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.expiryguard.app.util.DateUtils.todayTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/** 在跨午夜、手动改时间或时区变化时发出新的本地日期。 */
@Singleton
class LocalDateTicker @Inject constructor(@dagger.hilt.android.qualifiers.ApplicationContext private val context: Context) {
    private val midnightTicks = kotlinx.coroutines.flow.flow {
        while (true) {
            val now = java.time.ZonedDateTime.now()
            val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay((java.time.Duration.between(now, next).toMillis() + 50L).coerceAtLeast(50L))
            emit(Unit)
        }
    }

    private val systemTimeChanges = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }

    fun observeToday(): Flow<Long> = merge(midnightTicks, systemTimeChanges)
        .onStart { emit(Unit) }
        .mapToToday()
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    private fun Flow<Unit>.mapToToday(): Flow<Long> = map { todayTimestamp() }
}
