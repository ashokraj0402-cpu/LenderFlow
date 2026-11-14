package com.example.domain

import com.example.data.model.AccountStatus
import com.example.data.model.Customer
import com.example.data.model.LoanTransaction
import com.example.data.model.PortfolioKpis
import com.example.data.model.TransactionType
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

object LoanCalculator {

    fun todayIso(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun currentYearMonth(referenceDateIso: String = todayIso()): String {
        return if (referenceDateIso.length >= 7) referenceDateIso.substring(0, 7) else "2026-01"
    }

    fun formatDateDisplay(isoDate: String): String {
        if (isoDate.length != 10) return isoDate.ifBlank { "—" }
        val parts = isoDate.split("-")
        if (parts.size != 3) return isoDate
        val monthNames = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        val monthIdx = (parts[1].toIntOrNull() ?: 1).coerceIn(1, 12) - 1
        val day = parts[2].toIntOrNull() ?: 1
        return "$day ${monthNames[monthIdx]} ${parts[0]}"
    }

    fun formatCurrency(amount: Double): String {
        val negative = amount < 0
        val absAmount = abs(amount)
        val rounded = (absAmount * 100.0).roundToLong() / 100.0
        val whole = rounded.toLong()
        val fraction = ((rounded - whole) * 100.0).roundToLong()

        val formattedWhole = formatIndianInteger(whole)
        val prefix = if (negative) "-₹" else "₹"
        return if (fraction > 0) {
            "$prefix$formattedWhole.${String.format(Locale.US, "%02d", fraction)}"
        } else {
            "$prefix$formattedWhole"
        }
    }

    private fun formatIndianInteger(value: Long): String {
        val str = value.toString()
        if (str.length <= 3) return str
        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString() + "," + lastThree
    }

    fun formatCompactCurrency(amount: Double): String {
        return when {
            amount >= 10_000_000 -> String.format(Locale.US, "₹%.2fCr", amount / 10_000_000.0)
            amount >= 100_000 -> String.format(Locale.US, "₹%.2fL", amount / 100_000.0)
            else -> formatCurrency(amount)
        }
    }

    fun formatRate(rate: Double): String {
        val df = DecimalFormat("#.##")
        return "${df.format(rate)}%/mo"
    }

    fun calculateMonthlyInterest(principal: Double, ratePercent: Double = 2.0): Double {
        return ((max(0.0, principal) * max(0.0, ratePercent)) / 100.0).let {
            (it * 100.0).roundToLong() / 100.0
        }
    }

    /**
     * Determines how many monthly interest cycles are currently unpaid/overdue
     * based on disbursementDate, dueDayOfMonth, lastInterestPaidMonth, and referenceDateIso.
     */
    fun unpaidCyclesCount(
        customer: Customer,
        referenceDateIso: String = todayIso(),
    ): Int {
        if (customer.status == AccountStatus.CLOSED.label || customer.currentActivePrincipal <= 0.0) {
            return 0
        }
        val refParts = parseIsoDate(referenceDateIso) ?: return 0
        val disbParts = parseIsoDate(customer.disbursementDate) ?: return 0

        val refYear = refParts[0]
        val refMonth = refParts[1]
        val refDay = refParts[2]

        val dueDay = customer.dueDayOfMonth.toInt().coerceIn(1, 28)

        // Most recent due cycle year-month on or before referenceDateIso
        val latestDueYm = if (refDay >= dueDay) {
            refYear * 12 + (refMonth - 1)
        } else {
            refYear * 12 + (refMonth - 1) - 1
        }

        // First billing cycle is the month AFTER disbursement (unless disbursed strictly before dueDay in a prior month)
        val disbYm = disbParts[0] * 12 + (disbParts[1] - 1)
        val firstDueYm = disbYm + 1

        if (latestDueYm < firstDueYm) {
            return if (customer.status == AccountStatus.FOLLOW_UP_NEEDED.label) 1 else 0
        }

        val lastPaidYm = parseYearMonth(customer.lastInterestPaidMonth)
        val effectivePaidThrough = if (lastPaidYm != null) max(lastPaidYm, disbYm) else disbYm

        val unpaid = latestDueYm - effectivePaidThrough
        return when {
            unpaid > 0 -> unpaid
            customer.status == AccountStatus.FOLLOW_UP_NEEDED.label -> 1
            else -> 0
        }
    }

    fun isInterestOverdue(
        customer: Customer,
        referenceDateIso: String = todayIso(),
    ): Boolean {
        if (customer.status == AccountStatus.CLOSED.label || customer.currentActivePrincipal <= 0.0) {
            return false
        }
        return customer.outstandingInterest > 0.0 || unpaidCyclesCount(customer, referenceDateIso) > 0
    }

    fun overdueInterestAmount(
        customer: Customer,
        referenceDateIso: String = todayIso(),
    ): Double {
        if (customer.status == AccountStatus.CLOSED.label || customer.currentActivePrincipal <= 0.0) {
            return 0.0
        }
        val cycles = unpaidCyclesCount(customer, referenceDateIso)
        val cycleAmount = if (customer.outstandingInterest > 0.0 && customer.status == AccountStatus.FOLLOW_UP_NEEDED.label) {
            // Explicit outstanding arrears already reflects current unpaid balance
            0.0
        } else {
            customer.monthlyInterestDue * cycles
        }
        return customer.outstandingInterest + cycleAmount
    }

    fun formatTillDateRange(isoDate: String): String {
        val parts = parseIsoDate(isoDate) ?: return isoDate
        val monthNames = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        val monthIdx = parts[1].coerceIn(1, 12) - 1
        val day = parts[2]
        return "1–$day ${monthNames[monthIdx]} ${parts[0]}"
    }

    data class TillDateCycleStatus(
        val customer: Customer,
        val expectedAmount: Double,
        val paidTowardMonth: Double,
        val unpaidAmount: Double,
    )

    fun computeTillDateCycleStatuses(
        customers: List<Customer>,
        transactions: List<LoanTransaction>,
        referenceDateIso: String = todayIso(),
    ): List<TillDateCycleStatus> {
        val refParts = parseIsoDate(referenceDateIso) ?: return emptyList()
        val refYear = refParts[0]
        val refMonth = refParts[1]
        val refDay = refParts[2]
        val refYmIndex = refYear * 12 + (refMonth - 1)
        val refYmStr = currentYearMonth(referenceDateIso)

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, refYear)
            set(Calendar.MONTH, refMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDayInRefMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val paidByCustomerInMonth = transactions
            .filter { txn ->
                txn.transactionType == TransactionType.MONTHLY_INTEREST_PAYMENT.label &&
                    txn.transactionDate.startsWith(refYmStr) &&
                    txn.transactionDate <= referenceDateIso
            }
            .groupBy { it.customerId }
            .mapValues { (_, list) -> list.sumOf { max(0.0, it.amount) } }

        return customers
            .filter { it.status != AccountStatus.CLOSED.label && it.currentActivePrincipal > 0.0 }
            .mapNotNull { customer ->
                val disbParts = parseIsoDate(customer.disbursementDate)
                val disbYmIndex = if (disbParts != null) {
                    disbParts[0] * 12 + (disbParts[1] - 1)
                } else {
                    refYmIndex - 1
                }
                // First billing cycle begins the month after initial disbursement
                if (disbYmIndex >= refYmIndex) return@mapNotNull null

                val effectiveDueDay = customer.dueDayOfMonth.toInt().coerceIn(1, maxDayInRefMonth)
                if (effectiveDueDay > refDay) return@mapNotNull null

                val expected = max(0.0, customer.monthlyInterestDue)
                val recordedInTxns = paidByCustomerInMonth[customer.id] ?: 0.0
                val rawPaid = if (recordedInTxns > 0.0) {
                    recordedInTxns
                } else if (customer.lastInterestPaidMonth >= refYmStr && customer.outstandingInterest == 0.0) {
                    expected
                } else {
                    0.0
                }
                val paidTowardMonth = kotlin.math.min(expected, rawPaid)
                val unpaid = max(0.0, expected - paidTowardMonth)

                TillDateCycleStatus(
                    customer = customer,
                    expectedAmount = expected,
                    paidTowardMonth = paidTowardMonth,
                    unpaidAmount = unpaid,
                )
            }
            .sortedWith(
                compareByDescending<TillDateCycleStatus> { it.unpaidAmount > 0.0 }
                    .thenBy { it.customer.dueDayOfMonth }
            )
    }

    fun computeTillDateUnpaidBorrowers(
        customers: List<Customer>,
        transactions: List<LoanTransaction>,
        referenceDateIso: String = todayIso(),
    ): List<com.example.data.model.TillDateUnpaidBorrower> {
        return computeTillDateCycleStatuses(customers, transactions, referenceDateIso)
            .filter { it.unpaidAmount > 0.0 }
            .map { status ->
                com.example.data.model.TillDateUnpaidBorrower(
                    customer = status.customer,
                    expectedAmount = status.expectedAmount,
                    paidAmount = status.paidTowardMonth,
                    unpaidAmount = status.unpaidAmount,
                )
            }
    }

    fun computePortfolioKpis(
        customers: List<Customer>,
        transactions: List<LoanTransaction>,
        referenceDateIso: String = todayIso(),
    ): PortfolioKpis {
        val nonClosed = customers.filter { it.status != AccountStatus.CLOSED.label }
        val activePrincipal = nonClosed.sumOf { max(0.0, it.currentActivePrincipal) }
        val monthlyExpected = nonClosed.sumOf { max(0.0, it.monthlyInterestDue) }
        val totalInterestCollected = transactions
            .filter { it.transactionType == TransactionType.MONTHLY_INTEREST_PAYMENT.label }
            .sumOf { max(0.0, it.amount) }
        val overdueInterest = nonClosed.sumOf { overdueInterestAmount(it, referenceDateIso) }
        val followUpCount = nonClosed.count {
            it.status == AccountStatus.FOLLOW_UP_NEEDED.label || isInterestOverdue(it, referenceDateIso)
        }
        val closedCount = customers.count { it.status == AccountStatus.CLOSED.label }

        val refYmStr = currentYearMonth(referenceDateIso)
        val tillDateStatuses = computeTillDateCycleStatuses(customers, transactions, referenceDateIso)
        val tillDateExpected = tillDateStatuses.sumOf { it.expectedAmount }
        val tillDateReceived = tillDateStatuses.sumOf { it.paidTowardMonth }
        val tillDateUnpaid = tillDateStatuses.sumOf { it.unpaidAmount }
        val tillDateUnpaidCount = tillDateStatuses.count { it.unpaidAmount > 0.0 }
        val tillDatePaidCount = tillDateStatuses.count { it.unpaidAmount <= 0.0 && it.expectedAmount > 0.0 }
        val monthTotalCollected = transactions
            .filter {
                it.transactionType == TransactionType.MONTHLY_INTEREST_PAYMENT.label &&
                    it.transactionDate.startsWith(refYmStr) &&
                    it.transactionDate <= referenceDateIso
            }
            .sumOf { max(0.0, it.amount) }

        return PortfolioKpis(
            activeLoanPrincipal = activePrincipal,
            monthlyInterestExpected = monthlyExpected,
            totalInterestCollected = totalInterestCollected,
            overdueInterestAmount = overdueInterest,
            activeBorrowersCount = nonClosed.size,
            followUpCount = followUpCount,
            closedCount = closedCount,
            monthTillDateExpected = tillDateExpected,
            monthTillDateReceived = tillDateReceived,
            monthTillDateUnpaid = tillDateUnpaid,
            monthTillDateDueBorrowersCount = tillDateStatuses.size,
            monthTillDatePaidBorrowersCount = tillDatePaidCount,
            monthTillDateUnpaidBorrowersCount = tillDateUnpaidCount,
            monthTotalCollectedThisMonth = monthTotalCollected,
        )
    }

    fun buildMonthlyCollectionSummaries(
        transactions: List<LoanTransaction>,
    ): List<MonthlySummary> {
        return transactions
            .filter { it.transactionDate.length >= 7 }
            .groupBy { it.transactionDate.substring(0, 7) }
            .entries
            .sortedByDescending { it.key }
            .map { (ym, txns) ->
                MonthlySummary(
                    yearMonth = ym,
                    interestCollected = txns
                        .filter { it.transactionType == TransactionType.MONTHLY_INTEREST_PAYMENT.label }
                        .sumOf { it.amount },
                    principalRepaid = txns
                        .filter {
                            it.transactionType == TransactionType.PRINCIPAL_REPAYMENT.label ||
                                it.transactionType == TransactionType.FULL_CLOSURE.label
                        }
                        .sumOf { it.amount },
                    disbursedAndTopUps = txns
                        .filter {
                            it.transactionType == TransactionType.INITIAL_DISBURSEMENT.label ||
                                it.transactionType == TransactionType.TOP_UP.label
                        }
                        .sumOf { it.amount },
                    transactionCount = txns.size,
                )
            }
    }

    fun buildCsvExport(
        customers: List<Customer>,
        transactions: List<LoanTransaction>,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== BORROWER PORTFOLIO SUMMARY ===")
        sb.appendLine("Customer ID,Name,Phone,Disbursement Date,Due Day,Initial Principal,Top-Ups,Repaid,Current Principal,Rate %,Monthly Interest Due,Status,Last Payment,Notes")
        customers.forEach { c ->
            sb.appendLine(
                listOf(
                    csvEscape(c.customerCode),
                    csvEscape(c.name),
                    csvEscape(c.phoneNumber),
                    csvEscape(c.disbursementDate),
                    c.dueDayOfMonth.toString(),
                    c.initialPrincipal.toString(),
                    c.extraPrincipalAdded.toString(),
                    c.principalRepaid.toString(),
                    c.currentActivePrincipal.toString(),
                    c.monthlyInterestRate.toString(),
                    c.monthlyInterestDue.toString(),
                    csvEscape(c.status),
                    csvEscape(c.lastPaymentDate),
                    csvEscape(c.followUpNotes),
                ).joinToString(",")
            )
        }
        sb.appendLine()
        sb.appendLine("=== TRANSACTIONS AUDIT TRAIL ===")
        sb.appendLine("Transaction ID,Date,Customer ID,Customer Name,Type,Amount,Payment Mode,Notes")
        transactions.forEach { t ->
            sb.appendLine(
                listOf(
                    csvEscape(t.transactionCode),
                    csvEscape(t.transactionDate),
                    csvEscape(t.customerCode),
                    csvEscape(t.customerName),
                    csvEscape(t.transactionType),
                    t.amount.toString(),
                    csvEscape(t.paymentMode),
                    csvEscape(t.notes),
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun parseIsoDate(isoDate: String): IntArray? {
        if (isoDate.length != 10) return null
        val parts = isoDate.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return intArrayOf(y, m, d)
    }

    private fun parseYearMonth(ym: String): Int? {
        if (ym.length != 7) return null
        val parts = ym.split("-")
        if (parts.size != 2) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return y * 12 + (m - 1)
    }
}

data class MonthlySummary(
    val yearMonth: String,
    val interestCollected: Double,
    val principalRepaid: Double,
    val disbursedAndTopUps: Double,
    val transactionCount: Int,
)
