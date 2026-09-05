package com.aura.livewallpaper.service

import android.content.Context
import android.opengl.GLSurfaceView
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
import com.aura.livewallpaper.meditation.MeditationSessionManager
import com.aura.livewallpaper.meditation.BreathingGuide
import com.aura.livewallpaper.meditation.StressEstimator
import com.aura.livewallpaper.meditation.AdaptiveResponseSystem
import com.aura.livewallpaper.renderer.FractalRenderer
import com.aura.livewallpaper.sensor.LightSensorManager
import com.aura.livewallpaper.util.AuraPreferences

/**
 * Ana Live Wallpaper Service
 * Tüm bileşenleri birleştirir: sensörler, render, ses
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
        private lateinit var meditationSessionManager: MeditationSessionManager
        private lateinit var breathingGuide: BreathingGuide
        private lateinit var stressEstimator: StressEstimator
        private lateinit var adaptiveResponseSystem: AdaptiveResponseSystem
        private lateinit var fractalRenderer: FractalRenderer
        
        private var glSurfaceView: GLSurfaceView? = null
        private var isVisible = false
        
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            
            preferences = AuraPreferences(this@AuraWallpaperService)
            lightSensorManager = LightSensorManager(this@AuraWallpaperService)
            audioAnalyzer = AudioAnalyzer(this@AuraWallpaperService)
            generativeAudio = GenerativeAudioEngine()
            smartAudioAdapter = SmartAudioAdapter()
            accessibilityManager = AccessibilityManager(this@AuraWallpaperService)
            personalizationEngine = PersonalizationEngine(this@AuraWallpaperService)
            
            // Meditasyon bileşenlerini başlat
            meditationSessionManager = MeditationSessionManager(this@AuraWallpaperService)
            breathingGuide = BreathingGuide()
            stressEstimator = StressEstimator()
            adaptiveResponseSystem = AdaptiveResponseSystem()
            adaptiveResponseSystem.setup(stressEstimator, breathingGuide)
            
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
            
            // Listener'ları bağla
            lightSensorManager.setListener(this)
            audioAnalyzer.setListener(this)
            generativeAudio.setListener(this)
            
            setTouchEventsEnabled(true)
        }
        
        override fun onDestroy() {
            stopAll()
            super.onDestroy()
        }
        
        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            
            if (visible) {
                startAll()
            } else {
                stopAll()
            }
        }
        
        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            
            glSurfaceView = GLSurfaceView(this@AuraWallpaperService).apply {
                setEGLContextClientVersion(2)
                setRenderer(fractalRenderer)
                
                // FPS limiti ayarla
                val fps = preferences.fpsLimit
                if (fps <= 30) {
                    // Düşük FPS modu - manuel kontrol
                    renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                    // İlk render'ı tetikle
                    post { requestRender() }
                } else {
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
            }
            
            holder?.setFormat(android.graphics.PixelFormat.RGBA_8888)
        }
        
        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            glSurfaceView?.onPause()
            glSurfaceView = null
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
        
        private fun startAll() {
            // Erişilebilirlik ayarlarını uygula
            val accessibilityParams = accessibilityManager.getRenderParameters()
            fractalRenderer.applyAccessibilityParams(accessibilityParams)
            
            // Işık sensörünü başlat (her zaman açık olabilir, düşük pil)
            if (lightSensorManager.isAvailable) {
                lightSensorManager.start()
            }
            
            // Ses analizini başlat (sadece sessiz mod değilse ve izin varsa)
            if (!preferences.silentMode && audioAnalyzer.hasPermission) {
                audioAnalyzer.start()
            }
            
            // Ses motorunu başlat (sadece sessiz mod değilse)
            if (!preferences.silentMode) {
                generativeAudio.start()
            }
            
            // Render'ı devam ettir
            glSurfaceView?.onResume()
        }
        
        private fun stopAll() {
            lightSensorManager.stop()
            audioAnalyzer.stop()
            generativeAudio.stop()
            glSurfaceView?.onPause()
        }
        
        // LightSensorManager.Listener
        override fun onLightLevelChanged(lux: Float) {
            val sensitivity = preferences.lightSensitivity
            val adjustedLux = lux * sensitivity
            fractalRenderer.setLightLevel(adjustedLux)
            
            // Işık seviyesi ses filtresini de etkiler
            val filterCutoff = 0.3f + (lux / 10000f) * 0.7f
            generativeAudio.setFilterCutoff(filterCutoff)
            
            // Kişiselleştirme motoru için palet kullanımını kaydet
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            personalizationEngine.recordPaletteUsage(preferences.colorPaletteIndex, hour)
            
            // Meditasyon seansı için ışık verisini kaydet
            meditationSessionManager.recordLightLevel(lux / 10000f)
        }
        
        // AudioAnalyzer.Listener
        override fun onAudioEnergyChanged(energy: Float, rms: Float) {
            fractalRenderer.setAudioEnergy(energy)
            // SmartAudioAdapter'a ses verisini ilet (beat detection için)
            smartAudioAdapter.addAudioSample(rms)
            
            // Meditasyon sistemi için ses verisini analiz et
            if (meditationSessionManager.isMeditating.value) {
                stressEstimator.processAudioData(rms)
                meditationSessionManager.recordStressLevel(stressEstimator.estimate.value.level)
                
                // Adaptif tepki sistemini güncelle
                adaptiveResponseSystem.update()
                val adaptiveParams = adaptiveResponseSystem.params.value
                
                // Renderer'a adaptif parametreleri uygula
                fractalRenderer.setColorSaturation(adaptiveParams.colorSaturation)
                fractalRenderer.setAnimationSpeed(adaptiveParams.animationSpeed)
                fractalRenderer.setFractalComplexity(adaptiveParams.fractalComplexity)
                
                // Ses seviyesini ayarla
                generativeAudio.setVolume(adaptiveParams.audioVolume)
            }
        }
        
        // GenerativeAudioEngine.Listener
        override fun onNotePlayed(frequency: Double) {
            // İsteğe bağlı: her nota çaldığında hafif haptic feedback
            // (şimdilik sadece touch'ta feedback veriyoruz)
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
