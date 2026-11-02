package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SearchFilter
import com.example.ui.HudViewModel
import com.example.ui.components.HudBadge
import com.example.ui.components.HudBiomedicalGate
import com.example.ui.components.HudButton
import com.example.ui.components.HudFrameBox
import com.example.ui.components.HudImportDialog
import com.example.ui.components.HudRecordCard
import com.example.ui.components.HudRecordDetailDialog
import com.example.ui.components.HudScanningBeam
import com.example.ui.components.HudSearchArchiveScanningView
import com.example.ui.components.HudSearchProcessingBanner
import com.example.ui.components.HudStatsDialog
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudBackground
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudBorderGlow
import com.example.ui.theme.HudPrimary
import com.example.ui.theme.HudSecondary
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.HudTextMuted
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.ShareTechMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun HudVaultScreen(
    viewModel: HudViewModel = viewModel()
) {
    val context = LocalContext.current

    // Register resilient ActivityResultLauncher at Screen level
    // Handles data URI, ClipData URI, cancellation statuses, and persistable permissions
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data ?: result.data?.clipData?.let { clip ->
                if (clip.itemCount > 0) clip.getItemAt(0).uri else null
            }
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                viewModel.importCsv(uri)
            } else {
                viewModel.setImportError("No file was selected from the file picker.")
            }
        } else {
            viewModel.setImportStatus("File selection cancelled or dismissed.")
        }
    }

    val launchStoragePicker: () -> Unit = {
        viewModel.setImportStatus("LAUNCHING UNIVERSAL STORAGE CHOOSER...")
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/*", "text/csv", "application/csv", "text/comma-separated-values", "*/*"))
            }
            val chooser = Intent.createChooser(intent, "Select CSV Data File")
            filePickerLauncher.launch(chooser)
        } catch (e1: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                filePickerLauncher.launch(fallback)
            } catch (e2: Exception) {
                viewModel.setImportError("STORAGE PICKER UNAVAILABLE: ${e2.localizedMessage}")
            }
        }
    }

    val launchDocsPicker: () -> Unit = {
        viewModel.setImportStatus("LAUNCHING SYSTEM DOCUMENTS (SAF) PICKER...")
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/*", "text/csv", "application/csv", "text/comma-separated-values", "*/*"))
            }
            filePickerLauncher.launch(intent)
        } catch (e1: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                filePickerLauncher.launch(Intent.createChooser(fallback, "Select CSV Data File"))
            } catch (e2: Exception) {
                viewModel.setImportError("DOCUMENTS PICKER UNAVAILABLE: ${e2.localizedMessage}")
            }
        }
    }

    val isSetupCompleted by viewModel.isSetupCompleted.collectAsStateWithLifecycle()
    val isBiomedicalLocked by viewModel.isBiomedicalLocked.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val totalRecords by viewModel.recordCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val searchLatency by viewModel.searchLatencyMs.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val isImportOpen by viewModel.isImportDialogOpen.collectAsStateWithLifecycle()
    val isStatsOpen by viewModel.isStatsDialogOpen.collectAsStateWithLifecycle()
    val selectedRecord by viewModel.selectedRecord.collectAsStateWithLifecycle()
    val flashMessage by viewModel.flashMessage.collectAsStateWithLifecycle()

    // Auto-dismiss toast flash message after 3 seconds
    LaunchedEffect(flashMessage) {
        if (flashMessage != null) {
            delay(3000)
            viewModel.clearFlashMessage()
        }
    }

    // FIRST OPEN: Show Security Setup Wizard to set Master Passcode and Biometric lock
    if (!isSetupCompleted) {
        HudSetupWizardScreen(
            onSetupCompleted = { password, enableBiometrics ->
                viewModel.completeSetup(password, enableBiometrics)
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = HudBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Top Tactical Financial Header
                    HudFrameBox(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = HudBorder,
                        glowColor = HudBorderGlow,
                        cornerLength = 8.dp,
                        backgroundColor = HudSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = HudPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "HUD FINANCIAL VAULT",
                                            fontFamily = OrbitronFontFamily,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HudPrimary,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        text = "FAST SQLITE B-TREE ENGINE • SHETAB COMPLIANT",
                                        fontFamily = ShareTechMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = HudTextMuted
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Lock Vault Button (Re-engage Real Biometric Gate)
                                    IconButton(
                                        onClick = { viewModel.lockBiomedical() },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(HudSurfaceElevated, RoundedCornerShape(2.dp))
                                            .border(1.dp, HudAmber.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                            .testTag("lock_vault_biomedical_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Lock with Biometrics",
                                            tint = HudAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Import Button
                                    IconButton(
                                        onClick = { viewModel.openImportDialog() },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(HudSurfaceElevated, RoundedCornerShape(2.dp))
                                            .border(1.dp, HudSecondary.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                            .testTag("open_import_dialog_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileUpload,
                                            contentDescription = "Import CSV",
                                            tint = HudSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Diagnostics / Stats Button
                                    IconButton(
                                        onClick = { viewModel.openStatsDialog() },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(HudSurfaceElevated, RoundedCornerShape(2.dp))
                                            .border(1.dp, HudPrimary.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                            .testTag("open_stats_dialog_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Database Diagnostics",
                                            tint = HudPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Status Telemetry Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    HudBadge(
                                        text = "RECORDS: %,d".format(totalRecords),
                                        color = HudPrimary,
                                        indicatorOn = false
                                    )
                                    HudBadge(
                                        text = "BIO-SECURE",
                                        color = HudSecondary,
                                        indicatorOn = true
                                    )
                                    HudBadge(
                                        text = "OFFLINE",
                                        color = HudAmber,
                                        indicatorOn = false
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = HudSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${searchLatency}ms",
                                        fontFamily = ShareTechMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HudSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Futuristic Scanning Beam
                    HudScanningBeam(color = HudPrimary)

                    // Active / Paused Ingestion Mini-HUD Floating Strip
                    if ((importProgress.isRunning || importProgress.isPaused) && !isImportOpen) {
                        HudFrameBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable { viewModel.openImportDialog() },
                            borderColor = if (importProgress.isPaused) HudAmber else HudSecondary,
                            glowColor = if (importProgress.isPaused) HudAmber else HudSecondary,
                            cornerLength = 6.dp,
                            backgroundColor = Color(0xFF040E18)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (importProgress.isPaused) Icons.Default.Pause else Icons.Default.FileUpload,
                                        contentDescription = null,
                                        tint = if (importProgress.isPaused) HudAmber else HudSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (importProgress.isPaused) "INGESTION PAUSED:" else "INGESTION ACTIVE:",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (importProgress.isPaused) HudAmber else HudSecondary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${String.format("%,d", importProgress.recordsImported)} / ${if (importProgress.totalRecords > 0) String.format("%,d", importProgress.totalRecords) else "---"} (${String.format("%.1f%%", importProgress.percent)})",
                                        fontFamily = ShareTechMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = HudTextPrimary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (importProgress.isPaused) {
                                        HudButton(
                                            onClick = { viewModel.resumeImport() },
                                            text = "RESUME",
                                            accentColor = HudSecondary,
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("mini_resume_btn")
                                        )
                                    } else {
                                        HudButton(
                                            onClick = { viewModel.pauseImport() },
                                            text = "PAUSE",
                                            accentColor = HudAmber,
                                            modifier = Modifier
                                                .height(34.dp)
                                                .testTag("mini_pause_btn")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // FAST FINANCIAL SEARCH CONSOLE INPUT
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        HudFrameBox(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = if (searchQuery.isNotEmpty()) HudPrimary else HudBorder,
                            glowColor = HudPrimary,
                            cornerLength = 10.dp,
                            backgroundColor = HudSurfaceElevated
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (activeFilter) {
                                        SearchFilter.CARD_NO -> Icons.Default.CreditCard
                                        SearchFilter.ACCOUNT -> Icons.Default.AccountBalanceWallet
                                        SearchFilter.NATIONAL_CODE -> Icons.Default.Fingerprint
                                        SearchFilter.MOBILE -> Icons.Default.PhoneAndroid
                                        SearchFilter.NAME -> Icons.Default.Person
                                        SearchFilter.CITY -> Icons.Default.LocationCity
                                        SearchFilter.ALL -> Icons.Default.Search
                                    },
                                    contentDescription = "Search",
                                    tint = if (searchQuery.isNotEmpty()) HudPrimary else HudTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.onSearchQueryChange(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 10.dp)
                                        .testTag("search_text_input"),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = HudTextPrimary,
                                        fontSize = 15.sp,
                                        fontFamily = ShareTechMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    cursorBrush = SolidColor(HudPrimary),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = when (activeFilter) {
                                                    SearchFilter.CARD_NO -> "ENTER 16-DIGIT CARD (e.g. 6037, 6104)..."
                                                    SearchFilter.ACCOUNT -> "ENTER BANK ACCOUNT NUMBER..."
                                                    SearchFilter.NATIONAL_CODE -> "ENTER CITIZEN NATIONAL CODE..."
                                                    SearchFilter.MOBILE -> "ENTER MOBILE (e.g. 0912, 0913)..."
                                                    SearchFilter.NAME -> "ENTER CITIZEN / FATHER NAME..."
                                                    SearchFilter.CITY -> "ENTER CITY / PROVINCE..."
                                                    SearchFilter.ALL -> "SEARCH FINANCIAL, CARD, ACCOUNT, CODE..."
                                                },
                                                color = HudTextMuted,
                                                fontSize = 11.sp,
                                                fontFamily = ShareTechMonoFontFamily
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onSearchQueryChange("") },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = HudPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tactical Search Processing HUD Banner
                    HudSearchProcessingBanner(
                        isSearching = searchState.isSearching,
                        query = searchQuery,
                        filter = activeFilter,
                        phase = searchState.phase,
                        elapsedMs = searchState.elapsedMs,
                        resultCount = records.size,
                        algorithm = searchState.algorithm,
                        matchedField = searchState.matchedField,
                        onAbortClick = { viewModel.cancelSearch() }
                    )

                    // FINANCIAL GRAPHIC CHIPS ROW
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchFilter.values().forEach { filter ->
                            val isSelected = filter == activeFilter
                            val chipIcon: ImageVector = when (filter) {
                                SearchFilter.ALL -> Icons.Default.AccountBalance
                                SearchFilter.CARD_NO -> Icons.Default.CreditCard
                                SearchFilter.ACCOUNT -> Icons.Default.AccountBalanceWallet
                                SearchFilter.NATIONAL_CODE -> Icons.Default.Fingerprint
                                SearchFilter.MOBILE -> Icons.Default.PhoneAndroid
                                SearchFilter.NAME -> Icons.Default.Person
                                SearchFilter.CITY -> Icons.Default.LocationCity
                            }

                            val chipAccent: Color = when (filter) {
                                SearchFilter.CARD_NO -> HudAmber
                                SearchFilter.ACCOUNT -> HudSecondary
                                SearchFilter.NATIONAL_CODE -> HudPrimary
                                SearchFilter.MOBILE -> Color(0xFF00E5FF)
                                SearchFilter.NAME -> HudTextPrimary
                                SearchFilter.CITY -> Color(0xFF70A5FF)
                                SearchFilter.ALL -> HudPrimary
                            }

                            HudFinancialGraphicChip(
                                label = filter.label,
                                code = filter.code,
                                icon = chipIcon,
                                isSelected = isSelected,
                                accentColor = chipAccent,
                                onClick = { viewModel.onFilterChange(filter) }
                            )
                        }
                    }

                    // Financial Query Status Summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    "QUERY RESULTS: ${records.size} MATCHES"
                                } else {
                                    "STORED: ${records.size} RECORDS"
                                },
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (records.isNotEmpty()) HudSecondary else HudAmber
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• [FILTER: ${activeFilter.code}]",
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 10.sp,
                                color = HudTextMuted
                            )
                        }

                        Text(
                            text = "SPEED: ${searchLatency}ms",
                            fontFamily = ShareTechMonoFontFamily,
                            fontSize = 10.sp,
                            color = HudPrimary
                        )
                    }

                    // Records List, Archive Radar Scanning, or Empty State
                    if (searchState.isSearching && records.isEmpty()) {
                        HudSearchArchiveScanningView(
                            query = searchQuery,
                            filter = activeFilter,
                            elapsedMs = searchState.elapsedMs,
                            phase = searchState.phase,
                            algorithm = searchState.algorithm,
                            onAbortClick = { viewModel.cancelSearch() },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (records.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HudFrameBox(
                                modifier = Modifier.fillMaxWidth(0.92f),
                                borderColor = HudBorder,
                                glowColor = HudPrimary.copy(alpha = 0.5f),
                                cornerLength = 14.dp,
                                backgroundColor = HudSurface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = HudPrimary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (totalRecords == 0) "DATABASE VAULT EMPTY" else "NO MATCHING RECORDS",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HudPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (totalRecords == 0) {
                                            "No records found in local vault. Select a CSV file with your 13-column financial citizen data to import."
                                        } else {
                                            "No records matched '${searchQuery}' under ${activeFilter.label}. Try selecting 'ALL FINANCIAL' or clear query."
                                        },
                                        fontFamily = ShareTechMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = HudTextSecondary,
                                        lineHeight = 16.sp
                                    )

                                    if (totalRecords == 0) {
                                        Spacer(modifier = Modifier.height(18.dp))
                                        HudButton(
                                            onClick = { viewModel.openImportDialog() },
                                            text = "SELECT CSV FILE TO IMPORT",
                                            accentColor = HudSecondary,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("import_csv_empty_state_button"),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.FileUpload,
                                                    contentDescription = null,
                                                    tint = HudSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("records_lazy_list"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = records,
                                key = { it.id }
                            ) { record ->
                                HudRecordCard(
                                    record = record,
                                    onInspectClick = { viewModel.selectRecord(it) },
                                    onCopyField = { label, value ->
                                        viewModel.copyToClipboard(label, value)
                                    }
                                )
                            }
                        }
                    }
                }

                // Floating Toast Flash Banner at bottom
                AnimatedVisibility(
                    visible = flashMessage != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    flashMessage?.let { msg ->
                        HudFrameBox(
                            modifier = Modifier.fillMaxWidth(0.95f),
                            borderColor = HudPrimary,
                            glowColor = HudPrimary,
                            cornerLength = 8.dp,
                            backgroundColor = Color(0xFF04121E)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(HudSecondary, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = msg,
                                    fontFamily = ShareTechMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // REAL BIOMETRIC SECURITY GATE OVERLAY (ENTRY PROTECTION)
        HudBiomedicalGate(
            isLocked = isBiomedicalLocked,
            onUnlocked = { viewModel.unlockBiomedical() }
        )
    }

    HudImportDialog(
        isOpen = isImportOpen,
        progress = importProgress,
        onDismiss = { viewModel.closeImportDialog() },
        onPickFromStorage = launchStoragePicker,
        onPickFromDocs = launchDocsPicker,
        onImportText = { text -> viewModel.importCsvString(text, "MANUAL_ENTRY.csv") },
        onLoadSampleBenchmark = { viewModel.loadSampleBenchmark() },
        onPauseImport = { viewModel.pauseImport() },
        onResumeImport = { viewModel.resumeImport() },
        onCancelImport = { viewModel.cancelImport() }
    )

    HudStatsDialog(
        isOpen = isStatsOpen,
        totalRecords = totalRecords,
        onDismiss = { viewModel.closeStatsDialog() },
        onExportCsv = { viewModel.exportAndShareCsv(context) },
        onClearDatabase = { viewModel.clearDatabase() }
    )

    HudRecordDetailDialog(
        record = selectedRecord,
        onDismiss = { viewModel.selectRecord(null) },
        onCopyField = { label, value -> viewModel.copyToClipboard(label, value) },
        onCopyDossier = { rec -> viewModel.copyDossier(rec) },
        onShareRecord = { rec -> viewModel.shareRecord(context, rec) },
        onDeleteRecord = { id -> viewModel.deleteRecord(id) }
    )
}

/**
 * HIGH-TECH GRAPHICAL CHIP COMPONENT
 * Features cybernetic corner bevels, vector icon, active neon glow, and sub-label
 */
@Composable
private fun HudFinancialGraphicChip(
    label: String,
    code: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) accentColor else HudBorder
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.22f) else Color(0xFF061019)

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(bgColor, RoundedCornerShape(3.dp))
            .border(1.2.dp, borderColor, RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .drawBehind {
                if (isSelected) {
                    val cornerPx = 6.dp.toPx()
                    drawLine(
                        color = accentColor,
                        start = Offset(0f, 0f),
                        end = Offset(cornerPx, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = accentColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, cornerPx),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            .testTag("filter_chip_${code}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) accentColor else HudTextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                fontFamily = OrbitronFontFamily,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else HudTextSecondary,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "[${code}]",
                fontFamily = ShareTechMonoFontFamily,
                fontSize = 8.sp,
                color = if (isSelected) accentColor.copy(alpha = 0.8f) else HudTextMuted
            )
        }
    }
}
