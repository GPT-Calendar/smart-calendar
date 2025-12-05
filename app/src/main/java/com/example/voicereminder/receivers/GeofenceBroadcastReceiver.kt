package com.example.voicereminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.domain.LocationReminderManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles geofence transition events
 * Triggers location-based reminders when user enters or exits a geofence
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "GeofenceBroadcastReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        // Parse the geofencing event
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        
        // Check for errors
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }
        
        if (geofencingEvent.hasError()) {
            val errorCode = geofencingEvent.errorCode
            Log.e(TAG, "Geofence error: $errorCode")
            return
        }
        
        // Get the transition type
        val geofenceTransition = geofencingEvent.geofenceTransition
        
        // Handle both ENTER and EXIT transitions
        val transitionName = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
            else -> "UNKNOWN"
        }
        
        // Only handle ENTER and EXIT transitions
        if (geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER && 
            geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d(TAG, "Ignoring transition type: $transitionName")
            return
        }
        
        // Get the triggering geofences
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        
        if (triggeringGeofences.isNullOrEmpty()) {
            Log.w(TAG, "No triggering geofences found")
            return
        }
        
        Log.d(TAG, "Geofence $transitionName event detected for ${triggeringGeofences.size} geofence(s)")
        
        // Use goAsync to allow background work
        val pendingResult = goAsync()
        
        // Handle geofence transitions in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleGeofenceTransition(context, triggeringGeofences, geofenceTransition)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling geofence transition", e)
            } finally {
                // Signal that the async work is complete
                pendingResult.finish()
            }
        }
    }
    
    /**
     * Handle geofence transition events (ENTER or EXIT)
     * @param context Application context
     * @param geofences List of geofences that were triggered
     * @param transitionType The type of transition (ENTER or EXIT)
     */
    private suspend fun handleGeofenceTransition(
        context: Context, 
        geofences: List<Geofence>,
        transitionType: Int
    ) {
        try {
            // Get database instance
            val database = ReminderDatabase.getDatabase(context)
            
            // Initialize dependencies for LocationReminderManager
            val geofenceManager = com.example.voicereminder.domain.GeofenceManager(context)
            val placeResolver = com.example.voicereminder.domain.PlaceResolver(context, database)
            val locationServiceManager = com.example.voicereminder.domain.LocationServiceManager(context)
            
            // Initialize LocationReminderManager
            val locationReminderManager = LocationReminderManager(
                database = database,
                geofenceManager = geofenceManager,
                placeResolver = placeResolver,
                locationServiceManager = locationServiceManager,
                context = context
            )
            
            // Process each triggered geofence
            for (geofence in geofences) {
                val geofenceId = geofence.requestId
                val transitionName = if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) "ENTER" else "EXIT"
                Log.d(TAG, "Processing geofence $transitionName: $geofenceId")
                
                // Delegate to LocationReminderManager to handle the transition
                locationReminderManager.handleGeofenceTransition(
                    geofenceId = geofenceId,
                    transitionType = transitionType
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing geofence transition", e)
        }
    }
}
