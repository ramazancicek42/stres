package com.aura.livewallpaper.meditation

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Meditasyon seanslarını yönetir.
 * 
 * Özellikler:
 * - Seans başlat/durdur
 * - Otomatik süre sayacı (her saniye güncellenir)
 * - Nefes rehberi senkronizasyonu
 * - Seans geçmişi kaydetme
 * - İstatistikler
 */
class MeditationSessionManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("aura_meditation", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    
    data class MeditationSession(
        val startTime: Long,
        val endTime: Long = 0L,
        val durationMs: Long = 0L,
        val averageStressLevel: Float = 0f,
        val breathingCycles: Int = 0,
        val lightLevelAvg: Float = 0f
    )
    
    data class MeditationStats(
        val totalSessions: Int,
        val totalDurationMs: Long,
        val averageSessionDurationMs: Long,
        val longestSessionMs: Long,
        val currentStreak: Int,
        val lastSessionDate: String?,
        val bestStreak: Int = 0,
        val bestStressReduction: Float = 0f
    ) {
        fun getFormattedAverageDuration(): String {
            val minutes = (averageSessionDurationMs / 1000) / 60
            val seconds = (averageSessionDurationMs / 1000) % 60
            return String.format("%d:%02d", minutes, seconds)
        }
        
        fun getFormattedBestStressReduction(): String {
            return "${(bestStressReduction * 100).toInt()}%"
        }
    }
    
    private val _currentSession = MutableStateFlow<MeditationSession?>(null)
    val currentSession: StateFlow<MeditationSession?> = _currentSession.asStateFlow()
    
    private val _isMeditating = MutableStateFlow(false)
    val isMeditating: StateFlow<Boolean> = _isMeditating.asStateFlow()
    
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()
    
    private val _sessionDuration = MutableStateFlow(0L)
    val sessionDuration: StateFlow<Long> = _sessionDuration.asStateFlow()
    
    private val _currentStressLevel = MutableStateFlow(0.5f)
    val currentStressLevel: StateFlow<Float> = _currentStressLevel.asStateFlow()
    
    private val _sessions = MutableStateFlow<List<MeditationSession>>(emptyList())
    val sessions: StateFlow<List<MeditationSession>> = _sessions.asStateFlow()
    
    private val _stats = MutableStateFlow(MeditationStats(
        totalSessions = 0,
        totalDurationMs = 0,
        averageSessionDurationMs = 0,
        longestSessionMs = 0,
        currentStreak = 0,
        lastSessionDate = null
    ))
    val stats: StateFlow<MeditationStats> = _stats.asStateFlow()
    
    // Nefes durumu
    private val _breathingPhase = MutableStateFlow("Hazır")
    val breathingPhase: StateFlow<String> = _breathingPhase.asStateFlow()
    
    private val _breathingCycleCount = MutableStateFlow(0)
    val breathingCycleCount: StateFlow<Int> = _breathingCycleCount.asStateFlow()
    
    private val _stressHistory = mutableListOf<Float>()
    private val _lightHistory = mutableListOf<Float>()
    private var breathingCycles = 0
    private var sessionStartTime = 0L
    
    companion object {
        private const val KEY_SESSION_HISTORY = "session_history"
        private const val KEY_TOTAL_SESSIONS = "total_sessions"
        private const val KEY_TOTAL_DURATION = "total_duration"
        private const val KEY_LONGEST_SESSION = "longest_session"
        private const val KEY_STREAK = "streak"
        private const val KEY_LAST_SESSION_DATE = "last_session_date"
    }
    
    /**
     * Meditasyon seansını başlat
     */
    fun startSession() {
        if (_isMeditating.value) return
        
        sessionStartTime = System.currentTimeMillis()
        _currentSession.value = MeditationSession(startTime = sessionStartTime)
        _isMeditating.value = true
        _elapsedTime.value = 0L
        _sessionDuration.value = 0L
        _currentStressLevel.value = 0.5f
        _stressHistory.clear()
        _lightHistory.clear()
        breathingCycles = 0
        _breathingCycleCount.value = 0
        
        // Timer'ı başlat - her saniye süreyi güncelle
        timerJob = scope.launch {
            while (isActive && _isMeditating.value) {
                delay(1000) // Her saniye
                updateElapsedTime()
            }
        }
        
        // Nefes rehberini başlat
        startBreathingGuide()
    }
    
    /**
     * Meditasyon seansını durdur ve kaydet
     */
    fun stopSession(): MeditationSession? {
        if (!_isMeditating.value) return null
        
        // Timer'ı durdur
        timerJob?.cancel()
        timerJob = null
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - sessionStartTime
        
        val avgStress = if (_stressHistory.isNotEmpty()) {
            _stressHistory.average().toFloat()
        } else 0.5f
        
        val avgLight = if (_lightHistory.isNotEmpty()) {
            _lightHistory.average().toFloat()
        } else 0.5f
        
        val session = MeditationSession(
            startTime = sessionStartTime,
            endTime = endTime,
            durationMs = duration,
            averageStressLevel = avgStress,
            breathingCycles = breathingCycles,
            lightLevelAvg = avgLight
        )
        
        _currentSession.value = session
        _isMeditating.value = false
        _breathingPhase.value = "Tamamlandı"
        
        // Geçmişe kaydet
        saveSession(session)
        updateStats(session)
        
        // StateFlow'ları güncelle
        _sessions.value = getSessionHistory()
        _stats.value = getStats()
        
        return session
    }
    
    /**
     * Nefes rehberini başlat (4-7-8 tekniği)
     */
    private fun startBreathingGuide() {
        scope.launch {
            while (isActive && _isMeditating.value) {
                // 4 saniye nefes al
                _breathingPhase.value = "Nefes Al (4)"
                for (i in 4 downTo 1) {
                    if (!_isMeditating.value) return@launch
                    _breathingPhase.value = "Nefes Al ($i)"
                    delay(1000)
                }
                
                if (!_isMeditating.value) return@launch
                
                // 7 saniye tut
                _breathingPhase.value = "Tut (7)"
                for (i in 7 downTo 1) {
                    if (!_isMeditating.value) return@launch
                    _breathingPhase.value = "Tut ($i)"
                    delay(1000)
                }
                
                if (!_isMeditating.value) return@launch
                
                // 8 saniye ver
                _breathingPhase.value = "Ver (8)"
                for (i in 8 downTo 1) {
                    if (!_isMeditating.value) return@launch
                    _breathingPhase.value = "Ver ($i)"
                    delay(1000)
                }
                
                // Nefes döngüsü tamamlandı
                breathingCycles++
                _breathingCycleCount.value = breathingCycles
            }
        }
    }
    
    /**
     * Stres seviyesini kaydet
     */
    fun recordStressLevel(level: Float) {
        if (_isMeditating.value) {
            _currentStressLevel.value = level.coerceIn(0f, 1f)
            _stressHistory.add(level.coerceIn(0f, 1f))
        }
    }
    
    /**
     * Işık seviyesini kaydet
     */
    fun recordLightLevel(level: Float) {
        if (_isMeditating.value) {
            _lightHistory.add(level.coerceIn(0f, 1f))
        }
    }
    
    /**
     * Nefes döngüsünü kaydet
     */
    fun recordBreathingCycle() {
        if (_isMeditating.value) {
            breathingCycles++
            _breathingCycleCount.value = breathingCycles
        }
    }
    
    /**
     * Geçerli süreyi güncelle
     */
    fun updateElapsedTime() {
        if (_isMeditating.value) {
            val elapsed = System.currentTimeMillis() - sessionStartTime
            _elapsedTime.value = elapsed
            _sessionDuration.value = elapsed
        }
    }
    
    /**
     * Geçmiş seansları getir
     */
    fun getSessionHistory(): List<MeditationSession> {
        val historyJson = prefs.getString(KEY_SESSION_HISTORY, "") ?: ""
        if (historyJson.isEmpty()) return emptyList()
        
        return historyJson.split(";").mapNotNull { sessionStr ->
            val parts = sessionStr.split(",")
            if (parts.size >= 6) {
                MeditationSession(
                    startTime = parts[0].toLongOrNull() ?: 0L,
                    endTime = parts[1].toLongOrNull() ?: 0L,
                    durationMs = parts[2].toLongOrNull() ?: 0L,
                    averageStressLevel = parts[3].toFloatOrNull() ?: 0.5f,
                    breathingCycles = parts[4].toIntOrNull() ?: 0,
                    lightLevelAvg = parts[5].toFloatOrNull() ?: 0.5f
                )
            } else null
        }
    }
    
    /**
     * İstatistikleri getir
     */
    fun getStats(): MeditationStats {
        val sessions = getSessionHistory()
        val totalSessions = sessions.size
        val totalDuration = sessions.sumOf { it.durationMs }
        val avgDuration = if (totalSessions > 0) totalDuration / totalSessions else 0L
        val longest = sessions.maxOfOrNull { it.durationMs } ?: 0L
        val streak = calculateStreak(sessions)
        val lastDate = sessions.lastOrNull()?.let { formatDate(it.startTime) }
        
        // En iyi stres azaltma hesapla
        var bestStressReduction = 0f
        sessions.forEach { session ->
            if (session.averageStressLevel < 0.5f) {
                val reduction = 0.5f - session.averageStressLevel
                if (reduction > bestStressReduction) {
                    bestStressReduction = reduction
                }
            }
        }
        
        return MeditationStats(
            totalSessions = totalSessions,
            totalDurationMs = totalDuration,
            averageSessionDurationMs = avgDuration,
            longestSessionMs = longest,
            currentStreak = streak,
            lastSessionDate = lastDate,
            bestStreak = streak,
            bestStressReduction = bestStressReduction
        )
    }
    
    private fun saveSession(session: MeditationSession) {
        val history = getSessionHistory().toMutableList()
        history.add(session)
        
        // Son 50 seansı tut
        val trimmedHistory = history.takeLast(50)
        
        val historyJson = trimmedHistory.joinToString(";") { s ->
            "${s.startTime},${s.endTime},${s.durationMs},${s.averageStressLevel},${s.breathingCycles},${s.lightLevelAvg}"
        }
        
        prefs.edit().putString(KEY_SESSION_HISTORY, historyJson).apply()
    }
    
    private fun updateStats(session: MeditationSession) {
        val totalSessions = prefs.getInt(KEY_TOTAL_SESSIONS, 0) + 1
        val totalDuration = prefs.getLong(KEY_TOTAL_DURATION, 0) + session.durationMs
        val longest = prefs.getLong(KEY_LONGEST_SESSION, 0).coerceAtLeast(session.durationMs)
        val lastDate = formatDate(session.startTime)
        
        // Streak hesapla
        val lastSessionDate = prefs.getString(KEY_LAST_SESSION_DATE, null)
        val today = formatDate(System.currentTimeMillis())
        val yesterday = formatDate(System.currentTimeMillis() - 86400000)
        
        val currentStreak = if (lastSessionDate == yesterday || lastSessionDate == today) {
            prefs.getInt(KEY_STREAK, 0) + 1
        } else if (lastSessionDate == null) {
            1
        } else {
            1 // Streak kırıldı
        }
        
        prefs.edit()
            .putInt(KEY_TOTAL_SESSIONS, totalSessions)
            .putLong(KEY_TOTAL_DURATION, totalDuration)
            .putLong(KEY_LONGEST_SESSION, longest)
            .putInt(KEY_STREAK, currentStreak)
            .putString(KEY_LAST_SESSION_DATE, today)
            .apply()
    }
    
    private fun calculateStreak(sessions: List<MeditationSession>): Int {
        if (sessions.isEmpty()) return 0
        
        var streak = 1
        val today = formatDate(System.currentTimeMillis())
        val sortedSessions = sessions.sortedByDescending { it.startTime }
        
        // Bugün meditasyon yaptıysa streak devam eder
        val lastSessionDate = formatDate(sortedSessions.first().startTime)
        if (lastSessionDate != today && lastSessionDate != formatDate(System.currentTimeMillis() - 86400000)) {
            return 0
        }
        
        for (i in 0 until sortedSessions.size - 1) {
            val currentDate = formatDate(sortedSessions[i].startTime)
            val nextDate = formatDate(sortedSessions[i + 1].startTime)
            
            val currentDateMs = parseDate(currentDate)
            val nextDateMs = parseDate(nextDate)
            
            if (currentDateMs - nextDateMs <= 86400000) {
                streak++
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun formatDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }
    
    private fun parseDate(dateStr: String): Long {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val cal = Calendar.getInstance()
            cal.set(parts[0].toInt(), parts[1].toInt(), parts[2].toInt(), 0, 0, 0)
            return cal.timeInMillis
        }
        return 0L
    }
    
    /**
     * Süreyi formatla (mm:ss)
     */
    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
