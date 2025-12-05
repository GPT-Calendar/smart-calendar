package com.example.voicereminder.data.ai

import com.example.voicereminder.data.settings.AIProvider
import com.example.voicereminder.data.settings.AISettings
import kotlinx.coroutines.flow.Flow

interface AIService {
    suspend fun chatStream(
        messages: List<AIMessage>,
        settings: AISettings
    ): Flow<String>
    
    suspend fun testConnection(settings: AISettings): Pair<Boolean, String>
}

data class AIMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

class AIServiceFactory {
    companion object {
        fun create(provider: AIProvider): AIService {
            return when (provider) {
                AIProvider.OLLAMA -> OllamaAIService()
                AIProvider.OPENAI -> OpenAIService()
                AIProvider.ANTHROPIC -> AnthropicService()
                AIProvider.CUSTOM -> CustomAIService()
            }
        }
    }
}
