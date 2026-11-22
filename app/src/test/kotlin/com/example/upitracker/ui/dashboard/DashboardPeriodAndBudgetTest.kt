package com.example.upitracker.ui.dashboard

import com.example.upitracker.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DashboardPeriodAndBudgetTest {

    private val fixedToday = LocalDate.of(2026, 10, 15)
    private val istZone = ZoneId.of("Asia/Kolkata")

    @Test
    fun periodRange_today() {
        val (start, end) = periodRange(SpendPeriod.TODAY, fixedToday, istZone)

        val expectedStart = fixedToday.atStartOfDay(istZone).toInstant().toEpochMilli()
        val expectedEnd = fixedToday.plusDays(1).atStartOfDay(istZone).toInstant().toEpochMilli()

        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
    }

    @Test
    fun periodRange_week() {
        val (start, end) = periodRange(SpendPeriod.WEEK, fixedToday, istZone)

        // Monday 12 Oct 2026 00:00:00 IST
        val expectedStart = LocalDate.of(2026, 10, 12).atStartOfDay(istZone).toInstant().toEpochMilli()
        // Just before 16 Oct 2026 00:00:00 IST (exclusive upper bound = 16 Oct 00:00:00 IST)
        val expectedEnd = LocalDate.of(2026, 10, 16).atStartOfDay(istZone).toInstant().toEpochMilli()

        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
    }

    @Test
    fun periodRange_month() {
        val (start, end) = periodRange(SpendPeriod.MONTH, fixedToday, istZone)

        val oct2026 = YearMonth.of(2026, 10)
        val expectedStart = DateTimeUtils.monthStartMillis(oct2026, istZone)
        val expectedEnd = DateTimeUtils.monthEndExclusiveMillis(oct2026, istZone)

        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
    }

    @Test
    fun computeBudgetInfo_onTrackCalculation() {
        // limit 3000000, spent 2500000, Oct (31 days), today 15th
        // remaining 500000, daysLeft 17, perDay 29411 (floor)
        val month = YearMonth.of(2026, 10)
        val info = computeBudgetInfo(
            limitPaise = 3_000_000L,
            spentPaise = 2_500_000L,
            month = month,
            today = fixedToday
        )

        assertNotNull(info)
        assertEquals(3_000_000L, info!!.limitPaise)
        assertEquals(2_500_000L, info.spentPaise)
        assertEquals(500_000L, info.remainingPaise)
        assertEquals(17, info.daysLeft)
        assertEquals(29411L, info.perDayPaise)
        assertEquals(83, info.percent)
    }

    @Test
    fun computeBudgetInfo_spentExceedsLimit() {
        // Spent > limit -> remaining negative, perDay null
        val month = YearMonth.of(2026, 10)
        val info = computeBudgetInfo(
            limitPaise = 3_000_000L,
            spentPaise = 3_500_000L,
            month = month,
            today = fixedToday
        )

        assertNotNull(info)
        assertEquals(-500_000L, info!!.remainingPaise)
        assertTrue(info.remainingPaise < 0L)
        assertNull(info.perDayPaise)
    }

    @Test
    fun computeBudgetInfo_limitZeroReturnsNull() {
        // Limit 0 -> budgetInfo null
        val month = YearMonth.of(2026, 10)
        val info = computeBudgetInfo(
            limitPaise = 0L,
            spentPaise = 2_500_000L,
            month = month,
            today = fixedToday
        )

        assertNull(info)
    }
}
