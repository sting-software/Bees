package com.stingsoftware.pasika.ui.queenrearing.batches

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.QueenCell
import com.stingsoftware.pasika.databinding.ItemQueenCellBinding

class QueenCellAdapter(private val onCellClicked: (QueenCell) -> Unit) :
    ListAdapter<QueenCell, QueenCellAdapter.QueenCellViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueenCellViewHolder {
        val binding =
            ItemQueenCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QueenCellViewHolder(binding, onCellClicked)
    }

    override fun onBindViewHolder(holder: QueenCellViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class QueenCellViewHolder(
        private val binding: ItemQueenCellBinding,
        private val onCellClicked: (QueenCell) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cell: QueenCell) {
            binding.cellId.text = itemView.context.getString(R.string.cell, cell.id)
            binding.cellStatus.text = cell.status.name
            itemView.setOnClickListener { onCellClicked(cell) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<QueenCell>() {
        override fun areItemsTheSame(oldItem: QueenCell, newItem: QueenCell) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: QueenCell, newItem: QueenCell) = oldItem == newItem
    }
}
