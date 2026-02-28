package com.nexus.data.repository

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.nexus.data.local.DocumentDao
import com.nexus.data.local.LocalAiService
import com.nexus.data.local.ChatMessage
import com.nexus.data.local.ChatRequest
import com.nexus.data.local.EmbeddingRequest
import com.nexus.data.model.AppSettings
import com.nexus.data.model.DocumentEntity
import com.nexus.data.model.DocumentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class DocumentRepository @Inject constructor(
    private val dao: DocumentDao,
    private val extractor: DocumentExtractor,
    private val aiService: LocalAiService,
    private val settingsRepository: SettingsRepository,
    private val context: Context
) {
    val allDocuments: Flow<List<DocumentEntity>> = dao.getAllDocuments()

    // ── Indexing ──────────────────────────────────────────────────────────────
    suspend fun indexFolder(
        folderPath: String,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val folder = File(folderPath)
        if (!folder.exists()) return@withContext

        val files = folder.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in extractor.supportedExtensions }
            .toList()

        val existingPaths = mutableListOf<String>()

        files.forEachIndexed { i, file ->
            onProgress(i.toFloat() / files.size, file.name)
            existingPaths.add(file.absolutePath)

            val existing = dao.getByPath(file.absolutePath)
            if (existing != null && existing.lastModified == file.lastModified()) return@forEachIndexed

            val content = extractor.extractText(file)
            if (content.isBlank()) return@forEachIndexed

            val entity = DocumentEntity(
                path = file.absolutePath,
                name = file.nameWithoutExtension,
                extension = file.extension,
                content = content.take(50_000), // cap at 50k chars
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                indexedAt = System.currentTimeMillis()
            )
            dao.upsert(entity)
        }
        // Remove orphans
        if (existingPaths.isNotEmpty()) {
            dao.deleteOrphans(existingPaths)
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    suspend fun search(query: String): List<DocumentResult> = withContext(Dispatchers.IO) {
        val byContent = dao.searchByContent(query)
        val byName = dao.searchByName(query)
        val combined = (byContent + byName).distinctBy { it.path }

        combined.map { doc ->
            val snippet = extractSnippet(doc.content, query)
            DocumentResult(
                name = doc.name,
                path = doc.path,
                extension = doc.extension,
                snippet = snippet
            )
        }.take(30)
    }

    // ── AI semantic query ─────────────────────────────────────────────────────
    suspend fun queryWithAi(userQuery: String, context_docs: List<DocumentResult>): String {
        val settings = settingsRepository.getSettings()
        val contextText = context_docs.take(5).joinToString("\n---\n") {
            "FILE: ${it.name}.${it.extension}\n${it.snippet}"
        }
        val request = ChatRequest(
            model = settings.modelName,
            messages = listOf(
                ChatMessage("system", "You are NEXUS, a personal document intelligence system. Answer questions based on the user's documents. Be concise and precise."),
                ChatMessage("user", "Based on these documents:\n$contextText\n\nQuestion: $userQuery")
            )
        )
        return try {
            val response = aiService.chat(request)
            response.choices.firstOrNull()?.message?.content ?: "No response from AI"
        } catch (e: Exception) {
            "AI offline — showing file results only"
        }
    }

    // ── Open document ─────────────────────────────────────────────────────────
    fun openDocument(context: Context, path: String) {
        val file = File(path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file.extension))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    suspend fun totalCount() = dao.count()
    suspend fun totalSize() = dao.totalSize() ?: 0L
    suspend fun countByExtension() = dao.countByExtension()
    suspend fun recentlyIndexed() = dao.recentlyIndexed()

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun extractSnippet(content: String, query: String): String {
        val idx = content.indexOf(query, ignoreCase = true)
        return if (idx >= 0) {
            val start = maxOf(0, idx - 80)
            val end = minOf(content.length, idx + query.length + 160)
            "...${content.substring(start, end)}..."
        } else {
            content.take(220) + "..."
        }
    }

    private fun getMimeType(ext: String) = when (ext.lowercase()) {
        "pdf"  -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "doc"  -> "application/msword"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "xls"  -> "application/vnd.ms-excel"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "txt"  -> "text/plain"
        "csv"  -> "text/csv"
        else   -> "*/*"
    }
}
