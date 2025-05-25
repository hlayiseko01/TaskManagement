package com.example.taskmanagement

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.taskmanagement.adapter.TaskAdapter
import com.example.taskmanagement.data.Task
import com.example.taskmanagement.databinding.ActivityMainBinding
import com.example.taskmanagement.databinding.DialogAddTaskBinding
import com.example.taskmanagement.utils.ReminderScheduler
import com.example.taskmanagement.viewmodel.TaskViewModel
import com.example.taskmanagement.worker.TaskReminderWorker
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter
    private var mediaPlayer: MediaPlayer? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val SMS_PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Request focus for the dummy view to prevent search bar from getting initial focus
        binding.dummyFocusable.requestFocus()

        checkPermissions()
        setupThemeToggle()
        setupRecyclerView()
        setupListeners()
        observeTasks()
        setupSearch()
        setupReminderWorker()
    }

    private fun checkPermissions() {
        // Check SMS permission
        if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST_CODE)
        }

        // Check and request exact alarm permission for Android 12 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Open system settings to allow exact alarms
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                showSnackbar("SMS permission granted")
            } else {
                showSnackbar("SMS permission denied. Reminders will not be sent via SMS.")
            }
        }
    }

    private fun setupThemeToggle() {
        // Update the icon based on current theme
        updateThemeToggleIcon()

        binding.themeToggleButton.setOnClickListener {
            // Toggle between night and day modes
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }
            AppCompatDelegate.setDefaultNightMode(newMode)
        }
    }

    private fun updateThemeToggleIcon() {
        val isNightMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        binding.themeToggleButton.setIconResource(
            if (isNightMode) R.drawable.ic_light_mode
            else R.drawable.ic_dark_mode
        )
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                viewModel.updateTask(task)
                showSnackbar(if (task.isCompleted) "Task marked as completed" else "Task marked as incomplete")
            },
            onTaskLongClick = { task ->
                viewModel.deleteTask(task)
                showSnackbar("Task deleted")
            }
        )

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = taskAdapter
        }
    }

    private fun setupListeners() {
        binding.addTaskFab.setOnClickListener {
            showAddTaskDialog()
        }

        binding.deleteCompletedFab.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Completed Tasks")
                .setMessage("Are you sure you want to delete all completed tasks?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteCompletedTasks()
                    showSnackbar("Completed tasks deleted")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupSearch() {
        binding.searchEditText.apply {
            addTextChangedListener { text ->
                viewModel.setSearchQuery(text?.toString() ?: "")
            }

            // Clear focus and hide keyboard when done
            setOnEditorActionListener { v, _, _ ->
                v.clearFocus()
                hideKeyboard(v)
                true
            }
        }

        // Clear focus when clicking outside the search bar
        binding.root.setOnClickListener {
            if (binding.searchEditText.hasFocus()) {
                binding.searchEditText.clearFocus()
                hideKeyboard(binding.searchEditText)
            }
        }

        // Clear focus when clicking on the RecyclerView
        binding.tasksRecyclerView.setOnTouchListener { _, _ ->
            if (binding.searchEditText.hasFocus()) {
                binding.searchEditText.clearFocus()
                hideKeyboard(binding.searchEditText)
            }
            false
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun observeTasks() {
        viewModel.filteredTasks.observe(this) { tasks ->
            tasks?.let {
                taskAdapter.submitList(it.toList())
            }
        }
    }

    private fun showAddTaskDialog() {
        val dialogBinding = DialogAddTaskBinding.inflate(LayoutInflater.from(this))
        var dueDate: Date? = null
        var reminderDate: Date? = null

        dialogBinding.dueDateEditText.setOnClickListener {
            showDateTimePicker { selectedDate ->
                dueDate = selectedDate
                dialogBinding.dueDateEditText.setText(dateFormat.format(selectedDate))
            }
        }

        dialogBinding.reminderDateEditText.setOnClickListener {
            showDateTimePicker { selectedDate ->
                reminderDate = selectedDate
                dialogBinding.reminderDateEditText.setText(dateFormat.format(selectedDate))
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Add New Task")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val title = dialogBinding.taskTitleEditText.text.toString()
                val description = dialogBinding.taskDescriptionEditText.text.toString()
                val phoneNumber = dialogBinding.phoneNumberEditText.text.toString()
                val email = dialogBinding.emailEditText.text.toString()

                if (title.isNotBlank()) {
                    val task = Task(
                        title = title,
                        description = description,
                        dueDate = dueDate,
                        reminderDate = reminderDate,
                        phoneNumber = phoneNumber.takeIf { it.isNotBlank() },
                        email = email.takeIf { it.isNotBlank() }
                    )
                    viewModel.insertTask(task)

                    // Schedule SMS reminder if phone number and reminder date are provided
                    if (!phoneNumber.isBlank() && reminderDate != null) {
                        ReminderScheduler.scheduleReminder(this, task)
                    }

                    playNotificationSound()
                    showSnackbar("Task added successfully")
                } else {
                    showSnackbar("Please enter a task title")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDateTimePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()

        // Show Date Picker first
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                // After date is selected, show Time Picker
                android.app.TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onDateSelected(calendar.time)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true // 24-hour format
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupReminderWorker() {
        val reminderWorkRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(
            15, TimeUnit.MINUTES,  // Minimum interval allowed by Android
            5, TimeUnit.MINUTES    // Flex interval for battery optimization
        ).build()

        // Replace any existing work with the same name
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                "TaskReminders",
                androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                reminderWorkRequest
            )
    }

    private fun playNotificationSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.notification_sound)
        mediaPlayer?.start()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updateThemeToggleIcon()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}