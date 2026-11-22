package com.example.upitracker.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.category.AutoCategorizer
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.data.repository.TransactionRepository
import com.example.upitracker.data.sms.SmsSyncManager
import com.example.upitracker.domain.MonthSelection
import com.example.upitracker.domain.summary.MonthlySummary
import com.example.upitracker.domain.summary.MonthlySummaryCalculator
import com.example.upitracker.util.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID

import com.example.upitracker.data.local.entity.Budget
import com.example.upitracker.data.repository.BudgetRepository
import com.example.upitracker.domain.budget.BudgetLevel
import com.example.upitracker.domain.budget.BudgetLogic
import com.example.upitracker.domain.budget.BudgetStatus
import com.example.upitracker.service.BudgetAlertChecker
import java.time.LocalDate
import java.time.ZoneId

data class BalanceInfo(
    val balancePaise: Long,
    val bankName: String?,
    val asOfTimestamp: Long
)

data class DashboardBudgetSummary(
    val hasAnyBudgets: Boolean = false,
    val overallStatus: BudgetStatus? = null,
    val categoryAttentionCount: Int = 0,
    val totalCategoryBudgetsCount: Int = 0
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val summary: DashboardSummary,
        val budgetInfo: BudgetInfo?,
        val selectedPeriod: SpendPeriod,
        val periodSpent: Long,
        val periodReceived: Long,
        val periodCount: Int,
        val selectedMonth: YearMonth,
        val canGoNext: Boolean,
        val canGoPrevious: Boolean,
        val allMonthTransactions: List<Transaction>,
        val budgetSummary: DashboardBudgetSummary = DashboardBudgetSummary(),
        val currentMonthSummary: MonthlySummary = MonthlySummary(month = YearMonth.now())
    ) : DashboardUiState
}

class DashboardViewModel(
    private val repository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val alertChecker: BudgetAlertChecker,
    private val smsSyncManager: SmsSyncManager,
    private val monthSelection: MonthSelection,
    private val appContext: Context
) : ViewModel() {

    val selectedMonth: StateFlow<YearMonth> = monthSelection.selectedMonth

    private val _selectedPeriod = MutableStateFlow(SpendPeriod.TODAY)
    val selectedPeriod: StateFlow<SpendPeriod> = _selectedPeriod.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    /** Currently selected transaction to show in the edit bottom sheet; null = sheet closed */
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)
    val editingTransaction: StateFlow<Transaction?> = _editingTransaction.asStateFlow()

    /** Count of matching transactions for the merchant key in the edit sheet */
    private val _merchantCount = MutableStateFlow(0)
    val merchantCount: StateFlow<Int> = _merchantCount.asStateFlow()

    private val currentMonthSummaryFlow = run {
        val currentMonth = YearMonth.now()
        val currentStart = DateTimeUtils.monthStartMillis(currentMonth)
        val currentEnd = DateTimeUtils.monthEndExclusiveMillis(currentMonth)
        repository.getTransactionsBetween(currentStart, currentEnd)
            .map { txs -> MonthlySummaryCalculator.calculate(txs, currentMonth) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = monthSelection.selectedMonth
        .flatMapLatest { ym ->
            val start = DateTimeUtils.monthStartMillis(ym)
            val end = DateTimeUtils.monthEndExclusiveMillis(ym)

            combine(
                repository.getTransactionsBetween(start, end),
                monthSelection.canGoNext,
                monthSelection.canGoPrevious,
                budgetRepository.getAllFlow(),
                _selectedPeriod
            ) { monthTxs, canNext, canPrev, budgets, period ->
                val summary = DashboardCalculator.calculate(monthTxs, ym)

                val today = LocalDate.now()
                val zone = ZoneId.systemDefault()

                var totalSpent = 0L
                val catSpent = mutableMapOf<String, Long>()
                for (tx in monthTxs) {
                    if (tx.type == TransactionType.DEBIT) {
                        totalSpent += tx.amountPaise
                        catSpent[tx.category] = (catSpent[tx.category] ?: 0L) + tx.amountPaise
                    }
                }

                // Monthly overall budget info
                val overallBudget = budgets.firstOrNull { it.budgetKey == Budget.TOTAL_BUDGET_KEY }
                val budgetInfo = if (overallBudget != null) {
                    computeBudgetInfo(
                        limitPaise = overallBudget.limitPaise,
                        spentPaise = totalSpent,
                        month = ym,
                        today = today
                    )
                } else {
                    null
                }

                // Spend period derivation
                val (periodStart, periodEnd) = periodRange(period, today, zone, ym)
                val periodTxs = if (period == SpendPeriod.MONTH) {
                    monthTxs
                } else {
                    monthTxs.filter { it.timestamp in periodStart until periodEnd }
                }
                var periodSpent = 0L
                var periodReceived = 0L
                for (tx in periodTxs) {
                    if (tx.type == TransactionType.DEBIT) {
                        periodSpent += tx.amountPaise
                    } else if (tx.type == TransactionType.CREDIT) {
                        periodReceived += tx.amountPaise
                    }
                }
                val periodCount = periodTxs.size

                var overallStatus: BudgetStatus? = null
                var attentionCount = 0
                var categoryBudgetsCount = 0
                for (b in budgets) {
                    if (b.budgetKey == Budget.TOTAL_BUDGET_KEY) {
                        overallStatus = BudgetLogic.budgetStatus(b.limitPaise, totalSpent, ym, today)
                    } else {
                        categoryBudgetsCount++
                        val spent = catSpent[b.budgetKey] ?: 0L
                        val status = BudgetLogic.budgetStatus(b.limitPaise, spent, ym, today)
                        if (status.level == BudgetLevel.NEAR || status.level == BudgetLevel.OVER) {
                            attentionCount++
                        }
                    }
                }

                val budgetSummary = DashboardBudgetSummary(
                    hasAnyBudgets = budgets.isNotEmpty(),
                    overallStatus = overallStatus,
                    categoryAttentionCount = attentionCount,
                    totalCategoryBudgetsCount = categoryBudgetsCount
                )

                DashboardUiState.Success(
                    summary = summary,
                    budgetInfo = budgetInfo,
                    selectedPeriod = period,
                    periodSpent = periodSpent,
                    periodReceived = periodReceived,
                    periodCount = periodCount,
                    selectedMonth = ym,
                    canGoNext = canNext,
                    canGoPrevious = canPrev,
                    allMonthTransactions = monthTxs,
                    budgetSummary = budgetSummary
                )
            }.combine(currentMonthSummaryFlow) { baseState, currentMonthSummary ->
                baseState.copy(currentMonthSummary = currentMonthSummary)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )

    fun selectPeriod(period: SpendPeriod) {
        _selectedPeriod.value = period
    }

    fun saveBudget(key: String, limitPaise: Long) {
        viewModelScope.launch {
            val budget = Budget(
                budgetKey = key,
                limitPaise = limitPaise,
                updatedAt = System.currentTimeMillis()
            )
            budgetRepository.upsert(budget)
            alertChecker.check()
        }
    }

    fun deleteBudget(key: String) {
        viewModelScope.launch {
            budgetRepository.deleteByKey(key)
        }
    }

    fun previousMonth() {
        monthSelection.previous()
    }

    fun nextMonth() {
        monthSelection.next()
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = smsSyncManager.syncAll()
            _isSyncing.value = false

            if (result.error != null) {
                _snackbarMessage.emit("Sync failed: ${result.error}")
            } else {
                if (result.imported > 0) {
                    alertChecker.check()
                }
                _snackbarMessage.emit("Imported ${result.imported} new transactions")
            }
        }
    }

    /**
     * Re-scans the last 90 days of SMS unconditionally, ignoring lastSyncedDate.
     * Duplicates are skipped by the existing smsHash index.
     * Does NOT update lastSyncedDate so normal sync cadence is unaffected.
     */
    fun rescanSms() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = smsSyncManager.rescanLast90Days()
            _isSyncing.value = false

            if (result.error != null) {
                // Surface permission denial clearly
                _snackbarMessage.emit("Re-scan failed: ${result.error}")
            } else {
                if (result.imported > 0) {
                    alertChecker.check()
                }
                _snackbarMessage.emit(
                    "Scanned ${result.scanned}, imported ${result.imported}, " +
                    "duplicates ${result.duplicates}, skipped ${result.skipped}"
                )
            }
        }
    }

    suspend fun getAllTransactionsForExport(): List<Transaction> {
        return repository.getAllOnce()
    }

    fun addManualTransaction(
        amountPaise: Long,
        type: TransactionType,
        merchant: String,
        category: Category,
        note: String?
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                amountPaise = amountPaise,
                type = type,
                merchant = merchant.trim(),
                category = category.label,
                timestamp = System.currentTimeMillis(),
                bankName = "Manual",
                balanceAfterPaise = null,
                smsHash = "manual_${UUID.randomUUID()}",
                note = note?.trim()?.takeIf { it.isNotEmpty() }
            )
            repository.insert(tx)
            alertChecker.check()
            _snackbarMessage.emit(
                if (type == TransactionType.DEBIT) "Expense added" else "Income added"
            )
        }
    }

    fun addSampleTransaction() {
        viewModelScope.launch {
            val sample = Transaction(
                amountPaise = 15000L,
                type = TransactionType.DEBIT,
                merchant = "Sample Cafe",
                category = "Food & Dining",
                timestamp = System.currentTimeMillis(),
                bankName = "HDFC",
                balanceAfterPaise = 4520000L,
                smsHash = UUID.randomUUID().toString(),
                note = "Test transaction"
            )
            repository.insert(sample)
        }
    }

    /** Opens the edit bottom sheet for a transaction */
    fun openEditSheet(transaction: Transaction) {
        _editingTransaction.value = transaction
        viewModelScope.launch {
            val key = AutoCategorizer.merchantKey(transaction.merchant)
            _merchantCount.value = repository.countByMerchantKey(key)
        }
    }

    fun closeEditSheet() {
        _editingTransaction.value = null
    }

    /** Saves the edit from the bottom sheet */
    fun saveEdit(
        transaction: Transaction,
        newCategory: Category,
        newNote: String?,
        applyToAll: Boolean
    ) {
        viewModelScope.launch {
            val key = AutoCategorizer.merchantKey(transaction.merchant)
            repository.saveTransactionEdit(
                id = transaction.id,
                category = newCategory.label,
                note = newNote?.takeIf { it.isNotBlank() },
                merchantKey = key,
                applyToAll = applyToAll
            )
            _editingTransaction.value = null
        }
    }

    /**
     * Categorizes all "Uncategorized" transactions using built-in rules.
     * Never changes transactions that already have another category.
     */
    fun autoCategorizeUncategorized() {
        viewModelScope.launch {
            val uncategorized = repository.getUncategorizedTransactions()
            val rules = repository.getAllMerchantRules().associate { it.merchantKey to it.category }
            val updates = uncategorized.mapNotNull { tx ->
                val cat = AutoCategorizer.categorize(tx.merchant, tx.type, rules)
                if (cat != Category.UNCATEGORIZED) Pair(tx.id, cat.label) else null
            }
            if (updates.isNotEmpty()) {
                repository.updateCategoryBatch(updates)
            }
            _snackbarMessage.emit("Categorized ${updates.size} of ${uncategorized.size}")
        }
    }

    /**
     * One-time auto-categorize pass on first run of this app version.
     * Guards with SharedPreferences flag "autoCategorizeV1Done".
     */
    fun runOneTimeAutoCategorizeIfNeeded() {
        val prefs = appContext.getSharedPreferences("upi_tracker_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("autoCategorizeV1Done", false)) return
        viewModelScope.launch {
            val uncategorized = repository.getUncategorizedTransactions()
            val rules = repository.getAllMerchantRules().associate { it.merchantKey to it.category }
            val updates = uncategorized.mapNotNull { tx ->
                val cat = AutoCategorizer.categorize(tx.merchant, tx.type, rules)
                if (cat != Category.UNCATEGORIZED) Pair(tx.id, cat.label) else null
            }
            if (updates.isNotEmpty()) {
                repository.updateCategoryBatch(updates)
            }
            prefs.edit().putBoolean("autoCategorizeV1Done", true).apply()
        }
    }

    companion object {
        fun provideFactory(
            repository: TransactionRepository,
            budgetRepository: BudgetRepository,
            alertChecker: BudgetAlertChecker,
            smsSyncManager: SmsSyncManager,
            monthSelection: MonthSelection,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                    return DashboardViewModel(
                        repository = repository,
                        budgetRepository = budgetRepository,
                        alertChecker = alertChecker,
                        smsSyncManager = smsSyncManager,
                        monthSelection = monthSelection,
                        appContext = appContext
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
