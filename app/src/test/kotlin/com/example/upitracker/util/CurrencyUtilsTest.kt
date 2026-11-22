package com.example.upitracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun formatRupees_zero() {
        assertEquals("₹0.00", CurrencyUtils.formatRupees(0L))
    }

    @Test
    fun formatRupees_subRupee() {
        assertEquals("₹0.99", CurrencyUtils.formatRupees(99L))
    }

    @Test
    fun formatRupees_hundreds() {
        assertEquals("₹125.50", CurrencyUtils.formatRupees(12550L))
    }

    @Test
    fun formatRupees_lakh() {
        assertEquals("₹1,25,000.00", CurrencyUtils.formatRupees(12500000L))
    }

    @Test
    fun formatRupees_multiLakh() {
        assertEquals("₹12,34,567.89", CurrencyUtils.formatRupees(123456789L))
    }

    @Test
    fun formatRupees_negativeValue() {
        assertEquals("-₹50.00", CurrencyUtils.formatRupees(-5000L))
    }
}
