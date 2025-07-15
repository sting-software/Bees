package com.stingsoftware.pasika.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.databinding.ItemApiaryBinding

class ApiaryAdapter(
    private val onItemClick: (Apiary) -> Unit,
    private val onEditClick: (Apiary) -> Unit,
    private val onLongClick: (Apiary) -> Unit,
    private val onSelectionChange: (Int) -> Unit
) : ListAdapter<Apiary, ApiaryAdapter.ApiaryViewHolder>(ApiaryDiffCallback()) {

    private val selectedItems = mutableSetOf<Long>()
    private var isMultiSelectMode = false

    inner class ApiaryViewHolder(private val binding: ItemApiaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(apiary: Apiary) {
            binding.textViewApiaryName.text = apiary.name
            binding.textViewApiaryLocation.text = apiary.location
            binding.textViewNumberOfHives.text = itemView.context.getString(R.string.label_hives_count, apiary.numberOfHives)

            binding.root.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleSelection(apiary)
                } else {
                    onItemClick(apiary)
                }
            }

            binding.root.setOnLongClickListener {
                onLongClick(apiary)
                true
            }

            binding.imageButtonEdit.setOnClickListener {
                onEditClick(apiary)
            }

            // --- NEW: Manage checkbox and background color ---
            if (isMultiSelectMode) {
                binding.checkboxApiarySelect.visibility = View.VISIBLE
                binding.checkboxApiarySelect.isChecked = selectedItems.contains(apiary.id)
                binding.checkboxApiarySelect.setOnClickListener { toggleSelection(apiary) }
            } else {
                binding.checkboxApiarySelect.visibility = View.GONE
                binding.checkboxApiarySelect.isChecked = false
            }

            binding.apiaryCardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (selectedItems.contains(apiary.id)) R.color.colorHiveSelectedBackground else R.color.colorHiveDefaultBackground
                )
            )
        }
    }

    fun toggleSelection(apiary: Apiary) {
        if (selectedItems.contains(apiary.id)) {
            selectedItems.remove(apiary.id)
        } else {
            selectedItems.add(apiary.id)
        }
        notifyItemChanged(currentList.indexOf(apiary))
        onSelectionChange(selectedItems.size)
    }

    fun selectAll() {
        selectedItems.clear()
        currentList.forEach { selectedItems.add(it.id) }
        notifyDataSetChanged()
        onSelectionChange(selectedItems.size)
    }

    fun setMultiSelectMode(enabled: Boolean) {
        if (isMultiSelectMode != enabled) {
            isMultiSelectMode = enabled
            if (!enabled) {
                clearSelection()
            }
            notifyDataSetChanged()
        }
    }

    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged()
        onSelectionChange(0)
    }

    fun getSelectedItems(): List<Apiary> {
        return currentList.filter { selectedItems.contains(it.id) }
    }

    fun isMultiSelectMode(): Boolean {
        return isMultiSelectMode
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApiaryViewHolder {
        val binding = ItemApiaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApiaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApiaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ApiaryDiffCallback : DiffUtil.ItemCallback<Apiary>() {
        override fun areItemsTheSame(oldItem: Apiary, newItem: Apiary): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Apiary, newItem: Apiary): Boolean = oldItem == newItem
    }
}