package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.RecordEntity
import com.example.ui.theme.HudAlert
import com.example.ui.theme.HudAmber
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
fun HudRecordCard(
    record: RecordEntity,
    onInspectClick: (RecordEntity) -> Unit,
    onCopyField: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    HudFrameBox(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onInspectClick(record) }
            .testTag("record_card_${record.id}"),
        backgroundColor = HudSurfaceElevated,
        borderColor = HudBorder,
        glowColor = HudBorderGlow,
        cornerLength = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: ID + Financial Bank/Status + Inspect Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "REC::#%05d".format(record.id),
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    HudBadge(
                        text = if (record.nationalCode.isNotEmpty()) "FINANCIAL-VERIFIED" else "RAW",
                        color = if (record.nationalCode.isNotEmpty()) HudSecondary else HudAmber,
                        indicatorOn = true
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onInspectClick(record) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("inspect_button_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Inspect Dossier",
                            tint = HudPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PRIMARY IDENTITY: FULL NAME AND FATHER NAME (STRICTLY RIGHT-ALIGNED)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF06111C), RoundedCornerShape(2.dp))
                    .border(1.dp, HudBorder.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (record.fullName.isNotBlank()) record.fullName else "ANONYMOUS RECORD",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudTextPrimary,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = HudPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (record.fatherName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "نام پدر / Father: ${record.fatherName}",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HudTextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bank Issuer Header Tag if Card is present
            if (record.cardNo.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF051722), RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = HudAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = record.bankIssuerName,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HudAmber
                        )
                    }
                    Text(
                        text = "SHETAB 16-BIT",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 9.sp,
                        color = HudTextMuted
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Key Telemetry Grid (National Code & Mobile)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // National Code Box
                TelemetryCell(
                    label = "NAT. CODE",
                    value = record.nationalCode.ifEmpty { "---" },
                    modifier = Modifier.weight(1f),
                    accentColor = HudPrimary,
                    onCopy = {
                        if (record.nationalCode.isNotEmpty()) {
                            onCopyField("National Code", record.nationalCode)
                        }
                    }
                )

                // Mobile Box
                TelemetryCell(
                    label = "MOBILE",
                    value = record.formattedMobile.ifEmpty { "---" },
                    modifier = Modifier.weight(1f),
                    accentColor = HudSecondary,
                    onCopy = {
                        if (record.mobile.isNotEmpty()) {
                            onCopyField("Mobile", record.mobile)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Financial Telemetry Row: Card No & Account
            if (record.cardNo.isNotEmpty() || record.accountNumber.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (record.cardNo.isNotEmpty()) {
                        TelemetryCell(
                            label = "CARD NO",
                            value = record.formattedCardNumber,
                            modifier = Modifier.weight(1.3f),
                            accentColor = HudAmber,
                            onCopy = { onCopyField("Card No", record.cardNo) }
                        )
                    }

                    if (record.accountNumber.isNotEmpty()) {
                        TelemetryCell(
                            label = "ACCOUNT",
                            value = record.accountNumber,
                            modifier = Modifier.weight(1f),
                            accentColor = HudTextSecondary,
                            onCopy = { onCopyField("Account", record.accountNumber) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Location Bar: City & Province
            if (record.cityName.isNotEmpty() || record.provinceName.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF07111A))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = HudTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            if (record.cityName.isNotEmpty()) append(record.cityName)
                            if (record.cityName.isNotEmpty() && record.provinceName.isNotEmpty()) append(", ")
                            if (record.provinceName.isNotEmpty()) append(record.provinceName)
                        }.uppercase(),
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 11.sp,
                        color = HudTextSecondary
                    )

                    if (record.birthDate.isNotEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "DOB: ${record.birthDate}",
                            fontFamily = ShareTechMonoFontFamily,
                            fontSize = 11.sp,
                            color = HudTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryCell(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit
) {
    Row(
        modifier = modifier
            .background(Color(0xFF060F17), RoundedCornerShape(2.dp))
            .border(1.dp, HudBorder, RoundedCornerShape(2.dp))
            .clickable { onCopy() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = OrbitronFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = HudTextMuted
            )
            Text(
                text = value,
                fontFamily = ShareTechMonoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy $label",
            tint = HudTextMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
    }
}
