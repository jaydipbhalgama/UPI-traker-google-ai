package com.example.upitracker.ui.navigation

import androidx.compose.runtime.saveable.Saver
import com.example.upitracker.data.category.Category

/**
 * Screen destination model for UPI Tracker.
 */
sealed interface Screen {
    data object Home : Screen
    data object Insights : Screen
    data class Transactions(
        val category: Category? = null,
        val debitOnly: Boolean = false
    ) : Screen
    data object Budgets : Screen
    data object Settings : Screen
    data object UnrecognisedSms : Screen

    companion object {
        val Saver: Saver<Screen, String> = Saver(
            save = { screen ->
                when (screen) {
                    is Home -> "home"
                    is Insights -> "insights"
                    is Transactions -> {
                        val cat = screen.category?.name ?: ""
                        "transactions:$cat:${screen.debitOnly}"
                    }
                    is Budgets -> "budgets"
                    is Settings -> "settings"
                    is UnrecognisedSms -> "unrecognised_sms"
                }
            },
            restore = { value ->
                when {
                    value == "home" -> Home
                    value == "insights" -> Insights
                    value.startsWith("transactions:") -> {
                        val parts = value.split(":")
                        val catName = parts.getOrNull(1)
                        val cat = if (!catName.isNullOrEmpty()) {
                            runCatching { Category.valueOf(catName) }.getOrNull()
                        } else null
                        val debitOnly = parts.getOrNull(2)?.toBooleanStrictOrNull() ?: false
                        Transactions(cat, debitOnly)
                    }
                    value == "transactions" -> Transactions()
                    value == "budgets" -> Budgets
                    value == "settings" -> Settings
                    value == "unrecognised_sms" -> UnrecognisedSms
                    else -> Home
                }
            }
        )
    }
}

/**
 * Pure Kotlin back-navigation rule:
 * - Insights -> Home
 * - Transactions -> Home
 * - Budgets -> Home
 * - Settings -> Home
 * - UnrecognisedSms -> Home
 * - Home -> null (null = exit app)
 */
fun backTarget(current: Screen): Screen? = when (current) {
    is Screen.Home -> null
    is Screen.Insights -> Screen.Home
    is Screen.Transactions -> Screen.Home
    is Screen.Budgets -> Screen.Home
    is Screen.Settings -> Screen.Home
    is Screen.UnrecognisedSms -> Screen.Home
}
