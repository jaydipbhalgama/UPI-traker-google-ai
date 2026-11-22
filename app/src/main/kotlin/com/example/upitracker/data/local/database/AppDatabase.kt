package com.example.upitracker.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.upitracker.data.local.Converters
import com.example.upitracker.data.local.dao.BudgetDao
import com.example.upitracker.data.local.dao.MerchantRuleDao
import com.example.upitracker.data.local.dao.TransactionDao
import com.example.upitracker.data.local.entity.Budget
import com.example.upitracker.data.local.entity.MerchantRule
import com.example.upitracker.data.local.entity.Transaction

@Database(
    entities = [Transaction::class, MerchantRule::class, Budget::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun merchantRuleDao(): MerchantRuleDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "upi_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
