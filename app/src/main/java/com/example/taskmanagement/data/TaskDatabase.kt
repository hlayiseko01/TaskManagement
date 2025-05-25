package com.example.taskmanagement.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.taskmanagement.utils.DateConverter

@Database(entities = [Task::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to the tasks table
                database.execSQL("""
                    ALTER TABLE tasks ADD COLUMN reminderDate INTEGER
                """)
                database.execSQL("""
                    ALTER TABLE tasks ADD COLUMN phoneNumber TEXT
                """)
                database.execSQL("""
                    ALTER TABLE tasks ADD COLUMN email TEXT
                """)
                database.execSQL("""
                    ALTER TABLE tasks ADD COLUMN isReminderSent INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    ALTER TABLE tasks ADD COLUMN isOverdueNotificationSent INTEGER NOT NULL DEFAULT 0
                """)
            }
        }

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                .addMigrations(MIGRATION_1_2) // Add migration strategy
                .fallbackToDestructiveMigration() // Optional: if migration fails, recreate tables
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 