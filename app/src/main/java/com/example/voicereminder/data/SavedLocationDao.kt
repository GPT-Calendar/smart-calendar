package com.example.voicereminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for saved location database operations
 */
@Dao
interface SavedLocationDao {
    
    /**
     * Insert a new saved location into the database
     * @return The ID of the inserted location
     */
    @Insert
    suspend fun insert(location: SavedLocationEntity): Long
    
    /**
     * Get all saved locations ordered by name
     * @return List of all saved locations
     */
    @Query("SELECT * FROM saved_locations ORDER BY name ASC")
    suspend fun getAllLocations(): List<SavedLocationEntity>
    
    /**
     * Get all saved locations as a Flow for reactive updates
     * @return Flow of all saved locations that updates automatically when data changes
     */
    @Query("SELECT * FROM saved_locations ORDER BY name ASC")
    fun getAllLocationsFlow(): Flow<List<SavedLocationEntity>>
    
    /**
     * Get a specific saved location by its ID
     * @param id The location ID
     * @return The saved location entity or null if not found
     */
    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun getLocationById(id: Long): SavedLocationEntity?
    
    /**
     * Get a saved location by its name
     * @param name The location name (e.g., "Home", "Work")
     * @return The saved location entity or null if not found
     */
    @Query("SELECT * FROM saved_locations WHERE name = :name COLLATE NOCASE")
    suspend fun getLocationByName(name: String): SavedLocationEntity?
    
    /**
     * Update an existing saved location
     */
    @Update
    suspend fun update(location: SavedLocationEntity)
    
    /**
     * Delete a saved location from the database
     */
    @Delete
    suspend fun delete(location: SavedLocationEntity)
    
    /**
     * Delete a saved location by its ID
     * @param id The location ID
     */
    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
