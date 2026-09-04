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
     */
    fun getInterpolatedPalette(nextPalette: ColorPalette, progress: Float): ColorPalette {
        val current = _currentPalette.value.palette
        
        return ColorPalette.valueOf(current.paletteName)
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
 */
enum class ColorPalette(
    val paletteName: String,
    val primary: Long,    // ARGB format: 0xAARRGGBB
    val secondary: Long,
    val accent: Long,
    val background: Long
) {
    // Sabah: Ilık, uyanış tonları
    SUNRISE(
        paletteName = "Sunrise",
        primary = 0xFFFFD700L,
        secondary = 0xFFFF8C42L,
        accent = 0xFFFFB6C1L,
        background = 0xFF1A1A2EL
    ),
    
    OCEAN(
        paletteName = "Ocean",
        primary = 0xFF00B4DBL,
        secondary = 0xFF0083B0L,
        accent = 0xFF7BDCB5L,
        background = 0xFF0F0F1AL
    ),
    
    SUNSET(
        paletteName = "Sunset",
        primary = 0xFFFF6B6BL,
        secondary = 0xFFFF8E72L,
        accent = 0xFFC792E5L,
        background = 0xFF1A0F1AL
    ),
    
    COSMIC(
        paletteName = "Cosmic",
        primary = 0xFF7B68AAL,
        secondary = 0xFF483D8BL,
        accent = 0xFF9370DBL,
        background = 0xFF0A0A12L
    ),
    
    FOREST(
        paletteName = "Forest",
        primary = 0xFF228B22L,
        secondary = 0xFF32CD32L,
        accent = 0xFF90EE90L,
        background = 0xFF0F1F0FL
    ),
    
    FIRE(
        paletteName = "Fire",
        primary = 0xFFFF4500L,
        secondary = 0xFFFFD700L,
        accent = 0xFFFF6347L,
        background = 0xFF1F0F0FL
    ),
    
    MONOCHROME(
        paletteName = "Monochrome",
        primary = 0xFFCCCCCCCL,
        secondary = 0xFF888888L,
        accent = 0xFF444444L,
        background = 0xFF111111L
    ),
    
    NEON(
        paletteName = "Neon",
        primary = 0xFF00FFFFL,
        secondary = 0xFFFF00FFL,
        accent = 0xFF00FF00L,
        background = 0xFF0A0A0AL
    )
}
