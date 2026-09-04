# Aura Live Wallpaper - Sensör-Duyarlı Fraktal Meditasyon Duvar Kağıdı

Android için ortam ışığı ve sese duyarlı, prosedürel fraktal animasyonlar üreten canlı duvar kağıdı uygulaması.

## Özellikler

- **Ortam Işığı Sensörü**: Telefonun ışık sensörünü kullanarak renk paleti ve parlaklığı otomatik ayarlar
- **Ses Analizi**: Mikrofondan ortam sesi enerjisini analiz eder, görsel yoğunluğu değiştirir
- **Fraktal Animasyon**: OpenGL ES ile gerçek zamanlı Julia set tabanlı meditatif görseller
- **Prosedürel Müzik**: Algoritmik ambient müzik üretimi (Brian Eno tarzı)
- **Dokunma Etkileşimi**: Ekrana dokununca dalga efekti ve ses tetikleme
- **Pil Dostu**: Ekran kapalıyken tüm işlemler durur
- **Gizlilik Odaklı**: Kamera kullanılmaz, ses verisi cihazda işlenir

## Teknik Detaylar

### Mimari

```
┌─────────────────────────────────────────────────┐
│              AuraWallpaperService                 │
│         (WallpaperService.Engine)                 │
├─────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐             │
│  │LightSensor   │  │ AudioAnalyzer│             │
│  │Manager       │  │              │             │
│  └───────┬──────┘  └──────┬───────┘             │
│          │                │                      │
│          ▼                ▼                      │
│  ┌─────────────────────────────────┐              │
│  │      StateVector (Kotlin)        │              │
│  └───────┬─────────────────┬───────┘              │
│          │                 │                      │
│          ▼                 ▼                      │
│  ┌───────────────┐  ┌──────────────────┐          │
│  │ FractalRenderer│  │ GenerativeAudio  │          │
│  │  (OpenGL ES)   │  │  Engine          │          │
│  └───────────────┘  └──────────────────┘          │
└─────────────────────────────────────────────────┘
```

### Teknolojiler

- **Dil**: Kotlin
- **Render**: OpenGL ES 2.0/3.0 (GLSL shader)
- **UI**: Jetpack Compose
- **Ses**: AudioRecord + AudioTrack
- **Sensör**: Android SensorManager

## Kurulum

1. Android Studio'da projeyi açın
2. Gradle sync yapın
3. APK'yı build edin veya doğrudan cihaza run edin

## İzinler

- `RECORD_AUDIO`: Ses analizi için (opsiyonel, sessiz modda gerekmez)
- Işık sensörü: İzin gerektirmez (donanımsal)

## Kullanım

1. Uygulamayı yükleyin
2. Ana ekranda uzun basın → Duvar Kağıtları → Aura Meditation'ı seçin
3. Ayarlar için uygulama ikonuna tıklayın

## Ayarlar

- Işık Hassasiyeti
- Ses Hassasiyeti
- Sessiz Mod
- Pil Tasarrufu Modu
- Renk Paleti Seçimi (5 farklı tema)
- FPS Limiti (15/30/60)

## Build

```bash
./gradlew assembleDebug
```

## License

MIT License
