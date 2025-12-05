package com.example.voicereminder.presentation.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.presentation.TTSManager
import com.example.voicereminder.presentation.VoiceManager
import com.example.voicereminder.presentation.chat.components.*
import kotlinx.coroutines.launch

/**
 * Enhanced Chat Screen with smart suggestions, quick commands, and rich interactions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedChatScreen(
    onLocationPermissionNeeded: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    chatViewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = ChatViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val voiceManager = VoiceManager(context)
            if (voiceManager.isAvailable()) {
                voiceManager.startListening()
            }
        }
    }

    // State
    var userInputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var showQuickCommandPanel by remember { mutableStateOf(false) }
    var dismissedInsights by remember { mutableStateOf(setOf<String>()) }
    
    // ViewModel state
    val messages by chatViewModel.messages.collectAsState()
    val isTyping by chatViewModel.isTyping.collectAsState()
    val connectionStatus by chatViewModel.connectionStatus.collectAsState()
    
    // Smart suggestions engine
    val suggestionEngine = remember { SmartSuggestionEngine() }
    val insightEngine = remember { ProactiveInsightEngine() }
    
    // Generate smart suggestions based on context
    val smartSuggestions = remember(messages) {
        val context = SuggestionContext(
            timeOfDay = suggestionEngine.getCurrentTimeOfDay(),
            hasOverdueTasks = false, // TODO: Get from TaskManager
            hasUpcomingReminders = false, // TODO: Get from ReminderManager
            lastAction = null // TODO: Track last action
        )
        suggestionEngine.getSuggestions(context)
    }
    
    // Generate proactive insights
    val proactiveInsights = remember {
        insightEngine.getInsights(
            upcomingReminders = 2,
            overdueTasks = 0,
            pendingTasks = 3
        ).filter { it.id !in dismissedInsights }
    }
    
    // Autocomplete suggestions
    val autocompleteSuggestions = remember(userInputText) {
        getAutocompleteSuggestions(userInputText)
    }
    
    // Voice manager setup
    val voiceManager = remember { VoiceManager(context) }
    val ttsManager = remember { TTSManager(context) }

    // State for showing voice errors to user
    var voiceErrorMessage by remember { mutableStateOf<String?>(null) }
    
    val voiceCallback = remember {
        object : VoiceManager.VoiceCallback {
            override fun onResult(text: String) {
                coroutineScope.launch {
                    android.util.Log.d("EnhancedChatScreen", "Voice result received: '$text'")
                    isListening = false
                    voiceErrorMessage = null
                    if (text.isNotBlank()) {
                        userInputText = text
                        chatViewModel.sendMessage(text)
                        userInputText = ""
                    }
                    com.example.voicereminder.widget.WakeWordService.resume()
                }
            }
            override fun onError(error: String) { 
                android.util.Log.e("EnhancedChatScreen", "Voice error: $error")
                isListening = false 
                voiceErrorMessage = error
                com.example.voicereminder.widget.WakeWordService.resume()
                // Clear error after 3 seconds
                coroutineScope.launch {
                    kotlinx.coroutines.delay(3000)
                    voiceErrorMessage = null
                }
            }
            override fun onReadyForSpeech() { 
                android.util.Log.d("EnhancedChatScreen", "Ready for speech")
                isListening = true 
                voiceErrorMessage = null
            }
            override fun onEndOfSpeech() {
                android.util.Log.d("EnhancedChatScreen", "End of speech detected")
            }
        }
    }

    LaunchedEffect(Unit) {
        voiceManager.setCallback(voiceCallback)
        // Force refresh settings and recheck connection every time screen loads
        chatViewModel.recheckConnection(force = true)
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
            ttsManager.shutdown()
        }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Main Layout
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Quick Actions Bar with Clear Chat option
            ChatQuickActionsBar(
                onClearChat = { chatViewModel.clearChat() }
            )

            // Connection Status Banner
            connectionStatus?.let { status ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = status,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Voice Error Banner - shows when voice recognition fails
            voiceErrorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Proactive Insights Banner
            ProactiveInsightBanner(
                insights = proactiveInsights,
                onInsightClick = { insight ->
                    // Handle insight action
                    chatViewModel.sendMessage(insight.action)
                },
                onDismiss = { insight ->
                    dismissedInsights = dismissedInsights + insight.id
                }
            )

            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(messages.size) { index ->
                    val message = messages[index]
                    
                    MessageWithActions(
                        message = message.text,
                        isUser = message.isUser,
                        onRetry = if (!message.isUser) {
                            { /* TODO: Implement retry */ }
                        } else null,
                        onEdit = if (message.isUser) {
                            { text -> userInputText = text }
                        } else null,
                        onDelete = { /* TODO: Implement delete */ }
                    ) {
                        if (message.isUser) {
                            UserMessageBubble(message = message.text)
                        } else {
                            AIMessageBubble(message = message.text)
                        }
                    }
                }

                // Typing indicator
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }

                // Smart suggestion chips
                item {
                    SmartSuggestionChipsRow(
                        suggestions = smartSuggestions,
                        onSuggestionClick = { suggestion ->
                            if (suggestion.command.endsWith(" ")) {
                                userInputText = suggestion.command
                            } else {
                                chatViewModel.sendMessage(suggestion.command)
                            }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Enhanced Input Bar
            EnhancedInputBar(
                text = userInputText,
                onTextChange = { userInputText = it },
                onSend = {
                    if (userInputText.isNotBlank()) {
                        val textToSend = userInputText
                        userInputText = ""
                        chatViewModel.sendMessage(textToSend)
                    }
                },
                onVoiceClick = {
                    if (isListening) {
                        voiceManager.stopListening()
                        isListening = false
                        com.example.voicereminder.widget.WakeWordService.resume()
                    } else {
                        if (!voiceManager.isAvailable()) return@EnhancedInputBar
                        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            com.example.voicereminder.widget.WakeWordService.pause()
                            voiceManager.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                onQuickCommandClick = { showQuickCommandPanel = true },
                isListening = isListening,
                autocompleteSuggestions = autocompleteSuggestions
            )
        }
        
        // Quick Command Panel (overlay)
        QuickCommandPanel(
            isVisible = showQuickCommandPanel,
            onCommandSelected = { command ->
                userInputText = command.template
            },
            onDismiss = { showQuickCommandPanel = false }
        )
    }
}

/**
 * User message bubble with long-press support
 */
@Composable
fun UserMessageBubble(message: String) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 4.dp
            ),
            modifier = Modifier
                .padding(start = 48.dp, end = 4.dp)
                .widthIn(max = 300.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(12.dp),
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * AI message bubble
 */
@Composable
fun AIMessageBubble(message: String) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            modifier = Modifier
                .padding(end = 48.dp, start = 4.dp)
                .widthIn(max = 300.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp),
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Typing indicator with animated dots
 */
@Composable
fun TypingIndicator() {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "typing")
                val dotColor = MaterialTheme.colorScheme.onSurfaceVariant

                repeat(3) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

/**
 * Smart suggestion chips row
 */
@Composable
fun SmartSuggestionChipsRow(
    suggestions: List<SmartSuggestion>,
    onSuggestionClick: (SmartSuggestion) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { 
                    Text(
                        text = suggestion.label,
                        fontSize = 13.sp
                    ) 
                },
                icon = suggestion.icon?.let { icon ->
                    {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }
    }
}
