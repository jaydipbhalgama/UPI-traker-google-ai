package com.example.upitracker.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migration from version 1 to version 2.
 *
 * Adds the "merchant_rules" table.
 * Column types match exactly what Room generates in the schema JSON.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `merchant_rules` (
                `merchantKey` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`merchantKey`)
            )
            """.trimIndent()
        )
    }
}

/**
 * Room database migration from version 2 to version 3.
 *
 * Adds the "budgets" table.
 * Column types match exactly what Room generates in the schema JSON.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budgets` (
                `budgetKey` TEXT NOT NULL,
                `limitPaise` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`budgetKey`)
            )
            """.trimIndent()
        )
    }
}

