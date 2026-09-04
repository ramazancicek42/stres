package com.aura.livewallpaper.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Prosedürel ambient müzik üreten sınıf
 * Gerçek AI yerine algoritmik/generative music tekniği kullanır
 * Brian Eno tarzı ambient drone sesleri üretir
 */
class GenerativeAudioEngine {
    
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var synthesisThread: Thread? = null
    
    // Ses parametreleri
    private val sampleRate = 44100
    private var baseFrequency = 220.0 // A3 nota
    private var volume = 0.3f
    
    // Pentatonik skala frekansları (A minor pentatonic)
    private val pentatonicFrequencies = listOf(220.0, 261.63, 293.66, 329.63, 392.00, 440.0)
    
    private var currentNoteIndex = 0
    private var noteDuration = 4000L // ms
    private var lastNoteTime = 0L
    
    // Filtre parametreleri (ışık seviyesine bağlı modülasyon için)
    private var filterCutoff = 0.8f
    
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
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
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
        this.filterCutoff = cutoff.coerceIn(0.1f, 1f)
    }
    
    fun triggerNote(frequency: Double? = null) {
        val freq = frequency ?: pentatonicFrequencies.random()
        playNote(freq)
    }
    
    private fun playNote(frequency: Double) {
        listener?.onNotePlayed(frequency)
        
        val duration = 2000L // 2 saniye
        val samples = (sampleRate * duration / 1000).toInt()
        val buffer = ShortArray(samples)
        
        val attackSamples = (samples * 0.1).toInt()
        val decaySamples = (samples * 0.3).toInt()
        
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            
            // ADSR envelope
            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i < samples - decaySamples -> 1.0
                else -> (samples - i).toDouble() / decaySamples
            }
            
            // Temel dalga (sine + triangle karışımı)
            var sample = sin(2 * PI * frequency * t) * 0.5
            sample += sin(2 * PI * frequency * 2 * t) * 0.2 // Harmonik
            
            // Low-pass filter efekti (basitçe yüksek frekansları azalt)
            sample *= filterCutoff + 0.2
            
            // Normalize ve 16-bit'e çevir
            val amplified = sample * envelope * volume * 32767
            buffer[i] = amplified.toInt().coerceIn(-32768, 32767).toShort()
        }
        
        audioTrack?.write(buffer, 0, buffer.size)
    }
    
    private fun generateAmbientLoop() {
        while (isPlaying && !Thread.interrupted()) {
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastNoteTime > noteDuration) {
                // Rastgele ama kurallı nota seçimi (pentatonik skala içinde)
                val nextNoteIndex = (currentNoteIndex + (1..3).random()) % pentatonicFrequencies.size
                currentNoteIndex = nextNoteIndex
                
                val frequency = pentatonicFrequencies[nextNoteIndex]
                playNote(frequency)
                
                lastNoteTime = currentTime
            }
            
            Thread.sleep(100)
        }
    }
}
