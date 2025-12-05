package com.example.voicereminder.presentation.home

import android.Manifest
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.example.voicereminder.R
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.presentation.TTSManager
import com.example.voicereminder.presentation.VoiceManager
import com.example.voicereminder.presentation.viewmodel.VoiceInputViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    onLocationPermissionNeeded: () -> Unit = {},
    viewModel: VoiceInputViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = HomeScreenViewModelFactory(
            ReminderManager.getInstance(LocalContext.current),
            onLocationPermissionNeeded = onLocationPermissionNeeded
        )
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val keyboardController = LocalSoftwareKeyboardController.current

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

    // State for chat messages
    var messages by remember { mutableStateOf(
        listOf(
            ChatMessage(
                id = 1,
                text = "Hello! I'm your Smart Calendar. How can I help you today?",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
        )
    )}

    // State for text input
    var textInput by remember { mutableStateOf("") }

    // Observe state from ViewModel
    val isListening by viewModel.isListening.collectAsState()

    // Initialize voice manager
    val voiceManager = remember { VoiceManager(context) }
    val ttsManager = remember { TTSManager(context) }

    // Callbacks for voice and TTS managers
    val voiceCallback = remember {
        object : VoiceManager.VoiceCallback {
            override fun onResult(text: String) {
                coroutineScope.launch {
                    viewModel.updateListeningState(false)
                    viewModel.updateStatus("")
                    // Add user message to chat
                    messages = messages + ChatMessage(
                        id = messages.size + 1,
                        text = text,
                        isUser = true,
                        timestamp = System.currentTimeMillis()
                    )
                    // Process the voice input
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

    // Handle status updates from ViewModel
    LaunchedEffect(Unit) {
        viewModel.statusText.collect { status ->
            if (status.isNotEmpty()) {
                messages = messages + ChatMessage(
                    id = messages.size + 1,
                    text = status,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    // Handle error updates from ViewModel
    LaunchedEffect(Unit) {
        viewModel.errorText.collect { error ->
            if (error.isNotEmpty()) {
                messages = messages + ChatMessage(
                    id = messages.size + 1,
                    text = error,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    // Clean up on disposal
    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
            ttsManager.shutdown()
        }
    }

    // Calculate responsive values
    val horizontalPadding = if (screenWidth > 600) 24.dp else 16.dp
    val verticalPadding = if (screenHeight > 800) 16.dp else 10.dp
    val bubbleMaxWidth = if (screenWidth > 600) (screenWidth * 0.7).dp else (screenWidth * 0.85).dp
    val inputHeight = if (screenHeight > 800) 64.dp else 56.dp

    // Responsive font sizes
    val headerFontSize = if (screenWidth > 600) 22.sp else 18.sp
    val messageFontSize = if (screenWidth > 600) 16.sp else 15.sp
    val inputFontSize = if (screenWidth > 600) 16.sp else 15.sp
    val avatarFontSize = if (screenWidth > 600) 14.sp else 12.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // Handle keyboard avoiding issues
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Smart Calendar",
                fontSize = headerFontSize,
                fontWeight = FontWeight.Bold
            )
        }

        // Chat messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = horizontalPadding),
            state = rememberLazyListState(),
            verticalArrangement = Arrangement.spacedBy(if (screenHeight > 800) 16.dp else 12.dp)
        ) {
            items(messages.size) { index ->
                val message = messages[index]
                ChatBubble(
                    message = message,
                    maxWidth = bubbleMaxWidth,
                    fontSize = messageFontSize,
                    avatarFontSize = avatarFontSize
                )
            }
        }

        // Input area
        InputBar(
            textInput = textInput,
            onTextInputChange = { textInput = it },
            onSendClick = {
                if (textInput.isNotBlank()) {
                    // Add user message to chat
                    messages = messages + ChatMessage(
                        id = messages.size + 1,
                        text = textInput,
                        isUser = true,
                        timestamp = System.currentTimeMillis()
                    )
                    // Process the text input
                    coroutineScope.launch {
                        viewModel.processVoiceInput(textInput)
                    }
                    textInput = "" // Clear input after processing
                    keyboardController?.hide() // Hide keyboard after sending
                }
            },
            onVoiceClick = {
                if (isListening) {
                    voiceManager.stopListening()
                    viewModel.updateListeningState(false)
                    viewModel.updateStatus("")
                } else {
                    // Check if speech recognition is available
                    if (!voiceManager.isAvailable()) {
                        viewModel.updateError(context.getString(R.string.error_stt_unavailable))
                        return@InputBar
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
            isListening = isListening,
            height = inputHeight,
            fontSize = inputFontSize
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    maxWidth: Dp = 300.dp,
    fontSize: TextUnit = 16.sp,
    avatarFontSize: TextUnit = 12.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (message.isUser) {
            // User message bubble
            Card(
                modifier = Modifier
                    .padding(start = 60.dp, end = 8.dp)
                    .defaultMinSize(minWidth = 60.dp)
                    .wrapContentWidth(unbounded = true)
                    .fillMaxWidth(0.85f), // Take up to 85% of available space
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                    fontSize = fontSize,
                    softWrap = true
                )
            }
        } else {
            // Assistant message bubble
            Card(
                modifier = Modifier
                    .padding(start = 8.dp, end = 60.dp)
                    .defaultMinSize(minWidth = 60.dp)
                    .wrapContentWidth(unbounded = true)
                    .fillMaxWidth(0.85f), // Take up to 85% of available space
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Assistant avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AI",
                            fontSize = avatarFontSize,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = message.text,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp),
                        fontSize = fontSize,
                        softWrap = true
                    )
                }
            }
        }
    }
}

@Composable
fun InputBar(
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onVoiceClick: () -> Unit,
    isListening: Boolean,
    height: Dp = 56.dp,
    fontSize: TextUnit = 15.sp
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val horizontalPadding = if (screenWidth > 600) 24.dp else 16.dp
    val spacing = if (screenWidth > 600) 12.dp else 8.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .height(height),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = textInput,
                    onValueChange = onTextInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    placeholder = {
                        Text(
                            text = "Message Smart Calendar...",
                            fontSize = fontSize
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = fontSize)
                )

                Spacer(modifier = Modifier.width(spacing))

                IconButton(
                    onClick = onSendClick,
                    enabled = textInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send message",
                        tint = if (textInput.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(spacing))

        FloatingActionButton(
            onClick = onVoiceClick,
            containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size((height.value * 0.8f).dp) // Make FAB responsive to input height
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicNone else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop listening" else "Start listening"
            )
        }
    }
}

data class ChatMessage(
    val id: Int,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)

class HomeScreenViewModelFactory(
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