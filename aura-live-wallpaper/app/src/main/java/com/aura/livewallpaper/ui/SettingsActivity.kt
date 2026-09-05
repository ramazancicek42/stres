package com.aura.livewallpaper.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aura.livewallpaper.util.AuraPreferences

/**
 * Jetpack Compose ile ayarlar ekranı
 */
class SettingsActivity : ComponentActivity() {
    
    private lateinit var preferences: AuraPreferences
    
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
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    SettingsScreen(
                        preferences = preferences,
                        onRequestAudioPermission = {
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    preferences: AuraPreferences,
    onRequestAudioPermission: () -> Unit
) {
    val context = LocalContext.current
    var lightSensitivity by remember { mutableStateOf(preferences.lightSensitivity) }
    var audioSensitivity by remember { mutableStateOf(preferences.audioSensitivity) }
    var silentMode by remember { mutableStateOf(preferences.silentMode) }
    var powerSaverMode by remember { mutableStateOf(preferences.powerSaverMode) }
    var colorPaletteIndex by remember { mutableStateOf(preferences.colorPaletteIndex) }
    var fpsLimit by remember { mutableStateOf(preferences.fpsLimit) }
    
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
                    text = "💡 İpucu",
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
