package com.example.upitracker.ui.insights

import androidx.compose.ui.graphics.Color

object ChartColors {

    // Palette (7 colours, light theme — colorblind-distinguishable & harmonized with fintech palette)
    val LIGHT_PALETTE: List<Color> = listOf(
        Color(0xFF0B6E4F), // Emerald
        Color(0xFF2563EB), // Cobalt
        Color(0xFFD97706), // Amber
        Color(0xFF7C3AED), // Violet
        Color(0xFFDC2626), // Crimson
        Color(0xFF0891B2), // Cyan
        Color(0xFF64748B)  // Smaller categories (Slate)
    )

    // Dark theme variants
    val DARK_PALETTE: List<Color> = listOf(
        Color(0xFF34D399), // Electric Mint
        Color(0xFF60A5FA), // Sky Blue
        Color(0xFFFBBF24), // Amber Gold
        Color(0xFFA78BFA), // Lavender
        Color(0xFFF87171), // Coral Red
        Color(0xFF22D3EE), // Aqua
        Color(0xFF94A3B8)  // Smaller categories (Cool Slate)
    )

    // Daily bar chart colors
    val DAILY_BAR_LIGHT = Color(0xFF2563EB)
    val DAILY_BAR_DARK = Color(0xFF60A5FA)
    val DAILY_HIGHEST_LIGHT = Color(0xFFDC2626)
    val DAILY_HIGHEST_DARK = Color(0xFFF87171)

    // Monthly chart colors
    val MONTHLY_SPENT_LIGHT = Color(0xFFDC2626)
    val MONTHLY_SPENT_DARK = Color(0xFFF87171)
    val MONTHLY_RECEIVED_LIGHT = Color(0xFF0B6E4F)
    val MONTHLY_RECEIVED_DARK = Color(0xFF34D399)

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
