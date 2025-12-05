package com.example.voicereminder.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.domain.LocationReminderManager
import com.example.voicereminder.domain.LocationServiceManager
import com.example.voicereminder.presentation.location.LocationSettingsScreen
import com.example.voicereminder.presentation.ui.theme.SmartCalendarTheme

class LocationSettingsActivity : ComponentActivity() {

    private lateinit var locationServiceManager: LocationServiceManager
    private lateinit var locationReminderManager: LocationReminderManager

    // Permission launcher for location permissions
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission handling will be done in the Compose UI state
    }

    // Permission launcher for background location permission
    private val requestBackgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission handling will be done in the Compose UI state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        locationServiceManager = LocationServiceManager(this)
        val database = ReminderDatabase.getDatabase(this)
        locationReminderManager = LocationReminderManager(
            database = database,
            geofenceManager = com.example.voicereminder.domain.GeofenceManager(this),
            placeResolver = com.example.voicereminder.domain.PlaceResolver(this, database),
            locationServiceManager = locationServiceManager,
            context = this
        )

        setContent {
            SmartCalendarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocationSettingsScreen(
                        locationReminderManager = locationReminderManager,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}