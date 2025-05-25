package com.example.taskmanagement.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    var isCompleted: Boolean = false,
    val createdDate: Date = Date(),
    var dueDate: Date? = null,
    var reminderDate: Date? = null,
    var phoneNumber: String? = null,
    var email: String? = null,
    var isReminderSent: Boolean = false,
    var isOverdueNotificationSent: Boolean = false
) 