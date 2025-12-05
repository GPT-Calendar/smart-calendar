package com.example.voicereminder.domain

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/**
 * Manages location permissions and location access for location-based reminders.
 * Provides methods to check permissions, request permissions, and get current location.
 */
class LocationServiceManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1001
        const val REQUEST_BACKGROUND_LOCATION_PERMISSION = 1002
    }

    /**
     * Check if fine or coarse location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    /**
     * Check if background location permission is granted (Android 10+)
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Background location is automatically granted on Android 9 and below
            // if foreground location is granted
            true
        }
    }

    /**
     * Check if device location services are enabled
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Get the current device location using FusedLocationProviderClient
     * Implements retry logic with exponential backoff for location unavailable
     * 
     * BATTERY OPTIMIZATION:
     * - Uses PRIORITY_BALANCED_POWER_ACCURACY by default (less battery drain)
     * - Reduced default retries from 3 to 1
     * - Shorter delays between retries
     * 
     * @param maxRetries Maximum number of retry attempts (default: 1 for battery savings)
     * @param highAccuracy Use high accuracy mode (more battery drain) - default false
     * @return Current location or null if location cannot be determined
     */
    suspend fun getCurrentLocation(maxRetries: Int = 1, highAccuracy: Boolean = false): Location? {
        if (!hasLocationPermission()) {
            android.util.Log.e("LocationServiceManager", "Location permission not granted")
            return null
        }

        if (!isLocationEnabled()) {
            android.util.Log.e("LocationServiceManager", "Location services are disabled")
            return null
        }

        var retryCount = 0
        var delayMs = 500L // Start with 500ms delay (reduced from 1000ms)
        
        // Use balanced power accuracy by default to save battery
        // Only use high accuracy when explicitly requested
        val priority = if (highAccuracy) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        
        while (retryCount <= maxRetries) {
            try {
                val cancellationTokenSource = CancellationTokenSource()
                
                val location = suspendCancellableCoroutine { continuation ->
                    fusedLocationClient.getCurrentLocation(
                        priority,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        continuation.resume(location)
                    }.addOnFailureListener { exception ->
                        android.util.Log.e("LocationServiceManager", "Location request failed: ${exception.message}")
                        continuation.resume(null)
                    }

                    continuation.invokeOnCancellation {
                        cancellationTokenSource.cancel()
                    }
                }
                
                if (location != null) {
                    android.util.Log.d("LocationServiceManager", "Location obtained successfully")
                    return location
                }
                
                // Location is null, retry if we haven't exceeded max retries
                if (retryCount < maxRetries) {
                    android.util.Log.w("LocationServiceManager", "Location unavailable, retrying in ${delayMs}ms (attempt ${retryCount + 1}/$maxRetries)")
                    kotlinx.coroutines.delay(delayMs)
                    delayMs *= 2 // Exponential backoff
                    retryCount++
                } else {
                    android.util.Log.e("LocationServiceManager", "Failed to get location after $maxRetries attempts")
                    return null
                }
                
            } catch (e: SecurityException) {
                android.util.Log.e("LocationServiceManager", "Security exception getting location", e)
                return null
            } catch (e: Exception) {
                android.util.Log.e("LocationServiceManager", "Exception getting location: ${e.message}", e)
                if (retryCount < maxRetries) {
                    android.util.Log.w("LocationServiceManager", "Retrying in ${delayMs}ms (attempt ${retryCount + 1}/$maxRetries)")
                    kotlinx.coroutines.delay(delayMs)
                    delayMs *= 2 // Exponential backoff
                    retryCount++
                } else {
                    return null
                }
            }
        }
        
        return null
    }

    /**
     * Request location permissions from the user
     * Should be called from an Activity
     */
    fun requestLocationPermissions(activity: Activity) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        ActivityCompat.requestPermissions(
            activity,
            permissions,
            REQUEST_LOCATION_PERMISSION
        )
    }

    /**
     * Request background location permission (Android 10+)
     * Should be called AFTER foreground location permission is granted
     */
    fun requestBackgroundLocationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_BACKGROUND_LOCATION_PERMISSION
            )
        }
    }

    /**
     * Check if we should show permission rationale
     */
    fun shouldShowLocationPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    /**
     * Check if we should show background permission rationale (Android 10+)
     */
    fun shouldShowBackgroundPermissionRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            false
        }
    }

    /**
     * Open app settings to allow user to manually grant permissions
     */
    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        activity.startActivity(intent)
    }

    /**
     * Open location settings to allow user to enable location services
     */
    fun openLocationSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        activity.startActivity(intent)
    }
}
