package com.example.voicereminder

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicereminder.data.ReminderDao
import com.example.voicereminder.data.ReminderDatabase
import com.example.voicereminder.data.ReminderEntity
import com.example.voicereminder.data.ReminderStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ReminderDao
 * Tests CRUD operations and query correctness using in-memory database
 */
@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {

    private lateinit var database: ReminderDatabase
    private lateinit var reminderDao: ReminderDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ReminderDatabase::class.java
        ).build()
        reminderDao = database.reminderDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertReminder_returnsValidId() = runBlocking {
        val reminder = ReminderEntity(
            message = "Test reminder",
            scheduledTime = System.currentTimeMillis() + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        val id = reminderDao.insert(reminder)

        assertTrue(id > 0)
    }

    @Test
    fun insertAndGetReminder_returnsCorrectData() = runBlocking {
        val reminder = ReminderEntity(
            message = "Test reminder",
            scheduledTime = System.currentTimeMillis() + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        val id = reminderDao.insert(reminder)
        val retrieved = reminderDao.getReminderById(id)

        assertNotNull(retrieved)
        assertEquals(id, retrieved!!.id)
        assertEquals("Test reminder", retrieved.message)
        assertEquals(ReminderStatus.PENDING, retrieved.status)
    }

    @Test
    fun getActiveReminders_returnsOnlyPendingReminders() = runBlocking {
        val currentTime = System.currentTimeMillis()
        
        val reminder1 = ReminderEntity(
            message = "Pending reminder 1",
            scheduledTime = currentTime + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = currentTime
        )
        val reminder2 = ReminderEntity(
            message = "Completed reminder",
            scheduledTime = currentTime + 7200000,
            status = ReminderStatus.COMPLETED,
            createdAt = currentTime
        )
        val reminder3 = ReminderEntity(
            message = "Pending reminder 2",
            scheduledTime = currentTime + 10800000,
            status = ReminderStatus.PENDING,
            createdAt = currentTime
        )

        reminderDao.insert(reminder1)
        reminderDao.insert(reminder2)
        reminderDao.insert(reminder3)

        val activeReminders = reminderDao.getActiveReminders()

        assertEquals(2, activeReminders.size)
        assertTrue(activeReminders.all { it.status == ReminderStatus.PENDING })
    }

    @Test
    fun getActiveReminders_orderedByScheduledTime() = runBlocking {
        val currentTime = System.currentTimeMillis()
        
        val reminder1 = ReminderEntity(
            message = "Later reminder",
            scheduledTime = currentTime + 10800000,
            status = ReminderStatus.PENDING,
            createdAt = currentTime
        )
        val reminder2 = ReminderEntity(
            message = "Earlier reminder",
            scheduledTime = currentTime + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = currentTime
        )
        val reminder3 = ReminderEntity(
            message = "Middle reminder",
            scheduledTime = currentTime + 7200000,
            status = ReminderStatus.PENDING,
            createdAt = currentTime
        )

        reminderDao.insert(reminder1)
        reminderDao.insert(reminder2)
        reminderDao.insert(reminder3)

        val activeReminders = reminderDao.getActiveReminders()

        assertEquals(3, activeReminders.size)
        assertEquals("Earlier reminder", activeReminders[0].message)
        assertEquals("Middle reminder", activeReminders[1].message)
        assertEquals("Later reminder", activeReminders[2].message)
    }

    @Test
    fun updateReminder_changesStatus() = runBlocking {
        val reminder = ReminderEntity(
            message = "Test reminder",
            scheduledTime = System.currentTimeMillis() + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        val id = reminderDao.insert(reminder)
        val retrieved = reminderDao.getReminderById(id)!!
        val updated = retrieved.copy(status = ReminderStatus.COMPLETED)

        reminderDao.update(updated)

        val afterUpdate = reminderDao.getReminderById(id)
        assertEquals(ReminderStatus.COMPLETED, afterUpdate!!.status)
    }

    @Test
    fun deleteReminder_removesFromDatabase() = runBlocking {
        val reminder = ReminderEntity(
            message = "Test reminder",
            scheduledTime = System.currentTimeMillis() + 3600000,
            status = ReminderStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        val id = reminderDao.insert(reminder)
        val retrieved = reminderDao.getReminderById(id)!!

        reminderDao.delete(retrieved)

        val afterDelete = reminderDao.getReminderById(id)
        assertNull(afterDelete)
    }

    @Test
    fun getReminderById_withNonExistentId_returnsNull() = runBlocking {
        val result = reminderDao.getReminderById(999L)
        assertNull(result)
    }

    @Test
    fun getActiveReminders_withEmptyDatabase_returnsEmptyList() = runBlocking {
        val result = reminderDao.getActiveReminders()
        assertTrue(result.isEmpty())
    }

    @Test
    fun insertMultipleReminders_persistsAllData() = runBlocking {
        val currentTime = System.currentTimeMillis()
        val reminders = listOf(
            ReminderEntity(
                message = "Reminder 1",
                scheduledTime = currentTime + 3600000,
                status = ReminderStatus.PENDING,
                createdAt = currentTime
            ),
            ReminderEntity(
                message = "Reminder 2",
                scheduledTime = currentTime + 7200000,
                status = ReminderStatus.PENDING,
                createdAt = currentTime
            ),
            ReminderEntity(
                message = "Reminder 3",
                scheduledTime = currentTime + 10800000,
                status = ReminderStatus.PENDING,
                createdAt = currentTime
            )
        )

        reminders.forEach { reminderDao.insert(it) }

        val activeReminders = reminderDao.getActiveReminders()
        assertEquals(3, activeReminders.size)
    }
}
