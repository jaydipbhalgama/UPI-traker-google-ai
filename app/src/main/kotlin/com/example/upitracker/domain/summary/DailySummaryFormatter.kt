package com.example.upitracker.domain.summary

import com.example.upitracker.util.CurrencyUtils

object DailySummaryFormatter {

    fun format(
        todaySpentPaise: Long,
        monthSpentPaise: Long,
        monthlyBudgetPaise: Long?
    ): Pair<String, String> {
        val title = "Daily UPI Summary"
        val body = when {
            monthlyBudgetPaise == null || monthlyBudgetPaise <= 0L -> {
                val x = CurrencyUtils.formatRupees(todaySpentPaise)
                val y = CurrencyUtils.formatRupees(monthSpentPaise)
                "You've spent $x today ($y total this month). Set a budget to track what's left."
            }
            todaySpentPaise == 0L && monthSpentPaise <= monthlyBudgetPaise -> {
                val z = CurrencyUtils.formatRupees(monthlyBudgetPaise - monthSpentPaise)
                "No UPI spending today. You still have $z left this month."
            }
            monthSpentPaise > monthlyBudgetPaise -> {
                val x = CurrencyUtils.formatRupees(todaySpentPaise)
                val o = CurrencyUtils.formatRupees(monthSpentPaise - monthlyBudgetPaise)
                "You've spent $x today. You're $o over your monthly budget."
            }
            else -> {
                val x = CurrencyUtils.formatRupees(todaySpentPaise)
                val z = CurrencyUtils.formatRupees(monthlyBudgetPaise - monthSpentPaise)
                "You've spent $x today and have $z left this month."
            }
        }
        return Pair(title, body)
    }
}
