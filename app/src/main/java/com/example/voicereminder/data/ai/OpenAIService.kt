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

class OpenAIService : AIService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    override suspend fun chatStream(
        messages: List<AIMessage>,
        settings: AISettings
    ): Flow<String> = flow {
        val messagesArray = JSONArray()
        messages.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
            })
        }
        
        val requestBody = JSONObject().apply {
            put("model", settings.openaiModel)
            put("messages", messagesArray)
            put("stream", true)
        }
        
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${settings.openaiApiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("OpenAI API error: ${response.code}")
        }
        
        response.body?.byteStream()?.bufferedReader()?.useLines { lines ->
            lines.forEach { line ->
                if (line.startsWith("data: ") && !line.contains("[DONE]")) {
                    try {
                        val json = JSONObject(line.substring(6))
                        val delta = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("delta")
                        
                        if (delta.has("content")) {
                            emit(delta.getString("content"))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OpenAIService", "Error parsing chunk", e)
                    }
                }
            }
        }
    }
    
    override suspend fun testConnection(settings: AISettings): Pair<Boolean, String> {
        return try {
            if (settings.openaiApiKey.isBlank()) {
                return Pair(false, "OpenAI API key is required")
            }
            
            val request = Request.Builder()
                .url("https://api.openai.com/v1/models")
                .addHeader("Authorization", "Bearer ${settings.openaiApiKey}")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Pair(true, "Connected to OpenAI successfully")
            } else {
                Pair(false, "OpenAI authentication failed: ${response.code}")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.message}")
        }
    }
}
