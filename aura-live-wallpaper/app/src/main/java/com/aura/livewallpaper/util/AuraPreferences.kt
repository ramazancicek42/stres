package com.aura.livewallpaper.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Kullanıcı ayarlarını yöneten Preferences wrapper sınıfı
 * 
 * Yeni Alanlar:
 * - autoPaletteEnabled: Zaman bazlı otomatik palet değişimi
 * - targetFps: Hedef FPS değeri
 * - audioEnabled: Ses motoru açık/kapalı
 * - hapticEnabled: Dokunsal geri bildirim
 * - accessibilityMode: Erişilebilirlik modu (epilepsi güvenliği)
 */
class AuraPreferences(context: Context) {
    
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    var lightSensitivity: Float
        get() = prefs.getFloat(KEY_LIGHT_SENSITIVITY, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_LIGHT_SENSITIVITY, value).apply()
    
    var audioSensitivity: Float
        get() = prefs.getFloat(KEY_AUDIO_SENSITIVITY, 0.5f)
        set(value) = prefs.edit().putFloat(KEY_AUDIO_SENSITIVITY, value).apply()
    
    var silentMode: Boolean
        get() = prefs.getBoolean(KEY_SILENT_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_SILENT_MODE, value).apply()
    
    var powerSaverMode: Boolean
        get() = prefs.getBoolean(KEY_POWER_SAVER, false)
        set(value) = prefs.edit().putBoolean(KEY_POWER_SAVER, value).apply()
    
    var colorPaletteIndex: Int
        get() = prefs.getInt(KEY_COLOR_PALETTE, 0)
        set(value) = prefs.edit().putInt(KEY_COLOR_PALETTE, value).apply()
    
    var fpsLimit: Int
        get() = prefs.getInt(KEY_FPS_LIMIT, 30)
        set(value) = prefs.edit().putInt(KEY_FPS_LIMIT, value).apply()
    
    var autoPaletteEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PALETTE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PALETTE, value).apply()
    
    var targetFps: Int
        get() = prefs.getInt(KEY_TARGET_FPS, 30)
        set(value) = prefs.edit().putInt(KEY_TARGET_FPS, value).apply()
    
    var audioEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUDIO_ENABLED, value).apply()
    
    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, value).apply()
    
    var accessibilityMode: Boolean
        get() = prefs.getBoolean(KEY_ACCESSIBILITY_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_ACCESSIBILITY_MODE, value).apply()
    
    var showTimeOfDay: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TIME_OF_DAY, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TIME_OF_DAY, value).apply()
    
    var selectedPalette: String
        get() = prefs.getString(KEY_SELECTED_PALETTE, "Cosmic") ?: "Cosmic"
        set(value) = prefs.edit().putString(KEY_SELECTED_PALETTE, value).apply()
    
    val autoPalette: Boolean
        get() = autoPaletteEnabled
    
    companion object {
        private const val KEY_LIGHT_SENSITIVITY = "light_sensitivity"
        private const val KEY_AUDIO_SENSITIVITY = "audio_sensitivity"
        private const val KEY_SILENT_MODE = "silent_mode"
        private const val KEY_POWER_SAVER = "power_saver"
        private const val KEY_COLOR_PALETTE = "color_palette"
        private const val KEY_FPS_LIMIT = "fps_limit"
        private const val KEY_AUTO_PALETTE = "auto_palette"
        private const val KEY_TARGET_FPS = "target_fps"
        private const val KEY_AUDIO_ENABLED = "audio_enabled"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_ACCESSIBILITY_MODE = "accessibility_mode"
        private const val KEY_SHOW_TIME_OF_DAY = "show_time_of_day"
        private const val KEY_SELECTED_PALETTE = "selected_palette"
    }
}
