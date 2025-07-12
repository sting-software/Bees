package com.stingsoftware.pasika.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.databinding.ItemApiaryBinding // Import the generated binding class
import androidx.core.content.ContextCompat // Import ContextCompat for getColor
import com.google.android.material.card.MaterialCardView // Import MaterialCardView
import com.stingsoftware.pasika.R // Import your R file for colors

/**
 * RecyclerView Adapter for displaying a list of Apiary objects.
 * Uses DiffUtil for efficient list updates.
 *
 * @param onItemClick Lambda function to be called when an apiary item (the whole card) is clicked.
 * @param onEditClick Lambda function to be called when the edit button is clicked for an apiary.
 * @param onLongClick Lambda function to be called when an apiary item is long-clicked (to initiate multi-select).
 * @param onSelectionChange Lambda function to be called when the selection state changes (e.g., number of selected items).
 */
class ApiaryAdapter(
    private val onItemClick: (Apiary) -> Unit,
    private val onEditClick: (Apiary) -> Unit,
    private val onLongClick: (Apiary) -> Unit, // New: for initiating multi-select
    private val onSelectionChange: (Int) -> Unit // New: callback for selection count
) : ListAdapter<Apiary, ApiaryAdapter.ApiaryViewHolder>(ApiaryDiffCallback()) {

    // Set to keep track of selected apiary IDs
    private val selectedItems = mutableSetOf<Long>()
    // Flag to indicate if multi-selection mode is active
    private var isMultiSelectMode = false

    /**
     * ViewHolder for an individual Apiary item in the RecyclerView.
     * Binds Apiary data to the views defined in item_apiary.xml using View Binding.
     *
     * @param binding The generated binding object for item_apiary.xml.
     */
    inner class ApiaryViewHolder(private val binding: ItemApiaryBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds an Apiary object's data to the views in the ViewHolder.
         * Sets up click listeners for item click, edit, and long click actions,
         * and manages the visual selection state.
         * @param apiary The Apiary object to bind.
         */
        fun bind(apiary: Apiary) {
            binding.textViewApiaryName.text = apiary.name
            binding.textViewApiaryLocation.text = apiary.location
            binding.textViewNumberOfHives.text = itemView.context.getString(R.string.hives_count, apiary.numberOfHives)

            // Set up click listeners
            binding.root.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleSelection(apiary)
                } else {
                    onItemClick(apiary)
                }
            }

            binding.root.setOnLongClickListener {
                onLongClick(apiary)
                true // Consume the long click
            }

            binding.imageButtonEdit.setOnClickListener {
                onEditClick(apiary)
            }

            // Update card background based on selection
            binding.apiaryCardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (selectedItems.contains(apiary.id)) R.color.colorHiveSelectedBackground else android.R.color.transparent
                )
            )
        }
    }

    /**
     * Toggles the selection state of a given apiary.
     * @param apiary The Apiary object whose selection state is to be toggled.
     */
    fun toggleSelection(apiary: Apiary) {
        if (selectedItems.contains(apiary.id)) {
            selectedItems.remove(apiary.id)
        } else {
            selectedItems.add(apiary.id)
        }
        notifyItemChanged(currentList.indexOf(apiary)) // Rebind the item to update UI
        onSelectionChange(selectedItems.size) // Notify fragment about selection count change
    }

    /**
     * Sets multi-selection mode.
     * @param enabled True to enable multi-selection mode, false to disable.
     */
    fun setMultiSelectMode(enabled: Boolean) {
        if (isMultiSelectMode != enabled) {
            isMultiSelectMode = enabled
            if (!enabled) {
                clearSelection() // Clear selection when exiting multi-select mode
            }
            notifyDataSetChanged() // Rebind all items to update their appearance
        }
    }

    /**
     * Clears all selected items.
     */
    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged() // Rebind all items to uncheck/unhighlight
        onSelectionChange(0) // Notify fragment that selection count is 0
    }

    /**
     * Returns a list of currently selected Apiary objects.
     */
    fun getSelectedItems(): List<Apiary> {
        return currentList.filter { selectedItems.contains(it.id) }
    }

    /**
     * Checks if multi-selection mode is currently active.
     */
    fun isMultiSelectMode(): Boolean {
        return isMultiSelectMode
    }

    /**
     * Creates and returns a new ApiaryViewHolder.
     * Called when the RecyclerView needs a new ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApiaryViewHolder {
        // Inflate the layout using View Binding
        val binding = ItemApiaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApiaryViewHolder(binding)
    }

    /**
     * Binds the data at the specified position to the views in the ViewHolder.
     * Called by the RecyclerView to display the data at the specified position.
     */
    override fun onBindViewHolder(holder: ApiaryViewHolder, position: Int) {
        val currentApiary = getItem(position)
        holder.bind(currentApiary)
    }

    /**
     * DiffUtil Callback for Apiary objects.
     * Used by ListAdapter to calculate minimal updates when the list changes,
     * improving RecyclerView performance.
     */
    class ApiaryDiffCallback : DiffUtil.ItemCallback<Apiary>() {
        /**
         * Called to check whether two objects represent the same item.
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True if the two items represent the same object (usually by ID), false otherwise.
         */
        override fun areItemsTheSame(oldItem: Apiary, newItem: Apiary): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Called to check whether two items have the same data.
         * @param oldItem The item in the old list.
         * @param newItem The item in the new list.
         * @return True if the contents of the items are the same, false otherwise.
         */
        override fun areContentsTheSame(oldItem: Apiary, newItem: Apiary): Boolean {
            return oldItem == newItem // Data class equals method handles content comparison
        }
    }
}
