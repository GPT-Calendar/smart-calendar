package com.example.voicereminder.widget

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.voicereminder.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/**
 * Wake Word Detection Service
 * Uses continuous audio recording to detect "Kiro" wake word
 * Then activates speech recognition for the command
 */
class WakeWordService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isProcessingCommand = false
    
    // Speech recognizer for command capture (after wake word)
    private var speechRecognizer: SpeechRecognizer? = null

    companion object {
        private const val TAG = "WakeWordService"
        private const val CHANNEL_ID = "wake_word_channel"
        private const val NOTIFICATION_ID = 1002
        
        // Audio settings
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        
        // Wake word variants
        val WAKE_WORD_VARIANTS = listOf("kiro", "kyro", "kero", "cairo", "kira", "hero")
        
        // Silence threshold - audio level below this is considered silence
        private const val SILENCE_THRESHOLD = 1500
        // Minimum audio level to trigger speech detection
        private const val SPEECH_THRESHOLD = 3000

        private var isRunning = false
        private var isPaused = false
        private var instance: WakeWordService? = null

        fun isServiceRunning(): Boolean = isRunning
        
        /**
         * Pause the wake word service temporarily (e.g., when app voice input is active)
         */
        fun pause() {
            isPaused = true
            instance?.pauseListening()
            Log.d(TAG, "Wake word service paused")
        }
        
        /**
         * Resume the wake word service
         */
        fun resume() {
            isPaused = false
            instance?.resumeListening()
            Log.d(TAG, "Wake word service resumed")
        }

        fun start(context: Context) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                android.widget.Toast.makeText(
                    context,
                    "Microphone permission required for wake word detection",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return
            }

            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordService::class.java))
        }
    }

    private var phoneStateListener: android.telephony.PhoneStateListener? = null
    private var telephonyManager: android.telephony.TelephonyManager? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        instance = this
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Listen for phone calls to pause during calls
        setupPhoneStateListener()
        
        // Start continuous listening
        startContinuousListening()
    }
    
    /**
     * Setup listener to pause wake word during phone calls
     */
    private fun setupPhoneStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
        
        phoneStateListener = object : android.telephony.PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    android.telephony.TelephonyManager.CALL_STATE_RINGING,
                    android.telephony.TelephonyManager.CALL_STATE_OFFHOOK -> {
                        // Phone is ringing or in call - pause listening
                        Log.d(TAG, "Phone call detected, pausing wake word")
                        pauseListening()
                    }
                    android.telephony.TelephonyManager.CALL_STATE_IDLE -> {
                        // Call ended - resume listening
                        Log.d(TAG, "Phone call ended, resuming wake word")
                        if (!isPaused) {
                            resumeListening()
                        }
                    }
                }
            }
        }
        
        telephonyManager?.listen(phoneStateListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
    }
    
    /**
     * Pause audio recording to allow other apps to use microphone
     */
    fun pauseListening() {
        isRecording = false
        audioRecord?.stop()
        Log.d(TAG, "Audio recording paused")
    }
    
    /**
     * Resume audio recording
     */
    fun resumeListening() {
        if (!isPaused && audioRecord != null) {
            try {
                audioRecord?.startRecording()
                isRecording = true
                Log.d(TAG, "Audio recording resumed")
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming audio recording", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wake Word Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Listening for 'Kiro' wake word"
                setShowBadge(false)
                setSound(null, null) // No sound for this notification
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, WakeWordService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎤 Listening for \"Kiro\"")
            .setContentText("Say \"Kiro\" followed by your command")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()
    }

    /**
     * Start continuous audio monitoring
     * Detects when someone is speaking, then uses speech recognition
     */
    private fun startContinuousListening() {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No audio permission")
            stopSelf()
            return
        }
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                stopSelf()
                return
            }
            
            audioRecord?.startRecording()
            isRecording = true
            
            Log.d(TAG, "Started continuous audio monitoring")
            
            // Monitor audio in background
            serviceScope.launch {
                val buffer = ShortArray(bufferSize)
                var speechDetectedTime = 0L
                var lastSpeechTime = 0L
                
                while (isActive && isRecording) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (readCount > 0 && !isProcessingCommand) {
                        // Calculate audio level
                        val audioLevel = calculateAudioLevel(buffer, readCount)
                        
                        if (audioLevel > SPEECH_THRESHOLD) {
                            val now = System.currentTimeMillis()
                            
                            if (speechDetectedTime == 0L) {
                                speechDetectedTime = now
                                Log.d(TAG, "Speech detected, level: $audioLevel")
                            }
                            lastSpeechTime = now
                            
                            // If speech has been going for 300ms, trigger recognition
                            if (now - speechDetectedTime > 300 && !isProcessingCommand) {
                                Log.d(TAG, "Triggering speech recognition")
                                isProcessingCommand = true
                                
                                mainScope.launch {
                                    startSpeechRecognition()
                                }
                                
                                // Wait for recognition to complete
                                while (isProcessingCommand && isActive) {
                                    delay(100)
                                }
                                
                                speechDetectedTime = 0L
                            }
                        } else if (audioLevel < SILENCE_THRESHOLD) {
                            // Reset if silence for 500ms
                            if (speechDetectedTime > 0 && 
                                System.currentTimeMillis() - lastSpeechTime > 500) {
                                speechDetectedTime = 0L
                            }
                        }
                    }
                    
                    // Small delay to prevent CPU overuse
                    delay(50)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording", e)
            stopSelf()
        }
    }
    
    private fun calculateAudioLevel(buffer: ShortArray, readCount: Int): Int {
        var sum = 0L
        for (i in 0 until readCount) {
            sum += abs(buffer[i].toInt())
        }
        return (sum / readCount).toInt()
    }

    /**
     * Start speech recognition to capture what was said
     */
    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available")
            isProcessingCommand = false
            return
        }
        
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                Log.d(TAG, "Recognition ready")
            }
            
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d(TAG, "Recognition end of speech")
            }
            
            override fun onError(error: Int) {
                Log.d(TAG, "Recognition error: $error")
                isProcessingCommand = false
            }

            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.lowercase() ?: ""
                
                Log.d(TAG, "Heard: $spokenText")
                
                // Check if wake word was detected
                if (containsWakeWord(spokenText)) {
                    Log.d(TAG, "Wake word detected!")
                    
                    // Play activation sound
                    playActivationSound()
                    
                    // Extract command after wake word
                    val command = extractCommandAfterWakeWord(spokenText)
                    
                    if (command.isNotEmpty()) {
                        processCommand(command)
                    } else {
                        // Just wake word - show listening indicator
                        mainScope.launch {
                            android.widget.Toast.makeText(
                                this@WakeWordService, 
                                "🎤 Yes? (say your command)", 
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        // Listen for follow-up command
                        listenForCommand()
                        return
                    }
                }
                
                isProcessingCommand = false
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            isProcessingCommand = false
        }
    }
    
    /**
     * Listen for follow-up command after wake word only
     */
    private fun listenForCommand() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            
            override fun onError(error: Int) {
                Log.d(TAG, "Command recognition error: $error")
                isProcessingCommand = false
            }

            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val command = matches?.firstOrNull() ?: ""
                
                if (command.isNotEmpty()) {
                    processCommand(command)
                }
                
                isProcessingCommand = false
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting command recognition", e)
            isProcessingCommand = false
        }
    }

    private fun containsWakeWord(text: String): Boolean {
        val lowerText = text.lowercase()
        return WAKE_WORD_VARIANTS.any { lowerText.contains(it) }
    }

    private fun extractCommandAfterWakeWord(text: String): String {
        val lowerText = text.lowercase()
        for (variant in WAKE_WORD_VARIANTS) {
            val index = lowerText.indexOf(variant)
            if (index != -1) {
                val afterWakeWord = text.substring(index + variant.length).trim()
                return afterWakeWord
                    .removePrefix(",")
                    .removePrefix(".")
                    .trim()
            }
        }
        return ""
    }

    private fun processCommand(command: String) {
        Log.d(TAG, "Processing command: $command")
        
        mainScope.launch {
            // Show toast with the command
            android.widget.Toast.makeText(
                this@WakeWordService, 
                "✓ Got it: $command", 
                android.widget.Toast.LENGTH_LONG
            ).show()
            
            // Speak confirmation
            speakConfirmation("Got it. $command")
        }
        
        // Send to ChatWidgetService for AI processing
        ChatWidgetService.sendMessage(this, command)
        
        // Update widget
        ChatWidgetProvider.updateWidgetResponse(this, "🎤 \"$command\"\n⏳ Processing...")
        
        // Play confirmation sound
        playConfirmationSound()
        
        isProcessingCommand = false
    }

    private var tts: android.speech.tts.TextToSpeech? = null
    
    private fun speakConfirmation(text: String) {
        if (tts == null) {
            tts = android.speech.tts.TextToSpeech(this) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "confirmation")
                }
            }
        } else {
            tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "confirmation")
        }
    }

    private fun playActivationSound() {
        try {
            // Play a "listening" sound - two short beeps
            val toneGenerator = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION, 
                70
            )
            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100)
            mainScope.launch {
                delay(150)
                toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100)
                delay(150)
                toneGenerator.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing activation sound", e)
        }
    }
    
    private fun playConfirmationSound() {
        try {
            // Play a "success" sound - ascending tone
            val toneGenerator = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION, 
                60
            )
            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 200)
            mainScope.launch {
                delay(250)
                toneGenerator.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing confirmation sound", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isRecording = false
        isPaused = false
        instance = null
        
        // Stop phone state listener
        telephonyManager?.listen(phoneStateListener, android.telephony.PhoneStateListener.LISTEN_NONE)
        phoneStateListener = null
        telephonyManager = null
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        speechRecognizer?.destroy()
        speechRecognizer = null
        
        tts?.stop()
        tts?.shutdown()
        tts = null
        
        serviceScope.cancel()
        mainScope.cancel()
        
        Log.d(TAG, "Wake word service stopped")
    }
}
