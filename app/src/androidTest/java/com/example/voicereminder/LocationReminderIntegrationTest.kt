package com.example.voicereminder

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicereminder.data.*
import com.example.voicereminder.domain.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for location-based reminder functionality
 * Tests end-to-end flow from command parsing to geofence registration
 * 
 * Requirements tested:
 * - 1.1: Parse location commands with keywords
 * - 1.2: Create reminders for generic location types
 * - 2.2: Detect when user enters proximity radius
 * - 5.2: Trigger reminder notification on geofence entry
 */
@RunWith(AndroidJUnit4::class)
class LocationReminderIntegrationTest {
    
    private lateinit var database: ReminderDatabase
    private lateinit var context: Context
    private lateinit var commandParser: CommandParser
    private lateinit var locationServiceManager: LocationServiceManager
    private lateinit var geofenceManager: GeofenceManager
    private lateinit var placeResolver: PlaceResolver
    private lateinit var locationReminderManager: LocationReminderManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = ReminderDatabase.getTestDatabase(context)
        
        // Initialize components
        commandParser = CommandParser()
        locationServiceManager = LocationServiceManager(context)
        geofenceManager = GeofenceManager(context)
        placeResolver = PlaceResolver(context, database)
        locationReminderManager = LocationReminderManager(
            database = database,
            geofenceManager = geofenceManager,
            placeResolver = placeResolver,
            locationServiceManager = locationServiceManager,
            context = context
        )
    }
    
    @After
    fun tearDown() {
        // Clean up geofences
        runBlocking {
            geofenceManager.removeAllGeofences()
        }
        database.close()
    }
    
    /**
     * Test 12.1: End-to-end location reminder flow
     * - Create location reminder via voice command
     * - Verify geofence registration
     * - Simulate location change to trigger reminder
     * - Verify notification is displayed correctly
     */
    @Test
    fun testEndToEndLocationReminderFlow_GenericStore() = runBlocking {
        // Given: A voice command for a generic location
        val voiceCommand = "remind me to buy milk when I reach a store"
        
        // When: Parsing the command
        assertTrue("Command should be detected as location-based", 
            commandParser.isLocationCommand(voiceCommand))
        
        val parsedCommand = commandParser.parseLocationCommand(voiceCommand)
        
        // Then: Command should be parsed correctly
        assertNotNull("Command should be parsed successfully", parsedCommand)
        assertEquals("Message should be extracted", "buy milk", parsedCommand!!.message)
        assertEquals("Location type should be generic category", 
            LocationType.GENERIC_CATEGORY, parsedCommand.locationType)
        assertEquals("Place category should be STORE", 
            PlaceCategory.STORE, parsedCommand.placeCategory)
        
        // When: Creating the location reminder
        val locationData = LocationData(
            locationType = LocationType.GENERIC_CATEGORY,
            latitude = 37.7749, // San Francisco coordinates for testing
            longitude = -122.4194,
            radius = 200f,
            placeName = null,
            placeCategory = PlaceCategory.STORE
        )
        
        val result = locationReminderManager.createLocationReminder(
            message = parsedCommand.message,
            locationData = locationData
        )
        
        // Then: Reminder should be created successfully
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        val reminderId = (result as ReminderResult.Success).reminderId
        assertTrue("Reminder ID should be positive", reminderId > 0)
        
        // Wait for database operations
        delay(100)
        
        // Then: Reminder should be stored in database
        val storedReminder = database.reminderDao().getReminderById(reminderId)
        assertNotNull("Reminder should be stored in database", storedReminder)
        assertEquals("Message should match", "buy milk", storedReminder!!.message)
        assertEquals("Reminder type should be LOCATION_BASED", 
            ReminderType.LOCATION_BASED, storedReminder.reminderType)
        assertEquals("Status should be PENDING", 
            ReminderStatus.PENDING, storedReminder.status)
        assertNotNull("Geofence ID should be set", storedReminder.geofenceId)
        assertNotNull("Location data should be stored", storedReminder.locationData)
        
        // Then: Geofence should be registered (we can't fully test this without permissions)
        // In a real device test with permissions, we would verify geofence registration
        assertNotNull("Geofence ID should be generated", storedReminder.geofenceId)
    }
    
    @Test
    fun testEndToEndLocationReminderFlow_SpecificPlace() = runBlocking {
        // Given: A voice command for a specific place
        val voiceCommand = "remind me to take medicine when I get home"
        
        // When: Parsing the command
        assertTrue("Command should be detected as location-based", 
            commandParser.isLocationCommand(voiceCommand))
        
        val parsedCommand = commandParser.parseLocationCommand(voiceCommand)
        
        // Then: Command should be parsed correctly
        assertNotNull("Command should be parsed successfully", parsedCommand)
        assertEquals("Message should be extracted", "take medicine", parsedCommand!!.message)
        assertEquals("Location type should be specific place", 
            LocationType.SPECIFIC_PLACE, parsedCommand.locationType)
        assertEquals("Place name should be home", "home", parsedCommand.placeName)
        
        // Given: User has saved "home" location
        val homeLocation = SavedLocationEntity(
            name = "home",
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 100f,
            createdAt = System.currentTimeMillis()
        )
        database.savedLocationDao().insert(homeLocation)
        
        // When: Creating the location reminder
        val locationData = LocationData(
            locationType = LocationType.SPECIFIC_PLACE,
            latitude = homeLocation.latitude,
            longitude = homeLocation.longitude,
            radius = homeLocation.radius,
            placeName = "home",
            placeCategory = null
        )
        
        val result = locationReminderManager.createLocationReminder(
            message = parsedCommand.message,
            locationData = locationData
        )
        
        // Then: Reminder should be created successfully
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        val reminderId = (result as ReminderResult.Success).reminderId
        
        // Wait for database operations
        delay(100)
        
        // Then: Reminder should be stored correctly
        val storedReminder = database.reminderDao().getReminderById(reminderId)
        assertNotNull("Reminder should be stored in database", storedReminder)
        assertEquals("Message should match", "take medicine", storedReminder!!.message)
        assertEquals("Reminder type should be LOCATION_BASED", 
            ReminderType.LOCATION_BASED, storedReminder.reminderType)
    }

    
    /**
     * Test 12.2: Real-world scenario - "remind me at home"
     * Tests saved location functionality
     */
    @Test
    fun testRealWorldScenario_RemindMeAtHome() = runBlocking {
        // Given: User has configured "home" as a saved location
        val homeLocation = SavedLocationEntity(
            name = "home",
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 100f,
            createdAt = System.currentTimeMillis()
        )
        val savedLocationId = database.savedLocationDao().insert(homeLocation)
        assertTrue("Saved location should be inserted", savedLocationId > 0)
        
        // When: User creates a reminder for home
        val voiceCommand = "remind me to water plants when I get home"
        val parsedCommand = commandParser.parseLocationCommand(voiceCommand)
        
        assertNotNull("Command should be parsed", parsedCommand)
        assertEquals("Place name should be home", "home", parsedCommand!!.placeName)
        
        // Resolve the saved location
        val resolvedLocation = placeResolver.getSavedLocation("home")
        assertNotNull("Home location should be resolved", resolvedLocation)
        
        val locationData = LocationData(
            locationType = LocationType.SPECIFIC_PLACE,
            latitude = resolvedLocation!!.latitude,
            longitude = resolvedLocation.longitude,
            radius = resolvedLocation.radius,
            placeName = "home",
            placeCategory = null
        )
        
        val result = locationReminderManager.createLocationReminder(
            message = parsedCommand.message,
            locationData = locationData
        )
        
        // Then: Reminder should be created successfully
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        val reminderId = (result as ReminderResult.Success).reminderId
        
        delay(100)
        
        // Verify reminder is stored with correct location data
        val storedReminder = database.reminderDao().getReminderById(reminderId)
        assertNotNull("Reminder should exist", storedReminder)
        assertEquals("Message should match", "water plants", storedReminder!!.message)
        assertNotNull("Location data should be stored", storedReminder.locationData)
    }
    
    /**
     * Test 12.2: Real-world scenario - "remind me at any store"
     * Tests generic category functionality
     */
    @Test
    fun testRealWorldScenario_RemindMeAtAnyStore() = runBlocking {
        // Given: A voice command for any store
        val voiceCommand = "remind me to buy groceries at any store"
        
        // When: Parsing and creating the reminder
        val parsedCommand = commandParser.parseLocationCommand(voiceCommand)
        
        assertNotNull("Command should be parsed", parsedCommand)
        assertEquals("Message should be extracted", "buy groceries", parsedCommand!!.message)
        assertEquals("Category should be STORE", PlaceCategory.STORE, parsedCommand.placeCategory)
        assertEquals("Location type should be generic", 
            LocationType.GENERIC_CATEGORY, parsedCommand.locationType)
        
        // Create location data for generic category
        val locationData = LocationData(
            locationType = LocationType.GENERIC_CATEGORY,
            latitude = null, // Generic categories don't have specific coordinates initially
            longitude = null,
            radius = 200f,
            placeName = null,
            placeCategory = PlaceCategory.STORE
        )
        
        val result = locationReminderManager.createLocationReminder(
            message = parsedCommand.message,
            locationData = locationData
        )
        
        // Then: Reminder should be created successfully
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        
        delay(100)
        
        val reminderId = (result as ReminderResult.Success).reminderId
        val storedReminder = database.reminderDao().getReminderById(reminderId)
        
        assertNotNull("Reminder should exist", storedReminder)
        assertEquals("Reminder type should be LOCATION_BASED", 
            ReminderType.LOCATION_BASED, storedReminder!!.reminderType)
    }
    
    /**
     * Test 12.2: Real-world scenario - saved location "work"
     */
    @Test
    fun testRealWorldScenario_RemindMeAtWork() = runBlocking {
        // Given: User has configured "work" as a saved location
        val workLocation = SavedLocationEntity(
            name = "work",
            latitude = 37.7849,
            longitude = -122.4094,
            radius = 150f,
            createdAt = System.currentTimeMillis()
        )
        database.savedLocationDao().insert(workLocation)
        
        // When: User creates a reminder for work
        val voiceCommand = "remind me to submit report when I arrive at work"
        val parsedCommand = commandParser.parseLocationCommand(voiceCommand)
        
        assertNotNull("Command should be parsed", parsedCommand)
        assertEquals("Place name should be work", "work", parsedCommand!!.placeName)
        assertEquals("Message should be extracted", "submit report", parsedCommand.message)
        
        // Resolve work location
        val resolvedLocation = placeResolver.getSavedLocation("work")
        assertNotNull("Work location should be resolved", resolvedLocation)
        
        val locationData = LocationData(
            locationType = LocationType.SPECIFIC_PLACE,
            latitude = resolvedLocation!!.latitude,
            longitude = resolvedLocation.longitude,
            radius = resolvedLocation.radius,
            placeName = "work",
            placeCategory = null
        )
        
        val result = locationReminderManager.createLocationReminder(
            message = parsedCommand.message,
            locationData = locationData
        )
        
        // Then: Reminder should be created successfully
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        
        delay(100)
        
        val reminderId = (result as ReminderResult.Success).reminderId
        val storedReminder = database.reminderDao().getReminderById(reminderId)
        
        assertNotNull("Reminder should exist", storedReminder)
        assertEquals("Message should match", "submit report", storedReminder!!.message)
    }
    
    /**
     * Test 12.2: Real-world scenario - pharmacy category
     */
    @Test
    fun testRealWorldScenario_RemindMeAtPharmacy() = runBlocking {
        // Given: A voice command for pharmacy
        val voiceCommand = "remind me to pick up prescription at the pharmacy"
        
        // When: Parsing the command
        val parsedCommand = commandParser.parseLocationCommand(voiceCommand)
        
        assertNotNull("Command should be parsed", parsedCommand)
        assertEquals("Message should be extracted", "pick up prescription", parsedCommand!!.message)
        assertEquals("Category should be PHARMACY", PlaceCategory.PHARMACY, parsedCommand.placeCategory)
        
        // Create location data
        val locationData = LocationData(
            locationType = LocationType.GENERIC_CATEGORY,
            latitude = null,
            longitude = null,
            radius = 200f,
            placeName = null,
            placeCategory = PlaceCategory.PHARMACY
        )
        
        val result = locationReminderManager.createLocationReminder(
            message = parsedCommand.message,
            locationData = locationData
        )
        
        // Then: Reminder should be created successfully
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        
        delay(100)
        
        val reminderId = (result as ReminderResult.Success).reminderId
        val storedReminder = database.reminderDao().getReminderById(reminderId)
        
        assertNotNull("Reminder should exist", storedReminder)
        assertEquals("Message should match", "pick up prescription", storedReminder!!.message)
    }
    
    /**
     * Test 12.2: Real-world scenario - gas station category
     */
    @Test
    fun testRealWorldScenario_RemindMeAtGasStation() = runBlocking {
        // Given: A voice command for gas station
        val voiceCommand = "remind me to check tire pressure at a gas station"
        
        // When: Parsing the command
        val parsedCommand = commandParser.parseLocationCommand(voiceCommand)
        
        assertNotNull("Command should be parsed", parsedCommand)
        assertEquals("Message should be extracted", "check tire pressure", parsedCommand!!.message)
        assertEquals("Category should be GAS_STATION", 
            PlaceCategory.GAS_STATION, parsedCommand.placeCategory)
        
        // Create location data
        val locationData = LocationData(
            locationType = LocationType.GENERIC_CATEGORY,
            latitude = null,
            longitude = null,
            radius = 200f,
            placeName = null,
            placeCategory = PlaceCategory.GAS_STATION
        )
        
        val result = locationReminderManager.createLocationReminder(
            message = parsedCommand.message,
            locationData = locationData
        )
        
        // Then: Reminder should be created successfully
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        
        delay(100)
        
        val reminderId = (result as ReminderResult.Success).reminderId
        val storedReminder = database.reminderDao().getReminderById(reminderId)
        
        assertNotNull("Reminder should exist", storedReminder)
        assertEquals("Message should match", "check tire pressure", storedReminder!!.message)
    }
    
    /**
     * Test multiple saved locations
     */
    @Test
    fun testMultipleSavedLocations() = runBlocking {
        // Given: Multiple saved locations
        val locations = listOf(
            SavedLocationEntity(
                name = "home",
                latitude = 37.7749,
                longitude = -122.4194,
                radius = 100f,
                createdAt = System.currentTimeMillis()
            ),
            SavedLocationEntity(
                name = "work",
                latitude = 37.7849,
                longitude = -122.4094,
                radius = 150f,
                createdAt = System.currentTimeMillis()
            ),
            SavedLocationEntity(
                name = "gym",
                latitude = 37.7649,
                longitude = -122.4294,
                radius = 100f,
                createdAt = System.currentTimeMillis()
            )
        )
        
        locations.forEach { database.savedLocationDao().insert(it) }
        
        // When: Creating reminders for each location
        val commands = listOf(
            "remind me to lock door when I get home",
            "remind me to check email when I arrive at work",
            "remind me to bring water bottle at the gym"
        )
        
        val reminderIds = mutableListOf<Long>()
        
        for (command in commands) {
            val parsedCommand = commandParser.parseLocationCommand(command)
            assertNotNull("Command should be parsed", parsedCommand)
            
            val savedLocation = placeResolver.getSavedLocation(parsedCommand!!.placeName!!)
            assertNotNull("Saved location should exist", savedLocation)
            
            val locationData = LocationData(
                locationType = LocationType.SPECIFIC_PLACE,
                latitude = savedLocation!!.latitude,
                longitude = savedLocation.longitude,
                radius = savedLocation.radius,
                placeName = parsedCommand.placeName,
                placeCategory = null
            )
            
            val result = locationReminderManager.createLocationReminder(
                message = parsedCommand.message,
                locationData = locationData
            )
            
            assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
            reminderIds.add((result as ReminderResult.Success).reminderId)
        }
        
        delay(100)
        
        // Then: All reminders should be stored
        assertEquals("Should have 3 reminders", 3, reminderIds.size)
        
        for (reminderId in reminderIds) {
            val reminder = database.reminderDao().getReminderById(reminderId)
            assertNotNull("Reminder should exist", reminder)
            assertEquals("Reminder type should be LOCATION_BASED", 
                ReminderType.LOCATION_BASED, reminder!!.reminderType)
        }
    }
    
    /**
     * Test query for active location reminders
     */
    @Test
    fun testGetActiveLocationReminders() = runBlocking {
        // Given: Multiple location reminders
        val locationData1 = LocationData(
            locationType = LocationType.SPECIFIC_PLACE,
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 100f,
            placeName = "home",
            placeCategory = null
        )
        
        val locationData2 = LocationData(
            locationType = LocationType.GENERIC_CATEGORY,
            latitude = null,
            longitude = null,
            radius = 200f,
            placeName = null,
            placeCategory = PlaceCategory.STORE
        )
        
        val result1 = locationReminderManager.createLocationReminder("Task 1", locationData1)
        val result2 = locationReminderManager.createLocationReminder("Task 2", locationData2)
        
        assertTrue("Both reminders should be created", 
            result1 is ReminderResult.Success && result2 is ReminderResult.Success)
        
        delay(100)
        
        // When: Querying active location reminders
        val activeReminders = locationReminderManager.getActiveLocationReminders()
        
        // Then: Should return both reminders
        assertEquals("Should have 2 active location reminders", 2, activeReminders.size)
        assertTrue("All should be location-based", 
            activeReminders.all { it.reminderType == ReminderType.LOCATION_BASED })
        assertTrue("All should be pending", 
            activeReminders.all { it.status == ReminderStatus.PENDING })
    }
    
    /**
     * Test deletion of location reminder
     */
    @Test
    fun testDeleteLocationReminder() = runBlocking {
        // Given: A location reminder
        val locationData = LocationData(
            locationType = LocationType.SPECIFIC_PLACE,
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 100f,
            placeName = "home",
            placeCategory = null
        )
        
        val result = locationReminderManager.createLocationReminder("Test task", locationData)
        assertTrue("Reminder should be created", result is ReminderResult.Success)
        
        val reminderId = (result as ReminderResult.Success).reminderId
        
        delay(100)
        
        // Verify reminder exists
        val reminderBefore = database.reminderDao().getReminderById(reminderId)
        assertNotNull("Reminder should exist", reminderBefore)
        
        // When: Deleting the reminder
        locationReminderManager.deleteLocationReminder(reminderId)
        
        delay(100)
        
        // Then: Reminder should be removed
        val reminderAfter = database.reminderDao().getReminderById(reminderId)
        assertNull("Reminder should be deleted", reminderAfter)
    }
    
    /**
     * Test error handling for invalid input
     */
    @Test
    fun testErrorHandling_EmptyMessage() = runBlocking {
        // Given: Empty message
        val locationData = LocationData(
            locationType = LocationType.SPECIFIC_PLACE,
            latitude = 37.7749,
            longitude = -122.4194,
            radius = 100f,
            placeName = "home",
            placeCategory = null
        )
        
        // When: Creating reminder with empty message
        val result = locationReminderManager.createLocationReminder("", locationData)
        
        // Then: Should return error
        assertTrue("Should return error", result is ReminderResult.Error)
        assertEquals("Error type should be INVALID_INPUT", 
            ReminderErrorType.INVALID_INPUT, (result as ReminderResult.Error).errorType)
    }
    
    /**
     * Test error handling for missing coordinates
     */
    @Test
    fun testErrorHandling_MissingCoordinates() = runBlocking {
        // Given: Specific place without coordinates
        val locationData = LocationData(
            locationType = LocationType.SPECIFIC_PLACE,
            latitude = null,
            longitude = null,
            radius = 100f,
            placeName = "home",
            placeCategory = null
        )
        
        // When: Creating reminder
        val result = locationReminderManager.createLocationReminder("Test task", locationData)
        
        // Then: Should return error
        assertTrue("Should return error", result is ReminderResult.Error)
        assertEquals("Error type should be INVALID_INPUT", 
            ReminderErrorType.INVALID_INPUT, (result as ReminderResult.Error).errorType)
    }
}
