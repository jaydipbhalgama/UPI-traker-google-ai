package com.example.upitracker.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.upitracker.data.local.database.AppDatabase
import com.example.upitracker.data.local.database.MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_preservesTransactionsAndCreatesMerchantRulesTable() {
        // Step 1: Create Database in version 1
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert 2 transactions into version 1 schema
            execSQL(
                """
                INSERT INTO transactions (amountPaise, type, merchant, category, timestamp, bankName, balanceAfterPaise, smsHash, note)
                VALUES (15000, 'DEBIT', 'Swiggy', 'Uncategorized', 1700000000000, 'HDFC', 4500000, 'hash1', 'Test note 1')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO transactions (amountPaise, type, merchant, category, timestamp, bankName, balanceAfterPaise, smsHash, note)
                VALUES (50000, 'CREDIT', 'Employer', 'Uncategorized', 1700000010000, 'SBI', 5000000, 'hash2', NULL)
                """.trimIndent()
            )
            close()
        }

        // Step 2: Run migration to version 2 and validate schema
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Step 3: Verify existing transactions survived unchanged
        val cursor = db.query("SELECT * FROM transactions ORDER BY id ASC")
        assertEquals(2, cursor.count)

        assertTrue(cursor.moveToFirst())
        val amount1 = cursor.getLong(cursor.getColumnIndexOrThrow("amountPaise"))
        val merchant1 = cursor.getString(cursor.getColumnIndexOrThrow("merchant"))
        val note1 = cursor.getString(cursor.getColumnIndexOrThrow("note"))
        assertEquals(15000L, amount1)
        assertEquals("Swiggy", merchant1)
        assertEquals("Test note 1", note1)

        assertTrue(cursor.moveToNext())
        val amount2 = cursor.getLong(cursor.getColumnIndexOrThrow("amountPaise"))
        val merchant2 = cursor.getString(cursor.getColumnIndexOrThrow("merchant"))
        assertEquals(50000L, amount2)
        assertEquals("Employer", merchant2)
        cursor.close()

        // Step 4: Verify merchant_rules table exists and works
        db.execSQL(
            """
            INSERT INTO merchant_rules (merchantKey, category, updatedAt)
            VALUES ('swiggy', 'Food & Dining', 1700000020000)
            """.trimIndent()
        )
        val ruleCursor = db.query("SELECT * FROM merchant_rules WHERE merchantKey = 'swiggy'")
        assertEquals(1, ruleCursor.count)
        assertTrue(ruleCursor.moveToFirst())
        val cat = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow("category"))
        assertEquals("Food & Dining", cat)
        ruleCursor.close()

        db.close()
    }

    @Test
    fun migrate2To3_preservesDataAndCreatesBudgetsTable() {
        // Step 1: Create Database in version 2
        var db = helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                """
                INSERT INTO transactions (amountPaise, type, merchant, category, timestamp, bankName, balanceAfterPaise, smsHash, note)
                VALUES (15000, 'DEBIT', 'Swiggy', 'Food & Dining', 1700000000000, 'HDFC', 4500000, 'hash1', 'Test note 1')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO transactions (amountPaise, type, merchant, category, timestamp, bankName, balanceAfterPaise, smsHash, note)
                VALUES (50000, 'CREDIT', 'Employer', 'Income', 1700000010000, 'SBI', 5000000, 'hash2', NULL)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO merchant_rules (merchantKey, category, updatedAt)
                VALUES ('swiggy', 'Food & Dining', 1700000020000)
                """.trimIndent()
            )
            close()
        }

        // Step 2: Migrate from 2 to 3
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, com.example.upitracker.data.local.database.MIGRATION_2_3)

        // Step 3: Verify existing transactions and merchant rules survived
        val txCursor = db.query("SELECT * FROM transactions ORDER BY id ASC")
        assertEquals(2, txCursor.count)
        assertTrue(txCursor.moveToFirst())
        assertEquals(15000L, txCursor.getLong(txCursor.getColumnIndexOrThrow("amountPaise")))
        assertTrue(txCursor.moveToNext())
        assertEquals(50000L, txCursor.getLong(txCursor.getColumnIndexOrThrow("amountPaise")))
        txCursor.close()

        val ruleCursor = db.query("SELECT * FROM merchant_rules WHERE merchantKey = 'swiggy'")
        assertEquals(1, ruleCursor.count)
        ruleCursor.close()

        // Step 4: Verify budgets table exists and accepts inserts
        db.execSQL(
            """
            INSERT INTO budgets (budgetKey, limitPaise, updatedAt)
            VALUES ('__TOTAL__', 5000000, 1700000030000)
            """.trimIndent()
        )
        val budgetCursor = db.query("SELECT * FROM budgets WHERE budgetKey = '__TOTAL__'")
        assertEquals(1, budgetCursor.count)
        assertTrue(budgetCursor.moveToFirst())
        assertEquals(5000000L, budgetCursor.getLong(budgetCursor.getColumnIndexOrThrow("limitPaise")))
        budgetCursor.close()

        db.close()
    }

    @Test
    fun migrate1To3_bothMigrationsChained_preservesDataAndCreatesBudgetsTable() {
        // Step 1: Create Database in version 1
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO transactions (amountPaise, type, merchant, category, timestamp, bankName, balanceAfterPaise, smsHash, note)
                VALUES (25000, 'DEBIT', 'Amazon', 'Shopping', 1700000000000, 'ICICI', 3000000, 'hashA', 'Book')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO transactions (amountPaise, type, merchant, category, timestamp, bankName, balanceAfterPaise, smsHash, note)
                VALUES (12000, 'CREDIT', 'Friend', 'Transfers', 1700000010000, 'ICICI', 3120000, 'hashB', NULL)
                """.trimIndent()
            )
            close()
        }

        // Step 2: Migrate 1 to 3 with both migrations chained
        db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            com.example.upitracker.data.local.database.MIGRATION_1_2,
            com.example.upitracker.data.local.database.MIGRATION_2_3
        )

        // Step 3: Verify existing transactions survived
        val txCursor = db.query("SELECT * FROM transactions ORDER BY id ASC")
        assertEquals(2, txCursor.count)
        assertTrue(txCursor.moveToFirst())
        assertEquals("Amazon", txCursor.getString(txCursor.getColumnIndexOrThrow("merchant")))
        assertEquals(25000L, txCursor.getLong(txCursor.getColumnIndexOrThrow("amountPaise")))
        txCursor.close()

        // Step 4: Verify merchant_rules table exists and accepts inserts
        db.execSQL(
            """
            INSERT INTO merchant_rules (merchantKey, category, updatedAt)
            VALUES ('amazon', 'Shopping', 1700000020000)
            """.trimIndent()
        )
        val ruleCursor = db.query("SELECT * FROM merchant_rules WHERE merchantKey = 'amazon'")
        assertEquals(1, ruleCursor.count)
        ruleCursor.close()

        // Step 5: Verify budgets table exists and accepts inserts
        db.execSQL(
            """
            INSERT INTO budgets (budgetKey, limitPaise, updatedAt)
            VALUES ('Shopping', 1000000, 1700000040000)
            """.trimIndent()
        )
        val budgetCursor = db.query("SELECT * FROM budgets WHERE budgetKey = 'Shopping'")
        assertEquals(1, budgetCursor.count)
        assertTrue(budgetCursor.moveToFirst())
        assertEquals(1000000L, budgetCursor.getLong(budgetCursor.getColumnIndexOrThrow("limitPaise")))
        budgetCursor.close()

        db.close()
    }
}
