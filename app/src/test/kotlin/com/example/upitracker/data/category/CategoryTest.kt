package com.example.upitracker.data.category

import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.ui.dashboard.DashboardCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CategoryTest {

    @Test
    fun fromLabel_returnsCorrectCategoryForKnownLabels() {
        assertEquals(Category.FOOD_AND_DINING, Category.fromLabel("Food & Dining"))
        assertEquals(Category.GROCERIES, Category.fromLabel("Groceries"))
        assertEquals(Category.TRANSPORT, Category.fromLabel("Transport"))
        assertEquals(Category.UNCATEGORIZED, Category.fromLabel("Uncategorized"))
    }

    @Test
    fun fromLabel_returnsUncategorizedForUnknownText() {
        assertEquals(Category.UNCATEGORIZED, Category.fromLabel("NonExistentCategory"))
        assertEquals(Category.UNCATEGORIZED, Category.fromLabel(""))
        assertEquals(Category.UNCATEGORIZED, Category.fromLabel("Random 123"))
    }

    @Test
    fun categorySummaryCalculation_groupsAndOrdersDescendingByTotal() {
        val txs = listOf(
            Transaction(
                amountPaise = 50000L,
                type = TransactionType.DEBIT,
                merchant = "Swiggy",
                category = "Food & Dining",
                timestamp = 1000L,
                smsHash = "h1"
            ),
            Transaction(
                amountPaise = 20000L,
                type = TransactionType.DEBIT,
                merchant = "Zomato",
                category = "Food & Dining",
                timestamp = 2000L,
                smsHash = "h2"
            ),
            Transaction(
                amountPaise = 100000L,
                type = TransactionType.DEBIT,
                merchant = "Shell Petrol",
                category = "Fuel",
                timestamp = 3000L,
                smsHash = "h3"
            ),
            Transaction(
                amountPaise = 500000L,
                type = TransactionType.CREDIT, // Credit should NOT be included in category spending
                merchant = "Employer",
                category = "Income",
                timestamp = 4000L,
                smsHash = "h4"
            )
        )

        val summary = DashboardCalculator.calculate(
            transactions = txs,
            selectedMonth = YearMonth.of(2026, 10),
            now = LocalDate.of(2026, 10, 15)
        )

        val breakdown = summary.categoryBreakdown
        assertEquals(2, breakdown.size) // Only Fuel and Food & Dining debits

        // Fuel: 100000 paise (58% of 170000 total debits)
        assertEquals(Category.FUEL, breakdown[0].category)
        assertEquals(100000L, breakdown[0].totalPaise)
        assertEquals(1, breakdown[0].count)
        assertEquals(58, breakdown[0].percentOfTotal)

        // Food & Dining: 70000 paise (41% of 170000 total debits)
        assertEquals(Category.FOOD_AND_DINING, breakdown[1].category)
        assertEquals(70000L, breakdown[1].totalPaise)
        assertEquals(2, breakdown[1].count)
        assertEquals(41, breakdown[1].percentOfTotal)
    }

    @Test
    fun categorySummaryCalculation_emptyList_givesEmptyResultWithNoCrash() {
        val summary = DashboardCalculator.calculate(
            transactions = emptyList(),
            selectedMonth = YearMonth.of(2026, 10),
            now = LocalDate.of(2026, 10, 15)
        )

        assertTrue("Breakdown should be empty", summary.categoryBreakdown.isEmpty())
        assertEquals(0, summary.uncategorizedCount)
    }
}
