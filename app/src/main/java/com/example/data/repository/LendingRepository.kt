package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultBorrowersSeed
import com.example.data.local.LendingDao
import com.example.data.model.AccountStatus
import com.example.data.model.Borrower
import com.example.data.model.BorrowerWithTransactions
import com.example.data.model.NewCustomerInput
import com.example.data.model.RecordTransactionInput
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.domain.ExcelWorkbookExporter
import com.example.domain.LoanCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class LendingRepository(
    private val dao: LendingDao,
    private val appContext: Context? = null,
) {
    constructor(context: Context) : this(
        dao = AppDatabase.getInstance(context.applicationContext).lendingDao(),
        appContext = context.applicationContext,
    )

    private val seedMutex = Mutex()

    fun observeCustomers(): Flow<List<Borrower>> = dao.observeBorrowers()

    fun observeTransactions(): Flow<List<Transaction>> = dao.observeTransactions()

    fun observeTransactionsForBorrower(borrowerId: String): Flow<List<Transaction>> =
        dao.observeTransactionsForBorrower(borrowerId)

    fun observeBorrowerWithHistory(borrowerId: String): Flow<BorrowerWithTransactions?> =
        dao.observeBorrowerWithHistory(borrowerId)

    fun observeActivePrincipalTotal(): Flow<Double> = dao.observeActivePrincipalTotal()

    fun observeMonthlyInterestExpectedTotal(): Flow<Double> = dao.observeMonthlyInterestExpectedTotal()

    fun observeTotalInterestCollected(): Flow<Double> = dao.observeTotalInterestCollected()

    suspend fun ensureDefaultSeeded() {
        seedMutex.withLock {
            val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val alreadySeeded = prefs?.getBoolean(KEY_SEEDED_V3, false) ?: false
            if (!alreadySeeded || dao.getBorrowerCount() == 0) {
                if (dao.getBorrowerCount() == 0) {
                    val (borrowers, transactions) = DefaultBorrowersSeed.buildSeedData()
                    dao.seedDefaultData(borrowers, transactions)
                }
                prefs?.edit()?.putBoolean(KEY_SEEDED_V3, true)?.apply()
            }
            syncWorkbookFile()
        }
    }

    suspend fun getCustomerById(customerId: String): Borrower? = dao.getBorrowerById(customerId)

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

            val borrower = Borrower(
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
            val disbursement = Transaction(
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

            dao.createBorrowerWithInitialDisbursement(borrower, disbursement)
            syncWorkbookFile()
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
            dao.applyLoanTransaction(input = input, transactionId = transactionId)
            syncWorkbookFile()
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
        @Suppress("UNUSED_PARAMETER") currentActivePrincipal: Double,
    ): Result<Unit> {
        return try {
            dao.updateBorrowerTerms(
                borrowerId = customerId,
                name = name,
                phoneNumber = phoneNumber,
                dueDayOfMonth = dueDayOfMonth,
                monthlyInterestRate = monthlyInterestRate,
                status = status,
                followUpNotes = followUpNotes,
            )
            syncWorkbookFile()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return try {
            dao.deleteBorrowerById(customerId)
            syncWorkbookFile()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncWorkbookFile() {
        val ctx = appContext ?: return
        withContext(Dispatchers.IO) {
            val borrowers = dao.getAllBorrowers()
            val transactions = dao.getAllTransactions()
            val today = LoanCalculator.todayIso()
            val kpis = LoanCalculator.computePortfolioKpis(borrowers, transactions, today)
            val summaries = LoanCalculator.buildMonthlyCollectionSummaries(transactions)
            ExcelWorkbookExporter.syncWorkbookToAppStorage(
                context = ctx,
                customers = borrowers,
                transactions = transactions,
                kpis = kpis,
                summaries = summaries,
                referenceDateIso = today,
            )
        }
    }

    private companion object {
        const val PREFS_NAME = "lendflow_seed_prefs"
        const val KEY_SEEDED_V3 = "seeded_default_tamil_v3"
    }
}
