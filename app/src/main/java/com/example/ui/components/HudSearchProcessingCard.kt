package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SearchFilter
import com.example.ui.theme.HudAlert
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudPrimary
import com.example.ui.theme.HudSecondary
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.HudTextMuted
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.ShareTechMonoFontFamily
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tactical Search Processing HUD Banner.
 * Displays live progress, elapsed ms timer, B-Tree algorithm info, and instant abort button.
 */
@Composable
fun HudSearchProcessingBanner(
    isSearching: Boolean,
    query: String,
    filter: SearchFilter,
    phase: String,
    elapsedMs: Long,
    resultCount: Int,
    algorithm: String,
    matchedField: String,
    onAbortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isSearching && query.isEmpty()) return

    HudFrameBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .testTag("hud_search_processing_banner"),
        borderColor = if (isSearching) HudAmber.copy(alpha = 0.8f) else HudBorder,
        glowColor = if (isSearching) HudAmber else HudPrimary,
        cornerLength = 6.dp,
        strokeWidth = 1.dp,
        backgroundColor = HudSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(14.dp)
                                .testTag("search_running_spinner"),
                            color = HudAmber,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(HudSecondary, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isSearching) {
                            "SEARCHING 100M ARCHIVE • $phase"
                        } else {
                            "RESOLVED: $resultCount MATCHES FOUND"
                        },
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSearching) HudAmber else HudSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Live Millisecond Elapsed Timer
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (isSearching) HudAmber.copy(alpha = 0.15f) else HudPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSearching) HudAmber.copy(alpha = 0.5f) else HudPrimary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (isSearching) HudAmber else HudPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${elapsedMs}ms",
                            fontFamily = ShareTechMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSearching) HudAmber else HudPrimary
                        )
                    }

                    // Abort Button when search is running
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .background(HudAlert.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                .border(1.dp, HudAlert.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                .clickable(
                                    role = Role.Button,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = HudAlert),
                                    onClick = onAbortClick
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("abort_search_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Abort Search",
                                    tint = HudAlert,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "ABORT",
                                    fontFamily = OrbitronFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudAlert
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Algorithm & Index Telemetry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = HudTextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ALGO: $algorithm",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 9.sp,
                        color = HudTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "INDEX: [$matchedField]",
                    fontFamily = ShareTechMonoFontFamily,
                    fontSize = 9.sp,
                    color = HudPrimary
                )
            }
        }
    }
}

/**
 * Sci-Fi Radar Scanning View shown when an active query is executing
 * across 100,000,000 records. Assures the user the engine is actively processing.
 */
@Composable
fun HudSearchArchiveScanningView(
    query: String,
    filter: SearchFilter,
    elapsedMs: Long,
    phase: String,
    algorithm: String,
    onAbortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "radar_sweep")
    val angleDegrees by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle"
    )

    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("hud_search_archive_scanning_view"),
        contentAlignment = Alignment.Center
    ) {
        HudFrameBox(
            modifier = Modifier.fillMaxWidth(0.96f),
            borderColor = HudAmber.copy(alpha = 0.7f),
            glowColor = HudAmber,
            cornerLength = 14.dp,
            backgroundColor = HudSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Tactical Radar Reticle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .drawBehind {
                            val center = Offset(size.width / 2, size.height / 2)
                            val radius = size.width / 2

                            // Concentric rings
                            drawCircle(
                                color = HudAmber.copy(alpha = 0.2f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 1.5f)
                            )
                            drawCircle(
                                color = HudAmber.copy(alpha = 0.3f),
                                radius = radius * 0.66f,
                                center = center,
                                style = Stroke(width = 1f)
                            )
                            drawCircle(
                                color = HudAmber.copy(alpha = 0.4f),
                                radius = radius * 0.33f,
                                center = center,
                                style = Stroke(width = 1f)
                            )

                            // Crosshair axes
                            drawLine(
                                color = HudAmber.copy(alpha = 0.25f),
                                start = Offset(0f, center.y),
                                end = Offset(size.width, center.y),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = HudAmber.copy(alpha = 0.25f),
                                start = Offset(center.x, 0f),
                                end = Offset(center.x, size.height),
                                strokeWidth = 1f
                            )

                            // Rotating radar sweep line
                            val rad = Math.toRadians(angleDegrees.toDouble())
                            val sweepEnd = Offset(
                                x = (center.x + radius * cos(rad)).toFloat(),
                                y = (center.y + radius * sin(rad)).toFloat()
                            )
                            drawLine(
                                color = HudAmber.copy(alpha = pulseAlpha),
                                start = center,
                                end = sweepEnd,
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Scanning",
                        tint = HudAmber.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "TRAVERSING 100M RECORD ARCHIVE",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HudAmber,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "TARGET: '$query' • FILTER: ${filter.label}",
                    fontFamily = ShareTechMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HudTextPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = phase,
                    fontFamily = ShareTechMonoFontFamily,
                    fontSize = 10.sp,
                    color = HudTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Active Stopwatch & Telemetry
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudBadge(
                        text = "ELAPSED: ${elapsedMs}ms",
                        color = HudAmber,
                        indicatorOn = true
                    )
                    HudBadge(
                        text = "ALGO: $algorithm",
                        color = HudPrimary,
                        indicatorOn = false
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "B-Tree log2(N) Range Traversal • Zero Table Scans • SQLite WAL Active",
                    fontFamily = ShareTechMonoFontFamily,
                    fontSize = 9.sp,
                    color = HudTextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Abort Button
                HudButton(
                    onClick = onAbortClick,
                    text = "ABORT SEARCH",
                    accentColor = HudAlert,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(36.dp)
                        .testTag("abort_scanning_view_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = HudAlert,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}
