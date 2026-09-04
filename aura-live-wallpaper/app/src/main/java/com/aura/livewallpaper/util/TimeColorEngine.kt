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
        
        return ColorPalette(
            name = "${current.name}_to_${nextPalette.name}",
            primary = interpolateColor(current.primary, nextPalette.primary, progress),
            secondary = interpolateColor(current.secondary, nextPalette.secondary, progress),
            accent = interpolateColor(current.accent, nextPalette.accent, progress),
            background = interpolateColor(current.background, nextPalette.background, progress)
        )
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
    fun isAutoPaletteEnabled(): Boolean = true // Ayarlardan kontrol edilebilir
}

/**
 * Önceden tanımlanmış renk paletleri
 */
enum class ColorPalette(
    val name: String,
    val primary: Long,    // ARGB format: 0xAARRGGBB
    val secondary: Long,
    val accent: Long,
    val background: Long
) {
    // Sabah: Ilık, uyanış tonları
    SUNRISE(
        name = "Sunrise",
        primary = 0xFFFFD700L,    // Altın sarısı
        secondary = 0xFFFF8C42L,  // Turuncu
        accent = 0xFFFFB6C1L,     // Açık pembe
        background = 0xFF1A1A2EL  // Koyu lacivert
    ),
    
    // Öğle: Canlı, enerjik tonlar
    OCEAN(
        name = "Ocean",
        primary = 0xFF00B4DBL,    // Parlak mavi
        secondary = 0xFF0083B0L,  // Okyanus mavisi
        accent = 0xFF7BDCB5L,     // Nane yeşili
        background = 0xFF0F0F1AL  // Çok koyu mavi
    ),
    
    // Akşam: Sıcak, huzurlu tonlar
    SUNSET(
        name = "Sunset",
        primary = 0xFFFF6B6BL,    // Mercan kırmızısı
        secondary = 0xFFFF8E72L,  // Şeftali
        accent = 0xFFC792E5L,     // Lavanta
        background = 0xFF1A0F1AL  // Koyu mor
    ),
    
    // Gece: Soğuk, sakin tonlar
    COSMIC(
        name = "Cosmic",
        primary = 0xFF7B68AAL,    // Medium slate blue
        secondary = 0xFF483D8BL,  // Dark slate blue
        accent = 0xFF9370DBL,     // Medium purple
        background = 0xFF0A0A12L  // Neredeyse siyah
    ),
    
    // Ekstra paletler
    FOREST(
        name = "Forest",
        primary = 0xFF228B22L,    // Forest green
        secondary = 0xFF32CD32L,  // Lime green
        accent = 0xFF90EE90L,     // Light green
        background = 0xFF0F1F0FL  // Çok koyu yeşil
    ),
    
    FIRE(
        name = "Fire",
        primary = 0xFFFF4500L,    // Orange red
        secondary = 0xFFFFD700L,  // Gold
        accent = 0xFFFF6347L,     // Tomato
        background = 0xFF1F0F0FL  // Koyu kırmızı-siyah
    ),
    
    MONOCHROME(
        name = "Monochrome",
        primary = 0xFFCCCCCCCL,   // Açık gri
        secondary = 0xFF888888L,  // Orta gri
        accent = 0xFF444444L,     // Koyu gri
        background = 0xFF111111L  // Neredeyse siyah
    ),
    
    NEON(
        name = "Neon",
        primary = 0xFF00FFFFL,    // Cyan
        secondary = 0xFFFF00FFL,  // Magenta
        accent = 0xFF00FF00L,     // Lime
        background = 0xFF0A0A0AL  // Siyah
    )
}
