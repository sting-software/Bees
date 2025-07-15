package com.stingsoftware.pasika.ui.inspectionlist

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
import android.widget.Toast
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
import com.stingsoftware.pasika.data.Inspection
import com.stingsoftware.pasika.databinding.FragmentInspectionListBinding
import com.stingsoftware.pasika.viewmodel.InspectionListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InspectionListFragment : Fragment() {
    private val args: InspectionListFragmentArgs by navArgs()
    private val inspectionListViewModel: InspectionListViewModel by viewModels()
    private var _binding: FragmentInspectionListBinding? = null
    private val binding get() = _binding!!
    private lateinit var inspectionAdapter: InspectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInspectionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.title = getString(R.string.title_inspections_for_hive, args.hiveNumber)

        setupMenu()

        inspectionAdapter = InspectionAdapter { inspection ->
            val action =
                InspectionListFragmentDirections.actionInspectionListFragmentToAddEditInspectionFragment(
                    hiveId = args.hiveId,
                    inspectionId = inspection.id
                )
            findNavController().navigate(action)
        }
        binding.recyclerViewInspections.adapter = inspectionAdapter

        setupInspectionSwipeToDelete()

        inspectionListViewModel.inspectionsForHive.observe(viewLifecycleOwner) { inspections ->
            inspections?.let {
                inspectionAdapter.submitList(it)
                val isEmpty = it.isEmpty()
                binding.emptyStateView.root.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.recyclerViewInspections.visibility = if (isEmpty) View.GONE else View.VISIBLE
                binding.emptyStateView.textViewEmptyMessage.text = getString(R.string.empty_state_no_inspections)
            }
        }

        inspectionListViewModel.exportStatus.observe(viewLifecycleOwner) { success ->
            success?.let {
                val message = if (it) {
                    getString(R.string.message_export_successful)
                } else {
                    getString(R.string.error_export_failed_no_inspections)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                inspectionListViewModel.onExportStatusHandled()
            }
        }

        binding.fabAddInspection.setOnClickListener {
            val action =
                InspectionListFragmentDirections.actionInspectionListFragmentToAddEditInspectionFragment(
                    hiveId = args.hiveId,
                    inspectionId = -1L
                )
            findNavController().navigate(action)
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_inspection_list, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_export_csv -> {
                        showExportOptionsDialog()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showExportOptionsDialog() {
        val options = arrayOf(getString(R.string.action_export_to_csv), getString(R.string.action_export_to_pdf))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_title_export_format))
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> inspectionListViewModel.exportInspectionsToCsv(
                        requireContext(),
                        args.hiveNumber
                    )

                    1 -> inspectionListViewModel.exportInspectionsToPdf(
                        requireContext(),
                        args.hiveNumber
                    )
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun setupInspectionSwipeToDelete() {
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
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val inspectionToDelete = inspectionAdapter.currentList[position]
                    showDeleteInspectionConfirmationDialog(inspectionToDelete)
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
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewInspections)
    }

    private fun showDeleteInspectionConfirmationDialog(inspection: Inspection) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.title_delete_inspection))
            .setMessage(getString(R.string.dialog_message_delete_inspection))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                inspectionListViewModel.deleteInspection(inspection)
                Toast.makeText(requireContext(),
                    getString(R.string.message_inspection_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.action_cancel)) { _, _ ->
                inspectionAdapter.currentList.indexOf(inspection).takeIf { it != -1 }?.let {
                    inspectionAdapter.notifyItemChanged(it)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
