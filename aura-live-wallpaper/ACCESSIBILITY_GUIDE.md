# AURA Erişilebilirlik Kılavuzu

## Erişilebilirlik Özellikleri

AURA Live Wallpaper, tüm kullanıcılar için erişilebilir bir deneyim sunmak üzere tasarlanmıştır.

### 1. Epilepsi Güvenliği 🛡️

**Nedir?**
Hızlı renk değişimleri ve parlak flaş efektlerine duyarlı kullanıcılar için güvenli mod.

**Özellikler:**
- Maksimum parlaklık %60'a sınırlanır
- Ani renk geçişleri yumuşatılır
- Yüksek frekanslı titreşimler devre dışı bırakılır

**Nasıl Aktif Edilir?**
```kotlin
AccessibilityManager.setEpilepsySafeMode(true)
```

Veya Ayarlar ekranından "Epilepsi Güvenli Mod" seçeneğini açın.

### 2. Yüksek Kontrast Modu 🔆

**Nedir?**
Görme bozukluğu olan kullanıcılar için renkler arasında daha belirgin farklar oluşturur.

**Özellikler:**
- Renk paletleri otomatik olarak yüksek kontrastlı versiyonlara dönüştürülür
- Kenarlıklar ve şekiller daha belirgin hale gelir

**Nasıl Aktif Edilir?**
Sistem ayarlarından "Yüksek Kontrast" modunu açın veya AURA ayarlarından manuel olarak aktif edin.

### 3. Azaltılmış Hareket Modu 🐌

**Nedir?**
Hareket hassasiyeti olan veya baş dönmesi yaşayan kullanıcılar için animasyonları yavaşlatır.

**Özellikler:**
- Animasyon hızı %50 azaltılır
- Karmaşık fraktal dönüşümler basitleştirilir
- Sallantı ve dalga efektleri minimize edilir

**Nasıl Aktif Edilir?**
Sistem ayarlarından "Azaltılmış Hareket" modunu açın.

### 4. TalkBack / Screen Reader Desteği 🗣️

**Nedir?**
Görme engelli kullanıcılar için ekran okuyucu uyumluluğu.

**Özellikler:**
- Tüm butonlar ve kontroller açıklanır
- Widget elementleri erişilebilir
- Ayarlar ekranı tam uyumlu

**Desteklenen Okuyucular:**
- Android TalkBack
- Samsung Voice Assistant
- Diğer üçüncü parti ekran okuyucular

## Sistem Tercihlerini Algılama

AURA, sistem erişilebilirlik ayarlarını otomatik olarak algılar:

```kotlin
// AccessibilityManager.kt içinde
fun configureFromSystemPrefs(
    androidReducedMotion: Boolean,
    androidHighContrast: Boolean
) {
    isReducedMotionMode = androidReducedMotion
    isHighContrastMode = androidHighContrast
    // ... otomatik yapılandırma
}
```

## Manuel Ayarlar

Kullanıcılar AURA ayarlar ekranından manuel olarak da erişilebilirlik özelliklerini yönetebilir:

| Ayar | Varsayılan | Açıklama |
|------|-----------|----------|
| Epilepsi Güvenli Mod | ❌ Kapalı | Parlaklık ve hızlı değişimleri kısıtlar |
| Yüksek Kontrast | ❌ Kapalı | Renk kontrastını artırır |
| Azaltılmış Hareket | ❌ Kapalı | Animasyonları yavaşlatır |
| Maksimum Parlaklık | 100% | Epilepsi modunda %60'a düşer |
| Animasyon Hızı | 1.0x | Azaltılmış hareket modunda 0.5x |

## Geliştirici Notları

### Erişilebilirlik Kontrol Noktaları

Render döngüsünde erişilebilirlik ayarları kontrol edilir:

```kotlin
// FractalRenderer.kt içinde
if (AccessibilityManager.isEpilepsySafeMode) {
    brightness = min(brightness, 0.6f)
}

if (AccessibilityManager.isReducedMotionMode) {
    animationSpeed *= 0.5f
}
```

### Test Senaryoları

1. **Epilepsi Modu Testi:**
   - Modu aktif edin
   - Parlaklığın %60'ı aşmadığını doğrulayın
   - Hızlı renk değişimi olmadığını kontrol edin

2. **Yüksek Kontrast Testi:**
   - Modu aktif edin
   - Renklerin daha belirgin olduğunu doğrulayın
   - UI elementlerinin net göründüğünü kontrol edin

3. **Azaltılmış Hareket Testi:**
   - Modu aktif edin
   - Animasyonların yavaşladığını doğrulayın
   - Baş dönmesi tetikleyici efektlerin olmadığını kontrol edin

4. **TalkBack Testi:**
   - TalkBack'i aktif edin
   - Tüm butonların açıklandığını doğrulayın
   - Widget elementlerinin okunabilir olduğunu kontrol edin

## Standartlar ve Uyumluluk

AURA aşağıdaki standartlara uygundur:

- ✅ WCAG 2.1 Seviye AA
- ✅ Android Erişilebilirlik Yönergeleri
- ✅ Section 508 (ABD Federal Standardı)

## Geri Bildirim

Erişilebilirlik ile ilgili sorunlar veya öneriler için:
- GitHub Issues üzerinden bildirin
- E-posta: accessibility@aurawallpaper.com

---

**Not:** Erişilebilirlik özellikleri sürekli geliştirilmektedir. Kullanıcı geri bildirimleri bizim için çok önemlidir!
