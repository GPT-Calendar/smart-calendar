package com.example.voicereminder.presentation.alarms

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.domain.models.Alarm
import com.example.voicereminder.domain.models.SnoozeOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun AlarmTriggerScreen(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSnoozeOptions by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 200f
    
    // Pulsing animation for the alarm icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Current time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Text(
                    text = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Text(
                    text = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d")),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
            
            // Center section - Alarm info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing alarm icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .alpha(alpha)
                        .background(
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Alarm time
                Text(
                    text = alarm.getFormattedTime(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Alarm label
                Text(
                    text = alarm.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                
                if (alarm.isRepeating()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alarm.getRepeatDescription(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Bottom section - Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                // Snooze button
                OutlinedButton(
                    onClick = { showSnoozeOptions = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        Icons.Default.Snooze,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Snooze",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dismiss slider
                DismissSlider(
                    onDismiss = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Slide to dismiss",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
            }
        }
    }
    
    // Snooze options dialog
    if (showSnoozeOptions) {
        AlertDialog(
            onDismissRequest = { showSnoozeOptions = false },
            icon = { Icon(Icons.Default.Snooze, contentDescription = null) },
            title = { Text("Snooze for") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SnoozeOption.DEFAULT_OPTIONS.forEach { option ->
                        TextButton(
                            onClick = {
                                showSnoozeOptions = false
                                onSnooze(option.minutes)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSnoozeOptions = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DismissSlider(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val maxDrag = 250f
    val dismissThreshold = 200f
    
    val progress = (abs(dragOffset) / maxDrag).coerceIn(0f, 1f)
    
    Box(
        modifier = modifier
            .height(64.dp)
            .background(
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                RoundedCornerShape(32.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Progress indicator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                        RoundedCornerShape(28.dp)
                    )
            )
        }
        
        // Slider thumb
        Box(
            modifier = Modifier
                .offset(x = dragOffset.coerceIn(0f, maxDrag).dp)
                .padding(4.dp)
                .size(56.dp)
                .background(
                    if (progress > 0.8f) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onPrimary,
                    CircleShape
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset > dismissThreshold) {
                                onDismiss()
                            } else {
                                dragOffset = 0f
                            }
                        },
                        onDragCancel = {
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset = (dragOffset + dragAmount).coerceIn(0f, maxDrag)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (progress > 0.8f) Icons.Default.Close else Icons.Default.ChevronRight,
                contentDescription = "Dismiss",
                tint = if (progress > 0.8f) MaterialTheme.colorScheme.onError
                       else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Dismiss text
        Text(
            text = if (progress > 0.8f) "Release to dismiss" else "Slide to dismiss",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(1f - progress)
        )
    }
}
