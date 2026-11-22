package com.example.upitracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.upitracker.data.local.dao.BudgetDao
import com.example.upitracker.data.local.database.AppDatabase
import com.example.upitracker.data.local.entity.Budget
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
class BudgetDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var budgetDao: BudgetDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        budgetDao = database.budgetDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun upsert_replacesExistingKeyAndFlowEmitsUpdates() = runBlocking {
        // 1. Initial upsert
        val b1 = Budget(budgetKey = Budget.TOTAL_BUDGET_KEY, limitPaise = 5000000L, updatedAt = 1000L)
        budgetDao.upsert(b1)

        val list1 = budgetDao.getAllFlow().first()
        assertEquals(1, list1.size)
        assertEquals(Budget.TOTAL_BUDGET_KEY, list1[0].budgetKey)
        assertEquals(5000000L, list1[0].limitPaise)

        // 2. Upsert same key replaces it
        val b1Updated = Budget(budgetKey = Budget.TOTAL_BUDGET_KEY, limitPaise = 7500000L, updatedAt = 2000L)
        budgetDao.upsert(b1Updated)

        val list2 = budgetDao.getAllFlow().first()
        assertEquals(1, list2.size)
        assertEquals(7500000L, list2[0].limitPaise)
        assertEquals(2000L, list2[0].updatedAt)

        // 3. Insert another category budget
        val b2 = Budget(budgetKey = "Food & Dining", limitPaise = 1500000L, updatedAt = 3000L)
        budgetDao.upsert(b2)

        val list3 = budgetDao.getAll()
        assertEquals(2, list3.size)

        // 4. Delete by key works
        budgetDao.deleteByKey("Food & Dining")
        val list4 = budgetDao.getAllFlow().first()
        assertEquals(1, list4.size)
        assertEquals(Budget.TOTAL_BUDGET_KEY, list4[0].budgetKey)

        budgetDao.deleteByKey(Budget.TOTAL_BUDGET_KEY)
        assertTrue(budgetDao.getAll().isEmpty())
    }
}
