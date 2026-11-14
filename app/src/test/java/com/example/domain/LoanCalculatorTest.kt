package com.example.domain

import com.example.data.model.AccountStatus
import com.example.data.model.Customer
import com.example.data.model.LoanTransaction
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanCalculatorTest {

    @Test
    fun calculateMonthlyInterest_standardTwoPercent_returnsExactValue() {
        assertEquals(2000.0, LoanCalculator.calculateMonthlyInterest(100000.0, 2.0), 0.01)
        assertEquals(3500.0, LoanCalculator.calculateMonthlyInterest(175000.0, 2.0), 0.01)
    }

    @Test
    fun overdueDetection_flagsUnpaidPriorMonthAfterDueDay() {
        val borrower = Customer(
            id = "c1",
            customerCode = "CUST-1001",
            name = "Suresh",
            disbursementDate = "2026-09-05",
            dueDayOfMonth = 5L,
            initialPrincipal = 100000.0,
            currentActivePrincipal = 100000.0,
            monthlyInterestRate = 2.0,
            monthlyInterestDue = 2000.0,
            status = AccountStatus.ACTIVE.label,
            lastInterestPaidMonth = "2026-10",
        )
        assertFalse(LoanCalculator.isInterestOverdue(borrower, "2026-11-04"))
        assertTrue(LoanCalculator.isInterestOverdue(borrower, "2026-11-06"))
        assertEquals(2000.0, LoanCalculator.overdueInterestAmount(borrower, "2026-11-06"), 0.01)
    }

    @Test
    fun computePortfolioKpis_aggregatesActivePrincipalExpectedInterestAndCollected() {
        val c1 = Customer(
            id = "c1",
            customerCode = "CUST-1001",
            name = "A",
            disbursementDate = "2026-09-01",
            dueDayOfMonth = 5L,
            initialPrincipal = 100000.0,
            currentActivePrincipal = 120000.0,
            monthlyInterestRate = 2.0,
            monthlyInterestDue = 2400.0,
            status = AccountStatus.ACTIVE.label,
            lastInterestPaidMonth = "2026-11",
        )
        val c2 = Customer(
            id = "c2",
            customerCode = "CUST-1002",
            name = "B",
            disbursementDate = "2026-10-01",
            dueDayOfMonth = 5L,
            initialPrincipal = 50000.0,
            currentActivePrincipal = 0.0,
            monthlyInterestRate = 2.0,
            monthlyInterestDue = 0.0,
            status = AccountStatus.CLOSED.label,
        )
        val txns = listOf(
            LoanTransaction(
                id = "t1",
                transactionType = TransactionType.MONTHLY_INTEREST_PAYMENT.label,
                amount = 2400.0,
                transactionDate = "2026-11-05",
            )
        )
        val kpis = LoanCalculator.computePortfolioKpis(listOf(c1, c2), txns, "2026-11-14")
        assertEquals(120000.0, kpis.activeLoanPrincipal, 0.01)
        assertEquals(2400.0, kpis.monthlyInterestExpected, 0.01)
        assertEquals(2400.0, kpis.totalInterestCollected, 0.01)
        assertEquals(1, kpis.activeBorrowersCount)
        assertEquals(1, kpis.closedCount)
    }

    @Test
    fun computePortfolioKpis_monthTillDateOn12thAnd16th_tracksExpectedRecordedAndUnpaid() {
        val paidOn1st = Customer(
            id = "c1",
            customerCode = "CUST-1001",
            name = "Ram",
            disbursementDate = "2026-10-01",
            dueDayOfMonth = 1L,
            initialPrincipal = 100000.0,
            currentActivePrincipal = 100000.0,
            monthlyInterestRate = 2.0,
            monthlyInterestDue = 2000.0,
            status = AccountStatus.ACTIVE.label,
            lastInterestPaidMonth = "2026-11",
        )
        val unpaidDue5th = Customer(
            id = "c2",
            customerCode = "CUST-1002",
            name = "Kumaran",
            disbursementDate = "2026-10-05",
            dueDayOfMonth = 5L,
            initialPrincipal = 200000.0,
            currentActivePrincipal = 200000.0,
            monthlyInterestRate = 2.0,
            monthlyInterestDue = 4000.0,
            status = AccountStatus.ACTIVE.label,
            lastInterestPaidMonth = "2026-10",
        )
        val due15th = Customer(
            id = "c3",
            customerCode = "CUST-1003",
            name = "Dairy",
            disbursementDate = "2026-10-15",
            dueDayOfMonth = 15L,
            initialPrincipal = 900000.0,
            currentActivePrincipal = 900000.0,
            monthlyInterestRate = 2.0,
            monthlyInterestDue = 18000.0,
            status = AccountStatus.ACTIVE.label,
            lastInterestPaidMonth = "2026-10",
        )
        val txns = listOf(
            LoanTransaction(
                id = "t1",
                customerId = "c1",
                transactionType = TransactionType.MONTHLY_INTEREST_PAYMENT.label,
                amount = 2000.0,
                transactionDate = "2026-11-01",
            )
        )

        // By 12th: c1 (2000 paid) + c2 (4000 unpaid) are due; c3 (15th) is not yet due
        val kpisOn12th = LoanCalculator.computePortfolioKpis(
            listOf(paidOn1st, unpaidDue5th, due15th),
            txns,
            "2026-11-12",
        )
        assertEquals(6000.0, kpisOn12th.monthTillDateExpected, 0.01)
        assertEquals(2000.0, kpisOn12th.monthTillDateReceived, 0.01)
        assertEquals(4000.0, kpisOn12th.monthTillDateUnpaid, 0.01)
        assertEquals(2, kpisOn12th.monthTillDateDueBorrowersCount)
        assertEquals(1, kpisOn12th.monthTillDatePaidBorrowersCount)
        assertEquals(1, kpisOn12th.monthTillDateUnpaidBorrowersCount)

        val unpaidListOn12th = LoanCalculator.computeTillDateUnpaidBorrowers(
            listOf(paidOn1st, unpaidDue5th, due15th),
            txns,
            "2026-11-12",
        )
        assertEquals(1, unpaidListOn12th.size)
        assertEquals("c2", unpaidListOn12th.first().customer.id)
        assertEquals(4000.0, unpaidListOn12th.first().unpaidAmount, 0.01)

        // By 16th: c3 (18000 due on 15th) is now also due and unpaid
        val kpisOn16th = LoanCalculator.computePortfolioKpis(
            listOf(paidOn1st, unpaidDue5th, due15th),
            txns,
            "2026-11-16",
        )
        assertEquals(24000.0, kpisOn16th.monthTillDateExpected, 0.01)
        assertEquals(2000.0, kpisOn16th.monthTillDateReceived, 0.01)
        assertEquals(22000.0, kpisOn16th.monthTillDateUnpaid, 0.01)
        assertEquals(3, kpisOn16th.monthTillDateDueBorrowersCount)
        assertEquals(2, kpisOn16th.monthTillDateUnpaidBorrowersCount)
    }
}
