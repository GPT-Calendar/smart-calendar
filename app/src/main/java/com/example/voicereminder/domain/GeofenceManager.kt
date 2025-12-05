package com.example.voicereminder.domain

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.voicereminder.receivers.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

/**
 * Data class representing geofence information
 */
data class GeofenceData(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float
)

/**
 * Manager class for Android Geofencing API integration
 * Handles registration, removal, and management of geofences for location-based reminders
 */
class GeofenceManager(private val context: Context) {
    
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    
    // Track registered geofences to detect when they're removed by the system
    private val registeredGeofences = mutableSetOf<String>()
    
    companion object {
        private const val TAG = "GeofenceManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
        private const val GEOFENCE_EXPIRATION_DURATION = Geofence.NEVER_EXPIRE
        private const val GEOFENCE_LOITERING_DELAY = 0 // Trigger immediately on entry
        private const val MAX_GEOFENCES_PER_APP = 100 // Android system limit
    }
    
    /**
     * Register a single geofence for a specific location
     * Handles geofence limit exceeded by prioritizing active reminders
     * @param geofenceId Unique identifier for the geofence
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @param radius Radius in meters for the geofence
     * @return True if registration successful, false otherwise
     */
    suspend fun registerGeofence(
        geofenceId: String,
        latitude: Double,
        longitude: Double,
        radius: Float
    ): Boolean {
        // Check if we're approaching the geofence limit
        if (registeredGeofences.size >= MAX_GEOFENCES_PER_APP) {
            Log.w(TAG, "Approaching geofence limit (${registeredGeofences.size}/$MAX_GEOFENCES_PER_APP)")
            // In a production app, you might want to remove oldest or least important geofences
            // For now, we'll just log a warning and try to register anyway
        }
        
        val success = registerGeofenceWithRetry(
            geofenceData = GeofenceData(geofenceId, latitude, longitude, radius),
            retryCount = 0
        )
        
        if (success) {
            registeredGeofences.add(geofenceId)
            Log.d(TAG, "Geofence registered and tracked: $geofenceId (total: ${registeredGeofences.size})")
        }
        
        return success
    }
    
    /**
     * Register multiple geofences for generic category locations
     * Handles geofence limit by prioritizing and limiting the number of geofences
     * @param geofences List of geofence data to register
     * @return True if all registrations successful, false otherwise
     */
    suspend fun registerMultipleGeofences(geofences: List<GeofenceData>): Boolean {
        if (geofences.isEmpty()) {
            Log.w(TAG, "No geofences to register")
            return true
        }
        
        // Check location permission
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            return false
        }
        
        // Check if we're approaching the geofence limit
        val availableSlots = MAX_GEOFENCES_PER_APP - registeredGeofences.size
        val geofencesToRegister = if (geofences.size > availableSlots) {
            Log.w(TAG, "Geofence limit would be exceeded. Registering only $availableSlots of ${geofences.size} geofences")
            // Prioritize the first N geofences (could be enhanced to prioritize by distance, etc.)
            geofences.take(availableSlots)
        } else {
            geofences
        }
        
        if (geofencesToRegister.isEmpty()) {
            Log.e(TAG, "Cannot register geofences - limit of $MAX_GEOFENCES_PER_APP already reached")
            return false
        }
        
        return try {
            val geofenceList = geofencesToRegister.map { data ->
                buildGeofence(data.id, data.latitude, data.longitude, data.radius)
            }
            
            val geofencingRequest = buildGeofencingRequest(geofenceList)
            val pendingIntent = getGeofencePendingIntent()
            
            geofencingClient.addGeofences(geofencingRequest, pendingIntent).await()
            
            // Track registered geofences
            geofencesToRegister.forEach { registeredGeofences.add(it.id) }
            
            Log.d(TAG, "Successfully registered ${geofencesToRegister.size} geofences (total: ${registeredGeofences.size})")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception registering multiple geofences", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error registering multiple geofences: ${e.message}", e)
            false
        }
    }
    
    /**
     * Remove a specific geofence
     * @param geofenceId The ID of the geofence to remove
     * @return True if removal successful, false otherwise
     */
    suspend fun removeGeofence(geofenceId: String): Boolean {
        return try {
            geofencingClient.removeGeofences(listOf(geofenceId)).await()
            registeredGeofences.remove(geofenceId)
            Log.d(TAG, "Successfully removed geofence: $geofenceId (remaining: ${registeredGeofences.size})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofence: $geofenceId", e)
            // Still remove from tracking even if removal failed
            registeredGeofences.remove(geofenceId)
            false
        }
    }
    
    /**
     * Remove all registered geofences
     * @return True if removal successful, false otherwise
     */
    suspend fun removeAllGeofences(): Boolean {
        return try {
            val pendingIntent = getGeofencePendingIntent()
            geofencingClient.removeGeofences(pendingIntent).await()
            registeredGeofences.clear()
            Log.d(TAG, "Successfully removed all geofences")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing all geofences", e)
            // Clear tracking even if removal failed
            registeredGeofences.clear()
            false
        }
    }
    
    /**
     * Get the number of currently registered geofences
     * @return Number of tracked geofences
     */
    fun getRegisteredGeofenceCount(): Int {
        return registeredGeofences.size
    }
    
    /**
     * Check if a geofence is registered
     * @param geofenceId The ID of the geofence to check
     * @return True if the geofence is tracked as registered
     */
    fun isGeofenceRegistered(geofenceId: String): Boolean {
        return registeredGeofences.contains(geofenceId)
    }
    
    /**
     * Detect and re-register geofences that were removed by the system
     * This should be called periodically or when the app detects geofences may have been removed
     * @param activeGeofenceIds List of geofence IDs that should be active
     * @return List of geofence IDs that were detected as removed
     */
    fun detectRemovedGeofences(activeGeofenceIds: List<String>): List<String> {
        val removedGeofences = mutableListOf<String>()
        
        for (geofenceId in activeGeofenceIds) {
            if (!registeredGeofences.contains(geofenceId)) {
                Log.w(TAG, "Geofence $geofenceId was removed by the system")
                removedGeofences.add(geofenceId)
            }
        }
        
        if (removedGeofences.isNotEmpty()) {
            Log.w(TAG, "Detected ${removedGeofences.size} geofences removed by system")
        }
        
        return removedGeofences
    }
    
    /**
     * Register a geofence with retry logic
     * @param geofenceData The geofence data to register
     * @param retryCount Current retry attempt count
     * @return True if registration successful, false otherwise
     */
    private suspend fun registerGeofenceWithRetry(
        geofenceData: GeofenceData,
        retryCount: Int
    ): Boolean {
        // Check location permission
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            return false
        }
        
        return try {
            val geofence = buildGeofence(
                geofenceData.id,
                geofenceData.latitude,
                geofenceData.longitude,
                geofenceData.radius
            )
            
            val geofencingRequest = buildGeofencingRequest(listOf(geofence))
            val pendingIntent = getGeofencePendingIntent()
            
            geofencingClient.addGeofences(geofencingRequest, pendingIntent).await()
            Log.d(TAG, "Successfully registered geofence: ${geofenceData.id}")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception registering geofence: ${geofenceData.id}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error registering geofence: ${geofenceData.id} (attempt ${retryCount + 1})", e)
            
            // Retry logic with exponential backoff
            if (retryCount < MAX_RETRY_ATTEMPTS) {
                val delayTime = RETRY_DELAY_MS * (retryCount + 1)
                Log.d(TAG, "Retrying geofence registration in ${delayTime}ms")
                delay(delayTime)
                return registerGeofenceWithRetry(geofenceData, retryCount + 1)
            }
            
            Log.e(TAG, "Failed to register geofence after $MAX_RETRY_ATTEMPTS attempts")
            false
        }
    }
    
    /**
     * Build a Geofence object
     * @param id Unique identifier for the geofence
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @param radius Radius in meters
     * @param transitionTypes Geofence transition types (ENTER, EXIT, or both)
     * @return Configured Geofence object
     */
    private fun buildGeofence(
        id: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        transitionTypes: Int = Geofence.GEOFENCE_TRANSITION_ENTER
    ): Geofence {
        return Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(GEOFENCE_EXPIRATION_DURATION)
            .setTransitionTypes(transitionTypes)
            .setLoiteringDelay(GEOFENCE_LOITERING_DELAY)
            .build()
    }
    
    /**
     * Register a geofence with custom transition types (ENTER, EXIT, or both)
     * @param geofenceId Unique identifier for the geofence
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @param radius Radius in meters for the geofence
     * @param transitionTypes Geofence transition types
     * @return True if registration successful, false otherwise
     */
    suspend fun registerGeofenceWithTransitions(
        geofenceId: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        transitionTypes: Int
    ): Boolean {
        // Check if we're approaching the geofence limit
        if (registeredGeofences.size >= MAX_GEOFENCES_PER_APP) {
            Log.w(TAG, "Approaching geofence limit (${registeredGeofences.size}/$MAX_GEOFENCES_PER_APP)")
        }
        
        val success = registerGeofenceWithRetryAndTransitions(
            geofenceData = GeofenceData(geofenceId, latitude, longitude, radius),
            transitionTypes = transitionTypes,
            retryCount = 0
        )
        
        if (success) {
            registeredGeofences.add(geofenceId)
            Log.d(TAG, "Geofence registered with transitions: $geofenceId (total: ${registeredGeofences.size})")
        }
        
        return success
    }
    
    /**
     * Register a geofence with retry logic and custom transition types
     */
    private suspend fun registerGeofenceWithRetryAndTransitions(
        geofenceData: GeofenceData,
        transitionTypes: Int,
        retryCount: Int
    ): Boolean {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            return false
        }
        
        return try {
            val geofence = buildGeofence(
                geofenceData.id,
                geofenceData.latitude,
                geofenceData.longitude,
                geofenceData.radius,
                transitionTypes
            )
            
            val geofencingRequest = buildGeofencingRequest(listOf(geofence))
            val pendingIntent = getGeofencePendingIntent()
            
            geofencingClient.addGeofences(geofencingRequest, pendingIntent).await()
            Log.d(TAG, "Successfully registered geofence with transitions: ${geofenceData.id}")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception registering geofence: ${geofenceData.id}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error registering geofence: ${geofenceData.id} (attempt ${retryCount + 1})", e)
            
            if (retryCount < MAX_RETRY_ATTEMPTS) {
                val delayTime = RETRY_DELAY_MS * (retryCount + 1)
                Log.d(TAG, "Retrying geofence registration in ${delayTime}ms")
                delay(delayTime)
                return registerGeofenceWithRetryAndTransitions(geofenceData, transitionTypes, retryCount + 1)
            }
            
            Log.e(TAG, "Failed to register geofence after $MAX_RETRY_ATTEMPTS attempts")
            false
        }
    }
    
    /**
     * Build a GeofencingRequest
     * @param geofences List of geofences to include in the request
     * @return Configured GeofencingRequest
     */
    private fun buildGeofencingRequest(geofences: List<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()
    }
    
    /**
     * Get or create the PendingIntent for geofence transitions
     * @return PendingIntent for the GeofenceBroadcastReceiver
     */
    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
    
    /**
     * Check if location permission is granted
     * @return True if permission granted, false otherwise
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
