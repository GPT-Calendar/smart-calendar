package com.example.voicereminder.data.ollama

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class OllamaRepository(
    private val baseUrl: String = OllamaConfig.BASE_URL
) {
    private val gson = Gson()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val apiService = retrofit.create(OllamaApiService::class.java)

    fun chatStream(
        model: String = OllamaConfig.DEFAULT_MODEL,
        messages: List<OllamaMessage>
    ): Flow<String> = flow {
        try {
            val request = OllamaChatRequest(
                model = model,
                messages = messages,
                stream = true
            )
            
            val response = apiService.chat(request)
            val reader = response.byteStream().bufferedReader()
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            val ollamaResponse = gson.fromJson(line, OllamaResponse::class.java)
                            if (ollamaResponse.message.content.isNotEmpty()) {
                                emit(ollamaResponse.message.content)
                            }
                        } catch (e: Exception) {
                            // Skip malformed JSON lines
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("Error: ${e.message ?: "Failed to connect to Ollama. Make sure Ollama is running."}")
        }
    }.flowOn(Dispatchers.IO)
}
