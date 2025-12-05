package com.example.voicereminder.presentation.chat.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.presentation.chat.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

/**
 * Search filter options
 */
data class SearchFilter(
    val query: String = "",
    val messageType: MessageTypeFilter = MessageTypeFilter.ALL,
    val dateRange: DateRange? = null,
    val onlyUserMessages: Boolean = false
)

enum class MessageTypeFilter {
    ALL,
    REMINDERS,
    TASKS,
    FINANCE,
    ALARMS
}

data class DateRange(
    val startDate: Long,
    val endDate: Long
)

/**
 * Search result with highlighted matches
 */
data class SearchResult(
    val message: ChatMessage,
    val matchIndices: List<IntRange>
)

/**
 * Conversation search bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    resultCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Close search"
                )
            }
            
            // Search input
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search messages...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                }
            )
            
            // Result count
            if (query.isNotEmpty()) {
                Text(
                    text = "$resultCount results",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

/**
 * Search filter chips
 */
@Composable
fun SearchFilterChips(
    currentFilter: MessageTypeFilter,
    onFilterChange: (MessageTypeFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MessageTypeFilter.values().forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { 
                    Text(
                        text = when (filter) {
                            MessageTypeFilter.ALL -> "All"
                            MessageTypeFilter.REMINDERS -> "Reminders"
                            MessageTypeFilter.TASKS -> "Tasks"
                            MessageTypeFilter.FINANCE -> "Finance"
                            MessageTypeFilter.ALARMS -> "Alarms"
                        },
                        fontSize = 12.sp
                    )
                },
                leadingIcon = if (currentFilter == filter) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

/**
 * Search result item with highlighted text
 */
@Composable
fun SearchResultItem(
    result: SearchResult,
    searchQuery: String,
    onClick: () -> Unit
) {
    val message = result.message
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar/Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (message.isUser) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (message.isUser) Icons.Default.Person else Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (message.isUser) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // Sender and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (message.isUser) "You" else "Assistant",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text(
                        text = dateFormat.format(Date(message.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Highlighted message text
                Text(
                    text = highlightSearchQuery(message.text, searchQuery),
                    fontSize = 14.sp,
                    maxLines = 3,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Highlight search query in text
 */
@Composable
fun highlightSearchQuery(text: String, query: String) = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var currentIndex = 0
    
    while (currentIndex < text.length) {
        val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
        
        if (matchIndex == -1) {
            append(text.substring(currentIndex))
            break
        }
        
        // Add text before match
        if (matchIndex > currentIndex) {
            append(text.substring(currentIndex, matchIndex))
        }
        
        // Add highlighted match
        withStyle(
            SpanStyle(
                background = Color(0xFFFFEB3B).copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
        ) {
            append(text.substring(matchIndex, matchIndex + query.length))
        }
        
        currentIndex = matchIndex + query.length
    }
}

/**
 * Search messages in conversation
 */
fun searchMessages(
    messages: List<ChatMessage>,
    filter: SearchFilter
): List<SearchResult> {
    if (filter.query.isBlank()) return emptyList()
    
    val lowerQuery = filter.query.lowercase()
    
    return messages
        .filter { message ->
            // Filter by user/AI
            if (filter.onlyUserMessages && !message.isUser) return@filter false
            
            // Filter by date range
            filter.dateRange?.let { range ->
                if (message.timestamp < range.startDate || message.timestamp > range.endDate) {
                    return@filter false
                }
            }
            
            // Filter by message type
            when (filter.messageType) {
                MessageTypeFilter.ALL -> true
                MessageTypeFilter.REMINDERS -> message.text.lowercase().contains("remind")
                MessageTypeFilter.TASKS -> message.text.lowercase().contains("task")
                MessageTypeFilter.FINANCE -> message.text.lowercase().let { 
                    it.contains("spent") || it.contains("expense") || it.contains("income") || it.contains("birr")
                }
                MessageTypeFilter.ALARMS -> message.text.lowercase().contains("alarm")
            }
        }
        .filter { message ->
            message.text.lowercase().contains(lowerQuery)
        }
        .map { message ->
            val matchIndices = findMatchIndices(message.text, filter.query)
            SearchResult(message, matchIndices)
        }
}

/**
 * Find all match indices in text
 */
private fun findMatchIndices(text: String, query: String): List<IntRange> {
    val indices = mutableListOf<IntRange>()
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var currentIndex = 0
    
    while (currentIndex < text.length) {
        val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
        if (matchIndex == -1) break
        
        indices.add(matchIndex until (matchIndex + query.length))
        currentIndex = matchIndex + 1
    }
    
    return indices
}

/**
 * Export conversation dialog
 */
@Composable
fun ExportConversationDialog(
    onExportText: () -> Unit,
    onExportJson: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Conversation") },
        text = {
            Column {
                Text("Choose export format:")
                Spacer(modifier = Modifier.height(16.dp))
                
                // Text export option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onExportText),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Plain Text", fontWeight = FontWeight.Medium)
                            Text(
                                "Simple text format",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // JSON export option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onExportJson),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("JSON", fontWeight = FontWeight.Medium)
                            Text(
                                "Structured data format",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
