package com.expiryguard.app.util

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 日期计算统一基于本地日历日，避免直接按 24 小时毫秒数计算造成 DST 偏差。 */
object DateUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MM/dd")

    fun formatDate(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return toLocalDate(timestamp, zoneId).format(dateFormatter)
    }

    fun formatShortDate(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return toLocalDate(timestamp, zoneId).format(shortDateFormatter)
    }

    fun daysBetween(
        from: Long,
        to: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        val days = toLocalDate(to, zoneId).toEpochDay() - toLocalDate(from, zoneId).toEpochDay()
        return days.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }

    fun toLocalDate(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
        return Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
    }

    fun toTimestamp(localDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun todayTimestamp(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return toTimestamp(LocalDate.now(zoneId), zoneId)
    }

    fun isToday(
        timestamp: Long,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        return toLocalDate(timestamp, zoneId) == today
    }

    fun isTomorrow(
        timestamp: Long,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        return toLocalDate(timestamp, zoneId) == today.plusDays(1)
    }

    fun addDays(
        timestamp: Long,
        days: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return toTimestamp(toLocalDate(timestamp, zoneId).plusDays(days.toLong()), zoneId)
    }

    /**
     * 解析年/月/日、月/日或单独月份；两位年份按 20xx 处理。
     * 数字段必须能安全转为整数，且完整日期只允许恰好三段，避免静默忽略多余输入。
     */
    fun parseDate(input: String, referenceDate: LocalDate = LocalDate.now()): LocalDate? {
        val text = input.trim()
        if (text.isEmpty()) return null
        val numbers = Regex("\\d+").findAll(text).toList()
            .map { it.value.toLongOrNull() ?: return null }
        if (numbers.isEmpty() || numbers.size > 3) return null

        return when (numbers.size) {
            3 -> {
                val year = if (numbers[0] < 100) 2000 + numbers[0] else numbers[0]
                buildDate(year, numbers[1], numbers[2])
            }
            2 -> {
                val first = numbers[0]
                val second = numbers[1]
                if (first in 1..12 && second in 1..31) {
                    buildDate(referenceDate.year.toLong(), first, second)
                } else {
                    val year = if (first < 100) 2000 + first else first
                    buildDate(year, second, 1)
                }
            }
            else -> buildDate(referenceDate.year.toLong(), numbers[0], 1)
        }
    }

    private fun buildDate(year: Long, month: Long, day: Long): LocalDate? {
        if (year !in 1..9999 || month !in 1..12 || day !in 1..31) return null
        return try {
            LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        } catch (_: DateTimeException) {
            null
        }
    }

    fun parseDateToTimestamp(
        input: String,
        referenceDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        return parseDate(input, referenceDate)?.let { toTimestamp(it, zoneId) }
    }
}
