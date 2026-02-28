package com.nexus.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.data.model.DashboardState
import com.nexus.data.repository.DocumentRepository
import com.nexus.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    app: Application,
    private val repo: DocumentRepository,
    private val settingsRepo: SettingsRepository
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadStats()
        observeDocs()
    }

    private fun observeDocs() {
        repo.allDocuments.onEach { docs ->
            val byExt = docs.groupBy { it.extension.lowercase() }.mapValues { it.value.size }
            _state.update { it.copy(totalDocs = docs.size, docTypes = byExt) }
        }.launchIn(viewModelScope)
    }

    private fun loadStats() = viewModelScope.launch {
        val totalBytes = repo.totalSize()
        val recent = repo.recentlyIndexed()
        val activityLog = recent.take(8).map { "INDEXED: ${it.name}.${it.extension}" }
        _state.update {
            it.copy(
                storageUsed = formatSize(totalBytes),
                recentActivity = activityLog
            )
        }
    }

    fun startIndexing() = viewModelScope.launch {
        val settings = settingsRepo.getSettings()
        _state.update { it.copy(isIndexing = true, indexingProgress = 0f) }
        settings.watchedFolders.forEach { folder ->
            repo.indexFolder(folder) { progress, file ->
                _state.update { it.copy(indexingProgress = progress, currentFile = file) }
            }
        }
        val sdf = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        _state.update {
            it.copy(
                isIndexing = false,
                indexingProgress = 1f,
                lastScan = sdf.format(Date()),
                storageUsed = formatSize(viewModelScope.run { 0L }) // refresh
            )
        }
        loadStats()
    }

    private fun formatSize(bytes: Long): String = when {
        bytes > 1_000_000_000 -> "%.1f GB".format(bytes / 1e9)
        bytes > 1_000_000     -> "%.1f MB".format(bytes / 1e6)
        bytes > 1_000         -> "%.1f KB".format(bytes / 1e3)
        else                  -> "$bytes B"
    }
}
