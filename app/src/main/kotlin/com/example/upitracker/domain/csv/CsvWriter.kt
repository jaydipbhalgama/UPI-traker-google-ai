package com.example.upitracker.domain.csv

import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object CsvWriter {

    private const val CRLF = "\r\n"
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT)
    private val DANGEROUS_PREFIXES = charArrayOf('=', '+', '-', '@', '\t', '\r')

    /**
     * Writes CSV formatted transactions to [out].
     *
     * Header: Date,Time,Type,Amount (INR),Merchant,Category,Note,Bank,Balance After (INR)
     * Every line ends with CRLF.
     */
    fun writeCsv(
        out: Appendable,
        transactions: List<Transaction>,
        zone: ZoneId = ZoneId.systemDefault()
    ) {
        // 1. Header
        out.append("Date,Time,Type,Amount (INR),Merchant,Category,Note,Bank,Balance After (INR)")
        out.append(CRLF)

        // 2. Data rows
        for (tx in transactions) {
            val zdt = Instant.ofEpochMilli(tx.timestamp).atZone(zone)
            val date = zdt.format(DATE_FORMATTER)
            val time = zdt.format(TIME_FORMATTER)
            val type = if (tx.type == TransactionType.DEBIT) "Debit" else "Credit"
            val amount = formatPaiseToPlainRupees(tx.amountPaise)

            val merchant = sanitizeText(tx.merchant)
            val category = sanitizeText(tx.category)
            val note = sanitizeText(tx.note.orEmpty())
            val bank = sanitizeText(tx.bankName.orEmpty())
            val balance = tx.balanceAfterPaise?.let { formatPaiseToPlainRupees(it) } ?: ""

            out.append(escapeCsv(date)).append(',')
            out.append(escapeCsv(time)).append(',')
            out.append(escapeCsv(type)).append(',')
            out.append(escapeCsv(amount)).append(',')
            out.append(escapeCsv(merchant)).append(',')
            out.append(escapeCsv(category)).append(',')
            out.append(escapeCsv(note)).append(',')
            out.append(escapeCsv(bank)).append(',')
            out.append(escapeCsv(balance))
            out.append(CRLF)
        }
    }

    private fun formatPaiseToPlainRupees(paise: Long): String {
        return BigDecimal.valueOf(paise, 2).toPlainString()
    }

    /**
     * Spreadsheet formula protection for text columns.
     * Prefixes with a single quote if starting with =, +, -, @, \t, or \r.
     */
    private fun sanitizeText(value: String): String {
        if (value.isEmpty()) return ""
        val firstChar = value[0]
        return if (firstChar in DANGEROUS_PREFIXES) {
            "'$value"
        } else {
            value
        }
    }

    /**
     * Standard RFC 4180 CSV escaping:
     * If the string contains comma, double quote, CR, or LF, wrap in double quotes
     * and double any inner double quotes.
     */
    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') ||
                value.contains('"') ||
                value.contains('\r') ||
                value.contains('\n')

        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
