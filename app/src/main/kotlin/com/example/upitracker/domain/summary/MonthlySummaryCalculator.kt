package com.example.upitracker.domain.summary

import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Aggregated income and expense summary for a single calendar month.
 */
data class MonthlySummary(
    val month: YearMonth,
    val totalExpensePaise: Long = 0L,
    val totalIncomePaise: Long = 0L,
    val expenseCount: Int = 0,
    val incomeCount: Int = 0,
    val netPaise: Long = 0L,
    val expenseShareFraction: Float = 0f,
    val incomeShareFraction: Float = 0f,
    val savingsRatePercent: Int? = null
) {
    val hasActivity: Boolean
        get() = totalExpensePaise > 0L || totalIncomePaise > 0L

    val expenseSharePercent: Int
        get() = if (hasActivity) (expenseShareFraction * 100f).roundToInt() else 0

    val incomeSharePercent: Int
        get() = if (hasActivity) (100 - expenseSharePercent).coerceIn(0, 100) else 0
}

/**
 * Pure calculator that computes total expenses (DEBIT) and total income (CREDIT)
 * for a given month (defaulting to the current month).
 */
object MonthlySummaryCalculator {

    fun calculate(
        transactions: List<Transaction>,
        targetMonth: YearMonth = YearMonth.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): MonthlySummary {
        var totalExpense = 0L
        var totalIncome = 0L
        var expenseCount = 0
        var incomeCount = 0

        for (tx in transactions) {
            val txMonth = YearMonth.from(Instant.ofEpochMilli(tx.timestamp).atZone(zoneId))
            if (txMonth != targetMonth) continue

            when (tx.type) {
                TransactionType.DEBIT -> {
                    totalExpense += tx.amountPaise
                    expenseCount++
                }
                TransactionType.CREDIT -> {
                    totalIncome += tx.amountPaise
                    incomeCount++
                }
            }
        }

        val net = totalIncome - totalExpense
        val combinedVolume = totalExpense + totalIncome
        val expenseShare = if (combinedVolume > 0L) {
            totalExpense.toFloat() / combinedVolume.toFloat()
        } else {
            0f
        }
        val incomeShare = if (combinedVolume > 0L) {
            totalIncome.toFloat() / combinedVolume.toFloat()
        } else {
            0f
        }

        val savingsRate = if (totalIncome > 0L) {
            BigDecimal.valueOf(net * 100L)
                .divide(BigDecimal.valueOf(totalIncome), 0, RoundingMode.HALF_UP)
                .toInt()
        } else {
            null
        }

        return MonthlySummary(
            month = targetMonth,
            totalExpensePaise = totalExpense,
            totalIncomePaise = totalIncome,
            expenseCount = expenseCount,
            incomeCount = incomeCount,
            netPaise = net,
            expenseShareFraction = expenseShare,
            incomeShareFraction = incomeShare,
            savingsRatePercent = savingsRate
        )
    }
}
