package com.example.voicereminder.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereminder.R
import com.example.voicereminder.domain.models.Reminder
import com.example.voicereminder.domain.models.Task
import com.example.voicereminder.domain.models.Alarm
import com.example.voicereminder.data.entity.FinanceTransaction
import com.example.voicereminder.data.entity.TransactionType
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.domain.TaskManager
import com.example.voicereminder.domain.EnhancedAlarmManager
import com.example.voicereminder.data.FinanceDatabase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Calendar

/**
 * Unified Calendar Screen - Shows events AND finance transactions for selected days
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    reminders: List<Reminder> = emptyList(),
    onReminderClick: (Reminder) -> Unit = {},
    onReminderDelete: (Reminder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Managers
    val reminderManager = remember { ReminderManager.getInstance(context) }
    val taskManager = remember { TaskManager.getInstance(context) }
    val alarmManager = remember { EnhancedAlarmManager.getInstance(context) }
    val financeDb = remember { FinanceDatabase.getDatabase(context) }

    // State
    val currentMonth = remember { YearMonth.now() }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    
    // Data flows
    val allReminders by reminderManager.getAllRemindersFlow().collectAsState(initial = emptyList())
    val allTasks by taskManager.getAllTasksFlow().collectAsState(initial = emptyList())
    val allAlarms by alarmManager.getAllAlarmsFlow().collectAsState(initial = emptyList())
    var transactions by remember { mutableStateOf<List<FinanceTransaction>>(emptyList()) }
    
    // Load transactions
    LaunchedEffect(Unit) {
        transactions = financeDb.financeTransactionDao().getAllTransactions()
    }
    
    // Calculate event counts for each day (reminders + tasks + transactions)
    val eventCounts = remember(allReminders, allTasks, transactions) {
        val counts = mutableMapOf<LocalDate, Int>()
        
        // Count reminders
        allReminders.forEach { reminder ->
            reminder.scheduledTime?.toLocalDate()?.let { date ->
                counts[date] = counts.getOrDefault(date, 0) + 1
            }
        }
        
        // Count tasks
        allTasks.forEach { task ->
            task.dueDate?.toLocalDate()?.let { date ->
                counts[date] = counts.getOrDefault(date, 0) + 1
            }
        }
        
        // Count transactions
        transactions.forEach { transaction ->
            val date = transaction.timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            counts[date] = counts.getOrDefault(date, 0) + 1
        }
        
        counts
    }
    
    // Filter items for selected date
    val selectedDateReminders = remember(allReminders, selectedDate) {
        selectedDate?.let { date ->
            allReminders.filter { it.scheduledTime?.toLocalDate() == date }
        } ?: emptyList()
    }
    
    val selectedDateTasks = remember(allTasks, selectedDate) {
        selectedDate?.let { date ->
            allTasks.filter { it.dueDate?.toLocalDate() == date }
        } ?: emptyList()
    }
    
    val selectedDateTransactions = remember(transactions, selectedDate) {
        selectedDate?.let { date ->
            transactions.filter { transaction ->
                val txDate = transaction.timestamp.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                txDate == date
            }
        } ?: emptyList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Calendar grid
            val daysInMonth = selectedMonth.lengthOfMonth()
            val firstDayOfMonth = selectedMonth.atDay(1)
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

            val daysList = remember(selectedMonth, eventCounts) {
                val days = mutableListOf<CalendarDay?>()
                for (i in 0 until firstDayOfWeek) {
                    days.add(null)
                }
                for (day in 1..daysInMonth) {
                    val date = selectedMonth.atDay(day)
                    days.add(CalendarDay(date, eventCounts[date] ?: 0))
                }
                days
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Calendar Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Month navigation
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                                Icon(Icons.Default.NavigateBefore, "Previous", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                                Icon(Icons.Default.NavigateNext, "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Day headers
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            items(daysList) { day ->
                                if (day != null) {
                                    CalendarDayItem(
                                        day = day,
                                        isSelected = day.date == selectedDate,
                                        onClick = { selectedDate = day.date }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }
                }

                // Selected date content
                if (selectedDate != null) {
                    Text(
                        text = selectedDate!!.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    val hasContent = selectedDateReminders.isNotEmpty() || 
                                    selectedDateTasks.isNotEmpty() || 
                                    selectedDateTransactions.isNotEmpty()

                    if (!hasContent) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No events or transactions",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Reminders Section
                            if (selectedDateReminders.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Reminders",
                                        icon = Icons.Default.Notifications,
                                        count = selectedDateReminders.size,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(selectedDateReminders) { reminder ->
                                    ReminderEventCard(
                                        reminder = reminder,
                                        onClick = { onReminderClick(reminder) },
                                        onDelete = { 
                                            coroutineScope.launch { 
                                                reminderManager.deleteReminder(reminder.id) 
                                            }
                                        }
                                    )
                                }
                            }

                            // Tasks Section
                            if (selectedDateTasks.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Tasks",
                                        icon = Icons.Default.CheckCircle,
                                        count = selectedDateTasks.size,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                items(selectedDateTasks) { task ->
                                    TaskEventCard(
                                        task = task,
                                        onComplete = {
                                            coroutineScope.launch { taskManager.completeTask(task.id) }
                                        }
                                    )
                                }
                            }

                            // Finance Transactions Section
                            if (selectedDateTransactions.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Transactions",
                                        icon = Icons.Default.AccountBalance,
                                        count = selectedDateTransactions.size,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                                items(selectedDateTransactions) { transaction ->
                                    TransactionEventCard(transaction = transaction)
                                }
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { /* Navigate to add event */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, "Add Event", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.weight(1f))
        Surface(shape = CircleShape, color = color) {
            Text(
                count.toString(),
                fontSize = 11.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}


@Composable
private fun ReminderEventCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminder.message,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    reminder.scheduledTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "All Day",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TaskEventCard(
    task: Task,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onComplete() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) 
                        MaterialTheme.colorScheme.onSurfaceVariant 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                if (!task.description.isNullOrEmpty()) {
                    Text(
                        task.description!!,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Priority indicator
            val priorityColor = com.example.voicereminder.data.Priority.getColor(task.priority)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = priorityColor.copy(alpha = 0.2f)
            ) {
                Text(
                    task.priority.name,
                    fontSize = 10.sp,
                    color = priorityColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionEventCard(transaction: FinanceTransaction) {
    val isIncome = transaction.transactionType == TransactionType.CREDIT
    val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFE53935)
    val amountPrefix = if (isIncome) "+" else "-"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isIncome) Color(0xFF4CAF50).copy(alpha = 0.1f) 
                        else Color(0xFFE53935).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    null,
                    tint = amountColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    transaction.category ?: transaction.bankName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "$amountPrefix${transaction.currency} ${String.format("%.2f", transaction.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}


data class CalendarDay(
    val date: LocalDate,
    val eventCount: Int
)

@Composable
fun CalendarDayItem(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val isToday = day.date == LocalDate.now()

    Box(
        modifier = modifier
            .size(40.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) primaryColor.copy(alpha = 0.1f) else Color.Transparent,
                contentColor = when {
                    isSelected -> primaryColor
                    isToday -> primaryColor
                    else -> onSurfaceColor
                }
            ),
            border = when {
                isSelected -> androidx.compose.foundation.BorderStroke(2.dp, primaryColor)
                isToday -> androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
                else -> null
            }
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }

        // Event indicator
        if (day.eventCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-1).dp)
                    .size(if (day.eventCount > 1) 12.dp else 5.dp)
                    .background(primaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (day.eventCount > 1) {
                    Text(
                        text = if (day.eventCount > 9) "9+" else day.eventCount.toString(),
                        fontSize = 7.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarEventCard(
    reminder: Reminder,
    onReminderClick: (Reminder) -> Unit,
    onReminderDelete: (Reminder) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReminderClick(reminder) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reminder.scheduledTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "All Day",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (reminder.locationData != null) {
                    Text(
                        text = reminder.locationData!!,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
