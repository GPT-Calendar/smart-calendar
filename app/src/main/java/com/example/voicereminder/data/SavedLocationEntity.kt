package com.example.voicereminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved location (e.g., home, work) in the database
 */
@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // "Home", "Work", etc.
    val latitude: Double,
    val longitude: Double,
    val radius: Float = 100f,
    val createdAt: Long // Unix timestamp in milliseconds
)
