package com.example.voicereminder.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.data.ai.AIServiceFactory
import com.example.voicereminder.data.settings.AIProvider
import com.example.voicereminder.data.settings.AISettings
import com.example.voicereminder.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsRepository = SettingsRepository(application)
    
    private val _settings = MutableStateFlow(AISettings())
    val settings: StateFlow<AISettings> = _settings.asStateFlow()
    
    private val _testResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val testResult: StateFlow<Pair<Boolean, String>?> = _testResult.asStateFlow()
    
    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()
    
    init {
        viewModelScope.launch {
            settingsRepository.aiSettings.collect { loadedSettings ->
                _settings.value = loadedSettings
            }
        }
    }
    
    fun updateProvider(provider: AIProvider) {
        _settings.value = _settings.value.copy(provider = provider)
    }
    
    fun updateOllamaConnectionMode(mode: com.example.voicereminder.data.settings.OllamaConnectionMode) {
        _settings.value = _settings.value.copy(ollamaConnectionMode = mode)
    }
    
    fun updateOllamaBaseUrl(url: String) {
        _settings.value = _settings.value.copy(ollamaBaseUrl = url)
    }
    
    fun updateOllamaWifiIp(ip: String) {
        _settings.value = _settings.value.copy(ollamaWifiIp = ip)
    }
    
    fun updateOllamaModel(model: String) {
        _settings.value = _settings.value.copy(ollamaModel = model)
    }
    
    fun updateOpenAIApiKey(key: String) {
        _settings.value = _settings.value.copy(openaiApiKey = key)
    }
    
    fun updateOpenAIModel(model: String) {
        _settings.value = _settings.value.copy(openaiModel = model)
    }
    
    fun updateAnthropicApiKey(key: String) {
        _settings.value = _settings.value.copy(anthropicApiKey = key)
    }
    
    fun updateAnthropicModel(model: String) {
        _settings.value = _settings.value.copy(anthropicModel = model)
    }
    
    fun updateCustomBaseUrl(url: String) {
        _settings.value = _settings.value.copy(customBaseUrl = url)
    }
    
    fun updateCustomApiKey(key: String) {
        _settings.value = _settings.value.copy(customApiKey = key)
    }
    
    fun updateCustomModel(model: String) {
        _settings.value = _settings.value.copy(customModel = model)
    }
    
    fun updateSystemPrompt(prompt: String) {
        _settings.value = _settings.value.copy(systemPrompt = prompt)
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            settingsRepository.updateSettings(_settings.value)
        }
    }
    
    fun testConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null
            
            try {
                val aiService = AIServiceFactory.create(_settings.value.provider)
                val result = aiService.testConnection(_settings.value)
                _testResult.value = result
            } catch (e: Exception) {
                _testResult.value = Pair(false, "Error: ${e.message}")
            } finally {
                _isTesting.value = false
            }
        }
    }
    
    fun clearTestResult() {
        _testResult.value = null
    }
    
    fun resetToDefaults() {
        _settings.value = AISettings()
    }
}
