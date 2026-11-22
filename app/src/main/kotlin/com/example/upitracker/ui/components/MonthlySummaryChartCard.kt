package com.example.upitracker.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upitracker.domain.summary.MonthlySummary
import com.example.upitracker.ui.insights.ChartCalculations
import com.example.upitracker.ui.insights.ChartColors
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.ui.theme.MoneyTypography
import com.example.upitracker.util.CurrencyUtils
import com.example.upitracker.util.DateTimeUtils

enum class MonthlyChartMode(val label: String) {
    BAR("Bar"),
    DONUT("Split")
}

@Composable
fun MonthlySummaryChartCard(
    summary: MonthlySummary,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val incomeColor = if (isDark) ChartColors.MONTHLY_RECEIVED_DARK else ChartColors.MONTHLY_RECEIVED_LIGHT
    val expenseColor = if (isDark) ChartColors.MONTHLY_SPENT_DARK else ChartColors.MONTHLY_SPENT_LIGHT
    val creditTextColor = creditAmountColor()

    var chartMode by rememberSaveable { mutableStateOf(MonthlyChartMode.BAR) }
    var selectedBarIndex by rememberSaveable(summary.month.toString()) { mutableStateOf<Int?>(null) }

    val monthLabel = DateTimeUtils.formatMonthYear(summary.month)
    val semanticsDesc = buildString {
        append("Current month summary for $monthLabel. ")
        append("Total income ${CurrencyUtils.formatRupees(summary.totalIncomePaise)} across ${summary.incomeCount} credits. ")
        append("Total expenses ${CurrencyUtils.formatRupees(summary.totalExpensePaise)} across ${summary.expenseCount} debits. ")
        append("Net cashflow ${CurrencyUtils.formatRupees(summary.netPaise)}.")
    }

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_summary_card")
            .semantics { contentDescription = semanticsDesc },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Cashflow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(Dimens.dp8))

                Row(
                    modifier = Modifier.testTag("monthly_summary_mode_toggle"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonthlyChartMode.entries.forEach { mode ->
                        FilterChip(
                            selected = chartMode == mode,
                            onClick = { chartMode = mode },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            label = {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.dp12))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp12)
            ) {
                SummaryStatTile(
                    label = "Income",
                    amountPaise = summary.totalIncomePaise,
                    countText = "${summary.incomeCount} credit${if (summary.incomeCount != 1) "s" else ""}",
                    accentColor = incomeColor,
                    valueColor = creditTextColor,
                    isIncome = true,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatTile(
                    label = "Expenses",
                    amountPaise = summary.totalExpensePaise,
                    countText = "${summary.expenseCount} debit${if (summary.expenseCount != 1) "s" else ""}",
                    accentColor = expenseColor,
                    valueColor = MaterialTheme.colorScheme.error,
                    isIncome = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Dimens.dp16))

            if (!summary.hasActivity) {
                EmptyMonthlyChartPlaceholder(
                    monthLabel = monthLabel,
                    ringColor = MaterialTheme.colorScheme.outlineVariant
                )
            } else {
                when (chartMode) {
                    MonthlyChartMode.BAR -> {
                        IncomeVsExpenseBarChart(
                            summary = summary,
                            incomeColor = incomeColor,
                            expenseColor = expenseColor,
                            selectedBarIndex = selectedBarIndex,
                            onSelectBar = { idx ->
                                selectedBarIndex = if (selectedBarIndex == idx) null else idx
                            }
                        )
                    }
                    MonthlyChartMode.DONUT -> {
                        IncomeVsExpenseDonutChart(
                            summary = summary,
                            incomeColor = incomeColor,
                            expenseColor = expenseColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.dp12))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(Modifier.height(Dimens.dp12))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Net balance change",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    val netPrefix = if (summary.netPaise > 0L) "+" else ""
                    Text(
                        text = "$netPrefix${CurrencyUtils.formatRupees(summary.netPaise)}",
                        style = MoneyTypography.cardAmount,
                        color = if (summary.netPaise >= 0L) creditTextColor else MaterialTheme.colorScheme.error
                    )
                }

                val savingsRate = summary.savingsRatePercent
                val badgeColor = when {
                    savingsRate == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    savingsRate >= 0 -> creditTextColor
                    else -> MaterialTheme.colorScheme.error
                }
                val badgeIcon = when {
                    savingsRate == null -> Icons.Default.Info
                    savingsRate >= 0 -> Icons.Default.CheckCircle
                    else -> Icons.Default.Warning
                }
                val badgeText = when {
                    savingsRate == null && summary.totalExpensePaise > 0L -> "No income logged"
                    savingsRate == null -> "0% cashflow"
                    savingsRate >= 0 -> "Saved $savingsRate%"
                    else -> "Overspent ${-savingsRate}%"
                }

                StatusPill(
                    label = badgeText,
                    icon = badgeIcon,
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
private fun SummaryStatTile(
    label: String,
    amountPaise: Long,
    countText: String,
    accentColor: Color,
    valueColor: Color,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.cardInnerCornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Column(modifier = Modifier.padding(Dimens.dp12)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = CurrencyUtils.formatRupees(amountPaise),
                style = MoneyTypography.cardAmount.copy(fontSize = 17.sp),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = countText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncomeVsExpenseBarChart(
    summary: MonthlySummary,
    incomeColor: Color,
    expenseColor: Color,
    selectedBarIndex: Int?,
    onSelectBar: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val maxVal = maxOf(summary.totalIncomePaise, summary.totalExpensePaise)
    val axisMaxPaise = ChartCalculations.niceCeiling(maxVal)

    val targetIncomeFraction = if (axisMaxPaise > 0L) {
        (summary.totalIncomePaise.toFloat() / axisMaxPaise.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val targetExpenseFraction = if (axisMaxPaise > 0L) {
        (summary.totalExpensePaise.toFloat() / axisMaxPaise.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animIncomeFraction by animateFloatAsState(
        targetValue = targetIncomeFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "incomeBarFraction"
    )
    val animExpenseFraction by animateFloatAsState(
        targetValue = targetExpenseFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "expenseBarFraction"
    )

    val selectedReadout = when (selectedBarIndex) {
        0 -> "Income: ${CurrencyUtils.formatRupees(summary.totalIncomePaise)} (${summary.incomeSharePercent}% of cashflow)"
        1 -> "Expenses: ${CurrencyUtils.formatRupees(summary.totalExpensePaise)} (${summary.expenseSharePercent}% of cashflow)"
        else -> "Tap a bar to inspect share"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = selectedReadout,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selectedBarIndex != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selectedBarIndex != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Dimens.dp12))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(52.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = ChartCalculations.formatRupeesCompact(axisMaxPaise),
                    style = MoneyTypography.captionNumber,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = ChartCalculations.formatRupeesCompact(axisMaxPaise / 2),
                    style = MoneyTypography.captionNumber,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = ChartCalculations.formatRupeesCompact(0L),
                    style = MoneyTypography.captionNumber,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(Dimens.dp8))

            val outlineColor = MaterialTheme.colorScheme.outlineVariant
            val selectionStroke = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("monthly_summary_chart_canvas")
                    .pointerInput(summary.month) {
                        detectTapGestures { offset ->
                            val tapped = ChartCalculations.barIndexAt(offset.x, size.width.toFloat(), 2)
                            if (tapped != null) {
                                onSelectBar(tapped)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                val gridColor = outlineColor.copy(alpha = 0.55f)
                drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, h), Offset(w, h), strokeWidth = 1.5f)

                val slotWidth = w / 2f
                val barWidth = (slotWidth * 0.42f).coerceAtLeast(24.dp.toPx()).coerceAtMost(72.dp.toPx())
                val cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())

                val fractions = listOf(animIncomeFraction, animExpenseFraction)
                val colors = listOf(incomeColor, expenseColor)

                fractions.forEachIndexed { index, frac ->
                    val centerX = index * slotWidth + slotWidth / 2f
                    val barLeft = centerX - barWidth / 2f
                    val rawHeight = frac * h
                    val barHeight = if (rawHeight > 0f) rawHeight.coerceAtLeast(6.dp.toPx()) else 0f
                    val barTop = h - barHeight
                    val baseColor = colors[index]

                    if (barHeight > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(baseColor, baseColor.copy(alpha = 0.72f)),
                                startY = barTop,
                                endY = h
                            ),
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = cornerRadius
                        )
                    } else {
                        drawLine(
                            color = baseColor.copy(alpha = 0.45f),
                            start = Offset(barLeft, h - 2.dp.toPx()),
                            end = Offset(barLeft + barWidth, h - 2.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    if (selectedBarIndex == index) {
                        val highlightTop = if (barHeight > 0f) (barTop - 2.dp.toPx()).coerceAtLeast(0f) else h - 8.dp.toPx()
                        val highlightHeight = (h - highlightTop).coerceAtLeast(8.dp.toPx())
                        drawRoundRect(
                            color = selectionStroke,
                            topLeft = Offset(barLeft - 3.dp.toPx(), highlightTop),
                            size = Size(barWidth + 6.dp.toPx(), highlightHeight),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 60.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Income (${summary.incomeSharePercent}%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selectedBarIndex == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedBarIndex == 0) incomeColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Expenses (${summary.expenseSharePercent}%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selectedBarIndex == 1) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedBarIndex == 1) expenseColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IncomeVsExpenseDonutChart(
    summary: MonthlySummary,
    incomeColor: Color,
    expenseColor: Color
) {
    val animIncomeShare by animateFloatAsState(
        targetValue = summary.incomeShareFraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "incomeDonutSweep"
    )
    val animExpenseShare by animateFloatAsState(
        targetValue = summary.expenseShareFraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "expenseDonutSweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.dp4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp16)
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .testTag("monthly_summary_chart_canvas"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                val strokeWidth = 20.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val arcOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                val incomeSweep = animIncomeShare * 360f
                val expenseSweep = animExpenseShare * 360f

                if (incomeSweep > 0f) {
                    drawArc(
                        color = incomeColor,
                        startAngle = -90f,
                        sweepAngle = incomeSweep,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                }

                if (expenseSweep > 0f) {
                    drawArc(
                        color = expenseColor,
                        startAngle = -90f + incomeSweep,
                        sweepAngle = expenseSweep,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Volume",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = ChartCalculations.formatRupeesCompact(summary.totalIncomePaise + summary.totalExpensePaise),
                    style = MoneyTypography.rowAmount
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp12)
        ) {
            DonutLegendItem(
                label = "Income",
                percent = summary.incomeSharePercent,
                amountText = ChartCalculations.formatRupeesCompact(summary.totalIncomePaise),
                color = incomeColor
            )
            DonutLegendItem(
                label = "Expenses",
                percent = summary.expenseSharePercent,
                amountText = ChartCalculations.formatRupeesCompact(summary.totalExpensePaise),
                color = expenseColor
            )
        }
    }
}

@Composable
private fun DonutLegendItem(
    label: String,
    percent: Int,
    amountText: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendSwatch(color = color)
            Spacer(Modifier.width(Dimens.dp8))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$percent% of total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = amountText,
            style = MoneyTypography.rowAmount
        )
    }
}

@Composable
private fun EmptyMonthlyChartPlaceholder(
    monthLabel: String,
    ringColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_summary_chart_canvas")
            .padding(vertical = Dimens.dp12),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(92.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                drawArc(
                    color = ringColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )
                )
            }
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(Dimens.dp8))
        Text(
            text = "No transactions recorded for $monthLabel yet",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Sync bank SMS or add an entry to chart income vs. expenses.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}
