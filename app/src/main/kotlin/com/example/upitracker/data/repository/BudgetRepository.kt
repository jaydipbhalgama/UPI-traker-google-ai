package com.example.upitracker.data.repository

import com.example.upitracker.data.local.dao.BudgetDao
import com.example.upitracker.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllFlow(): Flow<List<Budget>>
    suspend fun getAll(): List<Budget>
    suspend fun upsert(budget: Budget)
    suspend fun deleteByKey(key: String)
}

class DefaultBudgetRepository(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getAllFlow(): Flow<List<Budget>> =
        budgetDao.getAllFlow()

    override suspend fun getAll(): List<Budget> =
        budgetDao.getAll()

    override suspend fun upsert(budget: Budget) =
        budgetDao.upsert(budget)

    override suspend fun deleteByKey(key: String) =
        budgetDao.deleteByKey(key)
}
