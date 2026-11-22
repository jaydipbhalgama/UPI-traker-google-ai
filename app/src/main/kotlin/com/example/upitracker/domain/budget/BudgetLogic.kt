package com.example.upitracker.domain.budget

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

const val MAX_BUDGET_PAISE: Long = 1_000_000_000L // Rs 1 crore

enum class BudgetLevel {
    OK,
    NEAR,
    OVER
}

data class BudgetStatus(
    val limitPaise: Long,
    val spentPaise: Long,
    val remainingPaise: Long,
    val percent: Int,
    val level: BudgetLevel,
    val daysLeft: Int,
    val perDayLeftPaise: Long?
)

data class AlertItem(
    val budgetKey: String,
    val threshold: Int,
    val percent: Int
)

data class AlertDecision(
    val toNotify: List<AlertItem>,
    val toMark: Set<String>
)

object BudgetLogic {

    private val RUPEE_REGEX = Regex("""^\d{1,9}(\.\d{1,2})?$""")

    /**
     * Parses a user-entered rupee string into paise (Long).
     *
     * Trims, removes commas, spaces, and an optional leading '₹'.
     * The remaining string must match ^\d{1,9}(\.\d{1,2})?$ else null.
     */
    fun parseRupeesToPaise(text: String): Long? {
        var s = text.trim()
        if (s.startsWith('₹')) {
            s = s.substring(1).trim()
        }
        s = s.replace(",", "").replace(" ", "")
        if (!s.matches(RUPEE_REGEX)) {
            return null
        }
        return try {
            BigDecimal(s).movePointRight(2).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculates the budget status given the monthly limit and spent paise.
     */
    fun budgetStatus(
        limitPaise: Long,
        spentPaise: Long,
        month: YearMonth,
        today: LocalDate
    ): BudgetStatus {
        val remainingPaise = limitPaise - spentPaise
        val percent = if (limitPaise > 0L) {
            ((spentPaise * 100) / limitPaise).toInt()
        } else {
            0
        }

        val level = when {
            percent < 80 -> BudgetLevel.OK
            percent in 80..99 -> BudgetLevel.NEAR
            else -> BudgetLevel.OVER
        }

        val isCurrentMonth = month.year == today.year && month.monthValue == today.monthValue
        val daysLeft = if (isCurrentMonth) {
            month.lengthOfMonth() - today.dayOfMonth + 1
        } else {
            0
        }

        val perDayLeftPaise = if (remainingPaise > 0L && daysLeft > 0) {
            remainingPaise / daysLeft
        } else {
            null
        }

        return BudgetStatus(
            limitPaise = limitPaise,
            spentPaise = spentPaise,
            remainingPaise = remainingPaise,
            percent = percent,
            level = level,
            daysLeft = daysLeft,
            perDayLeftPaise = perDayLeftPaise
        )
    }

    /**
     * Pure function to decide which budget alerts to trigger.
     *
     * Marker string format: "<yyyy-MM>|<budgetKey>|<threshold>", thresholds 80 and 100.
     */
    fun decideAlerts(
        budgets: Map<String, Long>,
        spentByKey: Map<String, Long>,
        month: YearMonth,
        alreadyNotified: Set<String>
    ): AlertDecision {
        val toNotify = mutableListOf<AlertItem>()
        val toMark = mutableSetOf<String>()

        for ((key, limit) in budgets) {
            if (limit <= 0L) continue

            val spent = spentByKey[key] ?: 0L
            val percent = ((spent * 100) / limit).toInt()

            val crossedThresholds = mutableListOf<Int>()
            if (percent >= 80) crossedThresholds.add(80)
            if (percent >= 100) crossedThresholds.add(100)

            for (th in crossedThresholds) {
                toMark.add("$month|$key|$th")
            }

            val highest = crossedThresholds.maxOrNull()
            if (highest != null) {
                val marker = "$month|$key|$highest"
                if (marker !in alreadyNotified) {
                    toNotify.add(AlertItem(budgetKey = key, threshold = highest, percent = percent))
                }
            }
        }

        return AlertDecision(toNotify = toNotify, toMark = toMark)
    }
}
