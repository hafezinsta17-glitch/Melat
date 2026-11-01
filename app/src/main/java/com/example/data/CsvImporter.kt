package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
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
    val isFinished: Boolean = false,
    val fileName: String = "",
    val fileSizeFormatted: String = ""
)

class CsvImporter(private val recordDao: RecordDao) {

    /**
     * Converts Persian and Arabic digits to standard ASCII digits and trims invisible control chars
     */
    fun normalizeDigits(input: String): String {
        val sb = java.lang.StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '\u0660'..'\u0669' -> sb.append((ch - '\u0660' + '0'.code).toChar())
                in '\u06F0'..'\u06F9' -> sb.append((ch - '\u06F0' + '0'.code).toChar())
                '\uFEFF', '\u200B', '\u200C', '\u200D', '\u200E', '\u200F' -> {
                    // Skip BOM and zero-width markers
                }
                else -> sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    /**
     * Detects delimiter (comma, semicolon, tab, pipe) by checking candidate frequency outside quotes
     */
    fun detectDelimiter(sampleLines: List<String>): Char {
        val candidates = listOf(',', ';', '\t', '|')
        val scores = mutableMapOf<Char, Int>()
        for (c in candidates) scores[c] = 0

        for (line in sampleLines.take(5)) {
            var inQuotes = false
            for (ch in line) {
                if (ch == '"') {
                    inQuotes = !inQuotes
                } else if (!inQuotes && ch in candidates) {
                    scores[ch] = (scores[ch] ?: 0) + 1
                }
            }
        }

        // Return candidate with highest count; default to comma
        val best = scores.maxByOrNull { it.value }
        return if (best != null && best.value > 0) best.key else ','
    }

    /**
     * Parses a single CSV line into tokens using the specified delimiter, respecting quotes and escaped quotes.
     */
    fun parseCsvLine(line: String, delimiter: Char = ','): List<String> {
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
                c == delimiter && !inQuotes -> {
                    tokens.add(cleanToken(sb.toString()))
                    sb.setLength(0)
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        tokens.add(cleanToken(sb.toString()))
        return tokens
    }

    private fun cleanToken(token: String): String {
        return token.trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .replace("\"\"", "\"")
            .replace("\uFEFF", "")
            .trim()
    }

    /**
     * Normalizes a header token to compare against canonical column keys
     */
    private fun sanitizeHeader(token: String): String {
        return token.uppercase()
            .replace("\uFEFF", "")
            .replace(" ", "_")
            .replace("-", "_")
            .replace("\"", "")
            .trim()
    }

    /**
     * Detects column indices from header row (supports both English and Persian banking headers)
     */
    private fun resolveColumnIndices(headers: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        val sanitized = headers.map { sanitizeHeader(it) }

        fun findIndex(vararg candidates: String): Int {
            for (candidate in candidates) {
                val idx = sanitized.indexOfFirst { it == candidate || it.contains(candidate) }
                if (idx != -1) return idx
            }
            return -1
        }

        map["NATIONAL_CODE"] = findIndex(
            "NATIONAL_CODE", "NATIONALCODE", "MELLI_CODE", "CODE_MELI", "NATIONAL_ID",
            "کد_ملی", "کدملی", "کد ملی", "کد"
        )
        map["ACCOUNT_NUMBER"] = findIndex(
            "ACCOUNT_NUMBER", "ACCOUNT_NO", "ACCOUNTNUMBER", "ACCOUNT", "HESAB",
            "شماره_حساب", "شماره حساب", "حساب"
        )
        map["FULL_NAME"] = findIndex(
            "FULL_NAME", "FULLNAME", "NAME_AND_FAMILY", "NAME",
            "نام_و_نام_خانوادگی", "نام و نام خانوادگی", "نام_خانوادگی", "نام"
        )
        map["FATHER_NAME"] = findIndex(
            "FATHER_NAME", "FATHERNAME", "FATHER",
            "نام_پدر", "نام پدر", "پدر"
        )
        map["ID_NUMBER"] = findIndex(
            "ID_NUMBER", "ID_NO", "IDNUMBER", "SHENASNAME", "CERTIFICATE_NO",
            "شماره_شناسنامه", "شماره شناسنامه", "شناسنامه"
        )
        map["BIRTH_DATE"] = findIndex(
            "BIRTH_DATE", "BIRTHDATE", "DATE_OF_BIRTH", "TARIKH_TAVALOD",
            "تاریخ_تولد", "تاریخ تولد", "تولد"
        )
        map["CITY_NAME"] = findIndex(
            "CITY_NAME", "CITYNAME", "CITY", "SHAHR",
            "شهر", "نام_شهر", "شهر_سکونت"
        )
        map["PROVINCE_NAME"] = findIndex(
            "PROVINCE_NAME", "PROVINCENAME", "PROVINCE", "OSTAN",
            "استان", "نام_استان"
        )
        map["BIRTH_CITY"] = findIndex(
            "BIRTH_CITY", "BIRTHCITY", "TAVALOD_SHAHR",
            "شهر_تولد", "محل_تولد", "صادره"
        )
        map["BIRTH_PROVINCE"] = findIndex(
            "BIRTH_PROVINCE", "BIRTHPROVINCE", "TAVALOD_OSTAN",
            "استان_تولد"
        )
        map["ADDRESS"] = findIndex(
            "ADDRESS", "ADDR", "POSTAL_ADDRESS", "NESHANI",
            "آدرس", "ادرس", "نشانی", "منزل"
        )
        map["CARD_NO"] = findIndex(
            "CARD_NO", "CARD_NUMBER", "CARDNO", "CARDNUMBER", "KART",
            "شماره_کارت", "شماره کارت", "کارت"
        )
        map["MOBILE"] = findIndex(
            "MOBILE", "PHONE", "MOBILE_NO", "MOBILE_NUMBER", "CELLPHONE", "TEL",
            "شماره_موبایل", "شماره موبایل", "موبایل", "همراه", "تلفن"
        )

        return map
    }

    /**
     * Resolves display name and size from a content Uri
     */
    fun queryUriMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = "storage_data.csv"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to URI path
            uri.lastPathSegment?.let { name = it }
        }
        return Pair(name, size)
    }

    /**
     * Imports from an Android content Uri by staging to a local cache file first.
     * This avoids all Storage Access Framework permission expiration and stream timeout issues.
     */
    suspend fun importFromUri(context: Context, uri: Uri): Flow<ImportProgress> {
        val (fileName, reportedSize) = queryUriMetadata(context, uri)
        val formattedSize = if (reportedSize > 0) {
            when {
                reportedSize >= 1024 * 1024 -> String.format("%.2f MB", reportedSize / (1024f * 1024f))
                reportedSize >= 1024 -> String.format("%.1f KB", reportedSize / 1024f)
                else -> "$reportedSize bytes"
            }
        } else {
            "Storage File"
        }

        val cacheFile = withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "vault_import_staging_${System.currentTimeMillis()}.csv")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalArgumentException("Could not open read channel to storage file: $fileName")
            tempFile
        }

        return importFile(cacheFile, fileName, formattedSize)
    }

    /**
     * Imports from a raw String or Clipboard content by staging to a local cache file.
     */
    suspend fun importFromString(
        context: Context,
        content: String,
        sourceName: String = "CLIPBOARD_DATA.csv"
    ): Flow<ImportProgress> {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        val formattedSize = when {
            bytes.size >= 1024 * 1024 -> String.format("%.2f MB", bytes.size / (1024f * 1024f))
            bytes.size >= 1024 -> String.format("%.1f KB", bytes.size / 1024f)
            else -> "${bytes.size} bytes"
        }

        val cacheFile = withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "vault_text_staging_${System.currentTimeMillis()}.csv")
            tempFile.writeBytes(bytes)
            tempFile
        }

        return importFile(cacheFile, sourceName, formattedSize)
    }

    /**
     * Imports records from a local File into Room database with batching, progress reporting, and auto-cleanup.
     */
    fun importFile(
        file: File,
        fileName: String,
        fileSizeFormatted: String,
        batchSize: Int = 500
    ): Flow<ImportProgress> = flow {
        emit(
            ImportProgress(
                isRunning = true,
                statusMessage = "EXAMINING STORAGE REPOSITORY: $fileName ($fileSizeFormatted)...",
                fileName = fileName,
                fileSizeFormatted = fileSizeFormatted
            )
        )

        if (!file.exists() || file.length() == 0L) {
            file.delete()
            emit(
                ImportProgress(
                    isRunning = false,
                    isFinished = true,
                    statusMessage = "FILE EMPTY",
                    error = "Selected file '$fileName' contains no data (0 bytes).",
                    fileName = fileName,
                    fileSizeFormatted = fileSizeFormatted
                )
            )
            return@flow
        }

        try {
            // Sample first few lines to detect delimiter and encoding
            val sampleLines = mutableListOf<String>()
            BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use { br ->
                var count = 0
                while (count < 10) {
                    val l = br.readLine() ?: break
                    if (l.isNotBlank()) {
                        sampleLines.add(l)
                        count++
                    }
                }
            }

            if (sampleLines.isEmpty()) {
                emit(
                    ImportProgress(
                        isRunning = false,
                        isFinished = true,
                        statusMessage = "NO DATA ROWS",
                        error = "Selected file '$fileName' has no valid text lines.",
                        fileName = fileName,
                        fileSizeFormatted = fileSizeFormatted
                    )
                )
                return@flow
            }

            val delimiter = detectDelimiter(sampleLines)
            val firstLine = sampleLines.first()
            val firstTokens = parseCsvLine(firstLine, delimiter)

            val hasHeader = firstTokens.any { token ->
                val s = sanitizeHeader(token)
                s.contains("NATIONAL") || s.contains("CODE") || s.contains("NAME") ||
                        s.contains("ACCOUNT") || s.contains("MOBILE") || s.contains("CARD") ||
                        s.contains("CITY") || s.contains("PROVINCE") || s.contains("کد") ||
                        s.contains("نام") || s.contains("حساب") || s.contains("کارت") ||
                        s.contains("تولد") || s.contains("موبایل") || s.contains("شهر")
            }

            val columnMapping = if (hasHeader) {
                resolveColumnIndices(firstTokens)
            } else {
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

            BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8), 32768).use { reader ->
                if (hasHeader) {
                    reader.readLine() // Skip header
                }

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val current = line ?: continue
                    if (current.isBlank()) continue
                    lineNumber++

                    val tokens = parseCsvLine(current, delimiter)
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
                                statusMessage = "INGESTING: $totalImported RECORDS INDEXED (${rate} RECS/S)",
                                fileName = fileName,
                                fileSizeFormatted = fileSizeFormatted
                            )
                        )
                    }
                }

                if (batch.isNotEmpty()) {
                    recordDao.insertBatch(batch)
                    totalImported += batch.size
                    batch.clear()
                }
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
                    statusMessage = "COMPLETED: $totalImported RECORDS INGESTED IN ${String.format("%.1f", totalTime)}s ($finalRate RECS/S)",
                    fileName = fileName,
                    fileSizeFormatted = fileSizeFormatted
                )
            )
        } catch (e: Exception) {
            emit(
                ImportProgress(
                    isRunning = false,
                    isFinished = true,
                    error = e.localizedMessage ?: "Import operation failed",
                    statusMessage = "CRITICAL INGESTION ERROR",
                    fileName = fileName,
                    fileSizeFormatted = fileSizeFormatted
                )
            )
        } finally {
            // Auto clean staging cache file
            try {
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Exports all database records into CSV string format
     */
    suspend fun exportToCsvString(): String = withContext(Dispatchers.IO) {
        val records = recordDao.getAllRecordsSync()
        val sb = java.lang.StringBuilder()
        // Header
        sb.append("NATIONAL_CODE,ACCOUNT_NUMBER,FULL_NAME,FATHER_NAME,ID_NUMBER,BIRTH_DATE,CITY_NAME,PROVINCE_NAME,BIRTH_CITY,BIRTH_PROVINCE,ADDRESS,CARD_NO,MOBILE\n")

        fun escape(s: String): String {
            return if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains(";")) {
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
