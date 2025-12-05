package com.example.voicereminder.data.ollama

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OllamaApiService {
    @POST("api/chat")
    @Streaming
    suspend fun chat(@Body request: OllamaChatRequest): okhttp3.ResponseBody
    
    @POST("api/chat")
    @Streaming
    suspend fun chatStream(@Body request: OllamaChatRequest): okhttp3.ResponseBody
    
    @GET("api/tags")
    suspend fun listModels(): Response<com.google.gson.JsonObject>
}

data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = true
)

data class OllamaMessage(
    val role: String, // "user", "assistant", or "system"
    val content: String
)

data class OllamaResponse(
    val model: String,
    val created_at: String,
    val message: OllamaMessage,
    val done: Boolean
)
