package com.example.voicereminder.domain

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages location history for smart suggestions
 * Tracks frequently visited places and suggests saving them
 */
class LocationHistoryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "LocationHistoryManager"
        private const val PREFS_NAME = "location_history_prefs"
        private const val KEY_VISIT_HISTORY = "visit_history"
        private const val KEY_SUGGESTED_LOCATIONS = "suggested_locations"
        private const val MIN_VISITS_FOR_SUGGESTION = 3
        private const val CLUSTER_RADIUS_METERS = 100f
        private const val MAX_HISTORY_ENTRIES = 500
        private const val MAX_SUGGESTIONS = 5
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    
    /**
     * Data class representing a location visit
     */
    @Serializable
    data class LocationVisit(
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long,
        val dayOfWeek: Int, // 1=Monday, 7=Sunday
        val hourOfDay: Int // 0-23
    )
    
    /**
     * Data class representing a frequently visited location cluster
     */
    @Serializable
    data class LocationCluster(
        val centerLatitude: Double,
        val centerLongitude: Double,
        val visitCount: Int,
        val lastVisit: Long,
        val commonDays: List<Int>, // Most common days of week
        val commonHours: List<Int>, // Most common hours
        val suggestedName: String? = null,
        val dismissed: Boolean = false
    )
    
    /**
     * Record a location visit
     */
    suspend fun recordVisit(location: Location) = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = now
            
            val visit = LocationVisit(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = now,
                dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK),
                hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            )
            
            val history = getVisitHistory().toMutableList()
            history.add(visit)
            
            // Trim history if too large
            if (history.size > MAX_HISTORY_ENTRIES) {
                history.removeAt(0)
            }
            
            saveVisitHistory(history)
            
            // Update clusters
            updateClusters()
            
            Log.d(TAG, "Recorded location visit at (${location.latitude}, ${location.longitude})")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording visit", e)
        }
    }
    
    /**
     * Get visit history
     */
    private fun getVisitHistory(): List<LocationVisit> {
        return try {
            val historyJson = prefs.getString(KEY_VISIT_HISTORY, null) ?: return emptyList()
            json.decodeFromString<List<LocationVisit>>(historyJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading visit history", e)
            emptyList()
        }
    }
    
    /**
     * Save visit history
     */
    private fun saveVisitHistory(history: List<LocationVisit>) {
        try {
            val historyJson = json.encodeToString(history)
            prefs.edit().putString(KEY_VISIT_HISTORY, historyJson).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving visit history", e)
        }
    }
    
    /**
     * Update location clusters based on visit history
     */
    private fun updateClusters() {
        try {
            val history = getVisitHistory()
            if (history.isEmpty()) return
            
            val clusters = mutableListOf<LocationCluster>()
            val processedIndices = mutableSetOf<Int>()
            
            for (i in history.indices) {
                if (i in processedIndices) continue
                
                val visit = history[i]
                val clusterVisits = mutableListOf(visit)
                processedIndices.add(i)
                
                // Find all visits within cluster radius
                for (j in (i + 1) until history.size) {
                    if (j in processedIndices) continue
                    
                    val otherVisit = history[j]
                    val distance = calculateDistance(
                        visit.latitude, visit.longitude,
                        otherVisit.latitude, otherVisit.longitude
                    )
                    
                    if (distance <= CLUSTER_RADIUS_METERS) {
                        clusterVisits.add(otherVisit)
                        processedIndices.add(j)
                    }
                }
                
                // Create cluster if enough visits
                if (clusterVisits.size >= MIN_VISITS_FOR_SUGGESTION) {
                    val centerLat = clusterVisits.map { it.latitude }.average()
                    val centerLon = clusterVisits.map { it.longitude }.average()
                    
                    // Find common days and hours
                    val dayFrequency = clusterVisits.groupingBy { it.dayOfWeek }.eachCount()
                    val hourFrequency = clusterVisits.groupingBy { it.hourOfDay }.eachCount()
                    
                    val commonDays = dayFrequency.entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { it.key }
                    
                    val commonHours = hourFrequency.entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { it.key }
                    
                    clusters.add(LocationCluster(
                        centerLatitude = centerLat,
                        centerLongitude = centerLon,
                        visitCount = clusterVisits.size,
                        lastVisit = clusterVisits.maxOf { it.timestamp },
                        commonDays = commonDays,
                        commonHours = commonHours
                    ))
                }
            }
            
            // Save top clusters as suggestions
            val topClusters = clusters
                .sortedByDescending { it.visitCount }
                .take(MAX_SUGGESTIONS)
            
            saveSuggestedLocations(topClusters)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating clusters", e)
        }
    }
    
    /**
     * Get suggested locations to save
     */
    suspend fun getSuggestedLocations(): List<LocationCluster> = withContext(Dispatchers.IO) {
        try {
            val suggestionsJson = prefs.getString(KEY_SUGGESTED_LOCATIONS, null) ?: return@withContext emptyList()
            json.decodeFromString<List<LocationCluster>>(suggestionsJson)
                .filter { !it.dismissed }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading suggested locations", e)
            emptyList()
        }
    }
    
    /**
     * Save suggested locations
     */
    private fun saveSuggestedLocations(clusters: List<LocationCluster>) {
        try {
            val clustersJson = json.encodeToString(clusters)
            prefs.edit().putString(KEY_SUGGESTED_LOCATIONS, clustersJson).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving suggested locations", e)
        }
    }
    
    /**
     * Dismiss a location suggestion
     */
    suspend fun dismissSuggestion(latitude: Double, longitude: Double) {
        withContext(Dispatchers.IO) {
            try {
                val suggestions = getSuggestedLocations().toMutableList()
                val index = suggestions.indexOfFirst { 
                    calculateDistance(it.centerLatitude, it.centerLongitude, latitude, longitude) < CLUSTER_RADIUS_METERS
                }
                
                if (index >= 0) {
                    suggestions[index] = suggestions[index].copy(dismissed = true)
                    saveSuggestedLocations(suggestions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing suggestion", e)
            }
            Unit // Explicit return to avoid if-expression issue
        }
    }
    
    /**
     * Get visit statistics for a location
     */
    suspend fun getVisitStats(latitude: Double, longitude: Double): VisitStats? {
        return withContext(Dispatchers.IO) {
            try {
                val history = getVisitHistory()
                val nearbyVisits = history.filter { visit ->
                    calculateDistance(visit.latitude, visit.longitude, latitude, longitude) <= CLUSTER_RADIUS_METERS
                }
                
                if (nearbyVisits.isEmpty()) {
                    null
                } else {
                    val dayFrequency = nearbyVisits.groupingBy { it.dayOfWeek }.eachCount()
                    val hourFrequency = nearbyVisits.groupingBy { it.hourOfDay }.eachCount()
                    
                    VisitStats(
                        totalVisits = nearbyVisits.size,
                        lastVisit = nearbyVisits.maxOf { it.timestamp },
                        mostCommonDay = dayFrequency.maxByOrNull { it.value }?.key,
                        mostCommonHour = hourFrequency.maxByOrNull { it.value }?.key
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting visit stats", e)
                null
            }
        }
    }
    
    /**
     * Calculate distance between two coordinates in meters
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    
    /**
     * Clear all history
     */
    fun clearHistory() {
        prefs.edit()
            .remove(KEY_VISIT_HISTORY)
            .remove(KEY_SUGGESTED_LOCATIONS)
            .apply()
    }
}

/**
 * Statistics about visits to a location
 */
data class VisitStats(
    val totalVisits: Int,
    val lastVisit: Long,
    val mostCommonDay: Int?, // 1=Sunday, 7=Saturday (Calendar constants)
    val mostCommonHour: Int? // 0-23
)
