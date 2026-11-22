package com.example.upitracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.upitracker.data.local.dao.TransactionDao
import com.example.upitracker.data.local.database.AppDatabase
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
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
class TransactionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.transactionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTransaction() = runBlocking {
        val transaction = Transaction(
            amountPaise = 25000L,
            type = TransactionType.DEBIT,
            merchant = "Grocery Store",
            timestamp = 1700000000000L,
            smsHash = "unique_hash_1"
        )

        val rowId = dao.insert(transaction)
        assertTrue("Row ID should be greater than 0", rowId > 0)

        val all = dao.getAllTransactions().first()
        assertEquals(1, all.size)
        assertEquals("Grocery Store", all[0].merchant)
        assertEquals(25000L, all[0].amountPaise)
    }

    @Test
    fun duplicateSmsHashIsIgnored() = runBlocking {
        val t1 = Transaction(
            amountPaise = 10000L,
            type = TransactionType.DEBIT,
            merchant = "Shop A",
            timestamp = 1700000000000L,
            smsHash = "duplicate_hash"
        )
        val t2 = Transaction(
            amountPaise = 20000L,
            type = TransactionType.DEBIT,
            merchant = "Shop B",
            timestamp = 1700000010000L,
            smsHash = "duplicate_hash" // same hash
        )

        val id1 = dao.insert(t1)
        val id2 = dao.insert(t2)

        assertTrue(id1 > 0)
        assertEquals(-1L, id2) // OnConflictStrategy.IGNORE returns -1 on duplicate

        val count = dao.getCount().first()
        assertEquals(1, count)
    }

    @Test
    fun getTotalByTypeBetweenReturnsCorrectSum() = runBlocking {
        val start = 1000L
        val end = 5000L

        // Inside window - DEBIT
        dao.insert(
            Transaction(
                amountPaise = 15000L,
                type = TransactionType.DEBIT,
                merchant = "Coffee",
                timestamp = 2000L,
                smsHash = "hash_1"
            )
        )
        dao.insert(
            Transaction(
                amountPaise = 35000L,
                type = TransactionType.DEBIT,
                merchant = "Dinner",
                timestamp = 3000L,
                smsHash = "hash_2"
            )
        )

        // Inside window - CREDIT (should not be included in DEBIT sum)
        dao.insert(
            Transaction(
                amountPaise = 50000L,
                type = TransactionType.CREDIT,
                merchant = "Salary/Refund",
                timestamp = 2500L,
                smsHash = "hash_3"
            )
        )

        // Outside window - DEBIT (should not be included)
        dao.insert(
            Transaction(
                amountPaise = 90000L,
                type = TransactionType.DEBIT,
                merchant = "Old bill",
                timestamp = 500L,
                smsHash = "hash_4"
            )
        )

        val totalDebit = dao.getTotalByTypeBetween(TransactionType.DEBIT, start, end).first()
        assertEquals(50000L, totalDebit) // 15000 + 35000 = 50000
    }
}
