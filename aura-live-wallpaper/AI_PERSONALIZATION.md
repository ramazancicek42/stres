# AURA AI Kişiselleştirme Motoru

## Genel Bakış

AURA, kullanıcı alışkanlıklarını öğrenen ve otomatik öneriler sunan bir yapay zeka motoru ile gelir.

## Özellikler

### 1. Palet Tercihi Öğrenimi 🎨

**Nasıl Çalışır:**
- Her palet değişimi kaydedilir
- Günün saatine göre kullanım pattern'leri analiz edilir
- En çok kullanılan paletler tespit edilir

**Örnek:**
```
Kullanıcı A:
- Sabah (06-12): Ocean paleti (%70)
- Öğle (12-18): Forest paleti (%50)
- Akşam (18-24): Sunset paleti (%80)
- Gece (00-06): Cosmic paleti (%90)
```

### 2. Otomatik Palet Önerisi ⏰

**Zaman Bazlı Öneriler:**
- Her saat dilimi için en popüler palet önerilir
- Yakın saatlerin verileri de dikkate alınır (±2 saat)
- Yeterli veri yoksa genel favoriler önerilir

**Kullanım:**
```kotlin
val engine = PersonalizationEngine(context)
val recommendedPalette = engine.recommendPaletteForHour(14) // Öğle için öneri
```

### 3. Pil Bazlı FPS Önerisi 🔋

**Akıllı FPS Ayarı:**
- Şarj durumuna göre otomatik FPS önerisi
- Pil seviyesine göre optimizasyon
- Kullanıcı deneyimini koruyarak pil tasarrufu

| Pil Seviyesi | Şarjda | Önerilen FPS |
|--------------|--------|--------------|
| %0-20        | Hayır  | 15 FPS       |
| %20-50       | Hayır  | 20 FPS       |
| %50-80       | Hayır  | 30 FPS       |
| %80-100      | Hayır  | 60 FPS       |
| Herhangi     | Evet   | 60 FPS       |

### 4. Ses Hassasiyeti Önerisi 🔊

**Zamana Göre Otomatik Ayar:**
- Gece saatlerinde daha düşük hassasiyet (0.3)
- Gündüz normal hassasiyet (0.7)
- Kullanıcının ortamına göre uyum sağlar

### 5. Kişiselleştirilmiş Raporlama 📊

**Rapor İçeriği:**
- En sevilen paletler (ilk 5)
- En aktif kullanım saati
- Önerilen FPS ayarı
- Anlık saat için palet önerisi

**Örnek Çıktı:**
```
=== AURA Kişiselleştirme Raporu ===
En sevdiğiniz paletler: 3, 7, 1, 5, 2
En aktif saat: 21:00
Önerilen FPS: 30
Şu anki saat için önerilen palet: 7
```

## Teknik Detaylar

### Veri Saklama

```kotlin
// SharedPreferences içinde
- palette_history: Son 100 palet seçimi
- time_palette_map: Saat -> (Palet -> Kullanım sayısı)
- fps_history: FPS tercih geçmişi
```

### Gizlilik

- ✅ Tüm veriler cihazda saklanır
- ✅ Buluta gönderilmez
- ✅ Üçüncü taraflarla paylaşılmaz
- ✅ Kullanıcı istediğinde sıfırlanabilir

### Performans

- Minimum bellek kullanımı (< 100 KB)
- Hesaplamalar hafif ve hızlı
- Arka planda çalışmaz, sadece gerektiğinde

## Kullanım Örnekleri

### Temel Kullanım

```kotlin
// Activity veya Service içinde
val personalizationEngine = PersonalizationEngine(context)

// Palet kullanımını kaydet
personalizationEngine.recordPaletteUsage(paletteIndex = 3, hourOfDay = 14)

// Öneri al
val favoritePalettes = personalizationEngine.getFavoritePalettes(limit = 3)
val recommendedForNow = personalizationEngine.recommendPaletteForHour(14)
val recommendedFPS = personalizationEngine.recommendFPSSetting(
    batteryLevel = 45,
    isCharging = false
)
```

### Otomatik Öneri Sistemi

```kotlin
// Wallpaper servisinde
class AuraWallpaperService : WallpaperService() {
    
    private lateinit var personalizationEngine: PersonalizationEngine
    
    override fun onCreateEngine(): Engine {
        personalizationEngine = PersonalizationEngine(this)
        return AuraEngine()
    }
    
    inner class AuraEngine : Engine() {
        
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            
            if (visible) {
                applyPersonalizedSettings()
            }
        }
        
        private fun applyPersonalizedSettings() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            
            // Palet önerisi
            personalizationEngine.recommendPaletteForHour(hour)?.let { palette ->
                applyPalette(palette)
            }
            
            // FPS önerisi
            val batteryLevel = getBatteryLevel()
            val isCharging = isDeviceCharging()
            val recommendedFPS = personalizationEngine.recommendFPSSetting(
                batteryLevel, isCharging
            )
            setTargetFPS(recommendedFPS)
            
            // Ses hassasiyeti önerisi
            val sensitivity = personalizationEngine.recommendAudioSensitivity(hour)
            setAudioSensitivity(sensitivity)
        }
    }
}
```

### Rapor Oluşturma

```kotlin
// Kullanıcıya göstermek için
val report = personalizationEngine.generatePersonalizationReport()
println(report)

// Veya UI'da göster
textView.text = report
```

### Verileri Sıfırlama

```kotlin
// Tüm öğrenilmiş verileri sil
personalizationEngine.resetAllData()
```

## Gelecek Özellikler (Roadmap)

### Kısa Vadeli (v1.1)
- [ ] Haftanın gününe göre öneriler
- [ ] Hava durumu entegrasyonu ile palet önerisi
- [ ] Kullanıcı geri bildirimi ile öğrenme

### Orta Vadeli (v1.2)
- [ ] Benzer kullanıcılarla anonim karşılaştırma
- [ ] Mevsimsel pattern algılama
- [ ] Özel etkinlik bazlı öneriler (doğumgünü, tatil, vb.)

### Uzun Vadeli (v2.0)
- [ ] On-device machine learning modeli
- [ ] Derin öğrenme ile pattern recognition
- [ ] Predictive palette switching (öngörülü palet değişimi)

## SSS

**S: Veriler ne kadar süre saklanır?**
C: Son 100 palet seçimi ve tüm saatlik kullanım verileri süresiz saklanır. Kullanıcı isterse sıfırlayabilir.

**S: Pil tüketimini etkiler mi?**
C: Hayır. Hesaplamalar çok hafiftir ve sadece kullanıcı etkileşimi olduğunda çalışır.

**S: İnternet erişimi gerektirir mi?**
C: Hayır. Tüm işlemler cihaz içinde yapılır.

**S: Verilerim güvende mi?**
C: Evet. Veriler sadece cihazınızda saklanır, hiçbir şekilde dışarı gönderilmez.

---

**Not:** AI kişiselleştirme motoru opsiyoneldir. Kullanıcılar istedikleri zaman devre dışı bırakabilir veya verileri silebilir.
