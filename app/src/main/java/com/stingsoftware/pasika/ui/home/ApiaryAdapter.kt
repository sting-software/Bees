package com.stingsoftware.pasika.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.databinding.ItemApiaryBinding
import java.util.Collections

class ApiaryAdapter(
    private val onItemClick: (Apiary) -> Unit,
    private val onEditClick: (Apiary) -> Unit,
    private val onSelectionChange: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onMultiSelectModeChange: (Boolean) -> Unit
) : ListAdapter<Apiary, ApiaryAdapter.ApiaryViewHolder>(ApiaryDiffCallback()) {

    private val selectedItems = mutableSetOf<Long>()
    private var isMultiSelectMode = false

    inner class ApiaryViewHolder(private val binding: ItemApiaryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("ClickableViewAccessibility")
        fun bind(apiary: Apiary) {
            binding.textViewApiaryName.text = apiary.name
            binding.textViewApiaryLocation.text = apiary.location
            binding.textViewApiaryType.text = itemView.context.getString(apiary.type.stringResId)
            binding.textViewNumberOfHives.text =
                itemView.context.getString(R.string.label_hives_count, apiary.numberOfHives)

            binding.root.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleSelection(apiary)
                } else {
                    onItemClick(apiary)
                }
            }

            binding.root.setOnLongClickListener {
                if (!isMultiSelectMode) {
                    setMultiSelectMode(true)
                    toggleSelection(apiary)
                }
                true
            }

            binding.imageButtonEdit.setOnClickListener {
                onEditClick(apiary)
            }

            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }

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

    /**
     * NEW: Moves an item in the list for smooth animation and returns the updated list.
     */
    fun moveItem(fromPosition: Int, toPosition: Int): List<Apiary> {
        val newList = currentList.toMutableList()
        Collections.swap(newList, fromPosition, toPosition)
        submitList(newList)
        return newList
    }

    fun toggleSelection(apiary: Apiary) {
        val apiaryIndex = currentList.indexOfFirst { it.id == apiary.id }
        if (apiaryIndex != -1) {
            if (selectedItems.contains(apiary.id)) {
                selectedItems.remove(apiary.id)
            } else {
                selectedItems.add(apiary.id)
            }
            notifyItemChanged(apiaryIndex)
            onSelectionChange(selectedItems.size)
        }
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
            onMultiSelectModeChange(enabled)
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
        override fun areItemsTheSame(oldItem: Apiary, newItem: Apiary): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Apiary, newItem: Apiary): Boolean =
            oldItem == newItem
    }
}
