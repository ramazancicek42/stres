package com.aura.livewallpaper.renderer

/**
 * Vertex shader - basit pass-through
 */
const val VERTEX_SHADER = """
    #version 300 es
    in vec4 aPosition;
    in vec2 aTexCoord;
    out vec2 vTexCoord;
    
    void main() {
        gl_Position = aPosition;
        vTexCoord = aTexCoord;
    }
"""

/**
 * Professional Fragment Shader v2.0
 * 
 * Multi-fraktal sistemi:
 * - Julia Set (klasik)
 * - Burning Ship (agresif)
 * - Reaction-Diffusion (organik)
 * - Flow Field (akış)
 * - Mandelbrot varyasyonu
 * 
 * Ek özellikler:
 * - Multi-octave noise
 * - Domain warping (gelişmiş)
 * - Smooth coloring (yüksek kalite)
 * - Multi-layer renklendirme
 * - Professional vignette
 */
const val FRACTAL_FRAGMENT_SHADER = """
    #version 300 es
    precision highp float;
    
    in vec2 vTexCoord;
    out vec4 fragColor;
    
    uniform float uTime;
    uniform float uLightLevel;
    uniform float uAudioEnergy;
    uniform float uBeatSync;
    uniform vec2 uTouchPos;
    uniform float uTouchIntensity;
    uniform vec4 uRipple; // x, y, radius, alpha
    uniform vec3 uColorDark;
    uniform vec3 uColorMid;
    uniform vec3 uColorLight;
    uniform float uAspectRatio;
    uniform float uComplexity;
    uniform bool uFrozen;
    uniform int uFractalType; // 0: Julia, 1: Burning Ship, 2: Mandelbrot, 3: Flow Field
    uniform float uZoom;
    uniform vec2 uPanOffset;
    
    // ============================================
    // NOISE FONKSİYONLARI (Multi-octave)
    // ============================================
    
    // Hash fonksiyonu
    float hash(vec2 p) {
        return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
    }
    
    // Value noise
    float noise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        
        float a = hash(i);
        float b = hash(i + vec2(1.0, 0.0));
        float c = hash(i + vec2(0.0, 1.0));
        float d = hash(i + vec2(1.0, 1.0));
        
        return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    }
    
    // FBM (Fractal Brownian Motion)
    float fbm(vec2 p) {
        float value = 0.0;
        float amplitude = 0.5;
        float frequency = 1.0;
        
        for (int i = 0; i < 6; i++) {
            value += amplitude * noise(p * frequency);
            amplitude *= 0.5;
            frequency *= 2.0;
        }
        
        return value;
    }
    
    // Domain warping (gelişmiş)
    vec2 domainWarp(vec2 p, float strength) {
        float t = uTime * 0.1;
        
        vec2 q = vec2(
            fbm(p + vec2(0.0, 0.0) + t * 0.3),
            fbm(p + vec2(5.2, 1.3) + t * 0.2)
        );
        
        vec2 r = vec2(
            fbm(p + 4.0 * q + vec2(1.7, 9.2) + t * 0.15),
            fbm(p + 4.0 * q + vec2(8.3, 2.8) + t * 0.12)
        );
        
        return p + strength * vec2(
            fbm(p + 4.0 * r),
            fbm(p + 4.0 * r + vec2(3.1, 7.4))
        );
    }
    
    // ============================================
    // FRAKTAL FONKSİYONLARI
    // ============================================
    
    // Julia Set
    float juliaSet(vec2 p) {
        vec2 c = vec2(
            -0.8 + sin(uTime * 0.1) * 0.15,
            0.156 + cos(uTime * 0.07) * 0.08
        );
        
        vec2 z = p;
        int maxIter = int(80.0 + uComplexity * 40.0);
        
        for (int i = 0; i < 150; i++) {
            if (i >= maxIter) break;
            
            float x = z.x * z.x - z.y * z.y + c.x;
            float y = 2.0 * z.x * z.y + c.y;
            
            if (x * x + y * y > 4.0) {
                return float(i) / float(maxIter);
            }
            
            z.x = x;
            z.y = y;
        }
        
        return 0.0;
    }
    
    // Burning Ship (mutlak değerli Julia)
    float burningShip(vec2 p) {
        vec2 c = vec2(
            -1.5 + sin(uTime * 0.08) * 0.2,
            -0.3 + cos(uTime * 0.06) * 0.1
        );
        
        vec2 z = p;
        int maxIter = int(80.0 + uComplexity * 40.0);
        
        for (int i = 0; i < 150; i++) {
            if (i >= maxIter) break;
            
            float x = z.x * z.x - z.y * z.y + c.x;
            float y = abs(2.0 * z.x * z.y) + c.y;
            
            if (x * x + y * y > 4.0) {
                return float(i) / float(maxIter);
            }
            
            z.x = x;
            z.y = y;
        }
        
        return 0.0;
    }
    
    // Mandelbrot Varyasyonu (zoom + time)
    float mandelbrot(vec2 p) {
        vec2 c = p;
        vec2 z = vec2(0.0);
        
        float zoom = uZoom * 2.0;
        c = c / zoom + vec2(-0.5, 0.0);
        
        int maxIter = int(80.0 + uComplexity * 40.0);
        
        for (int i = 0; i < 150; i++) {
            if (i >= maxIter) break;
            
            float x = z.x * z.x - z.y * z.y + c.x;
            float y = 2.0 * z.x * z.y + c.y;
            
            if (x * x + y * y > 4.0) {
                return float(i) / float(maxIter);
            }
            
            z.x = x;
            z.y = y;
        }
        
        return 0.0;
    }
    
    // Flow Field (akış tabanlı)
    float flowField(vec2 p) {
        float t = uTime * 0.2;
        vec2 q = vec2(
            fbm(p + vec2(0.0, 0.0) + t),
            fbm(p + vec2(5.2, 1.3) + t * 0.7)
        );
        
        vec2 r = vec2(
            fbm(p + 4.0 * q + vec2(1.7, 9.2) + t * 0.5),
            fbm(p + 4.0 * q + vec2(8.3, 2.8) + t * 0.4)
        );
        
        float f = fbm(p + 4.0 * r);
        
        // Renklendirme için float değer
        return f;
    }
    
    // Reaction-Diffusion (basitleştirilmiş)
    float reactionDiffusion(vec2 p) {
        float t = uTime * 0.15;
        
        // Gray-Scott modeli (basitleştirilmiş)
        float u = fbm(p * 2.0 + vec2(t * 0.3, 0.0));
        float v = fbm(p * 2.0 + vec2(0.0, t * 0.3) + vec2(5.0));
        
        // Reaksiyon-扩散 etkisi
        float reaction = sin(u * 6.28 + v * 3.14) * 0.5 + 0.5;
        reaction = pow(reaction, 2.0 - uComplexity);
        
        return reaction;
    }
    
    // ============================================
    // RENK PALETLERİ (Gelişmiş)
    // ============================================
    
    vec3 palette(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
        return a + b * cos(6.28318 * (c * t + d));
    }
    
    vec3 oceanPalette(float t) {
        return palette(t, 
            vec3(0.06, 0.20, 0.48),
            vec3(0.10, 0.37, 0.73),
            vec3(0.29, 0.56, 0.85),
            vec3(0.263, 0.416, 0.557)
        );
    }
    
    vec3 sunsetPalette(float t) {
        return palette(t,
            vec3(0.18, 0.11, 0.18),
            vec3(0.72, 0.36, 0.22),
            vec3(0.96, 0.64, 0.38),
            vec3(0.263, 0.416, 0.557)
        );
    }
    
    vec3 forestPalette(float t) {
        return palette(t,
            vec3(0.06, 0.16, 0.12),
            vec3(0.18, 0.42, 0.31),
            vec3(0.32, 0.72, 0.53),
            vec3(0.263, 0.416, 0.557)
        );
    }
    
    vec3 cosmicPalette(float t) {
        return palette(t,
            vec3(0.04, 0.04, 0.07),
            vec3(0.48, 0.41, 0.67),
            vec3(0.58, 0.44, 0.86),
            vec3(0.263, 0.416, 0.557)
        );
    }
    
    vec3 warmPalette(float t) {
        return palette(t,
            uColorDark,
            uColorMid,
            uColorLight,
            vec3(0.263, 0.416, 0.557)
        );
    }
    
    // ============================================
    // ANA FONKSİYON
    // ============================================
    
    void main() {
        // Koordinat hazırlığı
        vec2 uv = vTexCoord * 2.0 - 1.0;
        uv.x *= uAspectRatio;
        
        // Pan offset uygula
        uv += uPanOffset;
        
        // Touch etkisi
        if (uTouchIntensity > 0.01) {
            vec2 touchUV = uTouchPos * 2.0 - 1.0;
            touchUV.x *= uAspectRatio;
            float dist = distance(uv, touchUV);
            uv += (uv - touchUV) * uTouchIntensity * 0.4 * smoothstep(0.6, 0.0, dist);
        }
        
        // Ripple efekti
        if (uRipple.w > 0.01) {
            vec2 rippleUV = uRipple.xy * 2.0 - 1.0;
            rippleUV.x *= uAspectRatio;
            float rippleDist = distance(uv, rippleUV);
            float rippleWave = sin(rippleDist * 25.0 - uTime * 6.0) * uRipple.w;
            uv += normalize(uv - rippleUV) * rippleWave * 0.06;
        }
        
        // Zoom ayarı
        float zoom = uZoom * (2.0 - uAudioEnergy * 0.3 - uLightLevel * 0.2);
        vec2 p = uv / zoom;
        
        // Domain warping (complexity ile güçlendirilmiş)
        float warpStrength = (0.15 + uAudioEnergy * 0.2) * uComplexity;
        p = domainWarp(p, warpStrength);
        
        // Fraktal hesapla
        float fractalValue = 0.0;
        
        switch (uFractalType) {
            case 0: // Julia Set
                fractalValue = juliaSet(p);
                break;
            case 1: // Burning Ship
                fractalValue = burningShip(p);
                break;
            case 2: // Mandelbrot
                fractalValue = mandelbrot(p);
                break;
            case 3: // Flow Field
                fractalValue = flowField(p);
                break;
            case 4: // Reaction-Diffusion
                fractalValue = reactionDiffusion(p);
                break;
            default:
                fractalValue = juliaSet(p);
        }
        
        // Smooth coloring (yüksek kalite)
        float t = fractalValue;
        
        // Animasyonlu renk döngüsü
        if (!uFrozen) {
            t += uTime * 0.03;
        }
        t = fract(t);
        
        // Beat sync ile nabız efekti
        float beatPulse = uBeatSync * 0.4;
        t = mix(t, t * 1.6 + uAudioEnergy * 0.35 + beatPulse, 0.6);
        
        // Renk paleti seçimi (ışık seviyesine göre)
        vec3 color;
        float lightPhase = uLightLevel;
        
        if (lightPhase < 0.3) {
            color = cosmicPalette(t);
        } else if (lightPhase < 0.5) {
            color = oceanPalette(t);
        } else if (lightPhase < 0.7) {
            color = forestPalette(t);
        } else if (lightPhase < 0.9) {
            color = sunsetPalette(t);
        } else {
            color = warmPalette(t);
        }
        
        // Işık seviyesi ile parlaklık ayarla
        color *= 0.4 + uLightLevel * 0.9;
        
        // Ses enerjisi ile ekstra parlama
        color += vec3(uAudioEnergy * 0.35);
        
        // Beat sync ile parlama
        color += vec3(beatPulse * 0.25);
        
        // Multi-layer glow efekti
        float glow = exp(-fractalValue * 3.0) * 0.5;
        color += vec3(glow * uAudioEnergy);
        
        // Profesyonel vignette
        vec2 vignetteUV = uv * 0.7;
        float vignette = 1.0 - dot(vignetteUV, vignetteUV) * 0.4;
        vignette = smoothstep(0.0, 1.0, vignette);
        color *= vignette;
        
        // Film grain (hafif doku)
        float grain = hash(uv * 1000.0 + uTime) * 0.03;
        color += vec3(grain);
        
        // Clamp
        color = clamp(color, 0.0, 1.0);
        
        fragColor = vec4(color, 1.0);
    }
"""
