package com.aura.livewallpaper.meditation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stres seviyesi tahmin edici.
 * 
 * Ses paternlerinden stres seviyesini tahmin eder:
 * - Yüksek ses = yüksek stres
 * - Düzensiz ritim = yüksek stres
 * - Sessiz ortam = düşük stres
 * - Düzenli nefes = düşük stres
 */
class StressEstimator {
    
    enum class StressLevel {
        VERY_LOW,    // 0-0.2: Çok düşük stres
        LOW,         // 0.2-0.4: Düşük stres
        MEDIUM,      // 0.4-0.6: Orta stres
        HIGH,        // 0.6-0.8: Yüksek stres
        VERY_HIGH    // 0.8-1.0: Çok yüksek stres
    }
    
    data class StressEstimate(
        val level: Float = 0.5f,          // 0-1 arası
        val category: StressLevel = StressLevel.MEDIUM,
        val confidence: Float = 0.5f,     // Tahmin güveni
        val audioStress: Float = 0.5f,    // Ses bazlı stres
        val rhythmStress: Float = 0.5f,   // Ritim bazlı stres
        val trend: Float = 0f             // -1 (azalıyor) ile 1 (artıyor)
    )
    
    private val _estimate = MutableStateFlow(StressEstimate())
    val estimate: StateFlow<StressEstimate> = _estimate.asStateFlow()
    
    // Ses verileri için buffer
    private val audioBuffer = FloatArray(100)
    private var bufferIndex = 0
    
    // Ritim takibi
    private val rhythmBuffer = FloatArray(50)
    private var rhythmIndex = 0
    private var lastPeakTime = 0L
    private val peakIntervals = mutableListOf<Long>()
    
    // Trend takibi
    private val levelHistory = FloatArray(20)
    private var historyIndex = 0
    
    // Eşik değerleri
    private val highVolumeThreshold = 0.7f
    private val lowVolumeThreshold = 0.1f
    private val irregularityThreshold = 0.3f
    
    /**
     * Ses verisini analiz et ve stres tahminini güncelle
     */
    fun processAudioData(rmsLevel: Float, timestamp: Long = System.currentTimeMillis()) {
        // Ses seviyesini kaydet
        audioBuffer[bufferIndex % audioBuffer.size] = rmsLevel
        bufferIndex++
        
        // Ritim analizi - tepe noktalarını tespit et
        if (rmsLevel > 0.3f && timestamp - lastPeakTime > 200) {
            if (lastPeakTime > 0) {
                val interval = timestamp - lastPeakTime
                peakIntervals.add(interval)
                if (peakIntervals.size > 20) {
                    peakIntervals.removeAt(0)
                }
            }
            lastPeakTime = timestamp
        }
        
        // Ritim verisini kaydet
        rhythmBuffer[rhythmIndex % rhythmBuffer.size] = rmsLevel
        rhythmIndex++
        
        // Stres hesapla
        val audioStress = calculateAudioStress()
        val rhythmStress = calculateRhythmStress()
        val overallStress = (audioStress * 0.6f + rhythmStress * 0.4f).coerceIn(0f, 1f)
        
        // Trend hesapla
        levelHistory[historyIndex % levelHistory.size] = overallStress
        historyIndex++
        val trend = calculateTrend()
        
        // Kategori belirle
        val category = when {
            overallStress < 0.2f -> StressLevel.VERY_LOW
            overallStress < 0.4f -> StressLevel.LOW
            overallStress < 0.6f -> StressLevel.MEDIUM
            overallStress < 0.8f -> StressLevel.HIGH
            else -> StressLevel.VERY_HIGH
        }
        
        // Güven hesapla
        val confidence = calculateConfidence()
        
        _estimate.value = StressEstimate(
            level = overallStress,
            category = category,
            confidence = confidence,
            audioStress = audioStress,
            rhythmStress = rhythmStress,
            trend = trend
        )
    }
    
    /**
     * Ses seviyesinden stres hesapla
     */
    private fun calculateAudioStress(): Float {
        if (bufferIndex < 10) return 0.5f // Yeterli veri yok
        
        val recentSamples = audioBuffer.take(bufferIndex.coerceAtMost(audioBuffer.size))
        val avgLevel = recentSamples.average().toFloat()
        val maxLevel = recentSamples.maxOrNull() ?: 0f
        val minLevel = recentSamples.minOrNull() ?: 0f
        
        // Yüksek ses = yüksek stres
        val volumeStress = when {
            avgLevel > highVolumeThreshold -> 0.8f
            avgLevel > 0.5f -> 0.6f
            avgLevel > 0.3f -> 0.4f
            avgLevel > lowVolumeThreshold -> 0.2f
            else -> 0.1f
        }
        
        // Düzensizlik = yüksek stres
        val variance = maxLevel - minLevel
        val irregularityStress = (variance * 2f).coerceIn(0f, 1f)
        
        return (volumeStress * 0.7f + irregularityStress * 0.3f).coerceIn(0f, 1f)
    }
    
    /**
     * Ritim analizinden stres hesapla
     */
    private fun calculateRhythmStress(): Float {
        if (peakIntervals.size < 5) return 0.5f
        
        // Ritim düzensizliğini hesapla
        val avgInterval = peakIntervals.average()
        val variance = peakIntervals.map { (it - avgInterval) * (it - avgInterval) }.average()
        val stdDev = Math.sqrt(variance)
        
        // Düşük stdDev = düzenli ritim = düşük stres
        val rhythmRegularity = (stdDev / avgInterval).toFloat().coerceIn(0f, 1f)
        
        // Çok hızlı veya çok yavaş ritim = yüksek stres
        val avgBPM = if (avgInterval > 0) 60000.0 / avgInterval else 60.0
        val bpmStress = when {
            avgBPM > 120 -> 0.8f // Çok hızlı
            avgBPM > 100 -> 0.6f
            avgBPM > 80 -> 0.4f
            avgBPM > 60 -> 0.2f // Normal
            else -> 0.3f // Çok yavaş (rahatsız edici olabilir)
        }
        
        return (rhythmRegularity * 0.5f + bpmStress * 0.5f).coerceIn(0f, 1f)
    }
    
    /**
     * Trend hesapla (stres azalıyor mu artıyor mu)
     */
    private fun calculateTrend(): Float {
        if (historyIndex < 10) return 0f
        
        val recentLevels = levelHistory.take(historyIndex.coerceAtMost(levelHistory.size))
        val halfSize = recentLevels.size / 2
        
        val firstHalf = recentLevels.take(halfSize).average()
        val secondHalf = recentLevels.drop(halfSize).average()
        
        return (secondHalf - firstHalf).toFloat().coerceIn(-1f, 1f)
    }
    
    /**
     * Tahmin güvenini hesapla
     */
    private fun calculateConfidence(): Float {
        val dataPoints = bufferIndex.coerceAtMost(audioBuffer.size)
        return when {
            dataPoints >= 50 -> 0.9f
            dataPoints >= 30 -> 0.7f
            dataPoints >= 10 -> 0.5f
            else -> 0.3f
        }
    }
    
    /**
     * Stres seviyesini manuel olarak ayarla
     * (nefes ritmi gibi harici veriler için)
     */
    fun setManualStressLevel(level: Float) {
        val current = _estimate.value
        _estimate.value = current.copy(
            level = level.coerceIn(0f, 1f),
            category = when {
                level < 0.2f -> StressLevel.VERY_LOW
                level < 0.4f -> StressLevel.LOW
                level < 0.6f -> StressLevel.MEDIUM
                level < 0.8f -> StressLevel.HIGH
                else -> StressLevel.VERY_HIGH
            }
        )
    }
    
    /**
     * Stres azaltma önerisi
     */
    fun getReductionSuggestion(): String {
        val estimate = _estimate.value
        
        return when (estimate.category) {
            StressLevel.VERY_LOW, StressLevel.LOW -> 
                "Mükemmel! Derin nefes almaya devam edin."
            StressLevel.MEDIUM -> 
                "Nefesinize odaklanın. Yavaş ve derin nefesler alın."
            StressLevel.HIGH -> 
                "Lütfen gözlerinizi kapatın ve 4-7-8 nefes tekniğini deneyin."
            StressLevel.VERY_HIGH -> 
                "Çok gerginsiniz. Önce birkaç derin nefes alın, sonra devam edin."
        }
    }
    
    /**
     * Sıfırla
     */
    fun reset() {
        audioBuffer.fill(0f)
        rhythmBuffer.fill(0f)
        levelHistory.fill(0f)
        bufferIndex = 0
        rhythmIndex = 0
        historyIndex = 0
        lastPeakTime = 0L
        peakIntervals.clear()
        _estimate.value = StressEstimate()
    }
}
