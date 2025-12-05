package com.example.voicereminder.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Manages Speech-to-Text functionality using Android's SpeechRecognizer.
 * Handles voice input capture and provides callbacks for recognition results and errors.
 */
class VoiceManager(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: VoiceCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    companion object {
        private const val TAG = "VoiceManager"
    }
    
    /**
     * Callback interface for voice recognition events
     */
    interface VoiceCallback {
        fun onResult(text: String)
        fun onError(error: String)
        fun onReadyForSpeech()
        fun onEndOfSpeech()
    }
    
    /**
     * Check if speech recognition is available on the device
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Set the callback for voice recognition events
     */
    fun setCallback(callback: VoiceCallback) {
        this.callback = callback
    }
    
    /**
     * Start listening for voice input
     */
    fun startListening() {
        if (!isAvailable()) {
            callback?.onError("Speech recognition is not available on this device")
            return
        }
        
        // Ensure we run on main thread for SpeechRecognizer
        mainHandler.post {
            try {
                // Destroy existing recognizer and create fresh one to avoid stale state
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(recognitionListener)
                
                // Get device's default language
                val deviceLocale = Locale.getDefault()
                Log.d(TAG, "Using device locale: ${deviceLocale.language}-${deviceLocale.country}")
                
                // Create recognition intent with improved settings
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    // Use device's default language instead of hardcoded en-US
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, deviceLocale.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, deviceLocale.toLanguageTag())
                    // Also accept English as fallback
                    putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf(deviceLocale.toLanguageTag(), "en-US"))
                    // Enable partial results for better feedback
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    // Increase speech timeout for longer phrases
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
                }
                
                // Start listening
                speechRecognizer?.startListening(intent)
                Log.d(TAG, "Started listening for voice input")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                callback?.onError("Failed to start voice recognition: ${e.message}")
            }
        }
    }
    
    /**
     * Stop listening for voice input
     */
    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                Log.d(TAG, "Stopped listening for voice input")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognition", e)
            }
        }
    }
    
    /**
     * Cancel current recognition session
     */
    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                Log.d(TAG, "Cancelled voice recognition")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling speech recognition", e)
            }
        }
    }
    
    /**
     * Release resources and cleanup
     */
    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                callback = null
                Log.d(TAG, "VoiceManager destroyed")
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying VoiceManager", e)
            }
        }
    }
    
    /**
     * RecognitionListener implementation to handle speech recognition events
     */
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
            mainHandler.post { callback?.onReadyForSpeech() }
        }
        
        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech - user started talking")
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed - can be used for visual feedback
            // Log.v(TAG, "Audio level: $rmsdB dB")
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }
        
        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech - user stopped talking")
            mainHandler.post { callback?.onEndOfSpeech() }
        }
        
        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                SpeechRecognizer.ERROR_CLIENT -> "Client side error. Please try again."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required. Please grant access in Settings."
                SpeechRecognizer.ERROR_NETWORK -> "Network error. Check your internet connection."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Please try again."
                SpeechRecognizer.ERROR_NO_MATCH -> "Couldn't understand. Please speak clearly and try again."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy. Please wait and try again."
                SpeechRecognizer.ERROR_SERVER -> "Server error. Please try again later."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic and speak."
                else -> "Voice recognition error. Please try again."
            }
            
            Log.e(TAG, "Speech recognition error: $errorMessage (code: $error)")
            mainHandler.post { callback?.onError(errorMessage) }
        }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(TAG, "onResults called, matches: $matches")
            
            if (matches != null && matches.isNotEmpty()) {
                // Get the best match (first result)
                val recognizedText = matches[0].trim()
                if (recognizedText.isNotEmpty()) {
                    Log.d(TAG, "Speech recognized: '$recognizedText'")
                    mainHandler.post { callback?.onResult(recognizedText) }
                } else {
                    Log.w(TAG, "Empty result received")
                    mainHandler.post { callback?.onError("No speech recognized. Please try again.") }
                }
            } else {
                Log.w(TAG, "No results found in bundle")
                mainHandler.post { callback?.onError("No speech recognized. Please try again.") }
            }
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches != null && matches.isNotEmpty()) {
                Log.d(TAG, "Partial result: ${matches[0]}")
                // Could update UI with partial results for real-time feedback
            }
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "Recognition event: $eventType")
        }
    }
}
