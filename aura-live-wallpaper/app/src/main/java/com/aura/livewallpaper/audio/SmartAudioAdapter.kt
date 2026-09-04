package com.aura.livewallpaper.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.exp

/**
 * Akıllı Ses Uyumlaştırıcı
 * 
 * Özellikler:
 * - Ortam sesine göre müzik skalası değişimi (major/minor/modal)
 * - Ritim algılama ile senkronize fraktal pulsasyon
 * - Ses enerji analizi (RMS, zero-crossing rate)
 * - Beat detection
 */
class SmartAudioAdapter {
    
    companion object {
        private const val TAG = "AuraSmartAudio"
        
        // Farklı müzik skalaları
        val SCALE_MAJOR = doubleArrayOf(0, 2, 4, 5, 7, 9, 11)
        val SCALE_MINOR = doubleArrayOf(0, 2, 3, 5, 7, 8, 10)
        val SCALE_DORIAN = doubleArrayOf(0, 2, 3, 5, 7, 9, 10)
        val SCALE_PHRYGIAN = doubleArrayOf(0, 1, 3, 5, 7, 8, 10)
        val SCALE_LYDIAN = doubleArrayOf(0, 2, 4, 6, 7, 9, 11)
        val SCALE_MIXOLYDIAN = doubleArrayOf(0, 2, 4, 5, 7, 9, 10)
        val SCALE_LOCRIAN = doubleArrayOf(0, 1, 3, 5, 6, 8, 10)
        
        // Pentatonik skalalar (daha uyumlu)
        val SCALE_PENTATONIC_MAJOR = doubleArrayOf(0, 2, 4, 7, 9)
        val SCALE_PENTATONIC_MINOR = doubleArrayOf(0, 3, 5, 7, 10)
        
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 1024
    }
    
    data class AudioAnalysis(
        val rms: Float,           // Root Mean Square (enerji)
        val zeroCrossingRate: Float,
        val dominantFrequency: Float,
        val beatProbability: Float,
        val spectralCentroid: Float
    )
    
    data class ScaleRecommendation(
        val scale: DoubleArray,
        val rootNote: Int,
        val confidence: Float,
        val mood: String
    )
    
    private var previousEnergy = 0f
    private var energyHistory = mutableListOf<Float>()
    private var peakTimes = mutableListOf<Long>()
    private var lastBeatTime = 0L
    private var currentBPM = 0f
    
    // Ses analizi için buffer
    private val audioBuffer = FloatArray(BUFFER_SIZE)
    private var bufferPosition = 0
    
    /**
     * Ses verisini analiz et
     */
    fun analyzeAudioSamples(samples: FloatArray): AudioAnalysis {
        if (samples.isEmpty()) {
            return AudioAnalysis(0f, 0f, 0f, 0f, 0f)
        }
        
        // RMS (Root Mean Square) - Enerji ölçümü
        val rms = calculateRMS(samples)
        
        // Zero Crossing Rate - Sıklıkla ilgili bilgi
        val zcr = calculateZeroCrossingRate(samples)
        
        // Dominant frekans tahmini (basit yöntem)
        val dominantFreq = estimateDominantFrequency(samples)
        
        // Beat detection
        val beatProb = detectBeat(rms)
        
        // Spectral centroid (parlaklık ölçüsü)
        val spectralCentroid = calculateSpectralCentroid(samples)
        
        return AudioAnalysis(rms, zcr, dominantFreq, beatProb, spectralCentroid)
    }
    
    /**
     * RMS (Root Mean Square) hesaplama
     */
    private fun calculateRMS(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        
        var sum = 0.0
        for (sample in samples) {
            sum += sample * sample
        }
        
        return sqrt((sum / samples.size).toFloat())
    }
    
    /**
     * Zero Crossing Rate hesaplama
     */
    private fun calculateZeroCrossingRate(samples: FloatArray): Float {
        if (samples.size < 2) return 0f
        
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0 && samples[i - 1] < 0) ||
                (samples[i] < 0 && samples[i - 1] >= 0)) {
                crossings++
            }
        }
        
        return crossings.toFloat() / samples.size
    }
    
    /**
     * Basit dominant frekans tahmini
     */
    private fun estimateDominantFrequency(samples: FloatArray): Float {
        // Gerçek uygulamada FFT kullanılmalı
        // Bu basit bir tahmin
        val zcr = calculateZeroCrossingRate(samples)
        return zcr * SAMPLE_RATE / 2
    }
    
    /**
     * Beat detection (enerji artışlarını takip ederek)
     */
    private fun detectBeat(currentEnergy: Float): Float {
        energyHistory.add(currentEnergy)
        
        // Son 10 değeri tut
        if (energyHistory.size > 10) {
            energyHistory.removeAt(0)
        }
        
        if (energyHistory.size < 5) return 0f
        
        val avgEnergy = energyHistory.average().toFloat()
        val threshold = avgEnergy * 1.3f // Ortalamanın %30 üzeri
        
        val isBeat = currentEnergy > threshold && currentEnergy > previousEnergy
        
        if (isBeat) {
            val currentTime = System.currentTimeMillis()
            
            // BPM hesapla
            if (lastBeatTime > 0) {
                val delta = (currentTime - lastBeatTime) / 1000.0f
                if (delta > 0.3f && delta < 2.0f) { // 30-200 BPM arası
                    val instantBPM = 60f / delta
                    currentBPM = currentBPM * 0.8f + instantBPM * 0.2f // Smoothing
                }
            }
            
            lastBeatTime = currentTime
            peakTimes.add(currentTime)
            
            // Eski peak'leri temizle (2 saniyeden eski)
            peakTimes.removeAll { it < currentTime - 2000 }
        }
        
        previousEnergy = currentEnergy
        
        return if (isBeat) 1.0f else 0.0f
    }
    
    /**
     * Spectral centroid hesaplama (basit versiyon)
     */
    private fun calculateSpectralCentroid(samples: FloatArray): Float {
        // Gerçek uygulamada FFT gerekli
        // Basit approximation: yüksek ZCR = yüksek centroid
        val zcr = calculateZeroCrossingRate(samples)
        return zcr * SAMPLE_RATE / 4
    }
    
    /**
     * Ortam sesine göre skala önerisi
     */
    fun recommendScale(analysis: AudioAnalysis): ScaleRecommendation {
        val mood = determineMood(analysis)
        
        return when {
            // Yüksek enerji, parlak ses -> Major
            analysis.rms > 0.3f && analysis.spectralCentroid > 2000f -> {
                ScaleRecommendation(SCALE_PENTATONIC_MAJOR, 60, 0.8f, "Energetic")
            }
            
            // Düşük enerji, karanlık ses -> Minor
            analysis.rms < 0.1f && analysis.spectralCentroid < 1000f -> {
                ScaleRecommendation(SCALE_PENTATONIC_MINOR, 48, 0.8f, "Melancholic")
            }
            
            // Orta enerji, dengeli -> Dorian
            else -> {
                ScaleRecommendation(SCALE_DORIAN, 55, 0.6f, "Balanced")
            }
        }
    }
    
    /**
     * Ses analizine göre duygu durumu belirleme
     */
    private fun determineMood(analysis: AudioAnalysis): String {
        return when {
            analysis.rms > 0.4f && analysis.spectralCentroid > 3000f -> "Excited"
            analysis.rms > 0.2f && analysis.spectralCentroid > 1500f -> "Happy"
            analysis.rms < 0.1f && analysis.spectralCentroid < 800f -> "Sad"
            analysis.rms < 0.15f && analysis.zeroCrossingRate < 0.05f -> "Calm"
            else -> "Neutral"
        }
    }
    
    /**
     * Mevcut BPM'i döndür
     */
    fun getCurrentBPM(): Float = currentBPM
    
    /**
     * Fraktal pulsasyon için beat senkronizasyon sinyali
     */
    fun getBeatSyncSignal(): Float {
        if (lastBeatTime == 0L) return 0f
        
        val timeSinceBeat = (System.currentTimeMillis() - lastBeatTime) / 1000f
        val beatDuration = 60f / (if (currentBPM > 0) currentBPM else 120f)
        
        // Beat'ten sonraki zamanın normalize edilmiş hali (0-1 arası sinüs dalgası)
        val phase = (timeSinceBeat / beatDuration) * 2f * PI
        return (sin(phase) + 1f) / 2f
    }
    
    /**
     * Ses verisini buffer'a ekle
     */
    fun addAudioSample(sample: Float) {
        audioBuffer[bufferPosition++] = sample
        
        if (bufferPosition >= BUFFER_SIZE) {
            bufferPosition = 0
            // Buffer doldu, analiz edilebilir
        }
    }
    
    /**
     * Reset
     */
    fun reset() {
        energyHistory.clear()
        peakTimes.clear()
        lastBeatTime = 0L
        currentBPM = 0f
        previousEnergy = 0f
        bufferPosition = 0
        audioBuffer.fill(0f)
    }
}

/**
 * MIDI nota numaralarını frekansa çevirme
 */
fun midiToFrequency(midiNote: Int): Float {
    return 440f * exp((midiNote - 69) / 12.0 * ln(2.0)).toFloat()
}

private fun ln(value: Double): Double = Math.log(value)
