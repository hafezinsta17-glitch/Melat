package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow

enum class SearchFilter(
    val label: String,
    val code: String,
    val financialSubtitle: String
) {
    ALL("ALL FINANCIAL", "ALL", "Global Financial & Bio Records"),
    CARD_NO("CARD (16-DIGIT)", "CRD", "Shetab Bank Card"),
    ACCOUNT("ACCOUNT NO", "ACC", "Deposit & Ledger"),
    NATIONAL_CODE("NAT. CODE", "NAT", "Citizen National Code"),
    MOBILE("MOBILE / CELL", "MOB", "Registered Mobile"),
    NAME("CITIZEN NAME", "NAM", "Full Name & Father"),
    CITY("CITY / REGION", "LOC", "Residence & Branch")
}

class RecordRepository(
    private val recordDao: RecordDao,
    private val csvImporter: CsvImporter
) {

    fun getRecordCount(): Flow<Int> = recordDao.getRecordCount()

    fun searchRecords(query: String, filter: SearchFilter): Flow<List<RecordEntity>> {
        val cleanQuery = csvImporter.normalizeDigits(query.trim())
        if (cleanQuery.isEmpty()) {
            return recordDao.getAllRecords(limit = 100)
        }
        val prefix = "$cleanQuery%"
        return when (filter) {
            SearchFilter.ALL -> {
                // If numeric query, route to indexed prefix queries based on Iranian format length
                val digitsOnly = cleanQuery.filter { it.isDigit() }
                if (digitsOnly.length >= 4) {
                    when {
                        digitsOnly.length in 13..16 || digitsOnly.startsWith("6") || digitsOnly.startsWith("5") ->
                            recordDao.searchByCardNoPrefix(prefix)
                        digitsOnly.length in 8..10 ->
                            recordDao.searchByNationalCodePrefix(prefix)
                        digitsOnly.startsWith("09") || digitsOnly.length == 11 ->
                            recordDao.searchByMobilePrefix(prefix)
                        else ->
                            recordDao.searchByAccountPrefix(prefix)
                    }
                } else {
                    recordDao.searchAllFields(cleanQuery)
                }
            }
            SearchFilter.CARD_NO -> recordDao.searchByCardNoPrefix(prefix)
            SearchFilter.ACCOUNT -> recordDao.searchByAccountPrefix(prefix)
            SearchFilter.NATIONAL_CODE -> recordDao.searchByNationalCodePrefix(prefix)
            SearchFilter.MOBILE -> recordDao.searchByMobilePrefix(prefix)
            SearchFilter.NAME -> recordDao.searchByNamePrefix(prefix)
            SearchFilter.CITY -> recordDao.searchByCityPrefix(prefix)
        }
    }

    fun getRecordById(id: Long): Flow<RecordEntity?> = recordDao.getRecordById(id)

    suspend fun importCsvUri(context: Context, uri: Uri): Flow<ImportProgress> {
        return csvImporter.importFromUri(context, uri)
    }

    suspend fun importCsvString(context: Context, content: String, name: String = "TEXT_IMPORT.csv"): Flow<ImportProgress> {
        return csvImporter.importFromString(context, content, name)
    }

    suspend fun loadSampleRecords(): Int {
        val samples = SampleDataProvider.getSampleRecords()
        recordDao.insertBatch(samples)
        return samples.size
    }

    suspend fun deleteRecord(id: Long) = recordDao.deleteRecordById(id)

    suspend fun clearDatabase(): Int = recordDao.deleteAllRecords()

    suspend fun exportCsv(): String = csvImporter.exportToCsvString()
}
