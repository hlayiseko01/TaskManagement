package com.example.taskmanagement.adapter

import android.graphics.drawable.AnimatedVectorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagement.R
import com.example.taskmanagement.data.Task
import com.example.taskmanagement.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskLongClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        private var isUpdating = false

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && !isUpdating) {
                    val task = getItem(position)
                    task.isCompleted = !task.isCompleted
                    onTaskClick(task)
                    updateTaskStatus(task, true)
                }
            }
            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTaskLongClick(getItem(position))
                }
                true
            }
            binding.taskCheckbox.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && !isUpdating) {
                    val task = getItem(position)
                    task.isCompleted = binding.taskCheckbox.isChecked
                    onTaskClick(task)
                    updateTaskStatus(task, true)
                }
            }
        }

        fun bind(task: Task) {
            binding.apply {
                taskTitleText.text = task.title
                taskDescriptionText.text = task.description
                taskCheckbox.isChecked = task.isCompleted

                // Format and display the due date
                task.dueDate?.let { dueDate ->
                    val now = Date()
                    val dueDateText = when {
                        dueDate.before(now) -> "Overdue - Due: ${dateFormat.format(dueDate)}"
                        dueDate.time - now.time < 24 * 60 * 60 * 1000 -> "Due Today at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(dueDate)}"
                        else -> "Due: ${dateFormat.format(dueDate)}"
                    }
                    taskDueDateText.text = dueDateText
                    
                    // Set text color based on due date
                    val context = taskDueDateText.context
                    taskDueDateText.setTextColor(
                        when {
                            dueDate.before(now) -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
                            dueDate.time - now.time < 24 * 60 * 60 * 1000 -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                            else -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
                        }
                    )
                    taskDueDateText.visibility = ViewGroup.VISIBLE
                } ?: run {
                    taskDueDateText.visibility = ViewGroup.GONE
                }

                updateTaskStatus(task)
            }
        }

        private fun updateTaskStatus(task: Task, showLoading: Boolean = false) {
            binding.apply {
                if (showLoading) {
                    isUpdating = true
                    // Show loading animation
                    taskStatusImage.setImageResource(R.drawable.ic_task_loading)
                    (taskStatusImage.drawable as? AnimatedVectorDrawable)?.start()
                    
                    // Delay the final status update to show the loading animation
                    taskStatusImage.postDelayed({
                        updateFinalStatus(task)
                        isUpdating = false
                    }, 500) // Show loading for 500ms
                } else {
                    updateFinalStatus(task)
                }
            }
        }

        private fun updateFinalStatus(task: Task) {
            binding.taskStatusImage.setImageResource(
                if (task.isCompleted) R.drawable.ic_task_complete
                else R.drawable.ic_task_incomplete
            )
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
} 