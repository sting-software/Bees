package com.stingsoftware.pasika.ui.queenrearing.batches

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.databinding.ItemGraftingBatchBinding
import java.text.SimpleDateFormat
import java.util.*

class GraftingBatchAdapter(
    private val onItemClick: (GraftingBatch) -> Unit,
    private val onEditClick: (GraftingBatch) -> Unit,
    private val onLongClick: (GraftingBatch) -> Unit,
    private val onSelectionChange: (Int) -> Unit
) : ListAdapter<GraftingBatch, GraftingBatchAdapter.GraftingBatchViewHolder>(DiffCallback()) {

    private val selectedItems = mutableSetOf<Long>()
    private var isMultiSelectMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GraftingBatchViewHolder {
        val binding = ItemGraftingBatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GraftingBatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GraftingBatchViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class GraftingBatchViewHolder(private val binding: ItemGraftingBatchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLongClick(getItem(position))
                }
                true
            }
            binding.buttonEditBatch.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(position))
                }
            }
        }

        fun bind(batch: GraftingBatch) {
            binding.apply {
                batchName.text = batch.name
                graftingDate.text = itemView.context.getString(R.string.grafted, formatDate(batch.graftingDate))
                cellCount.text = itemView.context.getString(R.string.cells, batch.cellsGrafted)

                val cardColor = if (selectedItems.contains(batch.id)) {
                    ContextCompat.getColor(itemView.context, R.color.colorHiveSelectedBackground)
                } else {
                    ContextCompat.getColor(itemView.context, R.color.colorSurface)
                }
                (itemView as MaterialCardView).setCardBackgroundColor(cardColor)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    fun setMultiSelectMode(enabled: Boolean) {
        if (isMultiSelectMode != enabled) {
            isMultiSelectMode = enabled
            if (!enabled) {
                selectedItems.clear()
                onSelectionChange(0)
            }
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun toggleSelection(batch: GraftingBatch) {
        if (selectedItems.contains(batch.id)) {
            selectedItems.remove(batch.id)
        } else {
            selectedItems.add(batch.id)
        }
        notifyItemChanged(currentList.indexOf(batch))
        onSelectionChange(selectedItems.size)
    }

    fun selectAll() {
        if (selectedItems.size == currentList.size) {
            selectedItems.clear()
        } else {
            selectedItems.clear()
            currentList.forEach { selectedItems.add(it.id) }
        }
        notifyItemRangeChanged(0, itemCount)
        onSelectionChange(selectedItems.size)
    }

    fun getSelectedItems(): List<GraftingBatch> {
        return currentList.filter { selectedItems.contains(it.id) }
    }

    fun isMultiSelectMode(): Boolean = isMultiSelectMode

    class DiffCallback : DiffUtil.ItemCallback<GraftingBatch>() {
        override fun areItemsTheSame(oldItem: GraftingBatch, newItem: GraftingBatch) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: GraftingBatch, newItem: GraftingBatch) = oldItem == newItem
    }
}
