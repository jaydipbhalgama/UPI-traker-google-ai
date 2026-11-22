package com.example.upitracker.domain.filter

import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterLogicTest {

    private fun sampleTx(
        id: Long,
        amountPaise: Long,
        type: TransactionType = TransactionType.DEBIT,
        merchant: String = "Swiggy",
        category: String = "Food & Dining",
        timestamp: Long = 1000L,
        note: String? = null
    ) = Transaction(
        id = id,
        amountPaise = amountPaise,
        type = type,
        merchant = merchant,
        category = category,
        timestamp = timestamp,
        note = note,
        smsHash = "hash$id"
    )

    @Test
    fun matchesQuery_verifiesAllMatchingRules() {
        val tx = sampleTx(1, amountPaise = 25000L, merchant = "Swiggy", note = "Dinner with team", category = "Food & Dining")

        // 1. Empty query matches everything
        assertTrue(FilterLogic.matchesQuery(tx, ""))
        assertTrue(FilterLogic.matchesQuery(tx, "   "))

        // 2. Case-insensitive and note match
        assertTrue(FilterLogic.matchesQuery(tx, "dinner"))
        assertTrue(FilterLogic.matchesQuery(tx, "DINNER"))

        // 3. Category label match
        assertTrue(FilterLogic.matchesQuery(tx, "food"))
        assertTrue(FilterLogic.matchesQuery(tx, "dining"))

        // 4. AND of terms
        assertTrue(FilterLogic.matchesQuery(tx, "swiggy dinner"))
        assertFalse(FilterLogic.matchesQuery(tx, "swiggy lunch"))

        // 5. Amount term "250" matches amount 25000
        assertTrue(FilterLogic.matchesQuery(tx, "250"))
        assertTrue(FilterLogic.matchesQuery(tx, "swiggy 250"))

        // 6. Term "250" also matches merchant "Shop250"
        val shopTx = sampleTx(2, amountPaise = 10000L, merchant = "Shop250")
        assertTrue(FilterLogic.matchesQuery(shopTx, "250"))

        // 7. "250.5" matches 25050 paise
        val decimalTx = sampleTx(3, amountPaise = 25050L, merchant = "Amazon")
        assertTrue(FilterLogic.matchesQuery(decimalTx, "250.5"))
        assertFalse(FilterLogic.matchesQuery(tx, "250.5"))

        // 8. Extra spaces ignored
        assertTrue(FilterLogic.matchesQuery(tx, "   swiggy     250   "))

        // 9. No match returns false
        assertFalse(FilterLogic.matchesQuery(tx, "uber"))
    }

    @Test
    fun applyFilters_verifiesTypeAndCategoryFilters() {
        val t1 = sampleTx(1, 10000L, type = TransactionType.DEBIT, category = "Food & Dining")
        val t2 = sampleTx(2, 20000L, type = TransactionType.CREDIT, category = "Income")
        val t3 = sampleTx(3, 30000L, type = TransactionType.DEBIT, category = "Groceries")
        val list = listOf(t1, t2, t3)

        // Type filter: DEBIT
        val debitOnly = FilterLogic.applyFilters(list, FilterState(typeFilter = TransactionTypeFilter.DEBIT))
        assertEquals(listOf(t3, t1), debitOnly) // Default NEWEST sort (id 3 then 1)

        // Type filter: CREDIT
        val creditOnly = FilterLogic.applyFilters(list, FilterState(typeFilter = TransactionTypeFilter.CREDIT))
        assertEquals(listOf(t2), creditOnly)

        // Category filter: Groceries
        val groceriesOnly = FilterLogic.applyFilters(list, FilterState(category = Category.GROCERIES))
        assertEquals(listOf(t3), groceriesOnly)
    }

    @Test
    fun applyFilters_verifiesMinMaxInclusiveBoundaries() {
        val t1 = sampleTx(1, 14999L)
        val t2 = sampleTx(2, 15000L)
        val t3 = sampleTx(3, 15001L)
        val list = listOf(t1, t2, t3)

        // Min 15000 includes 15000 and excludes 14999
        val minFiltered = FilterLogic.applyFilters(list, FilterState(minPaise = 15000L))
        assertTrue(minFiltered.contains(t2))
        assertTrue(minFiltered.contains(t3))
        assertFalse(minFiltered.contains(t1))

        // Max 15000 includes 15000 and excludes 15001
        val maxFiltered = FilterLogic.applyFilters(list, FilterState(maxPaise = 15000L))
        assertTrue(maxFiltered.contains(t1))
        assertTrue(maxFiltered.contains(t2))
        assertFalse(maxFiltered.contains(t3))
    }

    @Test
    fun applyFilters_verifiesSortingOrders() {
        val t1 = sampleTx(1, amountPaise = 10000L, timestamp = 100L)
        val t2 = sampleTx(2, amountPaise = 30000L, timestamp = 200L)
        val t3 = sampleTx(3, amountPaise = 20000L, timestamp = 300L)
        val t4 = sampleTx(4, amountPaise = 20000L, timestamp = 400L) // tie with t3
        val list = listOf(t1, t2, t3, t4)

        // NEWEST: timestamp desc, id desc
        val newest = FilterLogic.applyFilters(list, FilterState(sort = SortOrder.NEWEST))
        assertEquals(listOf(4L, 3L, 2L, 1L), newest.map { it.id })

        // OLDEST: timestamp asc, id asc
        val oldest = FilterLogic.applyFilters(list, FilterState(sort = SortOrder.OLDEST))
        assertEquals(listOf(1L, 2L, 3L, 4L), oldest.map { it.id })

        // HIGHEST: amount desc, timestamp desc
        val highest = FilterLogic.applyFilters(list, FilterState(sort = SortOrder.HIGHEST))
        assertEquals(listOf(2L, 4L, 3L, 1L), highest.map { it.id })

        // LOWEST: amount asc, timestamp desc
        val lowest = FilterLogic.applyFilters(list, FilterState(sort = SortOrder.LOWEST))
        assertEquals(listOf(1L, 4L, 3L, 2L), lowest.map { it.id })
    }

    @Test
    fun summarize_computesCorrectSums() {
        val t1 = sampleTx(1, 10000L, type = TransactionType.DEBIT)
        val t2 = sampleTx(2, 5000L, type = TransactionType.DEBIT)
        val t3 = sampleTx(3, 40000L, type = TransactionType.CREDIT)

        val summary = FilterLogic.summarize(listOf(t1, t2, t3))
        assertEquals(3, summary.count)
        assertEquals(15000L, summary.spentPaise)
        assertEquals(40000L, summary.receivedPaise)
    }
}
