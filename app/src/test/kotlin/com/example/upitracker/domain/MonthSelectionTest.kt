package com.example.upitracker.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class MonthSelectionTest {

    private val fixedZone = ZoneId.of("Asia/Kolkata")
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun monthSelection_withNoTransactions_allowsOnlyCurrentMonth() {
        val currentMonth = YearMonth.of(2026, 10)
        val selection = MonthSelection(
            oldestTimestampFlow = flowOf(null),
            scope = scope,
            zoneId = fixedZone,
            initialMonth = currentMonth
        )

        assertEquals(currentMonth, selection.selectedMonth.value)
        assertFalse(selection.canGoNext.value)
        assertFalse(selection.canGoPrevious.value)

        // Attempting previous or next does not change month
        selection.previous()
        assertEquals(currentMonth, selection.selectedMonth.value)
        selection.next()
        assertEquals(currentMonth, selection.selectedMonth.value)
    }

    @Test
    fun monthSelection_withOldestTransaction_allowsNavigationWithinBounds() {
        val currentMonth = YearMonth.of(2026, 10)
        val oldestTs = LocalDateTime.of(2026, 8, 15, 12, 0)
            .atZone(fixedZone).toInstant().toEpochMilli() // August 2026

        val selection = MonthSelection(
            oldestTimestampFlow = flowOf(oldestTs),
            scope = scope,
            zoneId = fixedZone,
            initialMonth = currentMonth
        )

        // At current month (Oct 2026): cannot go next, can go previous
        assertFalse(selection.canGoNext.value)
        assertTrue(selection.canGoPrevious.value)

        // Go to September 2026
        selection.previous()
        assertEquals(YearMonth.of(2026, 9), selection.selectedMonth.value)
        assertTrue(selection.canGoNext.value)
        assertTrue(selection.canGoPrevious.value)

        // Go to August 2026 (oldest month)
        selection.previous()
        assertEquals(YearMonth.of(2026, 8), selection.selectedMonth.value)
        assertTrue(selection.canGoNext.value)
        assertFalse(selection.canGoPrevious.value) // reached oldest boundary

        // Trying to go before August does nothing
        selection.previous()
        assertEquals(YearMonth.of(2026, 8), selection.selectedMonth.value)

        // Navigate forward back to October
        selection.next()
        assertEquals(YearMonth.of(2026, 9), selection.selectedMonth.value)
        selection.next()
        assertEquals(YearMonth.of(2026, 10), selection.selectedMonth.value)
        assertFalse(selection.canGoNext.value) // reached current month
    }
}
