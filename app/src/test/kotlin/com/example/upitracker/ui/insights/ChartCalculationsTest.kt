package com.example.upitracker.ui.insights

import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.ui.dashboard.CategorySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class ChartCalculationsTest {

    private val fixedZone = ZoneId.of("Asia/Kolkata")

    // ── a) formatRupeesCompact ──────────────────────────────────────

    @Test
    fun formatRupeesCompact_verifiesAllPromptExamples() {
        assertEquals("₹0", ChartCalculations.formatRupeesCompact(0L))
        assertEquals("₹950", ChartCalculations.formatRupeesCompact(95000L))
        assertEquals("₹999", ChartCalculations.formatRupeesCompact(99999L))
        assertEquals("₹1K", ChartCalculations.formatRupeesCompact(100000L))
        assertEquals("₹1.2K", ChartCalculations.formatRupeesCompact(120000L))
        assertEquals("₹12.5K", ChartCalculations.formatRupeesCompact(1250000L))
        assertEquals("₹1.5L", ChartCalculations.formatRupeesCompact(15000000L))
        assertEquals("₹1.2Cr", ChartCalculations.formatRupeesCompact(1200000000L))
        assertEquals("-₹50", ChartCalculations.formatRupeesCompact(-5000L))
    }

    // ── b) niceCeiling ──────────────────────────────────────────────

    @Test
    fun niceCeiling_verifiesAllPromptExamples() {
        assertEquals(10000L, ChartCalculations.niceCeiling(0L))
        assertEquals(10000L, ChartCalculations.niceCeiling(9999L))
        assertEquals(10000L, ChartCalculations.niceCeiling(10000L))
        assertEquals(20000L, ChartCalculations.niceCeiling(10001L))
        assertEquals(50000L, ChartCalculations.niceCeiling(43000L))
        assertEquals(100000L, ChartCalculations.niceCeiling(50001L))
        assertEquals(200000000L, ChartCalculations.niceCeiling(123456789L))
    }

    // ── c) barIndexAt ───────────────────────────────────────────────

    @Test
    fun barIndexAt_handlesBoundariesAndEdgeCases() {
        val width = 300f
        val count = 10
        // barWidth = 30f
        assertEquals(0, ChartCalculations.barIndexAt(0f, width, count))
        assertEquals(0, ChartCalculations.barIndexAt(29.99f, width, count))
        assertEquals(1, ChartCalculations.barIndexAt(30f, width, count))
        assertEquals(9, ChartCalculations.barIndexAt(299.99f, width, count)) // just below width
        assertNull(ChartCalculations.barIndexAt(300f, width, count)) // x = width -> null
        assertNull(ChartCalculations.barIndexAt(301f, width, count)) // x > width -> null
        assertNull(ChartCalculations.barIndexAt(-1f, width, count)) // negative x -> null
        assertNull(ChartCalculations.barIndexAt(10f, width, 0)) // count 0 -> null
        assertNull(ChartCalculations.barIndexAt(10f, 0f, count)) // width 0 -> null
    }

    // ── d) pctChange ────────────────────────────────────────────────

    @Test
    fun pctChange_verifiesAllPromptExamples() {
        assertEquals(20, ChartCalculations.pctChange(12000L, 10000L))
        assertEquals(-10, ChartCalculations.pctChange(9000L, 10000L))
        assertEquals(1, ChartCalculations.pctChange(10050L, 10000L))
        assertEquals(-1, ChartCalculations.pctChange(9950L, 10000L))
        assertEquals(-100, ChartCalculations.pctChange(0L, 10000L))
        assertNull(ChartCalculations.pctChange(10000L, 0L)) // previous == 0 -> null
    }

    // ── e) dailySpend, visibleDays, highestDay ───────────────────────

    @Test
    fun dailySpend_verifiesBoundariesAndLeapYears() {
        val jan2026 = YearMonth.of(2026, 1)

        val tFirstDayStart = LocalDateTime.of(2026, 1, 1, 0, 0, 0, 0)
            .atZone(fixedZone).toInstant().toEpochMilli()
        val tLastDayEnd = LocalDateTime.of(2026, 1, 31, 23, 59, 59, 999_000_000)
            .atZone(fixedZone).toInstant().toEpochMilli()
        val tNextMonthFirstDay = LocalDateTime.of(2026, 2, 1, 0, 0, 0, 0)
            .atZone(fixedZone).toInstant().toEpochMilli()

        val txs = listOf(
            Transaction(amountPaise = 5000L, type = TransactionType.DEBIT, merchant = "A", timestamp = tFirstDayStart, smsHash = "h1"),
            Transaction(amountPaise = 10000L, type = TransactionType.DEBIT, merchant = "B", timestamp = tLastDayEnd, smsHash = "h2"),
            Transaction(amountPaise = 20000L, type = TransactionType.DEBIT, merchant = "C", timestamp = tNextMonthFirstDay, smsHash = "h3"), // should be ignored
            Transaction(amountPaise = 50000L, type = TransactionType.CREDIT, merchant = "D", timestamp = tFirstDayStart, smsHash = "h4") // CREDIT ignored
        )

        val daily = ChartCalculations.dailySpend(txs, jan2026, fixedZone)
        assertEquals(31, daily.size)
        assertEquals(5000L, daily[0]) // Day 1
        assertEquals(10000L, daily[30]) // Day 31
        assertEquals(0L, daily[1]) // Day 2 (no spend)

        // Leap year vs normal year February lengths
        val feb2024Leap = ChartCalculations.dailySpend(emptyList(), YearMonth.of(2024, 2), fixedZone)
        val feb2025Normal = ChartCalculations.dailySpend(emptyList(), YearMonth.of(2025, 2), fixedZone)
        assertEquals(29, feb2024Leap.size)
        assertEquals(28, feb2025Normal.size)
    }

    @Test
    fun visibleDays_and_highestDay() {
        val today = LocalDate.of(2026, 10, 15)
        val currentMonth = YearMonth.of(2026, 10)
        val pastMonth = YearMonth.of(2026, 9)

        assertEquals(15, ChartCalculations.visibleDays(currentMonth, today))
        assertEquals(30, ChartCalculations.visibleDays(pastMonth, today))

        // highestDay
        val allZero = LongArray(10)
        assertNull(ChartCalculations.highestDay(allZero))

        val spend = longArrayOf(100L, 500L, 500L, 200L) // Tie at day 2 and 3 -> earliest day (2) wins
        assertEquals(2, ChartCalculations.highestDay(spend))
    }

    // ── f) categorySlices ───────────────────────────────────────────

    @Test
    fun categorySlices_threeCategories_givesThreeSlicesAndNoMerged() {
        val summaries = listOf(
            CategorySummary(Category.FOOD_AND_DINING, 50000L, 2, 50),
            CategorySummary(Category.SHOPPING, 30000L, 1, 30),
            CategorySummary(Category.FUEL, 20000L, 1, 20)
        )
        val slices = ChartCalculations.categorySlices(summaries)
        assertEquals(3, slices.size)
        assertTrue(slices.none { it.isMerged })
        assertEquals("Food & Dining", slices[0].label)
        assertEquals("50%", slices[0].percentDisplay)
    }

    @Test
    fun categorySlices_eightCategories_mergesIntoSmallerCategories() {
        val summaries = listOf(
            CategorySummary(Category.FOOD_AND_DINING, 80000L, 1, 40),
            CategorySummary(Category.SHOPPING, 40000L, 1, 20),
            CategorySummary(Category.FUEL, 30000L, 1, 15),
            CategorySummary(Category.TRANSPORT, 20000L, 1, 10),
            CategorySummary(Category.ENTERTAINMENT, 10000L, 1, 5),
            CategorySummary(Category.BILLS_AND_UTILITIES, 10000L, 1, 5),
            CategorySummary(Category.HEALTH, 6000L, 1, 3),
            CategorySummary(Category.EDUCATION, 4000L, 1, 2)
        )
        val slices = ChartCalculations.categorySlices(summaries, maxSlices = 6)
        assertEquals(7, slices.size) // 6 + 1 merged

        val merged = slices.last()
        assertTrue(merged.isMerged)
        assertEquals("Smaller categories", merged.label)
        assertEquals(2, merged.mergedCategoryCount)
        assertEquals(10000L, merged.totalPaise) // 6000 + 4000
        assertEquals(2, merged.count)
    }

    @Test
    fun categorySlices_emptyInput_andSmallShare() {
        assertTrue(ChartCalculations.categorySlices(emptyList()).isEmpty())

        // Small share (e.g. 10 paise out of 100000 paise -> 0% mathematically -> displays "<1%")
        val summaries = listOf(
            CategorySummary(Category.FOOD_AND_DINING, 99990L, 1, 99),
            CategorySummary(Category.CASH, 10L, 1, 0)
        )
        val slices = ChartCalculations.categorySlices(summaries)
        assertEquals(2, slices.size)
        assertEquals("<1%", slices[1].percentDisplay)
    }

    // ── g) monthlyTotals ────────────────────────────────────────────

    @Test
    fun monthlyTotals_handlesWindowAndOldestMonth() {
        val jan2026 = YearMonth.of(2026, 1)
        val oct2025 = YearMonth.of(2025, 10)

        // Jan 2026 with oldest month Oct 2025 (4 months: Oct 2025, Nov 2025, Dec 2025, Jan 2026)
        val totals = ChartCalculations.monthlyTotals(
            transactions = emptyList(),
            endMonth = jan2026,
            count = 6,
            oldestMonth = oct2025,
            zone = fixedZone
        )
        assertEquals(4, totals.size)
        assertEquals(YearMonth.of(2025, 10), totals[0].month)
        assertEquals(YearMonth.of(2025, 11), totals[1].month)
        assertEquals(YearMonth.of(2025, 12), totals[2].month)
        assertEquals(YearMonth.of(2026, 1), totals[3].month)

        // Oldest month null -> returns only endMonth
        val single = ChartCalculations.monthlyTotals(
            transactions = emptyList(),
            endMonth = jan2026,
            count = 6,
            oldestMonth = null,
            zone = fixedZone
        )
        assertEquals(1, single.size)
        assertEquals(jan2026, single[0].month)
    }

    // ── h) comparison ───────────────────────────────────────────────

    @Test
    fun comparison_currentMonth_sameDaysLastMonth() {
        // Current month = Oct 2026, today = 15 Oct
        val today = LocalDate.of(2026, 10, 15)
        val selectedMonth = YearMonth.of(2026, 10)

        val tOct10 = LocalDateTime.of(2026, 10, 10, 12, 0).atZone(fixedZone).toInstant().toEpochMilli()
        val tSep14 = LocalDateTime.of(2026, 9, 14, 12, 0).atZone(fixedZone).toInstant().toEpochMilli()
        val tSep16 = LocalDateTime.of(2026, 9, 16, 12, 0).atZone(fixedZone).toInstant().toEpochMilli() // day 16 > 15 -> excluded

        val txs = listOf(
            Transaction(amountPaise = 12000L, type = TransactionType.DEBIT, merchant = "A", timestamp = tOct10, smsHash = "h1"),
            Transaction(amountPaise = 10000L, type = TransactionType.DEBIT, merchant = "B", timestamp = tSep14, smsHash = "h2"),
            Transaction(amountPaise = 50000L, type = TransactionType.DEBIT, merchant = "C", timestamp = tSep16, smsHash = "h3")
        )

        val result = ChartCalculations.comparison(txs, selectedMonth, today, fixedZone)
        assertEquals(ComparisonMode.SAME_DAYS_LAST_MONTH, result.mode)
        assertEquals(12000L, result.currentSpentPaise)
        assertEquals(10000L, result.previousSpentPaise) // Sep 16 excluded!
        assertEquals(20, result.pct) // 12000 vs 10000 -> 20%
    }

    @Test
    fun comparison_pastMonth_previousMonthMode() {
        val today = LocalDate.of(2026, 10, 15)
        val selectedMonth = YearMonth.of(2026, 8) // August 2026 (past month)

        val tAug10 = LocalDateTime.of(2026, 8, 10, 12, 0).atZone(fixedZone).toInstant().toEpochMilli()
        val tJul20 = LocalDateTime.of(2026, 7, 20, 12, 0).atZone(fixedZone).toInstant().toEpochMilli()

        val txs = listOf(
            Transaction(amountPaise = 9000L, type = TransactionType.DEBIT, merchant = "A", timestamp = tAug10, smsHash = "h1"),
            Transaction(amountPaise = 10000L, type = TransactionType.DEBIT, merchant = "B", timestamp = tJul20, smsHash = "h2")
        )

        val result = ChartCalculations.comparison(txs, selectedMonth, today, fixedZone)
        assertEquals(ComparisonMode.PREVIOUS_MONTH, result.mode)
        assertEquals(9000L, result.currentSpentPaise)
        assertEquals(10000L, result.previousSpentPaise)
        assertEquals(-10, result.pct) // 9000 vs 10000 -> -10%
    }

    @Test
    fun comparison_march31_cappedAtFeb28() {
        val today = LocalDate.of(2025, 3, 31) // 2025 is not a leap year (Feb has 28 days)
        val selectedMonth = YearMonth.of(2025, 3)

        val tMar20 = LocalDateTime.of(2025, 3, 20, 12, 0).atZone(fixedZone).toInstant().toEpochMilli()
        val tFeb28 = LocalDateTime.of(2025, 2, 28, 12, 0).atZone(fixedZone).toInstant().toEpochMilli()

        val txs = listOf(
            Transaction(amountPaise = 10000L, type = TransactionType.DEBIT, merchant = "A", timestamp = tMar20, smsHash = "h1"),
            Transaction(amountPaise = 10000L, type = TransactionType.DEBIT, merchant = "B", timestamp = tFeb28, smsHash = "h2")
        )

        val result = ChartCalculations.comparison(txs, selectedMonth, today, fixedZone)
        assertEquals(ComparisonMode.SAME_DAYS_LAST_MONTH, result.mode)
        assertEquals(10000L, result.currentSpentPaise)
        assertEquals(10000L, result.previousSpentPaise)
        assertEquals(0, result.pct)
    }

    @Test
    fun comparison_previousSpendingZero_givesPctNull() {
        val today = LocalDate.of(2026, 10, 15)
        val selectedMonth = YearMonth.of(2026, 10)
        val txs = listOf(
            Transaction(amountPaise = 10000L, type = TransactionType.DEBIT, merchant = "A", timestamp = System.currentTimeMillis(), smsHash = "h1")
        )
        val result = ChartCalculations.comparison(txs, selectedMonth, today, fixedZone)
        assertNull(result.pct)
    }
}
