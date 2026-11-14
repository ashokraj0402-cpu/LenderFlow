package com.example.ui.modals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AccountStatus
import com.example.data.model.Customer
import com.example.data.model.LoanTransaction
import com.example.data.model.NewCustomerInput
import com.example.data.model.PaymentMode
import com.example.data.model.RecordTransactionInput
import com.example.data.model.TransactionType
import com.example.domain.LoanCalculator
import com.example.domain.MonthlySummary
import com.example.ui.components.StatusBadge
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.AmberFollowUp
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.CrimsonOverdue
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.EmeraldCollected
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.TrustBlue
import com.example.ui.theme.TrustBlueSoft
import com.example.ui.theme.TrustNavy
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickPaymentModal(
    customers: List<Customer>,
    initialCustomer: Customer?,
    initialType: TransactionType = TransactionType.MONTHLY_INTEREST_PAYMENT,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (RecordTransactionInput) -> Unit,
) {
    val eligibleCustomers = remember(customers) {
        customers.filter { it.status != AccountStatus.CLOSED.label }
    }
    var selectedCustomer by remember(initialCustomer, eligibleCustomers) {
        mutableStateOf(
            initialCustomer?.takeIf { it.status != AccountStatus.CLOSED.label }
                ?: eligibleCustomers.firstOrNull()
        )
    }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val availableTypes = remember {
        listOf(
            TransactionType.MONTHLY_INTEREST_PAYMENT,
            TransactionType.TOP_UP,
            TransactionType.PRINCIPAL_REPAYMENT,
            TransactionType.FULL_CLOSURE,
        )
    }
    var selectedType by remember(initialType) { mutableStateOf(initialType) }

    fun defaultAmountFor(customer: Customer?, type: TransactionType): String {
        if (customer == null) return ""
        return when (type) {
            TransactionType.MONTHLY_INTEREST_PAYMENT -> {
                val target = if (customer.outstandingInterest > 0.0) {
                    customer.outstandingInterest
                } else {
                    customer.monthlyInterestDue
                }
                if (target > 0) target.toLong().toString() else ""
            }
            TransactionType.FULL_CLOSURE ->
                if (customer.currentActivePrincipal > 0) customer.currentActivePrincipal.toLong().toString() else ""
            else -> ""
        }
    }

    var amountText by remember(selectedCustomer?.id, selectedType) {
        mutableStateOf(defaultAmountFor(selectedCustomer, selectedType))
    }
    var selectedMode by remember { mutableStateOf(PaymentMode.UPI) }
    var dateText by remember { mutableStateOf(LoanCalculator.todayIso()) }
    var notesText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Quick Payment & Adjustment",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Updates borrower principal & 2% monthly interest live",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close quick payment")
                }
            }

            if (eligibleCustomers.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No active borrower accounts available. Add a new borrower first to log payments or top-ups.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { customerDropdownExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_pay_customer_selector"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = selectedCustomer?.let { "${it.name} (${it.customerCode})" }
                                        ?: "Select Borrower",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                selectedCustomer?.let {
                                    Text(
                                        text = "Principal: ${LoanCalculator.formatCurrency(it.currentActivePrincipal)} • Interest: ${LoanCalculator.formatCurrency(it.monthlyInterestDue)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select borrower")
                        }
                    }

                    DropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false },
                    ) {
                        eligibleCustomers.forEach { customer ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "${customer.name} (${customer.customerCode})",
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            text = "Principal ${LoanCalculator.formatCurrency(customer.currentActivePrincipal)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCustomer = customer
                                    amountText = defaultAmountFor(customer, selectedType)
                                    customerDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    availableTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                amountText = defaultAmountFor(selectedCustomer, type)
                                validationError = null
                            },
                            label = { Text(type.label) },
                            modifier = Modifier.testTag("txn_type_chip_${type.name.lowercase()}"),
                        )
                    }
                }

                val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                val currentCustomer = selectedCustomer
                if (currentCustomer != null && parsedAmount > 0.0) {
                    val previewPrincipal = when (selectedType) {
                        TransactionType.MONTHLY_INTEREST_PAYMENT -> currentCustomer.currentActivePrincipal
                        TransactionType.TOP_UP -> currentCustomer.currentActivePrincipal + parsedAmount
                        TransactionType.PRINCIPAL_REPAYMENT -> max(0.0, currentCustomer.currentActivePrincipal - parsedAmount)
                        TransactionType.FULL_CLOSURE -> 0.0
                        TransactionType.INITIAL_DISBURSEMENT -> currentCustomer.currentActivePrincipal
                    }
                    val previewInterest = LoanCalculator.calculateMonthlyInterest(
                        previewPrincipal,
                        currentCustomer.monthlyInterestRate,
                    )
                    Surface(
                        color = TrustBlueSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("New Active Principal", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = LoanCalculator.formatCurrency(previewPrincipal),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                    color = TrustNavy,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("New Monthly Interest", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = LoanCalculator.formatCurrency(previewInterest),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                    color = EmeraldCollected,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationError = null
                    },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_pay_amount_input"),
                )

                Text(
                    text = "Payment Mode",
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("payment_mode_${mode.name.lowercase()}"),
                        )
                    }
                }

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_pay_date_input"),
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_pay_notes_input"),
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = CrimsonOverdue,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Button(
                    onClick = {
                        val customer = selectedCustomer
                        val amount = amountText.toDoubleOrNull()
                        when {
                            customer == null -> validationError = "Please select a borrower."
                            amount == null || amount <= 0.0 -> validationError = "Enter a valid positive amount."
                            selectedType == TransactionType.PRINCIPAL_REPAYMENT && amount > customer.currentActivePrincipal + 0.01 ->
                                validationError = "Repayment cannot exceed ${LoanCalculator.formatCurrency(customer.currentActivePrincipal)}."
                            dateText.trim().length != 10 ->
                                validationError = "Enter date in YYYY-MM-DD format."
                            else -> onSubmit(
                                RecordTransactionInput(
                                    customerId = customer.id,
                                    transactionType = selectedType,
                                    amount = amount,
                                    paymentMode = selectedMode,
                                    transactionDate = dateText.trim(),
                                    notes = notesText,
                                )
                            )
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_transaction_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Confirm ${selectedType.label}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddBorrowerModal(
    suggestedCustomerCode: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (NewCustomerInput) -> Unit,
) {
    var code by remember(suggestedCustomerCode) { mutableStateOf(suggestedCustomerCode) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var principalText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("2.0") }
    var dueDayText by remember { mutableStateOf("5") }
    var disbursementDate by remember { mutableStateOf(LoanCalculator.todayIso()) }
    var paymentMode by remember { mutableStateOf(PaymentMode.BANK_TRANSFER) }
    var notes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("New Borrower & Disbursement", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Creates borrower profile & logs Initial Disbursement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close add borrower")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Customer ID") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_borrower_code"),
                )
                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it },
                    label = { Text("Due Day (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_borrower_due_day"),
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    validationError = null
                },
                label = { Text("Borrower Full Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_borrower_name"),
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    validationError = null
                },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_borrower_phone"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = principalText,
                    onValueChange = {
                        principalText = it
                        validationError = null
                    },
                    label = { Text("Initial Principal (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("add_borrower_principal"),
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate %/mo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_borrower_rate"),
                )
            }

            val principalPreview = principalText.toDoubleOrNull() ?: 0.0
            val ratePreview = rateText.toDoubleOrNull() ?: 2.0
            if (principalPreview > 0.0) {
                Surface(
                    color = EmeraldSoft,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Expected Monthly Interest",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldCollected,
                        )
                        Text(
                            text = LoanCalculator.formatCurrency(
                                LoanCalculator.calculateMonthlyInterest(principalPreview, ratePreview)
                            ),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = EmeraldCollected,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = disbursementDate,
                onValueChange = { disbursementDate = it },
                label = { Text("Disbursement Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_borrower_date"),
            )

            Text("Disbursement Mode", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PaymentMode.entries.forEach { mode ->
                    FilterChip(
                        selected = paymentMode == mode,
                        onClick = { paymentMode = mode },
                        label = { Text(mode.label) },
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Follow-up Remarks / Security Notes") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_borrower_notes"),
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = CrimsonOverdue,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    val principal = principalText.toDoubleOrNull()
                    val rate = rateText.toDoubleOrNull() ?: 2.0
                    val dueDay = dueDayText.toIntOrNull()
                    when {
                        code.isBlank() -> validationError = "Customer ID is required."
                        name.isBlank() -> validationError = "Borrower name is required."
                        principal == null || principal <= 0.0 ->
                            validationError = "Enter a positive initial principal."
                        dueDay == null || dueDay !in 1..31 ->
                            validationError = "Due day must be between 1 and 31."
                        disbursementDate.trim().length != 10 ->
                            validationError = "Enter date in YYYY-MM-DD format."
                        else -> onSubmit(
                            NewCustomerInput(
                                customerCode = code.trim(),
                                name = name.trim(),
                                phoneNumber = phone.trim(),
                                disbursementDate = disbursementDate.trim(),
                                dueDayOfMonth = dueDay,
                                initialPrincipal = principal,
                                monthlyInterestRate = rate,
                                paymentMode = paymentMode,
                                followUpNotes = notes.trim(),
                            )
                        )
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_new_borrower_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Create Account & Disburse")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BorrowerDetailDialog(
    customer: Customer,
    customerTransactions: List<LoanTransaction>,
    onDismiss: () -> Unit,
    onRecordAction: (TransactionType) -> Unit,
    onEditCustomer: () -> Unit,
    onCallCustomer: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val today = LoanCalculator.todayIso()
    val isOverdue = LoanCalculator.isInterestOverdue(customer, today)
    val overdueAmount = LoanCalculator.overdueInterestAmount(customer, today)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customer.customerCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = TrustBlue,
                        )
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = customer.phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row {
                        IconButton(
                            onClick = onCallCustomer,
                            modifier = Modifier.testTag("detail_call_button"),
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Call borrower", tint = TrustNavy)
                        }
                        IconButton(
                            onClick = onEditCustomer,
                            modifier = Modifier.testTag("detail_edit_button"),
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit borrower", tint = TrustNavy)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close details")
                        }
                    }
                }

                StatusBadge(status = customer.status, isOverdue = isOverdue)

                if (isOverdue && overdueAmount > 0.0 && customer.status != AccountStatus.CLOSED.label) {
                    Surface(
                        color = CrimsonSoft,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CrimsonBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Overdue / Unpaid Monthly Interest: ${LoanCalculator.formatCurrency(overdueAmount)}",
                            color = CrimsonOverdue,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DetailMetricRow(
                            "Current Active Principal",
                            LoanCalculator.formatCurrency(customer.currentActivePrincipal),
                            highlight = true,
                        )
                        DetailMetricRow(
                            "Monthly Interest Due (${LoanCalculator.formatRate(customer.monthlyInterestRate)})",
                            LoanCalculator.formatCurrency(customer.monthlyInterestDue),
                            highlight = true,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        DetailMetricRow("Initial Principal", LoanCalculator.formatCurrency(customer.initialPrincipal))
                        DetailMetricRow("Extra Principal (Top-Ups)", LoanCalculator.formatCurrency(customer.extraPrincipalAdded))
                        DetailMetricRow("Principal Repaid", LoanCalculator.formatCurrency(customer.principalRepaid))
                        DetailMetricRow("Disbursement Date", LoanCalculator.formatDateDisplay(customer.disbursementDate))
                        DetailMetricRow("Monthly Due Day", "Day ${customer.dueDayOfMonth}")
                        DetailMetricRow(
                            "Last Payment Date",
                            if (customer.lastPaymentDate.isBlank()) "None yet" else LoanCalculator.formatDateDisplay(customer.lastPaymentDate)
                        )
                    }
                }

                if (customer.followUpNotes.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Follow-up Remarks", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = customer.followUpNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (customer.status != AccountStatus.CLOSED.label) {
                    Text("Quick Actions", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onRecordAction(TransactionType.MONTHLY_INTEREST_PAYMENT) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldCollected),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("detail_log_interest_button"),
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pay Interest")
                        }
                        FilledTonalButton(
                            onClick = { onRecordAction(TransactionType.TOP_UP) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("detail_top_up_button"),
                        ) {
                            Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Top-Up")
                        }
                        OutlinedButton(
                            onClick = { onRecordAction(TransactionType.PRINCIPAL_REPAYMENT) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("detail_repay_button"),
                        ) {
                            Text("Repay Principal")
                        }
                        OutlinedButton(
                            onClick = { onRecordAction(TransactionType.FULL_CLOSURE) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("detail_close_account_button"),
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Full Closure")
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(
                    text = "Borrower Ledger (${customerTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                )

                if (customerTransactions.isEmpty()) {
                    Text(
                        text = "No transactions logged yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        customerTransactions.forEach { txn ->
                            TransactionRowCard(transaction = txn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetricRow(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (highlight) {
                MaterialTheme.typography.titleSmall.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                MaterialTheme.typography.bodySmall.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                )
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditCustomerModal(
    customer: Customer,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        dueDay: Int,
        rate: Double,
        status: AccountStatus,
        notes: String,
    ) -> Unit,
) {
    var name by remember(customer) { mutableStateOf(customer.name) }
    var phone by remember(customer) { mutableStateOf(customer.phoneNumber) }
    var dueDayText by remember(customer) { mutableStateOf(customer.dueDayOfMonth.toString()) }
    var rateText by remember(customer) { mutableStateOf(customer.monthlyInterestRate.toString()) }
    var status by remember(customer) { mutableStateOf(AccountStatus.fromLabel(customer.status)) }
    var notes by remember(customer) { mutableStateOf(customer.followUpNotes) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Edit ${customer.customerCode}", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close edit borrower")
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Borrower Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it },
                    label = { Text("Due Day (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate %/mo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Text("Account Status", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AccountStatus.entries.forEach { option ->
                    FilterChip(
                        selected = status == option,
                        onClick = { status = option },
                        label = { Text(option.label) },
                        modifier = Modifier.testTag("edit_status_${option.name.lowercase()}"),
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Follow-up Remarks / Notes") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_follow_up_notes"),
            )

            if (errorMsg != null) {
                Text(errorMsg!!, color = CrimsonOverdue, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val dueDay = dueDayText.toIntOrNull()
                    val rate = rateText.toDoubleOrNull()
                    when {
                        name.isBlank() -> errorMsg = "Name is required."
                        dueDay == null || dueDay !in 1..31 -> errorMsg = "Due day must be 1–31."
                        rate == null || rate < 0.0 -> errorMsg = "Rate must be 0 or higher."
                        else -> onSave(name, phone, dueDay, rate, status, notes)
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_customer_edit_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSummaryModal(
    summaries: List<MonthlySummary>,
    totalCustomers: Int,
    totalTransactions: Int,
    onDismiss: () -> Unit,
    onShareCsv: () -> Unit,
    onCopyCsv: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Monthly Summary & Export", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "$totalCustomers borrowers • $totalTransactions ledger entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close summary")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onShareCsv,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("share_csv_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share CSV")
                }
                OutlinedButton(
                    onClick = onCopyCsv,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_csv_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy CSV")
                }
            }

            if (summaries.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No monthly collection records yet. Summaries appear automatically as transactions are logged.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                summaries.forEach { month ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = month.yearMonth,
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                )
                                Text(
                                    text = "${month.transactionCount} txns",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DetailMetricRow("Interest Collected", LoanCalculator.formatCurrency(month.interestCollected), highlight = true)
                            DetailMetricRow("Principal Repaid / Closed", LoanCalculator.formatCurrency(month.principalRepaid))
                            DetailMetricRow("Disbursed & Top-Ups", LoanCalculator.formatCurrency(month.disbursedAndTopUps))
                        }
                    }
                }
            }
        }
    }
}
