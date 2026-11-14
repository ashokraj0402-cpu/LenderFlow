package com.example.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultBorrowersSeed
import com.example.data.repository.LendingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExcelWorkbookExporterTest {

    @Test
    fun buildWorkbookBytes_createsValidThreeSheetXlsxWithTamilData() {
        val (customers, transactions) = DefaultBorrowersSeed.buildSeedData()
        val kpis = LoanCalculator.computePortfolioKpis(customers, transactions, "2026-11-14")
        val summaries = LoanCalculator.buildMonthlyCollectionSummaries(transactions)

        val bytes = ExcelWorkbookExporter.buildWorkbookBytes(
            customers = customers,
            transactions = transactions,
            kpis = kpis,
            summaries = summaries,
            referenceDateIso = "2026-11-14",
        )

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(StandardCharsets.UTF_8)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("_rels/.rels"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/_rels/workbook.xml.rels"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet3.xml"))

        val summarySheet = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(summarySheet.contains("1900000"))
        assertTrue(summarySheet.contains("38000"))
        assertTrue(summarySheet.contains("9000"))

        val borrowersSheet = entries.getValue("xl/worksheets/sheet2.xml")
        assertTrue(borrowersSheet.contains("குமரன்"))
        assertTrue(borrowersSheet.contains("பால்பண்ணை"))
        assertTrue(borrowersSheet.contains("11000"))

        val transactionsSheet = entries.getValue("xl/worksheets/sheet3.xml")
        assertTrue(transactionsSheet.contains("ராம்குமார்"))
    }

    @Test
    fun repositorySeed_autoSyncsLocalXlsxFileToAppStorage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = LendingRepository(db.lendingDao(), appContext = context)
            repo.ensureDefaultSeeded()

            val localXlsx = ExcelWorkbookExporter.getLocalWorkbookFile(context)
            assertTrue(localXlsx.exists())
            assertTrue(localXlsx.length() > 500L)
        } finally {
            db.close()
        }
    }
}
