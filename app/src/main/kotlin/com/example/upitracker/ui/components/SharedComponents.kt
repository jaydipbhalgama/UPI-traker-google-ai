package com.example.upitracker.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.domain.budget.BudgetLevel
import com.example.upitracker.ui.insights.ChartColors
import com.example.upitracker.ui.theme.CreditColorDark
import com.example.upitracker.ui.theme.CreditColorLight
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.ui.theme.MoneyTypography
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

fun categoryIcon(category: Category): ImageVector = when (category) {
    Category.FOOD_AND_DINING -> Icons.Default.Star
    Category.GROCERIES -> Icons.Default.ShoppingCart
    Category.TRANSPORT -> Icons.Default.LocationOn
    Category.FUEL -> Icons.Default.Place
    Category.SHOPPING -> Icons.Default.ShoppingCart
    Category.BILLS_AND_UTILITIES -> Icons.Default.Build
    Category.RECHARGE -> Icons.Default.Phone
    Category.ENTERTAINMENT -> Icons.Default.PlayArrow
    Category.HEALTH -> Icons.Default.Favorite
    Category.EDUCATION -> Icons.Default.Create
    Category.RENT_AND_HOUSING -> Icons.Default.Home
    Category.TRANSFERS -> Icons.Default.Refresh
    Category.CASH -> Icons.Default.CheckCircle
    Category.INVESTMENTS -> Icons.Default.Star
    Category.INCOME -> Icons.Default.AddCircle
    Category.OTHER -> Icons.Default.List
    Category.UNCATEGORIZED -> Icons.Default.Info
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
fun CategoryIconBadge(
    category: Category,
    modifier: Modifier = Modifier,
    color: Color = ChartColors.colorForCategoryOrdinal(category.ordinal, isSystemInDarkTheme()),
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun LegendSwatch(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(Dimens.legendSwatch)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun StatusPill(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
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
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "linearProgress"
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (animatedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(color)
                )
            }
        }
        Text(
            text = "$percent%",
            style = MoneyTypography.captionNumber,
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
    val typeIcon = if (isDebit) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
    val formattedRupees = CurrencyUtils.formatRupees(transaction.amountPaise)
    val category = Category.fromLabel(transaction.category)
    val categoryLabel = category.label

    val dateStr = DateTimeUtils.formatShortDateTime(transaction.timestamp)
    val fullDateStr = DateTimeUtils.formatDateTime(transaction.timestamp)
    val speechRupees = (transaction.amountPaise / 100).toString()
    val accessibilityLabel = "$typeWord, $speechRupees rupees, ${transaction.merchant}, $categoryLabel, $fullDateStr"

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.transactionRowMinHeight)
            .testTag("transaction_row_${transaction.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
            .clip(RoundedCornerShape(Dimens.cardCornerRadius))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimens.transactionRowMinHeight)
                .padding(horizontal = Dimens.dp16, vertical = Dimens.dp12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(category = category)

            Spacer(Modifier.width(Dimens.dp12))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                val secondary = buildString {
                    append(categoryLabel)
                    append(" • ")
                    append(dateStr)
                    transaction.bankName?.takeIf { it.isNotBlank() }?.let { bank ->
                        append(" • ")
                        append(bank)
                    }
                }
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                transaction.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(Dimens.dp12))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$typeSign$formattedRupees",
                    style = MoneyTypography.rowAmount,
                    color = amountColor,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(13.dp)
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
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.minTouchTarget),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
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
    @Suppress("UNUSED_PARAMETER") emoji: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.dp24),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon ?: Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(Dimens.dp16))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Dimens.dp8))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if ((actionText != null && onActionClick != null) ||
                (secondaryActionText != null && onSecondaryActionClick != null)
            ) {
                Spacer(Modifier.height(Dimens.dp20))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (actionText != null && onActionClick != null) {
                        Button(
                            onClick = onActionClick,
                            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(actionText)
                        }
                    }
                    if (secondaryActionText != null && onSecondaryActionClick != null) {
                        OutlinedButton(
                            onClick = onSecondaryActionClick,
                            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(secondaryActionText)
                        }
                    }
                }
            }
        }
    }
}
