package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HUD Vault", appName)
  }

  @Test
  fun `verify security preferences hashing and biometric state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.security.SecurityPreferences(context)
    prefs.resetSecurity()

    org.junit.Assert.assertFalse(prefs.isSetupCompleted)
    prefs.saveMasterPassword("Vault@2026", enableBiometrics = true)

    org.junit.Assert.assertTrue(prefs.isSetupCompleted)
    org.junit.Assert.assertTrue(prefs.isBiometricEnabled)
    org.junit.Assert.assertTrue(prefs.verifyPassword("Vault@2026"))
    org.junit.Assert.assertFalse(prefs.verifyPassword("WrongPassword"))
  }
}
