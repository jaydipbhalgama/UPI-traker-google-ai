package com.example.upitracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.repository.TransactionRepository
import com.example.upitracker.domain.MonthSelection
import com.example.upitracker.domain.filter.FilterLogic
import com.example.upitracker.domain.filter.FilterState
import com.example.upitracker.domain.filter.FilterSummary
import com.example.upitracker.domain.filter.SortOrder
import com.example.upitracker.domain.filter.TransactionTypeFilter
import com.example.upitracker.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.ZoneId

data class AllTransactionsUiState(
    val filteredTransactions: List<Transaction> = emptyList(),
    val totalCountInScope: Int = 0,
    val summary: FilterSummary = FilterSummary(0, 0L, 0L),
    val filterState: FilterState = FilterState(),
    val searchQueryInput: String = "",
    val selectedMonth: YearMonth = YearMonth.now(),
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AllTransactionsViewModel(
    private val repository: TransactionRepository,
    private val monthSelection: MonthSelection,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    // Immediate user input for the search bar
    private val _searchQueryInput = MutableStateFlow("")
    val searchQueryInput: StateFlow<String> = _searchQueryInput.asStateFlow()

    // Non-query filter settings
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // Debounced query for background filtering
    private val debouncedQuery = _searchQueryInput
        .debounce(250L)
        .distinctUntilChanged()

    // Raw transactions stream: either selected month or all time
    private val sourceTransactions = _filterState
        .flatMapLatest { state ->
            if (state.allTime) {
                repository.getAllTransactions()
            } else {
                monthSelection.selectedMonth.flatMapLatest { month ->
                    val start = DateTimeUtils.monthStartMillis(month, zoneId)
                    val end = DateTimeUtils.monthEndExclusiveMillis(month, zoneId)
                    repository.getTransactionsBetween(start, end)
                }
            }
        }

    private val searchFlow = combine(_searchQueryInput, debouncedQuery) { input, eff ->
        Pair(input, eff)
    }

    private val monthInfo = combine(
        monthSelection.selectedMonth,
        monthSelection.canGoPrevious,
        monthSelection.canGoNext
    ) { month, canPrev, canNext ->
        Triple(month, canPrev, canNext)
    }

    val uiState: StateFlow<AllTransactionsUiState> = combine(
        sourceTransactions,
        _filterState,
        searchFlow,
        monthInfo
    ) { txs, currentFilter, (queryInput, effectiveQuery), (month, canPrev, canNext) ->
        val activeFilter = currentFilter.copy(query = effectiveQuery)
        val filtered = FilterLogic.applyFilters(txs, activeFilter)
        val summary = FilterLogic.summarize(filtered)

        AllTransactionsUiState(
            filteredTransactions = filtered,
            totalCountInScope = txs.size,
            summary = summary,
            filterState = currentFilter.copy(query = queryInput),
            searchQueryInput = queryInput,
            selectedMonth = month,
            canGoPrevious = canPrev,
            canGoNext = canNext,
            isLoading = false
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AllTransactionsUiState()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQueryInput.value = newQuery
    }

    fun onFilterStateChange(newState: FilterState) {
        _filterState.value = newState
        _searchQueryInput.value = newState.query
    }

    fun setAllTime(allTime: Boolean) {
        _filterState.value = _filterState.value.copy(allTime = allTime)
    }

    fun setCategory(category: Category?) {
        _filterState.value = _filterState.value.copy(category = category)
    }

    fun setTypeFilter(typeFilter: TransactionTypeFilter) {
        _filterState.value = _filterState.value.copy(typeFilter = typeFilter)
    }

    fun setAmountRange(minPaise: Long?, maxPaise: Long?) {
        _filterState.value = _filterState.value.copy(minPaise = minPaise, maxPaise = maxPaise)
    }

    fun setSort(sort: SortOrder) {
        _filterState.value = _filterState.value.copy(sort = sort)
    }

    fun clearFilters() {
        _searchQueryInput.value = ""
        _filterState.value = FilterState(allTime = _filterState.value.allTime)
    }

    fun reset() {
        _searchQueryInput.value = ""
        _filterState.value = FilterState()
    }

    fun preset(category: Category?, debitOnly: Boolean) {
        _searchQueryInput.value = ""
        _filterState.value = FilterState(
            category = category,
            typeFilter = if (debitOnly) TransactionTypeFilter.DEBIT else TransactionTypeFilter.ALL
        )
    }

    fun previousMonth() = monthSelection.previous()
    fun nextMonth() = monthSelection.next()

    companion object {
        fun provideFactory(
            repository: TransactionRepository,
            monthSelection: MonthSelection
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AllTransactionsViewModel(
                    repository = repository,
                    monthSelection = monthSelection
                ) as T
            }
        }
    }
}
