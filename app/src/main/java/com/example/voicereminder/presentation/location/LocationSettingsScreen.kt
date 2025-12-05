package com.example.voicereminder.presentation.location

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.voicereminder.data.SavedLocationEntity
import com.example.voicereminder.domain.LocationReminderManager
import com.example.voicereminder.domain.LocationServiceManager
import com.example.voicereminder.presentation.ui.AddLocationDialog
import com.example.voicereminder.presentation.ui.SavedLocationItem
import com.example.voicereminder.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSettingsScreen(
    locationReminderManager: LocationReminderManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var permissionGranted by remember {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        mutableStateOf(isGranted)
    }
    var hasBackgroundPermission by remember {
        val hasBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required on older versions
        }
        mutableStateOf(hasBackground)
    }

    var locationsList by remember { mutableStateOf<List<SavedLocationEntity>>(emptyList()) }
    var locationServiceManager = remember { LocationServiceManager(context) }

    LaunchedEffect(Unit) {
        // Load initial locations
        val initialLocations = locationReminderManager.getSavedLocations()
        locationsList = initialLocations

        // If LocationReminderManager has a flow-based method for updates, use that
        // For now, we'll just use the initial list
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_desc)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (permissionGranted) {
                FloatingActionButton(
                    onClick = { showAddLocationDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_location)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Permission Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.location_permission_status),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val permissionText = when {
                        permissionGranted && hasBackgroundPermission -> {
                            stringResource(R.string.location_permission_granted)
                        }
                        permissionGranted -> {
                            stringResource(R.string.location_permission_foreground_only)
                        }
                        else -> {
                            stringResource(R.string.location_permission_denied)
                        }
                    }

                    val permissionColor = when {
                        permissionGranted && hasBackgroundPermission -> {
                            MaterialTheme.colorScheme.primary
                        }
                        permissionGranted -> {
                            MaterialTheme.colorScheme.secondary
                        }
                        else -> {
                            MaterialTheme.colorScheme.error
                        }
                    }

                    Text(
                        text = permissionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = permissionColor
                    )

                    if (!permissionGranted || (!hasBackgroundPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val buttonText = if (!permissionGranted) {
                            stringResource(R.string.grant_permission)
                        } else {
                            stringResource(R.string.grant_background_permission)
                        }
                        Button(
                            onClick = {
                                // This would need to use an ActivityResultLauncher from the Activity
                                // For now, we'll just note that this needs to be handled differently
                            },
                            colors = ButtonDefaults.textButtonColors()
                        ) {
                            Text(buttonText)
                        }
                    }
                }
            }

            // Saved Locations Section
            Text(
                text = stringResource(R.string.saved_locations_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.saved_locations_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (locationsList.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.no_saved_locations),
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.no_saved_locations),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Saved Locations List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(locationsList) { location ->
                        SavedLocationItem(
                            locationName = location.name,
                            locationCoordinates = "${location.latitude}° N, ${location.longitude}° W",
                            locationRadius = "Radius: ${location.radius}m",
                            onDeleteClick = {
                                scope.launch {
                                    locationReminderManager.deleteSavedLocation(location.id)
                                }
                            }
                        )
                    }
                }
            }

            // Add Location Dialog
            if (showAddLocationDialog) {
                AddLocationDialog(
                    onDismiss = { showAddLocationDialog = false },
                    onAddLocation = { name, address, radius ->
                        scope.launch {
                            try {
                                // Try to parse as coordinates first
                                val coords = address.split(",")
                                if (coords.size == 2) {
                                    val lat = coords[0].trim().toDoubleOrNull()
                                    val lon = coords[1].trim().toDoubleOrNull()
                                    if (lat != null && lon != null) {
                                        locationReminderManager.addSavedLocation(name, lat, lon, radius.toFloat())
                                    }
                                } else if (address.isNotEmpty()) {
                                    // Otherwise, geocode the address
                                    val placeResolver = com.example.voicereminder.domain.PlaceResolver(context, com.example.voicereminder.data.ReminderDatabase.getDatabase(context))
                                    val placeResult = placeResolver.resolvePlace(address)
                                    if (placeResult != null) {
                                        locationReminderManager.addSavedLocation(name, placeResult.latitude, placeResult.longitude, radius.toFloat())
                                    }
                                } else {
                                    // Use current location if no address provided
                                    if (locationServiceManager.hasLocationPermission()) {
                                        val currentLocation = locationServiceManager.getCurrentLocation()
                                        if (currentLocation != null) {
                                            locationReminderManager.addSavedLocation(name, currentLocation.latitude, currentLocation.longitude, radius.toFloat())
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Handle error
                            }
                        }
                        showAddLocationDialog = false
                    }
                )
            }
        }
    }
}