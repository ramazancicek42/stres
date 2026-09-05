package com.aura.livewallpaper.meditation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Adaptif Tepki Sistemi - Stres seviyesine göre uygulamanın davranışını değiştirir.
 * 
 * Düşük stres: Canlı renkler, hızlı animasyon, upbeat müzik
 * Yüksek stres: Pastel renkler, yavaş animasyon, sakin müzik
 */
class AdaptiveResponseSystem {
    
    data class AdaptiveParams(
        val colorSaturation: Float = 1f,     // Renk doygunluğu (0-1)
        val animationSpeed: Float = 1f,      // Animasyon hızı çarpanı
        val audioVolume: Float = 0.5f,       // Ses seviyesi
        val fractalComplexity: Float = 1f,   // Fraktal karmaşıklığı
        val warmthShift: Float = 0f,         // Renk sıcaklığı (-1 soğuk, 1 sıcak)
        val breathingGuide: Boolean = false, // Nefes rehberi aktif mi
        val suggestedMode: String = "normal" // Önerilen mod
    )
    
    private val _params = MutableStateFlow(AdaptiveParams())
    val params: StateFlow<AdaptiveParams> = _params.asStateFlow()
    
    private var stressEstimator: StressEstimator? = null
    private var breathingGuide: BreathingGuide? = null
    
    // Geçiş yumuşatması
    private var targetParams = AdaptiveParams()
    private var currentParams = AdaptiveParams()
    
    /**
     * Bağımlılıkları ayarla
     */
    fun setup(stressEstimator: StressEstimator, breathingGuide: BreathingGuide) {
        this.stressEstimator = stressEstimator
        this.breathingGuide = breathingGuide
    }
    
    /**
     * Her frame'de güncelle
     */
    fun update() {
        val stress = stressEstimator?.estimate?.value?.level ?: 0.5f
        val breathIntensity = breathingGuide?.getBreathIntensity() ?: 0f
        val breathPhase = breathingGuide?.getCurrentPhase() ?: BreathingGuide.BreathingPhase.IDLE
        
        // Stres seviyesine göre parametreleri hesapla
        targetParams = calculateParams(stress, breathIntensity, breathPhase)
        
        // Yumuşak geçiş (lerp)
        currentParams = AdaptiveParams(
            colorSaturation = lerp(currentParams.colorSaturation, targetParams.colorSaturation, 0.05f),
            animationSpeed = lerp(currentParams.animationSpeed, targetParams.animationSpeed, 0.05f),
            audioVolume = lerp(currentParams.audioVolume, targetParams.audioVolume, 0.05f),
            fractalComplexity = lerp(currentParams.fractalComplexity, targetParams.fractalComplexity, 0.05f),
            warmthShift = lerp(currentParams.warmthShift, targetParams.warmthShift, 0.03f),
            breathingGuide = targetParams.breathingGuide,
            suggestedMode = targetParams.suggestedMode
        )
        
        _params.value = currentParams
    }
    
    /**
     * Stres seviyesine göre parametreleri hesapla
     */
    private fun calculateParams(
        stressLevel: Float,
        breathIntensity: Float,
        breathPhase: BreathingGuide.BreathingPhase
    ): AdaptiveParams {
        return when {
            // Çok düşük stres (0-0.2) - Neşeli mod
            stressLevel < 0.2f -> AdaptiveParams(
                colorSaturation = 1.2f,
                animationSpeed = 1.2f,
                audioVolume = 0.6f,
                fractalComplexity = 1.2f,
                warmthShift = 0.3f,
                breathingGuide = false,
                suggestedMode = "joyful"
            )
            
            // Düşük stres (0.2-0.4) - Normal mod
            stressLevel < 0.4f -> AdaptiveParams(
                colorSaturation = 1f,
                animationSpeed = 1f,
                audioVolume = 0.5f,
                fractalComplexity = 1f,
                warmthShift = 0f,
                breathingGuide = false,
                suggestedMode = "normal"
            )
            
            // Orta stres (0.4-0.6) - Sakinleştirici mod
            stressLevel < 0.6f -> AdaptiveParams(
                colorSaturation = 0.8f,
                animationSpeed = 0.8f,
                audioVolume = 0.4f,
                fractalComplexity = 0.8f,
                warmthShift = -0.2f,
                breathingGuide = true,
                suggestedMode = "calming"
            )
            
            // Yüksek stres (0.6-0.8) - Yoğun sakinleştirici
            stressLevel < 0.8f -> AdaptiveParams(
                colorSaturation = 0.6f,
                animationSpeed = 0.5f,
                audioVolume = 0.3f,
                fractalComplexity = 0.6f,
                warmthShift = -0.4f,
                breathingGuide = true,
                suggestedMode = "deep_calm"
            )
            
            // Çok yüksek stres (0.8-1.0) - Maksimum sakinleştirici
            else -> AdaptiveParams(
                colorSaturation = 0.4f,
                animationSpeed = 0.3f,
                audioVolume = 0.2f,
                fractalComplexity = 0.4f,
                warmthShift = -0.6f,
                breathingGuide = true,
                suggestedMode = "emergency_calm"
            )
        }
    }
    
    /**
     * Nefes fazına göre ek ayarlamalar
     */
    fun getBreathAdjustedParams(): AdaptiveParams {
        val base = currentParams
        val breathGuide = breathingGuide ?: return base
        
        if (!breathGuide.update()) return base
        
        val breathValue = breathGuide.getSmoothBreathValue()
        
        return base.copy(
            // Nefes alırken hafifçe parlaklık artır
            colorSaturation = base.colorSaturation * (1f + breathValue * 0.1f),
            // Nefes alırken hafifçe hareketi yavaşlat
            animationSpeed = base.animationSpeed * (1f - breathValue * 0.1f)
        )
    }
    
    /**
     * Stres seviyesini al
     */
    fun getCurrentStressLevel(): Float {
        return stressEstimator?.estimate?.value?.level ?: 0.5f
    }
    
    /**
     * Önerilen renk paletini al
     */
    fun getSuggestedPalette(): String {
        val stress = getCurrentStressLevel()
        return when {
            stress < 0.3f -> "Sunrise"  // Canlı, enerjik
            stress < 0.5f -> "Ocean"    // Dengeli
            stress < 0.7f -> "Forest"   // Sakinleştirici
            else -> "Cosmic"            // En sakin
        }
    }
    
    /**
     * Önerilen ses frekansını al
     */
    fun getSuggestedFrequency(): Double {
        val stress = getCurrentStressLevel()
        return when {
            stress < 0.3f -> 440.0  // A4 - neşeli
            stress < 0.5f -> 392.0  // G4 - dengeli
            stress < 0.7f -> 329.63 // E4 - sakinleştirici
            else -> 261.63          // C4 - en sakin
        }
    }
    
    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }
}
