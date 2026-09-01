# AURA — Sensör-Duyarlı Fraktal Meditasyon Duvar Kağıdı
## Profesyonel Ürün & Teknik Spesifikasyon Dokümanı

---

## 1. Konsept Özeti

**Ne yapıyor:** Telefonun ortam ışığı ve ortam sesi verilerini gerçek zamanlı analiz ederek, hem görsel (canlı duvar kağıdı üzerinde prosedürel fraktal animasyon) hem işitsel (üretken ambiyans müzik) çıktı üreten, kullanıcının dokunuşlarına tepki veren bir Android Live Wallpaper uygulaması.

**Fark yaratan nokta:** Piyasadaki fraktal duvar kağıtları statik veya sadece dokunuşa tepki veriyor. Buradaki fark: **çevresel veri + biyolojik ritim + prosedürel ses senteziyle** gerçek zamanlı, tekrar etmeyen bir "yaşayan" deneyim yaratmak — meditasyon/stres azaltma odaklı konumlandırma ile.

---

## 2. Kritik Mimari Karar: Kamera Yerine Işık Sensörü

Orijinal fikirde "kameradan gelen ışık seviyesi" var, ama bunu **canlı duvar kağıdı bağlamında böyle yapmamalısın.** Nedenleri:

- Android'de bir `WallpaperService` arka planda sürekli çalışır; kamerayı sürekli açık tutmak **pil tüketimini felakete çevirir** ve Play Store politikaları arka planda kamera kullanımını sıkı denetler (gizlilik nedeniyle reddedilme riski yüksek).
- Kullanıcı ana ekrana her baktığında kamera LED'i/aktivasyonu tetiklenirse **güven sorunu** yaratır (insanlar "duvar kağıdı beni izliyor mu?" diye endişelenir).
- Google Play, arka planda kamera erişimini foreground service + açık bildirim olmadan kısıtlıyor.

**Öneri:** `SensorManager.TYPE_LIGHT` (ortam ışık sensörü) kullan. Bu sensör zaten bu iş için var, sıfır pil maliyeti var (donanımsal, düşük güç), kamera izni gerektirmiyor ve gizlilik endişesi doğurmuyor. Kullanıcıya "kamerandan ışık okuyoruz" yerine "ortam ışığına duyarlı" demek hem daha doğru hem daha güven verici.

Kamerayı sadece **isteğe bağlı bir "gelişmiş mod"** olarak, kullanıcı açıkça etkinleştirirse ve sadece ana ekran aktifken kısa aralıklarla (örneğin renk sıcaklığı analizi için tek kare) kullan.

---

## 3. Sistem Mimarisi

```
┌─────────────────────────────────────────────────┐
│              AuraWallpaperService                 │
│         (WallpaperService.Engine)                 │
├─────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐             │
│  │ SensorFusion  │  │ AudioAnalyzer│             │
│  │ - Light (LUX) │  │ - Mic RMS    │             │
│  │ - Touch coords│  │ - FFT/Onset  │             │
│  │ - Accelerometer│  │  detection   │             │
│  │   (opsiyonel) │  └──────┬───────┘             │
│  └───────┬──────┘         │                      │
│          │                 │                      │
│          ▼                 ▼                      │
│  ┌─────────────────────────────────┐              │
│  │      StateVector (Kotlin)        │              │
│  │  lightLevel, audioEnergy,        │              │
│  │  audioTempo, touchX, touchY,     │              │
│  │  touchIntensity, timeOfDay       │              │
│  └───────┬─────────────────┬───────┘              │
│          │                 │                      │
│          ▼                 ▼                      │
│  ┌───────────────┐  ┌──────────────────┐          │
│  │ FractalRenderer│  │ GenerativeAudio  │          │
│  │  (OpenGL ES 3.0│  │  Engine          │          │
│  │  GLSL shader)  │  │  (Oboe/AAudio +  │          │
│  │                │  │  procedural synth)│          │
│  └───────────────┘  └──────────────────┘          │
└─────────────────────────────────────────────────┘
```

### 3.1 Veri Girişleri (Sensor Layer)

| Kaynak | API | Amaç | Pil Maliyeti |
|---|---|---|---|
| Ortam ışık sensörü | `SensorManager.TYPE_LIGHT` | Renk paleti, parlaklık, "gündüz/gece" modu | Çok düşük |
| Mikrofon | `AudioRecord` (16kHz, mono) | Ortam sesi enerjisi + ritim tespiti | Orta (sadece ekran açıkken aktif) |
| Dokunma | `Engine.onTouchEvent` | Fraktal odak noktası, dalga efekti | Yok |
| İvmeölçer (opsiyonel) | `TYPE_ACCELEROMETER` | Telefon hareketine tepki veren parallax | Düşük |
| Saat | `Calendar` | Gündüz/gece renk temaları | Yok |

**Önemli kısıtlama:** Mikrofon dinleme SADECE kullanıcı ana ekranı görüntülerken (`isVisible == true`) aktif olmalı; arka plana geçince veya ekran kapanınca `AudioRecord.stop()` çağrılmalı. Bu hem pil hem de RECORD_AUDIO izninin arka planda kötüye kullanılmaması için kritik (Android 10+ arka plan mikrofon kısıtlamaları).

### 3.2 Ses Analizi (Audio Analyzer)

- Ham PCM veriden **RMS enerjisi** (ses şiddeti) hesapla → görsel yoğunluğu besler.
- Basit **onset/beat detection** (spektral fark yöntemi, tam FFT kütüphanesi gerekmeden hafif bir algoritma; gerekirse JTransforms veya küçük bir Kotlin FFT implementasyonu) → müzik temposunu ve fraktal "nabız" animasyonunu tetikler.
- Gürültü tabanı filtreleme (sessiz ortamda rastgele tetiklenmeyi önlemek için eşik/gate uygula).

### 3.3 Görsel Katman: Fraktal Renderer

- **OpenGL ES 3.0** üzerinde tek bir fragment shader ile prosedürel fraktal (ör. Julia set varyasyonu, domain warping, veya flow-field/reaction-diffusion tarzı organik desenler — klasik Mandelbrot yerine daha "yumuşak, meditatif" görünümler stres azaltma hedefine daha uygun).
- Shader uniform'ları: `u_lightLevel`, `u_audioEnergy`, `u_touchPos`, `u_time`, `u_colorPalette`.
- **GLWallpaperService** pattern (açık kaynak, community-maintained) veya doğrudan `GLSurfaceView` mantığını `WallpaperService.Engine` içine entegre eden yaklaşım kullan.
- **Kritik performans kuralı:** Ekran görünür değilken (`onVisibilityChanged(false)`) render döngüsünü tamamen durdur. Bu, live wallpaper'ların en çok eleştirildiği pil tüketimi sorununu çözer.
- Hedef: 30 FPS (60 değil) — göz için yeterince akıcı, pil için çok daha ekonomik. Kullanıcı ayarından değiştirilebilir.

### 3.4 Ses Katmanı: Üretken Müzik

"Yapay zeka müzikleri" ifadesi pazarlamada güçlü ama teknik gerçekçilik açısından önemli bir ayrım yapalım:

- **Gerçekçi ve performanslı yaklaşım:** Cihaz üzerinde büyük bir AI müzik modeli çalıştırmak (ör. transformer tabanlı müzik üretimi) hem batarya hem gecikme açısından pratik değil.
- **Önerilen yaklaşım — prosedürel/algoritmik sentez:** Klasik ambient/generative music tekniği (Brian Eno'nun "Music for Airports" mantığı): birkaç uzun, örtüşen pad/drone sesi, pentatonik veya modal bir skala içinden sensör verisine göre rastgele ama kurallı seçilen notalar, düşük geçiren filtre modülasyonu ışık seviyesine bağlı.
- Ton üretimi için **Oboe** (Google'ın düşük gecikmeli ses kütüphanesi) + basit oscillator/envelope sentezi (sine/triangle dalga + ADSR), ya da hazır örnek (sample) tabanlı katmanlama.
- Bu şekilde "yapay zeka" hissi veren ama gerçekte **deterministik, düşük kaynaklı, App Store'da sorunsuz çalışan** bir sistem elde edilir. Pazarlamada "generative/adaptive soundscape" demek hem daha doğru hem de teknik beklenti yönetimi açısından daha sağlıklı.

---

## 4. Kullanıcı Etkileşimi

- **Dokunma:** Ekrana dokunma noktasında fraktalın "merkezi" o noktaya kayar, dalga/ripple efekti yayılır, o anki nota/akor tetiklenir (haptic feedback ile senkronize, `VibrationEffect`).
- **Basılı tutma:** Yavaşça zoom/derinlik efekti, drone sesinin sürmesi.
- **Ayarlar ekranı (Compose ile):** Renk paleti seçimi, hassasiyet (ışık/ses tepkisi şiddeti), sessiz mod (sadece görsel, mikrofon kapalı), pil tasarrufu modu (15 FPS + basitleştirilmiş shader).

---

## 5. İzinler ve Gizlilik (Play Store Onayı İçin Kritik)

| İzin | Gerekli mi? | Not |
|---|---|---|
| `RECORD_AUDIO` | Evet | Play Store'da açık gerekçe metni + ayarlarda kolay kapatma anahtarı şart |
| `CAMERA` | Hayır (varsayılan) | Sadece opsiyonel gelişmiş modda, kullanıcı onayıyla |
| Işık sensörü | İzin gerektirmez | Donanım sensörü, manifest'te ekstra izin yok |

Gizlilik politikasında **"ses verisi cihazda işlenir, hiçbir yere gönderilmez"** ifadesi hem kullanıcı güvenini hem Play Store data-safety formunu kolaylaştırır. Tüm işleme on-device olmalı — bulut API'sine ihtiyaç yok, bu da hem gecikmeyi hem gizlilik riskini ortadan kaldırır.

---

## 6. Önerilen Teknoloji Yığını

- **Dil:** Kotlin
- **Render:** OpenGL ES 3.0 (GLSL fragment shader tabanlı fraktal)
- **Ses I/O:** Oboe (C++ / JNI) veya `AudioTrack` + `AudioRecord` (daha basit MVP için)
- **UI (ayarlar ekranı):** Jetpack Compose
- **Sensör:** `SensorManager` standart Android API
- **Mimari:** MVVM, `WallpaperService.Engine` içinde bir `Choreographer` tabanlı render döngüsü

---

## 7. MVP Kapsamı (İlk Sürüm İçin Öneri)

1. Işık sensörü → renk paleti/parlaklık kontrolü (kamera yok)
2. Basit RMS ses enerjisi → görsel yoğunluk (tam beat detection MVP'de gerekmez)
3. Tek fraktal shader (Julia set domain-warp varyasyonu)
4. Dokunma → ripple + basit ton tetikleme
5. Temel ayarlar ekranı (hassasiyet, sessiz mod, pil tasarrufu)

Beat detection, çoklu fraktal tipi, gelişmiş kamera modu → v2'ye ertelenebilir.

---

## 8. Sonraki Adım Önerisi

Bu spesifikasyonu implementasyon aracına (DeepSeek/Cursor/OpenCode) verirken şu sırayla ilerlemeni öneririm:
1. `WallpaperService` iskeleti + görünürlük yaşam döngüsü (pil optimizasyonunun temeli)
2. Işık sensörü entegrasyonu + tek statik fraktal shader
3. Mikrofon RMS analizi entegrasyonu
4. Prosedürel ses sentezi katmanı
5. Dokunma etkileşimi + ayarlar ekranı
