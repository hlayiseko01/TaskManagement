package com.example.taskmanagement.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.taskmanagement.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.taskmanagement.data.TaskDatabase

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1)
        val phoneNumber = intent.getStringExtra("phoneNumber") ?: return
        val message = intent.getStringExtra("message") ?: return

        // Send SMS immediately
        NotificationHelper.sendSMS(context, phoneNumber, message)

        // Mark the reminder as sent in the database
        CoroutineScope(Dispatchers.IO).launch {
            val taskDao = TaskDatabase.getDatabase(context).taskDao()
            taskDao.getTaskById(taskId)?.let { task ->
                task.isReminderSent = true
                taskDao.updateTask(task)
            }
        }
    }
} 