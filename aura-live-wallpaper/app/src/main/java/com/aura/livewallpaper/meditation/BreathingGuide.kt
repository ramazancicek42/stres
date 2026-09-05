package com.aura.livewallpaper.meditation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Nefes rehberi - 4-7-8 tekniği ile meditasyon için görsel rehberlik sağlar.
 * 
 * 4-7-8 Tekniği:
 * - 4 saniye nefes al (inflate)
 * - 7 saniye nefesi tut (hold)
 * - 8 saniye nefes ver (deflate)
 * 
 * Toplam döngü: 19 saniye
 */
class BreathingGuide {
    
    enum class BreathingPhase {
        INHALE,    // Nefes al (4 saniye)
        HOLD,      // Nefesi tut (7 saniye)
        EXHALE,    // Nefes ver (8 saniye)
        IDLE       // Başlamadı/bitti
    }
    
    data class BreathingState(
        val phase: BreathingPhase = BreathingPhase.IDLE,
        val progress: Float = 0f,        // 0-1 arası
        val phaseProgress: Float = 0f,   // Mevcut fazda 0-1
        val cycleCount: Int = 0,         // Tamamlanan döngü sayısı
        val breathIntensity: Float = 0f, // Nefes şiddeti (0-1, görsel nabız için)
        val isInhaling: Boolean = false,
        val isExhaling: Boolean = false,
        val isHolding: Boolean = false
    )
    
    private val _state = MutableStateFlow(BreathingState())
    val state: StateFlow<BreathingState> = _state.asStateFlow()
    
    private var isActive = false
    private var phaseStartTime = 0L
    private var currentPhaseDuration = 0L
    private var totalCycles = 0
    
    // Faz süreleri (ms)
    private val inhaleDuration = 4000L   // 4 saniye
    private val holdDuration = 7000L     // 7 saniye
    private val exhaleDuration = 8000L   // 8 saniye
    
    // Yumuşak geçiş için
    private var targetIntensity = 0f
    private var currentIntensity = 0f
    
    /**
     * Nefes rehberini başlat
     */
    fun start() {
        isActive = true
        phaseStartTime = System.currentTimeMillis()
        currentPhaseDuration = inhaleDuration
        totalCycles = 0
        _state.value = BreathingState(
            phase = BreathingPhase.INHALE,
            progress = 0f,
            phaseProgress = 0f,
            cycleCount = 0,
            breathIntensity = 0f,
            isInhaling = true,
            isExhaling = false,
            isHolding = false
        )
    }
    
    /**
     * Nefes rehberini durdur
     */
    fun stop() {
        isActive = false
        _state.value = BreathingState()
    }
    
    /**
     * Her frame'de çağrılacak - durumu güncelle
     * @return Nefes rehberi aktif mi
     */
    fun update(): Boolean {
        if (!isActive) return false
        
        val now = System.currentTimeMillis()
        val elapsed = now - phaseStartTime
        val phaseProgress = (elapsed.toFloat() / currentPhaseDuration).coerceIn(0f, 1f)
        
        // Mevcut fazı belirle
        val currentState = _state.value
        var newPhase = currentState.phase
        var newCycleCount = currentState.cycleCount
        
        when (currentState.phase) {
            BreathingPhase.INHALE -> {
                targetIntensity = 1f
                if (elapsed >= inhaleDuration) {
                    newPhase = BreathingPhase.HOLD
                    phaseStartTime = now
                    currentPhaseDuration = holdDuration
                }
            }
            BreathingPhase.HOLD -> {
                targetIntensity = 1f
                if (elapsed >= holdDuration) {
                    newPhase = BreathingPhase.EXHALE
                    phaseStartTime = now
                    currentPhaseDuration = exhaleDuration
                }
            }
            BreathingPhase.EXHALE -> {
                targetIntensity = 0f
                if (elapsed >= exhaleDuration) {
                    newPhase = BreathingPhase.INHALE
                    phaseStartTime = now
                    currentPhaseDuration = inhaleDuration
                    newCycleCount++
                }
            }
            BreathingPhase.IDLE -> {
                // Başlamadı
            }
        }
        
        // Yumuşak geçiş (lerp)
        currentIntensity += (targetIntensity - currentIntensity) * 0.1f
        
        // State'i güncelle
        _state.value = BreathingState(
            phase = newPhase,
            progress = (newCycleCount * 19000L + getPhaseOffset(newPhase) + elapsed).toFloat() / 
                       (19000L * (newCycleCount + 1)).toFloat(),
            phaseProgress = phaseProgress,
            cycleCount = newCycleCount,
            breathIntensity = currentIntensity,
            isInhaling = newPhase == BreathingPhase.INHALE,
            isExhaling = newPhase == BreathingPhase.EXHALE,
            isHolding = newPhase == BreathingPhase.HOLD
        )
        
        totalCycles = newCycleCount
        return true
    }
    
    private fun getPhaseOffset(phase: BreathingPhase): Long {
        return when (phase) {
            BreathingPhase.INHALE -> 0L
            BreathingPhase.HOLD -> inhaleDuration
            BreathingPhase.EXHALE -> inhaleDuration + holdDuration
            BreathingPhase.IDLE -> 0L
        }
    }
    
    /**
     * Nefes intensitesini al (0-1)
     * Fraktal render'da kullanılır
     */
    fun getBreathIntensity(): Float {
        return _state.value.breathIntensity
    }
    
    /**
     * Mevcut nefes fazını al
     */
    fun getCurrentPhase(): BreathingPhase {
        return _state.value.phase
    }
    
    /**
     * Tamamlanan döngü sayısını al
     */
    fun getCycleCount(): Int {
        return totalCycles
    }
    
    /**
     * Nefes göstergesi için normalize edilmiş sinüs dalgası
     * Daha doğal bir nefes hissi için
     */
    fun getSmoothBreathValue(): Float {
        if (!isActive) return 0f
        
        val state = _state.value
        val now = System.currentTimeMillis()
        val elapsed = (now - phaseStartTime).toFloat()
        
        return when (state.phase) {
            BreathingPhase.INHALE -> {
                // Sinüs ile yumuşak çıkış
                sin((elapsed / inhaleDuration) * Math.PI / 2).toFloat()
            }
            BreathingPhase.HOLD -> {
                1f
            }
            BreathingPhase.EXHALE -> {
                // Sinüs ile yumuşak iniş
                cos((elapsed / exhaleDuration) * Math.PI / 2).toFloat()
            }
            BreathingPhase.IDLE -> 0f
        }
    }
}
