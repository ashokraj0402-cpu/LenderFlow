package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AccountStatus
import com.example.data.model.Customer
import com.example.data.model.LoanTransaction
import com.example.data.model.PaymentMode
import com.example.data.model.TransactionType
import com.example.domain.LoanCalculator
import java.util.Locale

@Database(
    entities = [Customer::class, LoanTransaction::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lendingDao(): LendingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lendflow_ledger.db",
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

object DefaultBorrowersSeed {

    private data class SeedSpec(
        val code: String,
        val name: String,
        val principal: Double,
        val dueDay: Long,
        val outstandingInterest: Double = 0.0,
        val status: AccountStatus = AccountStatus.ACTIVE,
        val lastPaidMonth: String,
        val lastPaymentDate: String,
        val notes: String,
    )

    private val defaultSpecs = listOf(
        SeedSpec(
            code = "CUST-1001",
            name = "மணிமாறன்",
            principal = 100000.0,
            dueDay = 15L,
            lastPaidMonth = "2026-10",
            lastPaymentDate = "2026-10-15",
            notes = "ஒழுங்காக செலுத்துபவர் (தேதி 15)",
        ),
        SeedSpec(
            code = "CUST-1002",
            name = "இளங்கோ",
            principal = 100000.0,
            dueDay = 20L,
            lastPaidMonth = "2026-10",
            lastPaymentDate = "2026-10-20",
            notes = "ஒழுங்காக செலுத்துபவர் (தேதி 20)",
        ),
        SeedSpec(
            code = "CUST-1003",
            name = "குமரன்",
            principal = 200000.0,
            dueDay = 1L,
            outstandingInterest = 11000.0,
            status = AccountStatus.FOLLOW_UP_NEEDED,
            lastPaidMonth = "2026-11",
            lastPaymentDate = "2026-08-01",
            notes = "நிலுவை வட்டி: ₹11,000 செலுத்த வேண்டும்",
        ),
        SeedSpec(
            code = "CUST-1004",
            name = "பால்பண்ணை",
            principal = 900000.0,
            dueDay = 15L,
            lastPaidMonth = "2026-10",
            lastPaymentDate = "2026-10-15",
            notes = "ஒழுங்காக செலுத்துபவர் (தேதி 15)",
        ),
        SeedSpec(
            code = "CUST-1005",
            name = "லதா ராஜா",
            principal = 100000.0,
            dueDay = 20L,
            lastPaidMonth = "2026-10",
            lastPaymentDate = "2026-10-20",
            notes = "ஒழுங்காக செலுத்துபவர் (தேதி 20)",
        ),
        SeedSpec(
            code = "CUST-1006",
            name = "கிருஷ்ணன் (1)",
            principal = 150000.0,
            dueDay = 2L,
            lastPaidMonth = "2026-11",
            lastPaymentDate = "2026-11-02",
            notes = "நவம்பர் வட்டி செலுத்தப்பட்டது (தேதி 2)",
        ),
        SeedSpec(
            code = "CUST-1007",
            name = "கிருஷ்ணன் (2)",
            principal = 50000.0,
            dueDay = 20L,
            lastPaidMonth = "2026-10",
            lastPaymentDate = "2026-10-20",
            notes = "ஒழுங்காக செலுத்துபவர் (தேதி 20)",
        ),
        SeedSpec(
            code = "CUST-1008",
            name = "ராம்குமார்",
            principal = 100000.0,
            dueDay = 1L,
            lastPaidMonth = "2026-11",
            lastPaymentDate = "2026-11-01",
            notes = "நவம்பர் வட்டி செலுத்தப்பட்டது (தேதி 1)",
        ),
        SeedSpec(
            code = "CUST-1009",
            name = "பாக்கியம்",
            principal = 200000.0,
            dueDay = 20L,
            lastPaidMonth = "2026-10",
            lastPaymentDate = "2026-10-20",
            notes = "ஒழுங்காக செலுத்துபவர் (தேதி 20)",
        ),
    )

    fun buildSeedData(): Pair<List<Customer>, List<LoanTransaction>> {
        val baseTime = 1760000000000L
        val customers = mutableListOf<Customer>()
        val transactions = mutableListOf<LoanTransaction>()
        var txnCounter = 1001

        defaultSpecs.forEachIndexed { index, spec ->
            val customerId = "seed_cust_${index + 1}"
            val dayStr = String.format(Locale.US, "%02d", spec.dueDay)
            val disbDate = if (spec.outstandingInterest > 0.0) "2026-07-$dayStr" else "2026-10-$dayStr"
            val monthlyDue = LoanCalculator.calculateMonthlyInterest(spec.principal, 2.0)
            val timestamp = baseTime + (index * 1000L)

            customers.add(
                Customer(
                    id = customerId,
                    customerCode = spec.code,
                    name = spec.name,
                    phoneNumber = "—",
                    disbursementDate = disbDate,
                    dueDayOfMonth = spec.dueDay,
                    initialPrincipal = spec.principal,
                    extraPrincipalAdded = 0.0,
                    principalRepaid = 0.0,
                    currentActivePrincipal = spec.principal,
                    monthlyInterestRate = 2.0,
                    monthlyInterestDue = monthlyDue,
                    outstandingInterest = spec.outstandingInterest,
                    status = spec.status.label,
                    lastPaymentDate = spec.lastPaymentDate,
                    lastInterestPaidMonth = spec.lastPaidMonth,
                    followUpNotes = spec.notes,
                    createdAtMillis = timestamp,
                    updatedAtMillis = timestamp,
                )
            )

            transactions.add(
                LoanTransaction(
                    id = "seed_txn_${txnCounter}",
                    transactionCode = "TXN-$txnCounter",
                    customerId = customerId,
                    customerCode = spec.code,
                    customerName = spec.name,
                    transactionType = TransactionType.INITIAL_DISBURSEMENT.label,
                    amount = spec.principal,
                    paymentMode = PaymentMode.CASH.label,
                    transactionDate = disbDate,
                    notes = "ஆரம்ப அசல் தொகை (வட்டி: ${LoanCalculator.formatCurrency(monthlyDue)}/மாதம்)",
                    createdAtMillis = timestamp,
                )
            )
            txnCounter++

            if (spec.lastPaidMonth == "2026-11" && spec.outstandingInterest == 0.0) {
                transactions.add(
                    LoanTransaction(
                        id = "seed_txn_${txnCounter}",
                        transactionCode = "TXN-$txnCounter",
                        customerId = customerId,
                        customerCode = spec.code,
                        customerName = spec.name,
                        transactionType = TransactionType.MONTHLY_INTEREST_PAYMENT.label,
                        amount = monthlyDue,
                        paymentMode = PaymentMode.UPI.label,
                        transactionDate = spec.lastPaymentDate,
                        notes = "நவம்பர் மாத வட்டி பெறப்பட்டது",
                        createdAtMillis = timestamp + 500_000L,
                    )
                )
                txnCounter++
            }
        }

        return customers to transactions
    }
}
