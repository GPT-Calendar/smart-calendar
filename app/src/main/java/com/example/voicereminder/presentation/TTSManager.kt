package com.example.voicereminder.presentation

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Manages Text-to-Speech functionality for voice confirmations and reminders.
 * Handles TTS engine initialization, speech output, and lifecycle management.
 */
class TTSManager(private val context: Context) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var callback: TTSCallback? = null
    
    companion object {
        private const val TAG = "TTSManager"
    }
    
    /**
     * Callback interface for TTS events
     */
    interface TTSCallback {
        fun onSpeakingStarted()
        fun onSpeakingCompleted()
        fun onError(error: String)
    }
    
    init {
        initializeTTS()
    }
    
    /**
     * Initialize the TextToSpeech engine
     */
    private fun initializeTTS() {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.getDefault())
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || 
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language not supported")
                        isInitialized = false
                        callback?.onError("Language not supported for text-to-speech")
                    } else {
                        isInitialized = true
                        Log.d(TAG, "TTS initialized successfully")
                        
                        // Set up utterance progress listener
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                callback?.onSpeakingStarted()
                            }
                            
                            override fun onDone(utteranceId: String?) {
                                callback?.onSpeakingCompleted()
                            }
                            
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                callback?.onError("TTS error occurred")
                            }
                            
                            override fun onError(utteranceId: String?, errorCode: Int) {
                                callback?.onError("TTS error: $errorCode")
                            }
                        })
                    }
                } else {
                    Log.e(TAG, "TTS initialization failed")
                    isInitialized = false
                    callback?.onError("Text-to-speech initialization failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during TTS initialization", e)
            isInitialized = false
            callback?.onError("Failed to initialize text-to-speech: ${e.message}")
        }
    }
    
    /**
     * Set callback for TTS events
     */
    fun setCallback(callback: TTSCallback) {
        this.callback = callback
    }
    
    /**
     * Speak the given text
     * @param text The text to speak
     * @param utteranceId Optional unique identifier for this utterance
     */
    fun speak(text: String, utteranceId: String = "utterance_${System.currentTimeMillis()}") {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, cannot speak")
            callback?.onError("Text-to-speech is not ready")
            return
        }
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text", e)
            callback?.onError("Failed to speak: ${e.message}")
        }
    }
    
    /**
     * Stop current speech output
     */
    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }
    
    /**
     * Check if TTS is ready to speak
     */
    fun isReady(): Boolean {
        return isInitialized && tts != null
    }
    
    /**
     * Shutdown TTS engine and release resources
     * Should be called in onDestroy() or when TTS is no longer needed
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            Log.d(TAG, "TTS shutdown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during TTS shutdown", e)
        }
    }
}
