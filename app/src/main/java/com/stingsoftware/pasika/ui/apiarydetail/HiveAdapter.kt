package com.stingsoftware.pasika.ui.apiarydetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.databinding.ItemHiveBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.ContextCompat

class HiveAdapter(
    private val onItemClick: (Hive) -> Unit,
    private val onEditClick: (Hive) -> Unit,
    private val onDeleteSwipe: (Hive) -> Unit,
    private val onLongClick: (Hive) -> Unit,
    private val onSelectionChange: (Int) -> Unit
) : ListAdapter<Hive, HiveAdapter.HiveViewHolder>(HiveDiffCallback()) {

    private val selectedItems = mutableSetOf<Long>()
    private var isMultiSelectMode = false

    inner class HiveViewHolder(private val binding: ItemHiveBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(hive: Hive) {
            binding.textViewHiveNumber.text = hive.hiveNumber?.let {
                itemView.context.getString(R.string.hive_number_format, it)
            } ?: itemView.context.getString(R.string.hive_number_unknown)

            if (hive.hiveType.isNullOrBlank()) {
                binding.textViewHiveType.visibility = View.GONE
            } else {
                binding.textViewHiveType.visibility = View.VISIBLE
                binding.textViewHiveType.text =
                    itemView.context.getString(R.string.hive_type_format, hive.hiveType)
            }

            if (hive.frameType.isNullOrBlank()) {
                binding.textViewFrameType.visibility = View.GONE
            } else {
                binding.textViewFrameType.visibility = View.VISIBLE
                binding.textViewFrameType.text =
                    itemView.context.getString(R.string.frame_type_format, hive.frameType)
            }

            if (hive.breed.isNullOrBlank()) {
                binding.textViewBreed.visibility = View.GONE
            } else {
                binding.textViewBreed.visibility = View.VISIBLE
                binding.textViewBreed.text =
                    itemView.context.getString(R.string.breed_format, hive.breed)
            }

            binding.root.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleSelection(hive)
                } else {
                    onItemClick(hive)
                }
            }

            binding.root.setOnLongClickListener {
                onLongClick(hive)
                true
            }

            binding.imageButtonEditHive.setOnClickListener {
                onEditClick(hive)
            }

            if (isMultiSelectMode) {
                binding.checkboxHiveSelect.visibility = View.VISIBLE
                binding.checkboxHiveSelect.isChecked = selectedItems.contains(hive.id)
                binding.checkboxHiveSelect.setOnClickListener {
                    toggleSelection(hive)
                }
            } else {
                binding.checkboxHiveSelect.visibility = View.GONE
                binding.checkboxHiveSelect.isChecked = false
            }

            binding.hiveCardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (selectedItems.contains(hive.id)) R.color.colorHiveSelectedBackground else R.color.colorHiveDefaultBackground
                )
            )
        }
    }

    fun toggleSelection(hive: Hive) {
        if (selectedItems.contains(hive.id)) {
            selectedItems.remove(hive.id)
        } else {
            selectedItems.add(hive.id)
        }
        notifyItemChanged(currentList.indexOf(hive))
        onSelectionChange(selectedItems.size)
    }

    fun selectAll() {
        selectedItems.clear()
        currentList.forEach { hive ->
            selectedItems.add(hive.id)
        }
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

    fun getSelectedItems(): List<Hive> {
        return currentList.filter { selectedItems.contains(it.id) }
    }

    fun isMultiSelectMode(): Boolean {
        return isMultiSelectMode
    }

    fun deleteItem(hive: Hive) {
        onDeleteSwipe(hive)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiveViewHolder {
        val binding = ItemHiveBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HiveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HiveViewHolder, position: Int) {
        val currentHive = getItem(position)
        holder.bind(currentHive)
    }

    class HiveDiffCallback : DiffUtil.ItemCallback<Hive>() {
        override fun areItemsTheSame(oldItem: Hive, newItem: Hive): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Hive, newItem: Hive): Boolean {
            return oldItem == newItem
        }
    }
}
