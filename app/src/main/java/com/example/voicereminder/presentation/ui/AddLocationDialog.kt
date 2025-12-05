package com.example.voicereminder.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Data class for enhanced location settings
 */
data class EnhancedLocationSettings(
    val name: String,
    val address: String,
    val radius: Int,
    val triggerOnExit: Boolean = false,
    val isRecurring: Boolean = false,
    val useCurrentLocation: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLocationDialog(
    onDismiss: () -> Unit,
    onAddLocation: (String, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var locationName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }
    var useCurrentLocation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // Location Name Input
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Location Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Use Current Location Button
                Button(
                    onClick = { useCurrentLocation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Current Location")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Address Input
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Radius Input
                OutlinedTextField(
                    value = radius,
                    onValueChange = { 
                        // Only allow numeric input
                        if (it.all { char -> char.isDigit() }) {
                            radius = it
                        }
                    },
                    label = { Text("Radius (meters)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val radiusValue = radius.toIntOrNull() ?: 100
                            onAddLocation(locationName, address, radiusValue)
                        },
                        enabled = locationName.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

/**
 * Enhanced dialog for creating location reminders with all new features
 * Now supports pre-selected location from map tap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLocationReminderDialog(
    onDismiss: () -> Unit,
    onCreateReminder: (
        message: String,
        locationName: String,
        radius: Int,
        triggerOnExit: Boolean,
        isRecurring: Boolean,
        timeConstraintStart: Int?,
        timeConstraintEnd: Int?
    ) -> Unit,
    savedLocations: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    // New parameters for map-selected location
    preSelectedAddress: String? = null,
    preSelectedPlaceName: String? = null,
    isLoadingAddress: Boolean = false
) {
    var reminderMessage by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    // Initialize customLocation with pre-selected address if available
    var customLocation by remember(preSelectedAddress) { 
        mutableStateOf(preSelectedPlaceName ?: preSelectedAddress ?: "") 
    }
    var radius by remember { mutableStateOf("100") }
    var triggerOnExit by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }
    var hasTimeConstraint by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf("9") }
    var endHour by remember { mutableStateOf("18") }
    var expandedLocationDropdown by remember { mutableStateOf(false) }
    // Track if using map-selected location
    val hasMapLocation = preSelectedAddress != null
    
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Create Location Reminder",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show selected map location info
                if (hasMapLocation) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (isLoadingAddress) {
                                    Text(
                                        text = "Getting address...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    if (preSelectedPlaceName != null) {
                                        Text(
                                            text = preSelectedPlaceName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = preSelectedAddress ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Reminder Message
                OutlinedTextField(
                    value = reminderMessage,
                    onValueChange = { reminderMessage = it },
                    label = { Text("Reminder Message") },
                    placeholder = { Text("e.g., Buy groceries") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                
                // Location Selection - only show if no map location selected
                if (!hasMapLocation && savedLocations.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedLocationDropdown,
                        onExpandedChange = { expandedLocationDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLocation.ifEmpty { "Select a saved location" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Location") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLocationDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLocationDropdown,
                            onDismissRequest = { expandedLocationDropdown = false }
                        ) {
                            savedLocations.forEach { location ->
                                DropdownMenuItem(
                                    text = { Text(location) },
                                    onClick = {
                                        selectedLocation = location
                                        expandedLocationDropdown = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Custom location") },
                                onClick = {
                                    selectedLocation = ""
                                    expandedLocationDropdown = false
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Custom Location Input (if no saved location selected and no map location)
                if (!hasMapLocation && selectedLocation.isEmpty()) {
                    OutlinedTextField(
                        value = customLocation,
                        onValueChange = { customLocation = it },
                        label = { Text("Location Name or Address") },
                        placeholder = { Text("e.g., Home, Work, or an address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Location name field for map-selected location
                if (hasMapLocation) {
                    OutlinedTextField(
                        value = customLocation,
                        onValueChange = { customLocation = it },
                        label = { Text("Location Name (optional)") },
                        placeholder = { Text("e.g., Coffee Shop, Gym") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Radius Slider
                Text(
                    text = "Trigger Radius: ${radius}m",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = radius.toFloatOrNull() ?: 100f,
                    onValueChange = { radius = it.toInt().toString() },
                    valueRange = 50f..500f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Trigger Type
                Text(
                    text = "Trigger When:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !triggerOnExit,
                        onClick = { triggerOnExit = false },
                        label = { Text("I arrive") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = triggerOnExit,
                        onClick = { triggerOnExit = true },
                        label = { Text("I leave") }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Recurring Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text("Repeat every time")
                }
                
                // Time Constraint Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasTimeConstraint,
                        onCheckedChange = { hasTimeConstraint = it }
                    )
                    Text("Only during specific hours")
                }
                
                if (hasTimeConstraint) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startHour,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) startHour = it },
                            label = { Text("From") },
                            suffix = { Text(":00") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endHour,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) endHour = it },
                            label = { Text("To") },
                            suffix = { Text(":00") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val locationToUse = selectedLocation.ifEmpty { customLocation }
                            val radiusValue = radius.toIntOrNull() ?: 100
                            val startHourValue = if (hasTimeConstraint) startHour.toIntOrNull() else null
                            val endHourValue = if (hasTimeConstraint) endHour.toIntOrNull() else null
                            
                            onCreateReminder(
                                reminderMessage,
                                locationToUse,
                                radiusValue,
                                triggerOnExit,
                                isRecurring,
                                startHourValue,
                                endHourValue
                            )
                        },
                        enabled = reminderMessage.isNotBlank() && 
                                  (selectedLocation.isNotBlank() || customLocation.isNotBlank())
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}