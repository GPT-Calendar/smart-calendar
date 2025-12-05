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

class CustomAIService : AIService {
    
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
            put("model", settings.customModel)
            put("messages", messagesArray)
            put("stream", true)
        }
        
        val requestBuilder = Request.Builder()
            .url("${settings.customBaseUrl}/v1/chat/completions")
            .addHeader("Content-Type", "application/json")
        
        if (settings.customApiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer ${settings.customApiKey}")
        }
        
        val request = requestBuilder
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Custom API error: ${response.code}")
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
                        android.util.Log.e("CustomAIService", "Error parsing chunk", e)
                    }
                }
            }
        }
    }
    
    override suspend fun testConnection(settings: AISettings): Pair<Boolean, String> {
        return try {
            if (settings.customBaseUrl.isBlank()) {
                return Pair(false, "Custom API base URL is required")
            }
            
            val requestBuilder = Request.Builder()
                .url("${settings.customBaseUrl}/v1/models")
            
            if (settings.customApiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${settings.customApiKey}")
            }
            
            val request = requestBuilder.get().build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Pair(true, "Connected to custom API successfully")
            } else {
                Pair(false, "Custom API connection failed: ${response.code}")
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.message}")
        }
    }
}
