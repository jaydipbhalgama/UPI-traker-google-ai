package com.example.upitracker.ui.dashboard

import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DashboardCalculatorTest {

    @Test
    fun calculate_spentReceivedNetTotals() {
        val transactions = listOf(
            Transaction(
                amountPaise = 15000L,
                type = TransactionType.DEBIT,
                merchant = "Merchant A",
                timestamp = 1000L,
                smsHash = "h1"
            ),
            Transaction(
                amountPaise = 25000L,
                type = TransactionType.DEBIT,
                merchant = "Merchant B",
                timestamp = 2000L,
                smsHash = "h2"
            ),
            Transaction(
                amountPaise = 50000L,
                type = TransactionType.CREDIT,
                merchant = "Employer",
                timestamp = 3000L,
                smsHash = "h3"
            )
        )

        val summary = DashboardCalculator.calculate(
            transactions = transactions,
            selectedMonth = YearMonth.of(2026, 10),
            now = LocalDate.of(2026, 10, 15)
        )

        assertEquals(40000L, summary.totalSpentPaise)
        assertEquals(50000L, summary.totalReceivedPaise)
        assertEquals(10000L, summary.netPaise)
        assertEquals(3, summary.transactionCount)
    }

    @Test
    fun calculate_biggestExpense_considersOnlyDebits() {
        val tx1 = Transaction(
            amountPaise = 15000L,
            type = TransactionType.DEBIT,
            merchant = "Cafe",
            timestamp = 1000L,
            smsHash = "h1"
        )
        val tx2 = Transaction(
            amountPaise = 45000L,
            type = TransactionType.DEBIT,
            merchant = "Electronics",
            timestamp = 2000L,
            smsHash = "h2"
        )
        val txCredit = Transaction(
            amountPaise = 200000L, // high credit
            type = TransactionType.CREDIT,
            merchant = "Salary",
            timestamp = 3000L,
            smsHash = "h3"
        )

        val summary = DashboardCalculator.calculate(
            transactions = listOf(tx1, tx2, txCredit),
            selectedMonth = YearMonth.of(2026, 10),
            now = LocalDate.of(2026, 10, 15)
        )

        assertNotNull(summary.biggestExpense)
        assertEquals(45000L, summary.biggestExpense?.amountPaise)
        assertEquals("Electronics", summary.biggestExpense?.merchant)
    }

    @Test
    fun calculate_topMerchants_caseInsensitiveAndSorted() {
        val transactions = listOf(
            Transaction(amountPaise = 10000L, type = TransactionType.DEBIT, merchant = "swiggy", timestamp = 1L, smsHash = "h1"),
            Transaction(amountPaise = 15000L, type = TransactionType.DEBIT, merchant = "SWIGGY ", timestamp = 2L, smsHash = "h2"),
            Transaction(amountPaise = 5000L, type = TransactionType.DEBIT, merchant = "Swiggy", timestamp = 3L, smsHash = "h3"),
            Transaction(amountPaise = 50000L, type = TransactionType.DEBIT, merchant = "Amazon", timestamp = 4L, smsHash = "h4"),
            Transaction(amountPaise = 20000L, type = TransactionType.DEBIT, merchant = "Uber", timestamp = 5L, smsHash = "h5"),
            Transaction(amountPaise = 90000L, type = TransactionType.DEBIT, merchant = "Unknown", timestamp = 6L, smsHash = "h6") // should be ignored
        )

        val summary = DashboardCalculator.calculate(
            transactions = transactions,
            selectedMonth = YearMonth.of(2026, 10),
            now = LocalDate.of(2026, 10, 15)
        )

        val top = summary.topMerchants
        assertEquals(3, top.size) // Unknown ignored since others exist
        assertEquals("Amazon", top[0].merchantName)
        assertEquals(50000L, top[0].totalPaise)
        assertEquals(1, top[0].count)

        assertEquals("swiggy", top[1].merchantName.lowercase())
        assertEquals(30000L, top[1].totalPaise)
        assertEquals(3, top[1].count)

        assertEquals("Uber", top[2].merchantName)
        assertEquals(20000L, top[2].totalPaise)
        assertEquals(1, top[2].count)
    }

    @Test
    fun calculate_topMerchants_keepsUnknownIfOnlyUnknownExists() {
        val transactions = listOf(
            Transaction(amountPaise = 10000L, type = TransactionType.DEBIT, merchant = "Unknown", timestamp = 1L, smsHash = "h1"),
            Transaction(amountPaise = 15000L, type = TransactionType.DEBIT, merchant = "UNKNOWN ", timestamp = 2L, smsHash = "h2")
        )

        val summary = DashboardCalculator.calculate(
            transactions = transactions,
            selectedMonth = YearMonth.of(2026, 10),
            now = LocalDate.of(2026, 10, 15)
        )

        assertEquals(1, summary.topMerchants.size)
        assertEquals("unknown", summary.topMerchants[0].merchantName.lowercase())
        assertEquals(25000L, summary.topMerchants[0].totalPaise)
        assertEquals(2, summary.topMerchants[0].count)
    }

    @Test
    fun calculate_averageDailySpend_currentMonth_usesElapsedDays() {
        // Oct 15: 15 days elapsed in current month
        val now = LocalDate.of(2026, 10, 15)
        val selectedMonth = YearMonth.of(2026, 10)
        val transactions = listOf(
            Transaction(amountPaise = 30000L, type = TransactionType.DEBIT, merchant = "A", timestamp = 1L, smsHash = "h1")
        )

        val summary = DashboardCalculator.calculate(transactions, selectedMonth, now)
        // 30000 / 15 = 2000 paise
        assertEquals(2000L, summary.averageDailySpendPaise)
    }

    @Test
    fun calculate_averageDailySpend_pastMonth_februaryNormalYear() {
        // Feb 2025 has 28 days
        val now = LocalDate.of(2026, 10, 15)
        val selectedMonth = YearMonth.of(2025, 2)
        val transactions = listOf(
            Transaction(amountPaise = 28000L, type = TransactionType.DEBIT, merchant = "A", timestamp = 1L, smsHash = "h1")
        )

        val summary = DashboardCalculator.calculate(transactions, selectedMonth, now)
        // 28000 / 28 = 1000 paise
        assertEquals(1000L, summary.averageDailySpendPaise)
    }

    @Test
    fun calculate_averageDailySpend_pastMonth_februaryLeapYear() {
        // Feb 2024 has 29 days
        val now = LocalDate.of(2026, 10, 15)
        val selectedMonth = YearMonth.of(2024, 2)
        val transactions = listOf(
            Transaction(amountPaise = 29000L, type = TransactionType.DEBIT, merchant = "A", timestamp = 1L, smsHash = "h1")
        )

        val summary = DashboardCalculator.calculate(transactions, selectedMonth, now)
        // 29000 / 29 = 1000 paise
        assertEquals(1000L, summary.averageDailySpendPaise)
    }

    @Test
    fun calculate_emptyList_returnsZerosAndNullsWithoutCrashing() {
        val summary = DashboardCalculator.calculate(
            transactions = emptyList(),
            selectedMonth = YearMonth.of(2026, 10),
            now = LocalDate.of(2026, 10, 15)
        )

        assertEquals(0L, summary.totalSpentPaise)
        assertEquals(0L, summary.totalReceivedPaise)
        assertEquals(0L, summary.netPaise)
        assertEquals(0, summary.transactionCount)
        assertEquals(0L, summary.averageDailySpendPaise)
        assertNull(summary.biggestExpense)
        assertTrue(summary.topMerchants.isEmpty())
        assertTrue(summary.recentTransactions.isEmpty())
    }
}
