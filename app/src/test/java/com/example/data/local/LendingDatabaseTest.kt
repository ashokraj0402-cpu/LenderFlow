package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AccountStatus
import com.example.data.model.PaymentMode
import com.example.data.model.RecordTransactionInput
import com.example.data.model.TransactionType
import com.example.data.repository.LendingRepository
import com.example.domain.LoanCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LendingDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: LendingRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LendingRepository(db.lendingDao(), appContext = null)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun defaultSeed_populatesNineTamilBorrowersWithKumaran11000OutstandingOnly() = runBlocking {
        repository.ensureDefaultSeeded()

        val customers = repository.observeCustomers().first()
        val transactions = repository.observeTransactions().first()

        assertEquals(9, customers.size)
        assertEquals(11, transactions.size)

        val kpis = LoanCalculator.computePortfolioKpis(customers, transactions, "2026-11-14")
        assertEquals(1900000.0, kpis.activeLoanPrincipal, 0.01)
        assertEquals(38000.0, kpis.monthlyInterestExpected, 0.01)
        assertEquals(11000.0, kpis.overdueInterestAmount, 0.01)
        assertEquals(9, kpis.activeBorrowersCount)
        assertEquals(1, kpis.followUpCount)

        // By Nov 12 / Nov 14: Day 1 (குமரன் 4000, ராம்குமார் 2000) & Day 2 (கிருஷ்ணன்(1) 3000) = 9000 due; 5000 paid; 4000 unpaid
        val kpis12th = LoanCalculator.computePortfolioKpis(customers, transactions, "2026-11-12")
        assertEquals(9000.0, kpis12th.monthTillDateExpected, 0.01)
        assertEquals(5000.0, kpis12th.monthTillDateReceived, 0.01)
        assertEquals(4000.0, kpis12th.monthTillDateUnpaid, 0.01)
        assertEquals(1, kpis12th.monthTillDateUnpaidBorrowersCount)

        val kumaran = customers.first { it.name == "குமரன்" }
        assertTrue(LoanCalculator.isInterestOverdue(kumaran, "2026-11-14"))
        assertEquals(11000.0, LoanCalculator.overdueInterestAmount(kumaran, "2026-11-14"), 0.01)
        assertEquals(AccountStatus.FOLLOW_UP_NEEDED.label, kumaran.status)

        customers.filter { it.name != "குமரன்" }.forEach { other ->
            assertFalse("Expected ${other.name} to be on track", LoanCalculator.isInterestOverdue(other, "2026-11-14"))
            assertEquals(0.0, LoanCalculator.overdueInterestAmount(other, "2026-11-14"), 0.01)
            assertEquals(AccountStatus.ACTIVE.label, other.status)
        }
    }

    @Test
    fun payingKumaranOutstandingInterest_clearsOverdueAndRestoresActiveStatus() = runBlocking {
        repository.ensureDefaultSeeded()
        val customers = repository.observeCustomers().first()
        val kumaran = customers.first { it.name == "குமரன்" }

        val payResult = repository.recordTransaction(
            RecordTransactionInput(
                customerId = kumaran.id,
                transactionType = TransactionType.MONTHLY_INTEREST_PAYMENT,
                amount = 11000.0,
                paymentMode = PaymentMode.UPI,
                transactionDate = "2026-11-14",
                notes = "Settled ₹11,000 outstanding interest",
            )
        )
        assertTrue(payResult.isSuccess)

        val updated = repository.getCustomerById(kumaran.id)!!
        assertEquals(0.0, updated.outstandingInterest, 0.01)
        assertEquals(AccountStatus.ACTIVE.label, updated.status)
        assertFalse(LoanCalculator.isInterestOverdue(updated, "2026-11-14"))

        val updatedCustomers = repository.observeCustomers().first()
        val updatedTransactions = repository.observeTransactions().first()
        val updatedKpis = LoanCalculator.computePortfolioKpis(updatedCustomers, updatedTransactions, "2026-11-14")
        assertEquals(9000.0, updatedKpis.monthTillDateExpected, 0.01)
        assertEquals(9000.0, updatedKpis.monthTillDateReceived, 0.01)
        assertEquals(0.0, updatedKpis.monthTillDateUnpaid, 0.01)
    }

    @Test
    fun topUpRepaymentAndClosure_recalculatePrincipalAndInterestInRealTime() = runBlocking {
        repository.ensureDefaultSeeded()
        val customers = repository.observeCustomers().first()
        val manimaran = customers.first { it.name == "மணிமாறன்" }

        val topUpResult = repository.recordTransaction(
            RecordTransactionInput(
                customerId = manimaran.id,
                transactionType = TransactionType.TOP_UP,
                amount = 50000.0,
                paymentMode = PaymentMode.UPI,
                transactionDate = "2026-11-14",
                notes = "Top-up",
            )
        )
        assertTrue(topUpResult.isSuccess)
        val afterTopUp = repository.getCustomerById(manimaran.id)!!
        assertEquals(150000.0, afterTopUp.currentActivePrincipal, 0.01)
        assertEquals(3000.0, afterTopUp.monthlyInterestDue, 0.01)

        val repayResult = repository.recordTransaction(
            RecordTransactionInput(
                customerId = manimaran.id,
                transactionType = TransactionType.PRINCIPAL_REPAYMENT,
                amount = 25000.0,
                paymentMode = PaymentMode.CASH,
                transactionDate = "2026-11-14",
                notes = "Partial repayment",
            )
        )
        assertTrue(repayResult.isSuccess)
        val afterRepay = repository.getCustomerById(manimaran.id)!!
        assertEquals(125000.0, afterRepay.currentActivePrincipal, 0.01)
        assertEquals(2500.0, afterRepay.monthlyInterestDue, 0.01)

        val closeResult = repository.recordTransaction(
            RecordTransactionInput(
                customerId = manimaran.id,
                transactionType = TransactionType.FULL_CLOSURE,
                amount = 125000.0,
                paymentMode = PaymentMode.BANK_TRANSFER,
                transactionDate = "2026-11-14",
                notes = "Settled",
            )
        )
        assertTrue(closeResult.isSuccess)
        val afterClose = repository.getCustomerById(manimaran.id)!!
        assertEquals(0.0, afterClose.currentActivePrincipal, 0.01)
        assertEquals(0.0, afterClose.monthlyInterestDue, 0.01)
        assertEquals(AccountStatus.CLOSED.label, afterClose.status)
    }
}
