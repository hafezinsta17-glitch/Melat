package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricHardwareStatus
import com.example.security.SecurityPreferences
import com.example.ui.theme.HudAlert
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudBackground
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudBorderGlow
import com.example.ui.theme.HudGridLine
import com.example.ui.theme.HudPrimary
import com.example.ui.theme.HudSecondary
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.HudTextMuted
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.ShareTechMonoFontFamily

@Composable
fun HudBiomedicalGate(
    isLocked: Boolean,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isLocked) return

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val securityPrefs = remember { SecurityPreferences(context) }
    val hardwareStatus = remember { BiometricAuthHelper.checkHardwareStatus(context) }

    var statusMessage by remember { mutableStateOf("SCAN CITIZEN FINGERPRINT OR ENTER PASSCODE") }
    var isSuccess by remember { mutableStateOf(false) }
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var enteredPasscode by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf<String?>(null) }
    var passcodeVisible by remember { mutableStateOf(false) }

    // Holographic radar rotation
    val infiniteTransition = rememberInfiniteTransition(label = "radar_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Laser beam vertical movement
    val beamOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam"
    )

    fun triggerVibration(isError: Boolean = false) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                val pattern = if (isError) longArrayOf(0, 50, 50, 50) else longArrayOf(0, 60, 40, 100)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {}
    }

    // Launch Real AndroidX BiometricPrompt
    fun triggerRealBiometricPrompt() {
        if (activity == null) {
            showPasscodeDialog = true
            return
        }

        if (hardwareStatus != BiometricHardwareStatus.AVAILABLE) {
            statusMessage = hardwareStatus.displayMessage
            showPasscodeDialog = true
            return
        }

        BiometricAuthHelper.launchRealBiometricPrompt(
            activity = activity,
            title = "HUD BIOMETRIC AUTHENTICATION",
            subtitle = "Touch fingerprint sensor to unlock financial database",
            negativeButtonText = "Use Master Passcode",
            onSuccess = {
                isSuccess = true
                statusMessage = "HARDWARE BIOMETRICS VERIFIED • ACCESS GRANTED"
                triggerVibration(isError = false)
                onUnlocked()
            },
            onError = { err ->
                statusMessage = "ALERT: $err"
                triggerVibration(isError = true)
            },
            onNegativeClick = {
                showPasscodeDialog = true
            }
        )
    }

    // Automatically prompt biometric hardware once on entry if enabled and available
    LaunchedEffect(Unit) {
        if (securityPrefs.isBiometricEnabled && hardwareStatus == BiometricHardwareStatus.AVAILABLE) {
            triggerRealBiometricPrompt()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HudBackground)
            .drawBehind {
                val step = 40.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(HudGridLine, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(HudGridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }
            .testTag("biomedical_gate_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Tactical Top Branding
            HudFrameBox(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isSuccess) HudSecondary else HudPrimary,
                glowColor = if (isSuccess) HudSecondary else HudPrimary,
                cornerLength = 12.dp,
                backgroundColor = HudSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isSuccess) HudSecondary else HudPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REAL BIOMETRIC SECURITY GATE",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) HudSecondary else HudPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "HARDWARE ANDROIDX BIOMETRIC ENCLAVE • ZERO TRUST",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 10.sp,
                        color = HudTextSecondary,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Central Biometric Scanner HUD Circle
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF030D16))
                    .border(2.dp, if (isSuccess) HudSecondary else HudPrimary.copy(alpha = 0.7f), CircleShape)
                    .drawBehind {
                        // Outer Radar Reticle Ring
                        drawCircle(
                            color = if (isSuccess) HudSecondary else HudPrimary,
                            radius = size.minDimension / 2 - 8.dp.toPx(),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(15f, 15f),
                                    0f
                                )
                            )
                        )

                        // Inner concentric target circle
                        drawCircle(
                            color = HudBorder,
                            radius = size.minDimension / 3,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        triggerRealBiometricPrompt()
                    }
                    .testTag("biomedical_sensor_target"),
                contentAlignment = Alignment.Center
            ) {
                // Rotating Radar Sweep Line
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotation)
                        .drawBehind {
                            val r = size.minDimension / 2
                            drawLine(
                                brush = Brush.radialGradient(
                                    listOf(
                                        if (isSuccess) HudSecondary else HudPrimary,
                                        Color.Transparent
                                    )
                                ),
                                start = center,
                                end = Offset(center.x + r, center.y),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                )

                // Biometric Fingerprint Glyph
                Icon(
                    imageVector = if (isSuccess) Icons.Default.VerifiedUser else Icons.Default.Fingerprint,
                    contentDescription = "Real Biometric Sensor",
                    tint = if (isSuccess) HudSecondary else HudPrimary,
                    modifier = Modifier.size(105.dp)
                )

                // Laser Scanning Bar
                if (!isSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(3.dp)
                            .align(Alignment.Center)
                            .drawBehind {
                                val ySpan = 90.dp.toPx()
                                val currentY = (beamOffset - 0.5f) * ySpan
                                drawLine(
                                    color = HudPrimary,
                                    start = Offset(0f, size.height / 2 + currentY),
                                    end = Offset(size.width, size.height / 2 + currentY),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Telemetry Readout Box
            HudFrameBox(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isSuccess) HudSecondary else HudBorder,
                glowColor = if (isSuccess) HudSecondary else HudBorderGlow,
                cornerLength = 10.dp,
                backgroundColor = HudSurfaceElevated
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSuccess) "AUTHENTICATION SUCCESSFUL" else "AWAITING BIOMETRIC SCAN",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) HudSecondary else HudPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = statusMessage,
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 11.sp,
                        color = if (statusMessage.startsWith("ALERT")) HudAlert else HudTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bio Telemetry Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HudBadge(
                            text = if (hardwareStatus == BiometricHardwareStatus.AVAILABLE) "SENSOR: READY" else "SENSOR: N/A",
                            color = if (hardwareStatus == BiometricHardwareStatus.AVAILABLE) HudSecondary else HudAmber,
                            indicatorOn = hardwareStatus == BiometricHardwareStatus.AVAILABLE
                        )
                        HudBadge(
                            text = if (isSuccess) "VAULT: OPEN" else "VAULT: LOCKED",
                            color = if (isSuccess) HudSecondary else HudAmber,
                            indicatorOn = true
                        )
                        HudBadge(
                            text = "SHA-256",
                            color = HudTextSecondary,
                            indicatorOn = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HudButton(
                    onClick = { triggerRealBiometricPrompt() },
                    text = "SCAN BIOMETRIC",
                    accentColor = HudPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_biometric_prompt_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = HudPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                HudButton(
                    onClick = { showPasscodeDialog = true },
                    text = "ENTER PASSCODE",
                    accentColor = HudAmber,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_passcode_dialog_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = HudAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }

    // Master Passcode Fallback Dialog
    if (showPasscodeDialog) {
        Dialog(onDismissRequest = { showPasscodeDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("passcode_unlock_dialog"),
                color = HudBackground,
                shape = RoundedCornerShape(4.dp)
            ) {
                HudFrameBox(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = HudAmber,
                    glowColor = HudAmber,
                    cornerLength = 12.dp,
                    backgroundColor = HudSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = HudAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ENTER MASTER PASSCODE",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter your secret master passcode configured during initial setup.",
                            fontFamily = ShareTechMonoFontFamily,
                            fontSize = 11.sp,
                            color = HudTextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Input field
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF040E17), RoundedCornerShape(2.dp))
                                .border(1.dp, HudBorder, RoundedCornerShape(2.dp))
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = enteredPasscode,
                                onValueChange = {
                                    enteredPasscode = it
                                    passcodeError = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 10.dp)
                                    .testTag("enter_passcode_input"),
                                singleLine = true,
                                visualTransformation = if (passcodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                textStyle = TextStyle(
                                    color = HudTextPrimary,
                                    fontSize = 15.sp,
                                    fontFamily = ShareTechMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                cursorBrush = SolidColor(HudAmber),
                                decorationBox = { innerTextField ->
                                    if (enteredPasscode.isEmpty()) {
                                        Text(
                                            text = "Enter master passcode...",
                                            color = HudTextMuted,
                                            fontSize = 12.sp,
                                            fontFamily = ShareTechMonoFontFamily
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            IconButton(
                                onClick = { passcodeVisible = !passcodeVisible },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (passcodeVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = HudTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (passcodeError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ERROR: $passcodeError",
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudAlert
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HudButton(
                                onClick = { showPasscodeDialog = false },
                                text = "CANCEL",
                                accentColor = HudTextMuted,
                                modifier = Modifier.weight(1f)
                            )

                            HudButton(
                                onClick = {
                                    if (securityPrefs.verifyPassword(enteredPasscode)) {
                                        showPasscodeDialog = false
                                        triggerVibration(isError = false)
                                        onUnlocked()
                                    } else {
                                        passcodeError = "Invalid master passcode. Access denied."
                                        triggerVibration(isError = true)
                                    }
                                },
                                text = "UNLOCK",
                                accentColor = HudAmber,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("submit_passcode_unlock_button")
                            )
                        }
                    }
                }
            }
        }
    }
}
