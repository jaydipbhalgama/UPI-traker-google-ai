package com.example.upitracker.util

import java.util.Locale

object CurrencyUtils {

    /**
     * Formats an amount in paise to Indian Rupee representation with ₹ symbol,
     * Indian digit grouping (e.g. ₹1,25,000.00), and always 2 decimal places.
     * Negative values are prefixed with a leading minus (e.g. -₹50.00).
     */
    fun formatRupees(paise: Long): String {
        val isNegative = paise < 0
        val absPaise = if (isNegative) -paise else paise
        val rupees = absPaise / 100
        val fraction = absPaise % 100

        val rupeesStr = rupees.toString()
        val formattedRupees = if (rupeesStr.length <= 3) {
            rupeesStr
        } else {
            val last3 = rupeesStr.takeLast(3)
            val remaining = rupeesStr.dropLast(3)
            val sb = StringBuilder()
            var count = 0
            for (i in remaining.indices.reversed()) {
                if (count > 0 && count % 2 == 0) {
                    sb.append(',')
                }
                sb.append(remaining[i])
                count++
            }
            sb.reverse().toString() + "," + last3
        }

        val fractionStr = String.format(Locale.US, "%02d", fraction)
        val prefix = if (isNegative) "-₹" else "₹"
        return "$prefix$formattedRupees.$fractionStr"
    }
}
