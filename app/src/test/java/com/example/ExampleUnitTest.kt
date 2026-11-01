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
        override fun getRecordCount() = kotlinx.coroutines.flow.flowOf(0)
        override suspend fun getRecordCountSync() = 0
        override fun searchAllFields(query: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<RecordEntity>())
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
}
