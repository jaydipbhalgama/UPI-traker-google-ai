package com.example.upitracker.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.category.AutoCategorizer
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.ui.components.CategoryColorDot
import com.example.upitracker.ui.components.creditAmountColor
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.ui.theme.MoneyTypography
import com.example.upitracker.util.CurrencyUtils
import com.example.upitracker.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditBottomSheet(
    transaction: Transaction,
    merchantCount: Int,
    onDismiss: () -> Unit,
    onSave: (newCategory: Category, newNote: String?, applyToAll: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isDebit = transaction.type == TransactionType.DEBIT
    val creditColor = creditAmountColor()
    val amountColor = if (isDebit) MaterialTheme.colorScheme.error else creditColor
    val merchantKey = AutoCategorizer.merchantKey(transaction.merchant)
    val isValidMerchant = merchantKey.isNotBlank() && merchantKey != "unknown"

    var selectedCategory by rememberSaveable {
        mutableStateOf(Category.fromLabel(transaction.category))
    }
    var noteText by rememberSaveable { mutableStateOf(transaction.note ?: "") }
    var applyToAll by rememberSaveable { mutableStateOf(isValidMerchant) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.dp24)
                .padding(bottom = Dimens.dp24)
        ) {
            Text(
                text = transaction.merchant,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            val typeSign = if (isDebit) "-" else "+"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$typeSign${CurrencyUtils.formatRupees(transaction.amountPaise)}",
                    style = MoneyTypography.cardAmount,
                    color = amountColor
                )
                Text(
                    text = "• ${if (isDebit) "Debit" else "Credit"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = DateTimeUtils.formatDateTime(transaction.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Category.entries.forEach { cat ->
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

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note (optional)") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )

            if (isValidMerchant) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Dimens.minTouchTarget)
                ) {
                    Checkbox(
                        checked = applyToAll,
                        onCheckedChange = { applyToAll = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Apply to all $merchantCount payment${if (merchantCount != 1) "s" else ""} to ${transaction.merchant}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                        onSave(
                            selectedCategory,
                            noteText.takeIf { it.isNotBlank() },
                            applyToAll && isValidMerchant
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = Dimens.minTouchTarget)
                ) {
                    Text("Save")
                }
            }
        }
    }
}
