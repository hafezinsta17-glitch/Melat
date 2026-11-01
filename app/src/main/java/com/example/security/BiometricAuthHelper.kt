package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricHardwareStatus(val displayMessage: String) {
    AVAILABLE("BIOMETRIC SENSOR READY & ENROLLED"),
    NONE_ENROLLED("NO BIOMETRICS ENROLLED IN SYSTEM SETTINGS"),
    NO_HARDWARE("DEVICE LACKS BIOMETRIC SENSOR HARDWARE"),
    UNAVAILABLE("BIOMETRIC SUBSYSTEM CURRENTLY UNAVAILABLE")
}

object BiometricAuthHelper {

    fun checkHardwareStatus(context: Context): BiometricHardwareStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricHardwareStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricHardwareStatus.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricHardwareStatus.NO_HARDWARE
            else -> BiometricHardwareStatus.UNAVAILABLE
        }
    }

    fun launchRealBiometricPrompt(
        activity: FragmentActivity,
        title: String = "HUD BIOMETRIC AUTHENTICATION",
        subtitle: String = "Scan citizen fingerprint to unlock financial vault",
        negativeButtonText: String = "Use Master Passcode",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onNegativeClick: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    onNegativeClick()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric mismatch. Please retry or use master passcode.")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
