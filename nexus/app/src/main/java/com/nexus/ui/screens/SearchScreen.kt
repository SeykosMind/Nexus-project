package com.nexus.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nexus.ui.components.*
import com.nexus.ui.theme.NexusTheme
import com.nexus.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    vm: SearchViewModel = hiltViewModel()
) {
    val colors = NexusTheme.colors
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val aiResponse by vm.aiResponse.collectAsState()
    val isSearching by vm.isSearching.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
    

        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null, tint = colors.primary)
            }
            // HUD search field
            OutlinedTextField(
                value = query,
                onValueChange = { vm.onQueryChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text("QUERY INTELLIGENCE BASE...", style = NexusTheme.typography.bodySmall, color = colors.onSurface.copy(alpha = 0.5f))
                },
                textStyle = NexusTheme.typography.bodyMedium.copy(color = colors.primary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.primaryDim.copy(alpha = 0.4f),
                    cursorColor = colors.primary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                singleLine = true,
                trailingIcon = {
                    if (isSearching)
                        CircularProgressIndicator(Modifier.size(20.dp), color = colors.primary, strokeWidth = 2.dp)
                    else
                        IconButton(onClick = { vm.search() }) {
                            Icon(Icons.Default.Search, null, tint = colors.primary)
                        }
                }
            )
        }

        LazyColumn(
            Modifier.fillMaxSize().background(colors.background),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // AI response
            if (aiResponse.isNotEmpty()) {
                item {
                    HudPanel(
                        title = "AI ANALYSIS",
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            TypewriterText(
                                text = aiResponse,
                                style = NexusTheme.typography.bodyMedium,
                                color = colors.onBackground
                            )
                        }
                    }
                }
            }

            if (results.isNotEmpty()) {
                item {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "${results.size} SIGNALS DETECTED",
                            style = NexusTheme.typography.labelSmall,
                            color = colors.secondary
                        )
                    }
                }
                items(results) { doc ->
                    DocumentCard(
                        name = doc.name,
                        path = doc.path,
                        extension = doc.extension,
                        snippet = doc.snippet,
                        onClick = { vm.openDocument(doc) }
                    )
                }
            } else if (!isSearching && query.isNotEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RadarPulse(active = false)
                            Spacer(Modifier.height(16.dp))
                            Text("NO SIGNALS FOUND", style = NexusTheme.typography.titleMedium, color = colors.onSurface)
                        }
                    }
                }
            }
        }
    }
}
