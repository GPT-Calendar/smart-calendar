package com.example.voicereminder.presentation.chat.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Autocomplete suggestion data
 */
data class AutocompleteSuggestion(
    val label: String,
    val completion: String
)

/**
 * Enhanced input bar with quick commands, voice, and autocomplete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onQuickCommandClick: () -> Unit,
    isListening: Boolean,
    autocompleteSuggestions: List<AutocompleteSuggestion> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Autocomplete suggestions
        AnimatedVisibility(
            visible = autocompleteSuggestions.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(autocompleteSuggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { onTextChange(suggestion.completion) },
                        label = { Text(suggestion.label, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
        
        // Main input bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(4.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick command button
                IconButton(
                    onClick = onQuickCommandClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Quick commands",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Voice button with animation
                VoiceButton(
                    isListening = isListening,
                    onClick = onVoiceClick
                )
                
                // Text input
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp, max = 120.dp),
                    placeholder = {
                        Text(
                            "Message...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = false,
                    maxLines = 4,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                // Send button
                AnimatedVisibility(
                    visible = text.isNotBlank(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated voice button with listening indicator
 */
@Composable
fun VoiceButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isListening) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(44.dp)
    ) {
        // Pulse effect when listening
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(40.dp * scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha * 0.3f))
            )
        }
        
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop listening" else "Start voice input",
                tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Voice waveform visualization
 */
@Composable
fun VoiceWaveform(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Row(
        modifier = modifier
            .height(32.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = if (isActive) 24f else 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 300,
                        delayMillis = index * 100
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * Voice input overlay with transcription preview
 */
@Composable
fun VoiceInputOverlay(
    isVisible: Boolean,
    transcription: String,
    onCancel: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Waveform
                VoiceWaveform(isActive = true)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Transcription preview
                Text(
                    text = if (transcription.isNotEmpty()) transcription else "Listening...",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Cancel button
                TextButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Get autocomplete suggestions based on input text
 */
fun getAutocompleteSuggestions(text: String): List<AutocompleteSuggestion> {
    if (text.isBlank()) return emptyList()
    
    val lowerText = text.lowercase()
    
    return when {
        lowerText.startsWith("remind") -> listOf(
            AutocompleteSuggestion("remind me to...", "Remind me to "),
            AutocompleteSuggestion("remind me at...", "Remind me at "),
            AutocompleteSuggestion("remind me tomorrow", "Remind me tomorrow to ")
        )
        lowerText.startsWith("set") -> listOf(
            AutocompleteSuggestion("set alarm at...", "Set alarm at "),
            AutocompleteSuggestion("set reminder", "Set reminder to ")
        )
        lowerText.startsWith("create") || lowerText.startsWith("add") -> listOf(
            AutocompleteSuggestion("create task", "Create task "),
            AutocompleteSuggestion("add expense", "I spent "),
            AutocompleteSuggestion("add income", "I received ")
        )
        lowerText.startsWith("show") || lowerText.startsWith("what") -> listOf(
            AutocompleteSuggestion("show schedule", "What's on my schedule?"),
            AutocompleteSuggestion("show tasks", "Show my tasks"),
            AutocompleteSuggestion("show expenses", "Show today's expenses")
        )
        lowerText.contains("spent") || lowerText.contains("paid") -> listOf(
            AutocompleteSuggestion("... birr on", "$text birr on ")
        )
        else -> emptyList()
    }
}
