package com.example.upitracker.ui.dashboard

import com.example.upitracker.util.DateTimeUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class SpendPeriod {
    TODAY,
    WEEK,
    MONTH
}

data class BudgetInfo(
    val limitPaise: Long,
    val spentPaise: Long,
    val remainingPaise: Long,
    val percent: Int,
    val perDayPaise: Long?,
    val daysLeft: Int
)

/**
 * Pure function to calculate start (inclusive) and end (exclusive) epoch milliseconds
 * for the specified [SpendPeriod].
 *
 * - TODAY: 00:00:00.000 today -> 00:00:00.000 tomorrow (exclusive).
 * - WEEK: Monday 00:00:00.000 of the current ISO week -> tomorrow 00:00:00.000 (exclusive, i.e. just before tomorrow 00:00).
 * - MONTH: start and exclusive end of [selectedMonth] (defaults to current month of [today]).
 */
fun periodRange(
    period: SpendPeriod,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    selectedMonth: YearMonth = YearMonth.from(today)
): Pair<Long, Long> {
    return when (period) {
        SpendPeriod.TODAY -> {
            val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            Pair(start, end)
        }
        SpendPeriod.WEEK -> {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            Pair(start, end)
        }
        SpendPeriod.MONTH -> {
            val start = DateTimeUtils.monthStartMillis(selectedMonth, zone)
            val end = DateTimeUtils.monthEndExclusiveMillis(selectedMonth, zone)
            Pair(start, end)
        }
    }
}

/**
 * Computes [BudgetInfo] given a budget limit and the total spent (in paise) for the month.
 * Returns null if [limitPaise] <= 0.
 */
fun computeBudgetInfo(
    limitPaise: Long,
    spentPaise: Long,
    month: YearMonth,
    today: LocalDate
): BudgetInfo? {
    if (limitPaise <= 0L) return null

    val remainingPaise = limitPaise - spentPaise
    val percent = ((spentPaise * 100) / limitPaise).toInt()

    val isCurrentMonth = month.year == today.year && month.monthValue == today.monthValue
    val daysLeft = if (isCurrentMonth) {
        month.lengthOfMonth() - today.dayOfMonth + 1
    } else {
        0
    }

    val perDayPaise = if (remainingPaise > 0L && daysLeft > 0) {
        remainingPaise / daysLeft
    } else {
        null
    }

    return BudgetInfo(
        limitPaise = limitPaise,
        spentPaise = spentPaise,
        remainingPaise = remainingPaise,
        percent = percent,
        perDayPaise = perDayPaise,
        daysLeft = daysLeft
    )
}
