package com.example.voicereminder.data

import androidx.room.TypeConverter
import com.example.voicereminder.data.entity.TransactionType
import java.util.Date

/**
 * Type converters for Room database to handle custom types
 */
class Converters {

    @TypeConverter
    fun fromReminderStatus(status: ReminderStatus): String {
        return status.name
    }

    @TypeConverter
    fun toReminderStatus(value: String): ReminderStatus {
        return ReminderStatus.valueOf(value)
    }

    @TypeConverter
    fun fromReminderType(type: ReminderType): String {
        return type.name
    }

    @TypeConverter
    fun toReminderType(value: String): ReminderType {
        return ReminderType.valueOf(value)
    }

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toTimestamp(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
}
