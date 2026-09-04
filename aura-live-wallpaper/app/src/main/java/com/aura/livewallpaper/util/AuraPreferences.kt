package com.aura.livewallpaper.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Kullanıcı ayarlarını yöneten Preferences wrapper sınıfı
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
    
    companion object {
        private const val KEY_LIGHT_SENSITIVITY = "light_sensitivity"
        private const val KEY_AUDIO_SENSITIVITY = "audio_sensitivity"
        private const val KEY_SILENT_MODE = "silent_mode"
        private const val KEY_POWER_SAVER = "power_saver"
        private const val KEY_COLOR_PALETTE = "color_palette"
        private const val KEY_FPS_LIMIT = "fps_limit"
    }
}
