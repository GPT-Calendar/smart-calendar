# Chat System Enhancement Design

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      ChatScreen                              │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ProactiveInsightBanner                  │   │
│  │  "Good morning! You have 3 tasks due today"         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Message List (LazyColumn)               │   │
│  │  ┌─────────────────────────────────────────────┐    │   │
│  │  │ UserMessageBubble (long-press for actions)  │    │   │
│  │  └─────────────────────────────────────────────┘    │   │
│  │  ┌─────────────────────────────────────────────┐    │   │
│  │  │ AIMessageBubble                             │    │   │
│  │  │  ├─ Text content                            │    │   │
│  │  │  ├─ RichCard (if structured data)           │    │   │
│  │  │  └─ QuickActionButtons                      │    │   │
│  │  └─────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           SmartSuggestionChips (dynamic)             │   │
│  │  [Check schedule] [Add expense] [Set reminder]       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              EnhancedInputBar                        │   │
│  │  [+] [🎤] [___autocomplete input___] [➤]            │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Component Design

### 1. Enhanced ChatMessage Data Model

```kotlin
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val messageType: MessageType = MessageType.TEXT,
    val richContent: RichContent? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val actions: List<MessageAction> = emptyList()
)

enum class MessageType {
    TEXT,
    REMINDER_CARD,
    TASK_CARD,
    ALARM_CARD,
    TRANSACTION_CARD,
    CALENDAR_CARD,
    INSIGHT_CARD,
    ERROR
}

sealed class RichContent {
    data class ReminderCard(val reminder: Reminder, val canEdit: Boolean = true) : RichContent()
    data class TaskCard(val task: Task, val canComplete: Boolean = true) : RichContent()
    data class AlarmCard(val alarm: Alarm, val canSnooze: Boolean = true) : RichContent()
    data class TransactionCard(val transaction: Transaction) : RichContent()
    data class InsightCard(val title: String, val items: List<InsightItem>) : RichContent()
}

enum class MessageStatus {
    SENDING, SENT, DELIVERED, ERROR, QUEUED_OFFLINE
}

data class MessageAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit
)
```

### 2. Smart Suggestion Engine

```kotlin
class SmartSuggestionEngine(
    private val reminderManager: ReminderManager,
    private val taskManager: TaskManager,
    private val financeRepository: FinanceRepository
) {
    fun getSuggestions(context: SuggestionContext): List<SmartSuggestion> {
        val suggestions = mutableListOf<SmartSuggestion>()
        
        // Time-based suggestions
        when (context.timeOfDay) {
            TimeOfDay.MORNING -> {
                suggestions.add(SmartSuggestion("☀️ Good morning summary", "show_summary"))
                suggestions.add(SmartSuggestion("📅 Today's schedule", "show_schedule"))
            }
            TimeOfDay.EVENING -> {
                suggestions.add(SmartSuggestion("💰 Today's expenses", "show_expenses"))
                suggestions.add(SmartSuggestion("✅ Review tasks", "show_tasks"))
            }
        }
        
        // Context-based suggestions
        if (context.hasOverdueTasks) {
            suggestions.add(SmartSuggestion("⚠️ Overdue tasks", "show_overdue"))
        }
        if (context.hasUpcomingReminders) {
            suggestions.add(SmartSuggestion("🔔 Upcoming reminders", "show_upcoming"))
        }
        
        // Recent action suggestions
        context.lastAction?.let { action ->
            suggestions.addAll(getFollowUpSuggestions(action))
        }
        
        return suggestions.take(4) // Limit to 4 suggestions
    }
}

data class SmartSuggestion(
    val label: String,
    val command: String,
    val icon: ImageVector? = null
)
```

### 3. Quick Command Panel

```kotlin
@Composable
fun QuickCommandPanel(
    isVisible: Boolean,
    onCommandSelected: (QuickCommand) -> Unit,
    onDismiss: () -> Unit
) {
    val commands = listOf(
        QuickCommand("📅", "New Reminder", "remind me to "),
        QuickCommand("✅", "New Task", "create task "),
        QuickCommand("⏰", "Set Alarm", "set alarm at "),
        QuickCommand("💰", "Add Expense", "spent "),
        QuickCommand("💵", "Add Income", "received "),
        QuickCommand("📍", "Location Reminder", "remind me when I reach "),
        QuickCommand("🔄", "Recurring", "remind me every day to ")
    )
    
    AnimatedVisibility(visible = isVisible) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(commands) { command ->
                    QuickCommandItem(command, onClick = { onCommandSelected(command) })
                }
            }
        }
    }
}
```

### 4. Message Action Menu

```kotlin
@Composable
fun MessageActionMenu(
    message: ChatMessage,
    onCopy: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = true, onDismissRequest = {}) {
        DropdownMenuItem(
            text = { Text("Copy") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
            onClick = onCopy
        )
        if (!message.isUser) {
            DropdownMenuItem(
                text = { Text("Retry") },
                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                onClick = onRetry
            )
        }
        if (message.isUser) {
            DropdownMenuItem(
                text = { Text("Edit & Resend") },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = onEdit
            )
        }
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Default.Share, null) },
            onClick = onShare
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            leadingIcon = { Icon(Icons.Default.Delete, null) },
            onClick = onDelete
        )
    }
}
```

### 5. Rich Content Cards

```kotlin
@Composable
fun ReminderCard(
    reminder: Reminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSnooze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(reminder.message, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                formatDateTime(reminder.scheduledTime),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onSnooze, label = { Text("Snooze") })
                AssistChip(onClick = onEdit, label = { Text("Edit") })
                AssistChip(onClick = onDelete, label = { Text("Delete") })
            }
        }
    }
}
```

### 6. Proactive Insight Banner

```kotlin
@Composable
fun ProactiveInsightBanner(
    insights: List<ProactiveInsight>,
    onInsightClick: (ProactiveInsight) -> Unit,
    onDismiss: () -> Unit
) {
    if (insights.isEmpty()) return
    
    val currentInsight = insights.first()
    
    AnimatedVisibility(visible = true) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = currentInsight.backgroundColor
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(currentInsight.icon, null, tint = currentInsight.iconColor)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentInsight.title, fontWeight = FontWeight.Medium)
                    Text(currentInsight.subtitle, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Dismiss")
                }
            }
        }
    }
}

data class ProactiveInsight(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color,
    val action: String
)
```

### 7. Enhanced Input with Autocomplete

```kotlin
@Composable
fun EnhancedInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onQuickCommandClick: () -> Unit,
    suggestions: List<AutocompleteSuggestion>,
    isListening: Boolean
) {
    Column {
        // Autocomplete suggestions
        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { onTextChange(suggestion.completion) },
                        label = { Text(suggestion.label) }
                    )
                }
            }
        }
        
        // Input bar
        Surface(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick command button
                IconButton(onClick = onQuickCommandClick) {
                    Icon(Icons.Default.Add, "Quick commands")
                }
                
                // Voice button
                IconButton(onClick = onVoiceClick) {
                    Icon(
                        if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        "Voice input",
                        tint = if (isListening) MaterialTheme.colorScheme.error 
                               else MaterialTheme.colorScheme.primary
                    )
                }
                
                // Text input
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message...") },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    )
                )
                
                // Send button
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }
    }
}
```

## State Management

```kotlin
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Connected,
    val smartSuggestions: List<SmartSuggestion> = emptyList(),
    val proactiveInsights: List<ProactiveInsight> = emptyList(),
    val autocompleteSuggestions: List<AutocompleteSuggestion> = emptyList(),
    val showQuickCommandPanel: Boolean = false,
    val selectedMessage: ChatMessage? = null,
    val isOffline: Boolean = false
)

sealed class ConnectionStatus {
    object Connected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
    object Offline : ConnectionStatus()
}
```

## Implementation Priority

1. **Phase 1 (Core UX)**: Smart suggestions, Quick command panel, Message actions
2. **Phase 2 (Rich Content)**: Rich message cards, Proactive insights
3. **Phase 3 (Advanced)**: Autocomplete, Search, Offline support
