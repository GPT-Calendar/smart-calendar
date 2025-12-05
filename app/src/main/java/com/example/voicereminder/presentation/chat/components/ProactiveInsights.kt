package com.example.voicereminder.presentation.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Proactive insight data class
 */
data class ProactiveInsight(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color,
    val action: String,
    val priority: Int = 0
)

/**
 * Engine for generating proactive insights
 */
class ProactiveInsightEngine {
    
    /**
     * Generate insights based on current context
     */
    fun getInsights(
        upcomingReminders: Int = 0,
        overdueTasks: Int = 0,
        todayExpenses: Double = 0.0,
        budgetLimit: Double = 0.0,
        pendingTasks: Int = 0
    ): List<ProactiveInsight> {
        val insights = mutableListOf<ProactiveInsight>()
        val hour = LocalTime.now().hour
        
        // Morning greeting (5 AM - 10 AM)
        if (hour in 5..10) {
            insights.add(
                ProactiveInsight(
                    id = "morning_greeting",
                    title = "☀️ Good morning!",
                    subtitle = buildMorningSummary(pendingTasks, upcomingReminders),
                    icon = Icons.Default.WbSunny,
                    iconColor = Color(0xFFFF9800),
                    backgroundColor = Color(0xFFFF9800).copy(alpha = 0.1f),
                    action = "show_summary",
                    priority = 5
                )
            )
        }
        
        // Evening summary (6 PM - 9 PM)
        if (hour in 18..21) {
            insights.add(
                ProactiveInsight(
                    id = "evening_summary",
                    title = "🌙 Evening wrap-up",
                    subtitle = "Review your day's progress",
                    icon = Icons.Default.Nightlight,
                    iconColor = Color(0xFF5C6BC0),
                    backgroundColor = Color(0xFF5C6BC0).copy(alpha = 0.1f),
                    action = "show_daily_summary",
                    priority = 4
                )
            )
        }
        
        // Overdue tasks alert
        if (overdueTasks > 0) {
            insights.add(
                ProactiveInsight(
                    id = "overdue_tasks",
                    title = "⚠️ Overdue tasks",
                    subtitle = "$overdueTasks task${if (overdueTasks > 1) "s" else ""} need${if (overdueTasks == 1) "s" else ""} attention",
                    icon = Icons.Default.Warning,
                    iconColor = Color(0xFFF44336),
                    backgroundColor = Color(0xFFF44336).copy(alpha = 0.1f),
                    action = "show_overdue",
                    priority = 10
                )
            )
        }
        
        // Upcoming reminders (within 2 hours)
        if (upcomingReminders > 0) {
            insights.add(
                ProactiveInsight(
                    id = "upcoming_reminders",
                    title = "🔔 Coming up soon",
                    subtitle = "$upcomingReminders reminder${if (upcomingReminders > 1) "s" else ""} in the next 2 hours",
                    icon = Icons.Default.NotificationsActive,
                    iconColor = Color(0xFF4CAF50),
                    backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                    action = "show_upcoming",
                    priority = 8
                )
            )
        }
        
        // Budget alert
        if (budgetLimit > 0 && todayExpenses > budgetLimit * 0.8) {
            val percentage = ((todayExpenses / budgetLimit) * 100).toInt()
            insights.add(
                ProactiveInsight(
                    id = "budget_alert",
                    title = "💸 Budget alert",
                    subtitle = "You've used $percentage% of your daily budget",
                    icon = Icons.Default.MoneyOff,
                    iconColor = if (percentage >= 100) Color(0xFFF44336) else Color(0xFFFF9800),
                    backgroundColor = if (percentage >= 100) Color(0xFFF44336).copy(alpha = 0.1f) else Color(0xFFFF9800).copy(alpha = 0.1f),
                    action = "show_budget",
                    priority = 9
                )
            )
        }
        
        return insights.sortedByDescending { it.priority }
    }
    
    private fun buildMorningSummary(tasks: Int, reminders: Int): String {
        val parts = mutableListOf<String>()
        if (tasks > 0) parts.add("$tasks task${if (tasks > 1) "s" else ""}")
        if (reminders > 0) parts.add("$reminders reminder${if (reminders > 1) "s" else ""}")
        
        return if (parts.isEmpty()) {
            "Your day is clear!"
        } else {
            "You have ${parts.joinToString(" and ")} today"
        }
    }
}

/**
 * Proactive insight banner composable
 */
@Composable
fun ProactiveInsightBanner(
    insights: List<ProactiveInsight>,
    onInsightClick: (ProactiveInsight) -> Unit,
    onDismiss: (ProactiveInsight) -> Unit
) {
    if (insights.isEmpty()) return
    
    val currentInsight = insights.first()
    
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onInsightClick(currentInsight) },
            shape = RoundedCornerShape(12.dp),
            color = currentInsight.backgroundColor,
            tonalElevation = 2.dp
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
                        .background(currentInsight.iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        currentInsight.icon,
                        contentDescription = null,
                        tint = currentInsight.iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentInsight.title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentInsight.subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Dismiss button
                IconButton(
                    onClick = { onDismiss(currentInsight) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Multiple insights carousel
 */
@Composable
fun InsightsCarousel(
    insights: List<ProactiveInsight>,
    onInsightClick: (ProactiveInsight) -> Unit,
    onDismiss: (ProactiveInsight) -> Unit
) {
    if (insights.isEmpty()) return
    
    var currentIndex by remember { mutableStateOf(0) }
    val currentInsight = insights.getOrNull(currentIndex) ?: return
    
    Column {
        ProactiveInsightBanner(
            insights = listOf(currentInsight),
            onInsightClick = onInsightClick,
            onDismiss = { 
                onDismiss(it)
                if (currentIndex < insights.size - 1) {
                    currentIndex++
                }
            }
        )
        
        // Page indicator
        if (insights.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                insights.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(if (index == currentIndex) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentIndex) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}
