package com.example.voicereminder.presentation.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Quick command data class
 */
data class QuickCommand(
    val emoji: String,
    val label: String,
    val template: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Predefined quick commands
 */
object QuickCommands {
    val commands = listOf(
        QuickCommand("📅", "Reminder", "Remind me to ", Icons.Default.Notifications, Color(0xFF4CAF50)),
        QuickCommand("✅", "Task", "Create task ", Icons.Default.CheckCircle, Color(0xFF2196F3)),
        QuickCommand("⏰", "Alarm", "Set alarm at ", Icons.Default.Alarm, Color(0xFFFF9800)),
        QuickCommand("💰", "Expense", "I spent ", Icons.Default.MoneyOff, Color(0xFFF44336)),
        QuickCommand("💵", "Income", "I received ", Icons.Default.AttachMoney, Color(0xFF4CAF50)),
        QuickCommand("📍", "Location", "Remind me when I reach ", Icons.Default.LocationOn, Color(0xFF9C27B0)),
        QuickCommand("🔄", "Recurring", "Remind me every day to ", Icons.Default.Repeat, Color(0xFF00BCD4)),
        QuickCommand("📋", "Schedule", "What's on my schedule?", Icons.Default.Event, Color(0xFF3F51B5))
    )
}

/**
 * Quick command panel that slides up from bottom
 */
@Composable
fun QuickCommandPanel(
    isVisible: Boolean,
    onCommandSelected: (QuickCommand) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header with drag handle
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Title
                Text(
                    text = "Quick Commands",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Command grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(180.dp)
                ) {
                    items(QuickCommands.commands) { command ->
                        QuickCommandItem(
                            command = command,
                            onClick = { 
                                onCommandSelected(command)
                                onDismiss()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * Individual quick command item
 */
@Composable
fun QuickCommandItem(
    command: QuickCommand,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(command.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = command.emoji,
                fontSize = 22.sp
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Label
        Text(
            text = command.label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Compact quick command row (alternative layout)
 */
@Composable
fun QuickCommandRow(
    onCommandSelected: (QuickCommand) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickCommands.commands.take(4).forEach { command ->
            QuickCommandChip(
                command = command,
                onClick = { onCommandSelected(command) }
            )
        }
    }
}

/**
 * Quick command as a chip
 */
@Composable
fun QuickCommandChip(
    command: QuickCommand,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = command.color.copy(alpha = 0.1f),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = command.emoji, fontSize = 14.sp)
            Text(
                text = command.label,
                fontSize = 12.sp,
                color = command.color
            )
        }
    }
}
