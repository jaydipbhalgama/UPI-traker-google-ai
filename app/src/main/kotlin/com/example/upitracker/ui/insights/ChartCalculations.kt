package com.example.upitracker.ui.insights

import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.ui.dashboard.CategorySummary
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.floor

enum class ComparisonMode {
    SAME_DAYS_LAST_MONTH,
    PREVIOUS_MONTH
}

data class ComparisonResult(
    val currentSpentPaise: Long,
    val previousSpentPaise: Long,
    val pct: Int?,
    val mode: ComparisonMode
)

data class CategorySlice(
    val category: Category?,
    val label: String,
    val emoji: String,
    val totalPaise: Long,
    val count: Int,
    val fraction: Float,
    val percentDisplay: String,
    val isMerged: Boolean,
    val mergedCategoryCount: Int = 0
)

data class MonthTotals(
    val month: YearMonth,
    val spentPaise: Long,
    val receivedPaise: Long
)

object ChartCalculations {

    /**
     * Integer math only:
     * rupees = paise / 100 (truncate).
     * < 1,000: "₹950"
     * 1,000 .. < 1,00,000: thousands with at most 1 decimal using "K", truncated not rounded, ".0" dropped.
     * 1,00,000 .. < 1,00,00,000: "L" (lakhs)
     * >= 1,00,00,000: "Cr" (crores)
     * Negative values get a leading minus.
     */
    fun formatRupeesCompact(paise: Long): String {
        val isNegative = paise < 0
        val absPaise = if (isNegative) -paise else paise
        val rupees = absPaise / 100

        val formatted = when {
            rupees < 1_000 -> "$rupees"
            rupees < 1_00_000 -> formatWithUnit(rupees, 1_000, "K")
            rupees < 1_00_00_000 -> formatWithUnit(rupees, 1_00_000, "L")
            else -> formatWithUnit(rupees, 1_00_00_000, "Cr")
        }

        val prefix = if (isNegative) "-₹" else "₹"
        return "$prefix$formatted"
    }

    private fun formatWithUnit(value: Long, divisor: Long, unit: String): String {
        val whole = value / divisor
        val remainder = value % divisor
        val tenths = (remainder * 10) / divisor
        return if (tenths > 0) {
            "$whole.$tenths$unit"
        } else {
            "$whole$unit"
        }
    }

    /**
     * Returns the smallest value of the form 1, 2 or 5 times a power of ten (in paise)
     * that is >= maxPaise, with a minimum of 10000 (Rs 100).
     */
    fun niceCeiling(maxPaise: Long): Long {
        if (maxPaise <= 10000L) return 10000L

        var powerOf10 = 1L
        while (powerOf10 * 10 <= maxPaise && powerOf10 <= Long.MAX_VALUE / 10) {
            powerOf10 *= 10
        }

        val multipliers = longArrayOf(1, 2, 5, 10)
        for (m in multipliers) {
            val candidate = m * powerOf10
            if (candidate >= maxPaise) {
                return candidate
            }
        }
        return 10 * powerOf10
    }

    /**
     * Returns bar index or null if count <= 0, width <= 0, or x outside [0, width).
     */
    fun barIndexAt(x: Float, width: Float, count: Int): Int? {
        if (count <= 0 || width <= 0f) return null
        if (x < 0f || x >= width) return null
        val barWidth = width / count
        val index = floor(x / barWidth).toInt()
        return index.coerceIn(0, count - 1)
    }

    /**
     * Percentage change between current and previous amounts:
     * returns null when previous == 0.
     * Otherwise ((current - previous) * 100 / previous) as BigDecimal rounded HALF_UP.
     */
    fun pctChange(current: Long, previous: Long): Int? {
        if (previous == 0L) return null
        val diff = (current - previous) * 100
        return BigDecimal.valueOf(diff)
            .divide(BigDecimal.valueOf(previous), 0, RoundingMode.HALF_UP)
            .toInt()
    }

    /**
     * LongArray of size = days in month. Index = dayOfMonth - 1.
     * Sums only DEBIT, ignores transactions outside the month.
     */
    fun dailySpend(
        transactions: List<Transaction>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault()
    ): LongArray {
        val daysInMonth = month.lengthOfMonth()
        val result = LongArray(daysInMonth)

        for (tx in transactions) {
            if (tx.type != TransactionType.DEBIT) continue
            val zdt = Instant.ofEpochMilli(tx.timestamp).atZone(zone)
            val txMonth = YearMonth.from(zdt)
            if (txMonth == month) {
                val dayIndex = zdt.dayOfMonth - 1
                if (dayIndex in 0 until daysInMonth) {
                    result[dayIndex] += tx.amountPaise
                }
            }
        }
        return result
    }

    /**
     * visibleDays: today's day-of-month for the current month, otherwise the full month length.
     */
    fun visibleDays(
        month: YearMonth,
        today: LocalDate
    ): Int {
        val currentMonth = YearMonth.from(today)
        return when {
            month == currentMonth -> today.dayOfMonth
            month.isBefore(currentMonth) -> month.lengthOfMonth()
            else -> month.lengthOfMonth()
        }
    }

    /**
     * highestDay: day number (1-based) with the largest amount (earliest day on ties),
     * or null if every day is zero.
     */
    fun highestDay(array: LongArray): Int? {
        var maxAmount = 0L
        var bestDay: Int? = null
        for (i in array.indices) {
            if (array[i] > maxAmount) {
                maxAmount = array[i]
                bestDay = i + 1
            }
        }
        return bestDay
    }

    /**
     * categorySlices: input is sorted (Category, totalPaise, count).
     * Output up to 6 individual slices; if > 6, merge remaining into "Smaller categories".
     */
    fun categorySlices(
        debitByCategory: List<CategorySummary>,
        maxSlices: Int = 6
    ): List<CategorySlice> {
        if (debitByCategory.isEmpty()) return emptyList()

        val totalSpent = debitByCategory.sumOf { it.totalPaise }
        if (totalSpent <= 0L) return emptyList()

        val slices = mutableListOf<CategorySlice>()

        if (debitByCategory.size <= maxSlices) {
            for (item in debitByCategory) {
                val fraction = item.totalPaise.toFloat() / totalSpent.toFloat()
                val pctDisplay = formatPercentDisplay(item.percentOfTotal, item.totalPaise)
                slices.add(
                    CategorySlice(
                        category = item.category,
                        label = item.category.label,
                        emoji = item.category.emoji,
                        totalPaise = item.totalPaise,
                        count = item.count,
                        fraction = fraction,
                        percentDisplay = pctDisplay,
                        isMerged = false
                    )
                )
            }
        } else {
            val topList = debitByCategory.take(maxSlices)
            val mergedList = debitByCategory.drop(maxSlices)

            for (item in topList) {
                val fraction = item.totalPaise.toFloat() / totalSpent.toFloat()
                val pctDisplay = formatPercentDisplay(item.percentOfTotal, item.totalPaise)
                slices.add(
                    CategorySlice(
                        category = item.category,
                        label = item.category.label,
                        emoji = item.category.emoji,
                        totalPaise = item.totalPaise,
                        count = item.count,
                        fraction = fraction,
                        percentDisplay = pctDisplay,
                        isMerged = false
                    )
                )
            }

            val mergedPaise = mergedList.sumOf { it.totalPaise }
            val mergedCount = mergedList.sumOf { it.count }
            val mergedFraction = mergedPaise.toFloat() / totalSpent.toFloat()
            val mergedPct = ((mergedPaise * 100L) / totalSpent).toInt()
            val mergedPctDisplay = formatPercentDisplay(mergedPct, mergedPaise)

            slices.add(
                CategorySlice(
                    category = null,
                    label = "Smaller categories",
                    emoji = "📦",
                    totalPaise = mergedPaise,
                    count = mergedCount,
                    fraction = mergedFraction,
                    percentDisplay = mergedPctDisplay,
                    isMerged = true,
                    mergedCategoryCount = mergedList.size
                )
            )
        }

        return slices
    }

    private fun formatPercentDisplay(pct: Int, totalPaise: Long): String {
        return if (pct == 0 && totalPaise > 0L) {
            "<1%"
        } else {
            "$pct%"
        }
    }

    /**
     * monthlyTotals covering up to `count` months ending with endMonth, ordered oldest to newest.
     * Months before oldestMonth are omitted. If oldestMonth is null, return only endMonth.
     */
    fun monthlyTotals(
        transactions: List<Transaction>,
        endMonth: YearMonth,
        count: Int = 6,
        oldestMonth: YearMonth? = null,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<MonthTotals> {
        val months = mutableListOf<YearMonth>()
        var current = endMonth
        for (i in 0 until count) {
            if (oldestMonth == null) {
                months.add(endMonth)
                break
            }
            if (current.isBefore(oldestMonth)) {
                break
            }
            months.add(current)
            current = current.minusMonths(1)
        }
        val orderedMonths = months.reversed()

        val totalsMap = mutableMapOf<YearMonth, Pair<Long, Long>>()
        for (m in orderedMonths) {
            totalsMap[m] = Pair(0L, 0L)
        }

        for (tx in transactions) {
            val zdt = Instant.ofEpochMilli(tx.timestamp).atZone(zone)
            val txMonth = YearMonth.from(zdt)
            val currentPair = totalsMap[txMonth]
            if (currentPair != null) {
                val (spent, received) = currentPair
                totalsMap[txMonth] = when (tx.type) {
                    TransactionType.DEBIT -> Pair(spent + tx.amountPaise, received)
                    TransactionType.CREDIT -> Pair(spent, received + tx.amountPaise)
                }
            }
        }

        return orderedMonths.map { ym ->
            val (spent, received) = totalsMap[ym] ?: Pair(0L, 0L)
            MonthTotals(
                month = ym,
                spentPaise = spent,
                receivedPaise = received
            )
        }
    }

    /**
     * comparison:
     * - If selectedMonth == currentMonth: compare day 1 to today's day-of-month against previous
     *   month's days 1 to same day number, capped at previous month's length. Mode = SAME_DAYS_LAST_MONTH.
     * - Otherwise: compare full selected month with full previous month. Mode = PREVIOUS_MONTH.
     * Only DEBIT is counted.
     */
    fun comparison(
        transactions: List<Transaction>,
        selectedMonth: YearMonth,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): ComparisonResult {
        val currentMonth = YearMonth.from(today)
        val isCurrent = (selectedMonth == currentMonth)
        val previousMonth = selectedMonth.minusMonths(1)

        var currentSpent = 0L
        var previousSpent = 0L

        if (isCurrent) {
            val maxDayCurrent = today.dayOfMonth
            val maxDayPrevious = maxDayCurrent.coerceAtMost(previousMonth.lengthOfMonth())

            for (tx in transactions) {
                if (tx.type != TransactionType.DEBIT) continue
                val zdt = Instant.ofEpochMilli(tx.timestamp).atZone(zone)
                val txMonth = YearMonth.from(zdt)
                val day = zdt.dayOfMonth

                if (txMonth == selectedMonth && day <= maxDayCurrent) {
                    currentSpent += tx.amountPaise
                } else if (txMonth == previousMonth && day <= maxDayPrevious) {
                    previousSpent += tx.amountPaise
                }
            }

            val pct = pctChange(currentSpent, previousSpent)
            return ComparisonResult(
                currentSpentPaise = currentSpent,
                previousSpentPaise = previousSpent,
                pct = pct,
                mode = ComparisonMode.SAME_DAYS_LAST_MONTH
            )
        } else {
            for (tx in transactions) {
                if (tx.type != TransactionType.DEBIT) continue
                val zdt = Instant.ofEpochMilli(tx.timestamp).atZone(zone)
                val txMonth = YearMonth.from(zdt)

                if (txMonth == selectedMonth) {
                    currentSpent += tx.amountPaise
                } else if (txMonth == previousMonth) {
                    previousSpent += tx.amountPaise
                }
            }

            val pct = pctChange(currentSpent, previousSpent)
            return ComparisonResult(
                currentSpentPaise = currentSpent,
                previousSpentPaise = previousSpent,
                pct = pct,
                mode = ComparisonMode.PREVIOUS_MONTH
            )
        }
    }
}
