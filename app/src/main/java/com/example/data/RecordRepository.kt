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
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return recordDao.getAllRecords(limit = 100)
        }
        return when (filter) {
            SearchFilter.ALL -> recordDao.searchAllFields(cleanQuery)
            SearchFilter.CARD_NO -> recordDao.searchByCardNo(cleanQuery)
            SearchFilter.ACCOUNT -> recordDao.searchByAccount(cleanQuery)
            SearchFilter.NATIONAL_CODE -> recordDao.searchByNationalCode(cleanQuery)
            SearchFilter.MOBILE -> recordDao.searchByMobile(cleanQuery)
            SearchFilter.NAME -> recordDao.searchByName(cleanQuery)
            SearchFilter.CITY -> recordDao.searchByCity(cleanQuery)
        }
    }

    fun getRecordById(id: Long): Flow<RecordEntity?> = recordDao.getRecordById(id)

    suspend fun importCsvUri(context: Context, uri: Uri): Flow<ImportProgress> {
        return csvImporter.importFromUri(context, uri)
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
