package com.aura.livewallpaper.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aura.livewallpaper.R
import com.aura.livewallpaper.ui.SettingsActivity

/**
 * Aura Widget Provider - Ana ekranda hızlı kontrol widget'ı
 * 
 * Özellikler:
 * - Palet değiştirme
 * - Ses açma/kapama
 * - Ayarlar ekranına kısayol
 * - Pil durumu gösterimi
 */
class AuraWidgetProvider : AppWidgetProvider() {
    
    companion object {
        const val ACTION_CHANGE_PALETTE = "com.aura.ACTION_CHANGE_PALETTE"
        const val ACTION_TOGGLE_AUDIO = "com.aura.ACTION_TOGGLE_AUDIO"
        const val ACTION_OPEN_SETTINGS = "com.aura.ACTION_OPEN_SETTINGS"
        
        const val EXTRA_PALETTE = "extra_palette"
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_CHANGE_PALETTE -> {
                val paletteName = intent.getStringExtra(EXTRA_PALETTE) ?: return
                // Palet değiştirme işlemi
                // Broadcast veya Service aracılığıyla wallpaper'e ilet
            }
            ACTION_TOGGLE_AUDIO -> {
                // Ses toggle işlemi
            }
            ACTION_OPEN_SETTINGS -> {
                // Ayarlar ekranını aç
                val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(settingsIntent)
            }
        }
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.aura_widget)
        
        // Butonlara click listener'ları ekle
        setupPaletteButtons(context, views)
        setupAudioButton(context, views)
        setupSettingsButton(context, views)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    private fun setupPaletteButtons(context: Context, views: RemoteViews) {
        // Sunrise paleti butonu
        views.setOnClickPendingIntent(
            R.id.btn_sunrise,
            createPaletteChangeIntent(context, "Sunrise")
        )
        
        // Ocean paleti butonu
        views.setOnClickPendingIntent(
            R.id.btn_ocean,
            createPaletteChangeIntent(context, "Ocean")
        )
        
        // Sunset paleti butonu
        views.setOnClickPendingIntent(
            R.id.btn_sunset,
            createPaletteChangeIntent(context, "Sunset")
        )
        
        // Cosmic paleti butonu
        views.setOnClickPendingIntent(
            R.id.btn_cosmic,
            createPaletteChangeIntent(context, "Cosmic")
        )
    }
    
    private fun setupAudioButton(context: Context, views: RemoteViews) {
        views.setOnClickPendingIntent(
            R.id.btn_audio,
            createToggleAudioIntent(context)
        )
    }
    
    private fun setupSettingsButton(context: Context, views: RemoteViews) {
        views.setOnClickPendingIntent(
            R.id.btn_settings,
            createOpenSettingsIntent(context)
        )
    }
    
    private fun createPaletteChangeIntent(context: Context, palette: String): PendingIntent {
        val intent = Intent(context, AuraWidgetProvider::class.java).apply {
            action = ACTION_CHANGE_PALETTE
            putExtra(EXTRA_PALETTE, palette)
        }
        return PendingIntent.getBroadcast(context, palette.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    
    private fun createToggleAudioIntent(context: Context): PendingIntent {
        val intent = Intent(context, AuraWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_AUDIO
        }
        return PendingIntent.getBroadcast(context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    
    private fun createOpenSettingsIntent(context: Context): PendingIntent {
        val intent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, 200, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
