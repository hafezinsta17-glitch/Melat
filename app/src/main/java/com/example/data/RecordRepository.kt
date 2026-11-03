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

data class SearchExecutionResult(
    val records: List<RecordEntity>,
    val executionTimeMs: Long,
    val algorithmName: String,
    val matchedField: String
)

class RecordRepository(
    private val recordDao: RecordDao,
    private val csvImporter: CsvImporter
) {

    fun getRecordCount(): Flow<Int> = recordDao.getRecordCount()

    /**
     * Ultra-High-Performance Search Algorithm tailored for tables with up to 100,000,000 records.
     * Uses indexed exact lookups, B-tree range prefix seeks, and multi-index parallel unions
     * to eliminate all full table scans.
     */
    suspend fun searchRecordsFast(
        query: String,
        filter: SearchFilter,
        limit: Int = 100
    ): SearchExecutionResult {
        val cleanQuery = csvImporter.normalizeDigits(query.trim())
        if (cleanQuery.isEmpty()) {
            val start = System.currentTimeMillis()
            val list = recordDao.getAllRecordsFast(limit = limit)
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            return SearchExecutionResult(
                records = list,
                executionTimeMs = elapsed,
                algorithmName = "PRIMARY_KEY_HEAD [O(1)]",
                matchedField = "ALL"
            )
        }

        val low = cleanQuery
        val high = cleanQuery + "\uFFFF"
        val digitsOnly = cleanQuery.filter { it.isDigit() }
        val isNumeric = digitsOnly.length == cleanQuery.length && cleanQuery.isNotEmpty()

        val start = System.currentTimeMillis()
        var algorithm = "B-TREE_RANGE_SEEK [O(log N)]"
        var matchedField = filter.code

        val results: List<RecordEntity> = when (filter) {
            SearchFilter.CARD_NO -> {
                if (digitsOnly.length == 16) {
                    algorithm = "EXACT_SHETAB_CARD_16 [O(log N)]"
                    recordDao.searchExactCard(digitsOnly, limit)
                } else {
                    algorithm = "CARD_B-TREE_RANGE [${cleanQuery}...]"
                    recordDao.searchCardPrefixRange(low, high, limit)
                }
            }
            SearchFilter.NATIONAL_CODE -> {
                if (digitsOnly.length == 10) {
                    algorithm = "EXACT_NAT_CODE_10 [O(log N)]"
                    recordDao.searchExactNationalCode(digitsOnly, limit)
                } else {
                    algorithm = "NAT_CODE_B-TREE_RANGE [${cleanQuery}...]"
                    recordDao.searchNationalCodePrefixRange(low, high, limit)
                }
            }
            SearchFilter.MOBILE -> {
                if (digitsOnly.length == 11 && digitsOnly.startsWith("09")) {
                    algorithm = "EXACT_MOBILE_11 [O(log N)]"
                    recordDao.searchExactMobile(digitsOnly, limit)
                } else {
                    algorithm = "MOBILE_B-TREE_RANGE [${cleanQuery}...]"
                    recordDao.searchMobilePrefixRange(low, high, limit)
                }
            }
            SearchFilter.ACCOUNT -> {
                if (digitsOnly.isNotEmpty() && digitsOnly.length == cleanQuery.length) {
                    val exact = recordDao.searchExactAccount(digitsOnly, limit)
                    if (exact.isNotEmpty()) {
                        algorithm = "EXACT_ACCOUNT [O(log N)]"
                        exact
                    } else {
                        algorithm = "ACCOUNT_B-TREE_RANGE [${cleanQuery}...]"
                        recordDao.searchAccountPrefixRange(low, high, limit)
                    }
                } else {
                    algorithm = "ACCOUNT_B-TREE_RANGE [${cleanQuery}...]"
                    recordDao.searchAccountPrefixRange(low, high, limit)
                }
            }
            SearchFilter.NAME -> {
                algorithm = "CITIZEN_NAME_B-TREE_RANGE [${cleanQuery}...]"
                recordDao.searchNamePrefixRange(low, high, limit)
            }
            SearchFilter.CITY -> {
                algorithm = "CITY_B-TREE_RANGE [${cleanQuery}...]"
                recordDao.searchCityPrefixRange(low, high, limit)
            }
            SearchFilter.ALL -> {
                if (isNumeric) {
                    when {
                        digitsOnly.length == 16 -> {
                            algorithm = "EXACT_CARD_16 [O(log N)]"
                            matchedField = "CRD"
                            val exactList = recordDao.searchExactCard(digitsOnly, limit)
                            if (exactList.isNotEmpty()) exactList else queryNumericMultiIndex(low, high, limit)
                        }
                        digitsOnly.length == 10 -> {
                            algorithm = "EXACT_NAT_CODE_10 [O(log N)]"
                            matchedField = "NAT"
                            val exactList = recordDao.searchExactNationalCode(digitsOnly, limit)
                            if (exactList.isNotEmpty()) exactList else queryNumericMultiIndex(low, high, limit)
                        }
                        digitsOnly.length == 11 && digitsOnly.startsWith("09") -> {
                            algorithm = "EXACT_MOBILE_11 [O(log N)]"
                            matchedField = "MOB"
                            val exactList = recordDao.searchExactMobile(digitsOnly, limit)
                            if (exactList.isNotEmpty()) exactList else queryNumericMultiIndex(low, high, limit)
                        }
                        else -> {
                            algorithm = "MULTI_INDEX_B-TREE [CARD+NAT+MOB+ACC]"
                            matchedField = "MULTI"
                            queryNumericMultiIndex(low, high, limit)
                        }
                    }
                } else {
                    algorithm = "TEXT_MULTI_INDEX [NAME+CITY]"
                    matchedField = "TEXT"
                    queryTextMultiIndex(low, high, limit)
                }
            }
        }

        val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
        return SearchExecutionResult(
            records = results,
            executionTimeMs = elapsed,
            algorithmName = algorithm,
            matchedField = matchedField
        )
    }

    private suspend fun queryNumericMultiIndex(low: String, high: String, limit: Int): List<RecordEntity> {
        val perIndexLimit = (limit / 4).coerceAtLeast(25)
        val cards = recordDao.searchCardPrefixRange(low, high, perIndexLimit)
        val natCodes = recordDao.searchNationalCodePrefixRange(low, high, perIndexLimit)
        val mobiles = recordDao.searchMobilePrefixRange(low, high, perIndexLimit)
        val accounts = recordDao.searchAccountPrefixRange(low, high, perIndexLimit)
        val combined = ArrayList<RecordEntity>(cards.size + natCodes.size + mobiles.size + accounts.size)
        combined.addAll(cards)
        combined.addAll(natCodes)
        combined.addAll(mobiles)
        combined.addAll(accounts)
        return combined.distinctBy { it.id }.take(limit)
    }

    private suspend fun queryTextMultiIndex(low: String, high: String, limit: Int): List<RecordEntity> {
        val names = recordDao.searchNamePrefixRange(low, high, limit)
        val cities = recordDao.searchCityPrefixRange(low, high, limit / 2)
        val combined = ArrayList<RecordEntity>(names.size + cities.size)
        combined.addAll(names)
        combined.addAll(cities)
        return combined.distinctBy { it.id }.take(limit)
    }

    fun searchRecords(query: String, filter: SearchFilter): Flow<List<RecordEntity>> {
        val cleanQuery = csvImporter.normalizeDigits(query.trim())
        if (cleanQuery.isEmpty()) {
            return recordDao.getAllRecords(limit = 100)
        }
        val prefix = "$cleanQuery%"
        return when (filter) {
            SearchFilter.ALL -> {
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
                    recordDao.searchByNamePrefix(prefix)
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

    suspend fun importCsvUri(
        context: Context,
        uri: Uri,
        controller: ImportController = ImportController(),
        startRecordOffset: Int = 0,
        threadCount: Int = 8
    ): Flow<ImportProgress> {
        return csvImporter.importFromUri(context, uri, controller, startRecordOffset, threadCount)
    }

    suspend fun importCsvString(
        context: Context,
        content: String,
        name: String = "TEXT_IMPORT.csv",
        controller: ImportController = ImportController(),
        startRecordOffset: Int = 0,
        threadCount: Int = 8
    ): Flow<ImportProgress> {
        return csvImporter.importFromString(context, content, name, controller, startRecordOffset, threadCount)
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
