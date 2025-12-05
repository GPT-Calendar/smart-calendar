package com.example.voicereminder.data

import androidx.room.TypeConverter
import com.example.voicereminder.data.entity.TransactionType
import java.util.Date

/**
 * Type converters for Finance Database to handle custom types
 */
class FinanceConverters {

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