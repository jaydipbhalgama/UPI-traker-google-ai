package com.example.upitracker.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// ─── Fintech Color System ("Ledger Mint & Obsidian Slate") ───────────────────

// Light Theme Palette
val primaryLight = Color(0xFF0B6E4F)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFD5F5E6)
val onPrimaryContainerLight = Color(0xFF053323)

val backgroundLight = Color(0xFFF5F7FA)
val onBackgroundLight = Color(0xFF0F172A)
val surfaceLight = Color(0xFFFFFFFF)
val onSurfaceLight = Color(0xFF0F172A)

val errorLight = Color(0xFFB91C1C)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFEE2E2)
val onErrorContainerLight = Color(0xFF5C0B0B)

val secondaryLight = Color(0xFF1E3A5F)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFE2ECF7)
val onSecondaryContainerLight = Color(0xFF10243D)

val tertiaryLight = Color(0xFF5B4B8A)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFEDE8FA)
val onTertiaryContainerLight = Color(0xFF231942)

val surfaceVariantLight = Color(0xFFEDF2F7)
val onSurfaceVariantLight = Color(0xFF475569)
val outlineLight = Color(0xFF64748B)
val outlineVariantLight = Color(0xFFDCE3EC)

// Dark Theme Palette
val primaryDark = Color(0xFF4ADEA0)
val onPrimaryDark = Color(0xFF042F1E)
val primaryContainerDark = Color(0xFF0F4C37)
val onPrimaryContainerDark = Color(0xFFC6FCE3)

val backgroundDark = Color(0xFF0B0F14)
val onBackgroundDark = Color(0xFFF1F5F9)
val surfaceDark = Color(0xFF131A23)
val onSurfaceDark = Color(0xFFF1F5F9)

val errorDark = Color(0xFFF87171)
val onErrorDark = Color(0xFF450A0A)
val errorContainerDark = Color(0xFF4C1518)
val onErrorContainerDark = Color(0xFFFEE2E2)

val secondaryDark = Color(0xFF93C5FD)
val onSecondaryDark = Color(0xFF0D2847)
val secondaryContainerDark = Color(0xFF1D334D)
val onSecondaryContainerDark = Color(0xFFDBEAFE)

val tertiaryDark = Color(0xFFC4B5FD)
val onTertiaryDark = Color(0xFF281B4F)
val tertiaryContainerDark = Color(0xFF3A2D63)
val onTertiaryContainerDark = Color(0xFFEDE9FE)

val surfaceVariantDark = Color(0xFF1C2532)
val onSurfaceVariantDark = Color(0xFF94A3B8)
val outlineDark = Color(0xFF64748B)
val outlineVariantDark = Color(0xFF283445)

// Semantic Transaction & Budget Status Colors
val CreditColorLight = Color(0xFF0B6E43)
val CreditColorDark = Color(0xFF34D399)
val DebitColor = Color(0xFFB91C1C)

val NearLimitLight = Color(0xFFB45309)
val NearLimitDark = Color(0xFFFBBF24)

// ─── WCAG Contrast Calculation ───────────────────────────────────────────────

/**
 * Calculates the WCAG 2.1 relative luminance of a color.
 * Normalized channel values are linearized before computing luminance.
 */
fun relativeLuminance(color: Color): Double {
    fun linearize(channel: Float): Double {
        val v = channel.toDouble().coerceIn(0.0, 1.0)
        return if (v <= 0.04045) {
            v / 12.92
        } else {
            ((v + 0.055) / 1.055).pow(2.4)
        }
    }

    val r = linearize(color.red)
    val g = linearize(color.green)
    val b = linearize(color.blue)

    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/**
 * Calculates the WCAG 2.1 contrast ratio between two colors.
 * Formula: (L1 + 0.05) / (L2 + 0.05), where L1 is the lighter luminance and L2 is the darker.
 * Returns a value in [1.0, 21.0].
 */
fun contrastRatio(foreground: Color, background: Color): Double {
    val l1 = relativeLuminance(foreground)
    val l2 = relativeLuminance(background)
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}