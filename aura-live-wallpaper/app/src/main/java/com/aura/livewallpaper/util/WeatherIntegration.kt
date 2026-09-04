package com.aura.livewallpaper.util

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Hava Durumu Entegrasyonu
 * 
 * Özellikler:
 * - Konuma göre hava durumu verisi çekme (OpenWeatherMap API)
 * - Hava durumuna uygun renk paleti önerisi
 * - Hava olaylarına özel efektler (yağmur, kar, fırtına)
 * 
 * Not: Gerçek uygulamada OpenWeatherMap API key gereklidir.
 * Bu sınıf mock veri ile çalışır, production'da API entegrasyonu yapılmalıdır.
 */
class WeatherIntegration(private val context: Context) {
    
    companion object {
        private const val TAG = "AuraWeather"
        
        // Mock API key (production'da gerçek key kullanılmalı)
        private const val API_KEY = "YOUR_OPENWEATHERMAP_API_KEY"
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"
    }
    
    enum class WeatherCondition {
        CLEAR,          // Açık hava
        CLOUDS,         // Bulutlu
        RAIN,           // Yağmur
        DRIZZLE,        // Çiseleyen yağmur
        THUNDERSTORM,   // Fırtına
        SNOW,           // Kar
        MIST,           // Sis
        ATMOSPHERE      // Atmosferik olaylar
    }
    
    data class WeatherData(
        val condition: WeatherCondition,
        val temperature: Float,
        val humidity: Int,
        val windSpeed: Float,
        val description: String,
        val isDaytime: Boolean
    )
    
    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()
    
    private var lastUpdateTime = 0L
    private val UPDATE_INTERVAL_MS = 30 * 60 * 1000L // 30 dakika
    
    /**
     * Hava durumunu güncelle
     * Production'da gerçek API çağrısı yapılacak
     */
    fun updateWeather(location: Location?) {
        val currentTime = System.currentTimeMillis()
        
        // Rate limiting
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL_MS) {
            return
        }
        
        lastUpdateTime = currentTime
        
        if (location == null) {
            Log.w(TAG, "Location null, using mock data")
            _weatherData.value = getMockWeatherData()
            return
        }
        
        // Gerçek API çağrısı (şu an mock ile çalışıyor)
        fetchWeatherFromApi(location.latitude, location.longitude)
    }
    
    /**
     * API'den hava durumu verisi çek
     */
    private fun fetchWeatherFromApi(lat: Double, lon: Double) {
        try {
            // Not: Bu kod production'da coroutine/async ile çalıştırılmalı
            val url = URL("$BASE_URL?lat=$lat&lon=$lon&appid=$API_KEY&units=metric")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonResponse = connection.inputStream.bufferedReader().use { it.readText() }
                parseWeatherResponse(jsonResponse)
            } else {
                Log.w(TAG, "API request failed: $responseCode")
                _weatherData.value = getMockWeatherData()
            }
            
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Weather API error: ${e.message}")
            _weatherData.value = getMockWeatherData()
        }
    }
    
    /**
     * API yanıtını parse et
     */
    private fun parseWeatherResponse(json: String) {
        try {
            val jsonObject = JSONObject(json)
            val weatherArray = jsonObject.getJSONArray("weather")
            val weatherObject = weatherArray.getJSONObject(0)
            val mainObject = jsonObject.getJSONObject("main")
            val windObject = jsonObject.getJSONObject("wind")
            val sysObject = jsonObject.getJSONObject("sys")
            
            val conditionId = weatherObject.getInt("id")
            val condition = mapWeatherCode(conditionId)
            val temperature = mainObject.getDouble("temp").toFloat()
            val humidity = mainObject.getInt("humidity")
            val windSpeed = windObject.getDouble("speed").toFloat()
            val description = weatherObject.getString("description")
            val isDaytime = sysObject.getInt("dt") > 0 // Basit kontrol
            
            _weatherData.value = WeatherData(
                condition = condition,
                temperature = temperature,
                humidity = humidity,
                windSpeed = windSpeed,
                description = description,
                isDaytime = isDaytime
            )
            
            Log.d(TAG, "Weather updated: $condition, ${temperature}°C")
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error: ${e.message}")
            _weatherData.value = getMockWeatherData()
        }
    }
    
    /**
     * Weather code mapping
     */
    private fun mapWeatherCode(code: Int): WeatherCondition {
        return when {
            code >= 200 && code < 300 -> WeatherCondition.THUNDERSTORM
            code >= 300 && code < 400 -> WeatherCondition.DRIZZLE
            code >= 500 && code < 600 -> WeatherCondition.RAIN
            code >= 600 && code < 700 -> WeatherCondition.SNOW
            code >= 700 && code < 800 -> WeatherCondition.ATMOSPHERE
            code == 800 -> WeatherCondition.CLEAR
            code > 800 -> WeatherCondition.CLOUDS
            else -> WeatherCondition.CLEAR
        }
    }
    
    /**
     * Mock hava durumu verisi (test için)
     */
    private fun getMockWeatherData(): WeatherData {
        // Gerçek uygulamada burası API'den gelecek
        return WeatherData(
            condition = WeatherCondition.CLEAR,
            temperature = 22f,
            humidity = 65,
            windSpeed = 3.5f,
            description = "Clear sky",
            isDaytime = true
        )
    }
    
    /**
     * Hava durumuna göre renk paleti öner
     */
    fun recommendPalette(): ColorPalette {
        val weather = _weatherData.value ?: return ColorPalette.OCEAN
        
        return when (weather.condition) {
            WeatherCondition.CLEAR -> if (weather.isDaytime) ColorPalette.OCEAN else ColorPalette.COSMIC
            WeatherCondition.CLOUDS -> ColorPalette.MONOCHROME
            WeatherCondition.RAIN, WeatherCondition.DRIZZLE -> ColorPalette.OCEAN
            WeatherCondition.THUNDERSTORM -> ColorPalette.NEON
            WeatherCondition.SNOW -> ColorPalette.MONOCHROME
            WeatherCondition.MIST, WeatherCondition.ATMOSPHERE -> ColorPalette.FOREST
        }
    }
    
    /**
     * Hava durumuna özel efekt parametreleri
     */
    fun getEffectParameters(): Map<String, Float> {
        val weather = _weatherData.value ?: return emptyMap()
        
        return when (weather.condition) {
            WeatherCondition.RAIN, WeatherCondition.DRIZZLE -> mapOf(
                "rainIntensity" to (weather.humidity / 100f),
                "windFactor" to (weather.windSpeed / 20f)
            )
            WeatherCondition.SNOW -> mapOf(
                "snowIntensity" to (weather.temperature.coerceAtMost(0f) / -10f),
                "windFactor" to (weather.windSpeed / 20f)
            )
            WeatherCondition.THUNDERSTORM -> mapOf(
                "lightningFrequency" to 0.3f,
                "darknessFactor" to 0.8f
            )
            else -> emptyMap()
        }
    }
    
    /**
     * Manuel olarak hava durumu ayarla (test için)
     */
    fun setMockWeather(condition: WeatherCondition) {
        _weatherData.value = _weatherData.value?.copy(condition = condition)
            ?: WeatherData(
                condition = condition,
                temperature = 20f,
                humidity = 50,
                windSpeed = 5f,
                description = condition.name,
                isDaytime = true
            )
    }
}
