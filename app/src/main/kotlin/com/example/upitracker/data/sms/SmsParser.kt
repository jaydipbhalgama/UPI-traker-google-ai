package com.example.upitracker.data.sms

import com.example.upitracker.data.local.entity.TransactionType
import java.math.BigDecimal

data class ParsedTransaction(
    val amountPaise: Long,
    val type: TransactionType,
    val merchant: String,
    val bankName: String?,
    val balanceAfterPaise: Long?
)

/**
 * Pure Kotlin parser that extracts UPI transaction details from bank SMS messages.
 * Does not depend on the Android SDK for easy unit testing.
 */
object SmsParser {

    const val UPI_ONLY: Boolean = true

    // Sender filter:
    //   - Must have at least one letter (so pure phone numbers are rejected)
    //   - Must NOT be purely digits / '+' / spaces
    //   - Must be at most 15 characters
    // Examples accepted: "AX-AXISBK-S", "AXISBK", "VM-HDFCBK", "JD-ICICIT-S"
    // Examples rejected: "+919876543210", "9876543210", "   "
    private val SENDER_HAS_LETTER = Regex("[a-zA-Z]")
    private val SENDER_ONLY_DIGITS_PLUS_SPACE = Regex("^[0-9+\\s]+$")

    private fun isSenderValid(sender: String): Boolean {
        if (sender.length > 15) return false
        if (!SENDER_HAS_LETTER.containsMatchIn(sender)) return false
        if (SENDER_ONLY_DIGITS_PLUS_SPACE.matches(sender)) return false
        return true
    }

    // Regex to match VPA handles: e.g. someone@okhdfcbank, merchant.store@upi, 9876543210@paytm
    private val VPA_REGEX = Regex("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9.\\-_]{2,64}")

    // PRIMARY amount pattern: Rs / Rs. / INR / ₹ followed by number with optional commas and decimals
    private val AMOUNT_REGEX = Regex(
        "(?:Rs\\.?|INR|₹)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // FALLBACK amount pattern: type keyword + optional connector + optional currency + number
    // Accepted connectors: by, with, for, of, colon (:)
    // Number must NOT be followed by a letter or more digits.
    // We require either a decimal part OR (connector present AND ≤9 digits).
    // Numbers with 10+ digits are never an amount.
    private val FALLBACK_AMOUNT_REGEX = Regex(
        """(?:debited|credited|paid|sent|received|withdrawn|spent|transferred|deposited)\s*""" +
        """(?:(?:by|with|for|of|:)\s*)?""" +
        """(?:(?:Rs\.?|INR|₹)\s*)?""" +
        """([0-9]{1,9}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]{1,9})""" +
        """(?![0-9a-zA-Z])""",
        RegexOption.IGNORE_CASE
    )

    // Balance pattern
    private val BALANCE_REGEX = Regex(
        "(?:Avl(?:\\s+Bal)?|Available\\s+Balance|Bal)\\.?\\s*(?::|-|is)?\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // VPA followed by parentheses/brackets containing merchant name
    private val VPA_MERCHANT_NAME_REGEX = Regex(
        "[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9.\\-_]{2,64}\\s*[\\(\\[]([^\\)\\]]+)[\\)\\]]"
    )

    // Merchant stop-words: cut at the FIRST occurrence of any of these (case-insensitive, whole-word where applicable)
    // Order matters: longer/more-specific phrases first to avoid partial matches
    private val MERCHANT_STOP_REGEX = Regex(
        """(?i)\b(?:refno|ref\s+no|upi\s+ref|rrn|txn|transaction|if\s+not|not\s+you|for\s+other|available\s+balance|balance|avl\s+bal|avl|dial|download|click|call|bal)\b""" +
        """|(?i)\b(?:ref|utr|on|a/c)\b""" +
        """|\(|\)|[0-9]{6,}| - """
    )

    // Rejection keyword phrases
    private val REJECT_PHRASES = listOf(
        "otp",
        "verification code",
        "one time password",
        "do not share",
        "never share",
        "will be debited",
        "has been requested",
        "collect request",
        "requested to pay",
        "failed",
        "declined",
        "unsuccessful",
        "minimum amount due",
        "total amount due",
        "credit card statement",
        "pre-approved",
        "apply now",
        "click here"
    )

    private val DEBIT_KEYWORDS = listOf(
        "debited",
        "spent",
        "paid",
        "sent",
        "withdrawn",
        "transferred",
        "purchase"
    )

    private val CREDIT_KEYWORDS = listOf(
        "credited",
        "received",
        "deposited",
        "refund"
    )

    private val BANK_MAP = listOf(
        "SBI" to "SBI",
        "HDFC" to "HDFC",
        "ICICI" to "ICICI",
        "AXIS" to "AXIS",
        "KOTAK" to "KOTAK",
        "PNB" to "PNB",
        "BOB" to "BOB",
        "CANARA" to "CANARA",
        "CANBNK" to "CANARA",
        "UNION" to "UNION",
        "IDFC" to "IDFC",
        "YES" to "YES",
        "INDUS" to "INDUSIND",
        "PAYTM" to "PAYTM",
        "AIRTEL" to "AIRTEL",
        "FEDERAL" to "FEDERAL",
        "JUPITER" to "JUPITER",
        "AU" to "AU SMALL FINANCE"
    )

    /**
     * Parses an incoming or imported SMS message into a [ParsedTransaction], or returns null
     * if the message is not a completed UPI transaction.
     */
    fun parse(sender: String, body: String, timestamp: Long): ParsedTransaction? {
        val trimmedSender = sender.trim()
        val trimmedBody = body.trim()

        // Rule a: Sender filter
        if (!isSenderValid(trimmedSender)) {
            return null
        }

        val lowerBody = trimmedBody.lowercase()

        // Rule e: Rejection check
        for (phrase in REJECT_PHRASES) {
            if (lowerBody.contains(phrase)) {
                return null
            }
        }

        // Rule d: UPI only check
        // Pass if: contains "upi", OR contains a VPA (x@y), OR contains UPI-style routing (trf to/from)
        if (UPI_ONLY) {
            val containsUpi = lowerBody.contains("upi")
            val containsVpa = VPA_REGEX.containsMatchIn(trimmedBody)
            val containsTrf = lowerBody.contains("trf to") || lowerBody.contains("trf from")
            if (!containsUpi && !containsVpa && !containsTrf) {
                return null
            }
        }

        // Rule c: Type determination (first occurring debit vs credit keyword)
        val firstDebitIdx = DEBIT_KEYWORDS.map { lowerBody.indexOf(it) }.filter { it >= 0 }.minOrNull()
        val firstCreditIdx = CREDIT_KEYWORDS.map { lowerBody.indexOf(it) }.filter { it >= 0 }.minOrNull()

        if (firstDebitIdx == null && firstCreditIdx == null) {
            return null
        }

        val type: TransactionType = when {
            firstDebitIdx != null && firstCreditIdx != null -> {
                if (firstDebitIdx < firstCreditIdx) TransactionType.DEBIT else TransactionType.CREDIT
            }
            firstDebitIdx != null -> TransactionType.DEBIT
            else -> TransactionType.CREDIT
        }

        // Rule b: Amount parsing — primary then fallback
        val amountPaise = parseAmountToPaise(trimmedBody) ?: return null

        // Rule f: Available balance
        val balanceAfterPaise = parseBalanceToPaise(trimmedBody)

        // Rule g: Merchant extraction
        val merchant = extractMerchant(trimmedBody, type)

        // Rule h: Bank name from sender
        val bankName = extractBankName(trimmedSender)

        return ParsedTransaction(
            amountPaise = amountPaise,
            type = type,
            merchant = merchant,
            bankName = bankName,
            balanceAfterPaise = balanceAfterPaise
        )
    }

    private fun parseAmountToPaise(text: String): Long? {
        // Primary: explicit currency prefix
        val primary = AMOUNT_REGEX.find(text)
        if (primary != null) {
            return parsePaise(primary.groupValues[1])
        }

        // Fallback: type keyword + optional connector + number
        val fallback = FALLBACK_AMOUNT_REGEX.find(text) ?: return null
        val raw = fallback.groupValues[1].replace(",", "").trim()
        val hasDecimal = raw.contains('.')
        // Count digits (no comma, no dot)
        val digitCount = raw.replace(".", "").length

        // Reject if no decimal AND 10+ digits (could be a phone/ref number)
        if (!hasDecimal && digitCount >= 10) return null

        return parsePaise(raw)
    }

    private fun parsePaise(amountStr: String): Long? {
        val cleaned = amountStr.replace(",", "").trim()
        return runCatching {
            val bigDecimal = BigDecimal(cleaned)
            bigDecimal.multiply(BigDecimal(100)).toLong()
        }.getOrNull()
    }

    private fun parseBalanceToPaise(text: String): Long? {
        val match = BALANCE_REGEX.find(text) ?: return null
        val amountStr = match.groupValues[1].replace(",", "").trim()
        return runCatching {
            val bigDecimal = BigDecimal(amountStr)
            bigDecimal.multiply(BigDecimal(100)).toLong()
        }.getOrNull()
    }

    private fun extractMerchant(text: String, type: TransactionType): String {
        // Priority 1: Name in parentheses/brackets right after a VPA
        // Apply stop-word cutting to the bracket content before accepting it
        val vpaNameMatch = VPA_MERCHANT_NAME_REGEX.find(text)
        if (vpaNameMatch != null) {
            val rawBracket = cutAtStopWord(vpaNameMatch.groupValues[1])
            val name = cleanMerchantName(rawBracket)
            if (name.isNotEmpty() && !name.equals("Unknown", ignoreCase = true)) {
                return name
            }
            // Bracket content was a stop-word / junk — fall through to VPA itself
        }

        // Priority 2: VPA itself
        val vpaMatch = VPA_REGEX.find(text)
        if (vpaMatch != null) {
            val vpa = cleanMerchantName(vpaMatch.value)
            if (vpa.isNotEmpty() && !vpa.equals("Unknown", ignoreCase = true)) {
                return vpa
            }
        }

        // Priority 3: Ordered prefix phrases (pattern priority, not text position)
        // DEBIT: trf to > paid to > sent to > transferred to > to VPA > to
        // CREDIT: trf from > received from > from > by (last, skip if followed by digit/currency)
        val prefixes: List<Pair<String, Boolean>> = if (type == TransactionType.DEBIT) {
            listOf(
                "trf to" to false,
                "paid to" to false,
                "sent to" to false,
                "transferred to" to false,
                "to vpa" to false,
                "to" to false
            )
        } else {
            listOf(
                "trf from" to false,
                "received from" to false,
                "from" to false,
                "by" to true   // true = "by" special: skip when followed by digit or currency symbol
            )
        }

        for ((prefix, isByPattern) in prefixes) {
            val regex = Regex("\\b${Regex.escape(prefix)}\\s+", RegexOption.IGNORE_CASE)
            val match = regex.find(text) ?: continue
            val after = text.substring(match.range.last + 1).trim()

            if (isByPattern) {
                // Skip "by" when followed by a digit or currency symbol
                val firstChar = after.firstOrNull()
                if (firstChar != null && (firstChar.isDigit() || firstChar == '₹' || firstChar == '$' || firstChar == '€')) {
                    continue
                }
            }

            val candidate = cutAtStopWord(after)
            val cleaned = cleanMerchantName(candidate)
            if (cleaned.isNotEmpty() && !cleaned.equals("Unknown", ignoreCase = true)) {
                return cleaned
            }
        }

        return "Unknown"
    }

    /**
     * Cuts [text] at the first merchant stop-word/pattern and returns the portion before it.
     */
    private fun cutAtStopWord(text: String): String {
        val stopMatch = MERCHANT_STOP_REGEX.find(text) ?: return text
        return text.substring(0, stopMatch.range.first)
    }

    private fun cleanMerchantName(raw: String): String {
        var cleaned = raw
            .trim()
            // Trim leading/trailing punctuation: . , : ; - ?
            .trimStart('.', ',', ':', ';', '-', '?', ' ')
            .trimEnd('.', ',', ':', ';', '-', '?', ' ')
            .replace(Regex("\\s+"), " ") // collapse spaces
            .trim()

        if (cleaned.length > 50) {
            cleaned = cleaned.substring(0, 50).trim()
        }
        return if (cleaned.isBlank()) "Unknown" else cleaned
    }

    private fun extractBankName(sender: String): String? {
        val upperSender = sender.uppercase()
        for ((key, name) in BANK_MAP) {
            if (upperSender.contains(key)) {
                return name
            }
        }
        return null
    }
}
