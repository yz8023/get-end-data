package com.expiryguard.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 日期工具类，提供常用日期格式化和计算功能。
 */
object DateUtils {

    // 缓存时区与 formatter，避免频繁调用 systemDefault() 的开销
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MM/dd")

    /**
     * 格式化为完整日期字符串（yyyy-MM-dd）
     */
    fun formatDate(timestamp: Long): String {
        return toLocalDate(timestamp).format(dateFormatter)
    }

    /**
     * 格式化为短日期字符串（MM/dd）
     */
    fun formatShortDate(timestamp: Long): String {
        return toLocalDate(timestamp).format(shortDateFormatter)
    }

    /**
     * 计算两个时间戳之间的天数差
     */
    fun daysBetween(from: Long, to: Long): Int {
        val fromDate = toLocalDate(from)
        val toDate = toLocalDate(to)
        return (toDate.toEpochDay() - fromDate.toEpochDay()).toInt()
    }

    /**
     * 将时间戳转为 LocalDate
     */
    fun toLocalDate(timestamp: Long): LocalDate {
        return Instant.ofEpochMilli(timestamp)
            .atZone(zoneId)
            .toLocalDate()
    }

    /**
     * 将 LocalDate 转为当天开始时间的时间戳
     */
    fun toTimestamp(localDate: LocalDate): Long {
        return localDate
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    /**
     * 获取今天开始时间的时间戳
     */
    fun todayTimestamp(): Long {
        return toTimestamp(LocalDate.now())
    }

    /**
     * 判断时间戳是否属于今天
     */
    fun isToday(timestamp: Long): Boolean {
        return toLocalDate(timestamp) == LocalDate.now()
    }

    /**
     * 判断时间戳是否属于明天
     */
    fun isTomorrow(timestamp: Long): Boolean {
        return toLocalDate(timestamp) == LocalDate.now().plusDays(1)
    }

    /**
     * 在指定时间戳上增加天数，返回新的时间戳
     */
    fun addDays(timestamp: Long, days: Int): Long {
        val date = toLocalDate(timestamp)
        val newDate = date.plusDays(days.toLong())
        return toTimestamp(newDate)
    }

    /**
     * 解析多种中文/符号分隔格式的日期字符串，返回 LocalDate；解析失败返回 null。
     *
     * 支持的格式（年/月/日分隔符不限，可为 点、斜杠、横杠、空格、中文年月日）：
     * - 2019.1.1
     * - 2019年1月1日
     * - 2019 1 1
     * - 2019年1.1
     * - 2019/01/01
     * - 2019-1-1
     * - 19.1.1（两位年份按 20xx 处理）
     * - 2019.01（只有年月时按 1 号）
     */
    fun parseDate(input: String): LocalDate? {
        val text = input.trim()
        if (text.isEmpty()) return null

        // 提取数字块：年（2~4位），月（1~2位），日（可选，1~2位）
        val regex = Regex("""(\d{2,4})\D{1,4}?(\d{1,2})(?:\D{1,4}?(\d{1,2}))?\D*$""")
        val match = regex.find(text) ?: return null

        val yearRaw = match.groupValues[1]
        val monthRaw = match.groupValues[2]
        val dayRaw = match.groupValues[3]

        val year = if (yearRaw.length == 2) 2000 + yearRaw.toInt() else yearRaw.toInt()
        val month = monthRaw.toInt()
        val day = if (dayRaw.isEmpty()) 1 else dayRaw.toInt()

        if (month !in 1..12) return null
        if (day !in 1..31) return null

        return try {
            LocalDate.of(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 解析日期字符串并返回时间戳（当天开始），解析失败返回 null。
     */
    fun parseDateToTimestamp(input: String): Long? {
        return parseDate(input)?.let { toTimestamp(it) }
    }
}