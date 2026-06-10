package com.example.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

data class DialerSettings(
    val blurStrength: Float = 40f,          // 0f - 100f
    val glassIntensity: Float = 0.35f,      // 0.1f - 0.9f (Opacity)
    val cornerRadius: Int = 24,            // 0dp - 48dp
    val accentColorHex: String = "#6366F1", // Indigo accent color
    val backgroundGradientIndex: Int = 0,   // 0 = High Density Slate, 1 = Sky Glass, 2 = Velvet Purple, 3 = AMOLED Cosmic
    val navigationStyle: String = "Glass Floating", // Floating, Standard, Minimal
    val dialPadStyle: String = "Circular Glass",    // Circular Glass, Squircle Glass, Borderless
    val incomingCallStyle: String = "Full Screen Blurred Glow",
    val contactCardStyle: String = "Large Glass Grid",
    val animationSpeedMultiplier: Float = 1.0f, // 0.5f, 1.0f, 1.5f
    val themeMode: String = "DARK" // LIGHT, DARK, AMOLED
) {
    fun getAccentColor(): Color = try {
        Color(android.graphics.Color.parseColor(accentColorHex))
    } catch (e: Exception) {
        Color(0xFF0288D1)
    }
}

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dialer_settings_prefs", Context.MODE_PRIVATE)

    fun getSettings(): DialerSettings {
        return DialerSettings(
            blurStrength = prefs.getFloat("blurStrength", 40f),
            glassIntensity = prefs.getFloat("glassIntensity", 0.35f),
            cornerRadius = prefs.getInt("cornerRadius", 24),
            accentColorHex = prefs.getString("accentColorHex", "#6366F1") ?: "#6366F1",
            backgroundGradientIndex = prefs.getInt("backgroundGradientIndex", 0),
            navigationStyle = prefs.getString("navigationStyle", "Glass Floating") ?: "Glass Floating",
            dialPadStyle = prefs.getString("dialPadStyle", "Circular Glass") ?: "Circular Glass",
            incomingCallStyle = prefs.getString("incomingCallStyle", "Full Screen Blurred Glow") ?: "Full Screen Blurred Glow",
            contactCardStyle = prefs.getString("contactCardStyle", "Large Glass Grid") ?: "Large Glass Grid",
            animationSpeedMultiplier = prefs.getFloat("animationSpeedMultiplier", 1.0f),
            themeMode = prefs.getString("themeMode", "DARK") ?: "DARK"
        )
    }

    fun saveSettings(settings: DialerSettings) {
        prefs.edit().apply {
            putFloat("blurStrength", settings.blurStrength)
            putFloat("glassIntensity", settings.glassIntensity)
            putInt("cornerRadius", settings.cornerRadius)
            putString("accentColorHex", settings.accentColorHex)
            putInt("backgroundGradientIndex", settings.backgroundGradientIndex)
            putString("navigationStyle", settings.navigationStyle)
            putString("dialPadStyle", settings.dialPadStyle)
            putString("incomingCallStyle", settings.incomingCallStyle)
            putString("contactCardStyle", settings.contactCardStyle)
            putFloat("animationSpeedMultiplier", settings.animationSpeedMultiplier)
            putString("themeMode", settings.themeMode)
            apply()
        }
    }
}
