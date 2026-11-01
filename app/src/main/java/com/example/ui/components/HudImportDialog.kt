package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
    onLoadSampleBenchmark: () -> Unit
) {
    if (!isOpen) return

    val clipboardManager = LocalClipboardManager.current
    var showManualInput by remember { mutableStateOf(false) }
    var manualCsvText by remember { mutableStateOf("") }
    var inlineError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {
            if (!progress.isRunning) onDismiss()
        },
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
                                    text = "OFFLINE ENGINE",
                                    color = HudSecondary,
                                    indicatorOn = true
                                )
                            }
                            Text(
                                text = "STREAMING INGESTION TO ROOM DB",
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 11.sp,
                                color = HudTextMuted
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            enabled = !progress.isRunning,
                            modifier = Modifier.testTag("import_dialog_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (progress.isRunning) HudTextMuted else HudPrimary
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
                        // Live Progress / Status / Error Banner (always visible when there is telemetry or status)
                        if (progress.isRunning || progress.recordsImported > 0 || progress.error != null || progress.statusMessage.isNotEmpty() || inlineError != null) {
                            val isError = progress.error != null || inlineError != null
                            val isSuccess = progress.isFinished && !isError
                            val bannerColor = if (isError) HudAlert else if (isSuccess) HudSecondary else HudAmber

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HudSurfaceElevated, RoundedCornerShape(2.dp))
                                    .border(1.dp, bannerColor, RoundedCornerShape(2.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isError) Icons.Default.ErrorOutline
                                                else if (isSuccess) Icons.Default.CheckCircle
                                                else Icons.Default.FileUpload,
                                                contentDescription = null,
                                                tint = bannerColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isError) "INGESTION NOTICE"
                                                else if (isSuccess) "IMPORT SUCCESSFUL"
                                                else "TELEMETRY STATUS",
                                                fontFamily = OrbitronFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = bannerColor
                                            )
                                        }

                                        if (progress.isRunning) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = HudSecondary
                                            )
                                        }
                                    }

                                    if (progress.fileName.isNotEmpty()) {
                                        Text(
                                            text = "SOURCE: ${progress.fileName} ${if (progress.fileSizeFormatted.isNotEmpty()) "(${progress.fileSizeFormatted})" else ""}",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = HudTextSecondary
                                        )
                                    }

                                    if (progress.statusMessage.isNotEmpty()) {
                                        Text(
                                            text = "> ${progress.statusMessage}",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 12.sp,
                                            color = HudTextPrimary
                                        )
                                    }

                                    if (progress.recordsImported > 0) {
                                        Text(
                                            text = "INDEXED: ${progress.recordsImported} RECORDS",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HudSecondary
                                        )
                                    }

                                    if (progress.recordsPerSecond > 0) {
                                        Text(
                                            text = "INGESTION SPEED: ${progress.recordsPerSecond} RECS/SEC",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = HudAmber
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
