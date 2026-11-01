package com.example.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.RecordEntity
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
fun HudRecordDetailDialog(
    record: RecordEntity?,
    onDismiss: () -> Unit,
    onCopyField: (label: String, value: String) -> Unit,
    onCopyDossier: (RecordEntity) -> Unit,
    onShareRecord: (RecordEntity) -> Unit,
    onDeleteRecord: (Long) -> Unit
) {
    if (record == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("record_detail_dialog"),
            color = HudBackground,
            shape = RoundedCornerShape(4.dp),
            tonalElevation = 8.dp
        ) {
            HudFrameBox(
                modifier = Modifier.fillMaxWidth(),
                borderColor = HudPrimary.copy(alpha = 0.8f),
                glowColor = HudPrimary,
                cornerLength = 16.dp,
                backgroundColor = HudBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FINANCIAL DOSSIER",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                HudBadge(
                                    text = "SECURE",
                                    color = HudSecondary,
                                    indicatorOn = true
                                )
                            }
                            Text(
                                text = "SYSTEM ID: #%06d • FINANCIAL RECORD".format(record.id),
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 11.sp,
                                color = HudTextMuted
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("dialog_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Dialog",
                                tint = HudPrimary
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = HudBorder
                    )

                    // Scrollable Fields List
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // FULL NAME (RIGHT ALIGNED)
                        DossierItem(
                            fieldKey = "FULL_NAME (نام و نام خانوادگی)",
                            value = record.fullName,
                            accentColor = HudTextPrimary,
                            isRightAligned = true,
                            onCopy = { onCopyField("FULL_NAME", record.fullName) }
                        )

                        // FATHER NAME (RIGHT ALIGNED)
                        DossierItem(
                            fieldKey = "FATHER_NAME (نام پدر)",
                            value = record.fatherName,
                            accentColor = HudTextSecondary,
                            isRightAligned = true,
                            onCopy = { onCopyField("FATHER_NAME", record.fatherName) }
                        )

                        // BANK ISSUER (FINANCIAL IDENTITY)
                        if (record.cardNo.isNotEmpty()) {
                            DossierItem(
                                fieldKey = "BANK_ISSUER (نام بانک صادرکننده)",
                                value = record.bankIssuerName,
                                accentColor = HudAmber,
                                onCopy = { onCopyField("BANK_ISSUER", record.bankIssuerName) }
                            )
                        }

                        DossierItem(
                            fieldKey = "NATIONAL_CODE",
                            value = record.nationalCode,
                            accentColor = HudPrimary,
                            onCopy = { onCopyField("NATIONAL_CODE", record.nationalCode) }
                        )

                        DossierItem(
                            fieldKey = "MOBILE",
                            value = record.formattedMobile,
                            rawValue = record.mobile,
                            accentColor = HudSecondary,
                            onCopy = { onCopyField("MOBILE", record.mobile) }
                        )

                        DossierItem(
                            fieldKey = "CARD_NO",
                            value = record.formattedCardNumber,
                            rawValue = record.cardNo,
                            accentColor = HudAmber,
                            onCopy = { onCopyField("CARD_NO", record.cardNo) }
                        )

                        DossierItem(
                            fieldKey = "ACCOUNT_NUMBER",
                            value = record.accountNumber,
                            accentColor = HudTextSecondary,
                            onCopy = { onCopyField("ACCOUNT_NUMBER", record.accountNumber) }
                        )

                        DossierItem(
                            fieldKey = "ID_NUMBER",
                            value = record.idNumber,
                            accentColor = HudTextSecondary,
                            onCopy = { onCopyField("ID_NUMBER", record.idNumber) }
                        )

                        DossierItem(
                            fieldKey = "BIRTH_DATE",
                            value = record.birthDate,
                            accentColor = HudTextSecondary,
                            onCopy = { onCopyField("BIRTH_DATE", record.birthDate) }
                        )

                        DossierItem(
                            fieldKey = "CITY_NAME",
                            value = record.cityName,
                            accentColor = HudPrimary,
                            onCopy = { onCopyField("CITY_NAME", record.cityName) }
                        )

                        DossierItem(
                            fieldKey = "PROVINCE_NAME",
                            value = record.provinceName,
                            accentColor = HudTextSecondary,
                            onCopy = { onCopyField("PROVINCE_NAME", record.provinceName) }
                        )

                        DossierItem(
                            fieldKey = "BIRTH_CITY",
                            value = record.birthCity,
                            accentColor = HudTextSecondary,
                            onCopy = { onCopyField("BIRTH_CITY", record.birthCity) }
                        )

                        DossierItem(
                            fieldKey = "BIRTH_PROVINCE",
                            value = record.birthProvince,
                            accentColor = HudTextSecondary,
                            onCopy = { onCopyField("BIRTH_PROVINCE", record.birthProvince) }
                        )

                        DossierItem(
                            fieldKey = "ADDRESS",
                            value = record.address,
                            accentColor = HudTextPrimary,
                            isMultiLine = true,
                            isRightAligned = true,
                            onCopy = { onCopyField("ADDRESS", record.address) }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = HudBorder
                    )

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HudButton(
                            onClick = { onCopyDossier(record) },
                            text = "COPY ALL",
                            accentColor = HudPrimary,
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = HudPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        HudButton(
                            onClick = { onShareRecord(record) },
                            text = "SHARE",
                            accentColor = HudSecondary,
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = HudSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        HudButton(
                            onClick = {
                                onDeleteRecord(record.id)
                                onDismiss()
                            },
                            text = "DELETE",
                            accentColor = HudAlert,
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
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
}

@Composable
private fun DossierItem(
    fieldKey: String,
    value: String,
    accentColor: Color,
    rawValue: String? = null,
    isMultiLine: Boolean = false,
    isRightAligned: Boolean = false,
    onCopy: () -> Unit
) {
    val displayValue = if (value.isBlank()) "[EMPTY]" else value
    val isAvailable = value.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HudSurfaceElevated, RoundedCornerShape(2.dp))
            .border(1.dp, HudBorder, RoundedCornerShape(2.dp))
            .clickable(enabled = isAvailable) { onCopy() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = if (isMultiLine) Alignment.Top else Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start
        ) {
            Text(
                text = fieldKey,
                fontFamily = OrbitronFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = HudTextMuted,
                letterSpacing = 0.8.sp,
                textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = displayValue,
                fontFamily = ShareTechMonoFontFamily,
                fontSize = 14.sp,
                color = if (isAvailable) accentColor else HudTextMuted.copy(alpha = 0.5f),
                textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isAvailable) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $fieldKey",
                    tint = HudPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
