package com.aura.livewallpaper.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.aura.livewallpaper.util.AuraPreferences
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Fraktal renderer - OpenGL ES ile animasyonlu Julia set render eder
 * Sensör verileri ve dokunma girdilerine tepki verir
 */
class FractalRenderer(
    private val context: Context,
    private val preferences: AuraPreferences
) : GLSurfaceView.Renderer {

    private var fullScreenQuad: FullScreenQuad? = null
    private var width = 0
    private var height = 0

    // Uniform location'lari
    private var uTimeLocation = -1
    private var uLightLevelLocation = -1
    private var uAudioEnergyLocation = -1
    private var uTouchPosLocation = -1
    private var uTouchIntensityLocation = -1
    private var uColorDarkLocation = -1
    private var uColorMidLocation = -1
    private var uColorLightLocation = -1
    private var uAspectRatioLocation = -1

    // State
    private var startTime = 0L
    private var lightLevel = 0.5f
    private var audioEnergy = 0f
    private var touchX = 0.5f
    private var touchY = 0.5f
    private var touchIntensity = 0f
    private var lastTouchTime = 0L

    // Renk paletleri
    private val colorPalettes = listOf(
        floatArrayOf(0.04f, 0.09f, 0.16f, 0.10f, 0.31f, 0.48f, 0.29f, 0.56f, 0.85f), // Ocean
        floatArrayOf(0.18f, 0.11f, 0.18f, 0.72f, 0.36f, 0.22f, 0.96f, 0.64f, 0.38f), // Sunset
        floatArrayOf(0.06f, 0.16f, 0.12f, 0.18f, 0.42f, 0.31f, 0.32f, 0.72f, 0.53f), // Forest
        floatArrayOf(0.10f, 0.06f, 0.18f, 0.29f, 0.18f, 0.48f, 0.61f, 0.45f, 0.81f), // Night
        floatArrayOf(0.18f, 0.12f, 0.06f, 0.72f, 0.49f, 0.22f, 0.96f, 0.77f, 0.38f)  // Amber
    )

    private var currentPaletteIndex = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        fullScreenQuad = FullScreenQuad()
        fullScreenQuad?.createProgram(VERTEX_SHADER, FRACTAL_FRAGMENT_SHADER)
        fullScreenQuad?.useProgram()

        // Uniform location'lari al
        uTimeLocation = fullScreenQuad?.getUniformLocation("uTime") ?: -1
        uLightLevelLocation = fullScreenQuad?.getUniformLocation("uLightLevel") ?: -1
        uAudioEnergyLocation = fullScreenQuad?.getUniformLocation("uAudioEnergy") ?: -1
        uTouchPosLocation = fullScreenQuad?.getUniformLocation("uTouchPos") ?: -1
        uTouchIntensityLocation = fullScreenQuad?.getUniformLocation("uTouchIntensity") ?: -1
        uColorDarkLocation = fullScreenQuad?.getUniformLocation("uColorDark") ?: -1
        uColorMidLocation = fullScreenQuad?.getUniformLocation("uColorMid") ?: -1
        uColorLightLocation = fullScreenQuad?.getUniformLocation("uColorLight") ?: -1
        uAspectRatioLocation = fullScreenQuad?.getUniformLocation("uAspectRatio") ?: -1

        startTime = System.currentTimeMillis()
        currentPaletteIndex = preferences.colorPaletteIndex
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        fullScreenQuad?.useProgram()

        val currentTime = (System.currentTimeMillis() - startTime) / 1000f

        // Touch intensity zamanla azalir (exponential decay)
        val timeSinceTouch = (System.currentTimeMillis() - lastTouchTime) / 1000f
        val decayedTouchIntensity = if (timeSinceTouch < 2.0f) {
            touchIntensity * (1.0f - timeSinceTouch / 2.0f)
        } else {
            0f
        }

        // Uniform'lari ayarla
        GLES20.glUniform1f(uTimeLocation, currentTime)
        GLES20.glUniform1f(uLightLevelLocation, lightLevel)
        GLES20.glUniform1f(uAudioEnergyLocation, audioEnergy)
        GLES20.glUniform2f(uTouchPosLocation, touchX, touchY)
        GLES20.glUniform1f(uTouchIntensityLocation, decayedTouchIntensity)

        // Aspect ratio (sifira bolme hatasini onle)
        val aspectRatio = if (width > 0 && height > 0) width.toFloat() / height else 1f
        GLES20.glUniform1f(uAspectRatioLocation, aspectRatio)

        // Renk paleti
        val palette = colorPalettes.getOrElse(currentPaletteIndex) { colorPalettes[0] }
        GLES20.glUniform3fv(uColorDarkLocation, 1, palette, 0)
        GLES20.glUniform3fv(uColorMidLocation, 1, palette, 3)
        GLES20.glUniform3fv(uColorLightLocation, 1, palette, 6)

        fullScreenQuad?.draw()
    }

    fun setLightLevel(lux: Float) {
        // LUX degerini normalize et (0-10000 -> 0-1)
        this.lightLevel = (lux / 10000f).coerceIn(0f, 1f)
    }

    fun setAudioEnergy(energy: Float) {
        this.audioEnergy = energy * preferences.audioSensitivity
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Normalized touch coordinates (0-1 araligi)
                touchX = event.x / width.coerceAtLeast(1)
                touchY = 1.0f - (event.y / height.coerceAtLeast(1)) // Y koordinatini ters cevir
                touchIntensity = 1f
                lastTouchTime = System.currentTimeMillis()
                return true
            }
        }
        return false
    }

    fun setPaletteIndex(index: Int) {
        currentPaletteIndex = index.coerceIn(0, colorPalettes.size - 1)
    }

    fun cleanup() {
        fullScreenQuad?.cleanup()
        fullScreenQuad = null
    }
}
