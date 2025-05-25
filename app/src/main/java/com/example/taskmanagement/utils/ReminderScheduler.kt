package com.example.taskmanagement.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.taskmanagement.data.Task
import com.example.taskmanagement.receiver.TaskReminderReceiver
import java.text.SimpleDateFormat
import java.util.Locale

object ReminderScheduler {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    fun scheduleReminder(context: Context, task: Task) {
        task.reminderDate?.let { reminderDate ->
            task.phoneNumber?.let { phoneNumber ->
                if (phoneNumber.isBlank()) return

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                    putExtra("taskId", task.id)
                    putExtra("phoneNumber", phoneNumber)
                    putExtra("message", "Reminder: Task '${task.title}' is due on ${task.dueDate?.let { dateFormat.format(it) }}")
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    task.id.toInt(), // Use task ID as request code to ensure uniqueness
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Schedule the alarm based on available permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    // Use exact alarms if permitted
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderDate.time,
                        pendingIntent
                    )
                } else {
                    // Fall back to inexact alarms
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderDate.time,
                        pendingIntent
                    )
                }
            }
        }
    }

    fun cancelReminder(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
} 