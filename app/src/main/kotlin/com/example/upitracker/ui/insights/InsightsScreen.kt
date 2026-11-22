package com.example.upitracker.ui.insights

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.upitracker.MonthSelector
import com.example.upitracker.data.category.Category
import com.example.upitracker.ui.components.AppCard
import com.example.upitracker.ui.components.CategoryIconBadge
import com.example.upitracker.ui.components.EmptyState
import com.example.upitracker.ui.components.LegendSwatch
import com.example.upitracker.ui.components.MonthlySummaryChartCard
import com.example.upitracker.ui.components.StatusPill
import com.example.upitracker.ui.components.creditAmountColor
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.ui.theme.MoneyTypography
import com.example.upitracker.ui.theme.NearLimitDark
import com.example.upitracker.ui.theme.NearLimitLight
import com.example.upitracker.util.CurrencyUtils
import com.example.upitracker.util.DateTimeUtils
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InsightsScreen(
    state: InsightsUiState,
    initialTab: Int = 0,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is InsightsUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is InsightsUiState.Empty -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(Dimens.dp24),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.DateRange,
                    title = "No insights yet",
                    hint = "Sync bank SMS or log a transaction to unlock category breakdowns, daily trends, and monthly comparisons."
                )
            }
        }
        is InsightsUiState.Success -> {
            InsightsContent(
                state = state,
                initialTab = initialTab,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onCategoryClick = onCategoryClick,
                modifier = modifier
            )
        }
    }
}

@Composable
fun InsightsContent(
    state: InsightsUiState.Success,
    initialTab: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTab) }
    val tabs = listOf("Categories", "Daily", "Monthly")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            end = Dimens.screenPadding,
            top = Dimens.screenPadding,
            bottom = Dimens.dp24
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp16)
    ) {
        item {
            MonthSelector(
                selectedMonth = state.selectedMonth,
                canGoPrevious = state.canGoPrevious,
                canGoNext = state.canGoNext,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth
            )
        }

        item {
            ComparisonCard(
                comparison = state.comparison,
                totalSpentPaise = state.totalSpentPaise
            )
        }

        item {
            InsightsSegmentedTabs(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )
        }

        when (selectedTabIndex) {
            0 -> {
                if (state.totalSpentPaise == 0L || state.slices.isEmpty()) {
                    item { EmptySpendingCard(message = "No spending recorded this month") }
                } else {
                    item {
                        DonutChartSection(
                            slices = state.slices,
                            totalSpentPaise = state.totalSpentPaise,
                            onCategoryClick = onCategoryClick
                        )
                    }
                }
            }
            1 -> {
                if (state.totalSpentPaise == 0L) {
                    item { EmptySpendingCard(message = "No spending recorded this month") }
                } else {
                    item {
                        DailyBarChartSection(
                            dailySpend = state.dailySpend,
                            visibleDays = state.visibleDays,
                            highestDay = state.highestDay,
                            averageDailyPaise = state.averageDailyPaise,
                            month = state.selectedMonth
                        )
                    }
                }
            }
            2 -> {
                item {
                    MonthlySummaryChartCard(summary = state.currentMonthSummary)
                }
                item {
                    MonthlyBarChartSection(
                        monthly = state.monthly,
                        selectedMonth = state.selectedMonth
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsSegmentedTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = selectedTabIndex == index
                Surface(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = Dimens.minTouchTarget),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (selected) 1.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonCard(
    comparison: ComparisonResult,
    totalSpentPaise: Long,
    modifier: Modifier = Modifier
) {
    val creditColor = creditAmountColor()
    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Text(
                text = "TOTAL SPEND",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = CurrencyUtils.formatRupees(totalSpentPaise),
                style = MoneyTypography.heroAmount,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Dimens.dp12))

            val periodText = if (comparison.mode == ComparisonMode.SAME_DAYS_LAST_MONTH) {
                "same days last month"
            } else {
                "previous month"
            }

            if (comparison.pct == null) {
                StatusPill(
                    label = "No prior month baseline",
                    icon = Icons.Default.Info,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val pct = comparison.pct
                when {
                    pct > 0 -> {
                        StatusPill(
                            label = "$pct% higher vs $periodText",
                            icon = Icons.Default.KeyboardArrowUp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    pct < 0 -> {
                        val absPct = -pct
                        StatusPill(
                            label = "$absPct% lower vs $periodText",
                            icon = Icons.Default.KeyboardArrowDown,
                            color = creditColor
                        )
                    }
                    else -> {
                        StatusPill(
                            label = "Even with $periodText",
                            icon = Icons.Default.CheckCircle,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChartSection(
    slices: List<CategorySlice>,
    totalSpentPaise: Long,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val topSliceName = slices.firstOrNull()?.label ?: "None"
    val semanticsDesc = "Donut chart of spending by category. Total spent ${CurrencyUtils.formatRupees(totalSpentPaise)}. Top category: $topSliceName."

    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "donutSweep"
    )

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${slices.size} group${if (slices.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(Dimens.dp12))

            Box(
                modifier = Modifier
                    .size(224.dp)
                    .semantics { contentDescription = semanticsDesc },
                contentAlignment = Alignment.Center
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    val strokeWidthPx = 30.dp.toPx()
                    val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                    val arcOffset = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx)
                    )

                    if (slices.size == 1) {
                        drawArc(
                            color = ChartColors.getCategorySliceColor(0, slices[0].isMerged, isDark),
                            startAngle = -90f,
                            sweepAngle = 360f * sweepProgress,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx)
                        )
                    } else {
                        var startAngle = -90f
                        slices.forEachIndexed { index, slice ->
                            val sweep = slice.fraction * 360f * sweepProgress
                            drawArc(
                                color = ChartColors.getCategorySliceColor(index, slice.isMerged, isDark),
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = arcOffset,
                                size = arcSize,
                                style = Stroke(width = strokeWidthPx)
                            )
                            startAngle += sweep
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = ChartCalculations.formatRupeesCompact(totalSpentPaise),
                        style = MoneyTypography.cardAmount,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(Dimens.dp12))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            Spacer(Modifier.height(Dimens.dp8))

            Column(modifier = Modifier.fillMaxWidth()) {
                slices.forEachIndexed { index, slice ->
                    val color = ChartColors.getCategorySliceColor(index, slice.isMerged, isDark)
                    val isClickable = slice.category != null
                    val clickableModifier = if (slice.category != null) {
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCategoryClick(slice.category) }
                    } else {
                        Modifier
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .then(clickableModifier)
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (slice.category != null) {
                                CategoryIconBadge(
                                    category = slice.category,
                                    color = color,
                                    size = 38.dp,
                                    iconSize = 18.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column {
                                val labelText = if (slice.isMerged) {
                                    "${slice.label} (${slice.mergedCategoryCount})"
                                } else {
                                    slice.label
                                }
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${slice.count} payment${if (slice.count != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyUtils.formatRupees(slice.totalPaise),
                                    style = MoneyTypography.rowAmount
                                )
                                Text(
                                    text = slice.percentDisplay,
                                    style = MoneyTypography.captionNumber,
                                    color = color
                                )
                            }
                            if (isClickable) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyBarChartSection(
    dailySpend: LongArray,
    visibleDays: Int,
    highestDay: Int?,
    averageDailyPaise: Long,
    month: YearMonth,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val avgLineColor = if (isDark) NearLimitDark else NearLimitLight
    var selectedDayIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(month) {
        selectedDayIndex = null
    }

    val maxDayPaise = (0 until visibleDays).maxOfOrNull { dailySpend[it] } ?: 0L
    val axisMaxPaise = ChartCalculations.niceCeiling(maxDayPaise)

    val highestDayLabel = highestDay?.let { day ->
        val dateStr = "$day ${month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
        "Peak: $dateStr (${CurrencyUtils.formatRupees(dailySpend[day - 1])})"
    }

    val selectedDayText = selectedDayIndex?.let { idx ->
        val day = idx + 1
        val dateStr = "$day ${month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
        val amt = dailySpend[idx]
        if (amt > 0L) {
            "$dateStr • ${CurrencyUtils.formatRupees(amt)}"
        } else {
            "$dateStr • No spending"
        }
    }

    val semanticsDesc = "Daily spending bar chart for ${DateTimeUtils.formatMonthYear(month)}. " +
        (highestDayLabel ?: "No spending recorded.")

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(Dimens.cardPadding)
                .semantics { contentDescription = semanticsDesc }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Spend Velocity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (highestDayLabel != null) {
                    Text(
                        text = highestDayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) ChartColors.DAILY_HIGHEST_DARK else ChartColors.DAILY_HIGHEST_LIGHT,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(Dimens.dp8))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selectedDayText != null) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                }
            ) {
                Text(
                    text = selectedDayText ?: "Tap any day bar to inspect exact total",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedDayText != null) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedDayText != null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(Dimens.dp16))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(52.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        ChartCalculations.formatRupeesCompact(axisMaxPaise),
                        style = MoneyTypography.captionNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        ChartCalculations.formatRupeesCompact(axisMaxPaise / 2),
                        style = MoneyTypography.captionNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        ChartCalculations.formatRupeesCompact(0L),
                        style = MoneyTypography.captionNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(Dimens.dp8))

                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                val selectedOutline = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(visibleDays, month) {
                            detectTapGestures { offset ->
                                val clickedIndex = ChartCalculations.barIndexAt(offset.x, size.width.toFloat(), visibleDays)
                                selectedDayIndex = if (selectedDayIndex == clickedIndex) null else clickedIndex
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    val gridColor = outlineColor.copy(alpha = 0.55f)
                    drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h), Offset(w, h), strokeWidth = 1.5f)

                    if (averageDailyPaise > 0L && averageDailyPaise <= axisMaxPaise) {
                        val avgY = h - (averageDailyPaise.toFloat() / axisMaxPaise.toFloat()) * h
                        drawLine(
                            color = avgLineColor,
                            start = Offset(0f, avgY),
                            end = Offset(w, avgY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    if (visibleDays > 0) {
                        val slotWidth = w / visibleDays
                        val barPadding = (slotWidth * 0.22f).coerceAtLeast(1f)
                        val barWidth = (slotWidth - barPadding * 2).coerceAtLeast(2f)
                        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

                        for (i in 0 until visibleDays) {
                            val amount = dailySpend[i]
                            val barHeight = (amount.toFloat() / axisMaxPaise.toFloat()) * h
                            val barLeft = i * slotWidth + barPadding
                            val barTop = h - barHeight

                            val isHighest = (highestDay != null && (i + 1) == highestDay)
                            val barColor = if (isHighest) {
                                if (isDark) ChartColors.DAILY_HIGHEST_DARK else ChartColors.DAILY_HIGHEST_LIGHT
                            } else {
                                if (isDark) ChartColors.DAILY_BAR_DARK else ChartColors.DAILY_BAR_LIGHT
                            }

                            if (barHeight > 0f) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(barColor, barColor.copy(alpha = 0.72f)),
                                        startY = barTop,
                                        endY = h
                                    ),
                                    topLeft = Offset(barLeft, barTop),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = radius
                                )
                            }

                            if (selectedDayIndex == i) {
                                val topY = (h - barHeight).coerceAtLeast(0f)
                                drawRoundRect(
                                    color = selectedOutline,
                                    topLeft = Offset(barLeft - 1.5f, topY - 1.5f),
                                    size = Size(barWidth + 3f, barHeight + 2f),
                                    cornerRadius = radius,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = 60.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val labelDays = listOf(1, 5, 10, 15, 20, 25, visibleDays)
                        .filter { it <= visibleDays }
                        .distinct()
                    val filteredLabels = if (labelDays.size >= 2 && (visibleDays - labelDays[labelDays.size - 2]) < 3) {
                        labelDays.dropLast(2) + visibleDays
                    } else {
                        labelDays
                    }

                    filteredLabels.forEach { day ->
                        val fraction = (day - 0.5f) / visibleDays.toFloat()
                        Row(
                            modifier = Modifier.fillMaxWidth(fraction),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "$day",
                                style = MoneyTypography.captionNumber,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (averageDailyPaise > 0L) {
                Spacer(Modifier.height(Dimens.dp12))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Spacer(Modifier.height(Dimens.dp12))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(avgLineColor)
                    )
                    Text(
                        text = "Daily average: ${CurrencyUtils.formatRupees(averageDailyPaise)} / day",
                        style = MaterialTheme.typography.labelMedium,
                        color = avgLineColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChartSection(
    monthly: List<MonthTotals>,
    selectedMonth: YearMonth,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val spentColor = if (isDark) ChartColors.MONTHLY_SPENT_DARK else ChartColors.MONTHLY_SPENT_LIGHT
    val receivedColor = if (isDark) ChartColors.MONTHLY_RECEIVED_DARK else ChartColors.MONTHLY_RECEIVED_LIGHT
    var selectedGroupIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val maxVal = monthly.maxOfOrNull { maxOf(it.spentPaise, it.receivedPaise) } ?: 0L
    val axisMaxPaise = ChartCalculations.niceCeiling(maxVal)

    val spansMultipleYears = monthly.map { it.month.year }.distinct().size > 1
    val monthPattern = if (spansMultipleYears) "MMM yy" else "MMM"
    val monthFormatter = DateTimeFormatter.ofPattern(monthPattern, Locale.forLanguageTag("en-IN"))

    val selectedGroupText = selectedGroupIndex?.let { idx ->
        if (idx in monthly.indices) {
            val item = monthly[idx]
            val mName = item.month.format(monthFormatter)
            "$mName: Spent ${CurrencyUtils.formatRupees(item.spentPaise)} • Received ${CurrencyUtils.formatRupees(item.receivedPaise)}"
        } else null
    }

    val semanticsDesc = "Monthly totals bar chart showing ${monthly.size} months. " + (selectedGroupText ?: "")

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(Dimens.cardPadding)
                .semantics { contentDescription = semanticsDesc }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "6-Month Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LegendSwatch(color = spentColor)
                        Spacer(Modifier.width(6.dp))
                        Text("Spent", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LegendSwatch(color = receivedColor)
                        Spacer(Modifier.width(6.dp))
                        Text("Received", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(Dimens.dp8))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selectedGroupText != null) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                }
            ) {
                Text(
                    text = selectedGroupText ?: "Tap any month pair to inspect totals",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedGroupText != null) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedGroupText != null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(Dimens.dp16))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(52.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        ChartCalculations.formatRupeesCompact(axisMaxPaise),
                        style = MoneyTypography.captionNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        ChartCalculations.formatRupeesCompact(axisMaxPaise / 2),
                        style = MoneyTypography.captionNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        ChartCalculations.formatRupeesCompact(0L),
                        style = MoneyTypography.captionNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(Dimens.dp8))

                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                val selectedOutline = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(monthly.size) {
                            detectTapGestures { offset ->
                                val clicked = ChartCalculations.barIndexAt(offset.x, size.width.toFloat(), monthly.size)
                                selectedGroupIndex = if (selectedGroupIndex == clicked) null else clicked
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    val gridColor = outlineColor.copy(alpha = 0.55f)
                    drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h), Offset(w, h), strokeWidth = 1.5f)

                    if (monthly.isNotEmpty()) {
                        val groupWidth = w / monthly.size
                        val barWidth = (groupWidth * 0.34f).coerceAtLeast(4f).coerceAtMost(24.dp.toPx())
                        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

                        monthly.forEachIndexed { i, item ->
                            val groupCenterX = i * groupWidth + groupWidth / 2f
                            val spentH = (item.spentPaise.toFloat() / axisMaxPaise.toFloat()) * h
                            val recH = (item.receivedPaise.toFloat() / axisMaxPaise.toFloat()) * h

                            val spentLeft = groupCenterX - barWidth - 2f
                            val recLeft = groupCenterX + 2f

                            if (spentH > 0f) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(spentColor, spentColor.copy(alpha = 0.72f)),
                                        startY = h - spentH,
                                        endY = h
                                    ),
                                    topLeft = Offset(spentLeft, h - spentH),
                                    size = Size(barWidth, spentH),
                                    cornerRadius = radius
                                )
                            }

                            if (recH > 0f) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(receivedColor, receivedColor.copy(alpha = 0.72f)),
                                        startY = h - recH,
                                        endY = h
                                    ),
                                    topLeft = Offset(recLeft, h - recH),
                                    size = Size(barWidth, recH),
                                    cornerRadius = radius
                                )
                            }

                            if (selectedGroupIndex == i) {
                                val groupLeft = i * groupWidth + 2f
                                val groupW = groupWidth - 4f
                                val highestH = maxOf(spentH, recH).coerceAtLeast(10f)
                                drawRoundRect(
                                    color = selectedOutline,
                                    topLeft = Offset(groupLeft, h - highestH - 4f),
                                    size = Size(groupW, highestH + 4f),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                monthly.forEach { item ->
                    val isCurrentSelected = (item.month == selectedMonth)
                    Text(
                        text = item.month.format(monthFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrentSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (monthly.size == 1) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "More months will appear automatically as your history grows.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptySpendingCard(
    message: String,
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Default.DateRange,
        title = message,
        hint = "Use the month selector above to browse other months.",
        modifier = modifier
    )
}
