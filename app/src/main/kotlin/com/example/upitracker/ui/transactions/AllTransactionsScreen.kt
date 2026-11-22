package com.example.upitracker.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upitracker.MonthSelector
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.domain.budget.BudgetLogic
import com.example.upitracker.domain.filter.FilterState
import com.example.upitracker.domain.filter.SortOrder
import com.example.upitracker.domain.filter.TransactionTypeFilter
import com.example.upitracker.ui.components.EmptyState
import com.example.upitracker.ui.components.TransactionRow
import com.example.upitracker.util.CurrencyUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    uiState: AllTransactionsUiState,
    onSearchQueryChange: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onFilterStateChange: (FilterState) -> Unit,
    onSetAllTime: (Boolean) -> Unit,
    onSetCategory: (Category?) -> Unit,
    onSetTypeFilter: (TransactionTypeFilter) -> Unit,
    onClearFilters: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFiltersSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // 1. Search Bar
        OutlinedTextField(
            value = uiState.searchQueryInput,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search merchant, note, category or amount") },
            leadingIcon = { Text("🔍", modifier = Modifier.padding(start = 8.dp)) },
            trailingIcon = {
                if (uiState.searchQueryInput.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.defaultMinSize(minWidth = com.example.upitracker.ui.theme.Dimens.minTouchTarget, minHeight = com.example.upitracker.ui.theme.Dimens.minTouchTarget)
                    ) {
                        Text(
                            "✕",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { contentDescription = "Clear search" }
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // 2. Filter Action Chips & Active Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Filters button
            val filterCount = uiState.filterState.activeFilterCount
            FilterChip(
                selected = uiState.filterState.hasActiveFilters,
                onClick = { showFiltersSheet = true },
                label = { Text(if (filterCount > 0) "Filters ($filterCount)" else "Filters") }
            )

            // All time chip
            FilterChip(
                selected = uiState.filterState.allTime,
                onClick = { onSetAllTime(!uiState.filterState.allTime) },
                label = { Text("All time") }
            )

            // Uncategorized chip
            FilterChip(
                selected = uiState.filterState.category == Category.UNCATEGORIZED,
                onClick = {
                    onSetCategory(
                        if (uiState.filterState.category == Category.UNCATEGORIZED) null else Category.UNCATEGORIZED
                    )
                },
                label = { Text("❓ Uncategorized") }
            )

            // Dismissible active filter chips
            uiState.filterState.category?.let { cat ->
                if (cat != Category.UNCATEGORIZED) {
                    FilterChip(
                        selected = true,
                        onClick = { onSetCategory(null) },
                        label = { Text("${cat.emoji} ${cat.label} ✕") }
                    )
                }
            }

            if (uiState.filterState.typeFilter != TransactionTypeFilter.ALL) {
                val label = if (uiState.filterState.typeFilter == TransactionTypeFilter.DEBIT) "Spent only ✕" else "Received only ✕"
                FilterChip(
                    selected = true,
                    onClick = { onSetTypeFilter(TransactionTypeFilter.ALL) },
                    label = { Text(label) }
                )
            }

            if (uiState.filterState.minPaise != null || uiState.filterState.maxPaise != null) {
                val minStr = uiState.filterState.minPaise?.let { CurrencyUtils.formatRupees(it) } ?: "₹0"
                val maxStr = uiState.filterState.maxPaise?.let { CurrencyUtils.formatRupees(it) } ?: "∞"
                FilterChip(
                    selected = true,
                    onClick = {
                        onFilterStateChange(uiState.filterState.copy(minPaise = null, maxPaise = null))
                    },
                    label = { Text("$minStr - $maxStr ✕") }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 3. Month selector or All Time header
        if (!uiState.filterState.allTime) {
            MonthSelector(
                selectedMonth = uiState.selectedMonth,
                canGoPrevious = uiState.canGoPrevious,
                canGoNext = uiState.canGoNext,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth
            )
            Spacer(Modifier.height(6.dp))
        }

        // 4. Summary line
        val summary = uiState.summary
        Text(
            text = "${summary.count} transaction${if (summary.count != 1) "s" else ""} • Spent ${CurrencyUtils.formatRupees(summary.spentPaise)} • Received ${CurrencyUtils.formatRupees(summary.receivedPaise)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(Modifier.height(4.dp))

        // 5. Transaction list or empty state
        if (uiState.filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    emoji = "🔍",
                    title = "No transactions match",
                    hint = "Try adjusting your search terms or filters.",
                    actionText = "Clear filters",
                    onActionClick = onClearFilters
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = uiState.filteredTransactions, key = { it.id }) { tx ->
                    TransactionRow(transaction = tx, onClick = { onTransactionClick(tx) })
                }
            }
        }
    }

    if (showFiltersSheet) {
        TransactionFiltersBottomSheet(
            currentFilter = uiState.filterState,
            onDismiss = { showFiltersSheet = false },
            onApply = { newFilter ->
                onFilterStateChange(newFilter)
                showFiltersSheet = false
            },
            onReset = {
                onClearFilters()
                showFiltersSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFiltersBottomSheet(
    currentFilter: FilterState,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var typeFilter by rememberSaveable { mutableStateOf(currentFilter.typeFilter) }
    var selectedCategory by remember { mutableStateOf(currentFilter.category) }
    var minText by rememberSaveable {
        mutableStateOf(currentFilter.minPaise?.let { (it / 100).toString() } ?: "")
    }
    var maxText by rememberSaveable {
        mutableStateOf(currentFilter.maxPaise?.let { (it / 100).toString() } ?: "")
    }
    var sortOrder by rememberSaveable { mutableStateOf(currentFilter.sort) }

    val parsedMin = if (minText.isNotBlank()) BudgetLogic.parseRupeesToPaise(minText) else null
    val parsedMax = if (maxText.isNotBlank()) BudgetLogic.parseRupeesToPaise(maxText) else null

    val isMinInvalid = minText.isNotBlank() && parsedMin == null
    val isMaxInvalid = maxText.isNotBlank() && parsedMax == null
    val isRangeInvalid = parsedMin != null && parsedMax != null && parsedMin > parsedMax
    val hasAmountError = isMinInvalid || isMaxInvalid || isRangeInvalid

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Filter transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Type
            Column {
                Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = typeFilter == TransactionTypeFilter.ALL,
                        onClick = { typeFilter = TransactionTypeFilter.ALL },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = typeFilter == TransactionTypeFilter.DEBIT,
                        onClick = { typeFilter = TransactionTypeFilter.DEBIT },
                        label = { Text("Spent") }
                    )
                    FilterChip(
                        selected = typeFilter == TransactionTypeFilter.CREDIT,
                        onClick = { typeFilter = TransactionTypeFilter.CREDIT },
                        label = { Text("Received") }
                    )
                }
            }

            // Category
            Column {
                Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Category.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                            label = { Text("${cat.emoji} ${cat.label}") }
                        )
                    }
                }
            }

            // Amount Range
            Column {
                Text("Amount range (₹)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { minText = it },
                        label = { Text("Min") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = isMinInvalid || isRangeInvalid,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxText,
                        onValueChange = { maxText = it },
                        label = { Text("Max") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = isMaxInvalid || isRangeInvalid,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (hasAmountError) {
                    val errorMsg = when {
                        isRangeInvalid -> "Min amount cannot be greater than Max amount"
                        isMinInvalid -> "Invalid Min amount"
                        else -> "Invalid Max amount"
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(errorMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            // Sort
            Column {
                Text("Sort by", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortOrder == SortOrder.NEWEST,
                        onClick = { sortOrder = SortOrder.NEWEST },
                        label = { Text("Newest") }
                    )
                    FilterChip(
                        selected = sortOrder == SortOrder.OLDEST,
                        onClick = { sortOrder = SortOrder.OLDEST },
                        label = { Text("Oldest") }
                    )
                    FilterChip(
                        selected = sortOrder == SortOrder.HIGHEST,
                        onClick = { sortOrder = SortOrder.HIGHEST },
                        label = { Text("Highest amount") }
                    )
                    FilterChip(
                        selected = sortOrder == SortOrder.LOWEST,
                        onClick = { sortOrder = SortOrder.LOWEST },
                        label = { Text("Lowest amount") }
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReset) {
                    Text("Reset")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val newFilter = currentFilter.copy(
                                typeFilter = typeFilter,
                                category = selectedCategory,
                                minPaise = parsedMin,
                                maxPaise = parsedMax,
                                sort = sortOrder
                            )
                            onApply(newFilter)
                        },
                        enabled = !hasAmountError
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
