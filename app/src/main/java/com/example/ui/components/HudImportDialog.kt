package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ImportProgress
import com.example.ui.theme.HudAlert
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudBackground
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudBorderGlow
import com.example.ui.theme.HudPrimary
import com.example.ui.theme.HudSecondary
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.HudTextMuted
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.ShareTechMonoFontFamily

@Composable
fun HudImportDialog(
    isOpen: Boolean,
    progress: ImportProgress,
    onDismiss: () -> Unit,
    onPickFromStorage: () -> Unit,
    onPickFromDocs: () -> Unit,
    onImportText: (String) -> Unit,
    onLoadSampleBenchmark: () -> Unit,
    onPauseImport: () -> Unit,
    onResumeImport: () -> Unit,
    onCancelImport: () -> Unit
) {
    if (!isOpen) return

    val clipboardManager = LocalClipboardManager.current
    var showManualInput by remember { mutableStateOf(false) }
    var manualCsvText by remember { mutableStateOf("") }
    var inlineError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("import_dialog"),
            color = HudBackground,
            shape = RoundedCornerShape(4.dp),
            tonalElevation = 8.dp
        ) {
            HudFrameBox(
                modifier = Modifier.fillMaxWidth(),
                borderColor = HudSecondary.copy(alpha = 0.8f),
                glowColor = HudSecondary,
                cornerLength = 16.dp,
                backgroundColor = HudBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "IMPORT CSV REPOSITORY",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                HudBadge(
                                    text = if (progress.isPaused) "PAUSED" else if (progress.isRunning) "STREAMING" else "READY",
                                    color = if (progress.isPaused) HudAmber else HudSecondary,
                                    indicatorOn = true
                                )
                            }
                            Text(
                                text = "STREAMING INGESTION TO ROOM DB • 100M ROWS READY",
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 11.sp,
                                color = HudTextMuted
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("import_dialog_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = HudPrimary
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        thickness = 1.dp,
                        color = HudBorder
                    )

                    // Scrollable Info & Progress Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Live Process Bar, Record Counter & Telemetry Banner
                        if (progress.isRunning || progress.isPaused || progress.recordsImported > 0 || progress.error != null || progress.statusMessage.isNotEmpty() || inlineError != null) {
                            val isError = progress.error != null || inlineError != null
                            val isSuccess = progress.isFinished && !isError
                            val isPaused = progress.isPaused
                            val bannerColor = if (isError) HudAlert else if (isSuccess) HudSecondary else if (isPaused) HudAmber else HudPrimary

                            val targetFraction = (progress.percent / 100f).coerceIn(0f, 1f)
                            val animatedProgress by animateFloatAsState(
                                targetValue = if ((progress.isRunning || isPaused) && targetFraction < 0.02f) 0.02f else targetFraction,
                                label = "import_progress_anim"
                            )

                            fun formatTime(sec: Int): String {
                                val m = sec / 60
                                val s = sec % 60
                                return String.format("%02d:%02d", m, s)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HudSurfaceElevated, RoundedCornerShape(2.dp))
                                    .border(1.dp, bannerColor, RoundedCornerShape(2.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Process Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isError) Icons.Default.ErrorOutline
                                                else if (isSuccess) Icons.Default.CheckCircle
                                                else if (isPaused) Icons.Default.Pause
                                                else Icons.Default.FileUpload,
                                                contentDescription = null,
                                                tint = bannerColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isError) "INGESTION NOTICE"
                                                else if (isSuccess) "DATABASE INGESTION COMPLETED"
                                                else if (isPaused) "DATABASE INGESTION PAUSED (ZERO DATA LOST)"
                                                else "DATABASE INGESTION PROCESS",
                                                fontFamily = OrbitronFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = bannerColor
                                            )
                                        }

                                        // Large Percentage readout
                                        Text(
                                            text = if (isPaused) "[ PAUSED ]" else String.format("[ %.1f%% ]", progress.percent),
                                            fontFamily = OrbitronFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSuccess) HudSecondary else if (isPaused) HudAmber else HudPrimary
                                        )
                                    }

                                    // Dynamic Tactical HUD Process Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(22.dp)
                                            .background(Color(0xFF040E18), RoundedCornerShape(2.dp))
                                            .border(1.dp, HudBorder, RoundedCornerShape(2.dp))
                                            .padding(2.dp)
                                    ) {
                                        // Filled track with neon gradient
                                        if (animatedProgress > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(animatedProgress)
                                                    .background(
                                                        brush = Brush.horizontalGradient(
                                                            colors = if (isSuccess) {
                                                                listOf(HudSecondary.copy(alpha = 0.8f), HudSecondary)
                                                            } else if (isPaused) {
                                                                listOf(HudAmber.copy(alpha = 0.85f), HudAmber)
                                                            } else {
                                                                listOf(HudPrimary, HudSecondary)
                                                            }
                                                        ),
                                                        shape = RoundedCornerShape(1.dp)
                                                    )
                                            )
                                        }

                                        // Centered text inside the process bar
                                        Text(
                                            text = if (progress.totalRecords > 0) {
                                                "${String.format("%,d", progress.recordsImported)} / ${String.format("%,d", progress.totalRecords)} RECORDS"
                                            } else if (progress.recordsImported > 0) {
                                                "${String.format("%,d", progress.recordsImported)} RECORDS INDEXED"
                                            } else {
                                                "CALCULATING RECORD VOLUME..."
                                            },
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HudTextPrimary,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }

                                    // Record Count & Rate row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "COUNTED: ${String.format("%,d", progress.recordsImported)} / ${if (progress.totalRecords > 0) String.format("%,d", progress.totalRecords) else "---"} ${if (progress.isTotalEstimated) "(ESTIMATED)" else "(EXACT)"}",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSuccess) HudSecondary else if (isPaused) HudAmber else HudTextPrimary
                                        )

                                        if (progress.recordsPerSecond > 0 && !isPaused) {
                                            Text(
                                                text = "${String.format("%,d", progress.recordsPerSecond)} RECS/SEC",
                                                fontFamily = ShareTechMonoFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = HudAmber
                                            )
                                        } else if (isPaused) {
                                            Text(
                                                text = "PAUSED (SAFE)",
                                                fontFamily = ShareTechMonoFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = HudAmber
                                            )
                                        }
                                    }

                                    // Telemetry Grid: Elapsed Time, ETA, and Batch
                                    if (progress.isRunning || progress.isPaused || progress.isFinished) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF02070D), RoundedCornerShape(2.dp))
                                                .border(0.5.dp, HudBorder)
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "ELAPSED: ${formatTime(progress.elapsedSeconds.toInt())}",
                                                fontFamily = ShareTechMonoFontFamily,
                                                fontSize = 10.sp,
                                                color = HudTextSecondary
                                            )
                                            Text(
                                                text = if (isSuccess) "STATUS: COMPLETE" else if (isPaused) "STATUS: PAUSED" else if (progress.etaSeconds > 0) "ETA: ${formatTime(progress.etaSeconds)}" else "ETA: CALCULATING...",
                                                fontFamily = ShareTechMonoFontFamily,
                                                fontSize = 10.sp,
                                                color = if (isSuccess) HudSecondary else HudAmber
                                            )
                                            if (progress.currentBatch > 0) {
                                                Text(
                                                    text = "BATCH #${progress.currentBatch}",
                                                    fontFamily = ShareTechMonoFontFamily,
                                                    fontSize = 10.sp,
                                                    color = HudPrimary
                                                )
                                            }
                                        }
                                    }

                                    if (progress.fileName.isNotEmpty()) {
                                        Text(
                                            text = "SOURCE: ${progress.fileName} ${if (progress.fileSizeFormatted.isNotEmpty()) "(${progress.fileSizeFormatted})" else ""}",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = HudTextSecondary
                                        )
                                    }

                                    if (progress.statusMessage.isNotEmpty()) {
                                        Text(
                                            text = "> ${progress.statusMessage}",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = HudTextPrimary
                                        )
                                    }

                                    val activeError = inlineError ?: progress.error
                                    if (activeError != null) {
                                        Text(
                                            text = "DETAIL: $activeError",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = HudAlert
                                        )
                                    }

                                    // TACTICAL HUD PAUSE / RESUME / ABORT INTERACTIVE CONTROLS
                                    if (progress.isRunning || progress.isPaused) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (progress.isPaused) {
                                                HudButton(
                                                    onClick = onResumeImport,
                                                    text = "▶ RESUME INGESTION",
                                                    accentColor = HudSecondary,
                                                    modifier = Modifier
                                                        .weight(1.4f)
                                                        .testTag("btn_resume_import"),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = HudSecondary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                )
                                            } else {
                                                HudButton(
                                                    onClick = onPauseImport,
                                                    text = "|| PAUSE INGESTION",
                                                    accentColor = HudAmber,
                                                    modifier = Modifier
                                                        .weight(1.4f)
                                                        .testTag("btn_pause_import"),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Pause,
                                                            contentDescription = null,
                                                            tint = HudAmber,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                )
                                            }

                                            HudButton(
                                                onClick = onCancelImport,
                                                text = "ABORT",
                                                accentColor = HudAlert,
                                                modifier = Modifier
                                                    .weight(0.9f)
                                                    .testTag("btn_abort_import"),
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = null,
                                                        tint = HudAlert,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Required Columns Mapping Spec
                        Text(
                            text = "SUPPORTED CSV SCHEMA (13 COLUMNS AUTO-MAPPED)",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HudPrimary
                        )

                        val columns = listOf(
                            "NATIONAL_CODE" to "کد ملی (Citizen Melli Code)",
                            "ACCOUNT_NUMBER" to "شماره حساب (Bank Account No)",
                            "FULL_NAME" to "نام و نام خانوادگی (Full Name)",
                            "FATHER_NAME" to "نام پدر (Father's Name)",
                            "ID_NUMBER" to "شماره شناسنامه (Certificate ID)",
                            "BIRTH_DATE" to "تاریخ تولد (YYYY/MM/DD)",
                            "CITY_NAME" to "شهر سکونت (Current City)",
                            "PROVINCE_NAME" to "استان (Province)",
                            "BIRTH_CITY" to "محل تولد (Birth City)",
                            "BIRTH_PROVINCE" to "استان تولد (Birth Province)",
                            "ADDRESS" to "آدرس (Street Address)",
                            "CARD_NO" to "شماره کارت (16-digit Shetab Card)",
                            "MOBILE" to "شماره همراه (Mobile Phone)"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF060E16))
                                .border(1.dp, HudBorder)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            columns.forEachIndexed { idx, (name, desc) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${idx + 1}. $name",
                                        fontFamily = ShareTechMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HudTextPrimary
                                    )
                                    Text(
                                        text = desc,
                                        fontFamily = ShareTechMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = HudTextMuted
                                    )
                                }
                            }
                        }

                        Text(
                            text = "• Compatible with Comma (,), Semicolon (;), Tab, and Pipe (|) delimiters.\n• Auto-normalizes Persian/Arabic digits and removes UTF-8 BOM automatically.",
                            fontFamily = ShareTechMonoFontFamily,
                            fontSize = 10.sp,
                            color = HudTextSecondary
                        )

                        // Collapsible Direct Paste / Manual Ingestion Section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManualInput = !showManualInput }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = HudAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DIRECT TEXT / CLIPBOARD INGESTION",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudAmber
                                )
                            }
                            Icon(
                                imageVector = if (showManualInput) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = HudAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = showManualInput) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF040B13), RoundedCornerShape(2.dp))
                                    .border(1.dp, HudBorder)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Paste or type CSV lines directly (ideal if device file chooser is blocked):",
                                    fontFamily = ShareTechMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = HudTextSecondary
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .background(HudBackground)
                                        .border(1.dp, HudBorder)
                                        .padding(8.dp)
                                ) {
                                    if (manualCsvText.isEmpty()) {
                                        Text(
                                            text = "Paste CSV rows here...\nExample:\n0012345678,123456789,Ali Rezaei,Hassan,...",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = HudTextMuted
                                        )
                                    }
                                    BasicTextField(
                                        value = manualCsvText,
                                        onValueChange = {
                                            manualCsvText = it
                                            inlineError = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = HudTextPrimary,
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 11.sp
                                        ),
                                        cursorBrush = SolidColor(HudSecondary)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HudButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                manualCsvText = clip
                                                inlineError = null
                                            } else {
                                                inlineError = "CLIPBOARD EMPTY: Copy CSV rows to clipboard first."
                                            }
                                        },
                                        text = "PASTE CLIPBOARD",
                                        accentColor = HudAmber,
                                        modifier = Modifier.weight(1f)
                                    )

                                    HudButton(
                                        onClick = {
                                            if (manualCsvText.isNotBlank()) {
                                                onImportText(manualCsvText)
                                                manualCsvText = ""
                                            } else {
                                                inlineError = "INPUT EMPTY: Enter or paste CSV text before importing."
                                            }
                                        },
                                        text = "INGEST TEXT",
                                        accentColor = HudSecondary,
                                        enabled = manualCsvText.isNotBlank() && !progress.isRunning,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        thickness = 1.dp,
                        color = HudBorder
                    )

                    // Actions Column - Universal Chooser, Documents SAF, Direct Clipboard, and Benchmark
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Primary: Universal Storage Chooser (Opens phone storage, Downloads, My Files)
                        HudButton(
                            onClick = {
                                inlineError = null
                                onPickFromStorage()
                            },
                            text = "SELECT CSV FROM PHONE STORAGE",
                            accentColor = HudSecondary,
                            enabled = !progress.isRunning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_csv_file_button"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = HudSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        // Secondary: System Documents (SAF) Alternative Picker
                        HudButton(
                            onClick = {
                                inlineError = null
                                onPickFromDocs()
                            },
                            text = "ALTERNATIVE DOCUMENTS PICKER",
                            accentColor = HudPrimary,
                            enabled = !progress.isRunning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_csv_doc_button"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = null,
                                    tint = HudPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        // Quick Benchmark Loader
                        HudButton(
                            onClick = {
                                inlineError = null
                                onLoadSampleBenchmark()
                            },
                            text = "LOAD SAMPLE BENCHMARK (100 CITIZENS)",
                            accentColor = HudAmber,
                            enabled = !progress.isRunning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("load_benchmark_button"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Dataset,
                                    contentDescription = null,
                                    tint = HudAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
