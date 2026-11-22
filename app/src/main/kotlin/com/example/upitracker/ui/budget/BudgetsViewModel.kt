package com.example.upitracker.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Budget
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.data.repository.BudgetRepository
import com.example.upitracker.data.repository.TransactionRepository
import com.example.upitracker.domain.MonthSelection
import com.example.upitracker.domain.budget.BudgetLogic
import com.example.upitracker.domain.budget.BudgetStatus
import com.example.upitracker.service.BudgetAlertChecker
import com.example.upitracker.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class BudgetRowUi(
    val budgetKey: String,
    val label: String,
    val emoji: String,
    val status: BudgetStatus
)

sealed interface BudgetsUiState {
    data object Loading : BudgetsUiState
    data class Success(
        val month: YearMonth,
        val canGoPrevious: Boolean,
        val canGoNext: Boolean,
        val overall: BudgetRowUi?,
        val categoryRows: List<BudgetRowUi>,
        val showNotificationHint: Boolean,
        val allBudgets: List<Budget>
    ) : BudgetsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val monthSelection: MonthSelection,
    private val alertChecker: BudgetAlertChecker,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    private val notificationHintFlow = MutableStateFlow(false)

    val uiState: StateFlow<BudgetsUiState> = combine(
        monthSelection.selectedMonth,
        monthSelection.canGoPrevious,
        monthSelection.canGoNext,
        budgetRepository.getAllFlow(),
        notificationHintFlow
    ) { month, canPrev, canNext, budgets, showHint ->
        Triple(Pair(month, budgets), Pair(canPrev, canNext), showHint)
    }.flatMapLatest { (pair, navPair, showHint) ->
        val (month, budgets) = pair
        val (canPrev, canNext) = navPair

        val start = DateTimeUtils.monthStartMillis(month, zoneId)
        val end = DateTimeUtils.monthEndExclusiveMillis(month, zoneId)

        combine(
            transactionRepository.getTransactionsBetween(start, end)
        ) { (transactions) ->
            val today = LocalDate.now(clock.withZone(zoneId))

            var totalSpent = 0L
            val catSpent = mutableMapOf<String, Long>()
            for (tx in transactions) {
                if (tx.type == TransactionType.DEBIT) {
                    totalSpent += tx.amountPaise
                    val prev = catSpent[tx.category] ?: 0L
                    catSpent[tx.category] = prev + tx.amountPaise
                }
            }

            var overallRow: BudgetRowUi? = null
            val categoryRows = mutableListOf<BudgetRowUi>()

            for (budget in budgets) {
                if (budget.budgetKey == Budget.TOTAL_BUDGET_KEY) {
                    val status = BudgetLogic.budgetStatus(budget.limitPaise, totalSpent, month, today)
                    overallRow = BudgetRowUi(
                        budgetKey = budget.budgetKey,
                        label = "Overall",
                        emoji = "🎯",
                        status = status
                    )
                } else {
                    val spent = catSpent[budget.budgetKey] ?: 0L
                    val status = BudgetLogic.budgetStatus(budget.limitPaise, spent, month, today)
                    val cat = Category.fromLabel(budget.budgetKey)
                    categoryRows.add(
                        BudgetRowUi(
                            budgetKey = budget.budgetKey,
                            label = cat.label,
                            emoji = cat.emoji,
                            status = status
                        )
                    )
                }
            }

            // Order category rows by percent descending
            categoryRows.sortByDescending { it.status.percent }

            BudgetsUiState.Success(
                month = month,
                canGoPrevious = canPrev,
                canGoNext = canNext,
                overall = overallRow,
                categoryRows = categoryRows,
                showNotificationHint = showHint,
                allBudgets = budgets
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState.Loading
    )

    fun setNotificationHintVisible(visible: Boolean) {
        notificationHintFlow.value = visible
    }

    fun previousMonth() = monthSelection.previous()
    fun nextMonth() = monthSelection.next()

    fun saveBudget(key: String, limitPaise: Long, onSaved: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val budget = Budget(
                budgetKey = key,
                limitPaise = limitPaise,
                updatedAt = System.currentTimeMillis()
            )
            budgetRepository.upsert(budget)
            alertChecker.resetAndMark(key)
            onSaved()
        }
    }

    fun deleteBudget(key: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetRepository.deleteByKey(key)
            alertChecker.deleteMarkers(key)
            onDeleted()
        }
    }

    companion object {
        fun provideFactory(
            budgetRepository: BudgetRepository,
            transactionRepository: TransactionRepository,
            monthSelection: MonthSelection,
            alertChecker: BudgetAlertChecker
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BudgetsViewModel(
                    budgetRepository = budgetRepository,
                    transactionRepository = transactionRepository,
                    monthSelection = monthSelection,
                    alertChecker = alertChecker
                ) as T
            }
        }
    }
}
