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

/**
 * RecyclerView Adapter for displaying a list of Inspection objects.
 * Uses DiffUtil for efficient list updates.
 *
 * @param onItemClick Lambda function to be called when an inspection item is clicked.
 */
class InspectionAdapter(
    private val onItemClick: (Inspection) -> Unit
) : ListAdapter<Inspection, InspectionAdapter.InspectionViewHolder>(InspectionDiffCallback()) {

    /**
     * ViewHolder for an individual Inspection item in the RecyclerView.
     * Binds Inspection data to the views defined in item_inspection.xml using View Binding.
     *
     * @param binding The generated binding object for item_inspection.xml.
     */
    inner class InspectionViewHolder(private val binding: ItemInspectionBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds an Inspection object's data to the views in the ViewHolder.
         * Sets up click listener for item click.
         * @param inspection The Inspection object to bind.
         */
        fun bind(inspection: Inspection) {
            // Format and display inspection date
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) // Include time for inspections
            binding.textViewInspectionDate.text = itemView.context.getString(R.string.inspection_date_format, formatter.format(Date(inspection.inspectionDate)))

            // Queen Cells Present & Count
            if (inspection.queenCellsPresent == true) {
                binding.textViewQueenCellsStatus.visibility = View.VISIBLE
                val queenCellsText = inspection.queenCellsCount?.let {
                    itemView.context.getString(R.string.queen_cells_present_with_count, it)
                } ?: itemView.context.getString(R.string.queen_cells_present_no_count)
                binding.textViewQueenCellsStatus.text = queenCellsText
            } else {
                binding.textViewQueenCellsStatus.visibility = View.GONE
            }

            // Brood Status Summary
            val broodStatus = mutableListOf<String>()
            inspection.framesEggsCount?.let { if (it > 0) broodStatus.add(itemView.context.getString(R.string.eggs_short_with_count, it)) }
            inspection.framesOpenBroodCount?.let { if (it > 0) broodStatus.add(itemView.context.getString(R.string.open_brood_short_with_count, it)) }
            inspection.framesCappedBroodCount?.let { if (it > 0) broodStatus.add(itemView.context.getString(R.string.capped_brood_short_with_count, it)) }

            if (broodStatus.isNotEmpty()) {
                binding.textViewBroodStatusSummary.visibility = View.VISIBLE
                binding.textViewBroodStatusSummary.text = itemView.context.getString(R.string.brood_status_format, broodStatus.joinToString(", "))
            } else {
                binding.textViewBroodStatusSummary.visibility = View.GONE
            }

            // Honey Stores (now in frames)
            inspection.honeyStoresEstimateFrames?.let {
                if (it > 0) {
                    binding.textViewHoneyStores.visibility = View.VISIBLE
                    binding.textViewHoneyStores.text = itemView.context.getString(R.string.honey_stores_format, it)
                } else {
                    binding.textViewHoneyStores.visibility = View.GONE
                }
            } ?: run {
                binding.textViewHoneyStores.visibility = View.GONE
            }

            // Pests/Diseases
            inspection.pestsDiseasesObserved?.let {
                if (it.isNotBlank()) {
                    binding.textViewPestsDiseases.visibility = View.VISIBLE
                    binding.textViewPestsDiseases.text = itemView.context.getString(R.string.pests_diseases_format, it)
                } else {
                    binding.textViewPestsDiseases.visibility = View.GONE
                }
            } ?: run {
                binding.textViewPestsDiseases.visibility = View.GONE
            }

            // Treatment Applied
            inspection.treatmentApplied?.let {
                if (it.isNotBlank()) {
                    binding.textViewTreatmentApplied.visibility = View.VISIBLE
                    binding.textViewTreatmentApplied.text = itemView.context.getString(R.string.treatment_applied_format, it)
                } else {
                    binding.textViewTreatmentApplied.visibility = View.GONE
                }
            } ?: run {
                binding.textViewTreatmentApplied.visibility = View.GONE
            }

            // Temperament Rating
            inspection.temperamentRating?.let {
                binding.textViewTemperamentRating.visibility = View.VISIBLE
                binding.textViewTemperamentRating.text = itemView.context.getString(R.string.temperament_rating_format, it)
            } ?: run {
                binding.textViewTemperamentRating.visibility = View.GONE
            }

            // Management Actions
            inspection.managementActionsTaken?.let {
                if (it.isNotBlank()) {
                    binding.textViewManagementActions.visibility = View.VISIBLE
                    binding.textViewManagementActions.text = itemView.context.getString(R.string.management_actions_format, it)
                } else {
                    binding.textViewManagementActions.visibility = View.GONE
                }
            } ?: run {
                binding.textViewManagementActions.visibility = View.GONE
            }

            // Notes
            inspection.notes?.let {
                if (it.isNotBlank()) {
                    binding.textViewInspectionNotes.visibility = View.VISIBLE
                    binding.textViewInspectionNotes.text = itemView.context.getString(R.string.notes_format, it)
                } else {
                    binding.textViewInspectionNotes.visibility = View.GONE
                }
            } ?: run {
                binding.textViewInspectionNotes.visibility = View.GONE
            }

            // Set up click listener for the entire card
            binding.root.setOnClickListener {
                onItemClick(inspection)
            }
        }
    }

    /**
     * Creates and returns a new InspectionViewHolder.
     * Called when the RecyclerView needs a new ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InspectionViewHolder {
        val binding = ItemInspectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InspectionViewHolder(binding)
    }

    /**
     * Binds the data at the specified position to the views in the ViewHolder.
     * Called by the RecyclerView to display the data at the specified position.
     */
    override fun onBindViewHolder(holder: InspectionViewHolder, position: Int) {
        val currentInspection = getItem(position)
        holder.bind(currentInspection)
    }

    /**
     * DiffUtil Callback for Inspection objects.
     * Used by ListAdapter to calculate minimal updates when the list changes,
     * improving RecyclerView performance.
     */
    class InspectionDiffCallback : DiffUtil.ItemCallback<Inspection>() {
        override fun areItemsTheSame(oldItem: Inspection, newItem: Inspection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Inspection, newItem: Inspection): Boolean {
            return oldItem == newItem
        }
    }
}
