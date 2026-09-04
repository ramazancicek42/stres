package com.aura.livewallpaper.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

/**
 * Mikrofondan ortam sesi verisi okuyup analiz eden sınıf
 * SADECE ekran açıkken ve kullanıcı ana ekrandayken aktif olmalı
 */
class AudioAnalyzer(context: Context) {
    
    interface Listener {
        fun onAudioEnergyChanged(energy: Float, rms: Float)
    }
    
    private val context = context.applicationContext
    private var listener: Listener? = null
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var analysisThread: Thread? = null
    
    // Ses analizi parametreleri
    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2
    
    private var currentRms: Float = 0f
    private var currentEnergy: Float = 0f
    
    // Noise gate - sessiz ortamda rastgele tetiklenmeyi önler
    private val noiseGateThreshold = 0.02f
    
    val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    
    fun setListener(listener: Listener) {
        this.listener = listener
    }
    
    fun start() {
        if (!hasPermission || isRecording) return
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }
            
            audioRecord?.startRecording()
            isRecording = true
            
            analysisThread = thread {
                analyzeAudioLoop()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun stop() {
        isRecording = false
        analysisThread?.interrupt()
        analysisThread = null
        
        audioRecord?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        audioRecord = null
    }
    
    private fun analyzeAudioLoop() {
        val buffer = ShortArray(bufferSize / 2)
        
        while (isRecording && !Thread.interrupted()) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
            
            if (read > 0) {
                val rms = calculateRMS(buffer, read)
                
                // Noise gate uygula
                val gatedRms = if (rms > noiseGateThreshold) rms else 0f
                
                // Energy hesapla (0-1 aralığında)
                val energy = (gatedRms * 5f).coerceIn(0f, 1f)
                
                currentRms = gatedRms
                currentEnergy = energy
                
                listener?.onAudioEnergyChanged(energy, gatedRms)
            }
        }
    }
    
    private fun calculateRMS(buffer: ShortArray, size: Int): Float {
        var sum = 0.0
        for (i in 0 until size) {
            val sample = buffer[i] / 32768.0 // Normalize to -1..1
            sum += sample * sample
        }
        return Math.sqrt(sum / size).toFloat()
    }
    
    fun getCurrentEnergy(): Float = currentEnergy
    fun getCurrentRMS(): Float = currentRms
}
