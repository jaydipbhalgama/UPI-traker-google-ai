package com.example.upitracker.domain.summary

import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class MonthlySummaryCalculatorTest {

    private val fixedZone = ZoneId.of("Asia/Kolkata")

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        LocalDateTime.of(year, month, day, hour, 0)
            .atZone(fixedZone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun calculate_sumsCurrentMonthIncomeAndExpensesAndIgnoresOtherMonths() {
        val nov2026 = YearMonth.of(2026, 11)
        val txs = listOf(
            Transaction(
                amountPaise = 500_000L, // ₹5,000
                type = TransactionType.CREDIT,
                merchant = "Salary",
                timestamp = millis(2026, 11, 1),
                smsHash = "h1"
            ),
            Transaction(
                amountPaise = 100_000L, // ₹1,000
                type = TransactionType.CREDIT,
                merchant = "Refund",
                timestamp = millis(2026, 11, 10),
                smsHash = "h2"
            ),
            Transaction(
                amountPaise = 150_000L, // ₹1,500
                type = TransactionType.DEBIT,
                merchant = "Groceries",
                timestamp = millis(2026, 11, 5),
                smsHash = "h3"
            ),
            Transaction(
                amountPaise = 50_000L, // ₹500
                type = TransactionType.DEBIT,
                merchant = "Cafe",
                timestamp = millis(2026, 11, 21),
                smsHash = "h4"
            ),
            // Previous month — must be ignored
            Transaction(
                amountPaise = 999_000L,
                type = TransactionType.DEBIT,
                merchant = "Rent",
                timestamp = millis(2026, 10, 31, 23),
                smsHash = "h5"
            ),
            // Next month — must be ignored
            Transaction(
                amountPaise = 888_000L,
                type = TransactionType.CREDIT,
                merchant = "Bonus",
                timestamp = millis(2026, 12, 1, 0),
                smsHash = "h6"
            )
        )

        val summary = MonthlySummaryCalculator.calculate(txs, nov2026, fixedZone)

        assertEquals(nov2026, summary.month)
        assertEquals(200_000L, summary.totalExpensePaise)
        assertEquals(600_000L, summary.totalIncomePaise)
        assertEquals(2, summary.expenseCount)
        assertEquals(2, summary.incomeCount)
        assertEquals(400_000L, summary.netPaise)
        assertEquals(0.25f, summary.expenseShareFraction, 0.0001f)
        assertEquals(0.75f, summary.incomeShareFraction, 0.0001f)
        assertEquals(25, summary.expenseSharePercent)
        assertEquals(75, summary.incomeSharePercent)
        assertEquals(67, summary.savingsRatePercent) // (400000 / 600000) * 100 = 66.666 -> 67
        assertTrue(summary.hasActivity)
    }

    @Test
    fun calculate_negativeNetAndZeroIncome_setsSavingsRateNull() {
        val nov2026 = YearMonth.of(2026, 11)
        val txs = listOf(
            Transaction(
                amountPaise = 120_000L,
                type = TransactionType.DEBIT,
                merchant = "Shopping",
                timestamp = millis(2026, 11, 15),
                smsHash = "h1"
            )
        )

        val summary = MonthlySummaryCalculator.calculate(txs, nov2026, fixedZone)

        assertEquals(120_000L, summary.totalExpensePaise)
        assertEquals(0L, summary.totalIncomePaise)
        assertEquals(-120_000L, summary.netPaise)
        assertEquals(1.0f, summary.expenseShareFraction, 0.0001f)
        assertEquals(0.0f, summary.incomeShareFraction, 0.0001f)
        assertNull(summary.savingsRatePercent)
        assertTrue(summary.hasActivity)
    }

    @Test
    fun calculate_overspentWithIncome_computesNegativeSavingsRate() {
        val nov2026 = YearMonth.of(2026, 11)
        val txs = listOf(
            Transaction(
                amountPaise = 100_000L,
                type = TransactionType.CREDIT,
                merchant = "Allowance",
                timestamp = millis(2026, 11, 2),
                smsHash = "h1"
            ),
            Transaction(
                amountPaise = 150_000L,
                type = TransactionType.DEBIT,
                merchant = "Flight",
                timestamp = millis(2026, 11, 3),
                smsHash = "h2"
            )
        )

        val summary = MonthlySummaryCalculator.calculate(txs, nov2026, fixedZone)

        assertEquals(-50_000L, summary.netPaise)
        assertEquals(-50, summary.savingsRatePercent)
    }

    @Test
    fun calculate_emptyTransactions_returnsZeroSummary() {
        val nov2026 = YearMonth.of(2026, 11)
        val summary = MonthlySummaryCalculator.calculate(emptyList(), nov2026, fixedZone)

        assertEquals(0L, summary.totalExpensePaise)
        assertEquals(0L, summary.totalIncomePaise)
        assertEquals(0L, summary.netPaise)
        assertEquals(0, summary.expenseCount)
        assertEquals(0, summary.incomeCount)
        assertEquals(0f, summary.expenseShareFraction, 0.0001f)
        assertEquals(0f, summary.incomeShareFraction, 0.0001f)
        assertNull(summary.savingsRatePercent)
        assertFalse(summary.hasActivity)
    }
}
