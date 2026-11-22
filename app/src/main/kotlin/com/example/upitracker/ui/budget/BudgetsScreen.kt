package com.example.upitracker.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import com.example.upitracker.MonthSelector
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Budget
import com.example.upitracker.domain.budget.BudgetLevel
import com.example.upitracker.domain.budget.BudgetLogic
import com.example.upitracker.domain.budget.MAX_BUDGET_PAISE
import com.example.upitracker.ui.components.AppCard
import com.example.upitracker.ui.components.CategoryColorDot
import com.example.upitracker.ui.components.ThemedLinearProgress
import com.example.upitracker.ui.components.budgetLevelColor
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.util.CurrencyUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun getBudgetLevelColor(level: BudgetLevel, isDark: Boolean = false): Color {
    return budgetLevelColor(level)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    uiState: BudgetsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSaveBudget: (key: String, limitPaise: Long) -> Unit,
    onDeleteBudget: (key: String) -> Unit,
    onNotificationSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingBudget by remember { mutableStateOf<BudgetSheetTarget?>(null) }

    when (uiState) {
        is BudgetsUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is BudgetsUiState.Success -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp24)
            ) {
                // 1. Month selector
                item {
                    MonthSelector(
                        selectedMonth = uiState.month,
                        canGoPrevious = uiState.canGoPrevious,
                        canGoNext = uiState.canGoNext,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth
                    )
                }

                // Subtitle
                item {
                    Text(
                        text = "Limits repeat every month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2. Notification permission hint if needed
                if (uiState.showNotificationHint) {
                    item {
                        AppCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.cardPadding),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(Modifier.width(Dimens.dp8))
                                Text(
                                    text = "Turn on notifications to get budget alerts",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(Dimens.dp8))
                                Button(onClick = onNotificationSettingsClick) {
                                    Text("Turn on")
                                }
                            }
                        }
                    }
                }

                // 3. Overall budget card
                item {
                    AppCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Overall monthly budget",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (uiState.overall != null) {
                                    TextButton(onClick = {
                                        editingBudget = BudgetSheetTarget.Edit(
                                            budgetKey = Budget.TOTAL_BUDGET_KEY,
                                            label = "Overall",
                                            emoji = "",
                                            currentLimitPaise = uiState.overall.status.limitPaise
                                        )
                                    }) {
                                        Text("Edit")
                                    }
                                }
                            }

                            Spacer(Modifier.height(Dimens.dp8))

                            if (uiState.overall != null) {
                                BudgetRowContent(
                                    row = uiState.overall,
                                    isCurrentMonth = uiState.month == YearMonth.from(LocalDate.now()),
                                    onClick = {
                                        editingBudget = BudgetSheetTarget.Edit(
                                            budgetKey = Budget.TOTAL_BUDGET_KEY,
                                            label = "Overall",
                                            emoji = "",
                                            currentLimitPaise = uiState.overall.status.limitPaise
                                        )
                                    }
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "No overall budget set",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(onClick = {
                                        editingBudget = BudgetSheetTarget.AddOverall
                                    }) {
                                        Text("Set budget")
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Category budgets section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category budgets",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(onClick = {
                            val existingKeys = uiState.allBudgets.map { it.budgetKey }.toSet()
                            val availableCats = Category.entries.filter {
                                it != Category.INCOME && it != Category.UNCATEGORIZED && it.label !in existingKeys
                            }
                            editingBudget = BudgetSheetTarget.AddCategory(availableCats)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Dimens.dp16))
                            Spacer(Modifier.width(Dimens.dp4))
                            Text("Add")
                        }
                    }
                }

                if (uiState.categoryRows.isEmpty()) {
                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.dp24),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No category budgets set",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.categoryRows, key = { it.budgetKey }) { row ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                                BudgetRowContent(
                                    row = row,
                                    isCurrentMonth = uiState.month == YearMonth.from(LocalDate.now()),
                                    onClick = {
                                        editingBudget = BudgetSheetTarget.Edit(
                                            budgetKey = row.budgetKey,
                                            label = row.label,
                                            emoji = "",
                                            currentLimitPaise = row.status.limitPaise
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Add/Edit
    editingBudget?.let { target ->
        BudgetEditBottomSheet(
            target = target,
            onDismiss = { editingBudget = null },
            onSave = { key, limitPaise ->
                onSaveBudget(key, limitPaise)
                editingBudget = null
            },
            onDelete = { key ->
                onDeleteBudget(key)
                editingBudget = null
            }
        )
    }
}

@Composable
fun BudgetRowContent(
    row: BudgetRowUi,
    isCurrentMonth: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = row.status
    val levelColor = budgetLevelColor(status.level)
    val statusIcon = when (status.level) {
        BudgetLevel.OK -> Icons.Default.CheckCircle
        BudgetLevel.NEAR -> Icons.Default.Warning
        BudgetLevel.OVER -> Icons.Default.Warning
    }
    val statusWord = when (status.level) {
        BudgetLevel.OK -> "On track"
        BudgetLevel.NEAR -> "Near limit"
        BudgetLevel.OVER -> "Over budget"
    }
    val category = Category.fromLabel(row.label)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (row.budgetKey != Budget.TOTAL_BUDGET_KEY) {
                    CategoryColorDot(category)
                    Spacer(Modifier.width(Dimens.dp8))
                }
                Text(row.label, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "${CurrencyUtils.formatRupees(status.spentPaise)} of ${CurrencyUtils.formatRupees(status.limitPaise)} used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(Dimens.dp8))

        val fraction = if (status.limitPaise > 0L) {
            (status.spentPaise.toFloat() / status.limitPaise.toFloat()).coerceIn(0f, 1f)
        } else 0f

        ThemedLinearProgress(
            fraction = fraction,
            percent = status.percent,
            color = levelColor
        )

        Spacer(Modifier.height(Dimens.dp8))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusWord,
                    tint = levelColor,
                    modifier = Modifier.size(Dimens.dp16)
                )
                Spacer(Modifier.width(Dimens.dp4))
                Text(
                    text = statusWord,
                    style = MaterialTheme.typography.labelSmall,
                    color = levelColor
                )
            }

            val subText = if (isCurrentMonth) {
                if (status.remainingPaise >= 0L) {
                    val perDay = status.perDayLeftPaise?.let { " • ${CurrencyUtils.formatRupees(it)}/day for ${status.daysLeft} days" } ?: ""
                    "${CurrencyUtils.formatRupees(status.remainingPaise)} left$perDay"
                } else {
                    "Over by ${CurrencyUtils.formatRupees(-status.remainingPaise)}"
                }
            } else {
                if (status.remainingPaise >= 0L) {
                    "${CurrencyUtils.formatRupees(status.remainingPaise)} left"
                } else {
                    "Over by ${CurrencyUtils.formatRupees(-status.remainingPaise)}"
                }
            }

            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

sealed interface BudgetSheetTarget {
    data object AddOverall : BudgetSheetTarget
    data class AddCategory(val availableCategories: List<Category>) : BudgetSheetTarget
    data class Edit(
        val budgetKey: String,
        val label: String,
        val emoji: String,
        val currentLimitPaise: Long
    ) : BudgetSheetTarget
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetEditBottomSheet(
    target: BudgetSheetTarget,
    onDismiss: () -> Unit,
    onSave: (key: String, limitPaise: Long) -> Unit,
    onDelete: (key: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isEditing = target is BudgetSheetTarget.Edit
    var selectedCategory by remember {
        mutableStateOf(
            if (target is BudgetSheetTarget.AddCategory) target.availableCategories.firstOrNull() else null
        )
    }

    val initialAmount = when (target) {
        is BudgetSheetTarget.Edit -> (target.currentLimitPaise / 100).toString()
        else -> ""
    }
    var amountText by rememberSaveable { mutableStateOf(initialAmount) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val parsedPaise = BudgetLogic.parseRupeesToPaise(amountText)
    val isValid = parsedPaise != null && parsedPaise in 1L..MAX_BUDGET_PAISE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.dp24)
                .padding(bottom = Dimens.dp24),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp16)
        ) {
            val title = when (target) {
                is BudgetSheetTarget.AddOverall -> "Overall monthly budget"
                is BudgetSheetTarget.Edit -> if (target.budgetKey == Budget.TOTAL_BUDGET_KEY) "Overall monthly budget" else "${target.label} budget"
                is BudgetSheetTarget.AddCategory -> "Category budget"
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )

            // Category selector when adding category budget
            if (target is BudgetSheetTarget.AddCategory) {
                if (target.availableCategories.isEmpty()) {
                    Text(
                        text = "All categories already have a budget set.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Choose category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dp8)
                    ) {
                        target.availableCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }
            }

            // Amount TextField
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Monthly limit") },
                prefix = { Text("₹ ") },
                placeholder = { Text("e.g. 5000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amountText.isNotBlank() && !isValid,
                supportingText = {
                    if (parsedPaise != null && isValid) {
                        Text("Limit: ${CurrencyUtils.formatRupees(parsedPaise)}")
                    } else if (amountText.isNotBlank()) {
                        Text(
                            text = "Enter a valid amount between ₹1 and ₹1,00,00,000",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                } else {
                    Spacer(Modifier.width(Dimens.dp4))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val key = when (target) {
                                is BudgetSheetTarget.AddOverall -> Budget.TOTAL_BUDGET_KEY
                                is BudgetSheetTarget.AddCategory -> selectedCategory?.label ?: return@Button
                                is BudgetSheetTarget.Edit -> target.budgetKey
                            }
                            if (parsedPaise != null && isValid) {
                                onSave(key, parsedPaise)
                            }
                        },
                        enabled = isValid && (target !is BudgetSheetTarget.AddCategory || selectedCategory != null)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && target is BudgetSheetTarget.Edit) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete budget?") },
            text = { Text("This will remove the monthly budget limit for ${target.label}.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(target.budgetKey)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
