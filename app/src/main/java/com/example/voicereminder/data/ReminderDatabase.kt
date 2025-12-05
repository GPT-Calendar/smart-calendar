package com.example.voicereminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.voicereminder.data.dao.AlarmDao
import com.example.voicereminder.data.dao.ChatMessageDao
import com.example.voicereminder.data.dao.TaskDao
import com.example.voicereminder.data.entity.AlarmEntity
import com.example.voicereminder.data.entity.ChatMessageEntity
import com.example.voicereminder.data.entity.TaskEntity

/**
 * Room database singleton for reminder, task, and alarm storage
 */
@Database(
    entities = [
        ReminderEntity::class, 
        SavedLocationEntity::class,
        TaskEntity::class,
        AlarmEntity::class,
        ChatMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReminderDatabase : RoomDatabase() {

    /**
     * Get the DAO for reminder operations
     */
    abstract fun reminderDao(): ReminderDao

    /**
     * Get the DAO for saved location operations
     */
    abstract fun savedLocationDao(): SavedLocationDao
    
    /**
     * Get the DAO for task operations
     */
    abstract fun taskDao(): TaskDao
    
    /**
     * Get the DAO for alarm operations
     */
    abstract fun alarmDao(): AlarmDao
    
    /**
     * Get the DAO for chat message operations
     */
    abstract fun chatMessageDao(): ChatMessageDao
    
    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null
        
        /**
         * Migration from version 1 to version 2
         * Adds location-based reminder support
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to reminders table
                database.execSQL("ALTER TABLE reminders ADD COLUMN reminderType TEXT NOT NULL DEFAULT 'TIME_BASED'")
                database.execSQL("ALTER TABLE reminders ADD COLUMN locationData TEXT")
                database.execSQL("ALTER TABLE reminders ADD COLUMN geofenceId TEXT")
                
                // Create saved_locations table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS saved_locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        radius REAL NOT NULL DEFAULT 100.0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        /**
         * Migration from version 2 to version 3
         * Adds tasks and alarms tables, enhances reminders with priority/category
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to reminders table for enhanced features
                database.execSQL("ALTER TABLE reminders ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'")
                database.execSQL("ALTER TABLE reminders ADD COLUMN category TEXT NOT NULL DEFAULT 'PERSONAL'")
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceRule TEXT")
                database.execSQL("ALTER TABLE reminders ADD COLUMN snoozeCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE reminders ADD COLUMN originalScheduledTime INTEGER")
                database.execSQL("ALTER TABLE reminders ADD COLUMN notes TEXT")
                
                // Create tasks table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        dueDate INTEGER,
                        priority TEXT NOT NULL DEFAULT 'MEDIUM',
                        category TEXT NOT NULL DEFAULT 'PERSONAL',
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        recurrenceRule TEXT,
                        parentTaskId INTEGER,
                        notes TEXT,
                        tags TEXT
                    )
                """.trimIndent())
                
                // Create alarms table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS alarms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        label TEXT NOT NULL DEFAULT 'Alarm',
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        repeatDays TEXT NOT NULL DEFAULT '',
                        soundUri TEXT,
                        vibrate INTEGER NOT NULL DEFAULT 1,
                        snoozeCount INTEGER NOT NULL DEFAULT 0,
                        snoozeDurationMinutes INTEGER NOT NULL DEFAULT 5,
                        lastTriggered INTEGER,
                        nextTrigger INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create indexes for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_dueDate ON tasks(dueDate)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_isCompleted ON tasks(isCompleted)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_category ON tasks(category)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_alarms_isEnabled ON alarms(isEnabled)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_alarms_nextTrigger ON alarms(nextTrigger)")
            }
        }
        
        /**
         * Migration from version 3 to version 4
         * Adds chat_messages table for conversation history persistence
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create chat_messages table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        text TEXT NOT NULL,
                        isUser INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        messageType TEXT NOT NULL,
                        status TEXT NOT NULL,
                        richContentJson TEXT,
                        sessionId TEXT
                    )
                """.trimIndent())
            }
        }
        
        /**
         * Migration from version 4 to version 5
         * Fixes chat_messages table schema (removes DEFAULT values that cause Room mismatch)
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop the old table with incorrect schema
                database.execSQL("DROP TABLE IF EXISTS chat_messages")
                
                // Drop old indexes
                database.execSQL("DROP INDEX IF EXISTS index_chat_messages_timestamp")
                database.execSQL("DROP INDEX IF EXISTS index_chat_messages_sessionId")
                database.execSQL("DROP INDEX IF EXISTS index_chat_messages_status")
                
                // Recreate chat_messages table with correct schema (no SQL DEFAULT values)
                database.execSQL("""
                    CREATE TABLE chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        text TEXT NOT NULL,
                        isUser INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        messageType TEXT NOT NULL,
                        status TEXT NOT NULL,
                        richContentJson TEXT,
                        sessionId TEXT
                    )
                """.trimIndent())
            }
        }
        
        /**
         * Get the singleton database instance
         * @param context Application context
         * @return The database instance
         */
        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Get an in-memory database instance for testing
         * @param context Application context
         * @return A test database instance
         */
        fun getTestDatabase(context: Context): ReminderDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ReminderDatabase::class.java
            ).allowMainThreadQueries()
                .build()
        }
    }
}
