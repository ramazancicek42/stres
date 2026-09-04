package com.aura.livewallpaper.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

/**
 * Akıllı Güç Yöneticisi
 * 
 * Pil seviyesi, şarj durumu, cihaz ısısı ve wallpaper görünürlüğüne göre
 * sistemin performans parametrelerini dinamik olarak ayarlar.
 * 
 * Özellikler:
 * - Pil %20'nin altına düşünce "Ultra Power Save" modu
 * - Şarjdayken "Performance" modu
 * - Cihaz ısınırsa otomatik throttling
 * - Ekran kapalıyken tüm işlemleri durdurma
 */
class PowerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AuraPowerManager"
        
        const val MIN_FPS = 15
        const val DEFAULT_FPS = 30
        const val MAX_FPS = 60
        
        const val MIN_SAMPLING_RATE_MS = 100L
        const val DEFAULT_SAMPLING_RATE_MS = 200L
        const val MAX_SAMPLING_RATE_MS = 500L
        
        const val QUALITY_LOW = 0.5f
        const val QUALITY_MEDIUM = 0.75f
        const val QUALITY_HIGH = 1.0f
    }
    
    enum class PowerMode {
        ULTRA_SAVE,      // < %15 pil veya aşırı ısınma
        POWER_SAVE,      // %15-%30 pil
        BALANCED,        // %30-%80 pil (normal kullanım)
        PERFORMANCE      // > %80 pil veya şarjda
    }
    
    data class PerformanceProfile(
        val targetFps: Int,
        val renderScale: Float,
        val sensorSamplingRateMs: Long,
        val audioEnabled: Boolean,
        val lightSensorEnabled: Boolean,
        val hapticEnabled: Boolean
    )
    
    private val _powerMode = MutableStateFlow(PowerMode.BALANCED)
    val powerMode: StateFlow<PowerMode> = _powerMode.asStateFlow()
    
    private val _performanceProfile = MutableStateFlow(getProfileForMode(PowerMode.BALANCED))
    val performanceProfile: StateFlow<PerformanceProfile> = _performanceProfile.asStateFlow()
    
    private var isScreenVisible = false
    private var batteryLevel = 100
    private var isCharging = false
    private var isOverheating = false
    
    init {
        registerBatteryReceiver()
        updatePowerMode()
    }
    
    fun setScreenVisible(visible: Boolean) {
        isScreenVisible = visible
        updatePowerMode()
    }
    
    fun setOverheating(overheating: Boolean) {
        isOverheating = overheating
        updatePowerMode()
    }
    
    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryLevel = if (level < 0 || scale < 0) 100 else (level * 100 / scale)
                
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                
                updatePowerMode()
            }
        }, filter)
    }
    
    private fun updatePowerMode() {
        val newMode = when {
            // Ekran görünmüyorsa ultra tasarruf
            !isScreenVisible -> PowerMode.ULTRA_SAVE
            
            // Aşırı ısınma
            isOverheating -> PowerMode.ULTRA_SAVE
            
            // Çok düşük pil
            batteryLevel < 15 -> PowerMode.ULTRA_SAVE
            
            // Düşük pil
            batteryLevel < 30 -> PowerMode.POWER_SAVE
            
            // Şarjdayken performans
            isCharging -> PowerMode.PERFORMANCE
            
            // Yüksek pil
            batteryLevel > 80 -> PowerMode.PERFORMANCE
            
            // Normal durum
            else -> PowerMode.BALANCED
        }
        
        if (_powerMode.value != newMode) {
            _powerMode.value = newMode
            _performanceProfile.value = getProfileForMode(newMode)
            
            Log.d(TAG, "Power mode changed to $newMode (Battery: $batteryLevel%, Charging: $isCharging)")
        }
    }
    
    private fun getProfileForMode(mode: PowerMode): PerformanceProfile {
        return when (mode) {
            PowerMode.ULTRA_SAVE -> PerformanceProfile(
                targetFps = MIN_FPS,
                renderScale = QUALITY_LOW,
                sensorSamplingRateMs = MAX_SAMPLING_RATE_MS,
                audioEnabled = false,
                lightSensorEnabled = true, // Işık sensörü çok az güç tüketir
                hapticEnabled = false
            )
            
            PowerMode.POWER_SAVE -> PerformanceProfile(
                targetFps = 20,
                renderScale = QUALITY_LOW,
                sensorSamplingRateMs = 400L,
                audioEnabled = false,
                lightSensorEnabled = true,
                hapticEnabled = false
            )
            
            PowerMode.BALANCED -> PerformanceProfile(
                targetFps = DEFAULT_FPS,
                renderScale = QUALITY_MEDIUM,
                sensorSamplingRateMs = DEFAULT_SAMPLING_RATE_MS,
                audioEnabled = true,
                lightSensorEnabled = true,
                hapticEnabled = true
            )
            
            PowerMode.PERFORMANCE -> PerformanceProfile(
                targetFps = MAX_FPS,
                renderScale = QUALITY_HIGH,
                sensorSamplingRateMs = MIN_SAMPLING_RATE_MS,
                audioEnabled = true,
                lightSensorEnabled = true,
                hapticEnabled = true
            )
        }
    }
    
    /**
     * Kullanıcı tercihleri ile birleştirilmiş final profil
     */
    fun getFinalProfile(userPreferredFps: Int, userAudioEnabled: Boolean): PerformanceProfile {
        val baseProfile = _performanceProfile.value
        
        return baseProfile.copy(
            targetFps = min(userPreferredFps, baseProfile.targetFps),
            audioEnabled = baseProfile.audioEnabled && userAudioEnabled
        )
    }
    
    fun getCurrentBatteryLevel(): Int = batteryLevel
    
    fun isOptimizedForBattery(): Boolean = _powerMode.value in listOf(PowerMode.ULTRA_SAVE, PowerMode.POWER_SAVE)
    
    fun isScreenVisible(): Boolean = isScreenVisible
}
