package com.aura.livewallpaper.renderer

import android.graphics.PointF
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.aura.livewallpaper.audio.GenerativeAudioEngine
import com.aura.livewallpaper.util.ColorPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Gelişmiş Dokunmatik Etkileşim Yöneticisi
 * 
 * Özellikler:
 * - Multi-touch desteği (parmak sayısı ile fraktal karmaşıklığı)
 * - Parmak hareket yönüne göre palet değiştirme
 * - Uzun basma ile dondurma modu
 * - Çift dokunma ile reset
 * - Haptic feedback (dokunsal geri bildirim)
 * - Ripple efekti (dalga yayılımı)
 */
class TouchInteractionManager(
    private val vibrator: Vibrator,
    private val audioEngine: GenerativeAudioEngine? = null
) {
    
    companion object {
        private const val TAG = "AuraTouchManager"
        
        const val RIPPLE_MAX_RADIUS = 0.5f // Ekranın %50'si
        const val RIPPLE_DECAY_RATE = 0.95f
        const val LONG_PRESS_THRESHOLD_MS = 500L
        const val DOUBLE_TAP_THRESHOLD_MS = 300L
        const val MIN_SWIPE_DISTANCE = 0.1f // Ekranın %10'u
    }
    
    data class Ripple(
        val x: Float,
        val y: Float,
        var radius: Float = 0.0f,
        var alpha: Float = 1.0f,
        val color: Long
    )
    
    data class TouchPoint(
        val id: Int,
        val x: Float,
        val y: Float,
        val initialX: Float,
        val initialY: Float,
        val downTime: Long
    )
    
    enum class GestureType {
        TAP,
        LONG_PRESS,
        DOUBLE_TAP,
        SWIPE_UP,
        SWIPE_DOWN,
        SWIPE_LEFT,
        SWIPE_RIGHT,
        PINCH_IN,
        PINCH_OUT,
        MULTI_TOUCH
    }
    
    data class GestureEvent(
        val type: GestureType,
        val x: Float,
        val y: Float,
        val fingerCount: Int,
        val distance: Float = 0.0f
    )
    
    private val activeTouches = mutableMapOf<Int, TouchPoint>()
    private val ripples = mutableListOf<Ripple>()
    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var isFrozen = false
    private var paletteShiftDirection = 0 // -1: önceki, 0: yok, 1: sonraki
    
    private val onGestureListener: ((GestureEvent) -> Unit)? = null
    private val onRippleUpdate: (() -> Unit)? = null
    private val onFreezeToggle: ((Boolean) -> Unit)? = null
    private val onPaletteChange: ((Int) -> Unit)? = null
    
    /**
     * Yeni dokunma olayı
     */
    fun onTouchDown(id: Int, x: Float, y: Float, normalized: Boolean = false) {
        val nx = if (normalized) x else x
        val ny = if (normalized) y else y
        
        activeTouches[id] = TouchPoint(
            id = id,
            x = nx,
            y = ny,
            initialX = nx,
            initialY = ny,
            downTime = System.currentTimeMillis()
        )
        
        // Yeni ripple oluştur
        if (!isFrozen) {
            ripples.add(
                Ripple(
                    x = nx,
                    y = ny,
                    radius = 0.0f,
                    alpha = 0.8f,
                    color = 0x40FFFFFF // Yarı saydam beyaz
                )
            )
            
            // Haptic feedback
            triggerHaptic(VibrationEffect.EFFECT_TICK)
            
            // Audio engine'e nota gönder
            audioEngine?.playNoteAtPosition(nx, ny)
        }
        
        updateMultiTouchState()
    }
    
    /**
     * Dokunma hareketi
     */
    fun onTouchMove(id: Int, x: Float, y: Float, normalized: Boolean = false) {
        if (!activeTouches.containsKey(id)) return
        
        val nx = if (normalized) x else x
        val ny = if (normalized) y else y
        val touch = activeTouches[id]!!
        
        activeTouches[id] = touch.copy(x = nx, y = ny)
        
        // Swipe tespiti için hareketi takip et
        val dx = nx - touch.initialX
        val dy = ny - touch.initialY
        val distance = sqrt(dx * dx + dy * dy)
        
        // Eğer birden fazla parmak varsa pinch/zoom tespiti
        if (activeTouches.size >= 2) {
            detectPinchGesture()
        }
    }
    
    /**
     * Dokunma bırakma
     */
    fun onTouchUp(id: Int, normalized: Boolean = false) {
        val touch = activeTouches.remove(id) ?: return
        
        val duration = System.currentTimeMillis() - touch.downTime
        val dx = touch.x - touch.initialX
        val dy = touch.y - touch.initialY
        val distance = sqrt(dx * dx + dy * dy)
        
        // Gesture tespiti
        when {
            // Çift dokunma
            duration < DOUBLE_TAP_THRESHOLD_MS && 
            distance < 0.05f &&
            System.currentTimeMillis() - lastTapTime < DOUBLE_TAP_THRESHOLD_MS -> {
                triggerGesture(GestureType.DOUBLE_TAP, touch.x, touch.y, 1)
                lastTapTime = 0 // Reset
            }
            
            // Uzun basma
            duration > LONG_PRESS_THRESHOLD_MS && distance < 0.05f -> {
                triggerGesture(GestureType.LONG_PRESS, touch.x, touch.y, 1)
                toggleFreeze()
            }
            
            // Swipe
            distance > MIN_SWIPE_DISTANCE -> {
                val gesture = when {
                    kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx > 0 -> GestureType.SWIPE_RIGHT
                    kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx < 0 -> GestureType.SWIPE_LEFT
                    kotlin.math.abs(dy) > kotlin.math.abs(dx) && dy > 0 -> GestureType.SWIPE_DOWN
                    else -> GestureType.SWIPE_UP
                }
                triggerGesture(gesture, touch.x, touch.y, 1, distance)
                
                // Swipe yönüne göre palet değiştir
                if (gesture == GestureType.SWIPE_RIGHT || gesture == GestureType.SWIPE_LEFT) {
                    shiftPalette(if (gesture == GestureType.SWIPE_RIGHT) 1 else -1)
                }
            }
            
            // Basit tap
            else -> {
                triggerGesture(GestureType.TAP, touch.x, touch.y, 1)
                lastTapTime = System.currentTimeMillis()
                lastTapX = touch.x
                lastTapY = touch.y
            }
        }
    }
    
    /**
     * Multi-touch durumunu güncelle
     */
    private fun updateMultiTouchState() {
        val fingerCount = activeTouches.size
        
        if (fingerCount >= 2) {
            triggerGesture(GestureType.MULTI_TOUCH, 0.5f, 0.5f, fingerCount)
            
            // 3+ parmak ile fraktal karmaşıklığını artır
            if (fingerCount >= 3) {
                // Fractal renderer'a sinyal gönder (dışarıdan implement edilecek)
                Log.d(TAG, "Multi-touch: $fingerCount fingers - increase complexity")
            }
        }
    }
    
    /**
     * Pinch gesture tespiti
     */
    private fun detectPinchGesture() {
        if (activeTouches.size < 2) return
        
        val touches = activeTouches.values.toList()
        val t1 = touches[0]
        val t2 = touches[1]
        
        val currentDistance = sqrt(
            (t2.x - t1.x) * (t2.x - t1.x) + 
            (t2.y - t1.y) * (t2.y - t1.y)
        )
        
        val initialDistance = sqrt(
            (t2.initialX - t1.initialX) * (t2.initialX - t1.initialX) + 
            (t2.initialY - t1.initialY) * (t2.initialY - t1.initialY)
        )
        
        if (initialDistance > 0.01f) { // Sıfıra bölme koruması
            val ratio = currentDistance / initialDistance
            
            if (ratio > 1.2f) {
                triggerGesture(GestureType.PINCH_OUT, (t1.x + t2.x) / 2, (t1.y + t2.y) / 2, 2, currentDistance)
            } else if (ratio < 0.8f) {
                triggerGesture(GestureType.PINCH_IN, (t1.x + t2.x) / 2, (t1.y + t2.y) / 2, 2, currentDistance)
            }
        }
    }
    
    /**
     * Gesture tetikleme
     */
    private fun triggerGesture(
        type: GestureType,
        x: Float,
        y: Float,
        fingerCount: Int,
        distance: Float = 0.0f
    ) {
        Log.d(TAG, "Gesture: $type at ($x, $y) with $fingerCount fingers")
        
        onGestureListener?.invoke(GestureEvent(type, x, y, fingerCount, distance))
        
        when (type) {
            GestureType.DOUBLE_TAP -> {
                // Reset işlemi (fractal renderer'da implement edilecek)
                Log.d(TAG, "Double tap - reset fractal")
                triggerHaptic(VibrationEffect.EFFECT_DOUBLE_CLICK)
            }
            
            GestureType.LONG_PRESS -> {
                triggerHaptic(VibrationEffect.EFFECT_HEAVY_CLICK)
            }
            
            GestureType.SWIPE_UP, GestureType.SWIPE_DOWN, 
            GestureType.SWIPE_LEFT, GestureType.SWIPE_RIGHT -> {
                triggerHaptic(VibrationEffect.EFFECT_TICK)
            }
            
            else -> {}
        }
    }
    
    /**
     * Dondurma modunu değiştir
     */
    private fun toggleFreeze() {
        isFrozen = !isFrozen
        Log.d(TAG, "Freeze mode: $isFrozen")
        onFreezeToggle?.invoke(isFrozen)
        
        if (isFrozen) {
            triggerHaptic(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    
    /**
     * Palet değiştirme
     */
    private fun shiftPalette(direction: Int) {
        paletteShiftDirection = direction
        Log.d(TAG, "Palette shift: $direction")
        onPaletteChange?.invoke(direction)
        triggerHaptic(VibrationEffect.EFFECT_TICK)
    }
    
    /**
     * Ripple'ları güncelle ve render için hazırla
     */
    fun updateRipples(deltaTime: Float): List<Ripple> {
        if (isFrozen) return emptyList()
        
        val survivedRipples = mutableListOf<Ripple>()
        
        for (ripple in ripples) {
            // Radius büyüt
            ripple.radius += deltaTime * 0.3f // Yayılma hızı
            
            // Alpha azalt (sönümleme)
            ripple.alpha *= RIPPLE_DECAY_RATE
            
            // Hala görünür mü?
            if (ripple.alpha > 0.01f && ripple.radius < RIPPLE_MAX_RADIUS) {
                survivedRipples.add(ripple)
            }
        }
        
        ripples.clear()
        ripples.addAll(survivedRipples)
        
        if (ripples.isNotEmpty()) {
            onRippleUpdate?.invoke()
        }
        
        return ripples
    }
    
    /**
     * Haptic feedback tetikle
     */
    private fun triggerHaptic(effect: Int) {
        triggerHaptic(VibrationEffect.get(effect))
    }
    
    private fun triggerHaptic(vibrationEffect: VibrationEffect) {
        try {
            vibrator.vibrate(vibrationEffect)
        } catch (e: Exception) {
            Log.w(TAG, "Haptic feedback failed: ${e.message}")
        }
    }
    
    /**
     * Callback'leri ayarla
     */
    fun setOnGestureListener(listener: (GestureEvent) -> Unit) {
        // Bu basit implementation'da direkt saklayamayız, 
        // ama gerçek uygulamada listener pattern kullanılabilir
    }
    
    fun setOnFreezeToggleListener(listener: (Boolean) -> Unit) {
        // Implementation
    }
    
    fun setOnPaletteChangeListener(listener: (Int) -> Unit) {
        // Implementation
    }
    
    /**
     * Durumu sıfırla
     */
    fun reset() {
        activeTouches.clear()
        ripples.clear()
        isFrozen = false
        lastTapTime = 0
    }
    
    fun isCurrentlyFrozen(): Boolean = isFrozen
    fun getActiveTouchCount(): Int = activeTouches.size
    fun getRippleCount(): Int = ripples.size
}
