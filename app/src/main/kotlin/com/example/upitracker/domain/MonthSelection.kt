package com.example.upitracker.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Shared state holder for selected month across Dashboard and Insights.
 *
 * Rules:
 * - Default is current month in [zoneId].
 * - Cannot go after current month (canGoNext = false).
 * - Cannot go before oldest transaction's month (canGoPrevious = false).
 *   If there are no transactions, only current month is allowed.
 */
class MonthSelection(
    oldestTimestampFlow: Flow<Long?>,
    scope: CoroutineScope,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    initialMonth: YearMonth = YearMonth.now(zoneId)
) {
    private val _selectedMonth = MutableStateFlow(initialMonth)
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val oldestMonthFlow: Flow<YearMonth?> = oldestTimestampFlow.map { oldestTs ->
        oldestTs?.let {
            YearMonth.from(Instant.ofEpochMilli(it).atZone(zoneId))
        }
    }

    val canGoNext: StateFlow<Boolean> = _selectedMonth.map { selected ->
        selected.isBefore(YearMonth.now(zoneId))
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val canGoPrevious: StateFlow<Boolean> = combine(_selectedMonth, oldestMonthFlow) { selected, oldest ->
        if (oldest != null) {
            selected.isAfter(oldest)
        } else {
            false
        }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    fun previous() {
        if (canGoPrevious.value) {
            _selectedMonth.value = _selectedMonth.value.minusMonths(1)
        }
    }

    fun next() {
        if (canGoNext.value) {
            _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        }
    }

    fun setSelectedMonth(ym: YearMonth) {
        val current = YearMonth.now(zoneId)
        if (!ym.isAfter(current)) {
            _selectedMonth.value = ym
        }
    }
}
