package com.example.taskmanagement.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.taskmanagement.data.TaskDatabase
import com.example.taskmanagement.utils.NotificationHelper
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val taskDao = TaskDatabase.getDatabase(applicationContext).taskDao()
            val currentTime = Date()

            // Get all tasks directly (not using LiveData)
            val tasks = taskDao.getAllTasksDirect()

            tasks.forEach { task ->
                task.reminderDate?.let { reminderDate ->
                    if (!task.isReminderSent && currentTime.after(reminderDate)) {
                        task.phoneNumber?.let { phoneNumber ->
                            if (phoneNumber.isNotBlank()) {
                                NotificationHelper.sendSMS(
                                    applicationContext,
                                    phoneNumber,
                                    "Reminder: Task '${task.title}' is due on ${task.dueDate}"
                                )
                            }
                        }
                        task.isReminderSent = true
                        taskDao.updateTask(task)
                    }
                }

                // Check for overdue tasks
                task.dueDate?.let { dueDate ->
                    if (!task.isOverdueNotificationSent && currentTime.after(dueDate) && !task.isCompleted) {
                        task.email?.let { email ->
                            if (email.isNotBlank()) {
                                NotificationHelper.sendEmail(
                                    applicationContext,
                                    email,
                                    "Task Overdue: ${task.title}",
                                    "The task '${task.title}' is overdue. It was due on ${task.dueDate}"
                                )
                            }
                        }
                        task.isOverdueNotificationSent = true
                        taskDao.updateTask(task)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
} 