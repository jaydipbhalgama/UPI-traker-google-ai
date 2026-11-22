package com.example.upitracker.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.data.repository.TransactionRepository
import com.example.upitracker.domain.MonthSelection
import com.example.upitracker.domain.summary.MonthlySummary
import com.example.upitracker.domain.summary.MonthlySummaryCalculator
import com.example.upitracker.ui.dashboard.DashboardCalculator
import com.example.upitracker.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

sealed interface InsightsUiState {
    data object Loading : InsightsUiState
    data object Empty : InsightsUiState
    data class Success(
        val selectedMonth: YearMonth,
        val canGoPrevious: Boolean,
        val canGoNext: Boolean,
        val totalSpentPaise: Long,
        val totalReceivedPaise: Long,
        val comparison: ComparisonResult,
        val slices: List<CategorySlice>,
        val dailySpend: LongArray,
        val visibleDays: Int,
        val highestDay: Int?,
        val averageDailyPaise: Long,
        val monthly: List<MonthTotals>,
        val currentMonthSummary: MonthlySummary = MonthlySummary(month = YearMonth.now())
    ) : InsightsUiState
}

class InsightsViewModel(
    private val repository: TransactionRepository,
    private val monthSelection: MonthSelection,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    val selectedMonth: StateFlow<YearMonth> = monthSelection.selectedMonth

    private val currentMonthSummaryFlow = run {
        val currentMonth = YearMonth.now(zoneId)
        val currentStart = DateTimeUtils.monthStartMillis(currentMonth, zoneId)
        val currentEnd = DateTimeUtils.monthEndExclusiveMillis(currentMonth, zoneId)
        repository.getTransactionsBetween(currentStart, currentEnd)
            .map { txs -> MonthlySummaryCalculator.calculate(txs, currentMonth, zoneId) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InsightsUiState> = monthSelection.selectedMonth
        .flatMapLatest { ym ->
            val startMonth = ym.minusMonths(5)
            val windowStartMillis = DateTimeUtils.monthStartMillis(startMonth, zoneId)
            val windowEndExclusiveMillis = DateTimeUtils.monthEndExclusiveMillis(ym, zoneId)

            val monthStartMillis = DateTimeUtils.monthStartMillis(ym, zoneId)
            val monthEndExclusiveMillis = DateTimeUtils.monthEndExclusiveMillis(ym, zoneId)

            combine(
                repository.getTransactionsBetween(windowStartMillis, windowEndExclusiveMillis),
                repository.getOldestTimestamp(),
                repository.getCount(),
                monthSelection.canGoNext,
                monthSelection.canGoPrevious
            ) { windowTxs, oldestTs, totalCount, canNext, canPrev ->
                if (totalCount == 0) {
                    return@combine InsightsUiState.Empty
                }

                val currentMonthTxs = windowTxs.filter {
                    it.timestamp >= monthStartMillis && it.timestamp < monthEndExclusiveMillis
                }

                val today = LocalDate.now(zoneId)
                val summary = DashboardCalculator.calculate(currentMonthTxs, ym, today)

                // Comparison against previous month
                val comp = ChartCalculations.comparison(
                    transactions = windowTxs,
                    selectedMonth = ym,
                    today = today,
                    zone = zoneId
                )

                // Category slices for DEBIT
                val slices = ChartCalculations.categorySlices(summary.categoryBreakdown, maxSlices = 6)

                // Daily spend
                val daily = ChartCalculations.dailySpend(currentMonthTxs, ym, zoneId)
                val visible = ChartCalculations.visibleDays(ym, today)
                val highest = ChartCalculations.highestDay(daily)

                // Monthly totals for 6 months
                val oldestYm = oldestTs?.let { YearMonth.from(Instant.ofEpochMilli(it).atZone(zoneId)) }
                val monthly = ChartCalculations.monthlyTotals(
                    transactions = windowTxs,
                    endMonth = ym,
                    count = 6,
                    oldestMonth = oldestYm,
                    zone = zoneId
                )

                InsightsUiState.Success(
                    selectedMonth = ym,
                    canGoPrevious = canPrev,
                    canGoNext = canNext,
                    totalSpentPaise = summary.totalSpentPaise,
                    totalReceivedPaise = summary.totalReceivedPaise,
                    comparison = comp,
                    slices = slices,
                    dailySpend = daily,
                    visibleDays = visible,
                    highestDay = highest,
                    averageDailyPaise = summary.averageDailySpendPaise,
                    monthly = monthly
                )
            }.combine(currentMonthSummaryFlow) { baseState, currentMonthSummary ->
                when (baseState) {
                    is InsightsUiState.Success -> baseState.copy(currentMonthSummary = currentMonthSummary)
                    else -> baseState
                }
            }.flowOn(Dispatchers.Default)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InsightsUiState.Loading
        )

    fun previousMonth() = monthSelection.previous()
    fun nextMonth() = monthSelection.next()

    companion object {
        fun provideFactory(
            repository: TransactionRepository,
            monthSelection: MonthSelection,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(InsightsViewModel::class.java)) {
                    return InsightsViewModel(repository, monthSelection, zoneId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
