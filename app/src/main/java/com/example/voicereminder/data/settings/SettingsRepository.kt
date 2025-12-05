package com.example.voicereminder.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        private val PROVIDER_KEY = stringPreferencesKey("ai_provider")
        private val OLLAMA_CONNECTION_MODE_KEY = stringPreferencesKey("ollama_connection_mode")
        private val OLLAMA_BASE_URL_KEY = stringPreferencesKey("ollama_base_url")
        private val OLLAMA_WIFI_IP_KEY = stringPreferencesKey("ollama_wifi_ip")
        private val OLLAMA_MODEL_KEY = stringPreferencesKey("ollama_model")
        private val OPENAI_API_KEY_KEY = stringPreferencesKey("openai_api_key")
        private val OPENAI_MODEL_KEY = stringPreferencesKey("openai_model")
        private val ANTHROPIC_API_KEY_KEY = stringPreferencesKey("anthropic_api_key")
        private val ANTHROPIC_MODEL_KEY = stringPreferencesKey("anthropic_model")
        private val CUSTOM_BASE_URL_KEY = stringPreferencesKey("custom_base_url")
        private val CUSTOM_API_KEY_KEY = stringPreferencesKey("custom_api_key")
        private val CUSTOM_MODEL_KEY = stringPreferencesKey("custom_model")
        private val SYSTEM_PROMPT_KEY = stringPreferencesKey("system_prompt")
    }
    
    val aiSettings: Flow<AISettings> = context.dataStore.data.map { preferences ->
        AISettings(
            provider = AIProvider.valueOf(
                preferences[PROVIDER_KEY] ?: AIProvider.OLLAMA.name
            ),
            ollamaConnectionMode = OllamaConnectionMode.valueOf(
                preferences[OLLAMA_CONNECTION_MODE_KEY] ?: OllamaConnectionMode.WIFI.name
            ),
            ollamaBaseUrl = preferences[OLLAMA_BASE_URL_KEY] ?: "http://localhost:11434/",
            ollamaWifiIp = preferences[OLLAMA_WIFI_IP_KEY] ?: "192.168.1.100",
            ollamaModel = preferences[OLLAMA_MODEL_KEY] ?: "llama3.1:latest",
            openaiApiKey = preferences[OPENAI_API_KEY_KEY] ?: "",
            openaiModel = preferences[OPENAI_MODEL_KEY] ?: "gpt-4",
            anthropicApiKey = preferences[ANTHROPIC_API_KEY_KEY] ?: "",
            anthropicModel = preferences[ANTHROPIC_MODEL_KEY] ?: "claude-3-5-sonnet-20241022",
            customBaseUrl = preferences[CUSTOM_BASE_URL_KEY] ?: "",
            customApiKey = preferences[CUSTOM_API_KEY_KEY] ?: "",
            customModel = preferences[CUSTOM_MODEL_KEY] ?: "",
            systemPrompt = preferences[SYSTEM_PROMPT_KEY] ?: AISettings.DEFAULT_SYSTEM_PROMPT
        )
    }
    
    suspend fun updateSettings(settings: AISettings) {
        context.dataStore.edit { preferences ->
            preferences[PROVIDER_KEY] = settings.provider.name
            preferences[OLLAMA_CONNECTION_MODE_KEY] = settings.ollamaConnectionMode.name
            preferences[OLLAMA_BASE_URL_KEY] = settings.ollamaBaseUrl
            preferences[OLLAMA_WIFI_IP_KEY] = settings.ollamaWifiIp
            preferences[OLLAMA_MODEL_KEY] = settings.ollamaModel
            preferences[OPENAI_API_KEY_KEY] = settings.openaiApiKey
            preferences[OPENAI_MODEL_KEY] = settings.openaiModel
            preferences[ANTHROPIC_API_KEY_KEY] = settings.anthropicApiKey
            preferences[ANTHROPIC_MODEL_KEY] = settings.anthropicModel
            preferences[CUSTOM_BASE_URL_KEY] = settings.customBaseUrl
            preferences[CUSTOM_API_KEY_KEY] = settings.customApiKey
            preferences[CUSTOM_MODEL_KEY] = settings.customModel
            preferences[SYSTEM_PROMPT_KEY] = settings.systemPrompt
        }
    }
}
