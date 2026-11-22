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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upitracker.MonthSelector
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Budget
import com.example.upitracker.domain.budget.BudgetLevel
import com.example.upitracker.domain.budget.BudgetLogic
import com.example.upitracker.domain.budget.MAX_BUDGET_PAISE
import com.example.upitracker.ui.components.AppCard
import com.example.upitracker.ui.components.CategoryColorDot
import com.example.upitracker.ui.components.CategoryIconBadge
import com.example.upitracker.ui.components.EmptyState
import com.example.upitracker.ui.components.SectionHeader
import com.example.upitracker.ui.components.StatusPill
import com.example.upitracker.ui.components.ThemedLinearProgress
import com.example.upitracker.ui.components.budgetLevelColor
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.ui.theme.MoneyTypography
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
                verticalArrangement = Arrangement.spacedBy(Dimens.dp16)
            ) {
                item {
                    MonthSelector(
                        selectedMonth = uiState.month,
                        canGoPrevious = uiState.canGoPrevious,
                        canGoNext = uiState.canGoNext,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth
                    )
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Monthly spending limits renew automatically each month.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.showNotificationHint) {
                    item {
                        AppCard(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.cardPadding),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(Modifier.width(Dimens.dp12))
                                Text(
                                    text = "Turn on notifications to receive 80% and 100% budget alerts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(Dimens.dp8))
                                Button(
                                    onClick = onNotificationSettingsClick,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                                ) {
                                    Text("Enable")
                                }
                            }
                        }
                    }
                }

                // Overall Monthly Budget Spotlight Card
                item {
                    AppCard(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Overall Monthly Budget",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.overall != null) {
                                    TextButton(
                                        onClick = {
                                            editingBudget = BudgetSheetTarget.Edit(
                                                budgetKey = Budget.TOTAL_BUDGET_KEY,
                                                label = "Overall",
                                                emoji = "",
                                                currentLimitPaise = uiState.overall.status.limitPaise
                                            )
                                        },
                                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                }
                            }

                            Spacer(Modifier.height(Dimens.dp8))

                            if (uiState.overall != null) {
                                BudgetRowContent(
                                    row = uiState.overall,
                                    isCurrentMonth = uiState.month == YearMonth.from(LocalDate.now()),
                                    isOverallHero = true,
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "No overall limit set",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Track total monthly UPI spend against a target cap.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(Dimens.dp12))
                                    Button(
                                        onClick = { editingBudget = BudgetSheetTarget.AddOverall },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                                    ) {
                                        Text("Set budget")
                                    }
                                }
                            }
                        }
                    }
                }

                // Category Budgets Header
                item {
                    SectionHeader(
                        title = "Category Budgets",
                        action = {
                            OutlinedButton(
                                onClick = {
                                    val existingKeys = uiState.allBudgets.map { it.budgetKey }.toSet()
                                    val availableCats = Category.entries.filter {
                                        it != Category.INCOME && it != Category.UNCATEGORIZED && it.label !in existingKeys
                                    }
                                    editingBudget = BudgetSheetTarget.AddCategory(availableCats)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(Dimens.dp4))
                                Text("Add limit")
                            }
                        }
                    )
                }

                if (uiState.categoryRows.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Star,
                            title = "No category budgets yet",
                            hint = "Create limits for Dining, Groceries, Shopping, or Travel to stay ahead of overspending."
                        )
                    }
                } else {
                    items(uiState.categoryRows, key = { it.budgetKey }) { row ->
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                                BudgetRowContent(
                                    row = row,
                                    isCurrentMonth = uiState.month == YearMonth.from(LocalDate.now()),
                                    isOverallHero = false,
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
    modifier: Modifier = Modifier,
    isOverallHero: Boolean = false
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
    val isOverall = row.budgetKey == Budget.TOTAL_BUDGET_KEY

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.minTouchTarget)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (!isOverall) {
                    CategoryIconBadge(
                        category = category,
                        size = 36.dp,
                        iconSize = 18.dp
                    )
                    Spacer(Modifier.width(Dimens.dp12))
                }
                Column {
                    if (!isOverall) {
                        Text(
                            row.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${CurrencyUtils.formatRupees(status.spentPaise)} of ${CurrencyUtils.formatRupees(status.limitPaise)}",
                        style = if (isOverallHero) MoneyTypography.cardAmount else MaterialTheme.typography.bodySmall,
                        color = if (isOverallHero) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            StatusPill(
                label = statusWord,
                icon = statusIcon,
                color = levelColor
            )
        }

        Spacer(Modifier.height(Dimens.dp12))

        val fraction = if (status.limitPaise > 0L) {
            (status.spentPaise.toFloat() / status.limitPaise.toFloat()).coerceIn(0f, 1f)
        } else 0f

        ThemedLinearProgress(
            fraction = fraction,
            percent = status.percent,
            color = levelColor
        )

        Spacer(Modifier.height(Dimens.dp8))

        val subText = if (isCurrentMonth) {
            if (status.remainingPaise >= 0L) {
                val perDay = status.perDayLeftPaise?.let { " • ${CurrencyUtils.formatRupees(it)}/day for ${status.daysLeft}d" } ?: ""
                "${CurrencyUtils.formatRupees(status.remainingPaise)} remaining$perDay"
            } else {
                "Over budget by ${CurrencyUtils.formatRupees(-status.remainingPaise)}"
            }
        } else {
            if (status.remainingPaise >= 0L) {
                "${CurrencyUtils.formatRupees(status.remainingPaise)} remaining"
            } else {
                "Over budget by ${CurrencyUtils.formatRupees(-status.remainingPaise)}"
            }
        }

        Text(
            text = subText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (status.remainingPaise < 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                fontWeight = FontWeight.Bold
            )

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
                        style = MaterialTheme.typography.labelLarge,
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
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Monthly limit") },
                prefix = { Text("₹ ") },
                placeholder = { Text("e.g. 5000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
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
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    ) {
                        Text("Delete")
                    }
                } else {
                    Spacer(Modifier.width(Dimens.dp4))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                    ) {
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
                        enabled = isValid && (target !is BudgetSheetTarget.AddCategory || selectedCategory != null),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
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
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
