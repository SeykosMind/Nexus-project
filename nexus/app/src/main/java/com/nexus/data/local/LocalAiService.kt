package com.nexus.data.local

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

data class ChatMessage(val role: String, val content: String)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 512,
    val stream: Boolean = false
)

data class ChatChoice(
    val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ChatResponse(
    val id: String?,
    val choices: List<ChatChoice>
)

data class EmbeddingRequest(
    val model: String,
    val input: String
)

data class EmbeddingData(val embedding: List<Float>)
data class EmbeddingResponse(val data: List<EmbeddingData>)

interface LocalAiService {
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @POST("v1/embeddings")
    suspend fun embeddings(@Body request: EmbeddingRequest): EmbeddingResponse
}
