package com.example.voicereminder.data.ai

import com.example.voicereminder.data.settings.AISettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AnthropicService : AIService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    override suspend fun chatStream(
        messages: List<AIMessage>,
        settings: AISettings
    ): Flow<String> = flow {
        // Anthropic requires system message separate from messages array
        val systemMessage = messages.firstOrNull { it.role == "system" }?.content ?: ""
        val conversationMessages = messages.filter { it.role != "system" }
        
        val messagesArray = JSONArray()
        conversationMessages.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
            })
        }
        
        val requestBody = JSONObject().apply {
            put("model", settings.anthropicModel)
            put("max_tokens", 4096)
            if (systemMessage.isNotEmpty()) {
                put("system", systemMessage)
            }
            put("messages", messagesArray)
            put("stream", true)
        }
        
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", settings.anthropicApiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Anthropic API error: ${response.code}")
        }
        
        response.body?.byteStream()?.bufferedReader()?.useLines { lines ->
            lines.forEach { line ->
                if (line.startsWith("data: ")) {
                    try {
                        val json = JSONObject(line.substring(6))
                        val type = json.getString("type")
                        
                        if (type == "content_block_delta") {
                            val delta = json.getJSONObject("delta")
                            if (delta.has("text")) {
                                emit(delta.getString("text"))
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AnthropicService", "Error parsing chunk", e)
                    }
                }
            }
        }
    }
    
    override suspend fun testConnection(settings: AISettings): Pair<Boolean, String> {
        return try {
            if (settings.anthropicApiKey.isBlank()) {
                return Pair(false, "Anthropic API key is required")
            }
            
            // Test with a minimal request
            val testBody = JSONObject().apply {
                put("model", settings.anthropicModel)
                put("max_tokens", 1)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Hi")
                    })
                })
            }
            
            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", settings.anthropicApiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(testBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Pair(true, "Connected to Anthropic successfully")
            } else {
                Pair(false, "Anthropic authentication failed: ${response.code}")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.message}")
        }
    }
}
