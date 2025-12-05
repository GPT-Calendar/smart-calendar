package com.example.voicereminder.data.ollama

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Utility class to check if Ollama server is accessible
 */
object OllamaHealthCheck {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Check if Ollama server is running and accessible
     * @return Pair<Boolean, String> - (isHealthy, message)
     */
    suspend fun checkConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${OllamaConfig.BASE_URL}api/tags")
                .get()
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Pair(true, "Connected to Ollama successfully")
            } else {
                Pair(false, "Ollama server returned error: ${response.code}")
            }
        } catch (e: Exception) {
            Pair(false, "Cannot connect to Ollama: ${e.message}")
        }
    }

    /**
     * Check if a specific model is available
     * @param modelName The name of the model to check (e.g., "llama3.1")
     * @return Pair<Boolean, String> - (isAvailable, message)
     */
    suspend fun checkModelAvailable(modelName: String = OllamaConfig.DEFAULT_MODEL): Pair<Boolean, String> = 
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${OllamaConfig.BASE_URL}api/tags")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.contains(modelName)) {
                        Pair(true, "Model '$modelName' is available")
                    } else {
                        Pair(false, "Model '$modelName' not found. Run: ollama pull $modelName")
                    }
                } else {
                    Pair(false, "Cannot check models: ${response.code}")
                }
            } catch (e: Exception) {
                Pair(false, "Error checking model: ${e.message}")
            }
        }
}
