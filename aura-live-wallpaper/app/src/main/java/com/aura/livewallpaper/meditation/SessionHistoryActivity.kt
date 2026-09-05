package com.aura.livewallpaper.meditation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Meditasyon geçmişi ekranı
 */
class SessionHistoryActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sessionManager = MeditationSessionManager(this)
        
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) {
                    darkColorScheme()
                } else {
                    lightColorScheme()
                }
            ) {
                SessionHistoryScreen(
                    sessionManager = sessionManager,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    sessionManager: MeditationSessionManager,
    onBack: () -> Unit
) {
    val stats by sessionManager.stats.collectAsState()
    val sessions by sessionManager.sessions.collectAsState()
    
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Meditasyon Geçmişi",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // İstatistikler kartı
            item {
                StatsCard(stats = stats)
            }
            
            // Başarı rozetleri
            item {
                AchievementsSection(stats = stats)
            }
            
            // Geçmiş seanslar
            item {
                Text(
                    text = "Son Seanslar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            if (sessions.isEmpty()) {
                item {
                    EmptyStateCard()
                }
            } else {
                items(sessions.take(20)) { session ->
                    SessionCard(
                        session = session,
                        dateFormatter = dateFormatter
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(stats: MeditationSessionManager.MeditationStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Genel İstatistikler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${stats.totalSessions}",
                    label = "Toplam Seans",
                    icon = Icons.Default.List
                )
                StatItem(
                    value = formatDuration(stats.totalDurationMs),
                    label = "Toplam Süre",
                    icon = Icons.Default.DateRange
                )
                StatItem(
                    value = "${stats.currentStreak}",
                    label = "Seri",
                    icon = Icons.Default.Star
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stats.getFormattedAverageDuration(),
                    label = "Ortalama Süre",
                    icon = Icons.Default.Check
                )
                StatItem(
                    value = stats.getFormattedBestStressReduction(),
                    label = "En İyi Azaltma",
                    icon = Icons.Default.Star
                )
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AchievementsSection(stats: MeditationSessionManager.MeditationStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Başarılar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AchievementBadge(
                    title = "İlk Adım",
                    description = "İlk seansını tamamla",
                    achieved = stats.totalSessions >= 1,
                    modifier = Modifier.weight(1f)
                )
                AchievementBadge(
                    title = "Düzenli",
                    description = "3 gün seri yap",
                    achieved = stats.currentStreak >= 3 || stats.bestStreak >= 3,
                    modifier = Modifier.weight(1f)
                )
                AchievementBadge(
                    title = "Usta",
                    description = "10 seans tamamla",
                    achieved = stats.totalSessions >= 10,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AchievementBadge(
                    title = "Zen",
                    description = "1 saat toplam meditasyon",
                    achieved = stats.totalDurationMs >= 3600000,
                    modifier = Modifier.weight(1f)
                )
                AchievementBadge(
                    title = "Stres Ustası",
                    description = "%50 stres azaltma",
                    achieved = stats.bestStressReduction >= 0.5f,
                    modifier = Modifier.weight(1f)
                )
                AchievementBadge(
                    title = "Efsane",
                    description = "7 gün seri yap",
                    achieved = stats.bestStreak >= 7,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AchievementBadge(
    title: String,
    description: String,
    achieved: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (achieved) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val iconTint = if (achieved) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun SessionCard(
    session: MeditationSessionManager.MeditationSession,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tarih ve süre
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dateFormatter.format(Date(session.startTime)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Süre: ${formatDuration(session.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Stres değişim göstergesi
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val stressChange = 0.5f - session.averageStressLevel
                val isPositive = stressChange > 0
                
                Text(
                    text = if (isPositive) "↓ ${"%.0f".format(stressChange * 100)}%" else "↑ ${"%.0f".format(-stressChange * 100)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    text = "Stres ${if (isPositive) "azaldı" else "arttı"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Henüz meditasyon seansın yok",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = "İlk meditasyon seansını başlatarak başla!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        minutes > 0 -> String.format("%d:%02d", minutes, seconds)
        else -> String.format("%d sn", seconds)
    }
}

@Composable
private fun isSystemInDarkTheme(): Boolean {
    // Basit karanlık tema kontrolü
    return false
}
