package com.expiryguard.app.util

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {
    @Test
    fun `daysBetween uses calendar days across daylight saving transition`() {
        val zone = ZoneId.of("America/New_York")
        val from = DateUtils.toTimestamp(LocalDate.of(2026, 3, 8), zone)
        val to = DateUtils.toTimestamp(LocalDate.of(2026, 3, 9), zone)
        assertEquals(23L * 60 * 60 * 1000, to - from)
        assertEquals(1, DateUtils.daysBetween(from, to, zone))
    }

    @Test
    fun `parseDate supports documented forms with fixed reference date`() {
        val reference = LocalDate.of(2026, 6, 26)
        assertEquals(LocalDate.of(2019, 1, 1), DateUtils.parseDate("2019年1.1", reference))
        assertEquals(LocalDate.of(2019, 1, 1), DateUtils.parseDate("19.1.1", reference))
        assertEquals(LocalDate.of(2026, 10, 12), DateUtils.parseDate("10.12", reference))
        assertEquals(LocalDate.of(2019, 1, 1), DateUtils.parseDate("2019.01", reference))
        assertEquals(LocalDate.of(2026, 3, 1), DateUtils.parseDate("3月", reference))
    }

    @Test
    fun `parseDate rejects malformed overflow and ambiguous extra fields`() {
        val reference = LocalDate.of(2026, 1, 1)
        assertNull(DateUtils.parseDate("999999999999999999999.1.1", reference))
        assertNull(DateUtils.parseDate("2026-02-29", reference))
        assertNull(DateUtils.parseDate("2026-01-01-99", reference))
        assertNull(DateUtils.parseDate("13", reference))
    }

    @Test
    fun `isToday can be evaluated deterministically`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val date = LocalDate.of(2026, 6, 26)
        val timestamp = DateUtils.toTimestamp(date, zone)
        assertTrue(DateUtils.isToday(timestamp, date, zone))
    }
}
