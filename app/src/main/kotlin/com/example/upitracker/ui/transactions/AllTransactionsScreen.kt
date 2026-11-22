package com.example.upitracker.ui.transactions

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.upitracker.MonthSelector
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.domain.budget.BudgetLogic
import com.example.upitracker.domain.filter.FilterState
import com.example.upitracker.domain.filter.SortOrder
import com.example.upitracker.domain.filter.TransactionTypeFilter
import com.example.upitracker.ui.components.AppCard
import com.example.upitracker.ui.components.CategoryColorDot
import com.example.upitracker.ui.components.EmptyState
import com.example.upitracker.ui.components.TransactionRow
import com.example.upitracker.ui.components.creditAmountColor
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.ui.theme.MoneyTypography
import com.example.upitracker.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
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
    val creditColor = creditAmountColor()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.screenPadding)
    ) {
        Spacer(Modifier.height(Dimens.dp8))

        // Search Bar
        OutlinedTextField(
            value = uiState.searchQueryInput,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    "Search merchant, note, category or amount",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (uiState.searchQueryInput.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.defaultMinSize(
                            minWidth = Dimens.minTouchTarget,
                            minHeight = Dimens.minTouchTarget
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Dimens.dp8))

        // Scrollable Filter Strip (48dp minHeight targets, Material icons, zero emojis)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filterCount = uiState.filterState.activeFilterCount
            FilterChip(
                selected = uiState.filterState.hasActiveFilters,
                onClick = { showFiltersSheet = true },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(if (filterCount > 0) "Filters ($filterCount)" else "Filters") },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
            )

            FilterChip(
                selected = uiState.filterState.allTime,
                onClick = { onSetAllTime(!uiState.filterState.allTime) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("All time") },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
            )

            FilterChip(
                selected = uiState.filterState.category == Category.UNCATEGORIZED,
                onClick = {
                    onSetCategory(
                        if (uiState.filterState.category == Category.UNCATEGORIZED) null else Category.UNCATEGORIZED
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("Uncategorized") },
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
            )

            uiState.filterState.category?.let { cat ->
                if (cat != Category.UNCATEGORIZED) {
                    FilterChip(
                        selected = true,
                        onClick = { onSetCategory(null) },
                        leadingIcon = { CategoryColorDot(cat) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove category filter",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(cat.label) },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    )
                }
            }

            if (uiState.filterState.typeFilter != TransactionTypeFilter.ALL) {
                val label = if (uiState.filterState.typeFilter == TransactionTypeFilter.DEBIT) {
                    "Spent only"
                } else {
                    "Received only"
                }
                FilterChip(
                    selected = true,
                    onClick = { onSetTypeFilter(TransactionTypeFilter.ALL) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove type filter",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(label) },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                )
            }

            if (uiState.filterState.minPaise != null || uiState.filterState.maxPaise != null) {
                val minStr = uiState.filterState.minPaise?.let { CurrencyUtils.formatRupees(it) } ?: "₹0"
                val maxStr = uiState.filterState.maxPaise?.let { CurrencyUtils.formatRupees(it) } ?: "Any"
                FilterChip(
                    selected = true,
                    onClick = {
                        onFilterStateChange(uiState.filterState.copy(minPaise = null, maxPaise = null))
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove amount range filter",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("$minStr - $maxStr") },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                )
            }
        }

        Spacer(Modifier.height(Dimens.dp8))

        if (!uiState.filterState.allTime) {
            MonthSelector(
                selectedMonth = uiState.selectedMonth,
                canGoPrevious = uiState.canGoPrevious,
                canGoNext = uiState.canGoNext,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth
            )
            Spacer(Modifier.height(Dimens.dp8))
        }

        // Summary Bar Card
        val summary = uiState.summary
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.dp16, vertical = Dimens.dp12),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${summary.count} ITEM${if (summary.count != 1) "S" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (uiState.filterState.allTime) "All time" else "Monthly ledger",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp16)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.formatRupees(summary.spentPaise),
                            style = MoneyTypography.rowAmount,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Received",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.formatRupees(summary.receivedPaise),
                            style = MoneyTypography.rowAmount,
                            color = creditColor
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.dp12))

        if (uiState.filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = Dimens.dp24),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "No matching transactions",
                    hint = "Try clearing filters or searching by a different merchant or amount.",
                    actionText = "Clear filters",
                    onActionClick = onClearFilters
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp8)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.dp24)
                .padding(bottom = Dimens.dp24),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp16)
        ) {
            Text(
                text = "Filter Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Column {
                Text(
                    "Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = typeFilter == TransactionTypeFilter.ALL,
                        onClick = { typeFilter = TransactionTypeFilter.ALL },
                        label = { Text("All") },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    )
                    FilterChip(
                        selected = typeFilter == TransactionTypeFilter.DEBIT,
                        onClick = { typeFilter = TransactionTypeFilter.DEBIT },
                        label = { Text("Spent") },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    )
                    FilterChip(
                        selected = typeFilter == TransactionTypeFilter.CREDIT,
                        onClick = { typeFilter = TransactionTypeFilter.CREDIT },
                        label = { Text("Received") },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    )
                }
            }

            Column {
                Text(
                    "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Category.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                            leadingIcon = { CategoryColorDot(cat) },
                            label = { Text(cat.label) },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                        )
                    }
                }
            }

            Column {
                Text(
                    "Amount range (₹)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        shape = RoundedCornerShape(14.dp),
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
                        shape = RoundedCornerShape(14.dp),
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
                    Text(
                        errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Column {
                Text(
                    "Sort by",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = sortOrder == SortOrder.NEWEST,
                        onClick = { sortOrder = SortOrder.NEWEST },
                        label = { Text("Newest") },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    )
                    FilterChip(
                        selected = sortOrder == SortOrder.OLDEST,
                        onClick = { sortOrder = SortOrder.OLDEST },
                        label = { Text("Oldest") },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    )
                    FilterChip(
                        selected = sortOrder == SortOrder.HIGHEST,
                        onClick = { sortOrder = SortOrder.HIGHEST },
                        label = { Text("Highest amount") },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    )
                    FilterChip(
                        selected = sortOrder == SortOrder.LOWEST,
                        onClick = { sortOrder = SortOrder.LOWEST },
                        label = { Text("Lowest amount") },
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                ) {
                    Text("Reset")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    ) {
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
                        enabled = !hasAmountError,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
