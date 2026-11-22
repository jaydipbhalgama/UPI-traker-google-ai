package com.example.upitracker.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating WCAG 2.1 AA contrast ratio (>= 4.5:1) for text/background pairs
 * across both Light and Dark themes.
 */
class ColorContrastTest {

    private val minContrast = 4.5

    @Test
    fun lightTheme_onPrimaryOnPrimary_satisfiesContrast() {
        val ratio = contrastRatio(onPrimaryLight, primaryLight)
        assertTrue("Expected onPrimary on primary >= $minContrast, but was $ratio", ratio >= minContrast)
    }

    @Test
    fun lightTheme_onPrimaryContainerOnPrimaryContainer_satisfiesContrast() {
        val ratio = contrastRatio(onPrimaryContainerLight, primaryContainerLight)
        assertTrue("Expected onPrimaryContainer on primaryContainer >= $minContrast, but was $ratio", ratio >= minContrast)
    }

    @Test
    fun lightTheme_onBackgroundOnBackground_satisfiesContrast() {
        val ratio = contrastRatio(onBackgroundLight, backgroundLight)
        assertTrue("Expected onBackground on background >= $minContrast, but was $ratio", ratio >= minContrast)
    }

    @Test
    fun lightTheme_onSurfaceOnSurface_satisfiesContrast() {
        val ratio = contrastRatio(onSurfaceLight, surfaceLight)
        assertTrue("Expected onSurface on surface >= $minContrast, but was $ratio", ratio >= minContrast)
    }

    @Test
    fun lightTheme_creditAmountColorOnBackground_satisfiesContrast() {
        val ratio = contrastRatio(CreditColorLight, backgroundLight)
        assertTrue("Expected CreditColorLight on backgroundLight >= $minContrast, but was $ratio", ratio >= minContrast)
    }

    @Test
    fun darkTheme_onPrimaryOnPrimary_satisfiesContrast() {
        val ratio = contrastRatio(onPrimaryDark, primaryDark)
        assertTrue("Expected onPrimary on primary >= $minContrast, but was $ratio", ratio >= minContrast)
    }

    @Test
    fun darkTheme_onPrimaryContainerOnPrimaryContainer_satisfiesContrast() {
        val ratio = contrastRatio(onPrimaryContainerDark, primaryContainerDark)
        assertTrue("Expected onPrimaryContainer on primaryContainer >= $minContrast, but was $ratio", ratio >= minContrast)
    }

    @Test
    fun darkTheme_onBackgroundOnBackground_satisfiesContrast() {
        val ratio = contrastRatio(onBackgroundDark, backgroundDark)
        assertTrue("Expected onBackground on background >= $minContrast, but was $ratio", ratio >= minContrast)
    }

    @Test
    fun darkTheme_onSurfaceOnSurface_satisfiesContrast() {
        val ratio = contrastRatio(onSurfaceDark, surfaceDark)
        assertTrue("Expected onSurface on surface >= $minContrast, but was $ratio", ratio >= minContrast)
    }

    @Test
    fun darkTheme_creditAmountColorOnBackground_satisfiesContrast() {
        val ratio = contrastRatio(CreditColorDark, backgroundDark)
        assertTrue("Expected CreditColorDark on backgroundDark >= $minContrast, but was $ratio", ratio >= minContrast)
    }
}
