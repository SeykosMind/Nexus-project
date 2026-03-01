package com.nexus.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nexus.ui.Screen
import com.nexus.ui.components.*
import com.nexus.ui.theme.NexusColors
import com.nexus.ui.theme.NexusTheme
import com.nexus.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    navController: NavController,
    vm: DashboardViewModel = hiltViewModel()
) {
    val colors = NexusTheme.colors
    val state by vm.uiState.collectAsState()

    // Boot sequence animation
    var booted by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (booted) 1f else 0f, tween(800), label = "boot")
    LaunchedEffect(Unit) { delay(300); booted = true }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Grid background
        HudGrid(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .alpha(alpha)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "NEXUS", style = NexusTheme.typography.displayLarge, color = colors.primary
                    )
                    Text(
                        "PERSONAL DOCUMENT INTELLIGENCE",
                        style = NexusTheme.typography.labelSmall,
                        color = colors.onSurface
                    )
                }
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Default.Settings, "settings", tint = colors.primary)
                }
            }

            // ── Radar + Stats ──────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadarPulse(
                    active = state.isIndexing,
                    size = 130.dp,
                    modifier = Modifier.padding(8.dp)
                )
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("DOCS INDEXED", "${state.totalDocs}", accent = colors.primary)
                    StatCard("LAST SCAN", state.lastScan, accent = colors.secondary)
                    StatCard("STORAGE", state.storageUsed, accent = colors.accent)
                }
            }

            // ── Indexing progress ──────────────────────────────────────
            if (state.isIndexing) {
                HudPanel(title = "ACTIVE SCAN") {
                    Column(Modifier.padding(12.dp)) {
                        HudProgressBar(state.indexingProgress, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        TypewriterText(
                            text = state.currentFile,
                            style = NexusTheme.typography.bodySmall,
                            color = colors.onSurface
                        )
                    }
                }
            }

            // ── Search shortcut ────────────────────────────────────────
            HudPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.Search.route) }
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, "search", tint = colors.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "QUERY INTELLIGENCE BASE...",
                        style = NexusTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                }
            }

            // ── Doc type breakdown ─────────────────────────────────────
            HudPanel(title = "DOCUMENT MATRIX") {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.docTypes.forEach { (ext, count) ->
                        DocTypeRow(ext, count, state.totalDocs)
                    }
                }
            }

            // ── Recent activity ────────────────────────────────────────
            HudPanel(title = "ACTIVITY LOG") {
                Column(Modifier.padding(8.dp)) {
                    state.recentActivity.forEach { log ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp, horizontal = 4.dp)) {
                            Text("▶", style = NexusTheme.typography.labelSmall, color = colors.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(log, style = NexusTheme.typography.bodySmall, color = colors.onSurface)
                        }
                    }
                }
            }

            // ── Action buttons ─────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HudButton(
                    text = if (state.isIndexing) "SCANNING..." else "FORCE SCAN",
                    onClick = { vm.startIndexing() },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isIndexing,
                    icon = Icons.Default.Refresh
                )
                HudButton(
                    text = "SEARCH",
                    onClick = { navController.navigate(Screen.Search.route) },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Search
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun DocTypeRow(ext: String, count: Int, total: Int) {
    val colors = NexusTheme.colors
    val fraction = if (total > 0) count.toFloat() / total else 0f
    val extColor = when (ext.lowercase()) {
        "pdf"  -> colors.accent
        "xlsx", "xls" -> colors.secondary
        "docx", "doc" -> colors.primary
        "pptx", "ppt" -> colors.warning
        else -> colors.onSurface
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(ext.uppercase().padEnd(5), style = NexusTheme.typography.labelSmall, color = extColor, modifier = Modifier.width(44.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(6.dp).background(colors.surfaceVariant)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(extColor.copy(alpha = 0.7f)))
        }
        Spacer(Modifier.width(8.dp))
        Text("$count", style = NexusTheme.typography.labelSmall, color = colors.onSurface, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
    }
}

@Composable
fun HudButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val colors = NexusTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary.copy(alpha = 0.15f),
            contentColor = colors.primary,
            disabledContainerColor = colors.surfaceVariant,
            disabledContentColor = colors.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) colors.primary.copy(alpha = 0.5f) else colors.onSurface.copy(alpha = 0.2f))
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = NexusTheme.typography.labelSmall)
    }
}
