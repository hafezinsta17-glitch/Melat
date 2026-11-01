package com.example.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

class SecurityPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "hud_security_vault_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_SETUP_COMPLETED = "key_setup_completed"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_PASSWORD_HASH = "key_password_hash"
        private const val KEY_PASSWORD_SALT = "key_password_salt"
    }

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETED, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashWithSalt(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$salt$password".toByteArray(Charsets.UTF_8)
        val hashBytes = digest.digest(combined)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun saveMasterPassword(password: String, enableBiometrics: Boolean) {
        val salt = generateSalt()
        val hash = hashWithSalt(password, salt)
        prefs.edit()
            .putString(KEY_PASSWORD_SALT, salt)
            .putString(KEY_PASSWORD_HASH, hash)
            .putBoolean(KEY_BIOMETRIC_ENABLED, enableBiometrics)
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .apply()
    }

    fun verifyPassword(input: String): Boolean {
        val salt = prefs.getString(KEY_PASSWORD_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val inputHash = hashWithSalt(input, salt)
        return inputHash == storedHash
    }

    fun resetSecurity() {
        prefs.edit().clear().apply()
    }
}
