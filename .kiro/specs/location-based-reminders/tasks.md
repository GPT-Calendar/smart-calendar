# Implementation Plan: Location-Based Reminders

## Task List

- [x] 1. Extend data models and database schema for location-based reminders





  - Add ReminderType enum (TIME_BASED, LOCATION_BASED) to support different reminder types
  - Extend ReminderEntity with nullable scheduledTime, reminderType, locationData (JSON), and geofenceId fields
  - Create LocationData, LocationType, and PlaceCategory data classes for location information
  - Create SavedLocationEntity for storing user-configured locations (home, work, etc.)
  - Update ReminderDao with queries for location-based reminders
  - Create SavedLocationDao with CRUD operations for saved locations
  - Implement database migration to add new columns to existing reminders table
  - _Requirements: 1.5, 4.3, 6.1_

- [x] 2. Implement location service infrastructure





  - [x] 2.1 Create LocationServiceManager for permission and location access management


    - Implement hasLocationPermission() and hasBackgroundLocationPermission() checks
    - Implement isLocationEnabled() to check if device location services are active
    - Implement getCurrentLocation() using FusedLocationProviderClient
    - Create permission request methods for activity integration
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 7.1_
  
  - [x] 2.2 Add location permissions to AndroidManifest.xml


    - Add ACCESS_FINE_LOCATION permission for accurate geofencing
    - Add ACCESS_COARSE_LOCATION permission as fallback
    - Add ACCESS_BACKGROUND_LOCATION permission for Android 10+ background monitoring
    - Register GeofenceBroadcastReceiver in manifest
    - _Requirements: 3.1, 3.3, 3.4, 3.5_
  
  - [x] 2.3 Add Google Play Services Location dependency


    - Update app/build.gradle.kts with play-services-location dependency
    - Add kotlinx-serialization plugin and dependency for LocationData JSON serialization
    - Sync project and verify dependencies resolve correctly
    - _Requirements: 1.5, 2.1_

- [x] 3. Implement geofencing functionality





  - [x] 3.1 Create GeofenceManager for Android Geofencing API integration


    - Initialize GeofencingClient from Google Play Services
    - Implement registerGeofence() for single location geofence registration
    - Implement registerMultipleGeofences() for generic category locations
    - Implement removeGeofence() and removeAllGeofences() for cleanup
    - Create getGeofencePendingIntent() for broadcast receiver integration
    - Handle geofence registration errors with retry logic
    - _Requirements: 2.1, 2.2, 2.4, 2.5, 5.1_
  
  - [x] 3.2 Create GeofenceBroadcastReceiver for geofence transition events


    - Implement onReceive() to handle GeofencingEvent
    - Extract triggering geofences and transition type (ENTER/EXIT)
    - Call LocationReminderManager to handle geofence entry
    - Implement error handling for geofencing errors
    - _Requirements: 2.2, 2.5, 5.2_
  
  - [x] 3.3 Implement geofence re-registration in BootReceiver


    - Extend existing BootReceiver to re-register location-based reminders
    - Query all active location reminders from database
    - Re-register geofences for each active location reminder
    - Handle errors gracefully with logging
    - _Requirements: 2.5, 7.5_

- [x] 4. Extend command parser for location-based commands







  - [x] 4.1 Add location command detection patterns to CommandParser

    - Create LOCATION_TRIGGER_PATTERN regex for "when I reach", "when I get to", "at the"
    - Create GENERIC_PLACE_PATTERN regex for store, shop, grocery, pharmacy, etc.
    - Create NAMED_PLACE_PATTERN regex for home, work, office, gym
    - Implement isLocationCommand() to detect location-based commands
    - _Requirements: 1.1, 1.2, 1.3_

  

  - [x] 4.2 Implement location command parsing logic


    - Create ParsedLocationCommand data class with message, locationType, placeName, placeCategory
    - Implement parseLocationCommand() to extract location details from voice text
    - Handle both generic categories and specific named places
    - Extract reminder message from location commands
    - Return null for unparseable location commands
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  
  - [ ]* 4.3 Write unit tests for location command parsing
    - Test parsing of generic location commands ("remind me to buy milk at any store")
    - Test parsing of specific location commands ("remind me to take medicine when I get home")
    - Test extraction of place categories (store, pharmacy, etc.)
    - Test extraction of named places (home, work)
    - Test edge cases (ambiguous commands, missing information)
    - _Requirements: 1.1, 1.2, 1.3_

- [x] 5. Implement place resolution and geocoding





  - [x] 5.1 Create PlaceResolver for geocoding and place search


    - Initialize Geocoder for address-to-coordinate conversion
    - Implement resolvePlace() to convert place names/addresses to coordinates
    - Implement getSavedLocation() to retrieve user-configured locations
    - Handle geocoding failures with appropriate error messages
    - _Requirements: 1.4, 4.2, 4.4_
  

  - [x] 5.2 Implement nearby place search for generic categories

    - Create findNearbyPlaces() method for category-based search
    - Use Android Places API or fallback to basic location matching
    - Return list of Place objects with coordinates and category
    - Cache search results to minimize API calls
    - _Requirements: 1.2, 2.2, 2.4_
  
  - [ ]* 5.3 Write unit tests for place resolution
    - Test geocoding with mock Geocoder
    - Test saved location retrieval from database
    - Test nearby place search with mock data
    - Test error handling for geocoding failures
    - _Requirements: 1.4, 4.2_

- [x] 6. Implement LocationReminderManager for business logic





  - [x] 6.1 Create LocationReminderManager class


    - Initialize with database, GeofenceManager, PlaceResolver, and context
    - Implement createLocationReminder() to save reminder and register geofence
    - Generate unique geofenceId for each location reminder
    - Serialize LocationData to JSON for database storage
    - Handle specific locations (single geofence) and generic categories (multiple geofences)
    - _Requirements: 1.5, 2.1, 2.2, 4.3, 4.4_
  
  - [x] 6.2 Implement geofence transition handling


    - Create handleGeofenceTransition() to process geofence entry events
    - Query reminder by geofenceId from database
    - Trigger notification for matched reminder
    - Mark reminder as completed or implement snooze logic
    - Implement 30-minute cooldown to prevent duplicate triggers
    - _Requirements: 2.2, 2.5, 5.2, 5.3, 7.4_
  
  - [x] 6.3 Implement location reminder query and management methods


    - Create getActiveLocationReminders() to fetch pending location reminders
    - Create deleteLocationReminder() to remove reminder and unregister geofence
    - Implement automatic location monitoring start/stop based on active reminders
    - _Requirements: 2.5, 5.4, 5.5, 6.3, 6.4_
  
  - [x] 6.4 Handle edge case: user already at location


    - Check if user's current location is within reminder's geofence radius
    - Trigger reminder immediately if user is already at location
    - Prevent geofence registration for already-triggered reminders
    - _Requirements: 7.3_
  
  - [ ]* 6.5 Write unit tests for LocationReminderManager
    - Test reminder creation with different location types
    - Test geofence transition handling
    - Test reminder triggering logic
    - Test duplicate trigger prevention
    - Test edge case handling (user already at location)
    - _Requirements: 1.5, 2.2, 5.2, 7.3, 7.4_

- [x] 7. Integrate location reminders with existing ReminderManager




  - [x] 7.1 Extend ReminderManager to support location-based reminders


    - Add LocationReminderManager as a dependency
    - Update createReminder() to route location commands to LocationReminderManager
    - Extend getAllReminders() and getActiveReminders() to include location reminders
    - Update deleteReminder() to handle location reminder cleanup
    - _Requirements: 1.5, 6.1, 6.3, 6.4_
  
  - [x] 7.2 Update domain Reminder model to include location data

    - Add reminderType, locationData, and geofenceId fields to Reminder domain model
    - Update toDomain() and toEntity() extension functions
    - Ensure backward compatibility with existing time-based reminders
    - _Requirements: 1.5, 6.1_

- [x] 8. Create saved location management functionality






  - [x] 8.1 Implement saved location CRUD operations

    - Create methods in LocationReminderManager for saving locations
    - Implement addSavedLocation() with name, coordinates, and radius
    - Implement updateSavedLocation() and deleteSavedLocation()
    - Implement getSavedLocations() to retrieve all saved locations
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  

  - [x] 8.2 Add current location capture functionality

    - Implement captureCurrentLocation() using LocationServiceManager
    - Allow users to save current position as a named location
    - Handle location unavailable errors gracefully
    - _Requirements: 4.2, 7.2_

- [x] 9. Implement UI for location reminders





  - [x] 9.1 Update MainActivity to handle location permissions


    - Add permission request handling for location permissions
    - Show permission rationale dialog before requesting
    - Handle permission denial with explanation and settings redirect
    - Request background location permission separately (Android 10+)
    - _Requirements: 3.1, 3.2, 3.3_
  
  - [x] 9.2 Integrate location command parsing in VoiceManager


    - Check if command is location-based using CommandParser.isLocationCommand()
    - Parse location command and extract location details
    - Call LocationReminderManager.createLocationReminder() for location commands
    - Provide voice feedback for successful location reminder creation
    - Handle errors (permission denied, location unavailable, geocoding failed)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 3.2_
  
  - [x] 9.3 Update reminder list UI to display location reminders


    - Add location icon indicator for location-based reminders
    - Display location name or category in reminder list item
    - Show proximity radius in reminder details
    - Differentiate between time-based and location-based reminders visually
    - _Requirements: 6.1, 6.2_
  
  - [x] 9.4 Create LocationSettingsActivity for managing saved locations


    - Create activity layout with RecyclerView for saved locations list
    - Implement add location dialog with options (current position, address entry)
    - Implement edit and delete functionality for saved locations
    - Show location permissions status and provide settings link
    - Display map preview for each saved location (optional)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [ ]* 9.5 Add accessibility support for location features
    - Add content descriptions for location icons and buttons
    - Ensure location permission dialogs are screen reader friendly
    - Test with TalkBack for location settings and reminder creation
    - _Requirements: 6.1, 6.2_

- [x] 10. Implement error handling and edge cases






  - [x] 10.1 Handle location service errors

    - Detect when location services are disabled and show enable prompt
    - Implement retry logic with exponential backoff for location unavailable
    - Show user-friendly error messages for geocoding failures
    - _Requirements: 7.1, 7.2_
  

  - [x] 10.2 Handle geofencing errors

    - Implement retry logic for geofence registration failures (up to 3 attempts)
    - Handle geofence limit exceeded (100 geofences per app) by prioritizing active reminders
    - Detect and re-register geofences removed by system
    - _Requirements: 2.1, 5.1_
  


  - [ ] 10.3 Handle permission changes and revocations
    - Detect when location permissions are revoked
    - Disable location reminders when permissions are denied
    - Show notification to user when location reminders are disabled
    - Re-enable location reminders when permissions are re-granted
    - _Requirements: 3.2, 7.1_
  
  - [ ]* 10.4 Write integration tests for error scenarios
    - Test behavior with location services disabled
    - Test behavior with permissions denied
    - Test geofence registration failure handling
    - Test location unavailable scenarios
    - _Requirements: 7.1, 7.2_

- [x] 11. Optimize battery and performance





  - [x] 11.1 Implement geofence lifecycle management


    - Stop location monitoring when no active location reminders exist
    - Resume location monitoring when new location reminder is created
    - Use appropriate geofence radius to balance accuracy and battery (100-200m)
    - _Requirements: 2.4, 2.5, 5.1, 5.4, 5.5_
  
  - [x] 11.2 Implement caching for geocoding and place search


    - Cache geocoding results to avoid repeated API calls
    - Cache nearby place search results with expiration
    - Limit saved locations to reasonable number (e.g., 20)
    - _Requirements: 1.4, 1.2_
  
  - [ ]* 11.3 Test battery impact of location monitoring
    - Monitor battery usage over 24 hours with active location reminders
    - Compare battery usage with time-based reminders only
    - Optimize geofence parameters based on battery impact
    - _Requirements: 5.1_

- [x] 12. Final integration and testing






  - [x] 12.1 Test end-to-end location reminder flow

    - Create location reminder via voice command
    - Verify geofence registration
    - Simulate location change to trigger reminder
    - Verify notification is displayed correctly
    - _Requirements: 1.1, 1.2, 2.2, 5.2_
  

  - [x] 12.2 Test with real-world scenarios

    - Test "remind me at home" by leaving and returning home
    - Test "remind me at any store" by visiting different stores
    - Test with saved locations (home, work)
    - Test with generic categories (pharmacy, gas station)
    - _Requirements: 1.2, 1.3, 4.4_
  
  - [ ]* 12.3 Perform manual testing for edge cases
    - Test with location services disabled
    - Test with permissions denied
    - Test device restart with active location reminders
    - Test app force-stop and geofence re-registration
    - Test user already at location when creating reminder
    - _Requirements: 7.1, 7.3, 7.5_
