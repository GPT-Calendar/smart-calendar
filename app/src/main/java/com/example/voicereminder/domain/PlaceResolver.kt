package com.example.voicereminder.domain

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import com.example.voicereminder.data.PlaceCategory
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.SavedLocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * PlaceResolver handles geocoding and place resolution for location-based reminders
 * Converts place names/addresses to coordinates and manages saved locations
 */
class PlaceResolver(
    private val context: Context,
    private val database: ReminderDatabase
) {
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
    
    // Cache for nearby place search results
    private val nearbyPlacesCache = ConcurrentHashMap<String, CachedPlaceResult>()
    
    // Cache for geocoding results
    private val geocodingCache = ConcurrentHashMap<String, CachedGeocodeResult>()
    
    companion object {
        private const val TAG = "PlaceResolver"
        private const val MAX_GEOCODING_RESULTS = 5
        private const val GEOCODING_TIMEOUT_MS = 10000L
        private const val CACHE_EXPIRATION_MS = 30 * 60 * 1000L // 30 minutes
        private const val MAX_SAVED_LOCATIONS = 20 // Limit saved locations to conserve memory
    }
    
    /**
     * Cached result for nearby places search
     */
    private data class CachedPlaceResult(
        val places: List<Place>,
        val timestamp: Long
    )
    
    /**
     * Cached result for geocoding
     */
    private data class CachedGeocodeResult(
        val placeResult: PlaceResult?,
        val timestamp: Long
    )
    
    /**
     * Result of a place resolution operation
     */
    data class PlaceResult(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val address: String? = null
    )
    
    /**
     * Represents a place with category information
     */
    data class Place(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val category: PlaceCategory
    )
    
    /**
     * Result of a place resolution operation with error details
     */
    sealed class PlaceResolutionResult {
        data class Success(val placeResult: PlaceResult) : PlaceResolutionResult()
        data class Error(val errorType: PlaceResolutionErrorType, val message: String) : PlaceResolutionResult()
    }
    
    /**
     * Types of errors that can occur during place resolution
     */
    enum class PlaceResolutionErrorType {
        NOT_FOUND,
        NETWORK_ERROR,
        SERVICE_UNAVAILABLE,
        INVALID_INPUT,
        UNKNOWN_ERROR
    }
    
    /**
     * Resolve a place name or address to geographic coordinates
     * 
     * @param placeName The name or address to resolve
     * @return PlaceResult with coordinates, or null if resolution fails
     */
    suspend fun resolvePlace(placeName: String): PlaceResult? = withContext(Dispatchers.IO) {
        val result = resolvePlaceWithError(placeName)
        return@withContext when (result) {
            is PlaceResolutionResult.Success -> result.placeResult
            is PlaceResolutionResult.Error -> {
                Log.e(TAG, "Place resolution failed: ${result.message}")
                null
            }
        }
    }
    
    /**
     * Resolve a place name or address to geographic coordinates with detailed error information
     * 
     * @param placeName The name or address to resolve
     * @return PlaceResolutionResult with success or detailed error information
     */
    suspend fun resolvePlaceWithError(placeName: String): PlaceResolutionResult = withContext(Dispatchers.IO) {
        try {
            if (placeName.isBlank()) {
                return@withContext PlaceResolutionResult.Error(
                    PlaceResolutionErrorType.INVALID_INPUT,
                    "Place name cannot be empty"
                )
            }
            
            Log.d(TAG, "Resolving place: $placeName")
            
            // First check if it's a saved location
            val savedLocation = getSavedLocation(placeName)
            if (savedLocation != null) {
                Log.d(TAG, "Found saved location: ${savedLocation.name}")
                return@withContext PlaceResolutionResult.Success(
                    PlaceResult(
                        name = savedLocation.name,
                        latitude = savedLocation.latitude,
                        longitude = savedLocation.longitude,
                        address = null
                    )
                )
            }
            
            // Check geocoding cache
            val cacheKey = placeName.lowercase().trim()
            val cachedResult = geocodingCache[cacheKey]
            if (cachedResult != null && !isCacheExpired(cachedResult.timestamp)) {
                Log.d(TAG, "Returning cached geocoding result for: $placeName")
                return@withContext if (cachedResult.placeResult != null) {
                    PlaceResolutionResult.Success(cachedResult.placeResult)
                } else {
                    PlaceResolutionResult.Error(
                        PlaceResolutionErrorType.NOT_FOUND,
                        "Could not find location '$placeName'. Please try a more specific address or save this location manually."
                    )
                }
            }
            
            // Use Geocoder to resolve the address
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use new async API for Android 13+
                geocodeAsync(placeName)
            } else {
                // Use legacy synchronous API
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(placeName, MAX_GEOCODING_RESULTS)
                } catch (e: IOException) {
                    Log.e(TAG, "Geocoding I/O error: ${e.message}")
                    return@withContext PlaceResolutionResult.Error(
                        PlaceResolutionErrorType.NETWORK_ERROR,
                        "Unable to connect to geocoding service. Please check your internet connection and try again."
                    )
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Invalid place name: ${e.message}")
                    return@withContext PlaceResolutionResult.Error(
                        PlaceResolutionErrorType.INVALID_INPUT,
                        "The place name '$placeName' is not valid. Please try a different location."
                    )
                }
            }
            
            if (addresses.isNullOrEmpty()) {
                Log.w(TAG, "No results found for: $placeName")
                // Cache the negative result to avoid repeated API calls
                geocodingCache[cacheKey] = CachedGeocodeResult(
                    placeResult = null,
                    timestamp = System.currentTimeMillis()
                )
                return@withContext PlaceResolutionResult.Error(
                    PlaceResolutionErrorType.NOT_FOUND,
                    "Could not find location '$placeName'. Please try a more specific address or save this location manually."
                )
            }
            
            val address = addresses.first()
            val result = PlaceResult(
                name = placeName,
                latitude = address.latitude,
                longitude = address.longitude,
                address = formatAddress(address)
            )
            
            // Cache the successful result
            geocodingCache[cacheKey] = CachedGeocodeResult(
                placeResult = result,
                timestamp = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Resolved place: ${result.name} at (${result.latitude}, ${result.longitude}) - cached for future use")
            return@withContext PlaceResolutionResult.Success(result)
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error resolving place: ${e.message}", e)
            return@withContext PlaceResolutionResult.Error(
                PlaceResolutionErrorType.NETWORK_ERROR,
                "Network error occurred. Please check your internet connection and try again."
            )
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid input: ${e.message}", e)
            return@withContext PlaceResolutionResult.Error(
                PlaceResolutionErrorType.INVALID_INPUT,
                "Invalid location name. Please provide a valid address or place name."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error resolving place: ${e.message}", e)
            return@withContext PlaceResolutionResult.Error(
                PlaceResolutionErrorType.UNKNOWN_ERROR,
                "An unexpected error occurred while finding the location. Please try again."
            )
        }
    }
    
    /**
     * Geocode using the async API (Android 13+)
     */
    private suspend fun geocodeAsync(placeName: String): List<Address>? = suspendCoroutine { continuation ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(
                placeName,
                MAX_GEOCODING_RESULTS,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        continuation.resume(addresses)
                    }
                    
                    override fun onError(errorMessage: String?) {
                        Log.e(TAG, "Geocoding error: $errorMessage")
                        continuation.resume(null)
                    }
                }
            )
        } else {
            continuation.resume(null)
        }
    }
    
    /**
     * Format an Address object into a readable string
     */
    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()
        
        // Add street address
        for (i in 0..address.maxAddressLineIndex) {
            address.getAddressLine(i)?.let { parts.add(it) }
        }
        
        // If no address lines, build from components
        if (parts.isEmpty()) {
            address.featureName?.let { parts.add(it) }
            address.locality?.let { parts.add(it) }
            address.adminArea?.let { parts.add(it) }
            address.countryName?.let { parts.add(it) }
        }
        
        return parts.joinToString(", ")
    }
    
    /**
     * Get a saved location by name from the database
     * 
     * @param name The location name (case-insensitive)
     * @return SavedLocationEntity or null if not found
     */
    suspend fun getSavedLocation(name: String): SavedLocationEntity? = withContext(Dispatchers.IO) {
        try {
            val location = database.savedLocationDao().getLocationByName(name)
            if (location != null) {
                Log.d(TAG, "Retrieved saved location: ${location.name}")
            }
            return@withContext location
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving saved location: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Find nearby places of a specific category
     * Uses caching to minimize API calls and improve performance
     * 
     * @param category The place category to search for
     * @param location The user's current location
     * @param radiusMeters Search radius in meters (default 5000m = 5km)
     * @return List of nearby places
     */
    suspend fun findNearbyPlaces(
        category: PlaceCategory,
        location: Location,
        radiusMeters: Int = 5000
    ): List<Place> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Finding nearby places for category: $category within ${radiusMeters}m")
            
            // Create cache key based on category, location, and radius
            val cacheKey = createCacheKey(category, location.latitude, location.longitude, radiusMeters)
            
            // Check cache first
            val cachedResult = nearbyPlacesCache[cacheKey]
            if (cachedResult != null && !isCacheExpired(cachedResult.timestamp)) {
                Log.d(TAG, "Returning ${cachedResult.places.size} cached places for $category")
                return@withContext cachedResult.places
            }
            
            // Perform the search
            val places = searchNearbyPlaces(category, location, radiusMeters)
            
            // Cache the results
            if (places.isNotEmpty()) {
                nearbyPlacesCache[cacheKey] = CachedPlaceResult(
                    places = places,
                    timestamp = System.currentTimeMillis()
                )
                Log.d(TAG, "Cached ${places.size} places for $category")
            }
            
            return@withContext places
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding nearby places: ${e.message}", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Perform the actual nearby place search using OpenStreetMap Overpass API
     * This is a free alternative to Google Places API
     */
    private suspend fun searchNearbyPlaces(
        category: PlaceCategory,
        location: Location,
        radiusMeters: Int
    ): List<Place> {
        return try {
            val osmTags = getOsmTagsForCategory(category)
            if (osmTags.isEmpty()) {
                Log.w(TAG, "No OSM tags defined for category: $category")
                return emptyList()
            }
            
            // Build Overpass API query
            val query = buildOverpassQuery(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusMeters = radiusMeters,
                tags = osmTags
            )
            
            // Execute query
            val places = executeOverpassQuery(query, category)
            
            Log.d(TAG, "Found ${places.size} nearby places for category $category")
            places
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching nearby places: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get OpenStreetMap tags for a place category
     */
    private fun getOsmTagsForCategory(category: PlaceCategory): List<String> {
        return when (category) {
            PlaceCategory.STORE -> listOf(
                "shop=supermarket",
                "shop=convenience",
                "shop=department_store",
                "shop=mall"
            )
            PlaceCategory.GROCERY -> listOf(
                "shop=supermarket",
                "shop=grocery",
                "shop=greengrocer"
            )
            PlaceCategory.PHARMACY -> listOf(
                "amenity=pharmacy",
                "healthcare=pharmacy"
            )
            PlaceCategory.GAS_STATION -> listOf(
                "amenity=fuel"
            )
            PlaceCategory.RESTAURANT -> listOf(
                "amenity=restaurant",
                "amenity=fast_food",
                "amenity=cafe"
            )
            PlaceCategory.CUSTOM -> emptyList()
        }
    }
    
    /**
     * Build Overpass API query string
     */
    private fun buildOverpassQuery(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        tags: List<String>
    ): String {
        val tagQueries = tags.joinToString("") { tag ->
            val parts = tag.split("=")
            if (parts.size == 2) {
                """node["${parts[0]}"="${parts[1]}"](around:$radiusMeters,$latitude,$longitude);"""
            } else {
                ""
            }
        }
        
        return """
            [out:json][timeout:10];
            (
                $tagQueries
            );
            out body center 10;
        """.trimIndent()
    }
    
    /**
     * Execute Overpass API query and parse results
     */
    private suspend fun executeOverpassQuery(query: String, category: PlaceCategory): List<Place> {
        return withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://overpass-api.de/api/interpreter")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                // Send query
                connection.outputStream.use { os ->
                    os.write("data=$query".toByteArray())
                }
                
                // Read response
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseOverpassResponse(response, category)
                } else {
                    Log.e(TAG, "Overpass API error: ${connection.responseCode}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing Overpass query: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    /**
     * Parse Overpass API JSON response
     */
    private fun parseOverpassResponse(response: String, category: PlaceCategory): List<Place> {
        return try {
            val places = mutableListOf<Place>()
            
            // Simple JSON parsing without external library
            val elementsStart = response.indexOf("\"elements\"")
            if (elementsStart == -1) return emptyList()
            
            // Extract elements array
            val arrayStart = response.indexOf("[", elementsStart)
            val arrayEnd = response.lastIndexOf("]")
            if (arrayStart == -1 || arrayEnd == -1) return emptyList()
            
            val elementsJson = response.substring(arrayStart, arrayEnd + 1)
            
            // Parse each element (simplified parsing)
            val elementPattern = """\{[^{}]*"lat"\s*:\s*([-\d.]+)[^{}]*"lon"\s*:\s*([-\d.]+)[^{}]*\}""".toRegex()
            val namePattern = """"name"\s*:\s*"([^"]+)"""".toRegex()
            
            // Find all elements with lat/lon
            var searchStart = 0
            while (searchStart < elementsJson.length) {
                val elementStart = elementsJson.indexOf("{", searchStart)
                if (elementStart == -1) break
                
                val elementEnd = elementsJson.indexOf("}", elementStart)
                if (elementEnd == -1) break
                
                val elementStr = elementsJson.substring(elementStart, elementEnd + 1)
                
                // Extract lat/lon
                val latMatch = """"lat"\s*:\s*([-\d.]+)""".toRegex().find(elementStr)
                val lonMatch = """"lon"\s*:\s*([-\d.]+)""".toRegex().find(elementStr)
                val nameMatch = namePattern.find(elementStr)
                
                if (latMatch != null && lonMatch != null) {
                    val lat = latMatch.groupValues[1].toDoubleOrNull()
                    val lon = lonMatch.groupValues[1].toDoubleOrNull()
                    val name = nameMatch?.groupValues?.get(1) ?: category.name.lowercase().replaceFirstChar { it.uppercase() }
                    
                    if (lat != null && lon != null) {
                        places.add(Place(
                            name = name,
                            latitude = lat,
                            longitude = lon,
                            category = category
                        ))
                    }
                }
                
                searchStart = elementEnd + 1
                
                // Limit results
                if (places.size >= 10) break
            }
            
            places
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Overpass response: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Create a cache key for nearby places search
     */
    private fun createCacheKey(
        category: PlaceCategory,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): String {
        // Round coordinates to reduce cache fragmentation
        val roundedLat = String.format("%.3f", latitude)
        val roundedLon = String.format("%.3f", longitude)
        return "${category.name}_${roundedLat}_${roundedLon}_$radiusMeters"
    }
    
    /**
     * Check if a cached result has expired
     */
    private fun isCacheExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_EXPIRATION_MS
    }
    
    /**
     * Clear all caches (nearby places and geocoding)
     * Should be called when user location changes significantly
     */
    fun clearCache() {
        val nearbyCount = nearbyPlacesCache.size
        val geocodingCount = geocodingCache.size
        nearbyPlacesCache.clear()
        geocodingCache.clear()
        Log.d(TAG, "Cleared all caches: $nearbyCount nearby places, $geocodingCount geocoding results")
    }
    
    /**
     * Clear expired entries from all caches
     * This helps conserve memory by removing stale data
     */
    fun clearExpiredCache() {
        val currentTime = System.currentTimeMillis()
        var removedCount = 0
        
        // Clear expired nearby places cache
        val nearbyIterator = nearbyPlacesCache.entries.iterator()
        while (nearbyIterator.hasNext()) {
            val entry = nearbyIterator.next()
            if (currentTime - entry.value.timestamp > CACHE_EXPIRATION_MS) {
                nearbyIterator.remove()
                removedCount++
            }
        }
        
        // Clear expired geocoding cache
        val geocodingIterator = geocodingCache.entries.iterator()
        while (geocodingIterator.hasNext()) {
            val entry = geocodingIterator.next()
            if (currentTime - entry.value.timestamp > CACHE_EXPIRATION_MS) {
                geocodingIterator.remove()
                removedCount++
            }
        }
        
        if (removedCount > 0) {
            Log.d(TAG, "Cleared $removedCount expired cache entries (nearby places + geocoding)")
        }
    }
    
    /**
     * Get cache statistics for monitoring
     * @return Pair of (nearby places cache size, geocoding cache size)
     */
    fun getCacheStats(): Pair<Int, Int> {
        return Pair(nearbyPlacesCache.size, geocodingCache.size)
    }
    
    /**
     * Map PlaceCategory to Google Places API types
     * This will be used when Places API is integrated
     */
    private fun getCategoryPlaceTypes(category: PlaceCategory): List<String> {
        return when (category) {
            PlaceCategory.STORE -> listOf("store", "shopping_mall", "department_store")
            PlaceCategory.GROCERY -> listOf("grocery_or_supermarket", "supermarket")
            PlaceCategory.PHARMACY -> listOf("pharmacy", "drugstore")
            PlaceCategory.GAS_STATION -> listOf("gas_station")
            PlaceCategory.RESTAURANT -> listOf("restaurant", "food")
            PlaceCategory.CUSTOM -> emptyList()
        }
    }
    
    /**
     * Calculate distance between two locations in meters
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    
    /**
     * Check if a location is within a geofence radius
     */
    fun isWithinRadius(
        userLat: Double,
        userLon: Double,
        targetLat: Double,
        targetLon: Double,
        radiusMeters: Float
    ): Boolean {
        val distance = calculateDistance(userLat, userLon, targetLat, targetLon)
        return distance <= radiusMeters
    }
}
