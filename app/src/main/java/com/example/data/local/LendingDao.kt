package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import androidx.room.Update
import com.example.data.model.AccountStatus
import com.example.data.model.Borrower
import com.example.data.model.BorrowerWithTransactions
import com.example.data.model.RecordTransactionInput
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.domain.LoanCalculator
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

@Dao
interface LendingDao {

    // -------------------------------------------------------------------------
    // Reactive Queries: Borrowers, Transaction History, and Live Aggregations
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM borrowers ORDER BY createdAtMillis DESC")
    fun observeBorrowers(): Flow<List<Borrower>>

    @Query("SELECT * FROM borrowers ORDER BY createdAtMillis DESC")
    fun observeCustomers(): Flow<List<Borrower>>

    @Query("SELECT * FROM borrowers WHERE status = :status ORDER BY dueDayOfMonth ASC")
    fun observeBorrowersByStatus(status: String): Flow<List<Borrower>>

    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, createdAtMillis DESC")
    fun observeTransactions(): Flow<List<Transaction>>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE customerId = :borrowerId " +
            "ORDER BY transactionDate DESC, createdAtMillis DESC"
    )
    fun observeTransactionsForBorrower(borrowerId: String): Flow<List<Transaction>>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE transactionType = :transactionType " +
            "ORDER BY transactionDate DESC, createdAtMillis DESC"
    )
    fun observeTransactionsByType(transactionType: String): Flow<List<Transaction>>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE transactionDate LIKE :yearMonthPrefix || '%' " +
            "ORDER BY transactionDate DESC, createdAtMillis DESC"
    )
    fun observeTransactionsInMonth(yearMonthPrefix: String): Flow<List<Transaction>>

    @RoomTransaction
    @Query("SELECT * FROM borrowers WHERE id = :borrowerId LIMIT 1")
    fun observeBorrowerWithHistory(borrowerId: String): Flow<BorrowerWithTransactions?>

    @RoomTransaction
    @Query("SELECT * FROM borrowers ORDER BY createdAtMillis DESC")
    fun observeAllBorrowersWithHistory(): Flow<List<BorrowerWithTransactions>>

    @Query("SELECT COALESCE(SUM(currentActivePrincipal), 0.0) FROM borrowers WHERE status != 'Closed'")
    fun observeActivePrincipalTotal(): Flow<Double>

    @Query("SELECT COALESCE(SUM(monthlyInterestDue), 0.0) FROM borrowers WHERE status != 'Closed'")
    fun observeMonthlyInterestExpectedTotal(): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amount), 0.0) FROM transactions " +
            "WHERE transactionType = 'Monthly Interest Payment'"
    )
    fun observeTotalInterestCollected(): Flow<Double>

    // -------------------------------------------------------------------------
    // Snapshot Queries
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM borrowers ORDER BY createdAtMillis DESC")
    suspend fun getAllBorrowers(): List<Borrower>

    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, createdAtMillis DESC")
    suspend fun getAllTransactions(): List<Transaction>

    @Query(
        "SELECT * FROM transactions " +
            "WHERE customerId = :borrowerId " +
            "ORDER BY transactionDate DESC, createdAtMillis DESC"
    )
    suspend fun getTransactionsForBorrower(borrowerId: String): List<Transaction>

    @Query("SELECT * FROM borrowers WHERE id = :borrowerId LIMIT 1")
    suspend fun getBorrowerById(borrowerId: String): Borrower?

    @Query("SELECT COUNT(*) FROM borrowers")
    suspend fun getBorrowerCount(): Int

    // -------------------------------------------------------------------------
    // Primitive Mutations
    // -------------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorrower(borrower: Borrower)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorrowers(borrowers: List<Borrower>)

    @Update
    suspend fun updateBorrower(borrower: Borrower)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Query("DELETE FROM borrowers WHERE id = :borrowerId")
    suspend fun deleteBorrowerById(borrowerId: String)

    // -------------------------------------------------------------------------
    // Atomic @Transaction Loan Calculations & History Workflows
    // -------------------------------------------------------------------------

    @RoomTransaction
    suspend fun createBorrowerWithInitialDisbursement(
        borrower: Borrower,
        disbursement: Transaction,
    ) {
        insertBorrower(borrower)
        insertTransaction(disbursement)
    }

    /**
     * Atomically applies a financial event (Monthly Interest Payment, Top-Up,
     * Principal Repayment, or Full Closure), recalculates Current Active Principal,
     * Monthly Interest Due, and Outstanding Interest, and appends the audit Transaction.
     */
    @RoomTransaction
    suspend fun applyLoanTransaction(
        input: RecordTransactionInput,
        transactionId: String,
    ): Transaction {
        val borrower = getBorrowerById(input.customerId)
            ?: throw IllegalStateException("Borrower account not found.")

        require(borrower.status != AccountStatus.CLOSED.label) {
            "Cannot log transactions on a closed borrower account."
        }
        require(input.amount > 0.0) {
            "Transaction amount must be greater than zero."
        }

        var newExtraAdded = borrower.extraPrincipalAdded
        var newRepaid = borrower.principalRepaid
        var newCurrentPrincipal = borrower.currentActivePrincipal
        var newMonthlyInterestDue = borrower.monthlyInterestDue
        var newStatus = borrower.status
        var newLastPaymentDate = borrower.lastPaymentDate
        var newLastInterestPaidMonth = borrower.lastInterestPaidMonth
        var newOutstandingInterest = borrower.outstandingInterest

        val txnYearMonth = if (input.transactionDate.length >= 7) {
            input.transactionDate.substring(0, 7)
        } else {
            LoanCalculator.currentYearMonth()
        }

        when (input.transactionType) {
            TransactionType.INITIAL_DISBURSEMENT -> {
                throw IllegalArgumentException("Use New Borrower flow for Initial Disbursement.")
            }

            TransactionType.MONTHLY_INTEREST_PAYMENT -> {
                newLastPaymentDate = input.transactionDate
                newLastInterestPaidMonth = txnYearMonth
                if (newOutstandingInterest > 0.0) {
                    newOutstandingInterest = max(0.0, newOutstandingInterest - input.amount)
                }
                if (newOutstandingInterest == 0.0 && newStatus == AccountStatus.FOLLOW_UP_NEEDED.label) {
                    newStatus = AccountStatus.ACTIVE.label
                }
            }

            TransactionType.TOP_UP -> {
                newExtraAdded += input.amount
                newCurrentPrincipal = max(
                    0.0,
                    borrower.initialPrincipal + newExtraAdded - newRepaid,
                )
                newMonthlyInterestDue = LoanCalculator.calculateMonthlyInterest(
                    principal = newCurrentPrincipal,
                    ratePercent = borrower.monthlyInterestRate,
                )
            }

            TransactionType.PRINCIPAL_REPAYMENT -> {
                require(input.amount <= borrower.currentActivePrincipal + 0.01) {
                    "Repayment cannot exceed current active principal (${LoanCalculator.formatCurrency(borrower.currentActivePrincipal)})."
                }
                newRepaid += input.amount
                newCurrentPrincipal = max(
                    0.0,
                    borrower.initialPrincipal + newExtraAdded - newRepaid,
                )
                newMonthlyInterestDue = LoanCalculator.calculateMonthlyInterest(
                    principal = newCurrentPrincipal,
                    ratePercent = borrower.monthlyInterestRate,
                )
                newLastPaymentDate = input.transactionDate
                if (newCurrentPrincipal <= 0.01) {
                    newCurrentPrincipal = 0.0
                    newMonthlyInterestDue = 0.0
                    newOutstandingInterest = 0.0
                    newStatus = AccountStatus.CLOSED.label
                }
            }

            TransactionType.FULL_CLOSURE -> {
                newRepaid = borrower.initialPrincipal + newExtraAdded
                newCurrentPrincipal = 0.0
                newMonthlyInterestDue = 0.0
                newOutstandingInterest = 0.0
                newStatus = AccountStatus.CLOSED.label
                newLastPaymentDate = input.transactionDate
                newLastInterestPaidMonth = txnYearMonth
            }
        }

        val now = System.currentTimeMillis()
        val updatedBorrower = borrower.copy(
            extraPrincipalAdded = newExtraAdded,
            principalRepaid = newRepaid,
            currentActivePrincipal = newCurrentPrincipal,
            monthlyInterestDue = newMonthlyInterestDue,
            outstandingInterest = newOutstandingInterest,
            status = newStatus,
            lastPaymentDate = newLastPaymentDate,
            lastInterestPaidMonth = newLastInterestPaidMonth,
            updatedAtMillis = now,
        )

        val txnCode = "TXN-${transactionId.take(6).uppercase()}"
        val transaction = Transaction(
            id = transactionId,
            transactionCode = txnCode,
            customerId = borrower.id,
            customerCode = borrower.customerCode,
            customerName = borrower.name,
            transactionType = input.transactionType.label,
            amount = input.amount,
            paymentMode = input.paymentMode.label,
            transactionDate = input.transactionDate,
            notes = input.notes.trim(),
            createdAtMillis = now,
        )

        updateBorrower(updatedBorrower)
        insertTransaction(transaction)
        return transaction
    }

    @RoomTransaction
    suspend fun updateBorrowerTerms(
        borrowerId: String,
        name: String,
        phoneNumber: String,
        dueDayOfMonth: Int,
        monthlyInterestRate: Double,
        status: AccountStatus,
        followUpNotes: String,
    ): Borrower {
        val existing = getBorrowerById(borrowerId)
            ?: throw IllegalStateException("Borrower account not found.")

        val recalculatedInterest = if (status == AccountStatus.CLOSED) {
            0.0
        } else {
            LoanCalculator.calculateMonthlyInterest(existing.currentActivePrincipal, monthlyInterestRate)
        }

        val updated = existing.copy(
            name = name.trim(),
            phoneNumber = phoneNumber.trim().ifBlank { "—" },
            dueDayOfMonth = dueDayOfMonth.toLong(),
            monthlyInterestRate = monthlyInterestRate,
            monthlyInterestDue = recalculatedInterest,
            status = status.label,
            followUpNotes = followUpNotes.trim(),
            updatedAtMillis = System.currentTimeMillis(),
        )
        updateBorrower(updated)
        return updated
    }

    @RoomTransaction
    suspend fun seedDefaultData(
        borrowers: List<Borrower>,
        transactions: List<Transaction>,
    ) {
        insertBorrowers(borrowers)
        insertTransactions(transactions)
    }
}
