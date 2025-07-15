package com.stingsoftware.pasika.todo

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.databinding.ItemTodoBinding
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(
    private val onTaskClicked: (Task) -> Unit,
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onLongClick: (Task) -> Unit,
    private val onSelectionChange: (Int) -> Unit
) : ListAdapter<Task, TodoAdapter.TodoViewHolder>(DiffCallback()) {

    private val selectedItems = mutableSetOf<Long>()
    private var isMultiSelectMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class TodoViewHolder(private val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = getItem(position)
                    if (isMultiSelectMode) {
                        toggleSelection(task)
                    } else {
                        onTaskClicked(task)
                    }
                }
            }
            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLongClick(getItem(position))
                }
                true
            }
            binding.checkboxCompleted.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = getItem(position)
                    if (isMultiSelectMode) {
                        toggleSelection(task)
                    } else {
                        onTaskChecked(task, binding.checkboxCompleted.isChecked)
                    }
                }
            }
        }

        fun bind(task: Task) {
            binding.apply {
                checkboxCompleted.isChecked = if (isMultiSelectMode) selectedItems.contains(task.id) else task.isCompleted
                textViewTaskTitle.text = task.title
                textViewTaskTitle.paintFlags = if (task.isCompleted && !isMultiSelectMode) {
                    textViewTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    textViewTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

                textViewTaskDescription.visibility = if (task.description.isNullOrBlank()) View.GONE else View.VISIBLE
                textViewTaskDescription.text = task.description

                if (task.dueDate != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    textViewDueDate.text = itemView.context.getString(R.string.hint_due_date_time_optional, sdf.format(Date(task.dueDate)))
                    textViewDueDate.visibility = View.VISIBLE
                } else {
                    textViewDueDate.visibility = View.GONE
                }

                val cardColor = if (selectedItems.contains(task.id)) {
                    ContextCompat.getColor(itemView.context, R.color.colorHiveSelectedBackground)
                } else if (task.isCompleted) {
                    ContextCompat.getColor(itemView.context, R.color.colorTaskCompletedBackground)
                } else {
                    ContextCompat.getColor(itemView.context, R.color.colorSurface)
                }
                (itemView as MaterialCardView).setCardBackgroundColor(cardColor)
            }
        }
    }

    fun setMultiSelectMode(enabled: Boolean) {
        if (isMultiSelectMode != enabled) {
            isMultiSelectMode = enabled
            if (!enabled) {
                selectedItems.clear()
                onSelectionChange(0)
            }
            // Redraw the entire list to show/hide checkboxes and update selection states
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun toggleSelection(task: Task) {
        if (selectedItems.contains(task.id)) {
            selectedItems.remove(task.id)
        } else {
            selectedItems.add(task.id)
        }
        notifyItemChanged(currentList.indexOf(task))
        onSelectionChange(selectedItems.size)
    }

    fun selectAll() {
        if (selectedItems.size == currentList.size) {
            selectedItems.clear()
        } else {
            selectedItems.clear()
            currentList.forEach { selectedItems.add(it.id) }
        }
        // Redraw all items to reflect the new selection state
        notifyItemRangeChanged(0, itemCount)
        onSelectionChange(selectedItems.size)
    }

    fun getSelectedItems(): List<Task> {
        return currentList.filter { selectedItems.contains(it.id) }
    }

    fun isMultiSelectMode(): Boolean = isMultiSelectMode

    class DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
