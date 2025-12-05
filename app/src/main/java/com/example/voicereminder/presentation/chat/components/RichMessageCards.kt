package com.example.voicereminder.presentation.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Rich card for displaying reminder information
 */
@Composable
fun ReminderCard(
    title: String,
    scheduledTime: LocalDateTime,
    isCompleted: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSnooze: () -> Unit = {},
    onComplete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatDateTime(scheduledTime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status indicator
                if (isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChip(
                    label = "Snooze",
                    icon = Icons.Default.Snooze,
                    onClick = onSnooze
                )
                ActionChip(
                    label = "Edit",
                    icon = Icons.Default.Edit,
                    onClick = onEdit
                )
                ActionChip(
                    label = "Delete",
                    icon = Icons.Default.Delete,
                    onClick = onDelete,
                    isDestructive = true
                )
            }
        }
    }
}

/**
 * Rich card for displaying task information
 */
@Composable
fun TaskCard(
    title: String,
    description: String? = null,
    dueDate: LocalDateTime? = null,
    priority: String = "MEDIUM",
    isCompleted: Boolean = false,
    onComplete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val priorityColor = when (priority.uppercase()) {
        "HIGH" -> Color(0xFFF44336)
        "LOW" -> Color(0xFF9E9E9E)
        else -> Color(0xFF2196F3)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = priorityColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Checkbox
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onComplete() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = priorityColor
                    )
                )
                
                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (dueDate != null) {
                        Text(
                            text = "Due: ${formatDateTime(dueDate)}",
                            fontSize = 11.sp,
                            color = priorityColor
                        )
                    }
                }
                
                // Priority badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = priorityColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = priority,
                        fontSize = 10.sp,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isCompleted) {
                    ActionChip(
                        label = "Complete",
                        icon = Icons.Default.Check,
                        onClick = onComplete
                    )
                }
                ActionChip(
                    label = "Edit",
                    icon = Icons.Default.Edit,
                    onClick = onEdit
                )
                ActionChip(
                    label = "Delete",
                    icon = Icons.Default.Delete,
                    onClick = onDelete,
                    isDestructive = true
                )
            }
        }
    }
}

/**
 * Rich card for displaying alarm information
 */
@Composable
fun AlarmCard(
    label: String,
    time: String,
    repeatDays: List<String> = emptyList(),
    isEnabled: Boolean = true,
    onToggle: () -> Unit = {},
    onSnooze: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9800).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = time,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (repeatDays.isNotEmpty()) {
                        Text(
                            text = repeatDays.joinToString(", "),
                            fontSize = 11.sp,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
                
                // Toggle switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFF9800),
                        checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.5f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChip(
                    label = "Snooze",
                    icon = Icons.Default.Snooze,
                    onClick = onSnooze
                )
                ActionChip(
                    label = "Delete",
                    icon = Icons.Default.Delete,
                    onClick = onDelete,
                    isDestructive = true
                )
            }
        }
    }
}

/**
 * Rich card for displaying transaction information
 */
@Composable
fun TransactionCard(
    amount: Double,
    currency: String = "ETB",
    description: String,
    isIncome: Boolean,
    timestamp: LocalDateTime,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val color = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val sign = if (isIncome) "+" else "-"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = description,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = formatDateTime(timestamp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Amount
                Text(
                    text = "$sign${amount.toInt()} $currency",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = color
                )
            }
        }
    }
}

/**
 * Insight card for proactive AI suggestions
 */
@Composable
fun InsightCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Reusable action chip
 */
@Composable
fun ActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val color = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    
    AssistChip(
        onClick = onClick,
        label = { 
            Text(
                text = label, 
                fontSize = 11.sp,
                color = color
            ) 
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = null,
        modifier = Modifier.height(28.dp)
    )
}

/**
 * Format LocalDateTime for display
 */
private fun formatDateTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val tomorrow = today.plusDays(1)
    val date = dateTime.toLocalDate()
    
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    
    val timeStr = dateTime.format(timeFormatter)
    
    return when (date) {
        today -> "Today at $timeStr"
        tomorrow -> "Tomorrow at $timeStr"
        else -> "${dateTime.format(dateFormatter)} at $timeStr"
    }
}
