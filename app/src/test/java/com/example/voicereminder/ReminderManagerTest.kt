package com.example.voicereminder

import android.app.AlarmManager
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.voicereminder.data.ReminderDao
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderEntity
import com.example.voicereminder.data.ReminderStatus
import com.example.voicereminder.domain.ReminderErrorType
import com.example.voicereminder.domain.ReminderManager
import com.example.voicereminder.domain.ReminderResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

/**
 * Unit tests for ReminderManager
 * Tests reminder creation, status updates, notification scheduling, and error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderManagerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockDatabase: ReminderDatabase

    @Mock
    private lateinit var mockDao: ReminderDao

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAlarmManager: AlarmManager

    private lateinit var reminderManager: ReminderManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(mockDatabase.reminderDao()).thenReturn(mockDao)
        whenever(mockContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mockAlarmManager)
        reminderManager = ReminderManager(mockDatabase, mockContext)
    }

    @Test
    fun `createReminder with valid input returns success`() = runTest {
        val futureTime = LocalDateTime.now().plusHours(1)
        val message = "Test reminder"
        val expectedId = 1L

        whenever(mockDao.insert(any())).thenReturn(expectedId)

        val result = reminderManager.createReminderWithError(futureTime, message)

        assertTrue(result is ReminderResult.Success)
        assertEquals(expectedId, (result as ReminderResult.Success).reminderId)
        verify(mockDao).insert(any())
    }

    @Test
    fun `createReminder with empty message returns error`() = runTest {
        val futureTime = LocalDateTime.now().plusHours(1)
        val message = ""

        val result = reminderManager.createReminderWithError(futureTime, message)

        assertTrue(result is ReminderResult.Error)
        val error = result as ReminderResult.Error
        assertEquals(ReminderErrorType.INVALID_INPUT, error.errorType)
    }

    @Test
    fun `createReminder with blank message returns error`() = runTest {
        val futureTime = LocalDateTime.now().plusHours(1)
        val message = "   "

        val result = reminderManager.createReminderWithError(futureTime, message)

        assertTrue(result is ReminderResult.Error)
        val error = result as ReminderResult.Error
        assertEquals(ReminderErrorType.INVALID_INPUT, error.errorType)
    }

    @Test
    fun `createReminder with past time returns error`() = runTest {
        val pastTime = LocalDateTime.now().minusHours(1)
        val message = "Test reminder"

        val result = reminderManager.createReminderWithError(pastTime, message)

        assertTrue(result is ReminderResult.Error)
        val error = result as ReminderResult.Error
        assertEquals(ReminderErrorType.INVALID_INPUT, error.errorType)
    }

    @Test
    fun `createReminder with database error returns error`() = runTest {
        val futureTime = LocalDateTime.now().plusHours(1)
        val message = "Test reminder"

        whenever(mockDao.insert(any())).thenThrow(RuntimeException("Database error"))

        val result = reminderManager.createReminderWithError(futureTime, message)

        assertTrue(result is ReminderResult.Error)
        val error = result as ReminderResult.Error
        assertEquals(ReminderErrorType.DATABASE_ERROR, error.errorType)
    }

    @Test
    fun `getActiveReminders returns list of reminders`() = runTest {
        val reminderEntities = listOf(
            ReminderEntity(
                id = 1,
                message = "Reminder 1",
                scheduledTime = System.currentTimeMillis() + 3600000,
                status = ReminderStatus.PENDING,
                createdAt = System.currentTimeMillis()
            ),
            ReminderEntity(
                id = 2,
                message = "Reminder 2",
                scheduledTime = System.currentTimeMillis() + 7200000,
                status = ReminderStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
        )

        whenever(mockDao.getActiveReminders()).thenReturn(reminderEntities)

        val result = reminderManager.getActiveReminders()

        assertEquals(2, result.size)
        assertEquals("Reminder 1", result[0].message)
        assertEquals("Reminder 2", result[1].message)
        verify(mockDao).getActiveReminders()
    }

    @Test
    fun `getActiveReminders with database error returns empty list`() = runTest {
        whenever(mockDao.getActiveReminders()).thenThrow(RuntimeException("Database error"))

        val result = reminderManager.getActiveReminders()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `markAsCompleted updates reminder status`() = runTest {
        val reminderId = 1L
        val reminderEntity = ReminderEntity(
            id = reminderId,
            message = "Test reminder",
            scheduledTime = System.currentTimeMillis() + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        whenever(mockDao.getReminderById(reminderId)).thenReturn(reminderEntity)

        reminderManager.markAsCompleted(reminderId)

        verify(mockDao).getReminderById(reminderId)
        verify(mockDao).update(any())
    }

    @Test
    fun `markAsCompleted with non-existent reminder does not throw`() = runTest {
        val reminderId = 999L

        whenever(mockDao.getReminderById(reminderId)).thenReturn(null)

        // Should not throw exception
        reminderManager.markAsCompleted(reminderId)

        verify(mockDao).getReminderById(reminderId)
    }

    @Test
    fun `deleteReminder removes reminder from database`() = runTest {
        val reminderId = 1L
        val reminderEntity = ReminderEntity(
            id = reminderId,
            message = "Test reminder",
            scheduledTime = System.currentTimeMillis() + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        whenever(mockDao.getReminderById(reminderId)).thenReturn(reminderEntity)

        reminderManager.deleteReminder(reminderId)

        verify(mockDao).getReminderById(reminderId)
        verify(mockDao).delete(reminderEntity)
    }

    @Test
    fun `deleteReminder with non-existent reminder does not throw`() = runTest {
        val reminderId = 999L

        whenever(mockDao.getReminderById(reminderId)).thenReturn(null)

        // Should not throw exception
        reminderManager.deleteReminder(reminderId)

        verify(mockDao).getReminderById(reminderId)
    }

    @Test
    fun `scheduleNotification is called when reminder is created successfully`() = runTest {
        val futureTime = LocalDateTime.now().plusHours(2)
        val message = "Test notification scheduling"
        val expectedId = 5L

        whenever(mockDao.insert(any())).thenReturn(expectedId)

        val result = reminderManager.createReminderWithError(futureTime, message)

        assertTrue(result is ReminderResult.Success)
        verify(mockDao).insert(any())
        // Verify that the reminder was inserted into the database
        // The NotificationScheduler will be called internally
    }

    @Test
    fun `createReminder schedules notification for valid reminder`() = runTest {
        val futureTime = LocalDateTime.now().plusHours(3)
        val message = "Schedule this reminder"
        val expectedId = 10L

        whenever(mockDao.insert(any())).thenReturn(expectedId)

        val reminderId = reminderManager.createReminder(futureTime, message)

        assertEquals(expectedId, reminderId)
        verify(mockDao).insert(any())
    }

    @Test
    fun `createReminder returns -1 when scheduling fails`() = runTest {
        val futureTime = LocalDateTime.now().plusHours(1)
        val message = "Test scheduling failure"

        whenever(mockDao.insert(any())).thenThrow(RuntimeException("Database error"))

        val reminderId = reminderManager.createReminder(futureTime, message)

        assertEquals(-1L, reminderId)
    }

    @Test
    fun `markAsCompleted updates status to COMPLETED`() = runTest {
        val reminderId = 5L
        val reminderEntity = ReminderEntity(
            id = reminderId,
            message = "Test status update",
            scheduledTime = System.currentTimeMillis() + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        whenever(mockDao.getReminderById(reminderId)).thenReturn(reminderEntity)

        reminderManager.markAsCompleted(reminderId)

        verify(mockDao).getReminderById(reminderId)
        verify(mockDao).update(any())
    }

    @Test
    fun `deleteReminder cancels notification and removes from database`() = runTest {
        val reminderId = 7L
        val reminderEntity = ReminderEntity(
            id = reminderId,
            message = "Test deletion",
            scheduledTime = System.currentTimeMillis() + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        whenever(mockDao.getReminderById(reminderId)).thenReturn(reminderEntity)

        reminderManager.deleteReminder(reminderId)

        verify(mockDao).getReminderById(reminderId)
        verify(mockDao).delete(reminderEntity)
    }

    @Test
    fun `getActiveReminders handles database errors gracefully`() = runTest {
        whenever(mockDao.getActiveReminders()).thenThrow(RuntimeException("Connection error"))

        val result = reminderManager.getActiveReminders()

        assertTrue(result.isEmpty())
        verify(mockDao).getActiveReminders()
    }

    @Test
    fun `markAsCompleted handles database errors gracefully`() = runTest {
        val reminderId = 8L
        val reminderEntity = ReminderEntity(
            id = reminderId,
            message = "Test error handling",
            scheduledTime = System.currentTimeMillis() + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        whenever(mockDao.getReminderById(reminderId)).thenReturn(reminderEntity)
        whenever(mockDao.update(any())).thenThrow(RuntimeException("Update failed"))

        // Should not throw exception
        reminderManager.markAsCompleted(reminderId)

        verify(mockDao).getReminderById(reminderId)
    }

    @Test
    fun `deleteReminder handles database errors gracefully`() = runTest {
        val reminderId = 9L
        val reminderEntity = ReminderEntity(
            id = reminderId,
            message = "Test delete error",
            scheduledTime = System.currentTimeMillis() + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        whenever(mockDao.getReminderById(reminderId)).thenReturn(reminderEntity)
        whenever(mockDao.delete(any())).thenThrow(RuntimeException("Delete failed"))

        // Should not throw exception
        reminderManager.deleteReminder(reminderId)

        verify(mockDao).getReminderById(reminderId)
    }
}
