package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.SampleDataProvider
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

    // System File Picker for CSV or Text files
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
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
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
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
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = HudBorder
                    )

                    // Scrollable Info & Progress Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Live Progress Section
                        if (progress.isRunning || progress.recordsImported > 0 || progress.error != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HudSurfaceElevated, RoundedCornerShape(2.dp))
                                    .border(1.dp, if (progress.error != null) HudAlert else HudSecondary, RoundedCornerShape(2.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (progress.isRunning) "STREAMING INGESTION ACTIVE" else "PROCESS FINISHED",
                                            fontFamily = OrbitronFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (progress.error != null) HudAlert else HudSecondary
                                        )

                                        if (progress.isRunning) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = HudSecondary
                                            )
                                        }
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
                                            text = "INGESTION SPEED: ${progress.recordsPerSecond} RECS/SEC",
                                            fontFamily = ShareTechMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = HudAmber
                                        )
                                    }

                                    if (progress.error != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = HudAlert,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ERROR: ${progress.error}",
                                                fontFamily = ShareTechMonoFontFamily,
                                                fontSize = 11.sp,
                                                color = HudAlert
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Required Columns Mapping Spec
                        Text(
                            text = "MAPPED CSV COLUMN SCHEMA (13 FIELDS)",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HudPrimary
                        )

                        val columns = listOf(
                            "NATIONAL_CODE" to "Melli / Citizen ID (Indexed)",
                            "ACCOUNT_NUMBER" to "Bank Account Number",
                            "FULL_NAME" to "First and Last Name",
                            "FATHER_NAME" to "Father's Name",
                            "ID_NUMBER" to "Birth Certificate ID",
                            "BIRTH_DATE" to "Date of Birth (YYYY/MM/DD)",
                            "CITY_NAME" to "Current City Residence",
                            "PROVINCE_NAME" to "Province / State",
                            "BIRTH_CITY" to "City of Birth",
                            "BIRTH_PROVINCE" to "Province of Birth",
                            "ADDRESS" to "Postal / Street Address",
                            "CARD_NO" to "16-digit Bank Card No",
                            "MOBILE" to "Cell Phone Number (Indexed)"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF060E16))
                                .border(1.dp, HudBorder)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            text = "Note: Header row is auto-detected. Persian/Arabic digits are normalized to ASCII automatically during import.",
                            fontFamily = ShareTechMonoFontFamily,
                            fontSize = 11.sp,
                            color = HudTextSecondary
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = HudBorder
                    )

                    // Actions Row
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HudButton(
                            onClick = {
                                filePicker.launch(arrayOf("text/*", "text/csv", "application/csv", "*/*"))
                            },
                            text = "SELECT CSV FILE",
                            accentColor = HudSecondary,
                            enabled = !progress.isRunning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_csv_file_button"),
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
    }
}
