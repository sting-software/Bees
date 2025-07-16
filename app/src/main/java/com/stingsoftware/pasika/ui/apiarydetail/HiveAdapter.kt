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

/**
 * RecyclerView Adapter for displaying a list of Hive objects.
 * Uses DiffUtil for efficient list updates.
 *
 * @param onItemClick Lambda function to be called when a hive item is clicked (normal mode).
 * @param onEditClick Lambda function to be called when the edit button is clicked for a hive.
 * @param onDeleteSwipe Lambda function to be called when a hive item is swiped for deletion.
 * @param onLongClick Lambda function to be called when a hive item is long-clicked (to initiate multi-select).
 * @param onSelectionChange Lambda function to be called when the selection state changes (e.g., number of selected items).
 */
class HiveAdapter(
    private val onItemClick: (Hive) -> Unit,
    private val onEditClick: (Hive) -> Unit,
    private val onDeleteSwipe: (Hive) -> Unit,
    private val onLongClick: (Hive) -> Unit,
    private val onSelectionChange: (Int) -> Unit // Callback for number of selected items
) : ListAdapter<Hive, HiveAdapter.HiveViewHolder>(HiveDiffCallback()) {

    // Set to keep track of selected hive IDs
    private val selectedItems = mutableSetOf<Long>()

    // Flag to indicate if multi-selection mode is active
    private var isMultiSelectMode = false

    /**
     * ViewHolder for an individual Hive item in the RecyclerView.
     * Binds Hive data to the views defined in item_hive.xml using View Binding.
     *
     * @param binding The generated binding object for item_hive.xml.
     */
    inner class HiveViewHolder(private val binding: ItemHiveBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds a Hive object's data to the views in the ViewHolder.
         * Sets up click listeners for item click, edit, long click, and manages selection state.
         * @param hive The Hive object to bind.
         */
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

            // Format and display last inspection date
            hive.lastInspectionDate?.let { dateMillis ->
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.textViewLastInspectionDate.text = itemView.context.getString(
                    R.string.last_inspection_format,
                    formatter.format(Date(dateMillis))
                )
            } ?: run {
                binding.textViewLastInspectionDate.text =
                    itemView.context.getString(R.string.last_inspection_none)
            }

            // Set up click listeners
            binding.root.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleSelection(hive)
                } else {
                    onItemClick(hive)
                }
            }

            binding.root.setOnLongClickListener {
                onLongClick(hive)
                true // Consume the long click
            }

            binding.imageButtonEditHive.setOnClickListener {
                onEditClick(hive)
            }

            // Manage checkbox visibility and checked state
            if (isMultiSelectMode) {
                binding.checkboxHiveSelect.visibility = View.VISIBLE
                binding.checkboxHiveSelect.isChecked = selectedItems.contains(hive.id)
                // Ensure checkbox state is updated when clicked directly
                binding.checkboxHiveSelect.setOnClickListener {
                    toggleSelection(hive)
                }
            } else {
                binding.checkboxHiveSelect.visibility = View.GONE
                binding.checkboxHiveSelect.isChecked = false // Reset checkbox state
            }

            // Update card background based on selection
            binding.hiveCardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (selectedItems.contains(hive.id)) R.color.colorHiveSelectedBackground else R.color.colorHiveDefaultBackground
                )
            )
        }
    }

    /**
     * Toggles the selection state of a given hive.
     * @param hive The Hive object whose selection state is to be toggled.
     */
    fun toggleSelection(hive: Hive) {
        if (selectedItems.contains(hive.id)) {
            selectedItems.remove(hive.id)
        } else {
            selectedItems.add(hive.id)
        }
        notifyItemChanged(currentList.indexOf(hive)) // Rebind the item to update UI
        onSelectionChange(selectedItems.size) // Notify fragment about selection count change
    }

    /**
     * Selects all items in the current list.
     */
    fun selectAll() {
        selectedItems.clear() // Clear existing selection
        currentList.forEach { hive ->
            selectedItems.add(hive.id) // Add all current items to selection
        }
        notifyDataSetChanged() // Rebind all items to update UI
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
            notifyDataSetChanged() // Rebind all items to show/hide checkboxes
        }
    }

    /**
     * Clears all selected items.
     */
    fun clearSelection() {
        selectedItems.clear()
        notifyDataSetChanged() // Rebind all items to uncheck checkboxes
        onSelectionChange(0) // Notify fragment that selection count is 0
    }

    /**
     * Returns a list of currently selected Hive objects.
     */
    fun getSelectedItems(): List<Hive> {
        return currentList.filter { selectedItems.contains(it.id) }
    }

    /**
     * Checks if multi-selection mode is currently active.
     */
    fun isMultiSelectMode(): Boolean {
        return isMultiSelectMode
    }

    /**
     * Public method to handle deletion via swipe.
     * This method will be called by the ItemTouchHelper in the Fragment.
     */
    fun deleteItem(hive: Hive) {
        onDeleteSwipe(hive)
    }

    /**
     * Creates and returns a new HiveViewHolder.
     * Called when the RecyclerView needs a new ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiveViewHolder {
        val binding = ItemHiveBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HiveViewHolder(binding)
    }

    /**
     * Binds the data at the specified position to the views in the ViewHolder.
     * Called by the RecyclerView to display the data at the specified position.
     */
    override fun onBindViewHolder(holder: HiveViewHolder, position: Int) {
        val currentHive = getItem(position)
        holder.bind(currentHive)
    }

    /**
     * DiffUtil Callback for Hive objects.
     * Used by ListAdapter to calculate minimal updates when the list changes,
     * improving RecyclerView performance.
     */
    class HiveDiffCallback : DiffUtil.ItemCallback<Hive>() {
        override fun areItemsTheSame(oldItem: Hive, newItem: Hive): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Hive, newItem: Hive): Boolean {
            return oldItem == newItem
        }
    }
}
