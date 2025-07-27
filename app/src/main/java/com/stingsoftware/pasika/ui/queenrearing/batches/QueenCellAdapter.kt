package com.stingsoftware.pasika.ui.queenrearing.batches

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.QueenCell
import com.stingsoftware.pasika.databinding.ItemQueenCellBinding

class QueenCellAdapter(
    private val onItemClick: (QueenCell, Int) -> Unit,
    private val onLongClick: (QueenCell) -> Unit,
    private val onSelectionChange: (Int) -> Unit
) : ListAdapter<QueenCell, QueenCellAdapter.QueenCellViewHolder>(DiffCallback()) {

    private val selectedItems = mutableSetOf<Long>()
    private var isMultiSelectMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueenCellViewHolder {
        val binding = ItemQueenCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QueenCellViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QueenCellViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QueenCellViewHolder(
        private val binding: ItemQueenCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val cell = getItem(position)
                    if (isMultiSelectMode) {
                        toggleSelection(cell)
                    } else {
                        onItemClick(cell, position + 1)
                    }
                }
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLongClick(getItem(position))
                }
                true
            }
        }

        fun bind(cell: QueenCell) {
            val cellNumber = adapterPosition + 1
            binding.cellId.text = itemView.context.getString(R.string.cell, cellNumber)
            binding.cellStatus.text = cell.status.getLabel(itemView.context)

            if (isMultiSelectMode) {
                binding.checkboxCellSelect.visibility = View.VISIBLE
                binding.checkboxCellSelect.isChecked = selectedItems.contains(cell.id)
            } else {
                binding.checkboxCellSelect.visibility = View.GONE
                binding.checkboxCellSelect.isChecked = false
            }

            val cardColor = if (selectedItems.contains(cell.id)) {
                ContextCompat.getColor(itemView.context, R.color.colorHiveSelectedBackground)
            } else {
                ContextCompat.getColor(itemView.context, R.color.colorSurface)
            }
            (itemView as MaterialCardView).setCardBackgroundColor(cardColor)
        }
    }

    fun setMultiSelectMode(enabled: Boolean) {
        isMultiSelectMode = enabled
    }

    fun toggleSelection(cell: QueenCell) {
        if (selectedItems.contains(cell.id)) {
            selectedItems.remove(cell.id)
        } else {
            selectedItems.add(cell.id)
        }
        notifyItemChanged(currentList.indexOf(cell))
        onSelectionChange(selectedItems.size)
    }

    fun selectAll() {
        if (selectedItems.size == currentList.size) {
            clearSelections()
        } else {
            selectedItems.clear()
            currentList.forEach { selectedItems.add(it.id) }
            notifyItemRangeChanged(0, itemCount)
        }
        onSelectionChange(selectedItems.size)
    }

    fun getSelectedItems(): List<QueenCell> {
        return currentList.filter { selectedItems.contains(it.id) }
    }

    fun isMultiSelectMode(): Boolean = isMultiSelectMode

    fun clearSelections() {
        val hadSelections = selectedItems.isNotEmpty()
        selectedItems.clear()
        if (hadSelections) {
            notifyDataSetChanged()
        }
        onSelectionChange(0)
    }

    class DiffCallback : DiffUtil.ItemCallback<QueenCell>() {
        override fun areItemsTheSame(oldItem: QueenCell, newItem: QueenCell) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: QueenCell, newItem: QueenCell) = oldItem == newItem
    }
}
