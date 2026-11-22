package com.example.upitracker.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import com.example.upitracker.data.category.Category

class BackNavigationTest {

    @Test
    fun backTarget_fromHome_returnsNull() {
        assertNull(backTarget(Screen.Home))
    }

    @Test
    fun backTarget_fromInsights_returnsHome() {
        assertEquals(Screen.Home, backTarget(Screen.Insights))
    }

    @Test
    fun backTarget_fromTransactions_returnsHome() {
        assertEquals(Screen.Home, backTarget(Screen.Transactions()))
        assertEquals(Screen.Home, backTarget(Screen.Transactions(category = Category.FOOD_AND_DINING, debitOnly = true)))
    }

    @Test
    fun backTarget_fromBudgets_returnsHome() {
        assertEquals(Screen.Home, backTarget(Screen.Budgets))
    }

    @Test
    fun backTarget_fromSettings_returnsHome() {
        assertEquals(Screen.Home, backTarget(Screen.Settings))
    }

    @Test
    fun backTarget_fromUnrecognisedSms_returnsHome() {
        assertEquals(Screen.Home, backTarget(Screen.UnrecognisedSms))
    }

    @Test
    fun screenSaver_restoresAllScreensAccurately() {
        val screens = listOf(
            Screen.Home,
            Screen.Insights,
            Screen.Transactions(),
            Screen.Transactions(category = Category.SHOPPING, debitOnly = true),
            Screen.Budgets,
            Screen.Settings,
            Screen.UnrecognisedSms
        )

        for (s in screens) {
            val saved = with(Screen.Saver) {
                SaverScopeLambda.save(s)
            }
            assertNotNull("Saved value should not be null", saved)
            val restored = Screen.Saver.restore(saved!!)
            assertEquals("Failed for screen $s", s, restored)
        }
    }

    private object SaverScopeLambda : androidx.compose.runtime.saveable.SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }
}
