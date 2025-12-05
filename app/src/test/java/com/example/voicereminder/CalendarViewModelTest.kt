package com.example.voicereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.voicereminder.data.ReminderStatus
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.domain.models.Reminder
import com.example.voicereminder.presentation.calendar.CalendarViewModel
import com.example.voicereminder.presentation.calendar.ReminderFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.threeten.bp.LocalDate
import java.time.LocalDateTime
import org.threeten.bp.YearMonth

/**
 * Unit tests for CalendarViewModel
 * Tests reminder grouping, date selection, filtering, and deletion
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockReminderManager: ReminderManager

    private lateinit var viewModel: CalendarViewModel

    // Test data
    private val testDate1 = org.threeten.bp.LocalDate.of(2025, 11, 15)
    private val testDate2 = org.threeten.bp.LocalDate.of(2025, 11, 16)
    private val testDate3 = org.threeten.bp.LocalDate.of(2025, 11, 17)

    private val reminder1 = Reminder(
        id = 1L,
        message = "Test reminder 1",
        scheduledTime = java.time.LocalDate.of(2025, 11, 15).atTime(10, 0),
        status = ReminderStatus.PENDING,
        createdAt = java.time.LocalDateTime.now()
    )

    private val reminder2 = Reminder(
        id = 2L,
        message = "Test reminder 2",
        scheduledTime = java.time.LocalDate.of(2025, 11, 15).atTime(14, 30),
        status = ReminderStatus.PENDING,
        createdAt = java.time.LocalDateTime.now()
    )

    private val reminder3 = Reminder(
        id = 3L,
        message = "Test reminder 3",
        scheduledTime = java.time.LocalDate.of(2025, 11, 16).atTime(9, 0),
        status = ReminderStatus.COMPLETED,
        createdAt = java.time.LocalDateTime.now()
    )

    private val reminder4 = Reminder(
        id = 4L,
        message = "Test reminder 4",
        scheduledTime = java.time.LocalDate.of(2025, 11, 17).atTime(16, 0),
        status = ReminderStatus.PENDING,
        createdAt = java.time.LocalDateTime.now()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup default mock behavior - return empty flow
        whenever(mockReminderManager.getAllRemindersFlow()).thenReturn(flowOf(emptyList()))
        
        viewModel = CalendarViewModel(mockReminderManager)
    }

    // Test reminder grouping by date logic
    @Test
    fun `groupRemindersByDate groups reminders correctly by date`() {
        val reminders = listOf(reminder1, reminder2, reminder3, reminder4)
        
        val grouped = viewModel.groupRemindersByDate(reminders)
        
        assertEquals(3, grouped.size)
        assertEquals(2, grouped[testDate1]?.size)
        assertEquals(1, grouped[testDate2]?.size)
        assertEquals(1, grouped[testDate3]?.size)
    }

    @Test
    fun `groupRemindersByDate handles empty list`() {
        val grouped = viewModel.groupRemindersByDate(emptyList())
        
        assertTrue(grouped.isEmpty())
    }

    @Test
    fun `groupRemindersByDate groups multiple reminders on same date`() {
        val reminders = listOf(reminder1, reminder2)
        
        val grouped = viewModel.groupRemindersByDate(reminders)
        
        assertEquals(1, grouped.size)
        assertEquals(2, grouped[testDate1]?.size)
        assertTrue(grouped[testDate1]?.contains(reminder1) == true)
        assertTrue(grouped[testDate1]?.contains(reminder2) == true)
    }

    // Test date selection updates selectedDateReminders
    @Test
    fun `selectDate updates selected date state`() = runTest {
        // Setup mock to return reminders
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1, reminder2, reminder3)))
        
        // Recreate viewModel to pick up the new mock behavior
        viewModel = CalendarViewModel(mockReminderManager)
        
        // Give time for flow to emit
        kotlinx.coroutines.delay(100)
        
        viewModel.selectDate(testDate1)
        
        assertEquals(testDate1, viewModel.selectedDate.value)
    }

    @Test
    fun `selectDate filters reminders for selected date`() = runTest {
        // Setup mock to return reminders
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1, reminder2, reminder3, reminder4)))
        
        // Recreate viewModel to pick up the new mock behavior
        viewModel = CalendarViewModel(mockReminderManager)
        
        // Give time for flow to emit
        kotlinx.coroutines.delay(100)
        
        viewModel.selectDate(testDate1)
        
        // Give time for state to update
        kotlinx.coroutines.delay(100)
        
        val selectedReminders = viewModel.selectedDateReminders.value
        assertEquals(2, selectedReminders.size)
        assertTrue(selectedReminders.contains(reminder1))
        assertTrue(selectedReminders.contains(reminder2))
    }

    @Test
    fun `selectDate returns empty list for date with no reminders`() = runTest {
        // Setup mock to return reminders
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1, reminder2)))
        
        // Recreate viewModel to pick up the new mock behavior
        viewModel = CalendarViewModel(mockReminderManager)
        
        // Give time for flow to emit
        kotlinx.coroutines.delay(100)
        
        val emptyDate = org.threeten.bp.LocalDate.of(2025, 12, 1)
        viewModel.selectDate(emptyDate)
        
        // Give time for state to update
        kotlinx.coroutines.delay(100)
        
        assertTrue(viewModel.selectedDateReminders.value.isEmpty())
    }

    @Test
    fun `selectDate sorts reminders by scheduled time`() = runTest {
        // Create reminders in non-chronological order
        val laterReminder = reminder2 // 14:30
        val earlierReminder = reminder1 // 10:00
        
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(laterReminder, earlierReminder)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        viewModel.selectDate(testDate1)
        kotlinx.coroutines.delay(100)
        
        val selectedReminders = viewModel.selectedDateReminders.value
        assertEquals(2, selectedReminders.size)
        assertEquals(reminder1.id, selectedReminders[0].id) // Earlier time first
        assertEquals(reminder2.id, selectedReminders[1].id)
    }

    // Test filter application (ALL, PENDING, COMPLETED)
    @Test
    fun `applyFilter with ALL shows all reminders`() = runTest {
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1, reminder2, reminder3, reminder4)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        viewModel.applyFilter(ReminderFilter.ALL)
        kotlinx.coroutines.delay(100)
        
        assertEquals(4, viewModel.allReminders.value.size)
    }

    @Test
    fun `applyFilter with PENDING shows only pending reminders`() = runTest {
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1, reminder2, reminder3, reminder4)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        viewModel.applyFilter(ReminderFilter.PENDING)
        kotlinx.coroutines.delay(100)
        
        val filteredReminders = viewModel.allReminders.value
        assertEquals(3, filteredReminders.size)
        assertTrue(filteredReminders.all { it.status == ReminderStatus.PENDING })
    }

    @Test
    fun `applyFilter with COMPLETED shows only completed reminders`() = runTest {
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1, reminder2, reminder3, reminder4)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        viewModel.applyFilter(ReminderFilter.COMPLETED)
        kotlinx.coroutines.delay(100)
        
        val filteredReminders = viewModel.allReminders.value
        assertEquals(1, filteredReminders.size)
        assertTrue(filteredReminders.all { it.status == ReminderStatus.COMPLETED })
    }

    @Test
    fun `applyFilter updates current filter state`() = runTest {
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        viewModel.applyFilter(ReminderFilter.PENDING)
        
        assertEquals(ReminderFilter.PENDING, viewModel.currentFilter.value)
    }

    // Test deleteReminder calls ReminderManager correctly
    @Test
    fun `deleteReminder calls ReminderManager deleteReminder`() = runTest {
        val reminderId = 1L
        
        val result = viewModel.deleteReminder(reminderId)
        
        verify(mockReminderManager).deleteReminder(reminderId)
        assertTrue(result)
    }

    @Test
    fun `deleteReminder returns true on success`() = runTest {
        val reminderId = 1L
        
        val result = viewModel.deleteReminder(reminderId)
        
        assertTrue(result)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `deleteReminder handles errors gracefully`() = runTest {
        val reminderId = 1L
        
        whenever(mockReminderManager.deleteReminder(reminderId))
            .thenThrow(RuntimeException("Delete failed"))
        
        val result = viewModel.deleteReminder(reminderId)
        
        assertFalse(result)
        assertNotNull(viewModel.errorMessage.value)
    }

    // Test getRemindersForMonth
    @Test
    fun `getRemindersForMonth returns correct event counts`() = runTest {
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1, reminder2, reminder3, reminder4)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        val yearMonth = org.threeten.bp.YearMonth.of(2025, 11)
        val eventCounts = viewModel.getRemindersForMonth(yearMonth)
        
        assertEquals(2, eventCounts[testDate1])
        assertEquals(1, eventCounts[testDate2])
        assertEquals(1, eventCounts[testDate3])
    }

    @Test
    fun `getRemindersForMonth filters by month correctly`() = runTest {
        val decemberReminder = Reminder(
            id = 5L,
            message = "December reminder",
            scheduledTime = java.time.LocalDate.of(2025, 12, 1).atTime(10, 0),
            status = ReminderStatus.PENDING,
            createdAt = LocalDateTime.now()
        )
        
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1, reminder2, decemberReminder)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        val novemberCounts = viewModel.getRemindersForMonth(org.threeten.bp.YearMonth.of(2025, 11))
        
        assertEquals(2, novemberCounts.size)
        assertFalse(novemberCounts.containsKey(org.threeten.bp.LocalDate.of(2025, 12, 1)))
    }

    @Test
    fun `getRemindersForMonth returns empty map for month with no reminders`() = runTest {
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        val eventCounts = viewModel.getRemindersForMonth(org.threeten.bp.YearMonth.of(2025, 12))
        
        assertTrue(eventCounts.isEmpty())
    }

    // Test getRemindersForDate
    @Test
    fun `getRemindersForDate returns reminders for specific date`() = runTest {
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1, reminder2, reminder3)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        val reminders = viewModel.getRemindersForDate(testDate1)
        
        assertEquals(2, reminders.size)
        assertTrue(reminders.contains(reminder1))
        assertTrue(reminders.contains(reminder2))
    }

    @Test
    fun `getRemindersForDate sorts by scheduled time`() = runTest {
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder2, reminder1)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        val reminders = viewModel.getRemindersForDate(testDate1)
        
        assertEquals(reminder1.id, reminders[0].id) // Earlier time first
        assertEquals(reminder2.id, reminders[1].id)
    }

    // Test hasReminders
    @Test
    fun `hasReminders returns true when reminders exist`() = runTest {
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(listOf(reminder1)))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        assertTrue(viewModel.hasReminders())
    }

    @Test
    fun `hasReminders returns false when no reminders exist`() = runTest {
        whenever(mockReminderManager.getAllRemindersFlow())
            .thenReturn(flowOf(emptyList()))
        
        viewModel = CalendarViewModel(mockReminderManager)
        kotlinx.coroutines.delay(100)
        
        assertFalse(viewModel.hasReminders())
    }

    // Test error handling
    @Test
    fun `clearError clears error message`() = runTest {
        whenever(mockReminderManager.deleteReminder(1L))
            .thenThrow(RuntimeException("Error"))
        
        viewModel.deleteReminder(1L)
        assertNotNull(viewModel.errorMessage.value)
        
        viewModel.clearError()
        assertNull(viewModel.errorMessage.value)
    }
}
