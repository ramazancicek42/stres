package com.aura.livewallpaper.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.livewallpaper.meditation.MeditationSessionManager
import com.aura.livewallpaper.meditation.BreathingGuide
import com.aura.livewallpaper.meditation.SessionHistoryActivity
import com.aura.livewallpaper.util.AuraPreferences
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Easing fonksiyonları
private val EaseInOutSine = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

/**
 * Jetpack Compose ile ayarlar ekranı
 */
class SettingsActivity : ComponentActivity() {
    
    private lateinit var preferences: AuraPreferences
    private lateinit var meditationSessionManager: MeditationSessionManager
    private lateinit var breathingGuide: BreathingGuide
    
    // Mikrofon izni için launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // İzin verilmezse sessiz modu aktif et
            preferences.silentMode = true
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferences = AuraPreferences(this)
        meditationSessionManager = MeditationSessionManager(this)
        breathingGuide = BreathingGuide()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    SettingsScreen(
                        preferences = preferences,
                        meditationSessionManager = meditationSessionManager,
                        breathingGuide = breathingGuide,
                        onRequestAudioPermission = {
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: AuraPreferences,
    meditationSessionManager: MeditationSessionManager,
    breathingGuide: BreathingGuide,
    onRequestAudioPermission: () -> Unit
) {
    val context = LocalContext.current
    var lightSensitivity by remember { mutableStateOf(preferences.lightSensitivity) }
    var audioSensitivity by remember { mutableStateOf(preferences.audioSensitivity) }
    var silentMode by remember { mutableStateOf(preferences.silentMode) }
    var powerSaverMode by remember { mutableStateOf(preferences.powerSaverMode) }
    var colorPaletteIndex by remember { mutableStateOf(preferences.colorPaletteIndex) }
    var fpsLimit by remember { mutableStateOf(preferences.fpsLimit) }
    var autoPaletteEnabled by remember { mutableStateOf(preferences.autoPaletteEnabled) }
    var hapticEnabled by remember { mutableStateOf(preferences.hapticEnabled) }
    var accessibilityMode by remember { mutableStateOf(preferences.accessibilityMode) }
    
    // Meditasyon durumları
    val isMeditating by meditationSessionManager.isMeditating.collectAsState()
    val sessionDuration by meditationSessionManager.sessionDuration.collectAsState()
    val currentStressLevel by meditationSessionManager.currentStressLevel.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Aura Ayarları",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // Meditasyon Bölümü
        MeditationSection(
            isMeditating = isMeditating,
            sessionDuration = sessionDuration,
            currentStressLevel = currentStressLevel,
            sessionManager = meditationSessionManager,
            breathingGuide = breathingGuide,
            onViewHistory = {
                context.startActivity(Intent(context, SessionHistoryActivity::class.java))
            }
        )
        
        Divider()
        
        // Işık Hassasiyeti Slider
        Column {
            Text(
                text = "Işık Hassasiyeti: ${(lightSensitivity * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = lightSensitivity,
                onValueChange = {
                    lightSensitivity = it
                    preferences.lightSensitivity = it
                },
                valueRange = 0f..1f,
                steps = 9
            )
        }
        
        // Ses Hassasiyeti Slider (sessiz mod değilse)
        if (!silentMode) {
            Column {
                Text(
                    text = "Ses Hassasiyeti: ${(audioSensitivity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge
                )
                Slider(
                    value = audioSensitivity,
                    onValueChange = {
                        audioSensitivity = it
                        preferences.audioSensitivity = it
                    },
                    valueRange = 0f..1f,
                    steps = 9
                )
            }
        }
        
        // Sessiz Mod Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sessiz Mod",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = silentMode,
                onCheckedChange = {
                    silentMode = it
                    preferences.silentMode = it
                    
                    // Sessiz mod kapatılıyorsa izin iste
                    if (!it) {
                        onRequestAudioPermission()
                    }
                }
            )
        }
        
        // Pil Tasarrufu Modu Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pil Tasarrufu Modu",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = powerSaverMode,
                onCheckedChange = {
                    powerSaverMode = it
                    preferences.powerSaverMode = it
                    
                    // Pil tasarrufu modunda FPS'i düşür
                    if (it) {
                        fpsLimit = 15
                        preferences.fpsLimit = 15
                    } else {
                        fpsLimit = 30
                        preferences.fpsLimit = 30
                    }
                }
            )
        }
        
        // Otomatik Palet Değişimi Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Otomatik Palet Değişimi",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = autoPaletteEnabled,
                onCheckedChange = {
                    autoPaletteEnabled = it
                    preferences.autoPaletteEnabled = it
                }
            )
        }
        
        // Haptic Feedback Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Titreşim Geri Bildirimi",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = hapticEnabled,
                onCheckedChange = {
                    hapticEnabled = it
                    preferences.hapticEnabled = it
                }
            )
        }
        
        // Erişilebilirlik Modu Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Erişilebilirlik Modu",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Epilepsi güvenli modu, azaltılmış hareket",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = accessibilityMode,
                onCheckedChange = {
                    accessibilityMode = it
                    preferences.accessibilityMode = it
                }
            )
        }
        
        Divider()
        
        // Renk Paleti Seçimi
        Text(
            text = "Renk Paleti",
            style = MaterialTheme.typography.titleMedium
        )
        
        val palettes = listOf(
            "Okyanus Mavisi", "Gün Batımı", "Orman Yeşili", "Mor Gece",
            "Sıcak Amber", "Gün Doğumu", "Kozmik", "Neon"
        )
        palettes.forEachIndexed { index, name ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (index == colorPaletteIndex),
                        onClick = {
                            colorPaletteIndex = index
                            preferences.colorPaletteIndex = index
                        },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (index == colorPaletteIndex),
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = name)
            }
        }
        
        Divider()
        
        // FPS Limiti Seçimi
        Text(
            text = "FPS Sınırı",
            style = MaterialTheme.typography.titleMedium
        )
        
        val fpsOptions = listOf(15, 30, 60)
        fpsOptions.forEach { fps ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (fps == fpsLimit),
                        onClick = {
                            fpsLimit = fps
                            preferences.fpsLimit = fps
                        },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (fps == fpsLimit),
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$fps FPS")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Bilgilendirme metni
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "İpucu",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Duvar kağıdı ortam ışığına ve sese duyarlıdır. Kamera kullanılmaz, sadece ışık sensörü ve mikrofon (isteğe bağlı) kullanılır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MeditationSection(
    isMeditating: Boolean,
    sessionDuration: Long,
    currentStressLevel: Float,
    sessionManager: MeditationSessionManager,
    breathingGuide: BreathingGuide,
    onViewHistory: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("mm:ss", Locale.getDefault()) }
    
    // Nefes fazı animasyonu
    var breathPhase by remember { mutableStateOf(BreathingGuide.BreathingPhase.IDLE) }
    var breathValue by remember { mutableStateOf(0f) }
    
    LaunchedEffect(isMeditating) {
        if (isMeditating) {
            while (isMeditating) {
                breathingGuide.update()
                breathPhase = breathingGuide.getCurrentPhase()
                breathValue = breathingGuide.getSmoothBreathValue()
                delay(50) // 20 FPS
            }
        } else {
            breathPhase = BreathingGuide.BreathingPhase.IDLE
            breathValue = 0f
        }
    }
    
    // Stres seviyesi rengi
    val stressColor = when {
        currentStressLevel < 0.3f -> Color(0xFF4CAF50) // Yeşil - düşük stres
        currentStressLevel < 0.5f -> Color(0xFFFFC107) // Sarı - orta stres
        currentStressLevel < 0.7f -> Color(0xFFFF9800) // Turuncu - yüksek stres
        else -> Color(0xFFF44336) // Kırmızı - çok yüksek stres
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMeditating) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Meditasyon",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Süre göstergesi
            Text(
                text = timeFormatter.format(Date(sessionDuration)),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Nefes rehberi göstergesi
            if (isMeditating) {
                BreathingGuideIndicator(
                    phase = breathPhase,
                    breathValue = breathValue
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Stres seviyesi göstergesi
            if (isMeditating && currentStressLevel > 0) {
                StressLevelIndicator(
                    stressLevel = currentStressLevel,
                    stressColor = stressColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Başlat/Durdur butonu
            Button(
                onClick = {
                    if (isMeditating) {
                        sessionManager.stopSession()
                    } else {
                        sessionManager.startSession()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMeditating) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isMeditating) "Durdur" else "Meditasyona Başla",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Geçmiş butonu
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Geçmiş Seanslar")
            }
        }
    }
}

@Composable
fun BreathingGuideIndicator(
    phase: BreathingGuide.BreathingPhase,
    breathValue: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nefes dairesi
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (phase) {
                    BreathingGuide.BreathingPhase.INHALE -> "Al"
                    BreathingGuide.BreathingPhase.HOLD -> "Tut"
                    BreathingGuide.BreathingPhase.EXHALE -> "Ver"
                    BreathingGuide.BreathingPhase.IDLE -> "Hazır"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Nefes fazı açıklaması
        Text(
            text = when (phase) {
                BreathingGuide.BreathingPhase.INHALE -> "4 saniye nefes al"
                BreathingGuide.BreathingPhase.HOLD -> "7 saniye tut"
                BreathingGuide.BreathingPhase.EXHALE -> "8 saniye ver"
                BreathingGuide.BreathingPhase.IDLE -> "Başlamak için butona bas"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StressLevelIndicator(
    stressLevel: Float,
    stressColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stres Seviyesi",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(stressLevel * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = stressColor
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Stres çubuğu
        @Suppress("DEPRECATION")
        LinearProgressIndicator(
            progress = stressLevel,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = stressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Stres açıklaması
        Text(
            text = when {
                stressLevel < 0.3f -> "Düşük stres - harika!"
                stressLevel < 0.5f -> "Orta stres - nefesine odaklan"
                stressLevel < 0.7f -> "Yüksek stres - derin nefes al"
                else -> "Çok yüksek stres - lütfen rahatla"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
