package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.HudAlert
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudBackground
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudPrimary
import com.example.ui.theme.HudSecondary
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.HudTextMuted
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.ShareTechMonoFontFamily

@Composable
fun HudStatsDialog(
    isOpen: Boolean,
    totalRecords: Int,
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onClearDatabase: () -> Unit
) {
    if (!isOpen) return

    var showConfirmWipe by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stats_dialog"),
            color = HudBackground,
            shape = RoundedCornerShape(4.dp)
        ) {
            HudFrameBox(
                modifier = Modifier.fillMaxWidth(),
                borderColor = HudPrimary,
                glowColor = HudPrimary,
                cornerLength = 14.dp,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VAULT DIAGNOSTICS",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            HudBadge(text = "SYS: OK", color = HudSecondary)
                        }

                        IconButton(onClick = onDismiss) {
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

                    // Stats Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatRow(label = "TOTAL PERSISTED RECORDS", value = "%,d".format(totalRecords), valueColor = HudPrimary)
                        StatRow(label = "STORAGE ENGINE", value = "ROOM / SQLITE (OFFLINE)", valueColor = HudSecondary)
                        StatRow(label = "INDEXED KEYS", value = "7 ACTIVE B-TREE INDICES", valueColor = HudAmber)
                        StatRow(label = "SEARCH LATENCY", value = "< 2 ms (LOCAL DISK)", valueColor = HudSecondary)
                        StatRow(label = "MEMORY PROFILE", value = "STREAM CHUNKED (O(1))", valueColor = HudTextSecondary)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = HudBorder
                    )

                    // Operations
                    if (!showConfirmWipe) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HudButton(
                                onClick = onExportCsv,
                                text = "EXPORT TO CSV",
                                accentColor = HudSecondary,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = HudSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )

                            HudButton(
                                onClick = { showConfirmWipe = true },
                                text = "PURGE DATABASE",
                                accentColor = HudAlert,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = null,
                                        tint = HudAlert,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    } else {
                        // Wipe confirmation
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E0A0E))
                                .border(1.dp, HudAlert)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CONFIRM PURGE ALL RECORDS?",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudAlert
                            )
                            Text(
                                text = "This will permanently remove all $totalRecords records from device storage.",
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 11.sp,
                                color = HudTextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HudButton(
                                    onClick = { showConfirmWipe = false },
                                    text = "CANCEL",
                                    accentColor = HudTextMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                HudButton(
                                    onClick = {
                                        onClearDatabase()
                                        showConfirmWipe = false
                                        onDismiss()
                                    },
                                    text = "YES, WIPE",
                                    accentColor = HudAlert,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HudSurfaceElevated)
            .border(1.dp, HudBorder)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = ShareTechMonoFontFamily,
            fontSize = 11.sp,
            color = HudTextMuted
        )
        Text(
            text = value,
            fontFamily = ShareTechMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
