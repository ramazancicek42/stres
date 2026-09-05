package com.aura.livewallpaper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.aura.livewallpaper.widget.AuraWidgetProvider

/**
 * Widget'tan gelen komutları dinleyen ve Wallpaper servisine ileten BroadcastReceiver.
 */
class AuraCommandReceiver : BroadcastReceiver() {
    
    companion object {
        const val ACTION_TOGGLE_FREEZE = "com.aura.livewallpaper.ACTION_TOGGLE_FREEZE"
        const val ACTION_NEXT_PALETTE = "com.aura.livewallpaper.ACTION_NEXT_PALETTE"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_FREEZE, AuraWidgetProvider.ACTION_FREEZE -> {
                toggleFreeze(context)
            }
            ACTION_NEXT_PALETTE, AuraWidgetProvider.ACTION_NEXT_PALETTE -> {
                nextPalette(context)
            }
        }
    }
    
    private fun toggleFreeze(context: Context) {
        val prefs = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
        val current = prefs.getBoolean("frozen", false)
        prefs.edit().putBoolean("frozen", !current).apply()
        
        // Servise haber ver (eğer çalışıyorsa)
        notifyService(context, "toggle_freeze")
    }
    
    private fun nextPalette(context: Context) {
        val prefs = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
        val current = prefs.getInt("palette_index", 0)
        prefs.edit().putInt("palette_index", (current + 1) % 8).apply()
        
        notifyService(context, "next_palette")
    }
    
    private fun notifyService(context: Context, command: String) {
        // Wallpaper servisine değişiklik olduğunu bildir
        // Not: Live Wallpaper servisine doğrudan intent gönderilemez,
        // bu yüzden SharedPreferences değişikliğini servis periyodik olarak kontrol eder
        // veya Engine.onSurfaceChanged gibi lifecycle metodlarında okur.
        Intent(context, AuraWallpaperService::class.java).apply {
            putExtra("command", command)
        }.also {
            // Servis çalışıyorsa broadcast olarak gönder
            try {
                context.sendBroadcast(it)
            } catch (e: Exception) {
                // Servis çalışmıyor olabilir, prefs zaten güncellendi
            }
        }
    }
}
