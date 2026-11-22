package com.example.upitracker.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// ─── Theme Colors ────────────────────────────────────────────────────────────

// Light Theme Palette
val primaryLight = Color(0xFF0072B2)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFD6EBFA)
val onPrimaryContainerLight = Color(0xFF00344F)
val backgroundLight = Color(0xFFFAFCFE)
val onBackgroundLight = Color(0xFF101418)
val surfaceLight = Color(0xFFFAFCFE)
val onSurfaceLight = Color(0xFF101418)
val errorLight = Color(0xFFB3261E)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFF9DEDC)
val onErrorContainerLight = Color(0xFF410E0B)

val secondaryLight = Color(0xFF486271)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFD3E4F5)
val onSecondaryContainerLight = Color(0xFF0C1D29)

val tertiaryLight = Color(0xFF6B5778)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFF2DAFF)
val onTertiaryContainerLight = Color(0xFF251431)

val surfaceVariantLight = Color(0xFFEEF2F6)
val onSurfaceVariantLight = Color(0xFF42474E)
val outlineLight = Color(0xFF72777F)
val outlineVariantLight = Color(0xFFC2C7CF)

// Dark Theme Palette
val primaryDark = Color(0xFF5DADE2)
val onPrimaryDark = Color(0xFF00334D)
val primaryContainerDark = Color(0xFF004A73)
val onPrimaryContainerDark = Color(0xFFD6EBFA)
val backgroundDark = Color(0xFF101418)
val onBackgroundDark = Color(0xFFE1E2E8)
val surfaceDark = Color(0xFF101418)
val onSurfaceDark = Color(0xFFE1E2E8)
val errorDark = Color(0xFFF2B8B5)
val onErrorDark = Color(0xFF601410)
val errorContainerDark = Color(0xFF8C1D18)
val onErrorContainerDark = Color(0xFFF9DEDC)

val secondaryDark = Color(0xFFB7C8D9)
val onSecondaryDark = Color(0xFF21323F)
val secondaryContainerDark = Color(0xFF384956)
val onSecondaryContainerDark = Color(0xFFD3E4F5)

val tertiaryDark = Color(0xFFD6BEE4)
val onTertiaryDark = Color(0xFF3B2A47)
val tertiaryContainerDark = Color(0xFF52405F)
val onTertiaryContainerDark = Color(0xFFF2DAFF)

val surfaceVariantDark = Color(0xFF1C2128)
val onSurfaceVariantDark = Color(0xFFC2C7CF)
val outlineDark = Color(0xFF8C9199)
val outlineVariantDark = Color(0xFF42474E)

// Credit amount text (always paired with a Debit/Credit or Spent/Received label)
val CreditColorLight = Color(0xFF137333)
val CreditColorDark = Color(0xFF48C9B0)
val DebitColor = Color(0xFFC62828)

val NearLimitLight = Color(0xFFE69F00)
val NearLimitDark = Color(0xFFF5B041)

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