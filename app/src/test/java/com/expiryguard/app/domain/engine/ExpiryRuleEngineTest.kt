package com.expiryguard.app.domain.engine

import com.expiryguard.app.data.db.entity.ProductEntity
import com.expiryguard.app.domain.model.ProductStatus
import com.expiryguard.app.util.DateUtils
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryRuleEngineTest {
    private val zone = ZoneId.of("UTC")
    private val date = LocalDate.of(2026, 6, 26)
    private val today = DateUtils.toTimestamp(date, zone)

    private fun product(id: Long, dayOffset: Long, shelfLifeDays: Int = 400): ProductEntity {
        return ProductEntity(
            id = id,
            name = "P$id",
            expiryDate = DateUtils.toTimestamp(date.plusDays(dayOffset), zone),
            shelfLifeDays = shelfLifeDays
        )
    }

    @Test
    fun `expiry day is not expired and yesterday is one day overdue`() {
        val todayStatus = ExpiryRuleEngine.calculateStatus(400, product(1, 0).expiryDate, null, 2, today)
        assertTrue(todayStatus is ProductStatus.TakeDown)
        assertEquals(0, (todayStatus as ProductStatus.TakeDown).remainingDays)

        val expired = ExpiryRuleEngine.calculateStatus(400, product(2, -1).expiryDate, null, 2, today)
        assertEquals(ProductStatus.Expired(1), expired)
    }

    @Test
    fun `custom return threshold is honored`() {
        val status = ExpiryRuleEngine.calculateStatus(
            30,
            product(1, 10, 30).expiryDate,
            30,
            0,
            today
        )
        assertEquals(ProductStatus.Returnable(10, 30), status)
    }

    @Test
    fun `pending groups use supplied today for every classification`() {
        val groups = ExpiryRuleEngine.computePendingGroups(
            products = listOf(product(1, 0), product(2, -1), product(3, 2)),
            today = today,
            takeDownThreshold = 2
        )
        assertEquals(listOf(1L), groups.todayExpiry.map { it.id })
        assertEquals(listOf(2L), groups.expired.map { it.id })
        assertEquals(listOf(3L), groups.takeDown.map { it.id })
        assertEquals(3, groups.totalCount)
    }

    @Test
    fun `custom group threshold is applied to status and pending groups`() {
        val policy = ExpiryPolicy(
            takeDownDays = 0,
            reminderDays = 7,
            groupRules = listOf(ShelfLifeRule(0, 100, 8))
        )
        val item = product(9, 5, 30)
        assertEquals(
            ProductStatus.Returnable(5, 8),
            ExpiryRuleEngine.calculateStatus(item.shelfLifeDays, item.expiryDate, policy, today)
        )
        assertEquals(
            listOf(9L),
            ExpiryRuleEngine.computePendingGroups(listOf(item), today, policy).returnable.map { it.id }
        )
    }

    @Test
    fun `notification reminder window includes otherwise upcoming product`() {
        val groups = ExpiryRuleEngine.computePendingGroups(
            products = listOf(product(1, 5, 30)),
            today = today,
            takeDownThreshold = 0,
            returnThreshold = 0,
            reminderDays = 7
        )
        assertEquals(listOf(1L), groups.warning.map { it.id })
    }
}
