package com.aura.livewallpaper.ai

import android.content.Context
import com.aura.livewallpaper.util.AuraPreferences
import com.aura.livewallpaper.util.PowerManager
import com.aura.livewallpaper.util.TimeColorEngine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * AI Personalization Engine - Kullanıcı alışkanlıklarını öğrenir ve otomatik öneriler sunar
 * 
 * Özellikler:
 * - Kullanım zamanı analizi (hangi saatlerde daha aktif)
 * - Tercih edilen palet öğrenme
 * - Otomatik ayar optimizasyonu
 * - Davranışsal pattern tespiti
 */
class AIPersonalizationEngine(private val context: Context) {
    
    companion object {
        private const val LEARNING_PERIOD_HOURS = 72 // 3 günlük öğrenme periyodu
        private const val MIN_DATA_POINTS = 50 // Öneri için minimum veri noktası
        private const val MAX_HISTORY_SIZE = 500 // Maksimum geçmiş boyutu
        
        // Kullanıcı davranış tipleri
        enum class UserBehaviorType {
            MORNING_PERSON,      // Sabahları aktif
            NIGHT_OWL,          // Geceleri aktif
            BALANCED,           // Gün içinde dengeli
            POWER_USER,         // Sürekli yüksek performans
            BATTERY_SAVER       // Pil tasarrufu odaklı
        }
    }
    
    data class UsageSession(
        val timestamp: Long,
        val hourOfDay: Int,
        val batteryLevel: Int,
        val preferredPalette: String,
        val fpsSetting: Int,
        val brightnessSensitivity: Float,
        val audioEnabled: Boolean,
        val sessionDurationMinutes: Long
    )
    
    data class UserPattern(
        val behaviorType: UserBehaviorType,
        val peakHours: List<Int>,
        val favoritePalettes: Map<String, Int>,
        val avgFpsPreference: Int,
        val avgBrightnessSensitivity: Float,
        val audioUsageRatio: Float,
        val confidenceScore: Float // 0.0 - 1.0 arası güven skoru
    )
    
    private val preferences = AuraPreferences(context)
    private val powerManager = PowerManager(context)
    private val timeColorEngine = TimeColorEngine(context)
    
    private val usageHistory = mutableListOf<UsageSession>()
    private var currentSessionStart: Long? = null
    private var lastKnownPalette: String = ""
    
    init {
        loadHistory()
    }
    
    /**
     * Yeni kullanım oturumu başlat
     */
    fun startSession() {
        currentSessionStart = System.currentTimeMillis()
        lastKnownPalette = preferences.selectedPalette
    }
    
    /**
     * Kullanım oturumunu sonlandır ve kaydet
     */
    fun endSession() {
        val sessionStart = currentSessionStart ?: return
        val duration = (System.currentTimeMillis() - sessionStart) / 60000 // dakika
        
        if (duration < 1) return // 1 dakikadan kısa oturumları yoksayla
        
        val now = System.currentTimeMillis()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        val session = UsageSession(
            timestamp = now,
            hourOfDay = hour,
            batteryLevel = powerManager.batteryLevel,
            preferredPalette = lastKnownPalette,
            fpsSetting = preferences.targetFps,
            brightnessSensitivity = preferences.lightSensitivity,
            audioEnabled = preferences.audioEnabled,
            sessionDurationMinutes = duration
        )
        
        usageHistory.add(session)
        trimHistory()
        saveHistory()
        
        // Pattern güncelleme
        if (usageHistory.size >= MIN_DATA_POINTS) {
            analyzeAndUpdatePatterns()
        }
        
        currentSessionStart = null
    }
    
    /**
     * Palet değişikliğini kaydet
     */
    fun onPaletteChanged(paletteName: String) {
        lastKnownPalette = paletteName
    }
    
    /**
     * Ayar değişikliğini kaydet
     */
    fun onSettingsChanged() {
        // Ayar değişikliklerini izle (opsiyonel)
    }
    
    /**
     * Kullanıcı pattern'ini analiz et
     */
    private fun analyzeAndUpdatePatterns(): UserPattern {
        if (usageHistory.size < MIN_DATA_POINTS) {
            return createDefaultPattern()
        }
        
        // Saatlik dağılım analizi
        val hourDistribution = mutableMapOf<Int, Int>()
        for (i in 0 until 24) hourDistribution[i] = 0
        
        usageHistory.forEach { session ->
            hourDistribution[session.hourOfDay] = (hourDistribution[session.hourOfDay] ?: 0) + 1
        }
        
        // Peak saatleri bul
        val sortedHours = hourDistribution.toList().sortedByDescending { it.second }
        val peakHours = sortedHours.take(3).map { it.first }
        
        // Davranış tipi belirleme
        val behaviorType = determineBehaviorType(hourDistribution)
        
        // Palet tercihleri
        val paletteCounts = mutableMapOf<String, Int>()
        usageHistory.forEach { session ->
            paletteCounts[session.preferredPalette] = (paletteCounts[session.preferredPalette] ?: 0) + 1
        }
        val favoritePalettes = paletteCounts.entries.sortedByDescending { it.value }.associate { it.key to it.value }
        
        // Ortalama FPS tercihi
        val avgFps = usageHistory.map { it.fpsSetting }.average().toInt()
        
        // Ortalama parlaklık hassasiyeti
        val avgBrightness = usageHistory.map { it.brightnessSensitivity }.average().toFloat()
        
        // Audio kullanım oranı
        val audioUsageRatio = usageHistory.count { it.audioEnabled }.toFloat() / usageHistory.size
        
        // Güven skoru hesaplama (veri miktarına göre)
        val confidenceScore = min(1.0f, usageHistory.size.toFloat() / MAX_HISTORY_SIZE)
        
        val pattern = UserPattern(
            behaviorType = behaviorType,
            peakHours = peakHours,
            favoritePalettes = favoritePalettes,
            avgFpsPreference = avgFps,
            avgBrightnessSensitivity = avgBrightness,
            audioUsageRatio = audioUsageRatio,
            confidenceScore = confidenceScore
        )
        
        // Otomatik öneriler uygula
        applySmartRecommendations(pattern)
        
        return pattern
    }
    
    /**
     * Davranış tipini belirle
     */
    private fun determineBehaviorType(hourDistribution: Map<Int, Int>): UserBehaviorType {
        val morningActivity = (6..11).sumOf { hourDistribution[it] ?: 0 }
        val eveningActivity = (18..23).sumOf { hourDistribution[it] ?: 0 }
        val nightActivity = (0..5).sumOf { hourDistribution[it] ?: 0 }
        val dayActivity = (12..17).sumOf { hourDistribution[it] ?: 0 }
        
        val total = morningActivity + eveningActivity + nightActivity + dayActivity
        if (total == 0) return UserBehaviorType.BALANCED
        
        // Güç kullanıcılarını tespit et (yüksek FPS, uzun oturumlar)
        val highFpsRatio = usageHistory.count { it.fpsSetting >= 60 }.toFloat() / usageHistory.size
        val avgDuration = usageHistory.map { it.sessionDurationMinutes }.average()
        
        if (highFpsRatio > 0.7 && avgDuration > 30) {
            return UserBehaviorType.POWER_USER
        }
        
        // Pil tasarrufu odaklı kullanıcılar
        val lowFpsRatio = usageHistory.count { it.fpsSetting <= 30 }.toFloat() / usageHistory.size
        if (lowFpsRatio > 0.7 || usageHistory.all { it.batteryLevel < 30 }) {
            return UserBehaviorType.BATTERY_SAVER
        }
        
        // Zaman bazlı tipler
        if (morningActivity > eveningActivity * 1.5 && morningActivity > nightActivity * 2) {
            return UserBehaviorType.MORNING_PERSON
        }
        
        if (nightActivity > morningActivity * 2 && eveningActivity > dayActivity) {
            return UserBehaviorType.NIGHT_OWL
        }
        
        return UserBehaviorType.BALANCED
    }
    
    /**
     * Akıllı önerileri uygula
     */
    private fun applySmartRecommendations(pattern: UserPattern) {
        if (pattern.confidenceScore < 0.5f) return // Yetersiz güven skoru
        
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        // 1. Otomatik palet önerisi (zaman + tercih bazlı)
        if (preferences.autoPalette) {
            val recommendedPalette = recommendPaletteForHour(currentHour, pattern)
            if (recommendedPalette != preferences.selectedPalette) {
                // Kullanıcıya bildirim göster (opsiyonel)
                // preferences.selectedPalette = recommendedPalette
            }
        }
        
        // 2. Otomatik FPS ayarı (davranış tipine göre)
        when (pattern.behaviorType) {
            UserBehaviorType.POWER_USER -> {
                if (powerManager.batteryLevel > 50 && preferences.targetFps < 60) {
                    // Öneri: 60 FPS'e geç
                }
            }
            UserBehaviorType.BATTERY_SAVER -> {
                if (powerManager.batteryLevel < 30 && preferences.targetFps > 30) {
                    // Öneri: 30 FPS'e düşür
                }
            }
            else -> {}
        }
        
        // 3. Ses ayarı önerisi
        if (pattern.audioUsageRatio < 0.3 && preferences.audioEnabled) {
            // Kullanıcı nadiren ses kullanıyor, kapatmayı öner
        }
    }
    
    /**
     * Saate göre palet öner
     */
    fun recommendPaletteForHour(hour: Int, pattern: UserPattern): String {
        // Kullanıcının favori paletlerini al
        val favorites = pattern.favoritePalettes.keys.take(3)
        
        // Zaman dilimine uygun paletleri al
        val timeAppropriatePalettes = timeColorEngine.getRecommendedPalettesForHour(hour)
        
        // Kesişimi bul (hem favori hem zamana uygun)
        val intersection = favorites.intersect(timeAppropriatePalettes.toSet())
        
        return if (intersection.isNotEmpty()) {
            intersection.first()
        } else if (favorites.isNotEmpty()) {
            favorites.first()
        } else {
            timeAppropriatePalettes.firstOrNull() ?: "Cosmic"
        }
    }
    
    /**
     * Otomatik ayar optimizasyonu öner
     */
    fun getOptimizationSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (usageHistory.size < MIN_DATA_POINTS) {
            suggestions.add("Daha fazla kullanım verisi toplandıktan sonra kişiselleştirilmiş öneriler sunulacaktır.")
            return suggestions
        }
        
        val pattern = analyzeAndUpdatePatterns()
        
        when (pattern.behaviorType) {
            UserBehaviorType.MORNING_PERSON -> {
                suggestions.add("Sabah saatlerinde 'Sunrise' paleti otomatik aktif edilsin mi?")
            }
            UserBehaviorType.NIGHT_OWL -> {
                suggestions.add("Gece saatlerinde 'Neon' veya 'Cosmic' paleti önerilsin mi?")
            }
            UserBehaviorType.POWER_USER -> {
                if (powerManager.batteryLevel > 50) {
                    suggestions.add("Yüksek performans modu aktif edilsin mi? (60 FPS, yüksek çözünürlük)")
                }
            }
            UserBehaviorType.BATTERY_SAVER -> {
                suggestions.add("Pil tasarrufu modu kalıcı olarak aktif edilsin mi? (30 FPS, düşük sensör hızı)")
            }
            UserBehaviorType.BALANCED -> {
                suggestions.add("Günün saatine göre otomatik palet değişimi aktif edilsin mi?")
            }
        }
        
        // Palet önerisi
        if (pattern.favoritePalettes.isNotEmpty()) {
            val topPalette = pattern.favoritePalettes.keys.first()
            suggestions.add("En çok kullandığınız palet: $topPalette. Varsayılan olarak ayarlansın mı?")
        }
        
        // Ses önerisi
        if (pattern.audioUsageRatio < 0.2) {
            suggestions.add("Ses efektlerini nadiren kullanıyorsunuz. Pil tasarrufu için kapatılsın mı?")
        } else if (pattern.audioUsageRatio > 0.8) {
            suggestions.add("Ses efektlerini sık kullanıyorsunuz. 'Beat Sync' özelliği aktif edilsin mi?")
        }
        
        return suggestions
    }
    
    /**
     * Kullanıcı istatistiklerini al
     */
    fun getUserStatistics(): Map<String, Any> {
        if (usageHistory.isEmpty()) {
            return mapOf("message" to "Yeterli veri yok")
        }
        
        val pattern = analyzeAndUpdatePatterns()
        val totalHours = usageHistory.sumOf { it.sessionDurationMinutes } / 60
        val avgSessionLength = usageHistory.map { it.sessionDurationMinutes }.average()
        
        val result = mutableMapOf<String, Any>()
        result["behaviorType"] = pattern.behaviorType.name
        result["peakHours"] = pattern.peakHours.joinToString("-")
        result["favoritePalette"] = pattern.favoritePalettes.keys.firstOrNull() ?: "N/A"
        result["totalUsageHours"] = totalHours
        result["avgSessionLength"] = "%.1f".format(avgSessionLength)
        result["audioUsagePercent"] = "${(pattern.audioUsageRatio * 100).toInt()}%"
        result["confidenceScore"] = "${(pattern.confidenceScore * 100).toInt()}%"
        result["dataPoints"] = usageHistory.size
        return result
    }
    
    /**
     * Geçmişi yükle
     */
    private fun loadHistory() {
        val prefs = context.getSharedPreferences("ai_personalization", Context.MODE_PRIVATE)
        val historyJson = prefs.getString("usage_history", "") ?: ""
        
        // Basit JSON parsing (gerçek uygulamada Gson/Moshi kullanılmalı)
        // Bu örnek için basit tutuldu
    }
    
    /**
     * Geçmişi kaydet
     */
    private fun saveHistory() {
        val prefs = context.getSharedPreferences("ai_personalization", Context.MODE_PRIVATE)
        // Basit JSON serialization (gerçek uygulamada Gson/Moshi kullanılmalı)
        prefs.edit().putString("usage_history", "serialized_data").apply()
    }
    
    /**
     * Geçmişi kırp
     */
    private fun trimHistory() {
        while (usageHistory.size > MAX_HISTORY_SIZE) {
            usageHistory.removeAt(0)
        }
    }
    
    /**
     * Varsayılan pattern oluştur
     */
    private fun createDefaultPattern(): UserPattern {
        return UserPattern(
            behaviorType = UserBehaviorType.BALANCED,
            peakHours = listOf(12, 18, 20),
            favoritePalettes = mapOf("Cosmic" to 1),
            avgFpsPreference = 30,
            avgBrightnessSensitivity = 0.5f,
            audioUsageRatio = 0.5f,
            confidenceScore = 0.0f
        )
    }
    
    /**
     * Verileri temizle
     */
    fun clearHistory() {
        usageHistory.clear()
        currentSessionStart = null
        val prefs = context.getSharedPreferences("ai_personalization", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
