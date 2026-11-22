package com.example.upitracker.ui.insights

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upitracker.MonthSelector
import com.example.upitracker.data.category.Category
import com.example.upitracker.util.CurrencyUtils
import com.example.upitracker.util.DateTimeUtils
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
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                com.example.upitracker.ui.components.EmptyState(
                    emoji = "📊",
                    title = "No transactions found",
                    hint = "Import your SMS messages or add transactions to view spending insights and charts."
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // a) Month selector row
        item {
            MonthSelector(
                selectedMonth = state.selectedMonth,
                canGoPrevious = state.canGoPrevious,
                canGoNext = state.canGoNext,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth
            )
        }

        // b) Comparison card
        item {
            ComparisonCard(
                comparison = state.comparison,
                totalSpentPaise = state.totalSpentPaise
            )
        }

        // c) TabRow
        item {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }

        // d) Tab contents / e) Empty states
        when (selectedTabIndex) {
            0 -> {
                if (state.totalSpentPaise == 0L || state.slices.isEmpty()) {
                    item {
                        EmptySpendingCard(message = "No spending in this month")
                    }
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
                    item {
                        EmptySpendingCard(message = "No spending in this month")
                    }
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
fun ComparisonCard(
    comparison: ComparisonResult,
    totalSpentPaise: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Spent ${CurrencyUtils.formatRupees(totalSpentPaise)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))

            val periodText = if (comparison.mode == ComparisonMode.SAME_DAYS_LAST_MONTH) {
                "same days last month"
            } else {
                "previous month"
            }

            if (comparison.pct == null) {
                Text(
                    text = "No spending data for last month to compare",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val pct = comparison.pct
                when {
                    pct > 0 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "▲ $pct% more than $periodText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                    pct < 0 -> {
                        val absPct = -pct
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "▼ $absPct% less than $periodText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "Same as $periodText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Donut Canvas with overlaid Text
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .semantics { contentDescription = semanticsDesc },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    val strokeWidthPx = 36.dp.toPx()
                    val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                    val arcOffset = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

                    if (slices.size == 1) {
                        drawArc(
                            color = ChartColors.getCategorySliceColor(0, slices[0].isMerged, isDark),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx)
                        )
                    } else {
                        var startAngle = -90f
                        slices.forEachIndexed { index, slice ->
                            val sweep = slice.fraction * 360f
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

                // Center overlaid text (NO drawText inside Canvas)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatRupees(totalSpentPaise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Legend rows
            Column(modifier = Modifier.fillMaxWidth()) {
                slices.forEachIndexed { index, slice ->
                    val color = ChartColors.getCategorySliceColor(index, slice.isMerged, isDark)
                    val clickableModifier = if (slice.category != null) {
                        Modifier.clickable { onCategoryClick(slice.category) }
                    } else {
                        Modifier
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .then(clickableModifier)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(slice.emoji, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(6.dp))
                            Column {
                                val labelText = if (slice.isMerged) {
                                    "${slice.label} (${slice.mergedCategoryCount})"
                                } else {
                                    slice.label
                                }
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${slice.count} payment${if (slice.count != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyUtils.formatRupees(slice.totalPaise),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = slice.percentDisplay,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
    month: java.time.YearMonth,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var selectedDayIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    // Clear selection when month changes
    LaunchedEffect(month) {
        selectedDayIndex = null
    }

    val maxDayPaise = (0 until visibleDays).maxOfOrNull { dailySpend[it] } ?: 0L
    val axisMaxPaise = ChartCalculations.niceCeiling(maxDayPaise)

    val highestDayLabel = highestDay?.let { day ->
        val dateStr = "$day ${month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
        "Highest day: $dateStr, ${CurrencyUtils.formatRupees(dailySpend[day - 1])}"
    }

    val selectedDayText = selectedDayIndex?.let { idx ->
        val day = idx + 1
        val dateStr = "$day ${month.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
        val amt = dailySpend[idx]
        if (amt > 0L) {
            "$dateStr: ${CurrencyUtils.formatRupees(amt)}"
        } else {
            "$dateStr: no spending"
        }
    }

    val semanticsDesc = "Daily spending bar chart for ${DateTimeUtils.formatMonthYear(month)}. " +
            (highestDayLabel ?: "No spending recorded.")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).semantics { contentDescription = semanticsDesc }
        ) {
            // Selected day text above chart
            Text(
                text = selectedDayText ?: "Tap a bar to see day's total",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selectedDayText != null) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedDayText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Chart area: Y-axis labels + Canvas
            Row(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                // Left axis labels (max, half, 0)
                Column(
                    modifier = Modifier.fillMaxHeight().width(52.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(ChartCalculations.formatRupeesCompact(axisMaxPaise), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    Text(ChartCalculations.formatRupeesCompact(axisMaxPaise / 2), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    Text(ChartCalculations.formatRupeesCompact(0L), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                }

                Spacer(Modifier.width(8.dp))

                // Canvas area with tap detection
                var canvasWidth by remember { mutableStateOf(0f) }

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
                    canvasWidth = size.width
                    val w = size.width
                    val h = size.height

                    // 3 horizontal grid lines (0, half, max)
                    val gridColor = Color.Gray.copy(alpha = 0.25f)
                    drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h), Offset(w, h), strokeWidth = 1f)

                    // Dashed line at averageDailyPaise
                    if (averageDailyPaise > 0L && averageDailyPaise <= axisMaxPaise) {
                        val avgY = h - (averageDailyPaise.toFloat() / axisMaxPaise.toFloat()) * h
                        drawLine(
                            color = Color(0xFFE69F00),
                            start = Offset(0f, avgY),
                            end = Offset(w, avgY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Draw bars
                    if (visibleDays > 0) {
                        val slotWidth = w / visibleDays
                        val barPadding = (slotWidth * 0.2f).coerceAtLeast(1f)
                        val barWidth = (slotWidth - barPadding * 2).coerceAtLeast(1f)

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
                                drawRect(
                                    color = barColor,
                                    topLeft = Offset(barLeft, barTop),
                                    size = Size(barWidth, barHeight)
                                )
                            }

                            // Selected outline
                            if (selectedDayIndex == i) {
                                drawRect(
                                    color = if (isDark) Color.White else Color.Black,
                                    topLeft = Offset(barLeft - 1f, (h - barHeight).coerceAtLeast(0f) - 1f),
                                    size = Size(barWidth + 2f, barHeight + 1f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            // X-axis day labels below chart (days 1, 5, 10, 15, 20, 25, visibleDays)
            Spacer(Modifier.height(4.dp))
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
                        // Position label with bias
                        Row(
                            modifier = Modifier.fillMaxWidth(fraction),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "$day",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Average legend line & Highest day text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (averageDailyPaise > 0L) {
                    Text(
                        text = "--- Average ${CurrencyUtils.formatRupees(averageDailyPaise)} / day",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE69F00),
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                if (highestDayLabel != null) {
                    Text(
                        text = highestDayLabel,
                        style = MaterialTheme.typography.labelSmall,
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
    selectedMonth: java.time.YearMonth,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
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

    val semanticsDesc = "Monthly totals bar chart showing ${monthly.size} months. " +
            (selectedGroupText ?: "")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).semantics { contentDescription = semanticsDesc }
        ) {
            // Legend row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(if (isDark) ChartColors.MONTHLY_SPENT_DARK else ChartColors.MONTHLY_SPENT_LIGHT))
                    Spacer(Modifier.width(4.dp))
                    Text("Spent", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(16.dp))
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(if (isDark) ChartColors.MONTHLY_RECEIVED_DARK else ChartColors.MONTHLY_RECEIVED_LIGHT))
                    Spacer(Modifier.width(4.dp))
                    Text("Received", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Selected group exact text
            Text(
                text = selectedGroupText ?: "Tap a month group to view details",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selectedGroupText != null) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedGroupText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // Chart area
            Row(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                // Left axis labels
                Column(
                    modifier = Modifier.fillMaxHeight().width(52.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(ChartCalculations.formatRupeesCompact(axisMaxPaise), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    Text(ChartCalculations.formatRupeesCompact(axisMaxPaise / 2), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    Text(ChartCalculations.formatRupeesCompact(0L), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                }

                Spacer(Modifier.width(8.dp))

                // Canvas
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

                    // 3 horizontal grid lines
                    val gridColor = Color.Gray.copy(alpha = 0.25f)
                    drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h), Offset(w, h), strokeWidth = 1f)

                    if (monthly.isNotEmpty()) {
                        val groupWidth = w / monthly.size
                        val barWidth = (groupWidth * 0.35f).coerceAtLeast(2f)

                        monthly.forEachIndexed { i, item ->
                            val groupCenterX = i * groupWidth + groupWidth / 2f
                            val spentH = (item.spentPaise.toFloat() / axisMaxPaise.toFloat()) * h
                            val recH = (item.receivedPaise.toFloat() / axisMaxPaise.toFloat()) * h

                            val spentLeft = groupCenterX - barWidth - 1f
                            val recLeft = groupCenterX + 1f

                            // Spent bar
                            if (spentH > 0f) {
                                drawRect(
                                    color = if (isDark) ChartColors.MONTHLY_SPENT_DARK else ChartColors.MONTHLY_SPENT_LIGHT,
                                    topLeft = Offset(spentLeft, h - spentH),
                                    size = Size(barWidth, spentH)
                                )
                            }

                            // Received bar
                            if (recH > 0f) {
                                drawRect(
                                    color = if (isDark) ChartColors.MONTHLY_RECEIVED_DARK else ChartColors.MONTHLY_RECEIVED_LIGHT,
                                    topLeft = Offset(recLeft, h - recH),
                                    size = Size(barWidth, recH)
                                )
                            }

                            // Outline if selected
                            if (selectedGroupIndex == i) {
                                val groupLeft = i * groupWidth + 2f
                                val groupW = groupWidth - 4f
                                val highestH = maxOf(spentH, recH).coerceAtLeast(10f)
                                drawRect(
                                    color = if (isDark) Color.White else Color.Black,
                                    topLeft = Offset(groupLeft, h - highestH - 4f),
                                    size = Size(groupW, highestH + 4f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            // Month labels under groups
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 60.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                monthly.forEach { item ->
                    val isCurrentSelected = (item.month == selectedMonth)
                    Text(
                        text = item.month.format(monthFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isCurrentSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isCurrentSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (monthly.size == 1) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "More months will appear as data grows",
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Use the month selector above to view other months.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
