package com.example.voicereminder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.domain.ReminderResult
import com.example.voicereminder.presentation.calendar.CalendarViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Integration test for voice-to-calendar flow
 * Tests that creating reminders via voice updates the calendar UI automatically
 */
@RunWith(AndroidJUnit4::class)
class VoiceCalendarIntegrationTest {
    
    private lateinit var database: ReminderDatabase
    private lateinit var reminderManager: ReminderManager
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = ReminderDatabase.getTestDatabase(context)
        
        // Create ReminderManager with test database
        reminderManager = ReminderManager(database, context)
        
        // Create CalendarViewModel with ReminderManager
        calendarViewModel = CalendarViewModel(reminderManager)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testCreateReminderViaVoice_UpdatesCalendarAutomatically() = runBlocking {
        // Given: A future date and time
        val futureTime = LocalDateTime.now().plusDays(1).withHour(14).withMinute(30)
        val message = "Team meeting"
        
        // When: Creating a reminder (simulating voice input)
        val result = reminderManager.createReminderWithError(futureTime, message)
        
        // Then: Reminder should be created successfully
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        val reminderId = (result as ReminderResult.Success).reminderId
        assertTrue("Reminder ID should be positive", reminderId > 0)
        
        // Wait a bit for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Then: All reminders list should contain the new reminder
        val allReminders = calendarViewModel.allReminders.value
        assertTrue("All reminders should contain the new reminder", 
            allReminders.any { it.id == reminderId })
        assertEquals("Message should match", message, 
            allReminders.find { it.id == reminderId }?.message)
    }
    
    @Test
    fun testCreateReminder_UpdatesEventIndicators() = runBlocking {
        // Given: A future date
        val futureTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0)
        val targetDate = futureTime.toLocalDate()
        val yearMonth = YearMonth.from(targetDate)
        
        // When: Creating a reminder for that date
        val result = reminderManager.createReminderWithError(futureTime, "Doctor appointment")
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Then: Event indicators should show the reminder
        val eventCounts = calendarViewModel.getRemindersForMonth(yearMonth)
        assertTrue("Event counts should contain the target date", 
            eventCounts.containsKey(targetDate))
        assertEquals("Event count should be 1", 1, eventCounts[targetDate])
    }
    
    @Test
    fun testCreateReminder_UpdatesSelectedDateReminders() = runBlocking {
        // Given: A selected date
        val futureTime = LocalDateTime.now().plusDays(3).withHour(15).withMinute(0)
        val targetDate = futureTime.toLocalDate()
        
        // Select the date first
        calendarViewModel.selectDate(targetDate)
        
        // Verify initially empty
        assertEquals("Selected date should have no reminders initially", 
            0, calendarViewModel.selectedDateReminders.value.size)
        
        // When: Creating a reminder for the selected date
        val result = reminderManager.createReminderWithError(futureTime, "Call dentist")
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Then: Selected date reminders should update automatically
        val selectedReminders = calendarViewModel.selectedDateReminders.value
        assertEquals("Selected date should have 1 reminder", 1, selectedReminders.size)
        assertEquals("Message should match", "Call dentist", selectedReminders[0].message)
    }
    
    @Test
    fun testCreateMultipleReminders_UpdatesAllViews() = runBlocking {
        // Given: Multiple reminders for different dates
        val tomorrow = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
        val dayAfter = LocalDateTime.now().plusDays(2).withHour(14).withMinute(30)
        
        // When: Creating multiple reminders
        val result1 = reminderManager.createReminderWithError(tomorrow, "Morning standup")
        val result2 = reminderManager.createReminderWithError(dayAfter, "Afternoon meeting")
        
        assertTrue("First reminder should succeed", result1 is ReminderResult.Success)
        assertTrue("Second reminder should succeed", result2 is ReminderResult.Success)
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Then: All reminders list should contain both
        val allReminders = calendarViewModel.allReminders.value
        assertEquals("Should have 2 reminders", 2, allReminders.size)
        
        // Then: Event counts should show both dates
        val yearMonth = YearMonth.from(tomorrow.toLocalDate())
        val eventCounts = calendarViewModel.getRemindersForMonth(yearMonth)
        assertTrue("Should have event on tomorrow", 
            eventCounts.containsKey(tomorrow.toLocalDate()))
        assertTrue("Should have event on day after", 
            eventCounts.containsKey(dayAfter.toLocalDate()))
    }
    
    @Test
    fun testDeleteReminder_UpdatesCalendarAutomatically() = runBlocking {
        // Given: An existing reminder
        val futureTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)
        val result = reminderManager.createReminderWithError(futureTime, "Test reminder")
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        val reminderId = (result as ReminderResult.Success).reminderId
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Verify reminder exists
        assertTrue("Reminder should exist in all reminders", 
            calendarViewModel.allReminders.value.any { it.id == reminderId })
        
        // When: Deleting the reminder
        calendarViewModel.deleteReminder(reminderId)
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Then: Calendar should update automatically
        assertFalse("Reminder should be removed from all reminders", 
            calendarViewModel.allReminders.value.any { it.id == reminderId })
    }
    
    @Test
    fun testFlowUpdates_AreReactive() = runBlocking {
        // Given: Observing the Flow
        var updateCount = 0
        val job = kotlinx.coroutines.launch {
            calendarViewModel.allReminders.collect {
                updateCount++
            }
        }
        
        // Initial state counts as first update
        kotlinx.coroutines.delay(50)
        val initialCount = updateCount
        
        // When: Creating a reminder
        val futureTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)
        reminderManager.createReminderWithError(futureTime, "Reactive test")
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Then: Flow should have emitted an update
        assertTrue("Flow should emit updates", updateCount > initialCount)
        
        job.cancel()
    }
    
    @Test
    fun testReminderCompletion_UpdatesCalendarStatus() = runBlocking {
        // Given: An existing pending reminder
        val futureTime = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0)
        val result = reminderManager.createReminderWithError(futureTime, "Meeting to complete")
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        val reminderId = (result as ReminderResult.Success).reminderId
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Verify reminder is pending
        val reminderBefore = calendarViewModel.allReminders.value.find { it.id == reminderId }
        assertNotNull("Reminder should exist", reminderBefore)
        assertEquals("Reminder should be pending", "PENDING", reminderBefore?.status?.name)
        
        // When: Completing the reminder (simulating notification trigger)
        reminderManager.completeReminder(reminderId)
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Then: Calendar should show updated status
        val reminderAfter = calendarViewModel.allReminders.value.find { it.id == reminderId }
        assertNotNull("Reminder should still exist", reminderAfter)
        assertEquals("Reminder should be completed", "COMPLETED", reminderAfter?.status?.name)
    }
    
    @Test
    fun testDeletionFromCalendar_RemovesFromAllViews() = runBlocking {
        // Given: A reminder visible in multiple views
        val futureTime = LocalDateTime.now().plusDays(2).withHour(13).withMinute(0)
        val targetDate = futureTime.toLocalDate()
        val result = reminderManager.createReminderWithError(futureTime, "Multi-view reminder")
        assertTrue("Reminder creation should succeed", result is ReminderResult.Success)
        val reminderId = (result as ReminderResult.Success).reminderId
        
        // Select the date to populate selectedDateReminders
        calendarViewModel.selectDate(targetDate)
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Verify reminder exists in all views
        assertTrue("Should exist in all reminders", 
            calendarViewModel.allReminders.value.any { it.id == reminderId })
        assertTrue("Should exist in selected date reminders", 
            calendarViewModel.selectedDateReminders.value.any { it.id == reminderId })
        
        val yearMonth = YearMonth.from(targetDate)
        val eventCountsBefore = calendarViewModel.getRemindersForMonth(yearMonth)
        assertTrue("Should have event indicator", eventCountsBefore.containsKey(targetDate))
        
        // When: Deleting the reminder from calendar view
        calendarViewModel.deleteReminder(reminderId)
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Then: Reminder should be removed from all views
        assertFalse("Should be removed from all reminders", 
            calendarViewModel.allReminders.value.any { it.id == reminderId })
        assertFalse("Should be removed from selected date reminders", 
            calendarViewModel.selectedDateReminders.value.any { it.id == reminderId })
        
        val eventCountsAfter = calendarViewModel.getRemindersForMonth(yearMonth)
        assertFalse("Should not have event indicator", 
            eventCountsAfter.containsKey(targetDate) && eventCountsAfter[targetDate]!! > 0)
    }
    
    @Test
    fun testNavigationPreservesState_BetweenFragments() = runBlocking {
        // Given: Multiple reminders and a selected date
        val date1 = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
        val date2 = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0)
        val date3 = LocalDateTime.now().plusDays(2).withHour(16).withMinute(0)
        
        reminderManager.createReminderWithError(date1, "Reminder 1")
        reminderManager.createReminderWithError(date2, "Reminder 2")
        reminderManager.createReminderWithError(date3, "Reminder 3")
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Select a specific date
        val selectedDate = date2.toLocalDate()
        calendarViewModel.selectDate(selectedDate)
        
        // Wait for selection to propagate
        kotlinx.coroutines.delay(50)
        
        // Verify state before "navigation"
        val allRemindersBefore = calendarViewModel.allReminders.value
        val selectedDateRemindersBefore = calendarViewModel.selectedDateReminders.value
        val selectedDateBefore = calendarViewModel.selectedDate.value
        
        assertEquals("Should have 3 total reminders", 3, allRemindersBefore.size)
        assertEquals("Should have 2 reminders on selected date", 2, selectedDateRemindersBefore.size)
        assertEquals("Selected date should match", selectedDate, selectedDateBefore)
        
        // Simulate navigation by creating a new ViewModel instance (as would happen in fragment recreation)
        // In real app, ViewModel would be preserved by ViewModelProvider
        // Here we verify that the data source (ReminderManager) maintains consistency
        val newViewModel = CalendarViewModel(reminderManager)
        
        // Wait for new ViewModel to initialize
        kotlinx.coroutines.delay(100)
        
        // Then: State should be consistent (data from same source)
        val allRemindersAfter = newViewModel.allReminders.value
        assertEquals("All reminders should be consistent", 3, allRemindersAfter.size)
        
        // When: Selecting the same date in new ViewModel
        newViewModel.selectDate(selectedDate)
        kotlinx.coroutines.delay(50)
        
        // Then: Selected date reminders should match
        val selectedDateRemindersAfter = newViewModel.selectedDateReminders.value
        assertEquals("Selected date reminders should be consistent", 
            2, selectedDateRemindersAfter.size)
    }
    
    @Test
    fun testFilterPreservation_AfterVoiceCreation() = runBlocking {
        // Given: Some completed and pending reminders
        val past = LocalDateTime.now().minusDays(1).withHour(10).withMinute(0)
        val future = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0)
        
        val result1 = reminderManager.createReminderWithError(past, "Past reminder")
        assertTrue("Past reminder creation should succeed", result1 is ReminderResult.Success)
        val pastId = (result1 as ReminderResult.Success).reminderId
        
        // Complete the past reminder
        reminderManager.completeReminder(pastId)
        
        reminderManager.createReminderWithError(future, "Future reminder")
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Apply PENDING filter
        calendarViewModel.applyFilter(com.example.voicereminder.presentation.calendar.ReminderFilter.PENDING)
        kotlinx.coroutines.delay(50)
        
        // Verify filter is applied
        val filteredBefore = calendarViewModel.allReminders.value
        assertEquals("Should have 1 pending reminder", 1, filteredBefore.size)
        assertTrue("Should only show pending", 
            filteredBefore.all { it.status.name == "PENDING" })
        
        // When: Creating a new reminder via voice (simulated)
        val newFuture = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0)
        reminderManager.createReminderWithError(newFuture, "New voice reminder")
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Then: Filter should still be active and include new pending reminder
        val filteredAfter = calendarViewModel.allReminders.value
        assertEquals("Should have 2 pending reminders", 2, filteredAfter.size)
        assertTrue("Should only show pending", 
            filteredAfter.all { it.status.name == "PENDING" })
        assertTrue("Should include new reminder", 
            filteredAfter.any { it.message == "New voice reminder" })
    }
    
    @Test
    fun testMultipleDeletions_UpdateAllViewsConsistently() = runBlocking {
        // Given: Multiple reminders on the same date
        val targetDate = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0)
        val date = targetDate.toLocalDate()
        
        val result1 = reminderManager.createReminderWithError(
            targetDate.withHour(9).withMinute(0), "Morning meeting")
        val result2 = reminderManager.createReminderWithError(
            targetDate.withHour(14).withMinute(0), "Afternoon meeting")
        val result3 = reminderManager.createReminderWithError(
            targetDate.withHour(17).withMinute(0), "Evening meeting")
        
        assertTrue("All creations should succeed", 
            result1 is ReminderResult.Success && 
            result2 is ReminderResult.Success && 
            result3 is ReminderResult.Success)
        
        val id1 = (result1 as ReminderResult.Success).reminderId
        val id2 = (result2 as ReminderResult.Success).reminderId
        
        // Select the date
        calendarViewModel.selectDate(date)
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(100)
        
        // Verify initial state
        assertEquals("Should have 3 reminders total", 3, calendarViewModel.allReminders.value.size)
        assertEquals("Should have 3 reminders on selected date", 
            3, calendarViewModel.selectedDateReminders.value.size)
        
        val yearMonth = YearMonth.from(date)
        val eventCountsBefore = calendarViewModel.getRemindersForMonth(yearMonth)
        assertEquals("Should have 3 events on date", 3, eventCountsBefore[date])
        
        // When: Deleting two reminders
        calendarViewModel.deleteReminder(id1)
        kotlinx.coroutines.delay(50)
        calendarViewModel.deleteReminder(id2)
        kotlinx.coroutines.delay(100)
        
        // Then: All views should show consistent state
        assertEquals("Should have 1 reminder total", 1, calendarViewModel.allReminders.value.size)
        assertEquals("Should have 1 reminder on selected date", 
            1, calendarViewModel.selectedDateReminders.value.size)
        
        val eventCountsAfter = calendarViewModel.getRemindersForMonth(yearMonth)
        assertEquals("Should have 1 event on date", 1, eventCountsAfter[date])
    }
    
    @Test
    fun testConcurrentOperations_MaintainConsistency() = runBlocking {
        // Given: Rapid creation and deletion operations (simulating real-world usage)
        val date1 = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)
        val date2 = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0)
        val date3 = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0)
        
        // When: Creating multiple reminders rapidly
        val result1 = reminderManager.createReminderWithError(date1, "Rapid 1")
        val result2 = reminderManager.createReminderWithError(date2, "Rapid 2")
        val result3 = reminderManager.createReminderWithError(date3, "Rapid 3")
        
        assertTrue("All creations should succeed", 
            result1 is ReminderResult.Success && 
            result2 is ReminderResult.Success && 
            result3 is ReminderResult.Success)
        
        val id1 = (result1 as ReminderResult.Success).reminderId
        
        // Wait for Flow to propagate
        kotlinx.coroutines.delay(150)
        
        // Verify all reminders are present
        assertEquals("Should have 3 reminders", 3, calendarViewModel.allReminders.value.size)
        
        // When: Deleting one while creating another
        calendarViewModel.deleteReminder(id1)
        val result4 = reminderManager.createReminderWithError(
            LocalDateTime.now().plusDays(3).withHour(15).withMinute(0), "Rapid 4")
        
        assertTrue("Creation should succeed", result4 is ReminderResult.Success)
        
        // Wait for operations to complete
        kotlinx.coroutines.delay(150)
        
        // Then: Final state should be consistent
        val finalReminders = calendarViewModel.allReminders.value
        assertEquals("Should have 3 reminders (deleted 1, added 1)", 3, finalReminders.size)
        assertFalse("Deleted reminder should not exist", 
            finalReminders.any { it.id == id1 })
        assertTrue("New reminder should exist", 
            finalReminders.any { it.message == "Rapid 4" })
    }
}
