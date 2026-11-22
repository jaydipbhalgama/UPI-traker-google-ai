package com.example.upitracker.di

import android.content.Context
import com.example.upitracker.data.local.database.AppDatabase
import com.example.upitracker.data.repository.BudgetRepository
import com.example.upitracker.data.repository.DefaultBudgetRepository
import com.example.upitracker.data.repository.DefaultTransactionRepository
import com.example.upitracker.data.repository.TransactionRepository
import com.example.upitracker.data.sms.SmsSyncManager
import com.example.upitracker.domain.MonthSelection
import com.example.upitracker.service.BudgetAlertChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual Dependency Injection container for the application.
 */
interface AppContainer {
    val transactionRepository: TransactionRepository
    val budgetRepository: BudgetRepository
    val budgetAlertChecker: BudgetAlertChecker
    val smsSyncManager: SmsSyncManager
    val monthSelection: MonthSelection
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val transactionRepository: TransactionRepository by lazy {
        DefaultTransactionRepository(
            transactionDao = database.transactionDao(),
            merchantRuleDao = database.merchantRuleDao()
        )
    }

    override val budgetRepository: BudgetRepository by lazy {
        DefaultBudgetRepository(
            budgetDao = database.budgetDao()
        )
    }

    override val budgetAlertChecker: BudgetAlertChecker by lazy {
        BudgetAlertChecker(
            context = context.applicationContext,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository
        )
    }

    override val smsSyncManager: SmsSyncManager by lazy {
        SmsSyncManager(context, transactionRepository)
    }

    override val monthSelection: MonthSelection by lazy {
        MonthSelection(
            oldestTimestampFlow = transactionRepository.getOldestTimestamp(),
            scope = applicationScope
        )
    }
}
