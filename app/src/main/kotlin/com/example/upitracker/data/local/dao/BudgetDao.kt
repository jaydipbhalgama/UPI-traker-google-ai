package com.example.upitracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.upitracker.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: Budget)

    @Query("DELETE FROM budgets WHERE budgetKey = :key")
    suspend fun deleteByKey(key: String)

    @Query("SELECT * FROM budgets")
    fun getAllFlow(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<Budget>
}
