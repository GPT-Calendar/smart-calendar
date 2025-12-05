package com.example.voicereminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.voicereminder.data.entity.FinanceTransaction
import com.example.voicereminder.data.entity.SmsSource
import com.example.voicereminder.data.entity.BudgetEntity
import com.example.voicereminder.data.entity.SavingsGoalEntity
import com.example.voicereminder.data.dao.FinanceTransactionDao
import com.example.voicereminder.data.dao.SmsSourceDao
import com.example.voicereminder.data.dao.BudgetDao
import com.example.voicereminder.data.dao.SavingsGoalDao

/**
 * Room database for finance tracking (separate from reminder database to avoid conflicts)
 */
@Database(
    entities = [
        FinanceTransaction::class, 
        SmsSource::class,
        BudgetEntity::class,
        SavingsGoalEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(FinanceConverters::class)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun financeTransactionDao(): FinanceTransactionDao
    abstract fun smsSourceDao(): SmsSourceDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    
    // Keep old method name for backward compatibility
    @Deprecated("Use smsSourceDao() instead")
    fun bankSmsPatternDao(): SmsSourceDao = smsSourceDao()

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        /**
         * Migration from version 2 to version 3
         * Renames bank_sms_patterns to sms_sources and removes regexPattern column
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new sms_sources table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sms_sources (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        description TEXT
                    )
                """.trimIndent())
                
                // Migrate data from old table (if exists)
                database.execSQL("""
                    INSERT INTO sms_sources (id, name, phoneNumber, isActive, description)
                    SELECT id, bankName, phoneNumber, isActive, NULL
                    FROM bank_sms_patterns
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE IF EXISTS bank_sms_patterns")
                
                // Create index for phone number lookup
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sms_sources_phoneNumber ON sms_sources(phoneNumber)")
            }
        }
        
        /**
         * Migration from version 1 to version 2
         * Adds budget and savings goal tables, enhances transactions with manual entry support
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to finance_transactions table
                database.execSQL("ALTER TABLE finance_transactions ADD COLUMN isManual INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE finance_transactions ADD COLUMN isEdited INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE finance_transactions ADD COLUMN notes TEXT")
                database.execSQL("ALTER TABLE finance_transactions ADD COLUMN originalAmount REAL")
                
                // Create budgets table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        category TEXT NOT NULL,
                        monthlyLimit REAL NOT NULL,
                        month TEXT NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create savings_goals table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS savings_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        targetAmount REAL NOT NULL,
                        currentAmount REAL NOT NULL DEFAULT 0.0,
                        deadline INTEGER,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        iconName TEXT NOT NULL DEFAULT 'savings',
                        color TEXT NOT NULL DEFAULT '#4CAF50',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create indexes for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_month ON budgets(month)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_category ON budgets(category)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_savings_goals_isCompleted ON savings_goals(isCompleted)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_finance_transactions_isManual ON finance_transactions(isManual)")
            }
        }

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}