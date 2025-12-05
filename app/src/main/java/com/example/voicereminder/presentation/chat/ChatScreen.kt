package com.example.voicereminder.presentation.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.presentation.TTSManager
import com.example.voicereminder.presentation.VoiceManager
import com.example.voicereminder.presentation.chat.components.*
import com.example.voicereminder.presentation.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
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

    var userInputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    
    val messages by chatViewModel.messages.collectAsState()
    val isTyping by chatViewModel.isTyping.collectAsState()
    val connectionStatus by chatViewModel.connectionStatus.collectAsState()
    
    LaunchedEffect(Unit) {
        // Force refresh settings and recheck connection every time screen loads
        chatViewModel.recheckConnection(force = true)
    }
    
    // Smart suggestions engine
    val suggestionEngine = remember { SmartSuggestionEngine() }
    var showQuickCommandPanel by remember { mutableStateOf(false) }
    
    // Generate smart suggestions based on context
    val smartSuggestions = remember(messages) {
        val context = SuggestionContext(
            timeOfDay = suggestionEngine.getCurrentTimeOfDay(),
            hasOverdueTasks = false,
            hasUpcomingReminders = false,
            lastAction = null
        )
        suggestionEngine.getSuggestions(context)
    }
    
    // Autocomplete suggestions
    val autocompleteSuggestions = remember(userInputText) {
        getAutocompleteSuggestions(userInputText)
    }
    
    // Legacy sample prompts (fallback)
    val samplePrompts = smartSuggestions.map { it.label }.ifEmpty {
        listOf("Set a reminder", "Check my schedule", "Track expenses", "What's new?")
    }

    val voiceManager = remember { VoiceManager(context) }
    val ttsManager = remember { TTSManager(context) }

    // State for showing voice errors to user
    var voiceErrorMessage by remember { mutableStateOf<String?>(null) }
    
    val voiceCallback = remember {
        object : VoiceManager.VoiceCallback {
            override fun onResult(text: String) {
                coroutineScope.launch {
                    android.util.Log.d("ChatScreen", "Voice result received: '$text'")
                    isListening = false
                    voiceErrorMessage = null
                    if (text.isNotBlank()) {
                        userInputText = text
                        chatViewModel.sendMessage(text)
                        userInputText = ""
                    }
                    // Resume wake word service after voice input completes
                    com.example.voicereminder.widget.WakeWordService.resume()
                }
            }
            override fun onError(error: String) {
                android.util.Log.e("ChatScreen", "Voice error: $error")
                isListening = false
                voiceErrorMessage = error
                // Resume wake word service on error too
                com.example.voicereminder.widget.WakeWordService.resume()
                // Clear error after 3 seconds
                coroutineScope.launch {
                    kotlinx.coroutines.delay(3000)
                    voiceErrorMessage = null
                }
            }
            override fun onReadyForSpeech() { 
                android.util.Log.d("ChatScreen", "Ready for speech")
                isListening = true 
                voiceErrorMessage = null
            }
            override fun onEndOfSpeech() {
                android.util.Log.d("ChatScreen", "End of speech detected")
            }
        }
    }

    val ttsCallback = remember {
        object : TTSManager.TTSCallback {
            override fun onSpeakingStarted() {}
            override fun onSpeakingCompleted() {}
            override fun onError(error: String) {}
        }
    }

    LaunchedEffect(Unit) {
        voiceManager.setCallback(voiceCallback)
        ttsManager.setCallback(ttsCallback)
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
            ttsManager.shutdown()
        }
    }

    // Main Layout - Use theme-aware background color
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Quick Actions Bar (Wake Word, Floating Chat) - replaces old top bar
        ChatQuickActionsBar()

        // Connection Status Banner - theme-aware colors
        connectionStatus?.let { status ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "⚠️ $status",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Voice Error Banner - shows when voice recognition fails
        voiceErrorMessage?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = "🎤 $error",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Main Content Area (60% White) - Include sample messages demonstrating embedded data
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            state = rememberLazyListState(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Sample messages to demonstrate embedded data
            item {
                AIBubbleWithEmbeddedData(
                    message = "Here's your weekly spending summary and upcoming calendar events:",
                    showFinanceData = true,
                    showCalendarData = true
                )
            }

            items(messages.size) { index ->
                val message = messages[index]
                if (message.isUser) {
                    // User Message Bubble - theme-aware colors
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = 18.dp,
                                bottomEnd = 4.dp // Pointed bottom-right
                            ),
                            modifier = Modifier
                                .padding(start = 40.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                                .widthIn(max = 280.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = message.text,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                } else {
                    // AI Message Bubble - theme-aware colors
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
                                .padding(end = 40.dp, start = 8.dp, top = 4.dp, bottom = 4.dp)
                                .widthIn(max = 280.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = message.text,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            if (isTyping) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
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
            }

            // Smart suggestion chips
            item {
                SmartSuggestionChipsEnhanced(
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
        
        // Quick Command Panel (overlay at bottom)
        QuickCommandPanel(
            isVisible = showQuickCommandPanel,
            onCommandSelected = { command ->
                userInputText = command.template
            },
            onDismiss = { showQuickCommandPanel = false }
        )

        // Enhanced Input Field with quick commands button
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
}

/**
 * Enhanced smart suggestion chips with icons
 */
@Composable
fun SmartSuggestionChipsEnhanced(
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

/**
 * Minimal Top Bar - Compact bar with profile and menu icons only
 * Uses theme-aware colors for proper light/dark mode support
 */
@Composable
fun MinimalTopBar(
    onSettingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onFloatingChatClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isWakeWordEnabled by remember { 
        mutableStateOf(com.example.voicereminder.widget.WakeWordService.isServiceRunning()) 
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Icon (left)
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color(0xFF5D83FF),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Wake Word Toggle - "Hey Kiro" activation
            IconButton(
                onClick = {
                    if (isWakeWordEnabled) {
                        com.example.voicereminder.widget.WakeWordService.stop(context)
                        isWakeWordEnabled = false
                    } else {
                        com.example.voicereminder.widget.WakeWordService.start(context)
                        isWakeWordEnabled = true
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isWakeWordEnabled) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isWakeWordEnabled) "Disable Kiro wake word" else "Enable Kiro wake word",
                    tint = if (isWakeWordEnabled) Color.White else Color(0xFF757575),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // Floating Chat Button - starts floating overlay
            IconButton(
                onClick = {
                    if (onFloatingChatClick != null) {
                        onFloatingChatClick()
                    } else {
                        com.example.voicereminder.widget.FloatingChatService.start(context)
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF5D83FF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = "Start Floating Chat",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Menu Icon (right)
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color(0xFF5D83FF),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Chat Quick Actions Bar - Compact bar with wake word and floating chat toggles
 * Shown below the global top bar on the chat screen
 * Uses theme-aware colors for proper light/dark mode support
 */
@Composable
fun ChatQuickActionsBar(
    onClearChat: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isWakeWordEnabled by remember { 
        mutableStateOf(com.example.voicereminder.widget.WakeWordService.isServiceRunning()) 
    }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    
    // Clear chat confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Chat") },
            text = { Text("Are you sure you want to clear all chat messages? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearChat?.invoke()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wake Word Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        if (isWakeWordEnabled) {
                            com.example.voicereminder.widget.WakeWordService.stop(context)
                            isWakeWordEnabled = false
                        } else {
                            com.example.voicereminder.widget.WakeWordService.start(context)
                            isWakeWordEnabled = true
                        }
                    }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isWakeWordEnabled) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isWakeWordEnabled) Color.White else Color(0xFF757575),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isWakeWordEnabled) "Hey Kiro: ON" else "Hey Kiro: OFF",
                    fontSize = 13.sp,
                    color = if (isWakeWordEnabled) Color(0xFF4CAF50) else Color(0xFF666666)
                )
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color(0xFFDDDDDD))
            )
            
            // Floating Chat Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        com.example.voicereminder.widget.FloatingChatService.start(context)
                    }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF5D83FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Floating Chat",
                    fontSize = 13.sp,
                    color = Color(0xFF5D83FF)
                )
            }
            
            // Clear Chat Button (only show if callback provided)
            if (onClearChat != null) {
                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0xFFDDDDDD))
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showClearConfirmDialog = true }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear chat",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clear",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalBottomNavigation(
    currentScreen: String = "assistant",
    onNavigate: (String) -> Unit
) {
    // Check if dark mode - use isSystemInDarkTheme or check background luminance
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // 60-30-10 colors for light mode, theme colors for dark mode
    val navBackgroundColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surface
    } else {
        Color(0xFF305CDE) // Deep Blue - 30% allocation for light mode
    }
    
    val selectedColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White // White for selected in light mode
    }
    
    val unselectedColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color.White.copy(alpha = 0.5f) // Dim white for unselected in light mode
    }
    
    val calendarButtonColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White
    }
    
    val calendarIconColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        Color(0xFF305CDE)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // Bottom navigation bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter),
            color = navBackgroundColor,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side items: Chat, Planner
                val leftItems = listOf(
                    "assistant" to "Chat",
                    "planner" to "Planner"
                )
                
                leftItems.forEach { (route, title) ->
                    val isSelected = when {
                        route == "planner" && currentScreen in listOf("planner", "tasks", "alarms", "task_detail", "alarm_detail") -> true
                        else -> route == currentScreen
                    }
                    val iconColor = if (isSelected) selectedColor else unselectedColor
                    val textColor = if (isSelected) selectedColor else unselectedColor

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable { onNavigate(route) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .widthIn(min = 56.dp)
                            .heightIn(min = 56.dp)
                    ) {
                        Icon(
                            imageVector = when (route) {
                                "assistant" -> Icons.Default.Chat
                                "planner" -> Icons.Default.EventNote
                                else -> Icons.Default.Home
                            },
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                
                // Spacer for center calendar button
                Spacer(modifier = Modifier.width(64.dp))
                
                // Right side items: Finance, Map
                val rightItems = listOf(
                    "finance" to "Finance",
                    "map" to "Map"
                )
                
                rightItems.forEach { (route, title) ->
                    val isSelected = route == currentScreen
                    val iconColor = if (isSelected) selectedColor else unselectedColor
                    val textColor = if (isSelected) selectedColor else unselectedColor

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable { onNavigate(route) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .widthIn(min = 56.dp)
                            .heightIn(min = 56.dp)
                    ) {
                        Icon(
                            imageVector = when (route) {
                                "finance" -> Icons.Default.AccountBalance
                                "map" -> Icons.Default.Map
                                else -> Icons.Default.Home
                            },
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        // Floating Calendar button in center
        val isCalendarSelected = currentScreen == "calendar"
        
        FloatingActionButton(
            onClick = { onNavigate("calendar") },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(56.dp),
            shape = CircleShape,
            containerColor = if (isCalendarSelected) {
                if (isDarkTheme) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFFD700)
            } else {
                calendarButtonColor
            },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Calendar",
                tint = if (isCalendarSelected) {
                    if (isDarkTheme) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF305CDE)
                } else {
                    calendarIconColor
                },
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun MinimalUserBubble(message: String) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 4.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp),
            color = MinimalChatBubbleUser
        ) {
            Text(
                text = message,
                color = MinimalChatTextDark,
                modifier = Modifier.padding(12.dp),
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun MinimalAIBubble(message: String) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp),
            color = MinimalChatBubbleAI
        ) {
            Text(
                text = message,
                color = MinimalChatTextDark,
                modifier = Modifier.padding(12.dp),
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun MinimalTypingIndicator() {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MinimalChatBubbleAI,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "typing")
                
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
                            .background(MinimalTextGrey.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
fun MinimalSuggestionChips(
    prompts: List<String>,
    onPromptClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(prompts) { prompt ->
                Surface(
                    onClick = { onPromptClick(prompt) },
                    shape = RoundedCornerShape(20.dp),
                    color = MinimalWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorderGrey),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = prompt,
                        fontSize = 14.sp,
                        color = MinimalTextGrey,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AIBubbleWithEmbeddedData(
    message: String,
    showFinanceData: Boolean = false,
    showCalendarData: Boolean = false
) {
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
                .padding(end = 40.dp, start = 8.dp, top = 4.dp, bottom = 4.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                if (showFinanceData) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Embedded Finance Chart
                    FinanceChart()
                }

                if (showCalendarData) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Embedded Calendar Summary
                    CalendarSummary()
                }
            }
        }
    }
}

@Composable
fun FinanceChart() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Weekly Spending",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            // Extract theme colors for Canvas
            val chartLineColor = MaterialTheme.colorScheme.primary
            val gridLineColor = MaterialTheme.colorScheme.outline

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                val width = size.width
                val height = size.height
                val padding = 8f
                val graphWidth = width - 2 * padding
                val graphHeight = height - 2 * padding

                // Draw grid lines
                drawLine(
                    color = gridLineColor,
                    start = Offset(padding, height / 2),
                    end = Offset(width - padding, height / 2),
                    strokeWidth = 1f
                )

                // Draw chart line
                val data = listOf(20f, 40f, 30f, 45f, 35f, 50f, 40f) // Sample data
                val maxValue = data.maxOrNull() ?: 1f
                val segmentWidth = if (data.size > 1) graphWidth / (data.size - 1) else 0f

                for (i in data.indices) {
                    val x = padding + i * segmentWidth
                    val y = height - padding - (data[i] / maxValue) * graphHeight

                    if (i > 0 && segmentWidth > 0) {
                        val prevX = padding + (i - 1) * segmentWidth
                        val prevY = height - padding - (data[i - 1] / maxValue) * graphHeight

                        drawLine(
                            color = chartLineColor,
                            start = Offset(prevX, prevY),
                            end = Offset(x, y),
                            strokeWidth = 3f
                        )
                    }

                    // Draw circle for each point
                    drawCircle(
                        color = chartLineColor,
                        radius = 4f,
                        center = Offset(x, y)
                    )
                }
            }

            Text(
                text = "Total: $230",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CalendarSummary() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "15",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Meeting with Team",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "2:00 PM - 3:00 PM",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalInputField(
    userInputText: String,
    onUserInputTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onVoiceClick: () -> Unit,
    isListening: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone Button - theme-aware accent color
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Start voice input",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text Input - theme-aware colors
            TextField(
                value = userInputText,
                onValueChange = onUserInputTextChange,
                placeholder = {
                    Text(
                        "Message...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 120.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = false,
                maxLines = 4,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            // Send Button
            IconButton(
                onClick = {
                    if (userInputText.isNotBlank()) {
                        onSendClick()
                    }
                },
                enabled = userInputText.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = if (userInputText.isNotBlank()) Color(0xFF5D83FF) else Color(0xFFCCCCCC), // Light Blue accent (#5D83FF) - 10% allocation when enabled
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
