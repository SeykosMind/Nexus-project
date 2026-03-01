package com.nexus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nexus.ui.components.HudButton
import com.nexus.ui.components.HudGrid
import com.nexus.ui.components.HudPanel
import com.nexus.ui.theme.NexusTheme
import com.nexus.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    vm: SettingsViewModel = hiltViewModel()
) {
    val colors = NexusTheme.colors
    val settings by vm.settings.collectAsState()

    Box(Modifier.fillMaxSize().background(colors.background)) {
        HudGrid(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = colors.primary)
                }
                Text("NEXUS CONFIG", style = NexusTheme.typography.displayMedium, color = colors.primary)
            }

            // API Settings
            HudPanel(title = "LOCAL AI API") {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HudTextField("API HOST", settings.apiHost, onValueChange = { vm.updateHost(it) })
                    HudTextField("PORT", settings.apiPort, onValueChange = { vm.updatePort(it) }, keyboardType = KeyboardType.Number)
                    HudTextField("MODEL", settings.modelName, onValueChange = { vm.updateModel(it) })
                    Text("Connection: ${settings.apiHost}:${settings.apiPort}", style = NexusTheme.typography.labelSmall, color = colors.secondary)
                    HudButton("TEST CONNECTION", onClick = { vm.testConnection() }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Folders to watch
            HudPanel(title = "MONITORED SECTORS") {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    settings.watchedFolders.forEach { folder ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("▶ $folder", style = NexusTheme.typography.bodySmall, color = colors.onBackground, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.removeFolder(folder) }) {
                                Text("REMOVE", style = NexusTheme.typography.labelSmall, color = colors.accent)
                            }
                        }
                    }
                    HudButton("ADD FOLDER", onClick = { vm.pickFolder() }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Index settings
            HudPanel(title = "INDEX PARAMETERS") {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("AUTO-SYNC", style = NexusTheme.typography.bodySmall, color = colors.onBackground)
                        Switch(
                            checked = settings.autoSync,
                            onCheckedChange = { vm.toggleAutoSync(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primary.copy(alpha = 0.3f))
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("INCLUDE IMAGES (OCR)", style = NexusTheme.typography.bodySmall, color = colors.onBackground)
                        Switch(
                            checked = settings.includeImages,
                            onCheckedChange = { vm.toggleImages(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primary.copy(alpha = 0.3f))
                        )
                    }
                }
            }

            HudButton("SAVE & APPLY", onClick = { vm.save(); navController.popBackStack() }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun HudTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val colors = NexusTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = NexusTheme.typography.labelSmall, color = colors.onSurface) },
        textStyle = NexusTheme.typography.bodyMedium.copy(color = colors.primary),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.primaryDim.copy(alpha = 0.4f),
            cursorColor = colors.primary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        singleLine = true
    )
}
