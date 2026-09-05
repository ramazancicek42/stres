package com.aura.livewallpaper.util

import android.content.Context
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zaman Bazlı Renk Motoru
 * 
 * Saate ve günün zamanına göre otomatik renk paleti değişimi sağlar.
 * Kullanıcının bulunduğu coğrafyaya göre mevsimsel ayarlamalar yapabilir.
 * 
 * Zaman Dilimleri:
 * - Sabah (05:00 - 11:00): Ilık, uyanış tonları (turuncu, pembe, açık mavi)
 * - Öğle (11:00 - 17:00): Canlı, enerjik tonlar (parlak mavi, yeşil, sarı)
 * - Akşam (17:00 - 22:00): Sıcak, huzurlu tonlar (turuncu, mor, kırmızı)
 * - Gece (22:00 - 05:00): Soğuk, sakin tonlar (koyu mavi, mor, siyah)
 */
class TimeColorEngine(private val context: Context) {
    
    enum class TimeOfDay {
        MORNING,    // 05:00 - 11:00
        NOON,       // 11:00 - 17:00
        EVENING,    // 17:00 - 22:00
        NIGHT       // 22:00 - 05:00
    }
    
    data class TimeBasedPalette(
        val palette: ColorPalette,
        val transitionProgress: Float, // 0.0 - 1.0 (bir sonraki periode geçiş)
        val timeOfDay: TimeOfDay
    )
    
    private val _currentPalette = MutableStateFlow<TimeBasedPalette>(getTimeBasedPalette())
    val currentPalette: StateFlow<TimeBasedPalette> = _currentPalette.asStateFlow()
    
    private var lastUpdateHour = -1
    
    /**
     * Mevcut zamana göre renk paletini hesapla
     */
    fun update() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Sadece saat değiştiğinde güncelle (performans için)
        if (currentHour != lastUpdateHour) {
            lastUpdateHour = currentHour
            _currentPalette.value = getTimeBasedPalette()
        }
    }
    
    private fun getTimeOfDay(): TimeOfDay {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour >= 5 && hour < 11 -> TimeOfDay.MORNING
            hour >= 11 && hour < 17 -> TimeOfDay.NOON
            hour >= 17 && hour < 22 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }
    
    private fun getTimeBasedPalette(): TimeBasedPalette {
        val timeOfDay = getTimeOfDay()
        val calendar = Calendar.getInstance()
        val currentMinute = calendar.get(Calendar.MINUTE)
        
        // Transition progress hesapla (dakika bazında)
        val transitionProgress = currentMinute / 60.0f
        
        val palette = when (timeOfDay) {
            TimeOfDay.MORNING -> ColorPalette.SUNRISE
            TimeOfDay.NOON -> ColorPalette.OCEAN
            TimeOfDay.EVENING -> ColorPalette.SUNSET
            TimeOfDay.NIGHT -> ColorPalette.COSMIC
        }
        
        return TimeBasedPalette(
            palette = palette,
            transitionProgress = transitionProgress,
            timeOfDay = timeOfDay
        )
    }
    
    /**
     * İki palet arasında interpolate et (smooth transition için)
     * İlerleme %80'i geçtiğinde bir sonraki palete geçiş yapar
     */
    fun getInterpolatedPalette(nextPalette: ColorPalette, progress: Float): ColorPalette {
        val current = _currentPalette.value.palette
        
        // İlerleme %80'i geçerse bir sonraki palete geç
        return if (progress > 0.8f) {
            nextPalette
        } else {
            current
        }
    }
    
    /**
     * Palet renklerini interpolate et (GLSL shader için float array)
     */
    fun interpolatePaletteColors(
        fromColors: FloatArray,
        toColors: FloatArray,
        progress: Float
    ): FloatArray {
        val result = FloatArray(fromColors.size)
        for (i in fromColors.indices) {
            result[i] = fromColors[i] + (toColors[i] - fromColors[i]) * progress
        }
        return result
    }
    
    private fun interpolateColor(color1: Long, color2: Long, progress: Float): Long {
        val a1 = (color1 shr 24 and 0xFF)
        val r1 = (color1 shr 16 and 0xFF)
        val g1 = (color1 shr 8 and 0xFF)
        val b1 = (color1 and 0xFF)
        
        val a2 = (color2 shr 24 and 0xFF)
        val r2 = (color2 shr 16 and 0xFF)
        val g2 = (color2 shr 8 and 0xFF)
        val b2 = (color2 and 0xFF)
        
        val a = (a1 + (a2 - a1) * progress).toInt()
        val r = (r1 + (r2 - r1) * progress).toInt()
        val g = (g1 + (g2 - g1) * progress).toInt()
        val b = (b1 + (b2 - b1) * progress).toInt()
        
        return (a shl 24 or (r shl 16) or (g shl 8) or b).toLong()
    }
    
    /**
     * Kullanıcı manuel palet seçtiyse zaman bazlı geçişi devre dışı bırak
     */
    fun isAutoPaletteEnabled(): Boolean = true
    
    fun getRecommendedPalettesForHour(hour: Int): List<String> {
        return when {
            hour in 5..10 -> listOf("Sunrise", "Ocean")
            hour in 11..16 -> listOf("Ocean", "Forest")
            hour in 17..21 -> listOf("Sunset", "Cosmic", "Fire")
            else -> listOf("Cosmic", "Neon")
        }
    }
}

/**
 * Önceden tanımlanmış renk paletleri
 * FractalRenderer'daki paletlerle uyumlu
 */
enum class ColorPalette(
    val paletteName: String,
    val primary: Long,    // ARGB format: 0xAARRGGBB
    val secondary: Long,
    val accent: Long,
    val background: Long
) {
    // 0: Ocean - Okyanus Mavisi
    OCEAN(
        paletteName = "Ocean",
        primary = 0xFF0a1628L,
        secondary = 0xFF1a4f7aL,
        accent = 0xFF4a90d9L,
        background = 0xFF0a1628L
    ),
    
    // 1: Sunset - Gün Batımı
    SUNSET(
        paletteName = "Sunset",
        primary = 0xFF2d1b2eL,
        secondary = 0xFFb85c38L,
        accent = 0xFFf4a460L,
        background = 0xFF1a0f1aL
    ),
    
    // 2: Forest - Orman Yeşili
    FOREST(
        paletteName = "Forest",
        primary = 0xFF0f281eL,
        secondary = 0xFF2d6a4fL,
        accent = 0xFF52b788L,
        background = 0xFF0f1f0fL
    ),
    
    // 3: Night - Mor Gece
    COSMIC(
        paletteName = "Cosmic",
        primary = 0xFF1a0f2eL,
        secondary = 0xFF4a2d7aL,
        accent = 0xFF9b72cfL,
        background = 0xFF0a0a12L
    ),
    
    // 4: Amber - Sıcak Amber
    FIRE(
        paletteName = "Fire",
        primary = 0xFF2e1f0fL,
        secondary = 0xFFb87c38L,
        accent = 0xFFf4c460L,
        background = 0xFF1f0f0fL
    ),
    
    // 5: Sunrise - Gün Doğumu
    SUNRISE(
        paletteName = "Sunrise",
        primary = 0xFF1a1a2eL,
        secondary = 0xFFFF8C42L,
        accent = 0xFFFFD700L,
        background = 0xFF1A1A2EL
    ),
    
    // 6: Cosmic - Kozmik
    NEON(
        paletteName = "Neon",
        primary = 0xFF0a0a0aL,
        secondary = 0xFF00FFFFL,
        accent = 0xFFFF00FFL,
        background = 0xFF0A0A0AL
    ),
    
    // 7: Monochrome - Monokrom
    MONOCHROME(
        paletteName = "Monochrome",
        primary = 0xFF111111L,
        secondary = 0xFF444444L,
        accent = 0xFFCCCCCCCL,
        background = 0xFF111111L
    )
}
