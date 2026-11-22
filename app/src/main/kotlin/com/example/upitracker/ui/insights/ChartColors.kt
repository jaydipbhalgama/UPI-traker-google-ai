package com.example.upitracker.ui.insights

import androidx.compose.ui.graphics.Color

object ChartColors {

    // Palette (7 colours, light theme)
    // #0072B2, #E69F00, #009E73, #CC79A7, #D55E00, #56B4E9, and #6B7280 for "Smaller categories"
    val LIGHT_PALETTE: List<Color> = listOf(
        Color(0xFF0072B2),
        Color(0xFFE69F00),
        Color(0xFF009E73),
        Color(0xFFCC79A7),
        Color(0xFFD55E00),
        Color(0xFF56B4E9),
        Color(0xFF6B7280) // Smaller categories
    )

    // Dark theme variants
    // #5DADE2, #F5B041, #48C9B0, #E8A0C8, #F08A5D, #85C1E9, #9CA3AF
    val DARK_PALETTE: List<Color> = listOf(
        Color(0xFF5DADE2),
        Color(0xFFF5B041),
        Color(0xFF48C9B0),
        Color(0xFFE8A0C8),
        Color(0xFFF08A5D),
        Color(0xFF85C1E9),
        Color(0xFF9CA3AF) // Smaller categories
    )

    // Daily bar chart colors
    val DAILY_BAR_LIGHT = Color(0xFF0072B2)
    val DAILY_BAR_DARK = Color(0xFF5DADE2)
    val DAILY_HIGHEST_LIGHT = Color(0xFFD55E00)
    val DAILY_HIGHEST_DARK = Color(0xFFF08A5D)

    // Monthly chart colors
    val MONTHLY_SPENT_LIGHT = Color(0xFFD55E00)
    val MONTHLY_SPENT_DARK = Color(0xFFF08A5D)
    val MONTHLY_RECEIVED_LIGHT = Color(0xFF009E73)
    val MONTHLY_RECEIVED_DARK = Color(0xFF48C9B0)

    fun getCategorySliceColor(index: Int, isMerged: Boolean, isDark: Boolean): Color {
        val palette = if (isDark) DARK_PALETTE else LIGHT_PALETTE
        return if (isMerged) {
            palette.last()
        } else {
            palette[index.coerceIn(0, 5)]
        }
    }

    fun colorForCategoryOrdinal(ordinal: Int, isDark: Boolean): Color {
        return getCategorySliceColor(ordinal % 6, isMerged = false, isDark = isDark)
    }
}
