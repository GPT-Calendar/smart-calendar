package com.example.voicereminder.presentation.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.voicereminder.R
import com.example.voicereminder.domain.models.Reminder
import com.example.voicereminder.presentation.ui.ReminderCardItem
import java.time.format.DateTimeFormatter

@Composable
fun EventListScreen(
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    onReminderDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(reminders) { reminder ->
            ReminderCardItem(
                timeText = if (reminder.reminderType.name == "LOCATION_BASED") {
                    "Location-based"
                } else {
                    reminder.scheduledTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "Unknown"
                },
                message = reminder.message,
                locationDetails = formatLocationDetails(reminder),
                isLocationBased = reminder.reminderType.name == "LOCATION_BASED",
                statusIconRes = if (reminder.status.name == "PENDING") {
                    R.drawable.ic_reminder_pending
                } else {
                    R.drawable.ic_reminder_completed
                },
                statusIconTint = if (reminder.status.name == "PENDING") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onCardClick = { onReminderClick(reminder) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatLocationDetails(reminder: Reminder): String? {
    if (reminder.reminderType.name != "LOCATION_BASED" || reminder.locationData.isNullOrBlank()) {
        return null
    }
    
    return try {
        // TODO: Parse location data properly if needed
        "Location-based reminder"
    } catch (e: Exception) {
        "Location-based"
    }
}