package com.example.voicereminder.presentation.voice

import android.Manifest
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereminder.R
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.presentation.TTSManager
import com.example.voicereminder.presentation.VoiceManager
import com.example.voicereminder.presentation.viewmodel.VoiceInputViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputScreen(
    onLocationPermissionNeeded: () -> Unit = {},
    viewModel: VoiceInputViewModel = viewModel(
        factory = VoiceInputViewModelFactory(
            ReminderManager.getInstance(LocalContext.current),
            onLocationPermissionNeeded = onLocationPermissionNeeded
        )
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Add ActivityResultLauncher for permission requests
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start listening
            val voiceManager = VoiceManager(context)
            if (voiceManager.isAvailable()) {
                voiceManager.startListening()
            } else {
                viewModel.updateError(context.getString(R.string.error_stt_unavailable))
            }
        } else {
            // Permission denied
            viewModel.updateError(context.getString(R.string.permission_rationale_audio))
        }
    }

    // Observe state from ViewModel
    val statusText by viewModel.statusText.collectAsState()
    val errorText by viewModel.errorText.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    
    // State for text input
    var textInput by remember { mutableStateOf("") }

    // Initialize voice manager and TTS manager
    val voiceManager = remember { VoiceManager(context) }
    val ttsManager = remember { TTSManager(context) }

    // Callbacks for voice and TTS managers
    val voiceCallback = remember {
        object : VoiceManager.VoiceCallback {
            override fun onResult(text: String) {
                coroutineScope.launch {
                    viewModel.updateListeningState(false)
                    viewModel.processVoiceInput(text)
                }
            }

            override fun onError(error: String) {
                viewModel.updateListeningState(false)
                viewModel.updateStatus("")
                viewModel.updateError(error)
            }

            override fun onReadyForSpeech() {
                viewModel.updateListeningState(true)
                viewModel.updateStatus(context.getString(R.string.listening))
                viewModel.clearError()
            }

            override fun onEndOfSpeech() {
                // Keep listening indicator visible while processing
            }
        }
    }

    val ttsCallback = remember {
        object : TTSManager.TTSCallback {
            override fun onSpeakingStarted() {
                // Visual indicator when TTS starts
            }

            override fun onSpeakingCompleted() {
                // Visual indicator when TTS completes
            }

            override fun onError(error: String) {
                // Handle TTS errors
            }
        }
    }

    // Set callbacks
    LaunchedEffect(Unit) {
        voiceManager.setCallback(voiceCallback)
        ttsManager.setCallback(ttsCallback)
    }

    // Clean up on disposal
    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
            ttsManager.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.voice_reminder_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Status display
        if (statusText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Error display
        if (errorText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Voice input button
        FloatingActionButton(
            onClick = {
                if (isListening) {
                    voiceManager.stopListening()
                    viewModel.updateListeningState(false)
                    viewModel.updateStatus("")
                } else {
                    // Check if speech recognition is available
                    if (!voiceManager.isAvailable()) {
                        viewModel.updateError(context.getString(R.string.error_stt_unavailable))
                        return@FloatingActionButton
                    }

                    if (androidx.core.app.ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        // Permission already granted, start listening
                        voiceManager.startListening()
                    } else {
                        // Request permission using the launcher
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicNone else Icons.Default.Mic,
                contentDescription = if (isListening) stringResource(R.string.desc_stop_listening) else stringResource(R.string.desc_start_listening)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isListening) stringResource(R.string.listening) else stringResource(R.string.tap_to_speak),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Divider
        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Text input section
        Text(
            text = "Or type your reminder",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g., remind me to call mom at 3 PM") },
                singleLine = false,
                maxLines = 3,
                enabled = !isListening
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        coroutineScope.launch {
                            viewModel.processVoiceInput(textInput)
                            textInput = "" // Clear input after processing
                        }
                    }
                },
                enabled = textInput.isNotBlank() && !isListening
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Submit reminder",
                    tint = if (textInput.isNotBlank() && !isListening) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Help text
        Text(
            text = stringResource(R.string.voice_instruction),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
    }
}

class VoiceInputViewModelFactory(
    private val reminderManager: ReminderManager,
    private val onLocationPermissionNeeded: () -> Unit = {}
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceInputViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VoiceInputViewModel(reminderManager, onLocationPermissionNeeded) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}