package com.stingsoftware.pasika.ui.inspectionlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Inspection
import com.stingsoftware.pasika.databinding.ItemInspectionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InspectionAdapter(
    private val onItemClick: (Inspection) -> Unit
) : ListAdapter<Inspection, InspectionAdapter.InspectionViewHolder>(InspectionDiffCallback()) {

    inner class InspectionViewHolder(private val binding: ItemInspectionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(inspection: Inspection) {
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateLabel = itemView.context.getString(R.string.hint_inspection_date)
            val formattedDate = formatter.format(Date(inspection.inspectionDate))
            binding.textViewInspectionDate.text = "$dateLabel: $formattedDate"

            if (inspection.queenCellsPresent == true) {
                binding.textViewQueenCellsStatus.visibility = View.VISIBLE
                val queenCellsText = inspection.queenCellsCount?.let {
                    itemView.context.getString(R.string.format_queen_cells_present_with_count, it)
                } ?: itemView.context.getString(R.string.format_queen_cells_present_no_count)
                binding.textViewQueenCellsStatus.text = queenCellsText
            } else {
                binding.textViewQueenCellsStatus.visibility = View.GONE
            }

            val broodStatus = mutableListOf<String>()
            inspection.framesEggsCount?.let { if (it > 0) broodStatus.add(itemView.context.getString(R.string.format_short_eggs_with_count, it)) }
            inspection.framesOpenBroodCount?.let { if (it > 0) broodStatus.add(itemView.context.getString(R.string.format_short_open_brood_with_count, it)) }
            inspection.framesCappedBroodCount?.let { if (it > 0) broodStatus.add(itemView.context.getString(R.string.format_short_capped_brood_with_count, it)) }

            if (broodStatus.isNotEmpty()) {
                binding.textViewBroodStatusSummary.visibility = View.VISIBLE
                binding.textViewBroodStatusSummary.text = itemView.context.getString(R.string.format_brood_status, broodStatus.joinToString(", "))
            } else {
                binding.textViewBroodStatusSummary.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(inspection)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspectionViewHolder {
        val binding = ItemInspectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InspectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InspectionViewHolder, position: Int) {
        val currentInspection = getItem(position)
        holder.bind(currentInspection)
    }

    class InspectionDiffCallback : DiffUtil.ItemCallback<Inspection>() {
        override fun areItemsTheSame(oldItem: Inspection, newItem: Inspection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Inspection, newItem: Inspection): Boolean {
            return oldItem == newItem
        }
    }
}
