package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CsvImporter
import com.example.data.ImportController
import com.example.data.ImportProgress
import com.example.data.RecordEntity
import com.example.data.RecordRepository
import com.example.data.SearchFilter
import com.example.security.SecurityPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Detailed Search Processing State for real-time HUD visual telemetry.
 */
data class SearchProcessingState(
    val isSearching: Boolean = false,
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val phase: String = "IDLE",
    val elapsedMs: Long = 0L,
    val resultCount: Int = 0,
    val algorithm: String = "PRIMARY_KEY_HEAD [O(1)]",
    val matchedField: String = "ALL",
    val canCancel: Boolean = false
)

class HudViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val importer = CsvImporter(database.recordDao())
    val repository = RecordRepository(database.recordDao(), importer)
    private val securityPreferences = SecurityPreferences(application)

    // First open setup wizard state
    private val _isSetupCompleted = MutableStateFlow(securityPreferences.isSetupCompleted)
    val isSetupCompleted = _isSetupCompleted.asStateFlow()

    // Biomedical security state - defaults to locked on entry
    private val _isBiomedicalLocked = MutableStateFlow(true)
    val isBiomedicalLocked = _isBiomedicalLocked.asStateFlow()

    val recordCount: StateFlow<Int> = repository.getRecordCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(SearchFilter.ALL)
    val activeFilter = _activeFilter.asStateFlow()

    private val _importProgress = MutableStateFlow(ImportProgress())
    val importProgress = _importProgress.asStateFlow()

    private val _selectedRecord = MutableStateFlow<RecordEntity?>(null)
    val selectedRecord = _selectedRecord.asStateFlow()

    private val _flashMessage = MutableStateFlow<String?>(null)
    val flashMessage = _flashMessage.asStateFlow()

    private val _isImportDialogOpen = MutableStateFlow(false)
    val isImportDialogOpen = _isImportDialogOpen.asStateFlow()

    private val _isStatsDialogOpen = MutableStateFlow(false)
    val isStatsDialogOpen = _isStatsDialogOpen.asStateFlow()

    private val _searchLatencyMs = MutableStateFlow(1L)
    val searchLatencyMs = _searchLatencyMs.asStateFlow()

    // Real-Time Search Processing HUD State
    private val _searchState = MutableStateFlow(SearchProcessingState())
    val searchState = _searchState.asStateFlow()

    private val _records = MutableStateFlow<List<RecordEntity>>(emptyList())
    val records: StateFlow<List<RecordEntity>> = _records.asStateFlow()

    private var searchJob: Job? = null
    private var timerJob: Job? = null
    private var activeImportJob: Job? = null
    private var activeImportController: ImportController? = null

    init {
        // Initial load of latest database records
        triggerSearch("", SearchFilter.ALL)
    }

    fun completeSetup(password: String, enableBiometrics: Boolean) {
        securityPreferences.saveMasterPassword(password, enableBiometrics)
        _isSetupCompleted.value = true
        _isBiomedicalLocked.value = false
        showFlash("VAULT INITIALIZED • MASTER PASSCODE DEPLOYED")
    }

    fun unlockBiomedical() {
        _isBiomedicalLocked.value = false
        showFlash("BIOMETRICS VERIFIED • VAULT UNLOCKED")
    }

    fun lockBiomedical() {
        _isBiomedicalLocked.value = true
        showFlash("VAULT SECURED • BIOMETRICS ENGAGED")
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        triggerSearch(query, _activeFilter.value)
    }

    fun onFilterChange(filter: SearchFilter) {
        _activeFilter.value = filter
        triggerSearch(_searchQuery.value, filter)
    }

    /**
     * Executes non-blocking, indexed search on Dispatchers.IO with real-time millisecond
     * telemetry and cooperative coroutine cancellation.
     */
    private fun triggerSearch(query: String, filter: SearchFilter) {
        searchJob?.cancel()
        timerJob?.cancel()

        val trimmedQuery = query.trim()
        val startTime = System.currentTimeMillis()

        _searchState.value = SearchProcessingState(
            isSearching = true,
            query = trimmedQuery,
            filter = filter,
            phase = if (trimmedQuery.isEmpty()) "RETRIEVING LATEST ARCHIVE ENTRIES..." else "INDEX SEEK: SCANNING 100M RECORD B-TREE...",
            elapsedMs = 0L,
            resultCount = _records.value.size,
            algorithm = if (trimmedQuery.isEmpty()) "PRIMARY_KEY_HEAD [O(1)]" else "O(log N) B-TREE RANGE",
            matchedField = filter.code,
            canCancel = true
        )

        // Live ~60fps millisecond ticking coroutine so UI constantly shows active movement
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(16)
                val currentElapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                _searchState.value = _searchState.value.copy(elapsedMs = currentElapsed)
            }
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (trimmedQuery.isNotEmpty()) {
                    delay(35) // Small 35ms debounce for smooth keyboard typing
                }

                _searchState.value = _searchState.value.copy(phase = "TRAVERSING B-TREE LEAF PAGES...")
                val result = repository.searchRecordsFast(trimmedQuery, filter, limit = 100)
                val finalElapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)

                timerJob?.cancel()
                _records.value = result.records
                _searchLatencyMs.value = finalElapsed
                _searchState.value = SearchProcessingState(
                    isSearching = false,
                    query = trimmedQuery,
                    filter = filter,
                    phase = "INDEX QUERY RESOLVED",
                    elapsedMs = finalElapsed,
                    resultCount = result.records.size,
                    algorithm = result.algorithmName,
                    matchedField = result.matchedField,
                    canCancel = false
                )
            } catch (e: CancellationException) {
                // Cancelled cleanly due to new query or user abort
            } catch (e: Exception) {
                timerJob?.cancel()
                _searchState.value = _searchState.value.copy(
                    isSearching = false,
                    phase = "ERROR: ${e.message}",
                    canCancel = false
                )
                showFlash("SEARCH ERROR: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Instantly aborts an active search operation if user desires.
     */
    fun cancelSearch() {
        searchJob?.cancel()
        timerJob?.cancel()
        _searchState.value = _searchState.value.copy(
            isSearching = false,
            phase = "SEARCH OPERATION ABORTED BY USER",
            canCancel = false
        )
        showFlash("SEARCH ABORTED • ENGINE READY")
    }

    fun openImportDialog() {
        _isImportDialogOpen.value = true
    }

    fun closeImportDialog() {
        _isImportDialogOpen.value = false
    }

    fun openStatsDialog() {
        _isStatsDialogOpen.value = true
    }

    fun closeStatsDialog() {
        _isStatsDialogOpen.value = false
    }

    fun selectRecord(record: RecordEntity?) {
        _selectedRecord.value = record
    }

    fun clearFlashMessage() {
        _flashMessage.value = null
    }

    fun showFlash(message: String) {
        _flashMessage.value = message
    }

    fun setImportStatus(status: String) {
        _importProgress.value = _importProgress.value.copy(
            isRunning = false,
            statusMessage = status,
            error = null
        )
    }

    fun setImportError(error: String) {
        _importProgress.value = _importProgress.value.copy(
            isRunning = false,
            isFinished = true,
            error = error,
            statusMessage = "SELECTION FAILED"
        )
        showFlash("ERROR: $error")
    }

    fun pauseImport() {
        val ctrl = activeImportController
        if (ctrl != null && !ctrl.isPaused) {
            ctrl.pause()
            _importProgress.value = _importProgress.value.copy(
                isRunning = false,
                isPaused = true,
                canResume = true,
                statusMessage = "PAUSE REQUESTED • COMMITTING CURRENT BATCH (ZERO DATA LOSS)..."
            )
            showFlash("IMPORT PAUSED • PROGRESS PRESERVED")
        }
    }

    fun resumeImport() {
        val ctrl = activeImportController
        if (ctrl != null && ctrl.isPaused) {
            ctrl.resume()
            _importProgress.value = _importProgress.value.copy(
                isRunning = true,
                isPaused = false,
                canResume = false,
                statusMessage = "RESUMING INGESTION STREAM..."
            )
            showFlash("IMPORT RESUMED • STREAMING BATCHES")
        }
    }

    fun cancelImport() {
        val ctrl = activeImportController
        ctrl?.cancel()
        activeImportJob?.cancel()
        activeImportController = null
        val currentProgress = _importProgress.value
        _importProgress.value = currentProgress.copy(
            isRunning = false,
            isPaused = false,
            canResume = false,
            isFinished = true,
            statusMessage = "IMPORT CANCELLED BY OPERATOR • ${String.format("%,d", currentProgress.recordsImported)} RECORDS COMMITTED"
        )
        showFlash("IMPORT CANCELLED")
        triggerSearch(_searchQuery.value, _activeFilter.value)
    }

    fun importCsv(uri: Uri) {
        activeImportJob?.cancel()
        val controller = ImportController()
        activeImportController = controller

        activeImportJob = viewModelScope.launch {
            try {
                _importProgress.value = ImportProgress(
                    isRunning = true,
                    statusMessage = "OPENING SECURE DATA CHANNEL..."
                )
                repository.importCsvUri(getApplication(), uri, controller).collect { progress ->
                    _importProgress.value = progress
                    if (progress.isFinished && progress.error == null) {
                        showFlash("IMPORT COMPLETE: ${String.format("%,d", progress.recordsImported)} RECORDS INDEXED")
                        triggerSearch(_searchQuery.value, _activeFilter.value)
                        activeImportController = null
                    }
                }
            } catch (e: CancellationException) {
                _importProgress.value = _importProgress.value.copy(
                    isRunning = false,
                    isPaused = false,
                    statusMessage = "IMPORT CANCELLED"
                )
            } catch (e: Exception) {
                _importProgress.value = ImportProgress(
                    isRunning = false,
                    isFinished = true,
                    error = e.localizedMessage ?: "Failed to import CSV",
                    statusMessage = "CRITICAL: IMPORT ABORTED"
                )
                showFlash("IMPORT FAILED: ${e.message}")
                activeImportController = null
            }
        }
    }

    fun importCsvString(content: String, sourceName: String = "CLIPBOARD_DATA.csv") {
        activeImportJob?.cancel()
        val controller = ImportController()
        activeImportController = controller

        activeImportJob = viewModelScope.launch {
            try {
                _importProgress.value = ImportProgress(
                    isRunning = true,
                    statusMessage = "PARSING CSV STREAM...",
                    fileName = sourceName
                )
                repository.importCsvString(getApplication(), content, sourceName, controller).collect { progress ->
                    _importProgress.value = progress
                    if (progress.isFinished && progress.error == null) {
                        showFlash("IMPORT COMPLETE: ${String.format("%,d", progress.recordsImported)} RECORDS INDEXED")
                        triggerSearch(_searchQuery.value, _activeFilter.value)
                        activeImportController = null
                    }
                }
            } catch (e: CancellationException) {
                _importProgress.value = _importProgress.value.copy(
                    isRunning = false,
                    isPaused = false,
                    statusMessage = "IMPORT CANCELLED"
                )
            } catch (e: Exception) {
                _importProgress.value = ImportProgress(
                    isRunning = false,
                    isFinished = true,
                    error = e.localizedMessage ?: "Failed to parse CSV",
                    statusMessage = "CRITICAL: IMPORT ABORTED"
                )
                showFlash("IMPORT FAILED: ${e.message}")
                activeImportController = null
            }
        }
    }

    fun loadSampleBenchmark() {
        activeImportJob?.cancel()
        val controller = ImportController()
        activeImportController = controller

        activeImportJob = viewModelScope.launch {
            try {
                _importProgress.value = ImportProgress(
                    isRunning = true,
                    statusMessage = "LOADING BENCHMARK REPOSITORY (100 CITIZENS)...",
                    fileName = "MELAT_BENCHMARK_100.csv"
                )
                val sampleCsv = com.example.data.SampleDataProvider.getSampleCsvString()
                repository.importCsvString(getApplication(), sampleCsv, "MELAT_BENCHMARK_100.csv", controller).collect { progress ->
                    _importProgress.value = progress
                    if (progress.isFinished && progress.error == null) {
                        showFlash("BENCHMARK LOADED: ${progress.recordsImported} CITIZENS ACTIVE")
                        triggerSearch(_searchQuery.value, _activeFilter.value)
                        activeImportController = null
                    }
                }
            } catch (e: CancellationException) {
                _importProgress.value = _importProgress.value.copy(
                    isRunning = false,
                    isPaused = false,
                    statusMessage = "BENCHMARK IMPORT CANCELLED"
                )
            } catch (e: Exception) {
                _importProgress.value = ImportProgress(
                    isRunning = false,
                    isFinished = true,
                    error = e.localizedMessage ?: "Benchmark load failed",
                    statusMessage = "LOAD FAILED"
                )
                activeImportController = null
            }
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id)
            showFlash("RECORD #$id PURGED FROM VAULT")
            triggerSearch(_searchQuery.value, _activeFilter.value)
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            val purged = repository.clearDatabase()
            showFlash("DATABASE PURGED: $purged RECORDS REMOVED")
            triggerSearch("", _activeFilter.value)
        }
    }

    fun copyToClipboard(label: String, value: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        clipboard.setPrimaryClip(clip)
        showFlash("COPIED $label: $value")
    }

    fun copyDossier(record: RecordEntity) {
        val dossier = """
            [FINANCIAL HUD RECORD DOSSIER #%05d]
            BANK_ISSUER: %s
            NATIONAL_CODE: %s
            ACCOUNT_NUMBER: %s
            FULL_NAME: %s
            FATHER_NAME: %s
            ID_NUMBER: %s
            BIRTH_DATE: %s
            CITY_NAME: %s
            PROVINCE_NAME: %s
            BIRTH_CITY: %s
            BIRTH_PROVINCE: %s
            ADDRESS: %s
            CARD_NO: %s
            MOBILE: %s
        """.trimIndent().format(
            record.id,
            record.bankIssuerName,
            record.nationalCode,
            record.accountNumber,
            record.fullName,
            record.fatherName,
            record.idNumber,
            record.birthDate,
            record.cityName,
            record.provinceName,
            record.birthCity,
            record.birthProvince,
            record.address,
            record.cardNo,
            record.mobile
        )
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Record Dossier #${record.id}", dossier)
        clipboard.setPrimaryClip(clip)
        showFlash("DOSSIER #%05d COPIED TO CLIPBOARD".format(record.id))
    }

    fun shareRecord(context: Context, record: RecordEntity) {
        val dossier = """
            FINANCIAL HUD DOSSIER #%05d
            --------------------------------
            Bank: %s
            Full Name: %s
            Father Name: %s
            National Code: %s
            Card No: %s
            Account No: %s
            Mobile: %s
            City: %s (%s)
            Address: %s
        """.trimIndent().format(
            record.id,
            record.bankIssuerName,
            record.fullName,
            record.fatherName,
            record.nationalCode,
            record.formattedCardNumber,
            record.accountNumber,
            record.formattedMobile,
            record.cityName,
            record.provinceName,
            record.address
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Financial Dossier #${record.id} - ${record.fullName}")
            putExtra(Intent.EXTRA_TEXT, dossier)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Financial Dossier").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun exportAndShareCsv(context: Context) {
        viewModelScope.launch {
            val csv = repository.exportCsv()
            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "HUD_Financial_Export.csv")
                    putExtra(Intent.EXTRA_TEXT, csv)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Export Database to").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                showFlash("CSV EXPORT READY FOR SHARING")
            }
        }
    }
}
