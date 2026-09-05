package com.aura.livewallpaper.renderer

/**
 * Vertex shader - basit pass-through
 */
const val VERTEX_SHADER = """
    attribute vec4 aPosition;
    attribute vec2 aTexCoord;
    
    varying vec2 vTexCoord;
    
    void main() {
        gl_Position = aPosition;
        vTexCoord = aTexCoord;
    }
"""

/**
 * Fragment shader - Julia set tabanlı meditatif fraktal animasyon
 * Domain warping ve yumuşak renk geçişleri içerir
 */
const val FRACTAL_FRAGMENT_SHADER = """
    precision highp float;
    
    varying vec2 vTexCoord;
    
    uniform float uTime;
    uniform float uLightLevel;
    uniform float uAudioEnergy;
    uniform float uBeatSync;
    uniform vec2 uTouchPos;
    uniform float uTouchIntensity;
    uniform vec3 uRipple;
    uniform vec3 uColorDark;
    uniform vec3 uColorMid;
    uniform vec3 uColorLight;
    uniform float uAspectRatio;
    uniform float uComplexity;
    uniform bool uFrozen;
    
    // Julia set parametreleri (zamanla değişen)
    vec2 juliaC = vec2(-0.8, 0.156);
    
    // Smooth coloring için palette
    vec3 palette(float t) {
        vec3 a = uColorDark;
        vec3 b = uColorMid;
        vec3 c = uColorLight;
        vec3 d = vec3(0.263, 0.416, 0.557);
        
        return a + b * cos(6.28318 * (c * t + d));
    }
    
    // Domain warping efekti
    vec2 warp(vec2 p, float strength) {
        vec2 warped = p;
        warped.x += sin(p.y * 3.0 + uTime * 0.5) * strength;
        warped.y += cos(p.x * 3.0 + uTime * 0.3) * strength;
        return warped;
    }
    
    void main() {
        // Ekran koordinatlarını -1..1 aralığına dönüştür (aspect ratio düzeltmeli)
        vec2 uv = vTexCoord * 2.0 - 1.0;
        uv.x *= uAspectRatio;
        
        // Touch etkisi - dokunulan noktaya doğru hafif kayma
        if (uTouchIntensity > 0.01) {
            vec2 touchUV = uTouchPos * 2.0 - 1.0;
            touchUV.x *= uAspectRatio;
            float dist = distance(uv, touchUV);
            uv += (uv - touchUV) * uTouchIntensity * 0.3 * smoothstep(0.5, 0.0, dist);
        }
        
        // Ripple efekti
        if (uRipple.z > 0.01) {
            vec2 rippleUV = uRipple.xy * 2.0 - 1.0;
            rippleUV.x *= uAspectRatio;
            float rippleDist = distance(uv, rippleUV);
            float rippleWave = sin(rippleDist * 20.0 - uTime * 5.0) * uRipple.z;
            uv += normalize(uv - rippleUV) * rippleWave * 0.05;
        }
        
        // Zoom - ses enerjisine ve ışığa bağlı
        float zoom = 1.5 - uAudioEnergy * 0.3 - uLightLevel * 0.2;
        vec2 p = uv * zoom;
        
        // Domain warping uygula (complexity ile güçlendirilmiş)
        float warpStrength = (0.1 + uAudioEnergy * 0.15) * uComplexity;
        p = warp(p, warpStrength);
        
        // Julia set parametreleri - zamanla变化 (donmuşsa变化olmaz)
        if (!uFrozen) {
            juliaC.x = -0.8 + sin(uTime * 0.1) * 0.1;
            juliaC.y = 0.156 + cos(uTime * 0.07) * 0.05;
        }
        
        // Julia set iterasyonu (complexity iterasyon sayısını etkiler)
        vec2 z = p;
        int maxIter = int(60.0 + uComplexity * 40.0);
        float iterFloat = 0.0;
        
        for (int i = 0; i < 100; i++) {
            if (i >= maxIter) break;
            // z = z^2 + c
            float x = (z.x * z.x - z.y * z.y) + juliaC.x;
            float y = (2.0 * z.x * z.y) + juliaC.y;
            
            if ((x * x + y * y) > 4.0) {
                iterFloat = float(i);
                break;
            }
            z.x = x;
            z.y = y;
        }
        
        // Smooth coloring
        if (iterFloat < float(maxIter - 1)) {
            float log_zn = log(z.x * z.x + z.y * z.y) / 2.0;
            float nu = log(log_zn / log(2.0)) / log(2.0);
            iterFloat = iterFloat + 1.0 - nu;
        }
        
        // Renklendirme
        float t = iterFloat / float(maxIter);
        
        // Animasyonlu renk döngüsü (donmuşsa zaman durur)
        if (!uFrozen) {
            t += uTime * 0.05;
        }
        t = fract(t); // 0-1 aralığında döngü
        
        // Beat sync ile nabız efekti
        float beatPulse = uBeatSync * 0.3;
        t = mix(t, t * 1.5 + uAudioEnergy * 0.3 + beatPulse, 0.5);
        
        vec3 color = palette(t);
        
        // Işık seviyesi ile parlaklık ayarla
        color *= 0.5 + uLightLevel * 0.8;
        
        // Ses enerjisi ile ekstra parlama ekle
        color += vec3(uAudioEnergy * 0.3);
        
        // Beat sync ile parlama
        color += vec3(beatPulse * 0.2);
        
        // Vignette efekti (kenarları karart)
        float vignette = 1.0 - dot(uv * uv, vec2(0.3));
        color *= vignette;
        
        gl_FragColor = vec4(color, 1.0);
    }
"""
