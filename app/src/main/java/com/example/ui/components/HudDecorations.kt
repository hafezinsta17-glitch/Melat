package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudBorderGlow
import com.example.ui.theme.HudGridLine
import com.example.ui.theme.HudPrimary
import com.example.ui.theme.HudSecondary
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudTextMuted
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.ShareTechMonoFontFamily

/**
 * Tactical HUD Frame Box with sci-fi corner brackets, fine border, and optional scanlines.
 */
@Composable
fun HudFrameBox(
    modifier: Modifier = Modifier,
    borderColor: Color = HudBorder,
    glowColor: Color = HudBorderGlow,
    cornerLength: Dp = 10.dp,
    strokeWidth: Dp = 1.5.dp,
    backgroundColor: Color = HudSurface,
    showScanlines: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .drawBehind {
                val cornerPx = cornerLength.toPx()
                val strokePx = strokeWidth.toPx()
                val w = size.width
                val h = size.height

                // Draw subtle outer border
                drawRect(
                    color = borderColor,
                    topLeft = Offset.Zero,
                    size = size,
                    style = Stroke(width = strokePx * 0.75f)
                )

                // Draw scanlines if requested
                if (showScanlines) {
                    var y = 0f
                    while (y < h) {
                        drawLine(
                            color = HudGridLine,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                        y += 6f
                    }
                }

                // Draw 4 Tactical HUD Corner Brackets with Glow Color
                val cornerPath = Path().apply {
                    // Top-Left
                    moveTo(0f, cornerPx)
                    lineTo(0f, 0f)
                    lineTo(cornerPx, 0f)

                    // Top-Right
                    moveTo(w - cornerPx, 0f)
                    lineTo(w, 0f)
                    lineTo(w, cornerPx)

                    // Bottom-Right
                    moveTo(w, h - cornerPx)
                    lineTo(w, h)
                    lineTo(w - cornerPx, h)

                    // Bottom-Left
                    moveTo(cornerPx, h)
                    lineTo(0f, h)
                    lineTo(0f, h - cornerPx)
                }

                drawPath(
                    path = cornerPath,
                    color = glowColor,
                    style = Stroke(width = strokePx * 1.5f)
                )
            },
        content = content
    )
}

/**
 * Tactical Telemetry Badge (e.g., [STATUS: ONLINE], [RECORDS: 1,240])
 */
@Composable
fun HudBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HudPrimary,
    indicatorOn: Boolean = true
) {
    Row(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (indicatorOn) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = ShareTechMonoFontFamily,
            letterSpacing = 0.8.sp
        )
    }
}

/**
 * Sci-Fi Tactical Button with corner ticks, neon glow, and press ripple
 */
@Composable
fun HudButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = HudPrimary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val effectiveColor = if (enabled) accentColor else HudTextMuted

    Box(
        modifier = modifier
            .height(48.dp)
            .background(effectiveColor.copy(alpha = 0.12f))
            .border(1.dp, effectiveColor.copy(alpha = 0.7f))
            .clickable(
                enabled = enabled && !isLoading,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(color = accentColor),
                onClick = onClick
            )
            .drawBehind {
                val cornerPx = 8.dp.toPx()
                val w = size.width
                val h = size.height

                // Draw corner accents
                drawLine(
                    color = effectiveColor,
                    start = Offset(0f, 0f),
                    end = Offset(cornerPx, 0f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = effectiveColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, cornerPx),
                    strokeWidth = 3f
                )
                drawLine(
                    color = effectiveColor,
                    start = Offset(w, h),
                    end = Offset(w - cornerPx, h),
                    strokeWidth = 3f
                )
                drawLine(
                    color = effectiveColor,
                    start = Offset(w, h),
                    end = Offset(w, h - cornerPx),
                    strokeWidth = 3f
                )
            }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isLoading) "PROCESSING..." else text.uppercase(),
                color = effectiveColor,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Animated Scanning Bar Effect for HUD aesthetic
 */
@Composable
fun HudScanningBeam(
    modifier: Modifier = Modifier,
    color: Color = HudPrimary
) {
    val transition = rememberInfiniteTransition(label = "scanning_beam")
    val sweepPosition by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beam_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .drawBehind {
                val x = size.width * sweepPosition
                drawLine(
                    color = color.copy(alpha = 0.9f),
                    start = Offset(x - 60f, size.height / 2),
                    end = Offset(x + 60f, size.height / 2),
                    strokeWidth = size.height * 2
                )
            }
    )
}
