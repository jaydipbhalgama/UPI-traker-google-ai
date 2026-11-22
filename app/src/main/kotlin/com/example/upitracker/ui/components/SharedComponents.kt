package com.example.upitracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.domain.budget.BudgetLevel
import com.example.upitracker.ui.insights.ChartColors
import com.example.upitracker.ui.theme.CreditColorDark
import com.example.upitracker.ui.theme.CreditColorLight
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.ui.theme.NearLimitDark
import com.example.upitracker.ui.theme.NearLimitLight
import com.example.upitracker.util.CurrencyUtils
import com.example.upitracker.util.DateTimeUtils

@Composable
fun creditAmountColor(): Color {
    return if (isSystemInDarkTheme()) CreditColorDark else CreditColorLight
}

@Composable
fun budgetLevelColor(level: BudgetLevel): Color {
    val scheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    return when (level) {
        BudgetLevel.OK -> scheme.primary
        BudgetLevel.NEAR -> if (isDark) NearLimitDark else NearLimitLight
        BudgetLevel.OVER -> scheme.error
    }
}

@Composable
fun CategoryColorDot(
    category: Category,
    modifier: Modifier = Modifier
) {
    val color = ChartColors.colorForCategoryOrdinal(category.ordinal, isSystemInDarkTheme())
    Box(
        modifier = modifier
            .size(Dimens.categoryDot)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun LegendSwatch(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(Dimens.legendSwatch)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardTonalElevation),
        content = content
    )
}

@Composable
fun ThemedLinearProgress(
    fraction: Float,
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(Dimens.dp4))
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDebit = transaction.type == TransactionType.DEBIT
    val creditColor = creditAmountColor()
    val amountColor = if (isDebit) MaterialTheme.colorScheme.error else creditColor

    val typeWord = if (isDebit) "Debit" else "Credit"
    val typeSign = if (isDebit) "-" else "+"
    val formattedRupees = CurrencyUtils.formatRupees(transaction.amountPaise)
    val category = Category.fromLabel(transaction.category)
    val categoryLabel = category.label

    val dateStr = DateTimeUtils.formatDateTime(transaction.timestamp)
    val speechRupees = (transaction.amountPaise / 100).toString()
    val accessibilityLabel = "$typeWord, $speechRupees rupees, ${transaction.merchant}, $categoryLabel, $dateStr"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.transactionRowMinHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardTonalElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimens.transactionRowMinHeight)
                .padding(Dimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryColorDot(category = category)
            Spacer(Modifier.width(Dimens.dp12))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.bodyLarge
                )
                val secondary = buildString {
                    append(dateStr)
                    append(" • ")
                    append(categoryLabel)
                    transaction.note?.let { note ->
                        append(" • ")
                        append(note)
                    }
                }
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(Dimens.dp8))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$typeSign$formattedRupees",
                    style = MaterialTheme.typography.bodyLarge,
                    color = amountColor,
                    textAlign = TextAlign.End
                )
                Text(
                    text = typeWord,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        action?.invoke()
    }
}

@Composable
fun EmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emoji: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardTonalElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.dp24),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (emoji != null) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(Dimens.dp8))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.dp24)
                )
                Spacer(Modifier.height(Dimens.dp8))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Dimens.dp8))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onActionClick != null) {
                Spacer(Modifier.height(Dimens.dp16))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                ) {
                    Text(actionText)
                }
            }
        }
    }
}
