package com.aura.livewallpaper.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.aura.livewallpaper.util.AuraPreferences

/**
 * Accessibility Manager - Erişilebilirlik özelliklerini yönetir
 * 
 * Özellikler:
 * - Epilepsi güvenliği (flashing lights kontrolü)
 * - Yüksek kontrast modu
 * - TalkBack desteği
 * - Motion azaltma
 */
class AccessibilityManager(private val context: Context) {
    
    companion object {
        // Epilepsi güvenliği için maksimum flash frekansı (Hz)
        private const val MAX_SAFE_FLASH_FREQUENCY = 3.0f
        
        // Minimum kontrast oranı (WCAG 2.1 AA standardı)
        private const val MIN_CONTRAST_RATIO = 4.5f
    }
    
    private val preferences = AuraPreferences(context)
    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    
    /**
     * Cihazda erişilebilirlik özellikleri aktif mi?
     */
    fun isAccessibilityEnabled(): Boolean {
        return accessibilityManager.isEnabled || 
               accessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * TalkBack (screen reader) aktif mi?
     */
    fun isTalkBackEnabled(): Boolean {
        return accessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * Hareket azaltma tercih edilmiş mi?
     */
    fun isReduceMotionEnabled(): Boolean {
        // Android 10+ reduce motion preference
        return preferences.accessibilityMode
    }
    
    /**
     * Epilepsi güvenli modu aktif et
     * - Flash efektlerini devre dışı bırak
     * - Ani renk değişimlerini yumuşat
     * - Maksimum frekansı sınırla
     */
    fun enableEpilepsySafeMode() {
        preferences.accessibilityMode = true
        // Beat sync efektlerini kapat
        // Rapid color transitions'ları yavaşlat
    }
    
    /**
     * Yüksek kontrast modunu aktif et
     * - Renk paletini yüksek kontrastlı olanla değiştir
     * - Glow efektlerini azalt
     * - Kenar çizgilerini belirginleştir
     */
    fun enableHighContrastMode() {
        preferences.selectedPalette = "Monochrome"
        // Render'da contrast boost uygula
    }
    
    /**
     * Güvenli flash frekansını kontrol et
     * @param frequency Flash frekansı (Hz)
     * @return Güvenli ise true
     */
    fun isFlashFrequencySafe(frequency: Float): Boolean {
        return frequency <= MAX_SAFE_FLASH_FREQUENCY
    }
    
    /**
     * Kontrast oranını hesapla (basit versiyon)
     * @param luminance1 İlk rengin parlaklığı (0-1)
     * @param luminance2 İkinci rengin parlaklığı (0-1)
     * @return Kontrast oranı
     */
    fun calculateContrastRatio(luminance1: Float, luminance2: Float): Float {
        val lighter = maxOf(luminance1, luminance2)
        val darker = minOf(luminance1, luminance2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
    
    /**
     * WCAG uyumlu kontrast kontrolü
     * @param luminance1 İlk rengin parlaklığı
     * @param luminance2 İkinci rengin parlaklığı
     * @return WCAG AA standardına uygunsa true
     */
    fun isWCAGCompliant(luminance1: Float, luminance2: Float): Boolean {
        return calculateContrastRatio(luminance1, luminance2) >= MIN_CONTRAST_RATIO
    }
    
    /**
     * Erişilebilirlik ayarlarını render parametrelerine dönüştür
     */
    fun getRenderParameters(): AccessibilityRenderParams {
        val reduceMotion = isReduceMotionEnabled()
        val talkBackEnabled = isTalkBackEnabled()
        val highContrast = preferences.selectedPalette == "Monochrome"
        
        return AccessibilityRenderParams(
            reduceMotion = reduceMotion,
            disableFlashing = reduceMotion || preferences.accessibilityMode,
            slowTransitions = reduceMotion,
            highContrast = highContrast,
            simplifyEffects = reduceMotion,
            maxFPS = if (reduceMotion) 30 else 60
        )
    }
    
    /**
     * Render parametreleri data class
     */
    data class AccessibilityRenderParams(
        val reduceMotion: Boolean,
        val disableFlashing: Boolean,
        val slowTransitions: Boolean,
        val highContrast: Boolean,
        val simplifyEffects: Boolean,
        val maxFPS: Int
    )
    
    /**
     * Erişilebilirlik durumu özeti
     */
    fun getAccessibilitySummary(): Map<String, Any> {
        return mapOf(
            "accessibilityEnabled" to isAccessibilityEnabled(),
            "talkBackEnabled" to isTalkBackEnabled(),
            "reduceMotionPreferred" to isReduceMotionEnabled(),
            "epilepsySafeMode" to preferences.accessibilityMode,
            "highContrastActive" to (preferences.selectedPalette == "Monochrome"),
            "currentPalette" to preferences.selectedPalette,
            "maxFPS" to getRenderParameters().maxFPS
        )
    }
}
