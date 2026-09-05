package com.aura.livewallpaper.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.Vibrator
import android.view.MotionEvent
import com.aura.livewallpaper.audio.GenerativeAudioEngine
import com.aura.livewallpaper.audio.SmartAudioAdapter
import com.aura.livewallpaper.util.AuraPreferences
import com.aura.livewallpaper.util.PowerManager
import com.aura.livewallpaper.util.TimeColorEngine
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Professional Fraktal Renderer v2.0
 * 
 * Multi-fraktal sistemi:
 * - Julia Set (klasik)
 * - Burning Ship (agresif)
 * - Mandelbrot (keşif)
 * - Flow Field (akış)
 * - Reaction-Diffusion (organik)
 * 
 * Ek özellikler:
 * - Multi-octave noise
 * - Domain warping (gelişmiş)
 * - Smooth coloring (yüksek kalite)
 * - Professional vignette
 * - Film grain
 */
class FractalRenderer(
    private val context: Context,
    private val preferences: AuraPreferences,
    private val powerManager: PowerManager,
    private val timeColorEngine: TimeColorEngine? = null,
    private val touchManager: TouchInteractionManager? = null,
    private val smartAudioAdapter: SmartAudioAdapter? = null,
    private val audioEngine: GenerativeAudioEngine? = null,
    private val vibrator: Vibrator? = null
) : GLSurfaceView.Renderer {

    private var fullScreenQuad: FullScreenQuad? = null
    private var width = 0
    private var height = 0

    // Uniform location'lari
    private var uTimeLocation = -1
    private var uLightLevelLocation = -1
    private var uAudioEnergyLocation = -1
    private var uBeatSyncLocation = -1
    private var uTouchPosLocation = -1
    private var uTouchIntensityLocation = -1
    private var uRippleLocation = -1
    private var uColorDarkLocation = -1
    private var uColorMidLocation = -1
    private var uColorLightLocation = -1
    private var uAspectRatioLocation = -1
    private var uComplexityLocation = -1
    private var uFrozenLocation = -1
    private var uFractalTypeLocation = -1
    private var uZoomLocation = -1
    private var uPanOffsetLocation = -1

    // State
    private var startTime = 0L
    private var lightLevel = 0.5f
    private var audioEnergy = 0f
    private var beatSyncSignal = 0f
    private var touchX = 0.5f
    private var touchY = 0.5f
    private var touchIntensity = 0f
    private var lastTouchTime = 0L
    private var isFrozen = false
    private var fractalComplexity = 1.0f
    private var currentPaletteIndex = 0
    private var autoPaletteEnabled = true
    
    // Adaptif parametreler (meditasyon sistemi için)
    private var colorSaturation = 1f
    private var animationSpeed = 1f
    
    // Ripple efekti için
    private var rippleX = -1f
    private var rippleY = -1f
    private var rippleRadius = 0f
    private var rippleAlpha = 0f
    
    // Yeni parametreler
    private var fractalType = 0 // 0: Julia, 1: Burning Ship, 2: Mandelbrot, 3: Flow Field, 4: Reaction-Diffusion
    private var zoomLevel = 1.0f
    private var panOffsetX = 0f
    private var panOffsetY = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 1f)

        fullScreenQuad = FullScreenQuad()
        fullScreenQuad?.createProgram(VERTEX_SHADER, FRACTAL_FRAGMENT_SHADER)
        fullScreenQuad?.useProgram()

        // Uniform location'lari al
        uTimeLocation = fullScreenQuad?.getUniformLocation("uTime") ?: -1
        uLightLevelLocation = fullScreenQuad?.getUniformLocation("uLightLevel") ?: -1
        uAudioEnergyLocation = fullScreenQuad?.getUniformLocation("uAudioEnergy") ?: -1
        uBeatSyncLocation = fullScreenQuad?.getUniformLocation("uBeatSync") ?: -1
        uTouchPosLocation = fullScreenQuad?.getUniformLocation("uTouchPos") ?: -1
        uTouchIntensityLocation = fullScreenQuad?.getUniformLocation("uTouchIntensity") ?: -1
        uRippleLocation = fullScreenQuad?.getUniformLocation("uRipple") ?: -1
        uColorDarkLocation = fullScreenQuad?.getUniformLocation("uColorDark") ?: -1
        uColorMidLocation = fullScreenQuad?.getUniformLocation("uColorMid") ?: -1
        uColorLightLocation = fullScreenQuad?.getUniformLocation("uColorLight") ?: -1
        uAspectRatioLocation = fullScreenQuad?.getUniformLocation("uAspectRatio") ?: -1
        uComplexityLocation = fullScreenQuad?.getUniformLocation("uComplexity") ?: -1
        uFrozenLocation = fullScreenQuad?.getUniformLocation("uFrozen") ?: -1
        uFractalTypeLocation = fullScreenQuad?.getUniformLocation("uFractalType") ?: -1
        uZoomLocation = fullScreenQuad?.getUniformLocation("uZoom") ?: -1
        uPanOffsetLocation = fullScreenQuad?.getUniformLocation("uPanOffset") ?: -1

        startTime = System.currentTimeMillis()
        currentPaletteIndex = preferences.colorPaletteIndex
        autoPaletteEnabled = preferences.autoPaletteEnabled
        
        // Touch manager callback'lerini ayarla
        setupTouchCallbacks()
    }

    private fun setupTouchCallbacks() {
        touchManager?.setOnFreezeToggleListener { frozen ->
            isFrozen = frozen
        }
        
        touchManager?.setOnPaletteChangeListener { direction ->
            cyclePalette(direction)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Power manager'dan performans profili al
        val profile = powerManager.getFinalProfile(preferences.targetFps, preferences.audioEnabled)
        
        // Ekran görünür değilse render atla
        if (!powerManager.isScreenVisible()) {
            return
        }

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        fullScreenQuad?.useProgram()

        val currentTime = (System.currentTimeMillis() - startTime) / 1000f * animationSpeed

        // Time-based palette güncelleme
        if (autoPaletteEnabled) {
            timeColorEngine?.update()
            updateTimeBasedPalette()
        }

        // Beat sync sinyali
        beatSyncSignal = smartAudioAdapter?.getBeatSyncSignal() ?: 0f

        // Touch intensity zamanla azalir (exponential decay)
        val timeSinceTouch = (System.currentTimeMillis() - lastTouchTime) / 1000f
        val decayedTouchIntensity = if (timeSinceTouch < 2.0f) {
            touchIntensity * kotlin.math.exp(-timeSinceTouch * 2f)
        } else {
            0f
        }

        // Ripple güncelleme
        updateRipple(currentTime)

        // Aspect ratio (sifira bolme hatasini onle)
        val aspectRatio = if (width > 0 && height > 0) width.toFloat() / height else 1f

        // Uniform'lari ayarla
        GLES30.glUniform1f(uTimeLocation, currentTime)
        GLES30.glUniform1f(uLightLevelLocation, lightLevel)
        GLES30.glUniform1f(uAudioEnergyLocation, audioEnergy * profile.renderScale)
        GLES30.glUniform1f(uBeatSyncLocation, beatSyncSignal)
        GLES30.glUniform2f(uTouchPosLocation, touchX, touchY)
        GLES30.glUniform1f(uTouchIntensityLocation, decayedTouchIntensity * profile.renderScale)
        GLES30.glUniform4f(uRippleLocation, rippleX, rippleY, rippleRadius, rippleAlpha)
        GLES30.glUniform1f(uComplexityLocation, fractalComplexity * profile.renderScale)
        GLES30.glUniform1i(uFrozenLocation, if (isFrozen) 1 else 0)
        GLES30.glUniform1f(uAspectRatioLocation, aspectRatio)
        GLES30.glUniform1i(uFractalTypeLocation, fractalType)
        GLES30.glUniform1f(uZoomLocation, zoomLevel)
        GLES30.glUniform2f(uPanOffsetLocation, panOffsetX, panOffsetY)

        // Renk paleti
        val colors = getCurrentPaletteColors()
        GLES30.glUniform3fv(uColorDarkLocation, 1, colors, 0)
        GLES30.glUniform3fv(uColorMidLocation, 1, colors, 3)
        GLES30.glUniform3fv(uColorLightLocation, 1, colors, 6)

        fullScreenQuad?.draw()
    }

    /**
     * Zaman bazlı palet renklerini al ve uygula
     */
    private fun updateTimeBasedPalette() {
        if (!autoPaletteEnabled) return
        
        timeColorEngine?.currentPalette?.value?.let { timePalette ->
            val targetIndex = when (timePalette.timeOfDay) {
                com.aura.livewallpaper.util.TimeColorEngine.TimeOfDay.MORNING -> 5
                com.aura.livewallpaper.util.TimeColorEngine.TimeOfDay.NOON -> 0
                com.aura.livewallpaper.util.TimeColorEngine.TimeOfDay.EVENING -> 1
                com.aura.livewallpaper.util.TimeColorEngine.TimeOfDay.NIGHT -> 3
            }
            
            if (targetIndex != currentPaletteIndex) {
                currentPaletteIndex = targetIndex
                preferences.colorPaletteIndex = targetIndex
            }
        }
    }

    /**
     * Mevcut palet renklerini float array olarak döndür
     */
    private fun getCurrentPaletteColors(): FloatArray {
        return when (val index = currentPaletteIndex % 8) {
            0 -> oceanPalette
            1 -> sunsetPalette
            2 -> forestPalette
            3 -> cosmicPalette
            4 -> amberPalette
            5 -> sunrisePalette
            6 -> neonPalette
            7 -> auroraPalette
            else -> oceanPalette
        }
    }

    /**
     * Palet değiştirme (swipe ile)
     */
    private fun cyclePalette(direction: Int) {
        currentPaletteIndex = (currentPaletteIndex + direction + 8) % 8
        preferences.colorPaletteIndex = currentPaletteIndex
    }

    /**
     * Ripple efektini güncelle
     */
    private fun updateRipple(time: Float) {
        if (rippleAlpha > 0.01f) {
            rippleRadius += 0.3f
            rippleAlpha *= 0.94f
            
            if (rippleRadius > 0.6f || rippleAlpha < 0.01f) {
                rippleAlpha = 0f
            }
        }
    }

    // ============================================
    // PUBLIC FONKSİYONLAR
    // ============================================

    fun setLightLevel(lux: Float) {
        this.lightLevel = (lux / 10000f).coerceIn(0f, 1f)
    }

    fun setAudioEnergy(energy: Float) {
        this.audioEnergy = energy * preferences.audioSensitivity
    }
    
    /**
     * Beat pulse tetikle (nota çalındığında)
     */
    fun triggerBeatPulse() {
        this.beatSyncSignal = 1.0f
    }
    
    fun setColorSaturation(saturation: Float) {
        this.colorSaturation = saturation.coerceIn(0f, 2f)
    }
    
    fun setAnimationSpeed(speed: Float) {
        this.animationSpeed = speed.coerceIn(0f, 2f)
    }
    
    /**
     * Fraktal tipini ayarla
     * 0: Julia Set, 1: Burning Ship, 2: Mandelbrot, 3: Flow Field, 4: Reaction-Diffusion
     */
    fun setFractalType(type: Int) {
        this.fractalType = type.coerceIn(0, 4)
    }
    
    /**
     * Zoom seviyesini ayarla
     */
    fun setZoom(level: Float) {
        this.zoomLevel = level.coerceIn(0.1f, 5.0f)
    }
    
    /**
     * Pan offset ayarla
     */
    fun setPanOffset(x: Float, y: Float) {
        this.panOffsetX = x.coerceIn(-2f, 2f)
        this.panOffsetY = y.coerceIn(-2f, 2f)
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (touchManager != null) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    for (i in 0 until event.pointerCount) {
                        val id = event.getPointerId(i)
                        val x = event.getX(i) / width.coerceAtLeast(1)
                        val y = event.getY(i) / height.coerceAtLeast(1)
                        touchManager.onTouchDown(id, x, y, normalized = true)
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until event.pointerCount) {
                        val id = event.getPointerId(i)
                        val x = event.getX(i) / width.coerceAtLeast(1)
                        val y = event.getY(i) / height.coerceAtLeast(1)
                        touchManager.onTouchMove(id, x, y, normalized = true)
                    }
                    return true
                }
                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                    for (i in 0 until event.pointerCount) {
                        val id = event.getPointerId(i)
                        touchManager.onTouchUp(id, normalized = true)
                    }
                    return true
                }
            }
        }
        
        // Fallback: eski tek-dokunma sistemi
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x / width.coerceAtLeast(1)
                touchY = 1.0f - (event.y / height.coerceAtLeast(1))
                touchIntensity = 1f
                lastTouchTime = System.currentTimeMillis()
                
                // Ripple başlat
                rippleX = touchX
                rippleY = touchY
                rippleRadius = 0f
                rippleAlpha = 1f
                
                // Audio engine'e nota gönder
                audioEngine?.playNoteAtPosition(touchX, touchY)
                
                return true
            }
        }
        return false
    }

    fun setPaletteIndex(index: Int) {
        currentPaletteIndex = index.coerceIn(0, 7)
        autoPaletteEnabled = false
        preferences.colorPaletteIndex = currentPaletteIndex
    }

    fun setAutoPaletteEnabled(enabled: Boolean) {
        autoPaletteEnabled = enabled
    }

    fun setFractalComplexity(complexity: Float) {
        fractalComplexity = complexity.coerceIn(0.5f, 2.0f)
    }
    
    fun applyAccessibilityParams(params: com.aura.livewallpaper.accessibility.AccessibilityManager.AccessibilityRenderParams) {
        if (params.reduceMotion) {
            fractalComplexity = 0.5f
        }
        if (params.disableFlashing) {
            beatSyncSignal = 0f
        }
    }

    fun toggleFreeze() {
        isFrozen = !isFrozen
    }

    fun resetFractal() {
        fractalComplexity = 1.0f
        touchIntensity = 0f
        rippleAlpha = 0f
        zoomLevel = 1.0f
        panOffsetX = 0f
        panOffsetY = 0f
        startTime = System.currentTimeMillis()
    }

    fun cleanup() {
        touchManager?.reset()
        fullScreenQuad?.cleanup()
        fullScreenQuad = null
    }
    
    // ============================================
    // PALETLER
    // ============================================
    
    private val oceanPalette = floatArrayOf(
        0.06f, 0.20f, 0.48f,
        0.10f, 0.37f, 0.73f,
        0.29f, 0.56f, 0.85f
    )
    
    private val sunsetPalette = floatArrayOf(
        0.18f, 0.11f, 0.18f,
        0.72f, 0.36f, 0.22f,
        0.96f, 0.64f, 0.38f
    )
    
    private val forestPalette = floatArrayOf(
        0.06f, 0.16f, 0.12f,
        0.18f, 0.42f, 0.31f,
        0.32f, 0.72f, 0.53f
    )
    
    private val cosmicPalette = floatArrayOf(
        0.04f, 0.04f, 0.07f,
        0.48f, 0.41f, 0.67f,
        0.58f, 0.44f, 0.86f
    )
    
    private val amberPalette = floatArrayOf(
        0.18f, 0.12f, 0.06f,
        0.72f, 0.49f, 0.22f,
        0.96f, 0.77f, 0.38f
    )
    
    private val sunrisePalette = floatArrayOf(
        0.10f, 0.10f, 0.18f,
        1.00f, 0.55f, 0.26f,
        1.00f, 0.84f, 0.00f
    )
    
    private val neonPalette = floatArrayOf(
        0.04f, 0.04f, 0.04f,
        0.00f, 1.00f, 1.00f,
        1.00f, 0.00f, 1.00f
    )
    
    private val auroraPalette = floatArrayOf(
        0.02f, 0.05f, 0.10f,
        0.10f, 0.80f, 0.50f,
        0.50f, 0.20f, 0.90f
    )
}
