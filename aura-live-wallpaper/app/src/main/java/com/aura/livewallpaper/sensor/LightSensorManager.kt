package com.aura.livewallpaper.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Ortam ışık sensöründen veri okuyan sınıf
 * SensorManager.TYPE_LIGHT kullanır - kamera değil, pil dostu ve gizlilik odaklı
 */
class LightSensorManager(context: Context) : SensorEventListener {
    
    interface Listener {
        fun onLightLevelChanged(lux: Float)
    }
    
    private val sensorManager: SensorManager = 
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    
    private var listener: Listener? = null
    private var currentLux: Float = 50f // Varsayılan orta seviye
    
    val isAvailable: Boolean get() = lightSensor != null
    
    fun setListener(listener: Listener) {
        this.listener = listener
    }
    
    fun start() {
        lightSensor?.let { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI // Pil tasarrufu için uygun aralık
            )
        }
    }
    
    fun stop() {
        sensorManager.unregisterListener(this)
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            currentLux = event.values[0].coerceIn(0f, 10000f) // 0-10000 LUX aralığı
            listener?.onLightLevelChanged(currentLux)
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Gerekirse accuracy değişikliklerini işle
    }
    
    /**
     * LUX değerini 0.0 - 1.0 aralığında normalize eder
     */
    fun getNormalizedLightLevel(): Float {
        return (currentLux / 10000f).coerceIn(0f, 1f)
    }
}
