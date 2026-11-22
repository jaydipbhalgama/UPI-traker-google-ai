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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Budget
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.ui.TransactionEditBottomSheet
import com.example.upitracker.ui.budget.BudgetEditBottomSheet
import com.example.upitracker.ui.budget.BudgetSheetTarget
import com.example.upitracker.ui.budget.BudgetsScreen
import com.example.upitracker.ui.budget.BudgetsUiState
import com.example.upitracker.ui.budget.BudgetsViewModel
import com.example.upitracker.ui.components.AppCard
import com.example.upitracker.ui.components.CategoryColorDot
import com.example.upitracker.ui.components.EmptyState
import com.example.upitracker.ui.components.SectionHeader
import com.example.upitracker.ui.components.ThemedLinearProgress
import com.example.upitracker.ui.components.TransactionRow
import com.example.upitracker.ui.components.budgetLevelColor
import com.example.upitracker.ui.components.creditAmountColor
import com.example.upitracker.ui.dashboard.BudgetInfo
import com.example.upitracker.ui.dashboard.CategorySummary
import com.example.upitracker.ui.dashboard.DashboardUiState
import com.example.upitracker.ui.dashboard.DashboardViewModel
import com.example.upitracker.ui.dashboard.SpendPeriod
import com.example.upitracker.ui.dashboard.TopMerchantSummary
import com.example.upitracker.ui.insights.InsightsScreen
import com.example.upitracker.ui.insights.InsightsViewModel
import com.example.upitracker.ui.navigation.Screen
import com.example.upitracker.ui.navigation.backTarget
import com.example.upitracker.ui.settings.SettingsScreen
import com.example.upitracker.ui.sms.UnrecognisedSmsScreen
import com.example.upitracker.ui.sms.UnrecognisedSmsViewModel
import com.example.upitracker.domain.budget.BudgetLevel
import com.example.upitracker.ui.theme.Dimens
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
        // One-time auto-categorize pass
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

    // Export confirmation & launcher state
    var pendingExportList by remember { mutableStateOf<List<Transaction>?>(null) }
    var showExportConfirmDialog by remember { mutableStateOf(false) }

    // Budget edit bottom sheet state
    var editingBudgetTarget by remember { mutableStateOf<BudgetSheetTarget?>(null) }

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

    // SMS permission check
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

    // Notifications permission launcher (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        budgetsViewModel.setNotificationHintVisible(!isGranted)
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
        dashboardViewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Navigation helper: navigate to a target screen, resetting transaction filters ONLY when leaving Transactions
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

    // Bottom navigation bar visibility: visible only on top-level tabs when keyboard is closed
    val isTopLevelTab = currentScreen is Screen.Home ||
                        currentScreen is Screen.Insights ||
                        currentScreen is Screen.Transactions

    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val showBottomBar = isTopLevelTab && !isImeVisible

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val title = when (currentScreen) {
                        is Screen.Home -> "UPI Tracker"
                        is Screen.Insights -> "Insights"
                        is Screen.Transactions -> "Transactions"
                        is Screen.Budgets -> "Budgets"
                        is Screen.Settings -> "Settings"
                        is Screen.UnrecognisedSms -> "Unrecognised bank SMS"
                    }
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    // Back arrow ONLY on sub-screens (Budgets, Settings, UnrecognisedSms).
                    // Top-level tabs (Home, Insights, Transactions) do NOT have back arrows.
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
                            // Settings gear icon
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
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(2.dp)
                                )
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
                                DropdownMenu(expanded = showDashboardMenu, onDismissRequest = { showDashboardMenu = false }) {
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
                                        onClick = { dashboardViewModel.autoCategorizeUncategorized(); showDashboardMenu = false }
                                    )
                                    if (BuildConfig.DEBUG) {
                                        DropdownMenuItem(
                                            text = { Text("Send test budget alert") },
                                            onClick = {
                                                val app = context.applicationContext as UpiTrackerApplication
                                                app.container.budgetAlertChecker.postTestAlert()
                                                showDashboardMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add sample transaction") },
                                            onClick = { dashboardViewModel.addSampleTransaction(); showDashboardMenu = false }
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
                                DropdownMenu(expanded = showAllTxMenu, onDismissRequest = { showAllTxMenu = false }) {
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
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home,
                        onClick = { navigateTo(Screen.Home) },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Insights,
                        onClick = { navigateTo(Screen.Insights) },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = { Text("Insights") }
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Transactions,
                        onClick = { navigateTo(Screen.Transactions()) },
                        icon = { Icon(Icons.Default.List, contentDescription = null) },
                        label = { Text("Transactions") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                                onViewAllClick = {
                                    navigateTo(Screen.Transactions())
                                },
                                onOpenInsights = { navigateTo(Screen.Insights) },
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
                    SettingsScreen()
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

    // CSV Export Confirmation Dialog
    if (showExportConfirmDialog) {
        val count = pendingExportList?.size ?: 0
        AlertDialog(
            onDismissRequest = {
                showExportConfirmDialog = false
                pendingExportList = null
            },
            title = { Text("Export transactions?") },
            text = {
                Text("$count transactions will be saved to a file you choose. It contains merchant names and amounts, so keep it private.")
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

    // Edit bottom sheet
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

    // Budget edit bottom sheet
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState.Success,
    hasSmsPermissions: Boolean,
    onRequestPermission: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSyncClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onOpenInsights: () -> Unit,
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
        contentPadding = PaddingValues(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp24)
    ) {
        // SMS permission banner
        if (!hasSmsPermissions) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.cardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardTonalElevation)
                ) {
                    Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                        Text("SMS permissions required", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(Dimens.dp4))
                        Text("Grant SMS permissions so UPI Tracker can read and parse incoming bank transaction messages offline.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(Dimens.dp8))
                        Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Grant permission")
                        }
                    }
                }
            }
        }

        // Month selector
        item { MonthSelector(state.selectedMonth, state.canGoPrevious, state.canGoNext, onPreviousMonth, onNextMonth) }

        // Budget card (replaces Balance left card)
        item {
            BudgetCard(
                budgetInfo = state.budgetInfo,
                onSetBudgetClick = onSetBudgetClick,
                onEditBudgetClick = { state.budgetInfo?.let { onEditBudgetClick(it.limitPaise) } }
            )
        }

        // Spend period switcher
        item {
            SpendPeriodSwitcher(
                selectedPeriod = state.selectedPeriod,
                onPeriodSelected = onPeriodSelected
            )
        }

        // Summary cards
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

        // Biggest expense
        if (state.summary.biggestExpense != null) {
            item { BiggestExpenseCard(transaction = state.summary.biggestExpense) }
        }

        // Spending by category with Insights button
        if (state.summary.categoryBreakdown.isNotEmpty()) {
            item {
                CategoryBreakdownSection(
                    breakdown = state.summary.categoryBreakdown,
                    onOpenInsights = onOpenInsights,
                    onCategoryClick = onCategoryClick
                )
            }
        }

        // Uncategorized reminder card
        if (state.summary.uncategorizedCount > 0) {
            item { UncategorizedCard(count = state.summary.uncategorizedCount, onReview = onReviewUncategorized) }
        }

        // Top merchants
        if (state.summary.topMerchants.isNotEmpty()) {
            item { TopMerchantsSection(merchants = state.summary.topMerchants) }
        }

        // Recent transactions
        if (state.summary.recentTransactions.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent Transactions",
                    action = {
                        TextButton(onClick = onViewAllClick) {
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
                    title = "No transactions found",
                    hint = "There are no transactions recorded for ${DateTimeUtils.formatMonthYear(state.selectedMonth)}.",
                    actionText = if (hasSmsPermissions) "Sync Bank SMS" else null,
                    onActionClick = if (hasSmsPermissions) onSyncClick else null,
                )
            }
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
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Spending by Category", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onOpenInsights) {
                    Text("Insights")
                }
            }
            Spacer(Modifier.height(Dimens.dp8))
            breakdown.forEachIndexed { idx, item ->
                if (idx > 0) Spacer(Modifier.height(Dimens.dp8))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.dp8))
                        .clickable { onCategoryClick(item.category) }
                        .padding(vertical = Dimens.dp4)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            CategoryColorDot(item.category)
                            Spacer(Modifier.width(Dimens.dp8))
                            Column {
                                Text(item.category.label, style = MaterialTheme.typography.bodyLarge)
                                Text("${item.count} payment${if (item.count != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(CurrencyUtils.formatRupees(item.totalPaise), style = MaterialTheme.typography.bodyLarge)
                            Text("${item.percentOfTotal}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(Dimens.dp4))
                    val fraction = if (maxPaise > 0L) (item.totalPaise.toFloat() / maxPaise.toFloat()).coerceIn(0.04f, 1f) else 0.04f
                    ThemedLinearProgress(
                        fraction = fraction,
                        percent = item.percentOfTotal,
                        color = MaterialTheme.colorScheme.primary
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
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimens.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$count transaction${if (count != 1) "s" else ""} need a category",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onReview) { Text("Review") }
        }
    }
}

@Composable
fun MonthSelector(selectedMonth: YearMonth, canGoPrevious: Boolean, canGoNext: Boolean, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardTonalElevation)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.dp8, vertical = Dimens.dp4),
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
                    tint = if (canGoPrevious) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            Text(DateTimeUtils.formatMonthYear(selectedMonth), style = MaterialTheme.typography.titleMedium)
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
                    tint = if (canGoNext) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
fun BudgetCard(
    budgetInfo: BudgetInfo?,
    onSetBudgetClick: () -> Unit,
    onEditBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            if (budgetInfo == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly budget",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(onClick = onSetBudgetClick) {
                        Text("Set monthly budget")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly budget",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onEditBudgetClick) {
                        Text("Edit")
                    }
                }

                Spacer(Modifier.height(Dimens.dp8))

                val level = when {
                    budgetInfo.percent >= 100 -> BudgetLevel.OVER
                    budgetInfo.percent >= 80 -> BudgetLevel.NEAR
                    else -> BudgetLevel.OK
                }
                val isOver = budgetInfo.remainingPaise < 0L
                val line1Text = if (!isOver) {
                    "${CurrencyUtils.formatRupees(budgetInfo.remainingPaise)} left"
                } else {
                    "Over by ${CurrencyUtils.formatRupees(-budgetInfo.remainingPaise)}"
                }
                val line1Color = if (!isOver) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOver || level == BudgetLevel.NEAR) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = if (isOver) "Over budget" else "Near limit",
                            tint = budgetLevelColor(level),
                            modifier = Modifier.size(Dimens.dp24)
                        )
                        Spacer(Modifier.width(Dimens.dp8))
                    }
                    Text(
                        text = line1Text,
                        style = MaterialTheme.typography.headlineMedium,
                        color = line1Color
                    )
                }

                Spacer(Modifier.height(Dimens.dp4))

                Text(
                    text = "${CurrencyUtils.formatRupees(budgetInfo.spentPaise)} spent of ${CurrencyUtils.formatRupees(budgetInfo.limitPaise)} budget",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(Dimens.dp8))

                val fraction = if (budgetInfo.limitPaise > 0L) {
                    (budgetInfo.spentPaise.toFloat() / budgetInfo.limitPaise.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                ThemedLinearProgress(
                    fraction = fraction,
                    percent = budgetInfo.percent,
                    color = budgetLevelColor(level)
                )

                if (budgetInfo.remainingPaise > 0L && budgetInfo.perDayPaise != null && budgetInfo.daysLeft > 0) {
                    Spacer(Modifier.height(Dimens.dp8))
                    Text(
                        text = "${CurrencyUtils.formatRupees(budgetInfo.perDayPaise)} per day for ${budgetInfo.daysLeft} days left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        FilterChip(
            selected = selectedPeriod == SpendPeriod.TODAY,
            onClick = { onPeriodSelected(SpendPeriod.TODAY) },
            label = { Text("Today") }
        )
        FilterChip(
            selected = selectedPeriod == SpendPeriod.WEEK,
            onClick = { onPeriodSelected(SpendPeriod.WEEK) },
            label = { Text("This Week") }
        )
        FilterChip(
            selected = selectedPeriod == SpendPeriod.MONTH,
            onClick = { onPeriodSelected(SpendPeriod.MONTH) },
            label = { Text("This Month") }
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
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
            AppCard(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                    Text("Spent", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.dp8))
                    Text(
                        CurrencyUtils.formatRupees(spentPaise),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            AppCard(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                    Text("Received", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Dimens.dp8))
                    Text(
                        CurrencyUtils.formatRupees(receivedPaise),
                        style = MaterialTheme.typography.headlineMedium,
                        color = creditColor
                    )
                }
            }
        }
        Text(
            text = "$transactionCount transaction${if (transactionCount != 1) "s" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimens.dp4)
        )
        if (showNetAndDaily) {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Dimens.cardPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Net: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            CurrencyUtils.formatRupees(netPaise),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (netPaise >= 0) creditColor else MaterialTheme.colorScheme.error
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Avg per day: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(CurrencyUtils.formatRupees(averageDailySpendPaise), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun BiggestExpenseCard(transaction: Transaction, modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Text("Biggest Expense", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(Dimens.dp8))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    CategoryColorDot(Category.fromLabel(transaction.category))
                    Spacer(Modifier.width(Dimens.dp8))
                    Column {
                        Text(transaction.merchant, style = MaterialTheme.typography.bodyLarge)
                        Text(DateTimeUtils.formatDateTime(transaction.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("- " + CurrencyUtils.formatRupees(transaction.amountPaise), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TopMerchantsSection(merchants: List<TopMerchantSummary>, modifier: Modifier = Modifier) {
    val maxPaise = merchants.firstOrNull()?.totalPaise ?: 1L
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Text("Top Merchants", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Dimens.dp8))
            merchants.forEachIndexed { idx, item ->
                if (idx > 0) Spacer(Modifier.height(Dimens.dp8))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.merchantName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${item.count} payment${if (item.count > 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(Dimens.dp8))
                            Text(CurrencyUtils.formatRupees(item.totalPaise), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(Modifier.height(Dimens.dp4))
                    val fraction = if (maxPaise > 0L) (item.totalPaise.toFloat() / maxPaise.toFloat()).coerceIn(0.04f, 1f) else 0.04f
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
