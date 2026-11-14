package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class AccountStatus(val label: String) {
    ACTIVE("Active"),
    FOLLOW_UP_NEEDED("Follow-up Needed"),
    CLOSED("Closed");

    companion object {
        fun fromLabel(value: String): AccountStatus =
            entries.firstOrNull { it.label == value } ?: ACTIVE
    }
}

enum class TransactionType(val label: String) {
    INITIAL_DISBURSEMENT("Initial Disbursement"),
    MONTHLY_INTEREST_PAYMENT("Monthly Interest Payment"),
    TOP_UP("Top-Up"),
    PRINCIPAL_REPAYMENT("Principal Repayment"),
    FULL_CLOSURE("Full Closure");

    companion object {
        fun fromLabel(value: String): TransactionType =
            entries.firstOrNull { it.label == value } ?: MONTHLY_INTEREST_PAYMENT
    }
}

enum class PaymentMode(val label: String) {
    UPI("UPI"),
    CASH("Cash"),
    BANK_TRANSFER("Bank Transfer"),
    CHEQUE("Cheque");

    companion object {
        fun fromLabel(value: String): PaymentMode =
            entries.firstOrNull { it.label == value } ?: UPI
    }
}

@Entity(tableName = "borrowers")
data class Borrower(
    @PrimaryKey val id: String = "",
    val customerCode: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val disbursementDate: String = "",
    val dueDayOfMonth: Long = 1L,
    val initialPrincipal: Double = 0.0,
    val extraPrincipalAdded: Double = 0.0,
    val principalRepaid: Double = 0.0,
    val currentActivePrincipal: Double = 0.0,
    val monthlyInterestRate: Double = 2.0,
    val monthlyInterestDue: Double = 0.0,
    val outstandingInterest: Double = 0.0,
    val status: String = AccountStatus.ACTIVE.label,
    val lastPaymentDate: String = "",
    val lastInterestPaidMonth: String = "",
    val followUpNotes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Borrower::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["transactionDate"]),
    ],
)
data class Transaction(
    @PrimaryKey val id: String = "",
    val transactionCode: String = "",
    val customerId: String = "",
    val customerCode: String = "",
    val customerName: String = "",
    val transactionType: String = TransactionType.MONTHLY_INTEREST_PAYMENT.label,
    val amount: Double = 0.0,
    val paymentMode: String = PaymentMode.UPI.label,
    val transactionDate: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Room 1-to-many relation pairing a Borrower with their chronological Transaction history.
 */
data class BorrowerWithTransactions(
    @Embedded val borrower: Borrower,
    @Relation(
        parentColumn = "id",
        entityColumn = "customerId",
    )
    val transactions: List<Transaction>,
)

typealias Customer = Borrower
typealias LoanTransaction = Transaction

data class NewCustomerInput(
    val customerCode: String,
    val name: String,
    val phoneNumber: String,
    val disbursementDate: String,
    val dueDayOfMonth: Int,
    val initialPrincipal: Double,
    val monthlyInterestRate: Double = 2.0,
    val paymentMode: PaymentMode = PaymentMode.BANK_TRANSFER,
    val followUpNotes: String = "",
)

data class RecordTransactionInput(
    val customerId: String,
    val transactionType: TransactionType,
    val amount: Double,
    val paymentMode: PaymentMode,
    val transactionDate: String,
    val notes: String = "",
)

data class TillDateUnpaidBorrower(
    val customer: Borrower,
    val expectedAmount: Double,
    val paidAmount: Double,
    val unpaidAmount: Double,
)

data class PortfolioKpis(
    val activeLoanPrincipal: Double = 0.0,
    val monthlyInterestExpected: Double = 0.0,
    val totalInterestCollected: Double = 0.0,
    val overdueInterestAmount: Double = 0.0,
    val activeBorrowersCount: Int = 0,
    val followUpCount: Int = 0,
    val closedCount: Int = 0,
    val monthTillDateExpected: Double = 0.0,
    val monthTillDateReceived: Double = 0.0,
    val monthTillDateUnpaid: Double = 0.0,
    val monthTillDateDueBorrowersCount: Int = 0,
    val monthTillDatePaidBorrowersCount: Int = 0,
    val monthTillDateUnpaidBorrowersCount: Int = 0,
    val monthTotalCollectedThisMonth: Double = 0.0,
)
