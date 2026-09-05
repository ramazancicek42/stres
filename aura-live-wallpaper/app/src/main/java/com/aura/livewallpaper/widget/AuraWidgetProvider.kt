package com.aura.livewallpaper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aura.livewallpaper.R
import com.aura.livewallpaper.ui.SettingsActivity

/**
 * Ana ekrana eklenebilen mini kontrol widget'ı.
 * Özellikler:
 * - Tek tıkla ayarları açma
 * - Fraktalı dondurma/çözme
 * - Renk paletini değiştirme
 */
class AuraWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_FREEZE = "com.aura.livewallpaper.ACTION_FREEZE"
        const val ACTION_NEXT_PALETTE = "com.aura.livewallpaper.ACTION_NEXT_PALETTE"
        const val ACTION_OPEN_SETTINGS = "com.aura.livewallpaper.ACTION_OPEN_SETTINGS"
        
        fun updateWidgets(context: Context) {
            val intent = Intent(context, AuraWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(android.content.ComponentName(context, AuraWidgetProvider::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
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

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_aura_control)

        // Ayarları Aç Butonu
        views.setOnClickPendingIntent(
            R.id.btn_settings,
            android.app.PendingIntent.getActivity(
                context, 0,
                Intent(context, SettingsActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // Dondur/Çöz Butonu
        val freezeIntent = Intent(context, AuraWidgetProvider::class.java).apply {
            action = ACTION_FREEZE
        }
        views.setOnClickPendingIntent(
            R.id.btn_freeze,
            android.app.PendingIntent.getBroadcast(
                context, 1, freezeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // Sonraki Palet Butonu
        val paletteIntent = Intent(context, AuraWidgetProvider::class.java).apply {
            action = ACTION_NEXT_PALETTE
        }
        views.setOnClickPendingIntent(
            R.id.btn_palette,
            android.app.PendingIntent.getBroadcast(
                context, 2, paletteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_FREEZE -> {
                val prefs = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
                val current = prefs.getBoolean("frozen", false)
                prefs.edit().putBoolean("frozen", !current).apply()
                
                // AuraCommandReceiver'a bildir
                val commandIntent = Intent(context, com.aura.livewallpaper.service.AuraCommandReceiver::class.java).apply {
                    action = com.aura.livewallpaper.service.AuraCommandReceiver.ACTION_TOGGLE_FREEZE
                }
                context.sendBroadcast(commandIntent)
                
                updateWidgets(context)
            }
            ACTION_NEXT_PALETTE -> {
                val prefs = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
                val current = prefs.getInt("palette_index", 0)
                prefs.edit().putInt("palette_index", (current + 1) % 8).apply()
                
                // AuraCommandReceiver'a bildir
                val commandIntent = Intent(context, com.aura.livewallpaper.service.AuraCommandReceiver::class.java).apply {
                    action = com.aura.livewallpaper.service.AuraCommandReceiver.ACTION_NEXT_PALETTE
                }
                context.sendBroadcast(commandIntent)
                
                updateWidgets(context)
            }
        }
    }
}
