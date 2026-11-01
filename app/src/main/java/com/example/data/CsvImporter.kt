package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class ImportProgress(
    val isRunning: Boolean = false,
    val recordsImported: Int = 0,
    val totalLinesEstimate: Int = 0,
    val currentBatch: Int = 0,
    val percent: Float = 0f,
    val recordsPerSecond: Int = 0,
    val statusMessage: String = "",
    val error: String? = null,
    val isFinished: Boolean = false
)

class CsvImporter(private val recordDao: RecordDao) {

    /**
     * Converts Persian and Arabic digits to standard ASCII digits
     */
    fun normalizeDigits(input: String): String {
        val sb = java.lang.StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '\u0660'..'\u0669' -> sb.append((ch - '\u0660' + '0'.code).toChar())
                in '\u06F0'..'\u06F9' -> sb.append((ch - '\u06F0' + '0'.code).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    /**
     * Parses a single CSV line into tokens, respecting quotes, commas inside quotes, and escaped quotes.
     */
    fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Normalizes a header token to compare against canonical column keys
     */
    private fun sanitizeHeader(token: String): String {
        return token.uppercase()
            .replace(" ", "_")
            .replace("-", "_")
            .replace("\"", "")
            .trim()
    }

    /**
     * Detects column indices from header row or falls back to standard order
     */
    private fun resolveColumnIndices(headers: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val sanitized = headers.map { sanitizeHeader(it) }

        // Find each field index by canonical or common alias
        fun findIndex(vararg candidates: String): Int {
            for (candidate in candidates) {
                val idx = sanitized.indexOfFirst { it == candidate || it.contains(candidate) }
                if (idx != -1) return idx
            }
            return -1
        }

        map["NATIONAL_CODE"] = findIndex("NATIONAL_CODE", "NATIONALCODE", "MELLI_CODE", "CODE_MELI", "NATIONAL_ID")
        map["ACCOUNT_NUMBER"] = findIndex("ACCOUNT_NUMBER", "ACCOUNT_NO", "ACCOUNTNUMBER", "ACCOUNT", "HESAB")
        map["FULL_NAME"] = findIndex("FULL_NAME", "FULLNAME", "NAME_AND_FAMILY", "NAME")
        map["FATHER_NAME"] = findIndex("FATHER_NAME", "FATHERNAME", "FATHER")
        map["ID_NUMBER"] = findIndex("ID_NUMBER", "ID_NO", "IDNUMBER", "SHENASNAME", "CERTIFICATE_NO")
        map["BIRTH_DATE"] = findIndex("BIRTH_DATE", "BIRTHDATE", "DATE_OF_BIRTH", "TARIKH_TAVALOD")
        map["CITY_NAME"] = findIndex("CITY_NAME", "CITYNAME", "CITY", "SHAHR")
        map["PROVINCE_NAME"] = findIndex("PROVINCE_NAME", "PROVINCENAME", "PROVINCE", "OSTAN")
        map["BIRTH_CITY"] = findIndex("BIRTH_CITY", "BIRTHCITY", "TAVALOD_SHAHR")
        map["BIRTH_PROVINCE"] = findIndex("BIRTH_PROVINCE", "BIRTHPROVINCE", "TAVALOD_OSTAN")
        map["ADDRESS"] = findIndex("ADDRESS", "ADDR", "POSTAL_ADDRESS", "NESHANI")
        map["CARD_NO"] = findIndex("CARD_NO", "CARD_NUMBER", "CARDNO", "CARDNUMBER", "KART")
        map["MOBILE"] = findIndex("MOBILE", "PHONE", "MOBILE_NO", "MOBILE_NUMBER", "CELLPHONE", "TEL")

        return map
    }

    /**
     * Imports a CSV stream directly into Room database with batching and progress updates
     */
    fun importStream(
        inputStream: InputStream,
        batchSize: Int = 500
    ): Flow<ImportProgress> = flow {
        emit(
            ImportProgress(
                isRunning = true,
                statusMessage = "INITIALIZING IMPORT ENGINE..."
            )
        )

        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val firstLine = reader.readLine()

        if (firstLine == null) {
            emit(
                ImportProgress(
                    isRunning = false,
                    isFinished = true,
                    statusMessage = "EMPTY FILE",
                    error = "CSV file is empty."
                )
            )
            return@flow
        }

        val firstTokens = parseCsvLine(firstLine)
        val hasHeader = firstTokens.any { token ->
            val s = sanitizeHeader(token)
            s.contains("NATIONAL") || s.contains("CODE") || s.contains("NAME") ||
                    s.contains("ACCOUNT") || s.contains("MOBILE") || s.contains("CARD") ||
                    s.contains("CITY") || s.contains("PROVINCE")
        }

        val columnMapping = if (hasHeader) {
            resolveColumnIndices(firstTokens)
        } else {
            // Default 13-column sequential mapping
            mapOf(
                "NATIONAL_CODE" to 0,
                "ACCOUNT_NUMBER" to 1,
                "FULL_NAME" to 2,
                "FATHER_NAME" to 3,
                "ID_NUMBER" to 4,
                "BIRTH_DATE" to 5,
                "CITY_NAME" to 6,
                "PROVINCE_NAME" to 7,
                "BIRTH_CITY" to 8,
                "BIRTH_PROVINCE" to 9,
                "ADDRESS" to 10,
                "CARD_NO" to 11,
                "MOBILE" to 12
            )
        }

        fun extractField(tokens: List<String>, key: String, fallbackIndex: Int): String {
            val idx = columnMapping[key] ?: fallbackIndex
            return if (idx in tokens.indices) {
                normalizeDigits(tokens[idx])
            } else {
                ""
            }
        }

        val batch = mutableListOf<RecordEntity>()
        var totalImported = 0
        var lineNumber = 0
        val startTime = System.currentTimeMillis()

        // If first line was data (no header), parse it now
        if (!hasHeader) {
            val record = RecordEntity(
                nationalCode = extractField(firstTokens, "NATIONAL_CODE", 0),
                accountNumber = extractField(firstTokens, "ACCOUNT_NUMBER", 1),
                fullName = extractField(firstTokens, "FULL_NAME", 2),
                fatherName = extractField(firstTokens, "FATHER_NAME", 3),
                idNumber = extractField(firstTokens, "ID_NUMBER", 4),
                birthDate = extractField(firstTokens, "BIRTH_DATE", 5),
                cityName = extractField(firstTokens, "CITY_NAME", 6),
                provinceName = extractField(firstTokens, "PROVINCE_NAME", 7),
                birthCity = extractField(firstTokens, "BIRTH_CITY", 8),
                birthProvince = extractField(firstTokens, "BIRTH_PROVINCE", 9),
                address = extractField(firstTokens, "ADDRESS", 10),
                cardNo = extractField(firstTokens, "CARD_NO", 11),
                mobile = extractField(firstTokens, "MOBILE", 12)
            )
            batch.add(record)
            lineNumber++
        }

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val current = line ?: continue
            if (current.isBlank()) continue
            lineNumber++

            val tokens = parseCsvLine(current)
            if (tokens.all { it.isBlank() }) continue

            val record = RecordEntity(
                nationalCode = extractField(tokens, "NATIONAL_CODE", 0),
                accountNumber = extractField(tokens, "ACCOUNT_NUMBER", 1),
                fullName = extractField(tokens, "FULL_NAME", 2),
                fatherName = extractField(tokens, "FATHER_NAME", 3),
                idNumber = extractField(tokens, "ID_NUMBER", 4),
                birthDate = extractField(tokens, "BIRTH_DATE", 5),
                cityName = extractField(tokens, "CITY_NAME", 6),
                provinceName = extractField(tokens, "PROVINCE_NAME", 7),
                birthCity = extractField(tokens, "BIRTH_CITY", 8),
                birthProvince = extractField(tokens, "BIRTH_PROVINCE", 9),
                address = extractField(tokens, "ADDRESS", 10),
                cardNo = extractField(tokens, "CARD_NO", 11),
                mobile = extractField(tokens, "MOBILE", 12)
            )
            batch.add(record)

            if (batch.size >= batchSize) {
                recordDao.insertBatch(batch)
                totalImported += batch.size
                batch.clear()

                val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.1f)
                val rate = (totalImported / elapsedSec).toInt()

                emit(
                    ImportProgress(
                        isRunning = true,
                        recordsImported = totalImported,
                        totalLinesEstimate = lineNumber,
                        recordsPerSecond = rate,
                        statusMessage = "STREAMING: $totalImported RECORDS SAVED"
                    )
                )
            }
        }

        if (batch.isNotEmpty()) {
            recordDao.insertBatch(batch)
            totalImported += batch.size
            batch.clear()
        }

        val totalTime = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.1f)
        val finalRate = (totalImported / totalTime).toInt()

        emit(
            ImportProgress(
                isRunning = false,
                isFinished = true,
                recordsImported = totalImported,
                totalLinesEstimate = lineNumber,
                recordsPerSecond = finalRate,
                statusMessage = "BATCH COMPLETED: $totalImported RECORDS INDEXED IN ${String.format("%.1f", totalTime)}s"
            )
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Imports from an Android content Uri (via Storage Access Framework)
     */
    suspend fun importFromUri(context: Context, uri: Uri): Flow<ImportProgress> {
        val inputStream = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")
        }
        return importStream(inputStream)
    }

    /**
     * Exports all database records into CSV string format
     */
    suspend fun exportToCsvString(): String = withContext(Dispatchers.IO) {
        val records = recordDao.getAllRecordsSync()
        val sb = java.lang.StringBuilder()
        // Header
        sb.append("NATIONAL_CODE,ACCOUNT_NUMBER,FULL_NAME,FATHER_NAME,ID_NUMBER,BIRTH_DATE,CITY_NAME,PROVINCE_NAME,BIRTH_CITY,BIRTH_PROVINCE,ADDRESS,CARD_NO,MOBILE\n")

        fun escape(s: String): String {
            return if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                "\"${s.replace("\"", "\"\"")}\""
            } else {
                s
            }
        }

        for (r in records) {
            sb.append(escape(r.nationalCode)).append(",")
                .append(escape(r.accountNumber)).append(",")
                .append(escape(r.fullName)).append(",")
                .append(escape(r.fatherName)).append(",")
                .append(escape(r.idNumber)).append(",")
                .append(escape(r.birthDate)).append(",")
                .append(escape(r.cityName)).append(",")
                .append(escape(r.provinceName)).append(",")
                .append(escape(r.birthCity)).append(",")
                .append(escape(r.birthProvince)).append(",")
                .append(escape(r.address)).append(",")
                .append(escape(r.cardNo)).append(",")
                .append(escape(r.mobile)).append("\n")
        }
        sb.toString()
    }
}
