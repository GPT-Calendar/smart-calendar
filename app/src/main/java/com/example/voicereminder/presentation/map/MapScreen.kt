package com.example.voicereminder.presentation.map

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereminder.data.GeofenceTriggerType
import com.example.voicereminder.presentation.ui.CreateLocationReminderDialog

import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onLocationPermissionNeeded: () -> Unit = {},
    onCreateReminder: ((Double, Double, String, Int, Boolean, Boolean) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    val filterCategories = listOf("All", "Home", "Work", "Store", "Grocery", "Pharmacy", "Restaurant")

    // Initialize OSMDroid configuration and load data when screen becomes visible
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        // Load reminders when screen is visible (lazy loading)
        viewModel.onScreenVisible()
        // REMOVED: automatic startLocationTracking() - now user must tap the button
        // This prevents unnecessary location requests and battery drain
    }

    // Stop location tracking when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLocationTracking()
        }
    }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    
    // Show create reminder dialog when location is tapped
    if (uiState.showCreateReminderDialog && uiState.selectedLocation != null) {
        CreateLocationReminderDialog(
            onDismiss = { viewModel.clearSelectedLocation() },
            onCreateReminder = { message, locationName, radius, triggerOnExit, isRecurring, startHour, endHour ->
                onCreateReminder?.invoke(
                    uiState.selectedLocation!!.latitude,
                    uiState.selectedLocation!!.longitude,
                    message,
                    radius,
                    triggerOnExit,
                    isRecurring
                )
                viewModel.clearSelectedLocation()
            },
            savedLocations = listOf("Home", "Work", "Gym"),
            // Pass the resolved address info from map tap
            preSelectedAddress = uiState.selectedLocationInfo?.address,
            preSelectedPlaceName = uiState.selectedLocationInfo?.placeName,
            isLoadingAddress = uiState.isResolvingAddress
        )
    }

    // Map screen content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Map content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // OSMDroid Map - Light themed (white/grey streets) - 60% allocation
            AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK) // Light themed map
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)

                        // Set default position (San Francisco)
                        val defaultPosition = GeoPoint(37.7749, -122.4194)
                        controller.setCenter(defaultPosition)

                        // Add my location overlay
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        overlays.add(locationOverlay)
                        
                        // Add map events overlay for tap-to-create
                        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                // Single tap also creates reminder for better UX
                                p?.let {
                                    viewModel.onMapTapped(it.latitude, it.longitude)
                                }
                                return true
                            }
                            
                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                p?.let {
                                    viewModel.onMapTapped(it.latitude, it.longitude)
                                }
                                return true
                            }
                        })
                        overlays.add(0, mapEventsOverlay) // Add at bottom so it receives events

                        mapView = this
                    }
            },
            update = { map ->
                    // Update map center when current location changes
                    uiState.currentLocation?.let { location ->
                        map.controller.setCenter(location)
                    }

                    // Clear existing markers and circles (except location overlay)
                    map.overlays.removeAll { it is Marker || it is Polygon }

                    // Get filtered reminders
                    val remindersToShow = if (selectedFilter == null) {
                        uiState.locationReminders
                    } else {
                        uiState.locationReminders.filter { 
                            it.locationData.placeCategory?.name?.equals(selectedFilter, ignoreCase = true) == true ||
                            it.locationData.placeName?.contains(selectedFilter ?: "", ignoreCase = true) == true
                        }
                    }
                    
                    // Add markers and circles for location-based reminders
                    remindersToShow.forEach { reminderWithLocation ->
                        val locationData = reminderWithLocation.locationData
                        val reminder = reminderWithLocation.reminder

                        locationData.latitude?.let { lat ->
                            locationData.longitude?.let { lng ->
                                val position = GeoPoint(lat, lng)
                                
                                // Color based on trigger type
                                val isExitTrigger = locationData.triggerType == GeofenceTriggerType.EXIT
                                val isBothTrigger = locationData.triggerType == GeofenceTriggerType.BOTH
                                val isRecurring = locationData.recurrence != com.example.voicereminder.data.LocationRecurrence.ONCE
                                
                                // Colors: Blue for enter, Orange for exit, Purple for both, Green outline for recurring
                                val fillColor = when {
                                    isBothTrigger -> android.graphics.Color.argb(50, 156, 39, 176) // Purple
                                    isExitTrigger -> android.graphics.Color.argb(50, 255, 152, 0) // Orange
                                    else -> android.graphics.Color.argb(50, 93, 131, 255) // Blue
                                }
                                val strokeColor = when {
                                    isBothTrigger -> android.graphics.Color.rgb(156, 39, 176) // Purple
                                    isExitTrigger -> android.graphics.Color.rgb(255, 152, 0) // Orange
                                    else -> android.graphics.Color.rgb(93, 131, 255) // Blue
                                }

                                // Add circle for geofence radius FIRST (so it appears behind marker)
                                val circle = Polygon(map).apply {
                                    points = Polygon.pointsAsCircle(position, locationData.radius.toDouble())
                                    fillPaint.color = fillColor
                                    fillPaint.style = android.graphics.Paint.Style.FILL
                                    outlinePaint.color = strokeColor
                                    outlinePaint.strokeWidth = if (isRecurring) 6f else 4f
                                    outlinePaint.style = android.graphics.Paint.Style.STROKE
                                    // Dashed line for recurring reminders
                                    if (isRecurring) {
                                        outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 5f), 0f)
                                    }
                                }
                                map.overlays.add(circle)

                                // Add marker
                                val marker = Marker(map).apply {
                                    this.position = position
                                    title = reminder.message
                                    val triggerText = when {
                                        isBothTrigger -> "Enter & Exit"
                                        isExitTrigger -> "When leaving"
                                        else -> "When arriving"
                                    }
                                    val recurText = if (isRecurring) " (Recurring)" else ""
                                    snippet = "${locationData.placeName ?: "Location"} • $triggerText$recurText"
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                                    // Create custom marker drawable
                                    val drawable = android.graphics.drawable.ShapeDrawable(
                                        android.graphics.drawable.shapes.OvalShape()
                                    )
                                    drawable.paint.color = strokeColor
                                    drawable.intrinsicWidth = 40
                                    drawable.intrinsicHeight = 40
                                    drawable.setBounds(0, 0, 40, 40)

                                    icon = drawable
                                }
                                map.overlays.add(marker)
                            }
                        }
                    }
                    
                    // Show selected location marker for tap-to-create
                    uiState.selectedLocation?.let { selectedPos ->
                        val selectedMarker = Marker(map).apply {
                            position = selectedPos
                            title = "Create reminder here"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            
                            val drawable = android.graphics.drawable.ShapeDrawable(
                                android.graphics.drawable.shapes.OvalShape()
                            )
                            drawable.paint.color = android.graphics.Color.rgb(76, 175, 80) // Green
                            drawable.intrinsicWidth = 50
                            drawable.intrinsicHeight = 50
                            drawable.setBounds(0, 0, 50, 50)
                            icon = drawable
                        }
                        map.overlays.add(selectedMarker)
                    }

                    map.invalidate()
            }
        )

        // Floating Search Bar - theme-aware colors
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .height(48.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (searchQuery.isEmpty()) "Search location..." else searchQuery,
                    color = if (searchQuery.isEmpty()) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimary,
                    fontSize = 15.sp
                )
            }
        }

        // Filter chips row
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 72.dp, start = 16.dp, end = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterCategories.forEach { category ->
                FilterChip(
                    selected = (category == "All" && selectedFilter == null) || selectedFilter == category,
                    onClick = {
                        selectedFilter = if (category == "All") null else category
                        viewModel.setFilterCategory(selectedFilter)
                    },
                    label = { Text(category, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        selectedContainerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
        
        // Show info card with location count - theme-aware colors
        if (!uiState.isLoading && uiState.locationReminders.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 120.dp, end = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${uiState.locationReminders.size} reminder${if (uiState.locationReminders.size != 1) "s" else ""}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // Tap to create hint
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 80.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Tap on map to create reminder",
                modifier = Modifier.padding(8.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Show error message if any
        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp
                )
            }
        }

        // Show loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Floating Navigate Button - theme-aware colors
        FloatingActionButton(
            onClick = {
                uiState.currentLocation?.let { location ->
                    mapView?.controller?.animateTo(location)
                    mapView?.controller?.setZoom(15.0)
                } ?: run {
                    viewModel.startLocationTracking()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Navigate to my location",
                modifier = Modifier.size(24.dp)
            )
        }
        }
    }

    // Cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
}
