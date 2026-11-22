package com.example.upitracker.domain.summary

import org.junit.Assert.assertEquals
import org.junit.Test

class DailySummaryFormatterTest {

    @Test
    fun format_noBudget_returnsPromptToSetBudget() {
        // monthlyBudgetPaise is null
        val (title1, body1) = DailySummaryFormatter.format(
            todaySpentPaise = 15000L,
            monthSpentPaise = 45000L,
            monthlyBudgetPaise = null
        )
        assertEquals("Daily UPI Summary", title1)
        assertEquals("You've spent ₹150.00 today (₹450.00 total this month). Set a budget to track what's left.", body1)

        // monthlyBudgetPaise <= 0
        val (title2, body2) = DailySummaryFormatter.format(
            todaySpentPaise = 0L,
            monthSpentPaise = 20000L,
            monthlyBudgetPaise = 0L
        )
        assertEquals("Daily UPI Summary", title2)
        assertEquals("You've spent ₹0.00 today (₹200.00 total this month). Set a budget to track what's left.", body2)
    }

    @Test
    fun format_zeroTodayAndUnderBudget_returnsNoSpendingNotice() {
        val (title, body) = DailySummaryFormatter.format(
            todaySpentPaise = 0L,
            monthSpentPaise = 30000L,
            monthlyBudgetPaise = 100000L
        )
        assertEquals("Daily UPI Summary", title)
        assertEquals("No UPI spending today. You still have ₹700.00 left this month.", body)
    }

    @Test
    fun format_overBudget_returnsOverBudgetWarning() {
        val (title, body) = DailySummaryFormatter.format(
            todaySpentPaise = 25000L,
            monthSpentPaise = 125000L,
            monthlyBudgetPaise = 100000L
        )
        assertEquals("Daily UPI Summary", title)
        assertEquals("You've spent ₹250.00 today. You're ₹250.00 over your monthly budget.", body)
    }

    @Test
    fun format_standard_returnsSpendingAndRemaining() {
        val (title, body) = DailySummaryFormatter.format(
            todaySpentPaise = 50000L,
            monthSpentPaise = 75000L,
            monthlyBudgetPaise = 100000L
        )
        assertEquals("Daily UPI Summary", title)
        assertEquals("You've spent ₹500.00 today and have ₹250.00 left this month.", body)
    }
}
