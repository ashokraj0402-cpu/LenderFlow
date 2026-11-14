package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultBorrowersSeed
import com.example.data.local.LendingDao
import com.example.data.model.AccountStatus
import com.example.data.model.Customer
import com.example.data.model.LoanTransaction
import com.example.data.model.NewCustomerInput
import com.example.data.model.RecordTransactionInput
import com.example.data.model.TransactionType
import com.example.domain.LoanCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.math.max

class LendingRepository(
    private val dao: LendingDao,
    private val appContext: Context? = null,
) {
    constructor(context: Context) : this(
        dao = AppDatabase.getInstance(context.applicationContext).lendingDao(),
        appContext = context.applicationContext,
    )

    private val seedMutex = Mutex()

    fun observeCustomers(): Flow<List<Customer>> = dao.observeCustomers()

    fun observeTransactions(): Flow<List<LoanTransaction>> = dao.observeTransactions()

    suspend fun ensureDefaultSeeded() {
        seedMutex.withLock {
            val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val alreadySeeded = prefs?.getBoolean(KEY_SEEDED_V1, false) ?: false
            if (alreadySeeded) return

            if (dao.getCustomerCount() == 0) {
                val (customers, transactions) = DefaultBorrowersSeed.buildSeedData()
                dao.seedDefaultData(customers, transactions)
            }
            prefs?.edit()?.putBoolean(KEY_SEEDED_V1, true)?.apply()
        }
    }

    suspend fun getCustomerById(customerId: String): Customer? = dao.getCustomerById(customerId)

    suspend fun createCustomerWithDisbursement(
        input: NewCustomerInput,
        customerId: String = UUID.randomUUID().toString(),
        transactionId: String = UUID.randomUUID().toString(),
    ): Result<String> {
        return try {
            val now = System.currentTimeMillis()
            val monthlyDue = LoanCalculator.calculateMonthlyInterest(
                principal = input.initialPrincipal,
                ratePercent = input.monthlyInterestRate,
            )

            val customer = Customer(
                id = customerId,
                customerCode = input.customerCode.trim(),
                name = input.name.trim(),
                phoneNumber = input.phoneNumber.trim().ifBlank { "—" },
                disbursementDate = input.disbursementDate,
                dueDayOfMonth = input.dueDayOfMonth.toLong(),
                initialPrincipal = input.initialPrincipal,
                extraPrincipalAdded = 0.0,
                principalRepaid = 0.0,
                currentActivePrincipal = input.initialPrincipal,
                monthlyInterestRate = input.monthlyInterestRate,
                monthlyInterestDue = monthlyDue,
                status = AccountStatus.ACTIVE.label,
                lastPaymentDate = "",
                lastInterestPaidMonth = "",
                followUpNotes = input.followUpNotes.trim(),
                createdAtMillis = now,
                updatedAtMillis = now,
            )

            val txnCode = "TXN-${transactionId.take(6).uppercase()}"
            val transaction = LoanTransaction(
                id = transactionId,
                transactionCode = txnCode,
                customerId = customerId,
                customerCode = input.customerCode.trim(),
                customerName = input.name.trim(),
                transactionType = TransactionType.INITIAL_DISBURSEMENT.label,
                amount = input.initialPrincipal,
                paymentMode = input.paymentMode.label,
                transactionDate = input.disbursementDate,
                notes = input.followUpNotes.trim().ifBlank { "Initial loan disbursement" },
                createdAtMillis = now,
            )

            dao.insertCustomerWithDisbursement(customer, transaction)
            Result.success(customerId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordTransaction(
        input: RecordTransactionInput,
        transactionId: String = UUID.randomUUID().toString(),
    ): Result<String> {
        return try {
            val customer = dao.getCustomerById(input.customerId)
                ?: throw IllegalStateException("Borrower account not found.")

            require(customer.status != AccountStatus.CLOSED.label) {
                "Cannot log transactions on a closed borrower account."
            }
            require(input.amount > 0.0) {
                "Transaction amount must be greater than zero."
            }

            var newExtraAdded = customer.extraPrincipalAdded
            var newRepaid = customer.principalRepaid
            var newCurrentPrincipal = customer.currentActivePrincipal
            var newMonthlyInterestDue = customer.monthlyInterestDue
            var newStatus = customer.status
            var newLastPaymentDate = customer.lastPaymentDate
            var newLastInterestPaidMonth = customer.lastInterestPaidMonth
            var newOutstandingInterest = customer.outstandingInterest

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
                        customer.initialPrincipal + newExtraAdded - newRepaid,
                    )
                    newMonthlyInterestDue = LoanCalculator.calculateMonthlyInterest(
                        principal = newCurrentPrincipal,
                        ratePercent = customer.monthlyInterestRate,
                    )
                }

                TransactionType.PRINCIPAL_REPAYMENT -> {
                    require(input.amount <= customer.currentActivePrincipal + 0.01) {
                        "Repayment cannot exceed current active principal (${LoanCalculator.formatCurrency(customer.currentActivePrincipal)})."
                    }
                    newRepaid += input.amount
                    newCurrentPrincipal = max(
                        0.0,
                        customer.initialPrincipal + newExtraAdded - newRepaid,
                    )
                    newMonthlyInterestDue = LoanCalculator.calculateMonthlyInterest(
                        principal = newCurrentPrincipal,
                        ratePercent = customer.monthlyInterestRate,
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
                    newRepaid = customer.initialPrincipal + newExtraAdded
                    newCurrentPrincipal = 0.0
                    newMonthlyInterestDue = 0.0
                    newOutstandingInterest = 0.0
                    newStatus = AccountStatus.CLOSED.label
                    newLastPaymentDate = input.transactionDate
                    newLastInterestPaidMonth = txnYearMonth
                }
            }

            val now = System.currentTimeMillis()
            val updatedCustomer = customer.copy(
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
            val transaction = LoanTransaction(
                id = transactionId,
                transactionCode = txnCode,
                customerId = customer.id,
                customerCode = customer.customerCode,
                customerName = customer.name,
                transactionType = input.transactionType.label,
                amount = input.amount,
                paymentMode = input.paymentMode.label,
                transactionDate = input.transactionDate,
                notes = input.notes.trim(),
                createdAtMillis = now,
            )

            dao.updateCustomerAndInsertTransaction(updatedCustomer, transaction)
            Result.success(transactionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerProfile(
        customerId: String,
        name: String,
        phoneNumber: String,
        dueDayOfMonth: Int,
        monthlyInterestRate: Double,
        status: AccountStatus,
        followUpNotes: String,
        currentActivePrincipal: Double,
    ): Result<Unit> {
        return try {
            val existing = dao.getCustomerById(customerId)
                ?: throw IllegalStateException("Borrower account not found.")
            val recalculatedInterest = if (status == AccountStatus.CLOSED) {
                0.0
            } else {
                LoanCalculator.calculateMonthlyInterest(currentActivePrincipal, monthlyInterestRate)
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
            dao.updateCustomer(updated)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return try {
            dao.deleteCustomerById(customerId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val PREFS_NAME = "lendflow_seed_prefs"
        const val KEY_SEEDED_V1 = "seeded_default_tamil_v2"
    }
}
