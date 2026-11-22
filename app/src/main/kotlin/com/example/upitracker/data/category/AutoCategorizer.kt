package com.example.upitracker.data.category

import com.example.upitracker.data.local.entity.TransactionType

/**
 * Pure Kotlin auto-categorizer. No Android dependencies → fully unit-testable.
 *
 * Priority order:
 *  a) Blank / unknown merchant → Uncategorized (never rules or keyword match)
 *  b) User rule (from [rules] map)
 *  c) MY_MERCHANT_RULES keyword list (from [CategoryKeywords])
 *  d) Built-in keyword groups (in order from [CategoryKeywords.KEYWORD_RULES])
 *     Income group is only checked when [type] == CREDIT.
 *  e) VPA whose local part (before "@") is exactly 10 digits → Transfers
 *  f) Uncategorized
 */
object AutoCategorizer {

    /** Regex for a UPI VPA: something@something */
    private val VPA_REGEX = Regex("""[^\s@]+@[^\s@]+""")
    /** Regex for a 10-digit phone number used as UPI local part */
    private val PHONE_LOCAL = Regex("""^\d{10}$""")

    /**
     * Normalises a raw merchant name to the canonical merchant key:
     * trim, lowercase, collapse repeated whitespace.
     */
    fun merchantKey(merchant: String): String =
        merchant.trim().lowercase().replace(Regex("""\s+"""), " ")

    /**
     * Splits [key] on any non-alphanumeric character to produce tokens.
     * Used for short-keyword (≤4 chars) whole-token matching.
     */
    private fun tokens(key: String): Set<String> =
        key.split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotEmpty() }.toSet()

    /**
     * Returns true if [keyword] matches [merchantKey]:
     *  - 4 chars or fewer, no spaces: must equal a whole token.
     *  - 5+ chars, or contains a space: must appear as a substring.
     */
    private fun matches(merchantKey: String, keyword: String): Boolean =
        if (keyword.length <= 4 && !keyword.contains(' ')) {
            tokens(merchantKey).contains(keyword)
        } else {
            merchantKey.contains(keyword, ignoreCase = false)
        }

    /** Returns first matching category from a list of keywords, or null. */
    private fun firstMatch(merchantKey: String, keywords: List<String>): Boolean =
        keywords.any { matches(merchantKey, it) }

    fun categorize(
        merchant: String,
        type: TransactionType,
        rules: Map<String, String>
    ): Category {
        val key = merchantKey(merchant)

        // b) Blank / unknown merchant: ALWAYS Uncategorized (even if a rule exists for "unknown")
        if (key.isBlank() || key == "unknown") return Category.UNCATEGORIZED

        // a) User rule
        val userRuleLabel = rules[key]
        if (userRuleLabel != null) {
            val cat = Category.fromLabel(userRuleLabel)
            if (cat != Category.UNCATEGORIZED || userRuleLabel == Category.UNCATEGORIZED.label) {
                return cat
            }
        }

        // c) MY_MERCHANT_RULES keyword list
        for ((myKey, myCat) in CategoryKeywords.MY_MERCHANT_RULES) {
            if (matches(key, myKey)) return myCat
        }

        // d) Income (CREDIT only) checked first among keyword groups
        if (type == TransactionType.CREDIT && firstMatch(key, CategoryKeywords.INCOME)) {
            return Category.INCOME
        }

        // d) Built-in keyword groups
        for ((keywords, category) in CategoryKeywords.KEYWORD_RULES) {
            if (firstMatch(key, keywords)) return category
        }

        // e) VPA with 10-digit local part → Transfers
        val vpaMatch = VPA_REGEX.find(key)
        if (vpaMatch != null) {
            val localPart = vpaMatch.value.substringBefore("@")
            if (PHONE_LOCAL.matches(localPart)) return Category.TRANSFERS
        }

        return Category.UNCATEGORIZED
    }
}
