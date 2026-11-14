package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountStatus
import com.example.data.model.Customer
import com.example.data.model.LoanTransaction
import com.example.data.model.NewCustomerInput
import com.example.data.model.PaymentMode
import com.example.data.model.PortfolioKpis
import com.example.data.model.RecordTransactionInput
import com.example.data.model.TillDateUnpaidBorrower
import com.example.data.model.TransactionType
import com.example.data.repository.LendingRepository
import com.example.domain.LoanCalculator
import com.example.domain.MonthlySummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatusFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    FOLLOW_UP("Follow-up Needed"),
    CLOSED("Closed"),
}

sealed interface LedgerUiState {
    data object Loading : LedgerUiState
    data class Success(
        val customers: List<Customer>,
        val filteredCustomers: List<Customer>,
        val followUpCustomers: List<Customer>,
        val tillDateUnpaidBorrowers: List<TillDateUnpaidBorrower>,
        val transactions: List<LoanTransaction>,
        val filteredTransactions: List<LoanTransaction>,
        val kpis: PortfolioKpis,
        val monthlySummaries: List<MonthlySummary>,
        val nextCustomerCode: String,
    ) : LedgerUiState

    data class Error(val message: String) : LedgerUiState
}

class LendingViewModel(
    private val repository: LendingRepository,
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.ensureDefaultSeeded()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter.asStateFlow()

    private val _txnTypeFilter = MutableStateFlow<TransactionType?>(null)
    val txnTypeFilter: StateFlow<TransactionType?> = _txnTypeFilter.asStateFlow()

    private val _txnModeFilter = MutableStateFlow<PaymentMode?>(null)
    val txnModeFilter: StateFlow<PaymentMode?> = _txnModeFilter.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val customersFlow = repository.observeCustomers()
    private val transactionsFlow = repository.observeTransactions()

    val uiState: StateFlow<LedgerUiState> = combine(
        customersFlow,
        transactionsFlow,
        _searchQuery,
        _statusFilter,
        combine(_txnTypeFilter, _txnModeFilter) { t, m -> t to m },
    ) { customers, transactions, query, statusFilter, (txnType, txnMode) ->
        val today = LoanCalculator.todayIso()
        val trimmedQuery = query.trim().lowercase()

        val filteredCustomers = customers.filter { customer ->
            val matchesStatus = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.ACTIVE -> customer.status == AccountStatus.ACTIVE.label
                StatusFilter.FOLLOW_UP ->
                    customer.status == AccountStatus.FOLLOW_UP_NEEDED.label ||
                        LoanCalculator.isInterestOverdue(customer, today)
                StatusFilter.CLOSED -> customer.status == AccountStatus.CLOSED.label
            }
            val matchesSearch = trimmedQuery.isEmpty() ||
                customer.name.lowercase().contains(trimmedQuery) ||
                customer.customerCode.lowercase().contains(trimmedQuery) ||
                customer.phoneNumber.lowercase().contains(trimmedQuery) ||
                customer.followUpNotes.lowercase().contains(trimmedQuery)
            matchesStatus && matchesSearch
        }.sortedWith(
            compareByDescending<Customer> { LoanCalculator.isInterestOverdue(it, today) }
                .thenBy { it.status == AccountStatus.CLOSED.label }
                .thenBy { it.dueDayOfMonth }
        )

        val followUpCustomers = customers.filter { customer ->
            customer.status != AccountStatus.CLOSED.label &&
                (customer.status == AccountStatus.FOLLOW_UP_NEEDED.label ||
                    LoanCalculator.isInterestOverdue(customer, today))
        }.sortedWith(
            compareByDescending<Customer> { LoanCalculator.overdueInterestAmount(it, today) }
                .thenBy { it.dueDayOfMonth }
        )

        val filteredTransactions = transactions.filter { txn ->
            val matchesType = txnType == null || txn.transactionType == txnType.label
            val matchesMode = txnMode == null || txn.paymentMode == txnMode.label
            val matchesSearch = trimmedQuery.isEmpty() ||
                txn.customerName.lowercase().contains(trimmedQuery) ||
                txn.customerCode.lowercase().contains(trimmedQuery) ||
                txn.transactionCode.lowercase().contains(trimmedQuery) ||
                txn.notes.lowercase().contains(trimmedQuery)
            matchesType && matchesMode && matchesSearch
        }

        val kpis = LoanCalculator.computePortfolioKpis(customers, transactions, today)
        val tillDateUnpaidBorrowers = LoanCalculator.computeTillDateUnpaidBorrowers(customers, transactions, today)
        val monthlySummaries = LoanCalculator.buildMonthlyCollectionSummaries(transactions)
        val nextNum = 1001 + customers.size
        val nextCode = "CUST-$nextNum"

        LedgerUiState.Success(
            customers = customers,
            filteredCustomers = filteredCustomers,
            followUpCustomers = followUpCustomers,
            tillDateUnpaidBorrowers = tillDateUnpaidBorrowers,
            transactions = transactions,
            filteredTransactions = filteredTransactions,
            kpis = kpis,
            monthlySummaries = monthlySummaries,
            nextCustomerCode = nextCode,
        ) as LedgerUiState
    }.catch { error ->
        if (error is CancellationException) throw error
        Log.w(TAG, "Error observing lending ledger", error)
        emit(LedgerUiState.Error(error.message ?: "Failed to load lending ledger"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = LedgerUiState.Loading,
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChange(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun onTransactionTypeFilterChange(type: TransactionType?) {
        _txnTypeFilter.value = type
    }

    fun onPaymentModeFilterChange(mode: PaymentMode?) {
        _txnModeFilter.value = mode
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun addBorrower(input: NewCustomerInput, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.createCustomerWithDisbursement(input)
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Added ${input.name} (${input.customerCode})"
                    onSuccess()
                },
                onFailure = { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Failed to add borrower"
                },
            )
        }
    }

    fun recordTransaction(input: RecordTransactionInput, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.recordTransaction(input)
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "${input.transactionType.label} recorded (${LoanCalculator.formatCurrency(input.amount)})"
                    onSuccess()
                },
                onFailure = { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Failed to record transaction"
                },
            )
        }
    }

    fun updateCustomerProfile(
        customer: Customer,
        name: String,
        phoneNumber: String,
        dueDayOfMonth: Int,
        monthlyInterestRate: Double,
        status: AccountStatus,
        followUpNotes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.updateCustomerProfile(
                customerId = customer.id,
                name = name,
                phoneNumber = phoneNumber,
                dueDayOfMonth = dueDayOfMonth,
                monthlyInterestRate = monthlyInterestRate,
                status = status,
                followUpNotes = followUpNotes,
                currentActivePrincipal = customer.currentActivePrincipal,
            )
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Updated $name's account"
                    onSuccess()
                },
                onFailure = { err ->
                    _feedbackMessage.value = err.localizedMessage ?: "Failed to update borrower"
                },
            )
        }
    }

    private companion object {
        const val TAG = "LendingVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
