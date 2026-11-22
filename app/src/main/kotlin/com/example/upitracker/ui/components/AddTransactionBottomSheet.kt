package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.domain.budget.BudgetLogic
import com.example.upitracker.domain.budget.MAX_BUDGET_PAISE
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionBottomSheet(
    onDismiss: () -> Unit,
    onSave: (
        amountPaise: Long,
        type: TransactionType,
        merchant: String,
        category: Category,
        note: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val creditColor = creditAmountColor()

    var txType by rememberSaveable { mutableStateOf(TransactionType.DEBIT) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var merchantText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(Category.FOOD_AND_DINING) }
    var noteText by rememberSaveable { mutableStateOf("") }

    val parsedPaise = BudgetLogic.parseRupeesToPaise(amountText)
    val isAmountValid = parsedPaise != null && parsedPaise in 1L..MAX_BUDGET_PAISE
    val isMerchantValid = merchantText.trim().isNotEmpty()
    val canSave = isAmountValid && isMerchantValid

    val availableCategories = if (txType == TransactionType.CREDIT) {
        listOf(Category.INCOME, Category.TRANSFERS, Category.UNCATEGORIZED)
    } else {
        Category.entries.filter { it != Category.INCOME }
    }

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
                text = "Add Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp12)
            ) {
                FilterChip(
                    selected = txType == TransactionType.DEBIT,
                    onClick = {
                        txType = TransactionType.DEBIT
                        if (selectedCategory == Category.INCOME) {
                            selectedCategory = Category.FOOD_AND_DINING
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Expense",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = Dimens.minTouchTarget)
                        .testTag("add_tx_type_expense")
                )
                FilterChip(
                    selected = txType == TransactionType.CREDIT,
                    onClick = {
                        txType = TransactionType.CREDIT
                        selectedCategory = Category.INCOME
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Income",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = creditColor.copy(alpha = 0.16f),
                        selectedLabelColor = creditColor,
                        selectedLeadingIconColor = creditColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = Dimens.minTouchTarget)
                        .testTag("add_tx_type_income")
                )
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                prefix = { Text("₹ ") },
                placeholder = { Text("e.g. 250") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                isError = amountText.isNotBlank() && !isAmountValid,
                supportingText = {
                    if (parsedPaise != null && isAmountValid) {
                        Text(CurrencyUtils.formatRupees(parsedPaise))
                    } else if (amountText.isNotBlank()) {
                        Text(
                            text = "Enter an amount between ₹1 and ₹1,00,00,000",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_tx_amount_input")
            )

            OutlinedTextField(
                value = merchantText,
                onValueChange = { merchantText = it },
                label = {
                    Text(if (txType == TransactionType.DEBIT) "Paid to (merchant)" else "Received from (source)")
                },
                placeholder = {
                    Text(if (txType == TransactionType.DEBIT) "e.g. Swiggy, Metro, Cafe" else "e.g. Salary, Refund, Freelance")
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_tx_merchant_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            leadingIcon = { CategoryColorDot(cat) },
                            label = {
                                Text(
                                    text = cat.label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
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

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (optional)") },
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp12)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = Dimens.minTouchTarget)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (parsedPaise != null && canSave) {
                            onSave(
                                parsedPaise,
                                txType,
                                merchantText.trim(),
                                selectedCategory,
                                noteText.takeIf { it.isNotBlank() }
                            )
                        }
                    },
                    enabled = canSave,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = Dimens.minTouchTarget)
                        .testTag("add_tx_save_button")
                ) {
                    Text("Save")
                }
            }
        }
    }
}
