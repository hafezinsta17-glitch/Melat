package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricHardwareStatus
import com.example.ui.components.HudBadge
import com.example.ui.components.HudButton
import com.example.ui.components.HudFrameBox
import com.example.ui.components.HudScanningBeam
import com.example.ui.theme.HudAlert
import com.example.ui.theme.HudAmber
import com.example.ui.theme.HudBackground
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudBorderGlow
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
fun HudSetupWizardScreen(
    onSetupCompleted: (password: String, enableBiometrics: Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var currentStep by remember { mutableIntStateOf(1) } // 1: Password, 2: Biometrics, 3: Confirmation

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var enableBiometrics by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var biometricTestedSuccess by remember { mutableStateOf(false) }

    val hardwareStatus = remember { BiometricAuthHelper.checkHardwareStatus(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBackground)
            .statusBarsPadding()
            .testTag("setup_wizard_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            HudFrameBox(
                modifier = Modifier.fillMaxWidth(),
                borderColor = HudPrimary,
                glowColor = HudPrimary,
                cornerLength = 12.dp,
                backgroundColor = HudSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = HudPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURITY SETUP WIZARD",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HudPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "INITIALIZE MASTER PASSCODE & BIOMETRIC GATE",
                        fontFamily = ShareTechMonoFontFamily,
                        fontSize = 11.sp,
                        color = HudTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step Indicator Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HudBadge(
                            text = "STEP 1: PASSCODE",
                            color = if (currentStep == 1) HudPrimary else HudTextMuted,
                            indicatorOn = currentStep == 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        HudBadge(
                            text = "STEP 2: BIOMETRICS",
                            color = if (currentStep == 2) HudSecondary else HudTextMuted,
                            indicatorOn = currentStep == 2
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        HudBadge(
                            text = "STEP 3: DEPLOY",
                            color = if (currentStep == 3) HudAmber else HudTextMuted,
                            indicatorOn = currentStep == 3
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HudScanningBeam(color = HudPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            when (currentStep) {
                1 -> {
                    // STEP 1: SET MASTER PASSCODE
                    HudFrameBox(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = HudBorder,
                        glowColor = HudBorderGlow,
                        cornerLength = 10.dp,
                        backgroundColor = HudSurfaceElevated
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "1. CREATE MASTER PASSCODE",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "The master passcode encrypts your offline financial store and provides fallback when biometrics are unavailable.",
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 11.sp,
                                color = HudTextSecondary,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Password Input
                            Text(
                                text = "MASTER PASSCODE (MIN 4 CHARACTERS)",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 10.sp,
                                color = HudTextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            PasscodeField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMessage = null
                                },
                                isVisible = passwordVisible,
                                onToggleVisibility = { passwordVisible = !passwordVisible },
                                placeholder = "Enter master passcode...",
                                testTag = "setup_passcode_input"
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Confirm Password Input
                            Text(
                                text = "CONFIRM MASTER PASSCODE",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 10.sp,
                                color = HudTextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            PasscodeField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    errorMessage = null
                                },
                                isVisible = passwordVisible,
                                onToggleVisibility = { passwordVisible = !passwordVisible },
                                placeholder = "Re-enter passcode...",
                                testTag = "setup_confirm_passcode_input"
                            )

                            // Error banner
                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "ALERT: $errorMessage",
                                    fontFamily = ShareTechMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudAlert
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            HudButton(
                                onClick = {
                                    if (password.length < 4) {
                                        errorMessage = "Passcode must be at least 4 characters."
                                    } else if (password != confirmPassword) {
                                        errorMessage = "Passcodes do not match."
                                    } else {
                                        errorMessage = null
                                        currentStep = 2
                                    }
                                },
                                text = "CONTINUE TO BIOMETRICS",
                                accentColor = HudPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_continue_step2_button")
                            )
                        }
                    }
                }

                2 -> {
                    // STEP 2: CONFIGURE HARDWARE BIOMETRICS
                    HudFrameBox(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = HudSecondary,
                        glowColor = HudSecondary,
                        cornerLength = 10.dp,
                        backgroundColor = HudSurfaceElevated
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "2. HARDWARE BIOMETRIC ENCLAVE",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enable real Android hardware fingerprint or biometric authentication for instant zero-latency vault access.",
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 11.sp,
                                color = HudTextSecondary,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Hardware status readout
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF04121A))
                                    .border(1.dp, HudBorder)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "DEVICE BIOMETRIC SUBSYSTEM",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 9.sp,
                                        color = HudTextMuted
                                    )
                                    Text(
                                        text = hardwareStatus.displayMessage,
                                        fontFamily = ShareTechMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hardwareStatus == BiometricHardwareStatus.AVAILABLE) HudSecondary else HudAmber
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = if (hardwareStatus == BiometricHardwareStatus.AVAILABLE) HudSecondary else HudAmber,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Enable toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF061019))
                                    .border(1.dp, HudBorder)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "USE BIOMETRIC LOCK",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HudTextPrimary
                                    )
                                    Text(
                                        text = "Prompt fingerprint sensor when opening app",
                                        fontFamily = ShareTechMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = HudTextMuted
                                    )
                                }

                                Switch(
                                    checked = enableBiometrics,
                                    onCheckedChange = { enableBiometrics = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = HudSecondary,
                                        checkedTrackColor = HudSecondary.copy(alpha = 0.3f),
                                        uncheckedThumbColor = HudTextMuted,
                                        uncheckedTrackColor = HudBorder
                                    ),
                                    modifier = Modifier.testTag("biometric_enable_switch")
                                )
                            }

                            // Test Real Biometric Prompt Button
                            if (enableBiometrics && activity != null && hardwareStatus == BiometricHardwareStatus.AVAILABLE) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HudButton(
                                    onClick = {
                                        BiometricAuthHelper.launchRealBiometricPrompt(
                                            activity = activity,
                                            title = "TEST BIOMETRIC ENCLAVE",
                                            subtitle = "Touch sensor to verify hardware integration",
                                            onSuccess = { biometricTestedSuccess = true },
                                            onError = { errorMessage = it },
                                            onNegativeClick = {}
                                        )
                                    },
                                    text = if (biometricTestedSuccess) "BIOMETRICS VERIFIED ✓" else "TEST HARDWARE SENSOR",
                                    accentColor = if (biometricTestedSuccess) HudSecondary else HudPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HudButton(
                                    onClick = { currentStep = 1 },
                                    text = "BACK",
                                    accentColor = HudTextMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                HudButton(
                                    onClick = { currentStep = 3 },
                                    text = "NEXT",
                                    accentColor = HudSecondary,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                    }
                }

                3 -> {
                    // STEP 3: DEPLOY & SECURE VAULT
                    HudFrameBox(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = HudSecondary,
                        glowColor = HudSecondary,
                        cornerLength = 12.dp,
                        backgroundColor = HudSurfaceElevated
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = HudSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "VAULT READY FOR DEPLOYMENT",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All security parameters successfully verified. Your 13-column financial database will be protected by master passcode and hardware biometric enclaves.",
                                fontFamily = ShareTechMonoFontFamily,
                                fontSize = 11.sp,
                                color = HudTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Parameters summary table
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF041018))
                                    .border(1.dp, HudBorder)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SummaryRow("MASTER PASSCODE", "CONFIGURED & SALTED (SHA-256)", HudPrimary)
                                SummaryRow("HARDWARE BIOMETRICS", if (enableBiometrics) "ENABLED & ACTIVE" else "DISABLED (PASSCODE ONLY)", if (enableBiometrics) HudSecondary else HudAmber)
                                SummaryRow("OFFLINE STORAGE", "ROOM B-TREE DATABASE", HudSecondary)
                                SummaryRow("FINANCIAL PARSER", "13 COLUMNS RFC-4180 STREAM", HudTextPrimary)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            HudButton(
                                onClick = {
                                    onSetupCompleted(password, enableBiometrics)
                                },
                                text = "ENTER SECURE VAULT",
                                accentColor = HudSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("finalize_setup_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasscodeField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    placeholder: String,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050E17), RoundedCornerShape(2.dp))
            .border(1.dp, HudBorder, RoundedCornerShape(2.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp)
                .testTag(testTag),
            singleLine = true,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = TextStyle(
                color = HudTextPrimary,
                fontSize = 15.sp,
                fontFamily = ShareTechMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            cursorBrush = SolidColor(HudPrimary),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = HudTextMuted,
                        fontSize = 12.sp,
                        fontFamily = ShareTechMonoFontFamily
                    )
                }
                innerTextField()
            }
        )

        IconButton(onClick = onToggleVisibility, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Toggle visibility",
                tint = HudTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = ShareTechMonoFontFamily,
            fontSize = 10.sp,
            color = HudTextMuted
        )
        Text(
            text = value,
            fontFamily = ShareTechMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
