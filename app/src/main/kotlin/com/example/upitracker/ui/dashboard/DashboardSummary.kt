package com.example.upitracker.ui.dashboard

import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

data class TopMerchantSummary(
    val merchantName: String,
    val totalPaise: Long,
    val count: Int
)

data class CategorySummary(
    val category: Category,
    val totalPaise: Long,
    val count: Int,
    /** Whole-number percentage of totalSpentPaise; 0 if totalSpent == 0 */
    val percentOfTotal: Int
)

data class DashboardSummary(
    val totalSpentPaise: Long,
    val totalReceivedPaise: Long,
    val netPaise: Long,
    val transactionCount: Int,
    val averageDailySpendPaise: Long,
    val biggestExpense: Transaction?,
    val topMerchants: List<TopMerchantSummary>,
    val recentTransactions: List<Transaction>,
    val categoryBreakdown: List<CategorySummary>,
    val uncategorizedCount: Int
)

object DashboardCalculator {

    fun calculate(
        transactions: List<Transaction>,
        selectedMonth: YearMonth,
        now: LocalDate = LocalDate.now()
    ): DashboardSummary {
        var totalSpent = 0L
        var totalReceived = 0L
        val debits = mutableListOf<Transaction>()

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.DEBIT -> {
                    totalSpent += tx.amountPaise
                    debits.add(tx)
                }
                TransactionType.CREDIT -> {
                    totalReceived += tx.amountPaise
                }
            }
        }

        val net = totalReceived - totalSpent
        val count = transactions.size

        // Calculate elapsed days for average daily spend
        val currentYm = YearMonth.from(now)
        val elapsedDays = when {
            selectedMonth == currentYm -> now.dayOfMonth
            selectedMonth.isBefore(currentYm) -> selectedMonth.lengthOfMonth()
            else -> selectedMonth.lengthOfMonth()
        }

        val avgDailySpend = if (totalSpent > 0L && elapsedDays > 0) {
            BigDecimal.valueOf(totalSpent)
                .divide(BigDecimal.valueOf(elapsedDays.toLong()), 0, RoundingMode.HALF_UP)
                .longValueExact()
        } else {
            0L
        }

        // Biggest expense
        val biggestExpense = debits.maxByOrNull { it.amountPaise }

        // Top merchants (DEBIT only, group case-insensitively, ignore "Unknown" unless nothing else exists)
        val hasNonUnknown = debits.any { !it.merchant.trim().equals("unknown", ignoreCase = true) }
        val eligibleDebits = if (hasNonUnknown) {
            debits.filter { !it.merchant.trim().equals("unknown", ignoreCase = true) }
        } else {
            debits
        }

        val topMerchants = eligibleDebits
            .groupBy { it.merchant.trim().lowercase() }
            .map { (_, groupTxs) ->
                TopMerchantSummary(
                    merchantName = groupTxs.first().merchant.trim(),
                    totalPaise = groupTxs.sumOf { it.amountPaise },
                    count = groupTxs.size
                )
            }
            .sortedWith(compareByDescending<TopMerchantSummary> { it.totalPaise }.thenByDescending { it.count })
            .take(5)

        // Category breakdown (DEBIT only, sorted by total descending)
        val categoryBreakdown = debits
            .groupBy { Category.fromLabel(it.category) }
            .map { (cat, groupTxs) ->
                val catTotal = groupTxs.sumOf { it.amountPaise }
                val pct = if (totalSpent > 0L) {
                    ((catTotal * 100L) / totalSpent).toInt()
                } else 0
                CategorySummary(
                    category = cat,
                    totalPaise = catTotal,
                    count = groupTxs.size,
                    percentOfTotal = pct
                )
            }
            .sortedByDescending { it.totalPaise }

        // Count uncategorized in the whole month (both debit and credit)
        val uncategorizedCount = transactions.count { it.category == Category.UNCATEGORIZED.label }

        val recentTransactions = transactions
            .sortedByDescending { it.timestamp }
            .take(10)

        return DashboardSummary(
            totalSpentPaise = totalSpent,
            totalReceivedPaise = totalReceived,
            netPaise = net,
            transactionCount = count,
            averageDailySpendPaise = avgDailySpend,
            biggestExpense = biggestExpense,
            topMerchants = topMerchants,
            recentTransactions = recentTransactions,
            categoryBreakdown = categoryBreakdown,
            uncategorizedCount = uncategorizedCount
        )
    }
}
