package com.example.voicereminder.presentation.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voicereminder.R
import com.example.voicereminder.domain.models.Reminder

import com.example.voicereminder.presentation.ui.DateHeaderItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllEventsScreen(
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    onReminderDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE - MMM d") }
    val groupedReminders = remember(reminders) {
        reminders
            .filter { it.scheduledTime != null }
            .sortedBy { it.scheduledTime }
            .groupBy { it.scheduledTime!!.toLocalDate() }
            .toSortedMap()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedReminders.forEach { (date, dateReminders) ->
                item {
                    DateHeaderItem(
                        text = formatDisplayDate(date, dateFormatter),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(dateReminders) { reminder ->
                    TimelineReminderCardItem(
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
                            primaryColor
                        } else {
                            onSurfaceVariantColor
                        },
                        onCardClick = { onReminderClick(reminder) },
                        modifier = Modifier.fillMaxWidth(),
                        isLastItem = dateReminders.indexOf(reminder) == dateReminders.size - 1
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineReminderCardItem(
    timeText: String,
    message: String,
    locationDetails: String? = null,
    isLocationBased: Boolean = false,
    statusIconRes: Int,
    statusIconTint: Color = MaterialTheme.colorScheme.primary,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isLastItem: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = { onCardClick?.invoke() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Timeline connector with dot
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
            ) {
                // Vertical timeline line - theme-aware
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(24.dp)
                        .align(Alignment.TopCenter)
                ) {
                    val strokeWidth = 2f
                    val centerX = size.width / 2
                    val startY = 20f
                    val endY = if (isLastItem) size.height / 2 else size.height

                    drawLine(
                        color = primaryColor,
                        start = androidx.compose.ui.geometry.Offset(centerX, startY),
                        end = androidx.compose.ui.geometry.Offset(centerX, endY),
                        strokeWidth = strokeWidth
                    )
                }

                // Status Icon as timeline dot
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(id = statusIconRes),
                    contentDescription = "Reminder Status",
                    tint = statusIconTint,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopCenter)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Reminder Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Time/Location Row - theme-aware colors
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLocationBased) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Location reminder indicator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Location Details (visible for location-based reminders)
                if (!locationDetails.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = locationDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575) // Medium grey text
                    )
                }
            }
        }
    }
}

private fun formatDisplayDate(date: LocalDate, formatter: DateTimeFormatter): String {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    return when (date) {
        today -> "Today - ${date.format(formatter)}"
        tomorrow -> "Tomorrow - ${date.format(formatter)}"
        else -> date.format(formatter)
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