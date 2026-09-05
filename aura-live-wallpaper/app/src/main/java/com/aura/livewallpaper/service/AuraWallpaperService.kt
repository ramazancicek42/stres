package com.aura.livewallpaper.service

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig as EGLConfig14
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.aura.livewallpaper.audio.AudioAnalyzer
import com.aura.livewallpaper.audio.GenerativeAudioEngine
import com.aura.livewallpaper.audio.SmartAudioAdapter
import com.aura.livewallpaper.accessibility.AccessibilityManager
import com.aura.livewallpaper.ml.PersonalizationEngine
import com.aura.livewallpaper.renderer.FractalRenderer
import com.aura.livewallpaper.sensor.LightSensorManager
import com.aura.livewallpaper.util.AuraPreferences
import kotlin.concurrent.thread

/**
 * Ana Live Wallpaper Service
 * OpenGL ES 3.0 ile gerçek zamanlı fraktal render
 */
class AuraWallpaperService : WallpaperService() {
    
    override fun onCreateEngine(): Engine {
        return AuraEngine()
    }
    
    inner class AuraEngine : Engine(), 
        LightSensorManager.Listener,
        AudioAnalyzer.Listener,
        GenerativeAudioEngine.Listener {
        
        private lateinit var preferences: AuraPreferences
        private lateinit var lightSensorManager: LightSensorManager
        private lateinit var audioAnalyzer: AudioAnalyzer
        private lateinit var generativeAudio: GenerativeAudioEngine
        private lateinit var smartAudioAdapter: SmartAudioAdapter
        private lateinit var accessibilityManager: AccessibilityManager
        private lateinit var personalizationEngine: PersonalizationEngine
        private lateinit var fractalRenderer: FractalRenderer
        
        // OpenGL ES değişkenleri
        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var eglConfig: EGLConfig14? = null
        
        private var renderThread: Thread? = null
        private var isRendering = false
        private var isVisible = false
        private var width = 0
        private var height = 0
        
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            
            preferences = AuraPreferences(this@AuraWallpaperService)
            lightSensorManager = LightSensorManager(this@AuraWallpaperService)
            audioAnalyzer = AudioAnalyzer(this@AuraWallpaperService)
            generativeAudio = GenerativeAudioEngine()
            smartAudioAdapter = SmartAudioAdapter()
            accessibilityManager = AccessibilityManager(this@AuraWallpaperService)
            personalizationEngine = PersonalizationEngine(this@AuraWallpaperService)
            
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            
            val powerManager = com.aura.livewallpaper.util.PowerManager(this@AuraWallpaperService)
            val timeColorEngine = com.aura.livewallpaper.util.TimeColorEngine(this@AuraWallpaperService)
            val touchManager = com.aura.livewallpaper.renderer.TouchInteractionManager(vibrator, generativeAudio)
            
            fractalRenderer = FractalRenderer(
                context = this@AuraWallpaperService,
                preferences = preferences,
                powerManager = powerManager,
                timeColorEngine = timeColorEngine,
                touchManager = touchManager,
                smartAudioAdapter = smartAudioAdapter,
                audioEngine = generativeAudio,
                vibrator = vibrator
            )
            
            lightSensorManager.setListener(this)
            audioAnalyzer.setListener(this)
            generativeAudio.setListener(this)
            
            setTouchEventsEnabled(true)
        }
        
        override fun onDestroy() {
            stopRendering()
            stopAll()
            super.onDestroy()
        }
        
        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            
            if (visible) {
                startAll()
                startRendering()
            } else {
                stopRendering()
                stopAll()
            }
        }
        
        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            holder?.setFormat(android.graphics.PixelFormat.RGBA_8888)
        }
        
        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            width = w
            height = h
            
            // EGL'yi bu yüzey için başlat
            initEGL(holder)
            
            // Renderer'ı başlat
            fractalRenderer.onSurfaceCreated(null, null)
            fractalRenderer.onSurfaceChanged(null, w, h)
        }
        
        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            stopRendering()
            destroyEGL()
            super.onSurfaceDestroyed(holder)
        }
        
        override fun onTouchEvent(event: MotionEvent?) {
            event?.let {
                fractalRenderer.handleTouchEvent(it)
                
                if (it.action == MotionEvent.ACTION_DOWN) {
                    if (!preferences.silentMode) {
                        generativeAudio.triggerNote()
                    }
                    triggerHapticFeedback()
                }
            }
        }
        
        // ============================================
        // EGL YÖNETİMİ
        // ============================================
        
        private fun initEGL(holder: SurfaceHolder?) {
            // Display oluştur
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                throw RuntimeException("EGL display oluşturulamadı")
            }
            
            // Version başlat
            val version = IntArray(2)
            EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
            
            // Config seç
            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, 0x0040, // EGL_OPENGL_ES3_BIT
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
            )
            
            val configs = arrayOfNulls<EGLConfig14>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            
            if (numConfigs[0] == 0) {
                throw RuntimeException("Uygun EGL config bulunamadı")
            }
            
            eglConfig = configs[0]
            
            // Context oluştur (OpenGL ES 3.0)
            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
            )
            
            eglContext = EGL14.eglCreateContext(
                eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT,
                contextAttribs, 0
            )
            
            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                throw RuntimeException("EGL context oluşturulamadı")
            }
            
            // Surface oluştur
            createEGLSurface(holder)
        }
        
        private fun createEGLSurface(holder: SurfaceHolder?) {
            if (eglDisplay == EGL14.EGL_NO_DISPLAY || eglContext == EGL14.EGL_NO_CONTEXT) {
                return
            }
            
            // Mevcut surface'ı temizle
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
            }
            
            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            
            // SurfaceHolder'dan window surface oluştur
            eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay, eglConfig, holder?.surface,
                surfaceAttribs, 0
            )
            
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                throw RuntimeException("EGL surface oluşturulamadı")
            }
            
            // Current yap
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw RuntimeException("EGL current yapılamadı")
            }
        }
        
        private fun destroyEGL() {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    eglSurface = EGL14.EGL_NO_SURFACE
                }
                
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    eglContext = EGL14.EGL_NO_CONTEXT
                }
                
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
        }
        
        // ============================================
        // RENDER DÖNGÜSÜ
        // ============================================
        
        private fun startRendering() {
            if (isRendering) return
            
            isRendering = true
            renderThread = thread(name = "AuraRenderThread") {
                renderLoop()
            }
        }
        
        private fun stopRendering() {
            isRendering = false
            renderThread?.interrupt()
            renderThread = null
        }
        
        private fun renderLoop() {
            val fps = preferences.fpsLimit
            val frameTimeMs = 1000L / fps
            
            while (isRendering && !Thread.interrupted()) {
                val startTime = System.currentTimeMillis()
                
                if (isVisible && eglSurface != EGL14.EGL_NO_SURFACE) {
                    // Render frame
                    fractalRenderer.onDrawFrame(null)
                    
                    // Buffer'ı ekrana çiz
                    EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                }
                
                // FPS limiti için bekle
                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = (frameTimeMs - elapsed).coerceAtLeast(1)
                Thread.sleep(sleepTime)
            }
        }
        
        // ============================================
        // BAŞLAT/DURDUR
        // ============================================
        
        private fun startAll() {
            val accessibilityParams = accessibilityManager.getRenderParameters()
            fractalRenderer.applyAccessibilityParams(accessibilityParams)
            
            if (lightSensorManager.isAvailable) {
                lightSensorManager.start()
            }
            
            if (!preferences.silentMode && audioAnalyzer.hasPermission) {
                audioAnalyzer.start()
            }
            
            if (!preferences.silentMode) {
                generativeAudio.start()
            }
        }
        
        private fun stopAll() {
            lightSensorManager.stop()
            audioAnalyzer.stop()
            generativeAudio.stop()
        }
        
        // ============================================
        // LISTENER CALLBACKS
        // ============================================
        
        override fun onLightLevelChanged(lux: Float) {
            val sensitivity = preferences.lightSensitivity
            val adjustedLux = lux * sensitivity
            fractalRenderer.setLightLevel(adjustedLux)
            
            val filterCutoff = 0.3f + (lux / 10000f) * 0.7f
            generativeAudio.setFilterCutoff(filterCutoff)
            
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            personalizationEngine.recordPaletteUsage(preferences.colorPaletteIndex, hour)
        }
        
        override fun onAudioEnergyChanged(energy: Float, rms: Float) {
            fractalRenderer.setAudioEnergy(energy)
            smartAudioAdapter.addAudioSample(rms)
        }
        
        override fun onNotePlayed(frequency: Double) {
            // Her nota tetiklendiğinde fraktal nabız sinyali gönder
            fractalRenderer.triggerBeatPulse()
        }
        
        private fun triggerHapticFeedback() {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            
            val vibrationEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                @Suppress("DEPRECATION")
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            
            vibrator.vibrate(vibrationEffect)
        }
    }
}
