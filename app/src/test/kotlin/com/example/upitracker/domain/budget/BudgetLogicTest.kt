package com.example.upitracker.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetLogicTest {

    @Test
    fun parseRupeesToPaise_handlesAllRequiredExamples() {
        // Valid cases
        assertEquals(15000L, BudgetLogic.parseRupeesToPaise("150"))
        assertEquals(125050L, BudgetLogic.parseRupeesToPaise("1,250.5"))
        assertEquals(50L, BudgetLogic.parseRupeesToPaise("0.5"))
        assertEquals(10000000L, BudgetLogic.parseRupeesToPaise("1,00,000"))
        assertEquals(20000L, BudgetLogic.parseRupeesToPaise("₹200"))
        assertEquals(0L, BudgetLogic.parseRupeesToPaise("0"))

        // Invalid cases
        assertNull(BudgetLogic.parseRupeesToPaise("12.345"))
        assertNull(BudgetLogic.parseRupeesToPaise("abc"))
        assertNull(BudgetLogic.parseRupeesToPaise(""))
        assertNull(BudgetLogic.parseRupeesToPaise("-5"))
        assertNull(BudgetLogic.parseRupeesToPaise("1.2.3"))
        assertNull(BudgetLogic.parseRupeesToPaise("1234567890"))
        assertNull(BudgetLogic.parseRupeesToPaise("150."))
    }

    @Test
    fun budgetStatus_calculatesLevelsAndPercentsCorrectly() {
        val month = YearMonth.of(2026, 10)
        val today = LocalDate.of(2026, 10, 15)

        // limit 10000 with spent 7999 -> 79 OK
        val s1 = BudgetLogic.budgetStatus(10000L, 7999L, month, today)
        assertEquals(79, s1.percent)
        assertEquals(BudgetLevel.OK, s1.level)
        assertEquals(2001L, s1.remainingPaise)

        // 8000 -> 80 NEAR
        val s2 = BudgetLogic.budgetStatus(10000L, 8000L, month, today)
        assertEquals(80, s2.percent)
        assertEquals(BudgetLevel.NEAR, s2.level)

        // 9999 -> 99 NEAR
        val s3 = BudgetLogic.budgetStatus(10000L, 9999L, month, today)
        assertEquals(99, s3.percent)
        assertEquals(BudgetLevel.NEAR, s3.level)

        // 10000 -> 100 OVER
        val s4 = BudgetLogic.budgetStatus(10000L, 10000L, month, today)
        assertEquals(100, s4.percent)
        assertEquals(BudgetLevel.OVER, s4.level)

        // 15000 -> 150 OVER and remaining -5000
        val s5 = BudgetLogic.budgetStatus(10000L, 15000L, month, today)
        assertEquals(150, s5.percent)
        assertEquals(BudgetLevel.OVER, s5.level)
        assertEquals(-5000L, s5.remainingPaise)
        assertNull(s5.perDayLeftPaise)
    }

    @Test
    fun budgetStatus_calculatesDaysLeftAndPerDay() {
        val oct2026 = YearMonth.of(2026, 10)

        // 15 Oct in current month: 31 - 15 + 1 = 17 days
        val s1 = BudgetLogic.budgetStatus(34000L, 0L, oct2026, LocalDate.of(2026, 10, 15))
        assertEquals(17, s1.daysLeft)
        assertEquals(2000L, s1.perDayLeftPaise) // 34000 / 17

        // Last day of month = 1 day left
        val s2 = BudgetLogic.budgetStatus(10000L, 5000L, oct2026, LocalDate.of(2026, 10, 31))
        assertEquals(1, s2.daysLeft)
        assertEquals(5000L, s2.perDayLeftPaise)

        // Leap-year Feb on the 29th = 1 day left
        val feb2024 = YearMonth.of(2024, 2)
        val s3 = BudgetLogic.budgetStatus(20000L, 10000L, feb2024, LocalDate.of(2024, 2, 29))
        assertEquals(1, s3.daysLeft)
        assertEquals(10000L, s3.perDayLeftPaise)

        // Past month = 0 days left, perDay null
        val pastMonth = YearMonth.of(2026, 9)
        val s4 = BudgetLogic.budgetStatus(20000L, 10000L, pastMonth, LocalDate.of(2026, 10, 15))
        assertEquals(0, s4.daysLeft)
        assertNull(s4.perDayLeftPaise)
    }

    @Test
    fun decideAlerts_handlesAllThresholdScenarios() {
        val month = YearMonth.of(2026, 10)
        val key = "Food & Dining"

        // 1. 79% gives nothing
        val d1 = BudgetLogic.decideAlerts(
            budgets = mapOf(key to 10000L),
            spentByKey = mapOf(key to 7900L),
            month = month,
            alreadyNotified = emptySet()
        )
        assertTrue(d1.toNotify.isEmpty())
        assertTrue(d1.toMark.isEmpty())

        // 2. 80% notifies 80
        val d2 = BudgetLogic.decideAlerts(
            budgets = mapOf(key to 10000L),
            spentByKey = mapOf(key to 8000L),
            month = month,
            alreadyNotified = emptySet()
        )
        assertEquals(1, d2.toNotify.size)
        assertEquals(80, d2.toNotify[0].threshold)
        assertEquals(80, d2.toNotify[0].percent)
        assertEquals(setOf("2026-10|$key|80"), d2.toMark)

        // 3. Already notified 80 at 85% gives nothing
        val d3 = BudgetLogic.decideAlerts(
            budgets = mapOf(key to 10000L),
            spentByKey = mapOf(key to 8500L),
            month = month,
            alreadyNotified = setOf("2026-10|$key|80")
        )
        assertTrue(d3.toNotify.isEmpty())
        assertEquals(setOf("2026-10|$key|80"), d3.toMark)

        // 4. 80 notified then 100% notifies 100
        val d4 = BudgetLogic.decideAlerts(
            budgets = mapOf(key to 10000L),
            spentByKey = mapOf(key to 10000L),
            month = month,
            alreadyNotified = setOf("2026-10|$key|80")
        )
        assertEquals(1, d4.toNotify.size)
        assertEquals(100, d4.toNotify[0].threshold)
        assertEquals(100, d4.toNotify[0].percent)
        assertEquals(setOf("2026-10|$key|80", "2026-10|$key|100"), d4.toMark)

        // 5. First check at 120% notifies ONLY 100 and marks both
        val d5 = BudgetLogic.decideAlerts(
            budgets = mapOf(key to 10000L),
            spentByKey = mapOf(key to 12000L),
            month = month,
            alreadyNotified = emptySet()
        )
        assertEquals(1, d5.toNotify.size)
        assertEquals(100, d5.toNotify[0].threshold)
        assertEquals(120, d5.toNotify[0].percent)
        assertEquals(setOf("2026-10|$key|80", "2026-10|$key|100"), d5.toMark)

        // 6. A new month notifies again
        val nextMonth = YearMonth.of(2026, 11)
        val d6 = BudgetLogic.decideAlerts(
            budgets = mapOf(key to 10000L),
            spentByKey = mapOf(key to 8500L),
            month = nextMonth,
            alreadyNotified = setOf("2026-10|$key|80") // October marker
        )
        assertEquals(1, d6.toNotify.size)
        assertEquals(80, d6.toNotify[0].threshold)
        assertEquals(setOf("2026-11|$key|80"), d6.toMark)

        // 7. Limit 0 is ignored
        val d7 = BudgetLogic.decideAlerts(
            budgets = mapOf(key to 0L),
            spentByKey = mapOf(key to 5000L),
            month = month,
            alreadyNotified = emptySet()
        )
        assertTrue(d7.toNotify.isEmpty())
        assertTrue(d7.toMark.isEmpty())

        // 8. After a reset (markers removed then recomputed with toMark applied) there is no notification
        val d8Pre = BudgetLogic.decideAlerts(
            budgets = mapOf(key to 10000L),
            spentByKey = mapOf(key to 9000L),
            month = month,
            alreadyNotified = emptySet()
        )
        val resetMarkers = d8Pre.toMark // Simulates resetAndMark saving toMark without notifying
        val d8Post = BudgetLogic.decideAlerts(
            budgets = mapOf(key to 10000L),
            spentByKey = mapOf(key to 9000L),
            month = month,
            alreadyNotified = resetMarkers
        )
        assertTrue(d8Post.toNotify.isEmpty())
    }
}
