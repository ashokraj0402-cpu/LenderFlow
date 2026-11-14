package com.example.domain

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Customer
import com.example.data.model.LoanTransaction
import com.example.data.model.PortfolioKpis
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generates a valid, 3-sheet OpenXML Excel workbook (.xlsx) using pure Kotlin + ZipOutputStream.
 * Compatible with Microsoft Excel, Google Sheets, WPS Office, and LibreOffice without heavy dependencies.
 */
object ExcelWorkbookExporter {

    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val DEFAULT_FILE_NAME = "LendFlow_Ledger.xlsx"

    fun getLocalWorkbookFile(context: Context): File {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, DEFAULT_FILE_NAME)
    }

    fun syncWorkbookToAppStorage(
        context: Context,
        customers: List<Customer>,
        transactions: List<LoanTransaction>,
        kpis: PortfolioKpis,
        summaries: List<MonthlySummary>,
        referenceDateIso: String = LoanCalculator.todayIso(),
    ): File {
        val target = getLocalWorkbookFile(context)
        val bytes = buildWorkbookBytes(
            customers = customers,
            transactions = transactions,
            kpis = kpis,
            summaries = summaries,
            referenceDateIso = referenceDateIso,
        )
        target.writeBytes(bytes)
        return target
    }

    fun writeWorkbookToStream(
        outputStream: OutputStream,
        customers: List<Customer>,
        transactions: List<LoanTransaction>,
        kpis: PortfolioKpis,
        summaries: List<MonthlySummary>,
        referenceDateIso: String = LoanCalculator.todayIso(),
    ) {
        val bytes = buildWorkbookBytes(
            customers = customers,
            transactions = transactions,
            kpis = kpis,
            summaries = summaries,
            referenceDateIso = referenceDateIso,
        )
        outputStream.write(bytes)
        outputStream.flush()
    }

    fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun buildWorkbookBytes(
        customers: List<Customer>,
        transactions: List<LoanTransaction>,
        kpis: PortfolioKpis,
        summaries: List<MonthlySummary>,
        referenceDateIso: String = LoanCalculator.todayIso(),
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zip, "_rels/.rels", rootRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", workbookXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zip, "xl/styles.xml", stylesXml())
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildSummarySheetXml(kpis, summaries, referenceDateIso))
            writeZipEntry(zip, "xl/worksheets/sheet2.xml", buildBorrowersSheetXml(customers))
            writeZipEntry(zip, "xl/worksheets/sheet3.xml", buildTransactionsSheetXml(transactions))
        }
        return baos.toByteArray()
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private sealed class CellVal {
        data class Text(val value: String, val bold: Boolean = false) : CellVal()
        data class Num(val value: Double, val bold: Boolean = false) : CellVal()
    }

    private fun buildSummarySheetXml(
        kpis: PortfolioKpis,
        summaries: List<MonthlySummary>,
        referenceDateIso: String,
    ): String {
        val rows = mutableListOf<List<CellVal>>()
        rows += listOf(
            CellVal.Text("LendFlow Portfolio Summary", bold = true),
            CellVal.Text("As of $referenceDateIso", bold = true),
        )
        rows.add(emptyList())
        rows += listOf(CellVal.Text("Metric", bold = true), CellVal.Text("Amount (INR) / Count", bold = true))
        rows += listOf(CellVal.Text("Total Active Principal"), CellVal.Num(kpis.activeLoanPrincipal))
        rows += listOf(CellVal.Text("Monthly Interest Expected (2%)"), CellVal.Num(kpis.monthlyInterestExpected))
        rows += listOf(CellVal.Text("Month Till Date — To Receive"), CellVal.Num(kpis.monthTillDateExpected))
        rows += listOf(CellVal.Text("Month Till Date — Recorded"), CellVal.Num(kpis.monthTillDateReceived))
        rows += listOf(CellVal.Text("Month Till Date — Not Paid"), CellVal.Num(kpis.monthTillDateUnpaid))
        rows += listOf(CellVal.Text("Total Overdue Interest"), CellVal.Num(kpis.overdueInterestAmount))
        rows += listOf(CellVal.Text("Total Interest Collected"), CellVal.Num(kpis.totalInterestCollected))
        rows += listOf(CellVal.Text("Active Borrowers"), CellVal.Num(kpis.activeBorrowersCount.toDouble()))
        rows += listOf(CellVal.Text("Follow-Up Accounts"), CellVal.Num(kpis.followUpCount.toDouble()))
        rows += listOf(CellVal.Text("Closed Accounts"), CellVal.Num(kpis.closedCount.toDouble()))

        if (summaries.isNotEmpty()) {
            rows.add(emptyList())
            rows += listOf(
                CellVal.Text("Month", bold = true),
                CellVal.Text("Interest Collected", bold = true),
                CellVal.Text("Principal Repaid / Closed", bold = true),
                CellVal.Text("Disbursed & Top-Ups", bold = true),
                CellVal.Text("Transactions", bold = true),
            )
            summaries.forEach { m ->
                rows += listOf(
                    CellVal.Text(m.yearMonth),
                    CellVal.Num(m.interestCollected),
                    CellVal.Num(m.principalRepaid),
                    CellVal.Num(m.disbursedAndTopUps),
                    CellVal.Num(m.transactionCount.toDouble()),
                )
            }
        }
        return buildWorksheetXml(rows, colWidths = listOf(34, 22, 24, 22, 16))
    }

    private fun buildBorrowersSheetXml(customers: List<Customer>): String {
        val rows = mutableListOf<List<CellVal>>()
        rows += listOf(
            CellVal.Text("Customer ID", bold = true),
            CellVal.Text("Name", bold = true),
            CellVal.Text("Phone", bold = true),
            CellVal.Text("Disbursement Date", bold = true),
            CellVal.Text("Due Day", bold = true),
            CellVal.Text("Initial Principal", bold = true),
            CellVal.Text("Top-Ups Added", bold = true),
            CellVal.Text("Principal Repaid", bold = true),
            CellVal.Text("Current Active Principal", bold = true),
            CellVal.Text("Rate (%)", bold = true),
            CellVal.Text("Monthly Interest Due", bold = true),
            CellVal.Text("Outstanding Interest", bold = true),
            CellVal.Text("Status", bold = true),
            CellVal.Text("Last Payment Date", bold = true),
            CellVal.Text("Notes", bold = true),
        )
        customers.forEach { c ->
            rows += listOf(
                CellVal.Text(c.customerCode),
                CellVal.Text(c.name),
                CellVal.Text(c.phoneNumber),
                CellVal.Text(c.disbursementDate),
                CellVal.Num(c.dueDayOfMonth.toDouble()),
                CellVal.Num(c.initialPrincipal),
                CellVal.Num(c.extraPrincipalAdded),
                CellVal.Num(c.principalRepaid),
                CellVal.Num(c.currentActivePrincipal),
                CellVal.Num(c.monthlyInterestRate),
                CellVal.Num(c.monthlyInterestDue),
                CellVal.Num(c.outstandingInterest),
                CellVal.Text(c.status),
                CellVal.Text(c.lastPaymentDate),
                CellVal.Text(c.followUpNotes),
            )
        }
        rows += listOf(
            CellVal.Text("TOTAL", bold = true),
            CellVal.Text("${customers.size} accounts", bold = true),
            CellVal.Text(""),
            CellVal.Text(""),
            CellVal.Text(""),
            CellVal.Num(customers.sumOf { it.initialPrincipal }, bold = true),
            CellVal.Num(customers.sumOf { it.extraPrincipalAdded }, bold = true),
            CellVal.Num(customers.sumOf { it.principalRepaid }, bold = true),
            CellVal.Num(customers.sumOf { it.currentActivePrincipal }, bold = true),
            CellVal.Text(""),
            CellVal.Num(customers.sumOf { it.monthlyInterestDue }, bold = true),
            CellVal.Num(customers.sumOf { it.outstandingInterest }, bold = true),
            CellVal.Text(""),
            CellVal.Text(""),
            CellVal.Text(""),
        )
        return buildWorksheetXml(
            rows = rows,
            colWidths = listOf(14, 22, 16, 16, 10, 18, 16, 18, 22, 10, 20, 20, 18, 16, 36),
        )
    }

    private fun buildTransactionsSheetXml(transactions: List<LoanTransaction>): String {
        val rows = mutableListOf<List<CellVal>>()
        rows += listOf(
            CellVal.Text("Transaction ID", bold = true),
            CellVal.Text("Date", bold = true),
            CellVal.Text("Customer ID", bold = true),
            CellVal.Text("Customer Name", bold = true),
            CellVal.Text("Transaction Type", bold = true),
            CellVal.Text("Amount", bold = true),
            CellVal.Text("Payment Mode", bold = true),
            CellVal.Text("Notes", bold = true),
        )
        transactions.forEach { t ->
            rows += listOf(
                CellVal.Text(t.transactionCode),
                CellVal.Text(t.transactionDate),
                CellVal.Text(t.customerCode),
                CellVal.Text(t.customerName),
                CellVal.Text(t.transactionType),
                CellVal.Num(t.amount),
                CellVal.Text(t.paymentMode),
                CellVal.Text(t.notes),
            )
        }
        return buildWorksheetXml(
            rows = rows,
            colWidths = listOf(16, 14, 14, 22, 26, 16, 16, 36),
        )
    }

    private fun buildWorksheetXml(rows: List<List<CellVal>>, colWidths: List<Int>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sb.append("<cols>")
        colWidths.forEachIndexed { idx, w ->
            val colNum = idx + 1
            sb.append("<col min=\"$colNum\" max=\"$colNum\" width=\"$w\" customWidth=\"1\"/>")
        }
        sb.append("</cols>")
        sb.append("<sheetData>")
        rows.forEachIndexed { rowIdx, cells ->
            val rowNum = rowIdx + 1
            if (cells.isEmpty()) {
                sb.append("<row r=\"$rowNum\"/>")
            } else {
                sb.append("<row r=\"$rowNum\">")
                cells.forEachIndexed { colIdx, cell ->
                    val ref = "${columnLetter(colIdx)}$rowNum"
                    when (cell) {
                        is CellVal.Text -> {
                            val style = if (cell.bold) " s=\"1\"" else ""
                            sb.append("<c r=\"$ref\"$style t=\"inlineStr\"><is><t xml:space=\"preserve\">${xmlEscape(cell.value)}</t></is></c>")
                        }
                        is CellVal.Num -> {
                            val style = if (cell.bold) " s=\"1\"" else ""
                            val formatted = if (cell.value % 1.0 == 0.0) {
                                cell.value.toLong().toString()
                            } else {
                                cell.value.toString()
                            }
                            sb.append("<c r=\"$ref\"$style t=\"n\"><v>$formatted</v></c>")
                        }
                    }
                }
                sb.append("</row>")
            }
        }
        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun columnLetter(zeroBasedIndex: Int): String {
        var index = zeroBasedIndex
        val sb = StringBuilder()
        while (index >= 0) {
            sb.append(('A' + (index % 26)))
            index = (index / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun xmlEscape(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    if (ch.code >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun contentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        </Types>
    """.trimIndent()

    private fun rootRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun workbookXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Summary" sheetId="1" r:id="rId1"/>
            <sheet name="Borrowers" sheetId="2" r:id="rId2"/>
            <sheet name="Transactions" sheetId="3" r:id="rId3"/>
          </sheets>
        </workbook>
    """.trimIndent()

    private fun workbookRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
          <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
          <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun stylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <fonts count="2">
            <font><sz val="11"/><name val="Calibri"/></font>
            <font><b/><sz val="11"/><name val="Calibri"/></font>
          </fonts>
          <fills count="2">
            <fill><patternFill patternType="none"/></fill>
            <fill><patternFill patternType="gray125"/></fill>
          </fills>
          <borders count="1">
            <border><left/><right/><top/><bottom/><diagonal/></border>
          </borders>
          <cellStyleXfs count="1">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
          </cellStyleXfs>
          <cellXfs count="2">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/>
          </cellXfs>
        </styleSheet>
    """.trimIndent()
}
