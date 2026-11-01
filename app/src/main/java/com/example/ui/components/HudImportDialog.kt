package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.ui.platform.LocalContext
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
    onFileSelected: (Uri) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var lastSelectedName by remember { mutableStateOf<String?>(null) }

    // UNIVERSAL FILE PICKER: Uses ACTION_GET_CONTENT with Intent.createChooser
    // Works across ALL phone file managers (Samsung My Files, Xiaomi File Manager, Downloads, Drive, etc.)
    val universalPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                lastSelectedName = uri.lastPathSegment ?: "storage_file.csv"
                onFileSelected(uri)
            } else {
                Toast.makeText(context, "No file selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ALTERNATIVE SYSTEM STORAGE PICKER: Uses ACTION_OPEN_DOCUMENT with wildcard
    val systemDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            lastSelectedName = uri.lastPathSegment ?: "document_file.csv"
            onFileSelected(uri)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!progress.isRunning) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
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
                        // Live Progress / Ingestion Banner
                        if (progress.isRunning || progress.recordsImported > 0 || progress.error != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HudSurfaceElevated, RoundedCornerShape(2.dp))
                                    .border(
                                        1.dp,
                                        if (progress.error != null) HudAlert
                                        else if (progress.isFinished) HudSecondary
                                        else HudAmber,
                                        RoundedCornerShape(2.dp)
                                    )
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
                                                imageVector = if (progress.error != null) Icons.Default.ErrorOutline
                                                else if (progress.isFinished) Icons.Default.CheckCircle
                                                else Icons.Default.FileUpload,
                                                contentDescription = null,
                                                tint = if (progress.error != null) HudAlert
                                                else if (progress.isFinished) HudSecondary
                                                else HudAmber,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (progress.error != null) "INGESTION ERROR"
                                                else if (progress.isFinished) "IMPORT SUCCESSFUL"
                                                else "STREAMING INGESTION ACTIVE",
                                                fontFamily = OrbitronFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (progress.error != null) HudAlert
                                                else if (progress.isFinished) HudSecondary
                                                else HudAmber
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
                                            text = "SOURCE: ${progress.fileName} (${progress.fileSizeFormatted})",
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

                                    if (progress.recordsPerSecond > 0) {
                                        Text(
                                            text = "TELEMETRY SPEED: ${progress.recordsPerSecond} RECS/SEC",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = HudAmber
                                        )
                                    }

                                    if (progress.error != null) {
                                        Text(
                                            text = "CAUSE: ${progress.error}",
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
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        thickness = 1.dp,
                        color = HudBorder
                    )

                    // Actions Row - Dual pickers to ensure 100% compatibility on any phone
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Primary: Universal Storage Chooser (Opens phone storage, Downloads, My Files)
                        HudButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        putExtra(Intent.EXTRA_LOCAL_ONLY, false)
                                    }
                                    universalPicker.launch(Intent.createChooser(intent, "Choose CSV File from Phone Storage"))
                                } catch (e: Exception) {
                                    // Fallback to direct document picker
                                    systemDocPicker.launch(arrayOf("*/*"))
                                }
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

                        // Secondary: Alternative System Documents Picker
                        HudButton(
                            onClick = {
                                systemDocPicker.launch(arrayOf("*/*"))
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
                    }
                }
            }
        }
    }
}
