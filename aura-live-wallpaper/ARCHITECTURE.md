# AURA Live Wallpaper - Mantık Şeması ve Sistem Mimarisi

## 📊 Genel Sistem Mimarisi

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        ANDROID LIVE WALLPAPER SERVICE                    │
│                         (AuraWallpaperService)                           │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐          ┌───────────────┐          ┌───────────────┐
│   UI LAYER    │          │  SENSOR LAYER │          │ RENDER LAYER  │
│               │          │               │          │               │
│ SettingsAct.  │          │ LightSensor   │          │ FractalRender │
│ Jetpack Compose│         │ AudioAnalyzer │          │ OpenGL ES 2.0 │
│ AuraPreferences│         │ GenerativeAud │          │ GLSL Shaders  │
└───────────────┘          └───────────────┘          └───────────────┘
```

## 🔄 Veri Akış Diyagramı

### 1. IŞIK SENSÖRÜ VERİ AKIŞI
```
[Ortam Işığı] 
      │
      ▼ (TYPE_LIGHT sensörü)
[LightSensorManager] ──► onLightLevelChanged(lux: Float)
      │
      ▼ (hassasiyet ayarı uygulanır)
[AuraPreferences.lightSensitivity]
      │
      ▼ (normalize: 0-10000 → 0-1)
[FractalRenderer.setLightLevel()]
      │
      ├─────────────────┐
      ▼                 ▼
[Shader: uLightLevel]  [GenerativeAudio.setFilterCutoff()]
      │                 │
      ▼                 ▼
[Julia Set Zoom]    [Audio Filter Modülasyonu]
[Renk Parlaklığı]
```

### 2. SES ANALİZİ VERİ AKIŞI
```
[Mikrofon] 
      │
      ▼ (RECORD_AUDIO izni gerekli)
[AudioRecord] ──► PCM 16-bit mono @ 16kHz
      │
      ▼ (buffer okuma thread'i)
[AudioAnalyzer.analyzeAudioLoop()]
      │
      ▼ (RMS hesaplama)
[calculateRMS(buffer)] ──► Noise Gate (>0.02)
      │
      ▼ (energy: 0-1 aralığı)
[onAudioEnergyChanged(energy, rms)]
      │
      ▼
[FractalRenderer.setAudioEnergy()]
      │
      ▼
[Shader: uAudioEnergy]
      │
      ├─────────────────────────┬─────────────────────────┐
      ▼                         ▼                         ▼
[Julia Set Domain Warping]  [Renk Yoğunluğu]       [Extra Glow Efekti]
```

### 3. GENERATİVE AUDIO MOTORU
```
[Pentatonik Skala] ──► [220Hz, 261Hz, 293Hz, 329Hz, 392Hz, 440Hz]
      │
      ▼ (rastgele ama kurallı seçim)
[Note Selector] ──► her 4 saniyede bir nota
      │
      ▼ (ADSR envelope: Attack 10%, Decay 30%)
[Sine + Triangle Wave Mix]
      │
      ▼ (Low-pass filter cutoff = lightLevel)
[AudioTrack.write(buffer)]
      │
      ▼
[Hoparlör] ──► Ambient Drone Sesi
```

### 4. DOKUNMA ETKİLEŞİMİ
```
[TouchEvent ACTION_DOWN/MOVE]
      │
      ▼
[AuraEngine.onTouchEvent()]
      │
      ├──► [FractalRenderer.handleTouchEvent()]
      │         │
      │         ▼
      │     [touchX, touchY, touchIntensity = 1.0]
      │         │
      │         ▼ (zamanla decay: intensity *= (1 - timeSinceTouch))
      │     [Shader: uTouchPos, uTouchIntensity]
      │         │
      │         ▼
      │     [Julia Set Distortion at Touch Point]
      │
      └──► [GenerativeAudio.triggerNote()] (sessiz mod değilse)
              │
              ▼
           [Anında Nota Çal]
      
      └──► [Vibrator.vibrate(50ms)] (Haptic Feedback)
```

## ⚙️ Render Pipeline (OpenGL ES)

### Vertex Shader (Pass-through)
```
Input: aPosition (x,y), aTexCoord (u,v)
Output: vTexCoord (varying)
Process: gl_Position = aPosition
```

### Fragment Shader (Julia Set + Effects)
```
Inputs:
  - uTime (animasyon zamanı)
  - uLightLevel (0-1, ışık sensörü)
  - uAudioEnergy (0-1, ses analizi)
  - uTouchPos (x,y), uTouchIntensity (0-1)
  - uColorDark, uColorMid, uColorLight (RGB palet)
  - uAspectRatio (ekran oranı)

Processing Steps:
  1. UV koordinatlarını -1..1 aralığına dönüştür
  2. Aspect ratio düzeltmesi uygula
  3. Touch distortion uygula (dokunma noktasına doğru kayma)
  4. Zoom hesapla (ses + ışığa bağlı)
  5. Domain warping uygula (sin/cos dalgaları ile)
  6. Julia Set iterasyonu (max 80 iterasyon)
  7. Smooth coloring (logarithmic smoothing)
  8. Renk paleti uygulaması (cosine-based palette)
  9. Işık seviyesi ile parlaklık ayarla
  10. Ses enerjisi ile glow ekle
  11. Vignette efekti (kenarları karart)

Output: gl_FragColor (RGBA)
```

## 🔋 Pil Optimizasyonu Stratejisi

### Ekran Görünür Durumu
```
onVisibilityChanged(visible: Boolean):
  │
  ├─ visible = true:
  │   ├─ startAll()
  │   │   ├─ LightSensorManager.start() (her zaman)
  │   │   ├─ AudioAnalyzer.start() (sadece sessiz mod değilse + izin varsa)
  │   │   ├─ GenerativeAudio.start() (sadece sessiz mod değilse)
  │   │   └─ GLSurfaceView.onResume()
  │   └─ RENDERMODE_CONTINUOUSLY (30/60 FPS)
  │
  └─ visible = false:
      └─ stopAll()
          ├─ LightSensorManager.stop()
          ├─ AudioAnalyzer.stop()
          ├─ GenerativeAudio.stop()
          └─ GLSurfaceView.onPause()
```

### FPS Yönetimi
```
Power Saver Mode = true:
  └─ FPS Limit = 15
  
Normal Mode:
  └─ FPS Limit = 30 (varsayılan)
  
Performance Mode:
  └─ FPS Limit = 60
```

## 🔒 Gizlilik ve Güvenlik

### İzin Modeli
| İzin | Kullanım Amacı | Zorunlu? | Ne Zaman Aktif |
|------|---------------|----------|----------------|
| RECORD_AUDIO | Ortam sesi analizi | ❌ Opsiyonel | Sadece ekran açıkken |
| (Işık sensörü) | Ortam ışığı ölçümü | ✅ Gerekmez | Her zaman (donanımsal) |
| VIBRATE | Haptic feedback | ✅ Gerekmez | Sadece dokunmada |

### Veri İşleme Prensipleri
- ✅ Tüm işleme **on-device** (buluta veri gönderilmez)
- ✅ Ses verisi **buffer'da kalır**, kaydedilmez
- ✅ Işık sensörü **kamera kullanmaz** (gizlilik dostu)
- ✅ Arka planda **hiçbir sensör aktif değil**

## 🎨 Renk Paletleri

| İsim | Dark (#RRGGBB) | Mid (#RRGGBB) | Light (#RRGGBB) |
|------|----------------|---------------|-----------------|
| Okyanus Mavisi | #0a1628 | #1a4f7a | #4a90d9 |
| Gün Batımı | #2d1b2e | #b85c38 | #f4a460 |
| Orman Yeşili | #0f281e | #2d6a4f | #52b788 |
| Mor Gece | #1a0f2e | #4a2d7a | #9b72cf |
| Sıcak Amber | #2e1f0f | #b87c38 | #f4c460 |

## 📱 Ayarlar Matrisi

| Ayar | Tip | Varsayılan | Aralık | Etki Alanı |
|------|-----|-----------|--------|------------|
| Işık Hassasiyeti | Float | 0.7 | 0.0 - 1.0 | Sensör → Render zoom/parlaklık |
| Ses Hassasiyeti | Float | 0.5 | 0.0 - 1.0 | Audio energy → Render warp强度 |
| Sessiz Mod | Boolean | false | true/false | Audio engine açık/kapalı |
| Pil Tasarrufu | Boolean | false | true/false | FPS 15'e düşer |
| Renk Paleti | Int | 0 | 0 - 4 | Fragment shader uniform'ları |
| FPS Limiti | Int | 30 | 15/30/60 | Render loop hızı |

## 🧩 Bileşen İlişkileri (Class Diagram)

```
┌────────────────────────────┐
│  AuraWallpaperService      │
│  (extends WallpaperService)│
└────────────────────────────┘
            │
            │ contains
            ▼
┌────────────────────────────┐
│  AuraEngine                │
│  (inner class: Engine)     │
│  implements:               │
│    - LightSensor.Listener  │
│    - AudioAnalyzer.Listener│
│    - GenAudio.Listener     │
└────────────────────────────┘
            │
            │ uses
            ├──────────────────────────────────────┐
            ▼                                      ▼
┌──────────────────────┐              ┌──────────────────────┐
│ LightSensorManager   │              │   AudioAnalyzer      │
│ - SensorManager      │              │   - AudioRecord      │
│ - Sensor.TYPE_LIGHT  │              │   - RMS calculation  │
│ - Listener pattern   │              │   - Noise gate       │
└──────────────────────┘              └──────────────────────┘
            │                                      │
            ▼                                      ▼
┌──────────────────────┐              ┌──────────────────────┐
│ GenerativeAudioEngine│              │   FractalRenderer    │
│ - Pentatonic scale   │◄─────────────│   - GLSurfaceView    │
│ - ADSR envelope      │  uniforms    │   - Shader programs  │
│ - AudioTrack stream  │              │   - Touch handling   │
└──────────────────────┘              └──────────────────────┘
                                               │
                                               ▼
                                    ┌──────────────────────┐
                                    │  FullScreenQuad      │
                                    │  - VBO/IBO           │
                                    │  - Draw call         │
                                    └──────────────────────┘

┌──────────────────────┐
│  AuraPreferences     │
│  - SharedPreferences │
│  - 6 ayar parametresi│
└──────────────────────┘
            ▲
            │ reads/writes
            │
┌──────────────────────┐
│  SettingsActivity    │
│  - Jetpack Compose   │
│  - Permission request│
└──────────────────────┘
```

## 🚀 Başlatma Sırası (Lifecycle)

```
1. onCreate()
   ├─ AuraPreferences init
   ├─ LightSensorManager init
   ├─ AudioAnalyzer init
   ├─ GenerativeAudioEngine init
   ├─ FractalRenderer init
   └─ Listeners bağla

2. onSurfaceCreated()
   ├─ GLSurfaceView oluştur
   ├─ EGLContext 2.0 ayarla
   ├─ Renderer set et
   └─ RENDERMODE_CONTINUOUSLY

3. onVisibilityChanged(true)
   ├─ LightSensorManager.start()
   ├─ AudioAnalyzer.start() (izin varsa)
   ├─ GenerativeAudio.start() (sessiz değilse)
   └─ GLSurfaceView.onResume()

4. onDrawFrame() (her frame)
   ├─ glClear()
   ├─ Uniform'ları güncelle (time, light, audio, touch)
   ├─ Color palette set et
   └─ FullScreenQuad.draw()

5. onTouchEvent()
   ├─ FractalRenderer.handleTouch()
   ├─ GenerativeAudio.triggerNote() (opsiyonel)
   └─ Vibrator.vibrate()

6. onVisibilityChanged(false) veya onDestroy()
   └─ Tüm sensörler ve render durdur
```

## 🎯 Performans Metrikleri (Hedef)

| Metrik | Hedef | Ölçüm Yöntemi |
|--------|-------|---------------|
| FPS (Normal) | 30 ± 2 | GPU Profiler |
| FPS (Pil Tasarrufu) | 15 ± 1 | GPU Profiler |
| CPU Kullanımı (Ekran açık) | < 15% | Android Profiler |
| CPU Kullanımı (Ekran kapalı) | 0% | Android Profiler |
| Bellek Kullanımı | < 50 MB | Memory Profiler |
| Pil Tüketimi (saatte) | < 2% | Battery Historian |
| Audio Latency | < 50 ms | Audio latency test |
| Sensor Sampling Rate | 200ms (UI delay) | Sensor log |

## 🔧 Gelecek İyileştirmeler (Roadmap)

### Kısa Vadeli (v1.1)
- [ ] Accelerometer desteği (cihaz eğimine duyarlılık)
- [ ] Daha fazla fraktal tipi (Mandelbrot, Burning Ship)
- [ ] Widget desteği (ana ekranda mini versiyon)

### Orta Vadeli (v1.2)
- [ ] Spotify/Apple Music entegrasyonu (müziğe senkronizasyon)
- [ ] Machine learning tabanlı renk paleti önerisi
- [ ] Çoklu dokunma desteği (multi-touch ripple)

### Uzun Vadeli (v2.0)
- [ ] AR mode (kamera overlay ile gerçek dünya + fraktal)
- [ ] Social sharing (render screenshot/video)
- [ ] Plugin sistemi (kullanıcılar kendi shader'ını yazabilir)

---

**Dokümantasyon Versiyonu:** 1.0.0  
**Son Güncelleme:** 2024  
**Yazar:** Aura Development Team
