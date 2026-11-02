package com.example

import com.example.data.AppDatabase
import com.example.data.CsvImporter
import com.example.data.RecordEntity
import com.example.data.SampleDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val fakeDao = object : com.example.data.RecordDao {
        override fun getAllRecords(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override suspend fun getAllRecordsFast(limit: Int) = emptyList<RecordEntity>()
        override fun getRecordCount() = kotlinx.coroutines.flow.flowOf(0)
        override suspend fun getRecordCountSync() = 0
        override suspend fun searchExactCard(query: String, limit: Int) = emptyList<RecordEntity>()
        override suspend fun searchExactNationalCode(query: String, limit: Int) = emptyList<RecordEntity>()
        override suspend fun searchExactMobile(query: String, limit: Int) = emptyList<RecordEntity>()
        override suspend fun searchExactAccount(query: String, limit: Int) = emptyList<RecordEntity>()
        override suspend fun searchExactIdNumber(query: String, limit: Int) = emptyList<RecordEntity>()
        override suspend fun searchCardPrefixRange(low: String, high: String, limit: Int) = emptyList<RecordEntity>()
        override suspend fun searchNationalCodePrefixRange(low: String, high: String, limit: Int) = emptyList<RecordEntity>()
        override suspend fun searchMobilePrefixRange(low: String, high: String, limit: Int) = emptyList<RecordEntity>()
        override suspend fun searchAccountPrefixRange(low: String, high: String, limit: Int) = emptyList<RecordEntity>()
        override suspend fun searchNamePrefixRange(low: String, high: String, limit: Int) = emptyList<RecordEntity>()
        override suspend fun searchCityPrefixRange(low: String, high: String, limit: Int) = emptyList<RecordEntity>()

        override fun searchAllFields(query: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByNationalCodePrefix(prefixQuery: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByMobilePrefix(prefixQuery: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByCardNoPrefix(prefixQuery: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByAccountPrefix(prefixQuery: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByNamePrefix(prefixQuery: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByCityPrefix(prefixQuery: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByNationalCode(query: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByMobile(query: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByCardNo(query: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByAccount(query: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByName(query: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun searchByCity(query: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
        override fun getRecordById(id: Long) = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun insertRecord(record: RecordEntity) = 1L
        override suspend fun insertBatch(records: List<RecordEntity>) = records.map { 1L }
        override suspend fun deleteAllRecords() = 0
        override suspend fun deleteRecordById(id: Long) = 1
        override suspend fun getAllRecordsSync() = emptyList<RecordEntity>()
    }

    private val importer = CsvImporter(fakeDao)

    @Test
    fun testPersianArabicDigitNormalization() {
        val persian = "۰۱۲۳۴۵۶۷۸۹"
        val arabic = "٠١٢٣٤٥٦٧٨٩"
        assertEquals("0123456789", importer.normalizeDigits(persian))
        assertEquals("0123456789", importer.normalizeDigits(arabic))
    }

    @Test
    fun testCsvLineParsingWithQuotesAndCommas() {
        val line = "0012345678,1234567890,\"Rostami, Sina\",\"Bahram\",4512,1990/04/15,\"Tehran\",\"Tehran\",\"Tehran\",\"Tehran\",\"Valiasr Ave, Mellat Park, Bldg 42\",6037991823451101,09121112233"
        val tokens = importer.parseCsvLine(line)
        assertEquals(13, tokens.size)
        assertEquals("0012345678", tokens[0])
        assertEquals("1234567890", tokens[1])
        assertEquals("Rostami, Sina", tokens[2])
        assertEquals("Bahram", tokens[3])
        assertEquals("4512", tokens[4])
        assertEquals("1990/04/15", tokens[5])
        assertEquals("Tehran", tokens[6])
        assertEquals("Tehran", tokens[7])
        assertEquals("Tehran", tokens[8])
        assertEquals("Tehran", tokens[9])
        assertEquals("Valiasr Ave, Mellat Park, Bldg 42", tokens[10])
        assertEquals("6037991823451101", tokens[11])
        assertEquals("09121112233", tokens[12])
    }

    @Test
    fun testRecordFormatters() {
        val record = RecordEntity(
            nationalCode = "0012345678",
            cardNo = "6037991812345678",
            mobile = "09123456789"
        )
        assertEquals("6037 9918 1234 5678", record.formattedCardNumber)
        assertEquals("6037 •••• •••• 5678", record.maskedCardNumber)
        assertEquals("بانک ملی / Bank Melli", record.bankIssuerName)
        assertEquals("0912 345 6789", record.formattedMobile)
    }

    @Test
    fun testSampleDatasetCoverage() {
        val samples = SampleDataProvider.getSampleRecords()
        assertTrue(samples.isNotEmpty())
        for (record in samples) {
            assertTrue(record.nationalCode.isNotEmpty())
            assertTrue(record.fullName.isNotEmpty())
            assertTrue(record.mobile.isNotEmpty())
            assertTrue(record.cityName.isNotEmpty())
        }
    }

    @Test
    fun testImportControllerPauseAndResume() = kotlinx.coroutines.runBlocking {
        val controller = com.example.data.ImportController()
        org.junit.Assert.assertFalse(controller.isPaused)
        org.junit.Assert.assertFalse(controller.isCancelled)

        controller.pause()
        assertTrue(controller.isPaused)

        // Resuming unblocks
        controller.resume()
        org.junit.Assert.assertFalse(controller.isPaused)
    }

    @Test
    fun testImportControllerCancel() = kotlinx.coroutines.runBlocking {
        val controller = com.example.data.ImportController()
        controller.pause()
        assertTrue(controller.isPaused)

        controller.cancel()
        assertTrue(controller.isCancelled)
        org.junit.Assert.assertFalse(controller.isPaused)
    }

    @Test
    fun testImportProgressDefaultAndPausedStates() {
        val progress = com.example.data.ImportProgress(
            isRunning = false,
            isPaused = true,
            canResume = true,
            recordsImported = 5000,
            totalRecords = 100000,
            currentBatch = 2,
            percent = 5.0f,
            statusMessage = "PAUSED AT BATCH 2"
        )
        assertTrue(progress.isPaused)
        assertTrue(progress.canResume)
        org.junit.Assert.assertFalse(progress.isRunning)
        assertEquals(5000, progress.recordsImported)
        assertEquals(100000, progress.totalRecords)
    }
}
