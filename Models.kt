package com.nexus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val path: String,
    val name: String,
    val extension: String,
    val content: String,       // extracted text
    val sizeBytes: Long,
    val lastModified: Long,
    val indexedAt: Long,
    val embedding: String = "" // JSON float array stored as string
)

data class DocumentResult(
    val name: String,
    val path: String,
    val extension: String,
    val snippet: String,
    val score: Float = 1f
)

data class AppSettings(
    val apiHost: String = "127.0.0.1",
    val apiPort: String = "8080",
    val modelName: String = "gemma",
    val watchedFolders: List<String> = listOf("/storage/emulated/0/Documents", "/storage/emulated/0/Downloads"),
    val autoSync: Boolean = true,
    val includeImages: Boolean = false
)

data class DashboardState(
    val totalDocs: Int = 0,
    val lastScan: String = "NEVER",
    val storageUsed: String = "0 MB",
    val isIndexing: Boolean = false,
    val indexingProgress: Float = 0f,
    val currentFile: String = "",
    val docTypes: Map<String, Int> = emptyMap(),
    val recentActivity: List<String> = emptyList()
)
