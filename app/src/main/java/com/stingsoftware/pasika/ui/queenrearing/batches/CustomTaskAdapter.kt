package com.stingsoftware.pasika.ui.queenrearing.batches

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.CustomTask
import com.stingsoftware.pasika.databinding.ItemCustomTaskBinding

class CustomTaskAdapter(
    private val onRemoveClicked: (CustomTask) -> Unit
) : RecyclerView.Adapter<CustomTaskAdapter.CustomTaskViewHolder>() {

    private var tasks = listOf<CustomTask>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomTaskViewHolder {
        val binding = ItemCustomTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomTaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount() = tasks.size

    fun submitList(newTasks: List<CustomTask>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    inner class CustomTaskViewHolder(private val binding: ItemCustomTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.removeTaskButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onRemoveClicked(tasks[adapterPosition])
                }
            }
        }

        fun bind(task: CustomTask) {
            binding.taskTitleTextView.text = task.title
            val context = binding.root.context
            binding.taskDaysTextView.text = context.resources.getQuantityString(
                R.plurals.days_after_grafting,
                task.daysAfterGrafting,
                task.daysAfterGrafting
            )
        }
    }
}
