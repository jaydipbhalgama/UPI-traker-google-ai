package com.example.upitracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.database.AppDatabase
import com.example.upitracker.data.local.entity.MerchantRule
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.data.repository.DefaultTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DefaultTransactionRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultTransactionRepository(
            transactionDao = database.transactionDao(),
            merchantRuleDao = database.merchantRuleDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun updateCategoryForMerchantKey_updatesOnlyMatchingRowsCaseAndSpaceHandled() = runBlocking {
        // Insert transactions with various merchant casings/spaces
        val t1 = Transaction(amountPaise = 1000L, type = TransactionType.DEBIT, merchant = "Swiggy", category = "Uncategorized", timestamp = 1L, smsHash = "h1")
        val t2 = Transaction(amountPaise = 2000L, type = TransactionType.DEBIT, merchant = " SWIGGY ", category = "Uncategorized", timestamp = 2L, smsHash = "h2")
        val t3 = Transaction(amountPaise = 3000L, type = TransactionType.DEBIT, merchant = "swiggy", category = "Uncategorized", timestamp = 3L, smsHash = "h3")
        val t4 = Transaction(amountPaise = 4000L, type = TransactionType.DEBIT, merchant = "Zomato", category = "Uncategorized", timestamp = 4L, smsHash = "h4")

        database.transactionDao().insertAll(listOf(t1, t2, t3, t4))

        // Check count by merchant key (matches LOWER(TRIM(merchant)))
        val count = database.transactionDao().countByMerchantKey("swiggy")
        assertEquals(3, count)

        // Update category for merchant key
        database.transactionDao().updateCategoryForMerchantKey("swiggy", Category.FOOD_AND_DINING.label)

        val all = database.transactionDao().getAllTransactions().first()
        val swiggyTxs = all.filter { it.merchant.trim().lowercase() == "swiggy" }
        assertEquals(3, swiggyTxs.size)
        assertTrue(swiggyTxs.all { it.category == Category.FOOD_AND_DINING.label })

        val zomato = all.first { it.merchant == "Zomato" }
        assertEquals("Uncategorized", zomato.category)
    }

    @Test
    fun saveTransactionEdit_createsRuleAndUpdatesAllWhenRequested() = runBlocking {
        val t1 = Transaction(amountPaise = 1000L, type = TransactionType.DEBIT, merchant = "Starbucks", category = "Uncategorized", timestamp = 1L, smsHash = "h1")
        val t2 = Transaction(amountPaise = 2000L, type = TransactionType.DEBIT, merchant = "Starbucks", category = "Uncategorized", timestamp = 2L, smsHash = "h2")
        val id1 = database.transactionDao().insert(t1)
        val id2 = database.transactionDao().insert(t2)

        // Save edit with applyToAll = true
        repository.saveTransactionEdit(
            id = id1,
            category = Category.FOOD_AND_DINING.label,
            note = "Morning coffee",
            merchantKey = "starbucks",
            applyToAll = true
        )

        // Rule should be upserted in merchant_rules table
        val rules = repository.getAllMerchantRules()
        assertEquals(1, rules.size)
        assertEquals("starbucks", rules[0].merchantKey)
        assertEquals(Category.FOOD_AND_DINING.label, rules[0].category)

        // Both transactions should be updated
        val all = database.transactionDao().getAllTransactions().first()
        val updated1 = all.first { it.id == id1 }
        val updated2 = all.first { it.id == id2 }
        assertEquals(Category.FOOD_AND_DINING.label, updated1.category)
        assertEquals("Morning coffee", updated1.note)
        assertEquals(Category.FOOD_AND_DINING.label, updated2.category)
    }

    @Test
    fun saveTransactionEdit_forUnknownMerchant_createsNoRule() = runBlocking {
        val t = Transaction(amountPaise = 500L, type = TransactionType.DEBIT, merchant = "Unknown", category = "Uncategorized", timestamp = 1L, smsHash = "h1")
        val id = database.transactionDao().insert(t)

        repository.saveTransactionEdit(
            id = id,
            category = Category.OTHER.label,
            note = "Random payment",
            merchantKey = "unknown",
            applyToAll = true // Even if true, Unknown must NEVER create a rule
        )

        // Verify no rule was created
        val rules = repository.getAllMerchantRules()
        assertTrue("No rule should be created for Unknown", rules.isEmpty())

        // But the single transaction itself is updated
        val updated = database.transactionDao().getAllTransactions().first().first()
        assertEquals(Category.OTHER.label, updated.category)
        assertEquals("Random payment", updated.note)
    }
}
