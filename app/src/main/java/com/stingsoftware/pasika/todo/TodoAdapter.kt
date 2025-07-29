package com.stingsoftware.pasika.todo

import android.animation.ObjectAnimator
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
import com.stingsoftware.pasika.databinding.ItemTodoHeaderBinding
import java.text.SimpleDateFormat
import java.util.*

sealed class TodoListItem {
    data class TaskItem(val task: Task) : TodoListItem()
    data class HeaderItem(val key: String, val title: String, val isExpandable: Boolean = false, val isExpanded: Boolean = false) : TodoListItem()
}

class TodoAdapter(
    private val onTaskClicked: (Task) -> Unit,
    private val onTaskChecked: (Task, Boolean) -> Unit,
    private val onLongClick: (Task) -> Unit,
    private val onSelectionChange: (Int) -> Unit,
    private val onHeaderClicked: (TodoListItem.HeaderItem) -> Unit
) : ListAdapter<TodoListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private val selectedItems = mutableSetOf<Long>()
    private var isMultiSelectMode = false

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TASK = 1
        private const val PAYLOAD_EXPAND_COLLAPSE = "PAYLOAD_EXPAND_COLLAPSE"
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TodoListItem.HeaderItem -> TYPE_HEADER
            is TodoListItem.TaskItem -> TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemTodoHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TYPE_TASK -> TodoViewHolder(ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TodoListItem.HeaderItem -> (holder as HeaderViewHolder).bind(item)
            is TodoListItem.TaskItem -> (holder as TodoViewHolder).bind(item.task)
        }
    }

    // This method is called for partial updates, allowing for animations.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_EXPAND_COLLAPSE) && holder is HeaderViewHolder) {
            val item = getItem(position) as TodoListItem.HeaderItem
            holder.animateExpandCollapse(item.isExpanded)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }


    inner class HeaderViewHolder(private val binding: ItemTodoHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position) as TodoListItem.HeaderItem
                    if (item.isExpandable) {
                        onHeaderClicked(item)
                    }
                }
            }
        }

        fun bind(header: TodoListItem.HeaderItem) {
            binding.headerTitle.text = header.title
            if (header.isExpandable) {
                binding.expandIcon.visibility = View.VISIBLE
                // Use a single drawable and rotate it.
                binding.expandIcon.setImageResource(R.drawable.ic_expand_more)
                // Set the initial rotation without animation.
                binding.expandIcon.rotation = if (header.isExpanded) 180f else 0f
            } else {
                binding.expandIcon.visibility = View.GONE
            }
        }

        fun animateExpandCollapse(isExpanded: Boolean) {
            val targetRotation = if (isExpanded) 180f else 0f
            ObjectAnimator.ofFloat(binding.expandIcon, "rotation", targetRotation).apply {
                duration = 300
                start()
            }
        }
    }

    inner class TodoViewHolder(private val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = (getItem(position) as TodoListItem.TaskItem).task
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
                    onLongClick((getItem(position) as TodoListItem.TaskItem).task)
                }
                true
            }
            binding.checkboxCompleted.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = (getItem(position) as TodoListItem.TaskItem).task
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
                checkboxCompleted.isChecked = selectedItems.contains(task.id) || task.isCompleted
                textViewTaskTitle.text = task.title
                textViewTaskTitle.paintFlags = if (task.isCompleted && !isMultiSelectMode) {
                    textViewTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    textViewTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

                if (task.description.isNullOrBlank()) {
                    textViewTaskDescription.visibility = View.GONE
                } else {
                    textViewTaskDescription.visibility = View.VISIBLE
                    textViewTaskDescription.text = task.description
                }

                task.dueDate?.let {
                    textViewDueDate.visibility = View.VISIBLE
                    textViewDueDate.text = formatDate(it)
                } ?: run {
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

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy, hh:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    fun setMultiSelectMode(enabled: Boolean) {
        if (isMultiSelectMode == enabled) return
        isMultiSelectMode = enabled
        if (!enabled) {
            clearSelections()
        }
        notifyDataSetChanged()
    }

    fun toggleSelection(task: Task) {
        if (selectedItems.contains(task.id)) {
            selectedItems.remove(task.id)
        } else {
            selectedItems.add(task.id)
        }
        val itemIndex = currentList.indexOfFirst { it is TodoListItem.TaskItem && it.task.id == task.id }
        if (itemIndex != -1) {
            notifyItemChanged(itemIndex)
        }
        onSelectionChange(selectedItems.size)
    }

    fun selectAll() {
        val allTaskIds = currentList.filterIsInstance<TodoListItem.TaskItem>().map { it.task.id }
        if (selectedItems.size == allTaskIds.size) {
            clearSelections()
        } else {
            selectedItems.clear()
            selectedItems.addAll(allTaskIds)
            notifyDataSetChanged()
        }
        onSelectionChange(selectedItems.size)
    }

    fun getSelectedItems(): List<Task> {
        return currentList.filterIsInstance<TodoListItem.TaskItem>()
            .filter { selectedItems.contains(it.task.id) }
            .map { it.task }
    }

    fun isMultiSelectMode(): Boolean = isMultiSelectMode

    fun clearSelections() {
        if (selectedItems.isNotEmpty()) {
            selectedItems.clear()
            onSelectionChange(0)
            notifyDataSetChanged()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TodoListItem>() {
        override fun areItemsTheSame(oldItem: TodoListItem, newItem: TodoListItem): Boolean {
            return when {
                oldItem is TodoListItem.TaskItem && newItem is TodoListItem.TaskItem -> oldItem.task.id == newItem.task.id
                oldItem is TodoListItem.HeaderItem && newItem is TodoListItem.HeaderItem -> oldItem.key == newItem.key
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: TodoListItem, newItem: TodoListItem): Boolean {
            return oldItem == newItem
        }

        // This method provides the payload for partial updates.
        override fun getChangePayload(oldItem: TodoListItem, newItem: TodoListItem): Any? {
            if (oldItem is TodoListItem.HeaderItem && newItem is TodoListItem.HeaderItem) {
                if (oldItem.isExpanded != newItem.isExpanded) {
                    return PAYLOAD_EXPAND_COLLAPSE
                }
            }
            return null
        }
    }
}
