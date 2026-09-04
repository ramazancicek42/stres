package com.aura.livewallpaper.quicksettings

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Context
import android.content.SharedPreferences

/**
 * Hızlı Ayarlar Paneli için AURA kontrol tile'ı.
 * Özellikler:
 * - Live Wallpaper'ı dondurma/çözme
 * - Aktif/pasif durum gösterimi
 */
class AuraTileService : TileService() {

    private lateinit var prefs: SharedPreferences

    companion object {
        private const val PREFS_NAME = "aura_prefs"
        private const val KEY_FROZEN = "frozen"
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        toggleFreeze()
        updateTileState()
    }

    private fun toggleFreeze() {
        val current = prefs.getBoolean(KEY_FROZEN, false)
        prefs.edit().putBoolean(KEY_FROZEN, !current).apply()
        
        // Kullanıcıya bildirim göster (opsiyonel)
        // NotificationManager ile toast benzeri bildirim
    }

    private fun updateTileState() {
        val isFrozen = prefs.getBoolean(KEY_FROZEN, false)
        
        qsTile?.apply {
            state = if (isFrozen) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "AURA Freeze"
            contentDescription = if (isFrozen) "Fraktal donduruldu" else "Fraktal aktif"
            updateTile()
        }
    }
}
