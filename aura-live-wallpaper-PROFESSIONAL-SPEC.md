# AURA — Professional Sensör-Duyarlı İnteraktif Duvar Kağıdı
## Geliştirilmiş Spesifikasyon Dokümanı v2.0

---

## 1. Vizyon

**"Cihazınızın duyusal verilerini benzersiz sanatsal deneyime dönüştüren, stres azaltan canlı duvar kağıdı."**

Bu uygulama, telefonun ortam ışığı, ses ritmi ve dokunma girdilerini gerçek zamanlı işleyerek:
- **Görsel:** Çok katmanlı, organik fraktal sanat (Julia set, reaction-diffusion, flow-field)
- **İşitsel:** Professional kalitede generatif ambient müzik (reverb, delay, pad synthesis, grain)
- **Davranışsal:** Stres seviyesine göre kendini adapte eden akıllı sistem

---

## 2. Orijinal Prompt Geliştirme

### Orijinal:
> "Kameradan veya mikrofondan gelen verileri, ekranda soyut dinamik grafiklere veya yapay zeka müziklerine dönüştürür."

### Geliştirilmiş:
> "Ortam ışığı sensörü, mikrofon ve dokunma girdilerini real-time işleyerek, multi-layer organik fraktal animasyonlar ve professional kalitede prosedürel ses manzarası üreten, kullanıcının stres seviyesine göre kendini adapte eden interaktif canlı duvar kağıdı."

---

## 3. Kritik Mimari Geliştirmeler

### 3.1 Görsel Katman: Multi-Fraktal Sistem

**Mevcut Durum:** Tek Julia set shader'ı
**Hedef:** 5 farklı fraktal tipi arasında geçiş + Organik desenler

| Fraktal Tipi | Kullanım | Görsel Karakter |
|---|---|---|
| Julia Set | Ana desen | Klasik, tanıdık |
| Mandelbrot Varyasyonu | Zoom modu | Derinlik, keşif |
| Burning Ship | Agresif mod | Dinamik, güçlü |
| Reaction-Diffusion | Organik mod | Biyolojik, doğal |
| Flow Field | Akış modu | Yumuşak, meditatif |

**Shader Geliştirmeleri:**
- Domain warping gücü artırıldı (0.1 → 0.3)
- Iterasyon sayısı dinamik (60-200 arası)
- Smooth coloring kalitesi artırıldı
- Vignette efekti yumuşatıldı
- Multi-octave noise eklendi

### 3.2 Ses Katmanı: Professional Generatif Müzik

**Mevcut Durum:** Tek sine dalga + basit harmonik
**Hedef:** Multi-layer, professional kalitede ambient ses manzarası

**Katmanlar:**
```
┌─────────────────────────────────────────┐
│  Layer 1: Drone Pad (Sürekli)           │
│  - Low-pass filtered saw wave           │
│  - 220Hz base + sub-octave              │
│  - Light sensor → filter cutoff         │
├─────────────────────────────────────────┤
│  Layer 2: Ambient Pad (Örtüşen)         │
│  - Sine + triangle karışımı            │
│  - Pentatonik skala notaları            │
│  - 4-8 saniye süreli attack/release     │
├─────────────────────────────────────────┤
│  Layer 3: Melodik Notalar (Rastgele)    │
│  - Piano-like envelope                  │
│  - Reverb + delay efekti               │
│  - Beat senkronizasyonu                 │
├─────────────────────────────────────────┤
│  Layer 4: Texture/Grain (Doku)          │
│  - Granular synthesis                   │
│  - Rastgele grain'lar                   │
│  - Stereo genişletme                    │
└─────────────────────────────────────────┘
```

**Ses Efektleri:**
- **Reverb:** Room simulation (3ms delay, 0.6 feedback)
- **Delay:** Tempo-senkrecho echo (beat-aligned)
- **Chorus:** Hafif pitch modulation (0.5Hz LFO)
- **Filter:** Low-pass + resonance (ışık sensörüne bağlı)

### 3.3 Beat Detection: Gelişmiş Ritim Analizi

**Mevcut Durum:** Basit RMS enerji eşiği
**Hedef:** Spektral analiz + Onset detection

**Algoritma:**
1. Ham PCM → FFT (1024 sample)
2. Frekans bantlarına ayır (bass/mid/treble)
3. Spektral fark hesapla (spectral flux)
4. Onset detection (eşik aşımı)
5. Tempo estimation (autocorrelation)
6. Beat sync sinyali üret

---

## 4. Yeni Özellikler

### 4.1 Kamera Entegrasyonu (Opsiyonel)
- Renk sıcaklığı analizi (warm/cool)
- Ortam parlaklığı (+ ışık sensörü)
- Kullanıcı onayıyla, sadece ana ekran aktifken
- Düşük çözünürlük (16x16), tek kare

### 4.2 İvmeölçer Entegrasyonu
- Telefona eğim/hareket → parallax efekti
- Sarsıntı → ripple tetikleme
- Düşük pil maliyeti

### 4.3 Stres Adaptif Sistem
- Ses paterninden stres tahmini
- Nefes rehberliği (4-7-8 tekniği)
- Renk/hız/karmaşıklık otomatik ayarı
- Meditasyon seansı yönetimi

---

## 5. Performans Hedefleri

| Metrik | Hedef | Mevcut |
|---|---|---|
| FPS (normal) | 30 | 30 ✅ |
| FPS (pil tasarrufu) | 15 | 15 ✅ |
| CPU kullanımı | %15-25 | Bilinmiyor |
| RAM kullanımı | <100MB | Bilinmiyor |
| Pil tüketimi (1 saat) | %5-8 | Bilinmiyor |
| İlk açılım süresi | <2sn | Bilinmiyor |

---

## 6. Uygulama Planı

### Aşama 1: Shader Profesyonelleştirme (Bu oturum)
- [ ] Multi-fraktal shader sistemi
- [ ] Reaction-diffusion shader
- [ ] Flow-field shader
- [ ] Multi-octave noise fonksiyonları
- [ ] Geliştirilmiş renk paletleri

### Aşama 2: Ses Profesyonelleştirme (Bu oturum)
- [ ] Multi-layer ses mimarisi
- [ ] Reverb/delay efekti
- [ ] Pad synthesizer
- [ ] Granular synthesis
- [ ] FFT tabanlı beat detection

### Aşama 3: Sensör Entegrasyonu (Bu oturum)
- [ ] Kamera modu (opsiyonel)
- [ ] İvmeölçer entegrasyonu
- [ ] Gelişmiş dokunma etkileşimi

### Aşama 4: UI/UX Geliştirme (Bu oturum)
- [ ] Gelişmiş ayarlar ekranı
- [ ] Fraktal tipi seçimi
- [ ] Ses katmanı kontrolü
- [ ] Performans monitörü

---

## 7. Teknoloji Yığını (Güncellenmiş)

- **Dil:** Kotlin
- **Render:** OpenGL ES 3.0 (GLSL 300 es)
- **Ses:** AudioTrack + FFT (JTransforms veya özel implementasyon)
- **UI:** Jetpack Compose
- **Sensör:** SensorManager + CameraX (opsiyonel)
- **Mimari:** MVVM + Coroutines

---

## 8. Beklenen Çıktı

**Kullanıcı Deneyimi:**
1. Uygulama açılır → İlk 2 saniyede fraktal yüklenir
2. Işık sensörü aktif → Renk paleti otomatik ayarlanır
3. Mikrofon dinler → Ses ritmine göre fraktal "nefes alır"
4. Ekrana dokunulur → Ripple + nota tetiklenir
5. Stres yüksek → Sistem otomatik sakinleştirici moda geçer
6. Meditasyon başlatılır → 4-7-8 nefes rehberliği başlar

**Görsel Kalite:**
- Pürüzsüz renk geçişleri (banding yok)
- 60fps'de akıcı animasyon
- Yüksek kontrast oranı
- Koyu arka plan + parlak fraktal

**Ses Kalitesi:**
- Professional ambient müzik hissi
- Distorsiyon yok
- Dengeli frekans spektrumu
- Ortam gürültüsüyle uyumlu
