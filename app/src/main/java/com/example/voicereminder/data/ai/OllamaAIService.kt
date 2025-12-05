package com.example.voicereminder.data.ai

import com.example.voicereminder.data.ollama.OllamaApiService
import com.example.voicereminder.data.ollama.OllamaMessage
import com.example.voicereminder.data.settings.AISettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OllamaAIService : AIService {
    
    private fun createApiService(baseUrl: String): OllamaApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(OllamaApiService::class.java)
    }
    
    override suspend fun chatStream(
        messages: List<AIMessage>,
        settings: AISettings
    ): Flow<String> = flow {
        val apiService = createApiService(settings.getEffectiveOllamaUrl())
        val ollamaMessages = messages.map { OllamaMessage(it.role, it.content) }
        
        val request = com.example.voicereminder.data.ollama.OllamaChatRequest(
            model = settings.ollamaModel,
            messages = ollamaMessages,
            stream = true
        )
        
        val response = apiService.chatStream(request)
        val reader = response.byteStream().bufferedReader()
        
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { currentLine ->
                    if (currentLine.isNotBlank()) {
                        try {
                            val json = com.google.gson.Gson().fromJson(
                                currentLine,
                                com.google.gson.JsonObject::class.java
                            )
                            val content = json.getAsJsonObject("message")
                                ?.get("content")?.asString
                            if (content != null) {
                                emit(content)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("OllamaAIService", "Error parsing chunk: $currentLine", e)
                        }
                    }
                }
            }
        } finally {
            reader.close()
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    
    override suspend fun testConnection(settings: AISettings): Pair<Boolean, String> {
        return try {
            val effectiveUrl = settings.getEffectiveOllamaUrl()
            android.util.Log.d("OllamaAIService", "Testing connection to: $effectiveUrl")
            
            val apiService = createApiService(effectiveUrl)
            val response = apiService.listModels()
            
            android.util.Log.d("OllamaAIService", "Response code: ${response.code()}")
            
            if (response.isSuccessful) {
                val body = response.body()
                android.util.Log.d("OllamaAIService", "Response body: $body")
                
                val models = body?.get("models")?.asJsonArray
                val modelNames = models?.map { 
                    it.asJsonObject.get("name")?.asString 
                }?.filterNotNull() ?: emptyList()
                
                android.util.Log.d("OllamaAIService", "Available models: $modelNames")
                
                if (modelNames.contains(settings.ollamaModel)) {
                    Pair(true, "Connected to Ollama. Model '${settings.ollamaModel}' is available.")
                } else {
                    Pair(false, "Connected to Ollama, but model '${settings.ollamaModel}' not found. Available: ${modelNames.joinToString()}")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("OllamaAIService", "Error response: $errorBody")
                Pair(false, "Failed to connect to Ollama: HTTP ${response.code()}")
            }
        } catch (e: java.net.ConnectException) {
            android.util.Log.e("OllamaAIService", "Connection refused", e)
            Pair(false, "Cannot connect to ${settings.getEffectiveOllamaUrl()}. Is Ollama Bridge running?")
        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("OllamaAIService", "Unknown host", e)
            Pair(false, "Cannot resolve host. Check URL: ${settings.getEffectiveOllamaUrl()}")
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("OllamaAIService", "Timeout", e)
            Pair(false, "Connection timeout. Is Ollama running and accessible?")
        } catch (e: Exception) {
            android.util.Log.e("OllamaAIService", "Connection error", e)
            Pair(false, "Connection error: ${e.javaClass.simpleName} - ${e.message}")
        }
    }
}
