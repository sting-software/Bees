package com.stingsoftware.pasika.ui.queenrearing.batches

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.GraftingBatch
import com.stingsoftware.pasika.databinding.ItemGraftingBatchBinding
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingFragmentDirections
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for the RecyclerView that displays a list of GraftingBatch items.
 */
class GraftingBatchAdapter :
    ListAdapter<GraftingBatch, GraftingBatchAdapter.GraftingBatchViewHolder>(DiffCallback()) {

    /**
     * Creates new views (invoked by the layout manager).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GraftingBatchViewHolder {
        val binding =
            ItemGraftingBatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GraftingBatchViewHolder(binding)
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager).
     * Binds the data to the view and sets up the click listener for navigation.
     */
    override fun onBindViewHolder(holder: GraftingBatchViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.itemView.setOnClickListener {
            val action =
                QueenRearingFragmentDirections.actionQueenRearingFragmentToBatchDetailFragment(
                    currentItem.id
                )
            it.findNavController().navigate(action)
        }
        holder.bind(currentItem)
    }

    /**
     * Provides a reference to the views for each data item.
     */
    class GraftingBatchViewHolder(private val binding: ItemGraftingBatchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(batch: GraftingBatch) {
            binding.apply {
                batchName.text = batch.name
                graftingDate.text =
                    itemView.context.getString(R.string.grafted, formatDate(batch.graftingDate))
                cellCount.text = itemView.context.getString(R.string.cells, batch.cellsGrafted)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    /**
     * Callback for calculating the diff between two non-null items in a list.
     * Used by ListAdapter to determine which items have changed when updating the list.
     */
    class DiffCallback : DiffUtil.ItemCallback<GraftingBatch>() {
        override fun areItemsTheSame(oldItem: GraftingBatch, newItem: GraftingBatch) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GraftingBatch, newItem: GraftingBatch) =
            oldItem == newItem
    }
}
