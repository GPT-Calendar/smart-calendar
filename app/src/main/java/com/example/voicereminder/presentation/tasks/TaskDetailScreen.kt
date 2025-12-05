package com.example.voicereminder.presentation.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voicereminder.data.Priority
import com.example.voicereminder.data.RecurrenceRule
import com.example.voicereminder.data.RecurrenceType
import com.example.voicereminder.data.TaskCategory
import com.example.voicereminder.domain.models.Task
import com.example.voicereminder.presentation.components.CategorySelector
import com.example.voicereminder.presentation.components.PrioritySelector
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: Task? = null,
    onSave: (title: String, description: String?, dueDate: LocalDateTime?, priority: Priority, category: TaskCategory, recurrenceRule: RecurrenceRule?) -> Unit,
    onDelete: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var selectedDate by remember { mutableStateOf(task?.dueDate?.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(task?.dueDate?.toLocalTime()) }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    var category by remember { mutableStateOf(task?.category ?: TaskCategory.PERSONAL) }
    var recurrenceType by remember { mutableStateOf(task?.recurrenceRule?.type ?: RecurrenceType.NONE) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val isEditing = task != null
    val titleError = title.isBlank()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Task" else "New Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing && onDelete != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title") },
                placeholder = { Text("What do you need to do?") },
                isError = titleError && title.isNotEmpty(),
                supportingText = if (titleError && title.isNotEmpty()) {
                    { Text("Title is required") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                placeholder = { Text("Add more details...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )
            
            // Due Date Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Due Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date picker button
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                                    ?: "Select Date"
                            )
                        }
                        
                        // Time picker button
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                            enabled = selectedDate != null
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedTime?.format(DateTimeFormatter.ofPattern("h:mm a"))
                                    ?: "Select Time"
                            )
                        }
                    }
                    
                    // Clear date button
                    if (selectedDate != null) {
                        TextButton(
                            onClick = {
                                selectedDate = null
                                selectedTime = null
                            }
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear due date")
                        }
                    }
                }
            }
            
            // Priority Selector
            PrioritySelector(
                selectedPriority = priority,
                onPrioritySelected = { priority = it }
            )
            
            // Category Selector
            CategorySelector(
                selectedCategory = category,
                onCategorySelected = { category = it }
            )
            
            // Recurrence Selector
            RecurrenceSelector(
                selectedType = recurrenceType,
                onTypeSelected = { recurrenceType = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Button
            Button(
                onClick = {
                    val dueDateTime = if (selectedDate != null) {
                        LocalDateTime.of(selectedDate, selectedTime ?: LocalTime.of(9, 0))
                    } else null
                    
                    val rule = if (recurrenceType != RecurrenceType.NONE) {
                        RecurrenceRule(type = recurrenceType)
                    } else null
                    
                    onSave(
                        title.trim(),
                        description.trim().ifEmpty { null },
                        dueDateTime,
                        priority,
                        category,
                        rule
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    if (isEditing) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Save Changes" else "Create Task",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.toEpochDay()?.times(86400000)
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = LocalDate.ofEpochDay(millis / 86400000)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime?.hour ?: 9,
            initialMinute = selectedTime?.minute ?: 0
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete Task?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecurrenceSelector(
    selectedType: RecurrenceType,
    onTypeSelected: (RecurrenceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Repeat",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecurrenceChip(
                label = "None",
                isSelected = selectedType == RecurrenceType.NONE,
                onClick = { onTypeSelected(RecurrenceType.NONE) },
                modifier = Modifier.weight(1f)
            )
            RecurrenceChip(
                label = "Daily",
                isSelected = selectedType == RecurrenceType.DAILY,
                onClick = { onTypeSelected(RecurrenceType.DAILY) },
                modifier = Modifier.weight(1f)
            )
            RecurrenceChip(
                label = "Weekly",
                isSelected = selectedType == RecurrenceType.WEEKLY,
                onClick = { onTypeSelected(RecurrenceType.WEEKLY) },
                modifier = Modifier.weight(1f)
            )
            RecurrenceChip(
                label = "Monthly",
                isSelected = selectedType == RecurrenceType.MONTHLY,
                onClick = { onTypeSelected(RecurrenceType.MONTHLY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RecurrenceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}
