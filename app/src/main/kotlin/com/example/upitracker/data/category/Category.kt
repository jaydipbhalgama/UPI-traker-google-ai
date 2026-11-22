package com.example.upitracker.data.category

/**
 * All spending/income categories for UPI Tracker.
 *
 * IMPORTANT: The [label] strings are stored in the database and must NEVER be renamed.
 * The [emoji] is display-only and can be changed freely.
 *
 * [fromLabel] always returns [Uncategorized] for any unknown/null string — it never crashes.
 */
enum class Category(val label: String, val emoji: String) {
    FOOD_AND_DINING("Food & Dining", "🍔"),
    GROCERIES("Groceries", "🛒"),
    TRANSPORT("Transport", "🚕"),
    FUEL("Fuel", "⛽"),
    SHOPPING("Shopping", "🛍️"),
    BILLS_AND_UTILITIES("Bills & Utilities", "💡"),
    RECHARGE("Recharge", "📱"),
    ENTERTAINMENT("Entertainment", "🎬"),
    HEALTH("Health", "💊"),
    EDUCATION("Education", "🎓"),
    RENT_AND_HOUSING("Rent & Housing", "🏠"),
    TRANSFERS("Transfers", "🔁"),
    CASH("Cash", "💵"),
    INVESTMENTS("Investments", "📈"),
    INCOME("Income", "💰"),
    OTHER("Other", "📦"),
    UNCATEGORIZED("Uncategorized", "❓");

    companion object {
        fun fromLabel(label: String): Category =
            entries.firstOrNull { it.label.equals(label, ignoreCase = false) }
                ?: UNCATEGORIZED
    }
}
