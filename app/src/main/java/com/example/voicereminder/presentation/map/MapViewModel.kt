package com.example.voicereminder.presentation.map

import android.app.Application
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereminder.data.LocationData
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderEntity
import com.example.voicereminder.domain.LocationServiceManager
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Locale

data class ReminderWithLocation(
    val reminder: ReminderEntity,
    val locationData: LocationData
)

data class SelectedLocationInfo(
    val geoPoint: GeoPoint,
    val address: String?,
    val placeName: String?
)

data class MapUiState(
    val currentLocation: GeoPoint? = null,
    val locationReminders: List<ReminderWithLocation> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isTrackingLocation: Boolean = false,
    val selectedLocation: GeoPoint? = null, // For tap-to-create
    val selectedLocationInfo: SelectedLocationInfo? = null, // Detailed info about selected location
    val showCreateReminderDialog: Boolean = false,
    val isResolvingAddress: Boolean = false, // Loading state for reverse geocoding
    val filterCategory: String? = null // Filter reminders by category
)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    
    // Lazy initialization - only create when actually needed
    private val database by lazy { ReminderDatabase.getDatabase(application) }
    private val locationServiceManager by lazy { LocationServiceManager(application) }
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    
    // Track if reminders have been loaded to avoid repeated loading
    private var remindersLoaded = false
    // Job for location reminders collection - can be cancelled
    private var remindersJob: kotlinx.coroutines.Job? = null
    
    init {
        // REMOVED: loadLocationReminders() call on init
        // Now loaded lazily when screen is actually displayed
        // This prevents unnecessary database operations on app startup
    }
    
    /**
     * Load reminders when screen becomes visible
     * Call this from the composable's LaunchedEffect
     */
    fun onScreenVisible() {
        if (!remindersLoaded) {
            loadLocationReminders()
        }
    }
    
    /**
     * Start tracking user's current location
     * Now with proper error handling and single request (not continuous)
     */
    fun startLocationTracking() {
        // Prevent multiple simultaneous tracking requests
        if (_uiState.value.isTrackingLocation) return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isTrackingLocation = true)
                
                if (!locationServiceManager.hasLocationPermission()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Location permission not granted",
                        isTrackingLocation = false
                    )
                    return@launch
                }
                
                // Get current location ONCE (not continuously)
                // Using maxRetries = 1 to reduce battery drain
                val location = locationServiceManager.getCurrentLocation(maxRetries = 1)
                if (location != null) {
                    _uiState.value = _uiState.value.copy(
                        currentLocation = GeoPoint(location.latitude, location.longitude),
                        errorMessage = null,
                        isTrackingLocation = false // Stop tracking after getting location
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Could not get current location",
                        isTrackingLocation = false
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to get location: ${e.message}",
                    isTrackingLocation = false
                )
            }
        }
    }
    
    /**
     * Stop tracking user's location
     */
    fun stopLocationTracking() {
        _uiState.value = _uiState.value.copy(isTrackingLocation = false)
    }
    
    /**
     * Load all location-based reminders from database
     * Now loads ONCE instead of continuously collecting
     */
    private fun loadLocationReminders() {
        // Cancel any existing job
        remindersJob?.cancel()
        
        remindersJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Use first() to get data once instead of continuous collection
                // This prevents the Flow from running indefinitely and draining battery
                val reminders = database.reminderDao().getAllRemindersFlow().first()
                
                val locationReminders = reminders.mapNotNull { reminder ->
                    reminder.locationData?.let { locationDataJson ->
                        try {
                            val locationData = json.decodeFromString<LocationData>(locationDataJson)
                            ReminderWithLocation(reminder, locationData)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    locationReminders = locationReminders,
                    isLoading = false,
                    errorMessage = null
                )
                
                remindersLoaded = true
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load reminders: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Refresh reminders - call when user explicitly requests refresh
     */
    fun refreshReminders() {
        remindersLoaded = false
        loadLocationReminders()
    }
    
    /**
     * Refresh current location
     */
    fun refreshLocation() {
        if (_uiState.value.isTrackingLocation) {
            startLocationTracking()
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Handle map tap to select location for creating reminder
     * Performs reverse geocoding to get address from coordinates
     */
    fun onMapTapped(latitude: Double, longitude: Double) {
        val geoPoint = GeoPoint(latitude, longitude)
        
        _uiState.value = _uiState.value.copy(
            selectedLocation = geoPoint,
            isResolvingAddress = true,
            showCreateReminderDialog = true
        )
        
        // Reverse geocode to get address
        viewModelScope.launch {
            val locationInfo = reverseGeocode(latitude, longitude)
            _uiState.value = _uiState.value.copy(
                selectedLocationInfo = locationInfo,
                isResolvingAddress = false
            )
        }
    }
    
    /**
     * Reverse geocode coordinates to get address
     */
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): SelectedLocationInfo {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                var address: String? = null
                var placeName: String? = null
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Use the new async API for Android 13+
                    val addresses = mutableListOf<android.location.Address>()
                    geocoder.getFromLocation(latitude, longitude, 1) { results ->
                        addresses.addAll(results)
                    }
                    // Wait a bit for the callback
                    kotlinx.coroutines.delay(500)
                    if (addresses.isNotEmpty()) {
                        val addr = addresses[0]
                        placeName = addr.featureName ?: addr.subLocality ?: addr.locality
                        address = addr.getAddressLine(0)
                    }
                } else {
                    // Use deprecated API for older versions
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        placeName = addr.featureName ?: addr.subLocality ?: addr.locality
                        address = addr.getAddressLine(0)
                    }
                }
                
                SelectedLocationInfo(
                    geoPoint = GeoPoint(latitude, longitude),
                    address = address,
                    placeName = placeName
                )
            } catch (e: Exception) {
                // Return location without address if geocoding fails
                SelectedLocationInfo(
                    geoPoint = GeoPoint(latitude, longitude),
                    address = String.format(Locale.US, "%.5f, %.5f", latitude, longitude),
                    placeName = null
                )
            }
        }
    }
    
    /**
     * Clear selected location
     */
    fun clearSelectedLocation() {
        _uiState.value = _uiState.value.copy(
            selectedLocation = null,
            selectedLocationInfo = null,
            showCreateReminderDialog = false
        )
    }
    
    /**
     * Show create reminder dialog
     */
    fun showCreateReminderDialog() {
        _uiState.value = _uiState.value.copy(showCreateReminderDialog = true)
    }
    
    /**
     * Hide create reminder dialog
     */
    fun hideCreateReminderDialog() {
        _uiState.value = _uiState.value.copy(showCreateReminderDialog = false)
    }
    
    /**
     * Filter reminders by category
     */
    fun setFilterCategory(category: String?) {
        _uiState.value = _uiState.value.copy(filterCategory = category)
    }
    
    /**
     * Get filtered reminders based on current filter
     */
    fun getFilteredReminders(): List<ReminderWithLocation> {
        val filter = _uiState.value.filterCategory
        return if (filter == null) {
            _uiState.value.locationReminders
        } else {
            _uiState.value.locationReminders.filter { 
                it.locationData.placeCategory?.name == filter ||
                it.locationData.placeName?.contains(filter, ignoreCase = true) == true
            }
        }
    }
}
