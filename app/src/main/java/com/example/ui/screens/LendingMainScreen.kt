package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.Customer
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionType
import com.example.data.repository.LendingRepository
import com.example.domain.LoanCalculator
import com.example.ui.components.BorrowerListCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MonthTillDateCard
import com.example.ui.components.QuickComposerCard
import com.example.ui.components.SleekCircularActionIcon
import com.example.ui.components.TransactionRowCard
import com.example.ui.modals.AddBorrowerModal
import com.example.ui.modals.BorrowerDetailDialog
import com.example.ui.modals.EditCustomerModal
import com.example.ui.modals.ExportSummaryModal
import com.example.ui.modals.QuickPaymentModal
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import com.example.ui.theme.AmberFollowUp
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.CrimsonOverdue
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.EmeraldCollected
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.TrustBlue
import com.example.ui.theme.TrustBlueBorder
import com.example.ui.theme.TrustBlueSoft
import com.example.ui.theme.TrustBlueWash
import com.example.ui.theme.TrustNavy
import com.example.ui.viewmodel.LedgerUiState
import com.example.ui.viewmodel.LendingViewModel
import com.example.ui.viewmodel.StatusFilter
import kotlinx.coroutines.launch

enum class LendingTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Filled.Home),
    BORROWERS("Borrowers", Icons.Filled.Group),
    FOLLOW_UP("Follow-up", Icons.Filled.NotificationsActive),
    LEDGER("Ledger", Icons.AutoMirrored.Filled.ReceiptLong),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendingMainScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: LendingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                LendingViewModel(LendingRepository(app))
            }
        },
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val txnTypeFilter by viewModel.txnTypeFilter.collectAsStateWithLifecycle()
    val txnModeFilter by viewModel.txnModeFilter.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(LendingTab.DASHBOARD) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var showQuickPaymentModal by remember { mutableStateOf(false) }
    var quickPayCustomer by remember { mutableStateOf<Customer?>(null) }
    var quickPayType by remember { mutableStateOf(TransactionType.MONTHLY_INTEREST_PAYMENT) }

    var showAddBorrowerModal by remember { mutableStateOf(false) }
    var selectedBorrowerId by remember { mutableStateOf<String?>(null) }
    var editingBorrower by remember { mutableStateOf<Customer?>(null) }
    var showExportModal by remember { mutableStateOf(false) }

    fun dialCustomer(customer: Customer) {
        val cleanPhone = customer.phoneNumber.trim()
        if (cleanPhone.isBlank() || cleanPhone == "—") {
            editingBorrower = customer
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Add a phone number for ${customer.name} to call.")
            }
        } else {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
            context.startActivity(intent)
        }
    }

    fun shareCsvReport(state: LedgerUiState.Success) {
        val csv = LoanCalculator.buildCsvExport(state.customers, state.filteredTransactions)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "LendFlow Portfolio & Transactions Export")
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Lending Ledger"))
    }

    fun copyCsvReport(state: LedgerUiState.Success) {
        val csv = LoanCalculator.buildCsvExport(state.customers, state.filteredTransactions)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("LendFlow Export", csv))
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Ledger CSV copied to clipboard")
        }
    }

    val followUpCount = (uiState as? LedgerUiState.Success)?.kpis?.followUpCount ?: 0

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "LendFlow",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = TrustBlue,
                            )
                        },
                        actions = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                modifier = Modifier.padding(end = 6.dp),
                            ) {
                                SleekCircularActionIcon(
                                    icon = Icons.Filled.Add,
                                    contentDescription = "Add borrower",
                                    onClick = { showAddBorrowerModal = true },
                                    modifier = Modifier.testTag("top_add_borrower_button"),
                                )
                                SleekCircularActionIcon(
                                    icon = Icons.Filled.Search,
                                    contentDescription = "Toggle search",
                                    onClick = { showSearchBar = !showSearchBar },
                                    tint = if (showSearchBar) MaterialTheme.colorScheme.onPrimary else TrustBlue,
                                    containerColor = if (showSearchBar) TrustBlue else TrustBlueSoft,
                                    modifier = Modifier.testTag("top_search_toggle_button"),
                                )
                                SleekCircularActionIcon(
                                    icon = Icons.Filled.Assessment,
                                    contentDescription = "Monthly summary and export",
                                    onClick = { showExportModal = true },
                                    modifier = Modifier.testTag("export_summary_button"),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                    HorizontalDivider(color = TrustBlueBorder, thickness = 0.75.dp)
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        HorizontalDivider(color = TrustBlueBorder, thickness = 0.75.dp)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                        ) {
                        LendingTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    if (tab == LendingTab.FOLLOW_UP && followUpCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = CrimsonOverdue,
                                                    contentColor = MaterialTheme.colorScheme.onError,
                                                ) {
                                                    Text("$followUpCount")
                                                }
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TrustBlue,
                                    selectedTextColor = TrustBlue,
                                    indicatorColor = TrustBlueSoft,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                            )
                        }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (uiState is LedgerUiState.Success) {
                    FloatingActionButton(
                        onClick = {
                            quickPayCustomer = null
                            quickPayType = TransactionType.MONTHLY_INTEREST_PAYMENT
                            showQuickPaymentModal = true
                        },
                        containerColor = TrustBlue,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.testTag("quick_payment_fab"),
                    ) {
                        Icon(Icons.Filled.Payments, contentDescription = "Quick payment entry")
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LendingTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    if (tab == LendingTab.FOLLOW_UP && followUpCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = CrimsonOverdue) {
                                                    Text("$followUpCount")
                                                }
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    when (val state = uiState) {
                        LedgerUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = TrustBlue)
                            }
                        }

                        is LedgerUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = state.message,
                                    color = CrimsonOverdue,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        is LedgerUiState.Success -> {
                            val today = LoanCalculator.todayIso()
                            when (selectedTab) {
                                LendingTab.DASHBOARD -> DashboardFeedContent(
                                    state = state,
                                    todayIso = today,
                                    showSearchBar = showSearchBar,
                                    searchQuery = searchQuery,
                                    statusFilter = statusFilter,
                                    onSearchChange = viewModel::onSearchQueryChange,
                                    onStatusFilterChange = viewModel::onStatusFilterChange,
                                    onQuickPayGeneral = { type ->
                                        quickPayCustomer = null
                                        quickPayType = type
                                        showQuickPaymentModal = true
                                    },
                                    onAddBorrower = { showAddBorrowerModal = true },
                                    onBorrowerClick = { selectedBorrowerId = it.id },
                                    onQuickPayBorrower = { customer ->
                                        quickPayCustomer = customer
                                        quickPayType = TransactionType.MONTHLY_INTEREST_PAYMENT
                                        showQuickPaymentModal = true
                                    },
                                    onCallBorrower = ::dialCustomer,
                                )

                                LendingTab.BORROWERS -> BorrowersTabContent(
                                    state = state,
                                    todayIso = today,
                                    searchQuery = searchQuery,
                                    statusFilter = statusFilter,
                                    onSearchChange = viewModel::onSearchQueryChange,
                                    onStatusFilterChange = viewModel::onStatusFilterChange,
                                    onAddBorrower = { showAddBorrowerModal = true },
                                    onBorrowerClick = { selectedBorrowerId = it.id },
                                    onQuickPay = { customer ->
                                        quickPayCustomer = customer
                                        quickPayType = TransactionType.MONTHLY_INTEREST_PAYMENT
                                        showQuickPaymentModal = true
                                    },
                                    onCallBorrower = ::dialCustomer,
                                )

                                LendingTab.FOLLOW_UP -> FollowUpTabContent(
                                    state = state,
                                    todayIso = today,
                                    onBorrowerClick = { selectedBorrowerId = it.id },
                                    onCollectInterest = { customer ->
                                        quickPayCustomer = customer
                                        quickPayType = TransactionType.MONTHLY_INTEREST_PAYMENT
                                        showQuickPaymentModal = true
                                    },
                                    onCallBorrower = ::dialCustomer,
                                )

                                LendingTab.LEDGER -> TransactionsTabContent(
                                    state = state,
                                    searchQuery = searchQuery,
                                    selectedType = txnTypeFilter,
                                    selectedMode = txnModeFilter,
                                    onSearchChange = viewModel::onSearchQueryChange,
                                    onTypeChange = viewModel::onTransactionTypeFilterChange,
                                    onModeChange = viewModel::onPaymentModeFilterChange,
                                    onExportClick = { showExportModal = true },
                                )
                            }

                            val activeDetailCustomer = state.customers.firstOrNull { it.id == selectedBorrowerId }
                            if (activeDetailCustomer != null) {
                                val customerTxns = state.transactions.filter { it.customerId == activeDetailCustomer.id }
                                BorrowerDetailDialog(
                                    customer = activeDetailCustomer,
                                    customerTransactions = customerTxns,
                                    onDismiss = { selectedBorrowerId = null },
                                    onRecordAction = { actionType ->
                                        quickPayCustomer = activeDetailCustomer
                                        quickPayType = actionType
                                        showQuickPaymentModal = true
                                    },
                                    onEditCustomer = { editingBorrower = activeDetailCustomer },
                                    onCallCustomer = { dialCustomer(activeDetailCustomer) },
                                )
                            }

                            if (showQuickPaymentModal) {
                                QuickPaymentModal(
                                    customers = state.customers,
                                    initialCustomer = quickPayCustomer,
                                    initialType = quickPayType,
                                    isSaving = isSaving,
                                    onDismiss = { showQuickPaymentModal = false },
                                    onSubmit = { input ->
                                        viewModel.recordTransaction(input) {
                                            showQuickPaymentModal = false
                                        }
                                    },
                                )
                            }

                            if (showAddBorrowerModal) {
                                AddBorrowerModal(
                                    suggestedCustomerCode = state.nextCustomerCode,
                                    isSaving = isSaving,
                                    onDismiss = { showAddBorrowerModal = false },
                                    onSubmit = { input ->
                                        viewModel.addBorrower(input) {
                                            showAddBorrowerModal = false
                                        }
                                    },
                                )
                            }

                            editingBorrower?.let { target ->
                                EditCustomerModal(
                                    customer = target,
                                    isSaving = isSaving,
                                    onDismiss = { editingBorrower = null },
                                    onSave = { name, phone, dueDay, rate, status, notes ->
                                        viewModel.updateCustomerProfile(
                                            customer = target,
                                            name = name,
                                            phoneNumber = phone,
                                            dueDayOfMonth = dueDay,
                                            monthlyInterestRate = rate,
                                            status = status,
                                            followUpNotes = notes,
                                        ) {
                                            editingBorrower = null
                                        }
                                    },
                                )
                            }

                            if (showExportModal) {
                                ExportSummaryModal(
                                    summaries = state.monthlySummaries,
                                    totalCustomers = state.customers.size,
                                    totalTransactions = state.transactions.size,
                                    onDismiss = { showExportModal = false },
                                    onShareCsv = { shareCsvReport(state) },
                                    onCopyCsv = { copyCsvReport(state) },
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
private fun DashboardFeedContent(
    state: LedgerUiState.Success,
    todayIso: String,
    showSearchBar: Boolean,
    searchQuery: String,
    statusFilter: StatusFilter,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onQuickPayGeneral: (TransactionType) -> Unit,
    onAddBorrower: () -> Unit,
    onBorrowerClick: (Customer) -> Unit,
    onQuickPayBorrower: (Customer) -> Unit,
    onCallBorrower: (Customer) -> Unit,
) {
    val kpis = state.kpis
    val onTrackCount = (kpis.activeBorrowersCount - kpis.followUpCount).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 840.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AnimatedVisibility(visible = showSearchBar || searchQuery.isNotEmpty()) {
                SleekSearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Search borrower name, ID, notes…",
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .testTag("borrower_search_input"),
                )
            }
        }

        // Front-Page Primary Dashboard Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = TrustBlue,
                        )
                        Text(
                            text = "மாதாந்திர பணம் மற்றும் வட்டி கணக்கு",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        color = TrustBlueSoft,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, TrustBlueBorder),
                    ) {
                        Text(
                            text = "${kpis.activeBorrowersCount} Active",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = TrustBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val useFourColumns = maxWidth >= 660.dp
                    if (useFourColumns) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            KpiMetricCard(
                                title = "Total Active Principal",
                                value = LoanCalculator.formatCurrency(kpis.activeLoanPrincipal),
                                subtitle = "${kpis.activeBorrowersCount} active borrowers",
                                icon = Icons.Filled.AccountBalanceWallet,
                                accentColor = TrustBlue,
                                accentBackground = TrustBlueSoft,
                                valueColor = TrustBlue,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kpi_active_principal"),
                            )
                            KpiMetricCard(
                                title = "Monthly Interest Due",
                                value = LoanCalculator.formatCurrency(kpis.monthlyInterestExpected),
                                subtitle = "2% rate · மாத வட்டி",
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                accentColor = TrustBlue,
                                accentBackground = TrustBlueSoft,
                                valueColor = TrustNavy,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kpi_monthly_interest_due"),
                            )
                            KpiMetricCard(
                                title = "Overdue Interest",
                                value = LoanCalculator.formatCurrency(kpis.overdueInterestAmount),
                                subtitle = if (kpis.followUpCount > 0) "${kpis.followUpCount} follow-up needed" else "All on track",
                                icon = Icons.Filled.Warning,
                                accentColor = if (kpis.overdueInterestAmount > 0) CrimsonOverdue else TrustBlue,
                                accentBackground = if (kpis.overdueInterestAmount > 0) CrimsonSoft else TrustBlueSoft,
                                highlightBorder = kpis.overdueInterestAmount > 0,
                                valueColor = if (kpis.overdueInterestAmount > 0) CrimsonOverdue else TrustBlue,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kpi_overdue_interest"),
                            )
                            KpiMetricCard(
                                title = "Interest Collected",
                                value = LoanCalculator.formatCurrency(kpis.totalInterestCollected),
                                subtitle = "$onTrackCount on track",
                                icon = Icons.Filled.Payments,
                                accentColor = EmeraldCollected,
                                accentBackground = EmeraldSoft,
                                valueColor = TrustNavy,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("kpi_interest_collected"),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                KpiMetricCard(
                                    title = "Total Active Principal",
                                    value = LoanCalculator.formatCurrency(kpis.activeLoanPrincipal),
                                    subtitle = "${kpis.activeBorrowersCount} active borrowers",
                                    icon = Icons.Filled.AccountBalanceWallet,
                                    accentColor = TrustBlue,
                                    accentBackground = TrustBlueSoft,
                                    valueColor = TrustBlue,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_active_principal"),
                                )
                                KpiMetricCard(
                                    title = "Monthly Interest Due",
                                    value = LoanCalculator.formatCurrency(kpis.monthlyInterestExpected),
                                    subtitle = "2% rate · மாத வட்டி",
                                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                                    accentColor = TrustBlue,
                                    accentBackground = TrustBlueSoft,
                                    valueColor = TrustNavy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_monthly_interest_due"),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                KpiMetricCard(
                                    title = "Overdue Interest",
                                    value = LoanCalculator.formatCurrency(kpis.overdueInterestAmount),
                                    subtitle = if (kpis.followUpCount > 0) "${kpis.followUpCount} follow-up needed" else "All on track",
                                    icon = Icons.Filled.Warning,
                                    accentColor = if (kpis.overdueInterestAmount > 0) CrimsonOverdue else TrustBlue,
                                    accentBackground = if (kpis.overdueInterestAmount > 0) CrimsonSoft else TrustBlueSoft,
                                    highlightBorder = kpis.overdueInterestAmount > 0,
                                    valueColor = if (kpis.overdueInterestAmount > 0) CrimsonOverdue else TrustBlue,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_overdue_interest"),
                                )
                                KpiMetricCard(
                                    title = "Interest Collected",
                                    value = LoanCalculator.formatCurrency(kpis.totalInterestCollected),
                                    subtitle = "$onTrackCount on track",
                                    icon = Icons.Filled.Payments,
                                    accentColor = EmeraldCollected,
                                    accentBackground = EmeraldSoft,
                                    valueColor = TrustNavy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_interest_collected"),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Month Till Date Collection Tracker (Due by today, Recorded vs Not Yet Paid)
        item {
            MonthTillDateCard(
                todayIso = todayIso,
                kpis = kpis,
                unpaidBorrowers = state.tillDateUnpaidBorrowers,
                onPayBorrower = onQuickPayBorrower,
                onViewBorrower = onBorrowerClick,
            )
        }

        // Facebook-style Quick Composer Card
        item {
            QuickComposerCard(
                onQuickPay = { onQuickPayGeneral(TransactionType.MONTHLY_INTEREST_PAYMENT) },
                onNewBorrower = onAddBorrower,
                onTopUpOrRepay = { onQuickPayGeneral(TransactionType.TOP_UP) },
            )
        }

        // Borrower Accounts Header & Pill Status Filter Row
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Borrower Accounts (${state.filteredCustomers.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TrustNavy,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
                StatusFilterPillRow(
                    statusFilter = statusFilter,
                    onStatusFilterChange = onStatusFilterChange,
                )
            }
        }

        if (state.filteredCustomers.isEmpty()) {
            item {
                EmptyLedgerCard(
                    title = if (state.customers.isEmpty()) "No Borrowers Yet" else "No Matching Borrowers",
                    subtitle = if (state.customers.isEmpty()) {
                        "Tap New Loan to add your first borrower."
                    } else {
                        "Try clearing your search or choosing All."
                    },
                    showAddButton = state.customers.isEmpty(),
                    onAddBorrower = onAddBorrower,
                )
            }
        } else {
            items(state.filteredCustomers, key = { it.id }) { customer ->
                val isOverdue = LoanCalculator.isInterestOverdue(customer, todayIso)
                val overdueAmt = LoanCalculator.overdueInterestAmount(customer, todayIso)
                BorrowerListCard(
                    customer = customer,
                    isOverdue = isOverdue,
                    overdueAmount = overdueAmt,
                    onClick = { onBorrowerClick(customer) },
                    onQuickPay = { onQuickPayBorrower(customer) },
                    onCall = { onCallBorrower(customer) },
                )
            }
        }
    }
}

@Composable
private fun BorrowersTabContent(
    state: LedgerUiState.Success,
    todayIso: String,
    searchQuery: String,
    statusFilter: StatusFilter,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onAddBorrower: () -> Unit,
    onBorrowerClick: (Customer) -> Unit,
    onQuickPay: (Customer) -> Unit,
    onCallBorrower: (Customer) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TrustBlueBorder),
                shadowElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Borrowers", style = MaterialTheme.typography.titleLarge, color = TrustBlue)
                        Text(
                            text = "${state.customers.size} accounts · 2%/mo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = onAddBorrower,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("borrowers_add_button"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Loan")
                    }
                }
            }
        }

        item {
            SleekSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Search borrower name, ID, or phone…",
                modifier = Modifier.testTag("borrower_search_input"),
            )
        }

        item {
            StatusFilterPillRow(
                statusFilter = statusFilter,
                onStatusFilterChange = onStatusFilterChange,
            )
        }

        if (state.filteredCustomers.isEmpty()) {
            item {
                EmptyLedgerCard(
                    title = if (state.customers.isEmpty()) "Borrower Directory Empty" else "No Matches Found",
                    subtitle = if (state.customers.isEmpty()) {
                        "Tap New Loan to record an initial disbursement."
                    } else {
                        "Adjust your search or status filter to view accounts."
                    },
                    showAddButton = state.customers.isEmpty(),
                    onAddBorrower = onAddBorrower,
                )
            }
        } else {
            items(state.filteredCustomers, key = { it.id }) { customer ->
                val isOverdue = LoanCalculator.isInterestOverdue(customer, todayIso)
                val overdueAmt = LoanCalculator.overdueInterestAmount(customer, todayIso)
                BorrowerListCard(
                    customer = customer,
                    isOverdue = isOverdue,
                    overdueAmount = overdueAmt,
                    onClick = { onBorrowerClick(customer) },
                    onQuickPay = { onQuickPay(customer) },
                    onCall = { onCallBorrower(customer) },
                )
            }
        }
    }
}

@Composable
private fun FollowUpTabContent(
    state: LedgerUiState.Success,
    todayIso: String,
    onBorrowerClick: (Customer) -> Unit,
    onCollectInterest: (Customer) -> Unit,
    onCallBorrower: (Customer) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TrustBlueBorder),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.followUpCustomers.isEmpty()) EmeraldSoft else CrimsonSoft
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = if (state.followUpCustomers.isEmpty()) EmeraldCollected else CrimsonOverdue,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.followUpCustomers.isEmpty()) {
                                "All Payments On Track"
                            } else {
                                "${state.followUpCustomers.size} Follow-Up Due"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TrustNavy,
                        )
                        Text(
                            text = "Outstanding interest: ${LoanCalculator.formatCurrency(state.kpis.overdueInterestAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.kpis.overdueInterestAmount > 0) CrimsonOverdue else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (state.followUpCustomers.isEmpty()) {
            item {
                EmptyLedgerCard(
                    title = "No Overdue Accounts",
                    subtitle = "All borrowers are up to date without delay.",
                    showAddButton = false,
                    onAddBorrower = {},
                )
            }
        } else {
            items(state.followUpCustomers, key = { it.id }) { customer ->
                val isOverdue = LoanCalculator.isInterestOverdue(customer, todayIso)
                val overdueAmt = LoanCalculator.overdueInterestAmount(customer, todayIso)
                BorrowerListCard(
                    customer = customer,
                    isOverdue = isOverdue,
                    overdueAmount = overdueAmt,
                    onClick = { onBorrowerClick(customer) },
                    onQuickPay = { onCollectInterest(customer) },
                    onCall = { onCallBorrower(customer) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionsTabContent(
    state: LedgerUiState.Success,
    searchQuery: String,
    selectedType: TransactionType?,
    selectedMode: PaymentMode?,
    onSearchChange: (String) -> Unit,
    onTypeChange: (TransactionType?) -> Unit,
    onModeChange: (PaymentMode?) -> Unit,
    onExportClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TrustBlueBorder),
                shadowElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Transactions Ledger", style = MaterialTheme.typography.titleLarge, color = TrustBlue)
                        Text(
                            text = "${state.filteredTransactions.size} chronological entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = onExportClick,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("ledger_export_button"),
                    ) {
                        Text("Export", color = TrustBlue)
                    }
                }
            }
        }

        item {
            SleekSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Search customer, TXN ID, notes…",
                modifier = Modifier.testTag("ledger_search_input"),
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SleekFilterChip(
                        selected = selectedType == null,
                        onClick = { onTypeChange(null) },
                        label = "All Types",
                    )
                }
                items(TransactionType.entries) { type ->
                    SleekFilterChip(
                        selected = selectedType == type,
                        onClick = { onTypeChange(if (selectedType == type) null else type) },
                        label = type.label,
                    )
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SleekFilterChip(
                    selected = selectedMode == null,
                    onClick = { onModeChange(null) },
                    label = "All Modes",
                )
                PaymentMode.entries.forEach { mode ->
                    SleekFilterChip(
                        selected = selectedMode == mode,
                        onClick = { onModeChange(if (selectedMode == mode) null else mode) },
                        label = mode.label,
                    )
                }
            }
        }

        if (state.filteredTransactions.isEmpty()) {
            item {
                EmptyLedgerCard(
                    title = "No Ledger Entries Yet",
                    subtitle = "Disbursements, monthly interest payments, top-ups, and repayments appear here.",
                    showAddButton = false,
                    onAddBorrower = {},
                )
            }
        } else {
            items(state.filteredTransactions, key = { it.id }) { txn ->
                TransactionRowCard(transaction = txn)
            }
        }
    }
}

@Composable
private fun SleekSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(999.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = TrustBlueWash,
            focusedBorderColor = TrustBlue,
            unfocusedBorderColor = TrustBlueBorder,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun StatusFilterPillRow(
    statusFilter: StatusFilter,
    onStatusFilterChange: (StatusFilter) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(StatusFilter.entries) { filter ->
            SleekFilterChip(
                selected = statusFilter == filter,
                onClick = { onStatusFilterChange(filter) },
                label = filter.label,
                modifier = Modifier.testTag("status_filter_${filter.name.lowercase()}"),
            )
        }
    }
}

@Composable
private fun SleekFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        },
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = TrustNavy,
            selectedContainerColor = TrustBlueSoft,
            selectedLabelColor = TrustBlue,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = TrustBlueBorder,
            selectedBorderColor = TrustBlue,
        ),
        modifier = modifier,
    )
}

@Composable
private fun EmptyLedgerCard(
    title: String,
    subtitle: String,
    showAddButton: Boolean,
    onAddBorrower: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TrustBlueBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(TrustBlueSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = TrustBlue,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (showAddButton) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAddBorrower,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.testTag("empty_add_borrower_button"),
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Loan")
                }
            }
        }
    }
}
