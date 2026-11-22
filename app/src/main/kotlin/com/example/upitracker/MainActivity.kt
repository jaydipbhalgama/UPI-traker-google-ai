package com.example.upitracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Budget
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.settings.SettingsRepository
import com.example.upitracker.domain.budget.BudgetLevel
import com.example.upitracker.service.DailySummaryReceiver
import com.example.upitracker.service.DailySummaryScheduler
import com.example.upitracker.ui.TransactionEditBottomSheet
import com.example.upitracker.ui.budget.BudgetEditBottomSheet
import com.example.upitracker.ui.budget.BudgetSheetTarget
import com.example.upitracker.ui.budget.BudgetsScreen
import com.example.upitracker.ui.budget.BudgetsUiState
import com.example.upitracker.ui.budget.BudgetsViewModel
import com.example.upitracker.ui.components.AddTransactionBottomSheet
import com.example.upitracker.ui.components.AppCard
import com.example.upitracker.ui.components.CategoryIconBadge
import com.example.upitracker.ui.components.EmptyState
import com.example.upitracker.ui.components.MonthlySummaryChartCard
import com.example.upitracker.ui.components.SectionHeader
import com.example.upitracker.ui.components.StatusPill
import com.example.upitracker.ui.components.ThemedLinearProgress
import com.example.upitracker.ui.components.TransactionRow
import com.example.upitracker.ui.components.budgetLevelColor
import com.example.upitracker.ui.components.creditAmountColor
import com.example.upitracker.ui.dashboard.BudgetInfo
import com.example.upitracker.ui.dashboard.CategorySummary
import com.example.upitracker.ui.dashboard.DashboardBudgetSummary
import com.example.upitracker.ui.dashboard.DashboardUiState
import com.example.upitracker.ui.dashboard.DashboardViewModel
import com.example.upitracker.ui.dashboard.SpendPeriod
import com.example.upitracker.ui.dashboard.TopMerchantSummary
import com.example.upitracker.ui.insights.ChartColors
import com.example.upitracker.ui.insights.InsightsScreen
import com.example.upitracker.ui.insights.InsightsViewModel
import com.example.upitracker.ui.navigation.Screen
import com.example.upitracker.ui.navigation.backTarget
import com.example.upitracker.ui.settings.SettingsScreen
import com.example.upitracker.ui.sms.UnrecognisedSmsScreen
import com.example.upitracker.ui.sms.UnrecognisedSmsViewModel
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.ui.theme.MoneyTypography
import com.example.upitracker.ui.theme.UPITrackerTheme
import com.example.upitracker.ui.transactions.AllTransactionsScreen
import com.example.upitracker.ui.transactions.AllTransactionsViewModel
import com.example.upitracker.util.CsvExporter
import com.example.upitracker.util.CurrencyUtils
import com.example.upitracker.util.DateTimeUtils
import kotlinx.coroutines.launch
import java.time.YearMonth

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels {
        val app = application as UpiTrackerApplication
        DashboardViewModel.provideFactory(
            repository = app.container.transactionRepository,
            budgetRepository = app.container.budgetRepository,
            alertChecker = app.container.budgetAlertChecker,
            smsSyncManager = app.container.smsSyncManager,
            monthSelection = app.container.monthSelection,
            appContext = applicationContext
        )
    }

    private val insightsViewModel: InsightsViewModel by viewModels {
        val app = application as UpiTrackerApplication
        InsightsViewModel.provideFactory(
            repository = app.container.transactionRepository,
            monthSelection = app.container.monthSelection
        )
    }

    private val budgetsViewModel: BudgetsViewModel by viewModels {
        val app = application as UpiTrackerApplication
        BudgetsViewModel.provideFactory(
            budgetRepository = app.container.budgetRepository,
            transactionRepository = app.container.transactionRepository,
            monthSelection = app.container.monthSelection,
            alertChecker = app.container.budgetAlertChecker
        )
    }

    private val allTransactionsViewModel: AllTransactionsViewModel by viewModels {
        val app = application as UpiTrackerApplication
        AllTransactionsViewModel.provideFactory(
            repository = app.container.transactionRepository,
            monthSelection = app.container.monthSelection
        )
    }

    private val unrecognisedSmsViewModel: UnrecognisedSmsViewModel by viewModels {
        UnrecognisedSmsViewModel.provideFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UPITrackerTheme {
                UpiTrackerMainApp(
                    dashboardViewModel = dashboardViewModel,
                    insightsViewModel = insightsViewModel,
                    budgetsViewModel = budgetsViewModel,
                    allTransactionsViewModel = allTransactionsViewModel,
                    unrecognisedSmsViewModel = unrecognisedSmsViewModel
                )
            }
        }
        dashboardViewModel.runOneTimeAutoCategorizeIfNeeded()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiTrackerMainApp(
    dashboardViewModel: DashboardViewModel,
    insightsViewModel: InsightsViewModel,
    budgetsViewModel: BudgetsViewModel,
    allTransactionsViewModel: AllTransactionsViewModel,
    unrecognisedSmsViewModel: UnrecognisedSmsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val insightsUiState by insightsViewModel.uiState.collectAsStateWithLifecycle()
    val budgetsUiState by budgetsViewModel.uiState.collectAsStateWithLifecycle()
    val allTransactionsUiState by allTransactionsViewModel.uiState.collectAsStateWithLifecycle()
    val unrecognisedSmsUiState by unrecognisedSmsViewModel.uiState.collectAsStateWithLifecycle()

    val isSyncing by dashboardViewModel.isSyncing.collectAsStateWithLifecycle()
    val editingTransaction by dashboardViewModel.editingTransaction.collectAsStateWithLifecycle()
    val merchantCount by dashboardViewModel.merchantCount.collectAsStateWithLifecycle()

    var currentScreen by rememberSaveable(stateSaver = Screen.Saver) {
        mutableStateOf<Screen>(Screen.Home)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingExportList by remember { mutableStateOf<List<Transaction>?>(null) }
    var showExportConfirmDialog by remember { mutableStateOf(false) }
    var editingBudgetTarget by remember { mutableStateOf<BudgetSheetTarget?>(null) }
    var showAddTransactionSheet by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val txs = pendingExportList ?: emptyList()
            coroutineScope.launch {
                val result = CsvExporter.exportToUri(context, uri, txs)
                result.onSuccess { count ->
                    snackbarHostState.showSnackbar("Exported $count transactions")
                }.onFailure {
                    snackbarHostState.showSnackbar("Export failed. Please try another location.")
                }
                pendingExportList = null
            }
        } else {
            pendingExportList = null
        }
    }

    var hasSmsPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermissions = permissions[Manifest.permission.READ_SMS] == true &&
            permissions[Manifest.permission.RECEIVE_SMS] == true
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        budgetsViewModel.setNotificationHintVisible(!isGranted)
        if (isGranted) {
            DailySummaryReceiver.createNotificationChannel(context)
            if (SettingsRepository(context).dailySummaryEnabled) {
                DailySummaryScheduler.scheduleNext(context)
            }
        }
    }

    fun requestNotificationPermissionOrSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (status != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openAppNotificationSettings(context)
            }
        } else {
            openAppNotificationSettings(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val settingsRepo = SettingsRepository(context)
            if (settingsRepo.dailySummaryEnabled &&
                !DailySummaryReceiver.hasNotificationPermission(context) &&
                !settingsRepo.hasPromptedNotificationPermission
            ) {
                settingsRepo.hasPromptedNotificationPermission = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        dashboardViewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateTo(target: Screen) {
        if (currentScreen is Screen.Transactions && target !is Screen.Transactions) {
            allTransactionsViewModel.reset()
        }
        currentScreen = target
    }

    BackHandler(enabled = backTarget(currentScreen) != null) {
        val next = backTarget(currentScreen)
        if (next != null) {
            navigateTo(next)
        }
    }

    var showDashboardMenu by remember { mutableStateOf(false) }
    var showAllTxMenu by remember { mutableStateOf(false) }

    val isTopLevelTab = currentScreen is Screen.Home ||
        currentScreen is Screen.Insights ||
        currentScreen is Screen.Transactions

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val showBottomBar = isTopLevelTab && !isImeVisible

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        if (currentScreen is Screen.Home) {
                            Column {
                                Text(
                                    text = "UPI Tracker",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = creditAmountColor(),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "On-device ledger",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            val title = when (currentScreen) {
                                is Screen.Home -> "UPI Tracker"
                                is Screen.Insights -> "Insights"
                                is Screen.Transactions -> "Transactions"
                                is Screen.Budgets -> "Budgets"
                                is Screen.Settings -> "Settings"
                                is Screen.UnrecognisedSms -> "Unrecognised SMS"
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        val target = backTarget(currentScreen)
                        if (!isTopLevelTab && target != null) {
                            IconButton(
                                onClick = { navigateTo(target) },
                                modifier = Modifier
                                    .defaultMinSize(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
                                    .clearAndSetSemantics { contentDescription = "Back" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    actions = {
                        when (currentScreen) {
                            is Screen.Home -> {
                                IconButton(
                                    onClick = { navigateTo(Screen.Settings) },
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
                                        .clearAndSetSemantics { contentDescription = "Settings" }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null
                                    )
                                }

                                if (isSyncing) {
                                    Box(
                                        modifier = Modifier.defaultMinSize(
                                            minWidth = Dimens.minTouchTarget,
                                            minHeight = Dimens.minTouchTarget
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            if (hasSmsPermissions) dashboardViewModel.syncNow()
                                            else permissionLauncher.launch(
                                                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                                            )
                                        },
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
                                            .clearAndSetSemantics { contentDescription = "Sync bank SMS" }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null
                                        )
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { showDashboardMenu = !showDashboardMenu },
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
                                            .clearAndSetSemantics { contentDescription = "More options" }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = null
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showDashboardMenu,
                                        onDismissRequest = { showDashboardMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Re-scan SMS (last 90 days)") },
                                            onClick = {
                                                showDashboardMenu = false
                                                dashboardViewModel.rescanSms()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Unrecognised bank SMS") },
                                            onClick = {
                                                showDashboardMenu = false
                                                unrecognisedSmsViewModel.load()
                                                navigateTo(Screen.UnrecognisedSms)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Export all data (CSV)") },
                                            onClick = {
                                                showDashboardMenu = false
                                                coroutineScope.launch {
                                                    val allTxs = dashboardViewModel.getAllTransactionsForExport()
                                                    if (allTxs.isNotEmpty()) {
                                                        pendingExportList = allTxs
                                                        showExportConfirmDialog = true
                                                    } else {
                                                        snackbarHostState.showSnackbar("No transactions to export")
                                                    }
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Auto-categorize uncategorized") },
                                            onClick = {
                                                dashboardViewModel.autoCategorizeUncategorized()
                                                showDashboardMenu = false
                                            }
                                        )
                                        if (BuildConfig.DEBUG) {
                                            DropdownMenuItem(
                                                text = { Text("Add sample transaction") },
                                                onClick = {
                                                    dashboardViewModel.addSampleTransaction()
                                                    showDashboardMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            is Screen.Transactions -> {
                                Box {
                                    IconButton(
                                        onClick = { showAllTxMenu = !showAllTxMenu },
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
                                            .clearAndSetSemantics { contentDescription = "More options" }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = null
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showAllTxMenu,
                                        onDismissRequest = { showAllTxMenu = false }
                                    ) {
                                        val hasTransactions = allTransactionsUiState.filteredTransactions.isNotEmpty()
                                        DropdownMenuItem(
                                            text = { Text("Export shown list (CSV)") },
                                            enabled = hasTransactions,
                                            onClick = {
                                                showAllTxMenu = false
                                                pendingExportList = allTransactionsUiState.filteredTransactions
                                                showExportConfirmDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                            else -> { /* No actions */ }
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
        },
        floatingActionButton = {
            if ((currentScreen is Screen.Home || currentScreen is Screen.Transactions) && !isImeVisible) {
                FloatingActionButton(
                    onClick = { showAddTransactionSheet = true },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_transaction_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add transaction"
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Home,
                            onClick = { navigateTo(Screen.Home) },
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text("Home", fontWeight = FontWeight.SemiBold) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Insights,
                            onClick = { navigateTo(Screen.Insights) },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            label = { Text("Insights", fontWeight = FontWeight.SemiBold) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = currentScreen is Screen.Transactions,
                            onClick = { navigateTo(Screen.Transactions()) },
                            icon = { Icon(Icons.Default.List, contentDescription = null) },
                            label = { Text("Transactions", fontWeight = FontWeight.SemiBold) },
                            colors = navColors
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    when (val state = dashboardUiState) {
                        is DashboardUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is DashboardUiState.Success -> {
                            DashboardScreen(
                                state = state,
                                hasSmsPermissions = hasSmsPermissions,
                                onRequestPermission = {
                                    permissionLauncher.launch(
                                        arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                                    )
                                },
                                onPreviousMonth = { dashboardViewModel.previousMonth() },
                                onNextMonth = { dashboardViewModel.nextMonth() },
                                onSyncClick = { dashboardViewModel.syncNow() },
                                onAddTransactionClick = { showAddTransactionSheet = true },
                                onViewAllClick = { navigateTo(Screen.Transactions()) },
                                onOpenInsights = { navigateTo(Screen.Insights) },
                                onOpenBudgets = { navigateTo(Screen.Budgets) },
                                onSetBudgetClick = {
                                    editingBudgetTarget = BudgetSheetTarget.AddOverall
                                },
                                onEditBudgetClick = { limitPaise ->
                                    editingBudgetTarget = BudgetSheetTarget.Edit(
                                        budgetKey = Budget.TOTAL_BUDGET_KEY,
                                        label = "Overall",
                                        emoji = "",
                                        currentLimitPaise = limitPaise
                                    )
                                },
                                onPeriodSelected = { dashboardViewModel.selectPeriod(it) },
                                onCategoryClick = { cat ->
                                    allTransactionsViewModel.preset(cat, debitOnly = true)
                                    navigateTo(Screen.Transactions(category = cat, debitOnly = true))
                                },
                                onReviewUncategorized = {
                                    allTransactionsViewModel.preset(Category.UNCATEGORIZED, debitOnly = false)
                                    navigateTo(Screen.Transactions(category = Category.UNCATEGORIZED))
                                },
                                onTransactionClick = { dashboardViewModel.openEditSheet(it) }
                            )
                        }
                    }
                }
                is Screen.Transactions -> {
                    LaunchedEffect(screen.category, screen.debitOnly) {
                        if (screen.category != null || screen.debitOnly) {
                            allTransactionsViewModel.preset(screen.category, screen.debitOnly)
                        }
                    }
                    AllTransactionsScreen(
                        uiState = allTransactionsUiState,
                        onSearchQueryChange = { allTransactionsViewModel.onSearchQueryChange(it) },
                        onPreviousMonth = { allTransactionsViewModel.previousMonth() },
                        onNextMonth = { allTransactionsViewModel.nextMonth() },
                        onFilterStateChange = { allTransactionsViewModel.onFilterStateChange(it) },
                        onSetAllTime = { allTransactionsViewModel.setAllTime(it) },
                        onSetCategory = { allTransactionsViewModel.setCategory(it) },
                        onSetTypeFilter = { allTransactionsViewModel.setTypeFilter(it) },
                        onClearFilters = { allTransactionsViewModel.clearFilters() },
                        onTransactionClick = { dashboardViewModel.openEditSheet(it) }
                    )
                }
                is Screen.Insights -> {
                    InsightsScreen(
                        state = insightsUiState,
                        onPreviousMonth = { insightsViewModel.previousMonth() },
                        onNextMonth = { insightsViewModel.nextMonth() },
                        onCategoryClick = { cat ->
                            allTransactionsViewModel.preset(cat, debitOnly = true)
                            navigateTo(Screen.Transactions(category = cat, debitOnly = true))
                        }
                    )
                }
                is Screen.Budgets -> {
                    BudgetsScreen(
                        uiState = budgetsUiState,
                        onPreviousMonth = { budgetsViewModel.previousMonth() },
                        onNextMonth = { budgetsViewModel.nextMonth() },
                        onSaveBudget = { key, limitPaise ->
                            val state = budgetsUiState
                            if (state is BudgetsUiState.Success && state.allBudgets.isEmpty()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                    if (status != PackageManager.PERMISSION_GRANTED) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            }
                            budgetsViewModel.saveBudget(key, limitPaise)
                        },
                        onDeleteBudget = { key ->
                            budgetsViewModel.deleteBudget(key)
                        },
                        onNotificationSettingsClick = {
                            requestNotificationPermissionOrSettings()
                        }
                    )
                }
                is Screen.Settings -> {
                    SettingsScreen(
                        onShowMessage = { message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }
                is Screen.UnrecognisedSms -> {
                    UnrecognisedSmsScreen(
                        uiState = unrecognisedSmsUiState,
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                            )
                        }
                    )
                }
            }
        }
    }

    if (showExportConfirmDialog) {
        val count = pendingExportList?.size ?: 0
        AlertDialog(
            onDismissRequest = {
                showExportConfirmDialog = false
                pendingExportList = null
            },
            title = { Text("Export transactions?") },
            text = {
                Text("$count transactions will be saved to a CSV file you choose. It contains merchant names and amounts, so keep it private.")
            },
            confirmButton = {
                Button(onClick = {
                    showExportConfirmDialog = false
                    val defaultFileName = "upi-tracker-${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}.csv"
                    createDocumentLauncher.launch(defaultFileName)
                }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportConfirmDialog = false
                    pendingExportList = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    editingTransaction?.let { tx ->
        TransactionEditBottomSheet(
            transaction = tx,
            merchantCount = merchantCount,
            onDismiss = { dashboardViewModel.closeEditSheet() },
            onSave = { cat, note, applyToAll ->
                dashboardViewModel.saveEdit(tx, cat, note, applyToAll)
            }
        )
    }

    editingBudgetTarget?.let { target ->
        BudgetEditBottomSheet(
            target = target,
            onDismiss = { editingBudgetTarget = null },
            onSave = { key, limitPaise ->
                dashboardViewModel.saveBudget(key, limitPaise)
                editingBudgetTarget = null
            },
            onDelete = { key ->
                dashboardViewModel.deleteBudget(key)
                editingBudgetTarget = null
            }
        )
    }

    if (showAddTransactionSheet) {
        AddTransactionBottomSheet(
            onDismiss = { showAddTransactionSheet = false },
            onSave = { amountPaise, type, merchant, category, note ->
                dashboardViewModel.addManualTransaction(
                    amountPaise = amountPaise,
                    type = type,
                    merchant = merchant,
                    category = category,
                    note = note
                )
                showAddTransactionSheet = false
            }
        )
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            action = "android.settings.APP_NOTIFICATION_SETTINGS"
            putExtra("app_package", context.packageName)
            putExtra("app_uid", context.applicationInfo.uid)
        }
    }
    context.startActivity(intent)
}

@Composable
fun DashboardScreen(
    state: DashboardUiState.Success,
    hasSmsPermissions: Boolean,
    onRequestPermission: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSyncClick: () -> Unit,
    onAddTransactionClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenBudgets: () -> Unit,
    onSetBudgetClick: () -> Unit,
    onEditBudgetClick: (Long) -> Unit,
    onPeriodSelected: (SpendPeriod) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onReviewUncategorized: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            end = Dimens.screenPadding,
            top = Dimens.screenPadding,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp16)
    ) {
        if (!hasSmsPermissions) {
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.28f)
                ) {
                    Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "SMS permission needed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.height(Dimens.dp8))
                        Text(
                            "Allow SMS access so UPI Tracker can automatically detect incoming bank alerts offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(Dimens.dp12))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                        ) {
                            Text("Grant permission")
                        }
                    }
                }
            }
        }

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
            SpendPeriodSwitcher(
                selectedPeriod = state.selectedPeriod,
                onPeriodSelected = onPeriodSelected
            )
        }

        item {
            SummaryCards(
                spentPaise = state.periodSpent,
                receivedPaise = state.periodReceived,
                transactionCount = state.periodCount,
                showNetAndDaily = state.selectedPeriod == SpendPeriod.MONTH,
                netPaise = state.summary.netPaise,
                averageDailySpendPaise = state.summary.averageDailySpendPaise
            )
        }

        item {
            QuickActionsRow(
                budgetSummary = state.budgetSummary,
                onOpenBudgets = onOpenBudgets,
                onOpenInsights = onOpenInsights,
                onAddTransaction = onAddTransactionClick
            )
        }

        item {
            BudgetCard(
                budgetInfo = state.budgetInfo,
                budgetSummary = state.budgetSummary,
                onSetBudgetClick = onSetBudgetClick,
                onEditBudgetClick = { state.budgetInfo?.let { onEditBudgetClick(it.limitPaise) } },
                onManageBudgetsClick = onOpenBudgets
            )
        }

        item {
            MonthlySummaryChartCard(summary = state.currentMonthSummary)
        }

        if (state.summary.biggestExpense != null) {
            item { BiggestExpenseCard(transaction = state.summary.biggestExpense) }
        }

        if (state.summary.categoryBreakdown.isNotEmpty()) {
            item {
                CategoryBreakdownSection(
                    breakdown = state.summary.categoryBreakdown,
                    onOpenInsights = onOpenInsights,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        if (state.summary.uncategorizedCount > 0) {
            item {
                UncategorizedCard(
                    count = state.summary.uncategorizedCount,
                    onReview = onReviewUncategorized
                )
            }
        }

        if (state.summary.topMerchants.isNotEmpty()) {
            item { TopMerchantsSection(merchants = state.summary.topMerchants) }
        }

        if (state.summary.recentTransactions.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent Activity",
                    action = {
                        TextButton(
                            onClick = onViewAllClick,
                            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                        ) {
                            Text("View all (${state.summary.transactionCount})")
                        }
                    }
                )
            }
            items(items = state.summary.recentTransactions, key = { it.id }) { tx ->
                TransactionRow(transaction = tx, onClick = { onTransactionClick(tx) })
            }
        } else {
            item {
                EmptyState(
                    icon = Icons.Default.List,
                    title = "No transactions yet",
                    hint = "Nothing recorded for ${DateTimeUtils.formatMonthYear(state.selectedMonth)}.",
                    actionText = "Add Transaction",
                    onActionClick = onAddTransactionClick,
                    secondaryActionText = if (hasSmsPermissions) "Sync SMS" else null,
                    onSecondaryActionClick = if (hasSmsPermissions) onSyncClick else null
                )
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    budgetSummary: DashboardBudgetSummary,
    onOpenBudgets: () -> Unit,
    onOpenInsights: () -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)
    ) {
        QuickActionPill(
            label = if (budgetSummary.categoryAttentionCount > 0) {
                "Budgets (${budgetSummary.categoryAttentionCount})"
            } else {
                "Budgets"
            },
            icon = Icons.Default.CheckCircle,
            tint = MaterialTheme.colorScheme.primary,
            onClick = onOpenBudgets,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_action_budgets")
        )
        QuickActionPill(
            label = "Insights",
            icon = Icons.Default.DateRange,
            tint = MaterialTheme.colorScheme.secondary,
            onClick = onOpenInsights,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_action_insights")
        )
        QuickActionPill(
            label = "Add Entry",
            icon = Icons.Default.Add,
            tint = MaterialTheme.colorScheme.tertiary,
            onClick = onAddTransaction,
            modifier = Modifier
                .weight(1f)
                .testTag("quick_action_add")
        )
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier
            .defaultMinSize(minHeight = Dimens.minTouchTarget)
            .clip(RoundedCornerShape(Dimens.cardCornerRadius))
            .clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.dp8, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryBreakdownSection(
    breakdown: List<CategorySummary>,
    onOpenInsights: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxPaise = breakdown.firstOrNull()?.totalPaise ?: 1L
    val isDark = isSystemInDarkTheme()
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Spending by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onOpenInsights,
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                ) {
                    Text("See charts")
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(Dimens.dp8))
            breakdown.forEachIndexed { idx, item ->
                if (idx > 0) Spacer(Modifier.height(Dimens.dp8))
                val catColor = ChartColors.colorForCategoryOrdinal(item.category.ordinal, isDark)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = Dimens.minTouchTarget)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCategoryClick(item.category) }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CategoryIconBadge(
                                category = item.category,
                                color = catColor,
                                size = 36.dp,
                                iconSize = 18.dp
                            )
                            Spacer(Modifier.width(Dimens.dp12))
                            Column {
                                Text(
                                    item.category.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${item.count} payment${if (item.count != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(Dimens.dp8))
                        Text(
                            CurrencyUtils.formatRupees(item.totalPaise),
                            style = MoneyTypography.rowAmount
                        )
                    }
                    Spacer(Modifier.height(Dimens.dp8))
                    val fraction = if (maxPaise > 0L) {
                        (item.totalPaise.toFloat() / maxPaise.toFloat()).coerceIn(0.04f, 1f)
                    } else 0.04f
                    ThemedLinearProgress(
                        fraction = fraction,
                        percent = item.percentOfTotal,
                        color = catColor
                    )
                }
            }
        }
    }
}

@Composable
fun UncategorizedCard(
    count: Int,
    onReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.dp16, vertical = Dimens.dp12),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(Dimens.dp12))
                Text(
                    "$count transaction${if (count != 1) "s" else ""} need a category",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            TextButton(
                onClick = onReview,
                modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
            ) {
                Text("Review", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MonthSelector(
    selectedMonth: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                enabled = canGoPrevious,
                modifier = Modifier
                    .defaultMinSize(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
                    .clearAndSetSemantics { contentDescription = "Previous month" }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = if (canGoPrevious) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    DateTimeUtils.formatMonthYear(selectedMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onNextMonth,
                enabled = canGoNext,
                modifier = Modifier
                    .defaultMinSize(minWidth = Dimens.minTouchTarget, minHeight = Dimens.minTouchTarget)
                    .clearAndSetSemantics { contentDescription = "Next month" }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (canGoNext) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
        }
    }
}

@Composable
fun BudgetCard(
    budgetInfo: BudgetInfo?,
    budgetSummary: DashboardBudgetSummary,
    onSetBudgetClick: () -> Unit,
    onEditBudgetClick: () -> Unit,
    onManageBudgetsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            if (budgetInfo == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly Budget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        val catHint = if (budgetSummary.totalCategoryBudgetsCount > 0) {
                            "${budgetSummary.totalCategoryBudgetsCount} category limit${if (budgetSummary.totalCategoryBudgetsCount != 1) "s" else ""} active"
                        } else {
                            "Set a monthly cap & get alerts"
                        }
                        Text(
                            text = catHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(Dimens.dp8))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onManageBudgetsClick,
                            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                        ) {
                            Text("Manage")
                        }
                        Button(
                            onClick = onSetBudgetClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                        ) {
                            Text("Set limit")
                        }
                    }
                }
            } else {
                val level = when {
                    budgetInfo.percent >= 100 -> BudgetLevel.OVER
                    budgetInfo.percent >= 80 -> BudgetLevel.NEAR
                    else -> BudgetLevel.OK
                }
                val levelColor = budgetLevelColor(level)
                val statusIcon = if (level == BudgetLevel.OK) Icons.Default.CheckCircle else Icons.Default.Warning
                val statusLabel = when (level) {
                    BudgetLevel.OK -> "On track"
                    BudgetLevel.NEAR -> "Near limit"
                    BudgetLevel.OVER -> "Over budget"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Monthly Budget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StatusPill(
                            label = statusLabel,
                            icon = statusIcon,
                            color = levelColor
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onManageBudgetsClick,
                            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                        ) {
                            Text("All")
                        }
                        TextButton(
                            onClick = onEditBudgetClick,
                            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                        ) {
                            Text("Edit")
                        }
                    }
                }

                Spacer(Modifier.height(Dimens.dp8))

                val isOver = budgetInfo.remainingPaise < 0L
                val line1Text = if (!isOver) {
                    "${CurrencyUtils.formatRupees(budgetInfo.remainingPaise)} left"
                } else {
                    "Over by ${CurrencyUtils.formatRupees(-budgetInfo.remainingPaise)}"
                }
                val line1Color = if (!isOver) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error

                Text(
                    text = line1Text,
                    style = MoneyTypography.heroAmount.copy(fontSize = 26.sp),
                    color = line1Color
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${CurrencyUtils.formatRupees(budgetInfo.spentPaise)} spent of ${CurrencyUtils.formatRupees(budgetInfo.limitPaise)} limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(Dimens.dp12))

                val fraction = if (budgetInfo.limitPaise > 0L) {
                    (budgetInfo.spentPaise.toFloat() / budgetInfo.limitPaise.toFloat()).coerceIn(0f, 1f)
                } else 0f

                ThemedLinearProgress(
                    fraction = fraction,
                    percent = budgetInfo.percent,
                    color = levelColor
                )

                if (budgetInfo.remainingPaise > 0L && budgetInfo.perDayPaise != null && budgetInfo.daysLeft > 0) {
                    Spacer(Modifier.height(Dimens.dp8))
                    Text(
                        text = "${CurrencyUtils.formatRupees(budgetInfo.perDayPaise)} / day safe spend for ${budgetInfo.daysLeft} days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (budgetSummary.totalCategoryBudgetsCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    val catText = if (budgetSummary.categoryAttentionCount > 0) {
                        "${budgetSummary.totalCategoryBudgetsCount} category budgets • ${budgetSummary.categoryAttentionCount} need attention"
                    } else {
                        "${budgetSummary.totalCategoryBudgetsCount} category budgets on track"
                    }
                    Text(
                        text = catText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (budgetSummary.categoryAttentionCount > 0) {
                            budgetLevelColor(BudgetLevel.NEAR)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SpendPeriodSwitcher(
    selectedPeriod: SpendPeriod,
    onPeriodSelected: (SpendPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
    ) {
        val chipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        FilterChip(
            selected = selectedPeriod == SpendPeriod.TODAY,
            onClick = { onPeriodSelected(SpendPeriod.TODAY) },
            label = { Text("Today", fontWeight = FontWeight.SemiBold) },
            shape = RoundedCornerShape(999.dp),
            colors = chipColors,
            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
        )
        FilterChip(
            selected = selectedPeriod == SpendPeriod.WEEK,
            onClick = { onPeriodSelected(SpendPeriod.WEEK) },
            label = { Text("This Week", fontWeight = FontWeight.SemiBold) },
            shape = RoundedCornerShape(999.dp),
            colors = chipColors,
            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
        )
        FilterChip(
            selected = selectedPeriod == SpendPeriod.MONTH,
            onClick = { onPeriodSelected(SpendPeriod.MONTH) },
            label = { Text("This Month", fontWeight = FontWeight.SemiBold) },
            shape = RoundedCornerShape(999.dp),
            colors = chipColors,
            modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
        )
    }
}

@Composable
fun SummaryCards(
    spentPaise: Long,
    receivedPaise: Long,
    transactionCount: Int,
    showNetAndDaily: Boolean,
    netPaise: Long,
    averageDailySpendPaise: Long,
    modifier: Modifier = Modifier
) {
    val creditColor = creditAmountColor()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp12)
        ) {
            AppCard(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            "Spent",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(Dimens.dp12))
                    Text(
                        text = CurrencyUtils.formatRupees(spentPaise),
                        style = MoneyTypography.cardAmount,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AppCard(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(creditColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = creditColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            "Received",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(Dimens.dp12))
                    Text(
                        text = CurrencyUtils.formatRupees(receivedPaise),
                        style = MoneyTypography.cardAmount,
                        color = creditColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        AppCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            borderColor = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.dp16, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$transactionCount payment${if (transactionCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showNetAndDaily) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
                        val netPrefix = if (netPaise > 0L) "+" else ""
                        Text(
                            text = "Net $netPrefix${CurrencyUtils.formatRupees(netPaise)}",
                            style = MoneyTypography.captionNumber,
                            fontWeight = FontWeight.SemiBold,
                            color = if (netPaise >= 0L) creditColor else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Avg ${CurrencyUtils.formatRupees(averageDailySpendPaise)}/d",
                            style = MoneyTypography.captionNumber,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BiggestExpenseCard(transaction: Transaction, modifier: Modifier = Modifier) {
    val category = Category.fromLabel(transaction.category)
    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "HIGHEST SINGLE EXPENSE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusPill(
                    label = category.label,
                    icon = Icons.Default.Star,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(Dimens.dp12))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    CategoryIconBadge(category = category)
                    Spacer(Modifier.width(Dimens.dp12))
                    Column {
                        Text(
                            transaction.merchant,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            DateTimeUtils.formatShortDateTime(transaction.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(Dimens.dp8))
                Text(
                    "-${CurrencyUtils.formatRupees(transaction.amountPaise)}",
                    style = MoneyTypography.cardAmount.copy(fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun TopMerchantsSection(merchants: List<TopMerchantSummary>, modifier: Modifier = Modifier) {
    val maxPaise = merchants.firstOrNull()?.totalPaise ?: 1L
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Text(
                "Top Merchants",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Dimens.dp12))
            merchants.forEachIndexed { idx, item ->
                if (idx > 0) Spacer(Modifier.height(Dimens.dp12))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MoneyTypography.captionNumber,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.width(Dimens.dp12))
                            Column {
                                Text(
                                    item.merchantName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${item.count} payment${if (item.count > 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(Dimens.dp8))
                        Text(
                            CurrencyUtils.formatRupees(item.totalPaise),
                            style = MoneyTypography.rowAmount
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val fraction = if (maxPaise > 0L) {
                        (item.totalPaise.toFloat() / maxPaise.toFloat()).coerceIn(0.04f, 1f)
                    } else 0.04f
                    val percent = if (maxPaise > 0L) ((item.totalPaise * 100) / maxPaise).toInt() else 0
                    ThemedLinearProgress(
                        fraction = fraction,
                        percent = percent,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
