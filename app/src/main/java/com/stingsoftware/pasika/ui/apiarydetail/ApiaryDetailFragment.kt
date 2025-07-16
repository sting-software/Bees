package com.stingsoftware.pasika.ui.apiarydetail

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Hive
import com.stingsoftware.pasika.databinding.FragmentApiaryDetailBinding
import com.stingsoftware.pasika.viewmodel.ApiaryDetailViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApiaryDetailFragment : Fragment(), SearchView.OnQueryTextListener {

    private val args: ApiaryDetailFragmentArgs by navArgs()
    private val apiaryDetailViewModel: ApiaryDetailViewModel by viewModels()

    private var _binding: FragmentApiaryDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var hiveAdapter: HiveAdapter
    private var searchView: SearchView? = null
    private var searchMenuItem: MenuItem? = null

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApiaryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hiveAdapter = HiveAdapter(
            onItemClick = { hive ->
                val action =
                    ApiaryDetailFragmentDirections.actionApiaryDetailFragmentToInspectionListFragment(
                        hiveId = hive.id,
                        hiveNumber = hive.hiveNumber ?: "N/A"
                    )
                findNavController().navigate(action)
            },
            onEditClick = { hive ->
                val action =
                    ApiaryDetailFragmentDirections.actionApiaryDetailFragmentToAddEditHiveFragment(
                        apiaryId = args.apiaryId,
                        hiveId = hive.id
                    )
                findNavController().navigate(action)
            },
            onDeleteSwipe = { hive ->
                showDeleteHiveConfirmationDialog(hive)
            },
            onLongClick = { hive ->
                if (!hiveAdapter.isMultiSelectMode()) {
                    setMultiSelectMode(true)
                    hiveAdapter.toggleSelection(hive)
                }
            },
            onSelectionChange = { count ->
                if (count > 0) {
                    activity?.title = getString(R.string.selected_count_format, count)
                } else {
                    activity?.title = ""
                }
            }
        )
        binding.recyclerViewHives.adapter = hiveAdapter

        setupHiveSwipeToDelete()

        apiaryDetailViewModel.apiary.observe(viewLifecycleOwner) { apiary ->
            apiary?.let {
                binding.textViewApiaryLocationDetail.text = it.location
                binding.textViewApiaryTypeDetail.text = getString(it.type.stringResId)
            } ?: run {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_apiary_not_found), Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }
        }

        apiaryDetailViewModel.filteredHivesForApiary.observe(viewLifecycleOwner) { hives ->
            hives?.let {
                hiveAdapter.submitList(it)
                val isEmpty = it.isEmpty()
                binding.emptyStateContainer.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.recyclerViewHives.visibility = if (isEmpty) View.GONE else View.VISIBLE
                binding.emptyStateContainer.findViewById<TextView>(R.id.text_view_empty_message).text =
                    getString(R.string.empty_state_no_hives)

                apiaryDetailViewModel.allHivesForApiary.value?.size?.let { allHivesCount ->
                    binding.textViewApiaryHivesCountDetail.text =
                        getString(R.string.label_hives_count, allHivesCount)
                } ?: run {
                    binding.textViewApiaryHivesCountDetail.text =
                        getString(R.string.label_hives_count, 0)
                }
            }
        }

        apiaryDetailViewModel.moveStatus.observe(viewLifecycleOwner) { success ->
            success?.let {
                val message =
                    if (it) getString(R.string.message_hives_moved) else getString(R.string.error_move_hives_failed)
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                if (it) {
                    setMultiSelectMode(false)
                }
                apiaryDetailViewModel.onMoveStatusHandled()
            }
        }

        apiaryDetailViewModel.exportStatus.observe(viewLifecycleOwner) { success ->
            success?.let {
                val message = if (it) getString(R.string.message_export_successful) else getString(
                    R.string.error_export_failed
                )
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                apiaryDetailViewModel.onExportStatusHandled()
            }
        }

        binding.fabAddHive.setOnClickListener {
            val action =
                ApiaryDetailFragmentDirections.actionApiaryDetailFragmentToAddEditHiveFragment(args.apiaryId)
            findNavController().navigate(action)
        }

        binding.imageButtonEditApiary.setOnClickListener {
            val action =
                ApiaryDetailFragmentDirections.actionApiaryDetailFragmentToAddEditApiaryFragment(
                    args.apiaryId
                )
            findNavController().navigate(action)
        }

        setupMenu()

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (searchView?.isIconified == false) {
                    searchMenuItem?.collapseActionView()
                    return
                }
                if (hiveAdapter.isMultiSelectMode()) {
                    setMultiSelectMode(false)
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    override fun onResume() {
        super.onResume()
        if (searchView?.isIconified == false) {
            searchMenuItem?.collapseActionView()
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_apiary_detail, menu)
                searchMenuItem = menu.findItem(R.id.action_search_hives)
                searchView = searchMenuItem?.actionView as? SearchView
                searchView?.setOnQueryTextListener(this@ApiaryDetailFragment)
            }

            override fun onPrepareMenu(menu: Menu) {
                val inMultiSelectMode = hiveAdapter.isMultiSelectMode()

                menu.findItem(R.id.action_search_hives)?.isVisible = !inMultiSelectMode
                menu.findItem(R.id.action_export_apiary)?.isVisible = true
                menu.findItem(R.id.action_select_all_hives)?.isVisible = inMultiSelectMode
                menu.findItem(R.id.action_move_selected_hives)?.isVisible = inMultiSelectMode
                menu.findItem(R.id.action_edit_selected_hives)?.isVisible = inMultiSelectMode
                menu.findItem(R.id.action_delete_selected_hives)?.isVisible = inMultiSelectMode
                menu.findItem(R.id.action_cancel_selection)?.isVisible = inMultiSelectMode

                binding.fabAddHive.visibility = if (inMultiSelectMode) View.GONE else View.VISIBLE
                binding.imageButtonEditApiary.visibility =
                    if (inMultiSelectMode) View.GONE else View.VISIBLE

                if (inMultiSelectMode && searchMenuItem?.isActionViewExpanded == true) {
                    searchMenuItem?.collapseActionView()
                }
                super.onPrepareMenu(menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_export_apiary -> {
                        apiaryDetailViewModel.exportApiaryData(requireContext())
                        true
                    }

                    R.id.action_select_all_hives -> {
                        hiveAdapter.selectAll()
                        true
                    }

                    R.id.action_move_selected_hives -> {
                        showMoveHivesDialog()
                        true
                    }

                    R.id.action_edit_selected_hives -> {
                        val selectedHives = hiveAdapter.getSelectedItems()
                        if (selectedHives.isNotEmpty()) {
                            val selectedHiveIds = selectedHives.map { it.id }.toLongArray()
                            val action =
                                ApiaryDetailFragmentDirections.actionApiaryDetailFragmentToBulkEditHiveFragment(
                                    selectedHiveIds
                                )
                            findNavController().navigate(action)
                            setMultiSelectMode(false)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.error_no_hives_selected_for_editing),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }

                    R.id.action_delete_selected_hives -> {
                        val selectedHives = hiveAdapter.getSelectedItems()
                        if (selectedHives.isNotEmpty()) {
                            showBulkDeleteConfirmationDialog(selectedHives)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.error_no_hives_selected_for_deletion),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }

                    R.id.action_cancel_selection -> {
                        setMultiSelectMode(false)
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showMoveHivesDialog() {
        val selectedHives = hiveAdapter.getSelectedItems()
        if (selectedHives.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_no_hives_selected_to_move), Toast.LENGTH_SHORT
            ).show()
            return
        }

        apiaryDetailViewModel.allApiaries.observe(viewLifecycleOwner) { allApiaries ->
            val destinationApiaries = allApiaries.filter { it.id != args.apiaryId }
            if (destinationApiaries.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_no_other_apiaries_to_move_to), Toast.LENGTH_SHORT
                ).show()
                return@observe
            }

            val apiaryNames = destinationApiaries.map { it.name }.toTypedArray()

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_title_move_hives, selectedHives.size))
                .setItems(apiaryNames) { dialog, which ->
                    val destination = destinationApiaries[which]
                    val selectedIds = selectedHives.map { it.id }
                    apiaryDetailViewModel.moveHives(selectedIds, destination.id)
                    dialog.dismiss()
                }
                .show()

            apiaryDetailViewModel.allApiaries.removeObservers(viewLifecycleOwner)
        }
    }

    private fun setMultiSelectMode(enabled: Boolean) {
        hiveAdapter.setMultiSelectMode(enabled)
        if (!enabled) {
            activity?.title = args.apiaryName
        }
        requireActivity().invalidateOptionsMenu()
    }

    private fun setupHiveSwipeToDelete() {
        val itemTouchHelperCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            private val background = ColorDrawable(Color.RED)
            private val deleteIcon =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (hiveAdapter.isMultiSelectMode()) {
                    hiveAdapter.notifyItemChanged(viewHolder.adapterPosition)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_swipe_delete_in_multiselect), Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val hiveToDelete = hiveAdapter.currentList[position]
                    showDeleteHiveConfirmationDialog(hiveToDelete)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )

                // --- FIX: This block restores the swipe-to-delete visual effect ---
                val itemView = viewHolder.itemView
                deleteIcon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + (itemView.height - it.intrinsicHeight) / 2
                    val iconBottom = iconTop + it.intrinsicHeight

                    if (dX > 0) { // Swiping right
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = itemView.left + iconMargin + it.intrinsicWidth
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        background.setBounds(
                            itemView.left,
                            itemView.top,
                            itemView.left + dX.toInt(),
                            itemView.bottom
                        )
                    } else if (dX < 0) { // Swiping left
                        val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        background.setBounds(
                            itemView.right + dX.toInt(),
                            itemView.top,
                            itemView.right,
                            itemView.bottom
                        )
                    } else { // No swipe
                        background.setBounds(0, 0, 0, 0)
                    }
                    background.draw(c)
                    it.draw(c)
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewHives)
    }

    private fun showDeleteHiveConfirmationDialog(hive: Hive) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.title_delete_hive))
            .setMessage(
                getString(
                    R.string.dialog_message_delete_hive,
                    hive.hiveNumber ?: "N/A"
                )
            )
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                apiaryDetailViewModel.deleteHive(hive)
            }
            .setNegativeButton(getString(R.string.action_cancel)) { _, _ ->
                hiveAdapter.currentList.indexOf(hive).takeIf { it != -1 }?.let {
                    hiveAdapter.notifyItemChanged(it)
                }
            }
            .show()
    }

    private fun showBulkDeleteConfirmationDialog(hivesToDelete: List<Hive>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_delete_selected_items))
            .setMessage(
                getString(
                    R.string.dialog_message_delete_selected_items,
                    hivesToDelete.size,
                    hivesToDelete.joinToString(", ") { it.hiveNumber ?: "N/A" }
                )
            )
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                hivesToDelete.forEach { apiaryDetailViewModel.deleteHive(it) }
                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_hives_deleted, hivesToDelete.size),
                    Toast.LENGTH_SHORT
                ).show()
                setMultiSelectMode(false)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        apiaryDetailViewModel.setHiveSearchQuery(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        apiaryDetailViewModel.setHiveSearchQuery(newText)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        onBackPressedCallback.remove()
    }
}
