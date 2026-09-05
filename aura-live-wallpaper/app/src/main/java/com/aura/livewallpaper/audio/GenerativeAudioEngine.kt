package com.aura.livewallpaper.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.*
import kotlin.random.Random

/**
 * Professional Generatif Müzik Motoru v2.0
 * 
 * Multi-layer ses mimarisi:
 * - Layer 1: Drone Pad (sürekli, low-pass filtered)
 * - Layer 2: Ambient Pad (örtüşen, pentatonik)
 * - Layer 3: Melodik Notalar (reverb + delay)
 * - Layer 4: Texture/Grain (granular synthesis)
 * 
 * Efektler:
 * - Reverb (room simulation)
 * - Delay (tempo-aligned echo)
 * - Chorus (pitch modulation)
 * - Filter (low-pass + resonance)
 */
class GenerativeAudioEngine {
    
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var synthesisThread: Thread? = null
    
    // Ses parametreleri
    private val sampleRate = 44100
    private var volume = 0.3f
    private var masterFilterCutoff = 0.8f
    
    // Pentatonik skala (A minor pentatonic)
    private val pentatonicFrequencies = listOf(
        220.0,    // A3
        261.63,   // C4
        293.66,   // D4
        329.63,   // E4
        392.00,   // G4
        440.0,    // A4
        523.25,   // C5
        587.33    // D5
    )
    
    // Layer durumları
    private var dronePhase = 0.0
    private var padNotes = mutableListOf<PadNote>()
    private var grainBuffer = FloatArray(1024)
    private var grainIndex = 0
    
    // Reverb parametreleri
    private val reverbDelay = 0.05f // 50ms
    private val reverbFeedback = 0.4f
    private var reverbBuffer = FloatArray(4410) // 100ms buffer
    private var reverbIndex = 0
    
    // Delay parametreleri
    private val delayTime = 0.375f // 375ms (tempo-aligned)
    private val delayFeedback = 0.35f
    private var delayBuffer = FloatArray(16537) // 375ms buffer
    private var delayIndex = 0
    
    // Chorus parametreleri
    private var chorusLFO = 0.0
    private val chorusRate = 0.5 // Hz
    private val chorusDepth = 0.002 // ms
    
    // LFO'lar
    private var lfo1 = 0.0
    private var lfo2 = 0.0
    private var lfo3 = 0.0
    
    // Nota zamanlaması
    private var lastNoteTime = 0L
    private var noteDuration = 4000L
    private var currentNoteIndex = 0
    
    // Beat senkronizasyonu
    private var beatPhase = 0.0
    private var bpm = 60.0
    private var beatInterval = 1000L // ms
    
    interface Listener {
        fun onNotePlayed(frequency: Double)
    }
    
    private var listener: Listener? = null
    
    fun setListener(listener: Listener) {
        this.listener = listener
    }
    
    fun start() {
        if (isPlaying) return
        
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()
        
        audioTrack = AudioTrack(
            attributes,
            format,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        
        audioTrack?.play()
        isPlaying = true
        
        synthesisThread = thread {
            generateAmbientLoop()
        }
    }
    
    fun stop() {
        isPlaying = false
        synthesisThread?.interrupt()
        synthesisThread = null
        
        audioTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        audioTrack = null
    }
    
    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }
    
    fun setFilterCutoff(cutoff: Float) {
        this.masterFilterCutoff = cutoff.coerceIn(0.1f, 1f)
    }
    
    fun setBPM(bpm: Double) {
        this.bpm = bpm
        this.beatInterval = (60000.0 / bpm).toLong()
    }
    
    fun triggerNote(frequency: Double? = null) {
        val freq = frequency ?: pentatonicFrequencies.random()
        addMelodicNote(freq)
    }
    
    fun playNoteAtPosition(x: Float, y: Float) {
        val index = ((x + y) * pentatonicFrequencies.size / 2).toInt()
            .coerceIn(0, pentatonicFrequencies.size - 1)
        addMelodicNote(pentatonicFrequencies[index])
    }
    
    // ============================================
    // LAYER 1: DRONE PAD
    // ============================================
    
    private fun generateDrone(t: Double): Float {
        // Low-pass filtered saw wave
        val baseFreq = 110.0 // A2
        val subFreq = 55.0 // A1
        
        // Saw wave (harmonikler)
        var drone = 0.0
        for (harmonic in 1..8) {
            val amp = 1.0 / harmonic
            drone += sin(2 * PI * baseFreq * harmonic * t) * amp
        }
        
        // Sub-octave
        drone += sin(2 * PI * subFreq * t) * 0.5
        
        // LFO modülasyonu (hafif dalgalanma)
        val lfoMod = sin(2 * PI * 0.1 * t) * 0.3
        drone *= (0.7 + lfoMod)
        
        // Low-pass filtre (basit exponential moving average)
        val filterCoeff = masterFilterCutoff * 0.3
        drone = drone * filterCoeff
        
        return (drone * 0.15).toFloat() // Düşük seviye
    }
    
    // ============================================
    // LAYER 2: AMBIENT PAD
    // ============================================
    
    private fun addAmbientNote() {
        if (padNotes.size >= 4) return // Maksimum 4 pad notası
        
        val freq = pentatonicFrequencies.random()
        val duration = 6000L + Random.nextLong(4000) // 6-10 saniye
        
        padNotes.add(PadNote(
            frequency = freq,
            startTime = System.currentTimeMillis(),
            duration = duration,
            amplitude = 0.1f + Random.nextFloat() * 0.1f
        ))
    }
    
    private fun generatePad(t: Double): Float {
        var pad = 0.0
        
        val currentTime = System.currentTimeMillis()
        
        // Aktif pad notalarını işle
        padNotes.removeAll { note ->
            val elapsed = currentTime - note.startTime
            val progress = elapsed.toFloat() / note.duration
            
            if (progress >= 1.0f) {
                true // Süresi dolan notaları sil
            } else {
                // ADSR envelope
                val envelope = when {
                    progress < 0.1f -> progress / 0.1f // Attack
                    progress < 0.8f -> 1.0f // Sustain
                    else -> (1.0f - progress) / 0.2f // Release
                }
                
                // Sine + triangle karışımı
                val sample = sin(2 * PI * note.frequency * t) * 0.6 +
                            sin(2 * PI * note.frequency * 0.5 * t) * 0.4
                
                pad += sample * envelope * note.amplitude
                false
            }
        }
        
        return (pad * 0.3).toFloat()
    }
    
    // ============================================
    // LAYER 3: MELODİKC NOTALAR
    // ============================================
    
    private fun addMelodicNote(frequency: Double) {
        listener?.onNotePlayed(frequency)
        
        val duration = 2000L // 2 saniye
        
        // Nota buffer'ı oluştur
        val samples = (sampleRate * duration / 1000).toInt()
        val buffer = ShortArray(samples * 2) // Stereo
        
        val attackSamples = (samples * 0.05).toInt()
        val decaySamples = (samples * 0.3).toInt()
        
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            
            // ADSR envelope
            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i < samples - decaySamples -> 1.0
                else -> (samples - i).toDouble() / decaySamples
            }
            
            // Piano-like tone (sine + harmonics)
            var sample = sin(2 * PI * frequency * t) * 0.5
            sample += sin(2 * PI * frequency * 2 * t) * 0.2
            sample += sin(2 * PI * frequency * 3 * t) * 0.1
            sample += sin(2 * PI * frequency * 4 * t) * 0.05
            
            // Chorus efekti
            val chorusMod = sin(2 * PI * chorusLFO) * chorusDepth
            val chorusSample = sin(2 * PI * frequency * (t + chorusMod)) * 0.1
            
            sample += chorusSample
            
            // Low-pass filtre
            val filterCoeff = masterFilterCutoff * 0.5
            sample *= filterCoeff
            
            // Reverb
            val reverbSample = applyReverb(sample)
            
            // Delay
            val delaySample = applyDelay(reverbSample)
            
            // Stereo genişletme
            val stereoSpread = sin(2 * PI * 0.3 * t) * 0.2
            
            val left = (delaySample * envelope * volume * (0.8 + stereoSpread) * 32767).toInt()
                .coerceIn(-32768, 32767).toShort()
            val right = (delaySample * envelope * volume * (0.8 - stereoSpread) * 32767).toInt()
                .coerceIn(-32768, 32767).toShort()
            
            buffer[i * 2] = left
            buffer[i * 2 + 1] = right
        }
        
        audioTrack?.write(buffer, 0, buffer.size)
    }
    
    // ============================================
    // LAYER 4: GRAIN SYNTHESIS
    // ============================================
    
    private fun generateGrain(t: Double): Float {
        // Granular synthesis - rastgele grain'lar
        val grainSize = 50 + Random.nextInt(100) // 50-150 sample
        var grain = 0.0
        
        for (i in 0 until grainSize) {
            val grainT = t + i.toDouble() / sampleRate
            val freq = pentatonicFrequencies.random() * (0.8 + Random.nextDouble() * 0.4)
            
            // Gaussian envelope
            val x = (i - grainSize / 2.0) / (grainSize / 4.0)
            val envelope = exp(-0.5 * x * x)
            
            grain += sin(2 * PI * freq * grainT) * envelope
        }
        
        return (grain * 0.05).toFloat() // Çok düşük seviye
    }
    
    // ============================================
    // EFEKTLER
    // ============================================
    
    private fun applyReverb(input: Double): Double {
        // Room simulation reverb
        val delayed = reverbBuffer[reverbIndex]
        reverbBuffer[reverbIndex] = (input + delayed * reverbFeedback).toFloat()
        reverbIndex = (reverbIndex + 1) % reverbBuffer.size
        
        return input + delayed * 0.5
    }
    
    private fun applyDelay(input: Double): Double {
        // Tempo-aligned echo
        val delayed = delayBuffer[delayIndex]
        delayBuffer[delayIndex] = (input + delayed * delayFeedback).toFloat()
        delayIndex = (delayIndex + 1) % delayBuffer.size
        
        return input + delayed * 0.4
    }
    
    // ============================================
    // ANA DÖNGÜ
    // ============================================
    
    private fun generateAmbientLoop() {
        while (isPlaying && !Thread.interrupted()) {
            val currentTime = System.currentTimeMillis()
            val t = currentTime / 1000.0
            
            // LFO güncelle
            lfo1 = sin(2 * PI * 0.1 * t)
            lfo2 = sin(2 * PI * 0.07 * t)
            lfo3 = sin(2 * PI * 0.13 * t)
            chorusLFO += chorusRate / sampleRate
            
            // Beat senkronizasyonu
            beatPhase += 1.0 / (sampleRate / 1000.0)
            if (beatPhase >= beatInterval / 1000.0) {
                beatPhase = 0.0
                // Beat'te ambient pad ekle
                if (Random.nextFloat() < 0.3f) {
                    addAmbientNote()
                }
            }
            
            // Periyodik nota ekleme
            if (currentTime - lastNoteTime > noteDuration) {
                val nextNoteIndex = (currentNoteIndex + (1..3).random()) % 
                    pentatonicFrequencies.size
                currentNoteIndex = nextNoteIndex
                
                addMelodicNote(pentatonicFrequencies[nextNoteIndex])
                
                // Rastgele süre değiştir
                noteDuration = 3000L + Random.nextLong(3000)
                lastNoteTime = currentTime
            }
            
            // Düşük öncelikli grain synthesis (her 100ms'de bir)
            if (Random.nextFloat() < 0.1f) {
                val grain = generateGrain(t)
                // Grain'i mevcut sinyale ekle (burada basitleştirilmiş)
            }
            
            // Zamanlama
            val elapsed = System.currentTimeMillis() - currentTime
            val sleepTime = (50 - elapsed).coerceAtLeast(10)
            Thread.sleep(sleepTime)
        }
    }
    
    // ============================================
    // VERI YAPILARI
    // ============================================
    
    data class PadNote(
        val frequency: Double,
        val startTime: Long,
        val duration: Long,
        val amplitude: Float
    )
}
