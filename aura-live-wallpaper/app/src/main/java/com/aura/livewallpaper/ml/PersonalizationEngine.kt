package com.aura.livewallpaper.ml

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.max
import kotlin.math.min

/**
 * Yapay Zeka tabanlı kişiselleştirme motoru.
 * Kullanım alışkanlıklarını öğrenerek otomatik öneriler sunar.
 * 
 * Özellikler:
 * - En çok kullanılan paletleri tespit eder
 * - Günün saatine göre otomatik palet önerisi
 * - Ses/ışık hassasiyeti optimizasyonu
 * - Pil tasarrufu önerileri
 */
class PersonalizationEngine(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("aura_personalization", Context.MODE_PRIVATE)
    
    companion object {
        private const val MAX_HISTORY_SIZE = 100
        private const val KEY_PALETTE_HISTORY = "palette_history"
        private const val KEY_FPS_HISTORY = "fps_history"
        private const val KEY_TIME_PALETTE_MAP = "time_palette_map"
    }
    
    /**
     * Kullanılan renk paletlerinin geçmişini tutar.
     */
    private val paletteHistory: MutableList<Int> by lazy {
        val historyStr = prefs.getString(KEY_PALETTE_HISTORY, "") ?: ""
        historyStr.split(",").filter { it.isNotEmpty() }.map { it.toInt() }.toMutableList()
    }
    
    /**
     * Günün saatine göre (0-23) en çok kullanılan paletleri map'ler.
     */
    private val timePaletteMap: MutableMap<Int, MutableMap<Int, Int>> by lazy {
        // Format: hour -> (paletteIndex -> count)
        val map = mutableMapOf<Int, MutableMap<Int, Int>>()
        val dataStr = prefs.getString(KEY_TIME_PALETTE_MAP, "") ?: ""
        
        dataStr.split(";").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val paletteCounts = parts[1].split(",").associate { 
                    val p = it.split("=")
                    Pair(p[0].toInt(), p[1].toInt())
                }.toMutableMap()
                map[hour] = paletteCounts
            }
        }
        map
    }
    
    /**
     * Palet kullanımını kaydet.
     */
    fun recordPaletteUsage(paletteIndex: Int, hourOfDay: Int) {
        // History güncelle
        paletteHistory.add(paletteIndex)
        if (paletteHistory.size > MAX_HISTORY_SIZE) {
            paletteHistory.removeAt(0)
        }
        savePaletteHistory()
        
        // Time-palette map güncelle
        if (!timePaletteMap.containsKey(hourOfDay)) {
            timePaletteMap[hourOfDay] = mutableMapOf()
        }
        val currentCount = timePaletteMap[hourOfDay]?.get(paletteIndex) ?: 0
        timePaletteMap[hourOfDay]?.put(paletteIndex, currentCount + 1)
        saveTimePaletteMap()
    }
    
    /**
     * En çok kullanılan paletleri döndürür.
     */
    fun getFavoritePalettes(limit: Int = 3): List<Int> {
        val counts = mutableMapOf<Int, Int>()
        paletteHistory.forEach { palette ->
            counts[palette] = (counts[palette] ?: 0) + 1
        }
        return counts.toList()
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
    
    /**
     * Belirli bir saat için en uygun paleti önerir.
     */
    fun recommendPaletteForHour(hourOfDay: Int): Int? {
        // Önce bu saat için direkt veri var mı bak
        timePaletteMap[hourOfDay]?.let { counts ->
            if (counts.isNotEmpty()) {
                return counts.maxByOrNull { it.value }?.key
            }
        }
        
        // Yakın saatlere bak (±2 saat)
        val nearbyHours = listOf(
            (hourOfDay - 2 + 24) % 24,
            (hourOfDay - 1 + 24) % 24,
            (hourOfDay + 1) % 24,
            (hourOfDay + 2) % 24
        )
        
        val nearbyCounts = mutableMapOf<Int, Int>()
        nearbyHours.forEach { h ->
            timePaletteMap[h]?.forEach { (palette, count) ->
                nearbyCounts[palette] = (nearbyCounts[palette] ?: 0) + count
            }
        }
        
        return if (nearbyCounts.isNotEmpty()) {
            nearbyCounts.maxByOrNull { it.value }?.key
        } else {
            null // Hiç veri yoksa default döndür
        }
    }
    
    /**
     * Kullanıcı davranışına göre FPS ayarı önerir.
     */
    fun recommendFPSSetting(batteryLevel: Int, isCharging: Boolean): Int {
        return when {
            isCharging -> 60 // Şarjdayken maksimum
            batteryLevel > 80 -> 60
            batteryLevel > 50 -> 30
            batteryLevel > 20 -> 20
            else -> 15 // Ultra power save
        }
    }
    
    /**
     * Kullanıcı davranışına göre ses hassasiyeti önerir.
     */
    fun recommendAudioSensitivity(hourOfDay: Int): Float {
        // Gece saatlerinde daha düşük hassasiyet (0-6)
        return if (hourOfDay in 0..6 || hourOfDay in 22..23) {
            0.3f // Daha sessiz
        } else {
            0.7f // Normal hassasiyet
        }
    }
    
    /**
     * Kişiselleştirilmiş bir özet raporu oluşturur.
     */
    fun generatePersonalizationReport(): String {
        val favorites = getFavoritePalettes(5)
        val peakHour = findPeakUsageHour()
        
        return buildString {
            appendLine("=== AURA Kişiselleştirme Raporu ===")
            appendLine("En sevdiğiniz paletler: ${favorites.joinToString(", ")}")
            appendLine("En aktif saat: $peakHour:00")
            appendLine("Önerilen FPS: ${recommendFPSSetting(50, false)}")
            appendLine("Şu anki saat için önerilen palet: ${recommendPaletteForHour(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY))}")
        }
    }
    
    /**
     * En çok kullanım saati bul.
     */
    private fun findPeakUsageHour(): Int {
        return timePaletteMap.maxByOrNull { (_, counts) -> 
            counts.values.sum() 
        }?.key ?: 12 // Default öğle
    }
    
    private fun savePaletteHistory() {
        prefs.edit()
            .putString(KEY_PALETTE_HISTORY, paletteHistory.joinToString(","))
            .apply()
    }
    
    private fun saveTimePaletteMap() {
        val dataStr = timePaletteMap.entries.joinToString(";") { (hour, counts) ->
            "$hour:${counts.entries.joinToString(",") { "${it.key}=${it.value}" }}"
        }
        prefs.edit()
            .putString(KEY_TIME_PALETTE_MAP, dataStr)
            .apply()
    }
    
    /**
     * Tüm kişiselleştirme verilerini sıfırlar.
     */
    fun resetAllData() {
        prefs.edit().clear().apply()
        paletteHistory.clear()
        timePaletteMap.clear()
    }
}
