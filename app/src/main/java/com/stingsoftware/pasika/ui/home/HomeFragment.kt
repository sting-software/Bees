package com.stingsoftware.pasika.ui.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Apiary
import com.stingsoftware.pasika.databinding.FragmentHomeBinding
import com.stingsoftware.pasika.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(), SearchView.OnQueryTextListener {

    private val homeViewModel: HomeViewModel by viewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var apiaryAdapter: ApiaryAdapter
    private var searchView: SearchView? = null
    private var searchMenuItem: MenuItem? = null

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private val importApiaryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            homeViewModel.importApiaryFromFile(requireContext(), it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        apiaryAdapter = ApiaryAdapter(
            onItemClick = { apiary ->
                if (apiaryAdapter.isMultiSelectMode()) {
                    apiaryAdapter.toggleSelection(apiary)
                } else {
                    val action = HomeFragmentDirections.actionHomeFragmentToApiaryDetailFragment(apiary.id, apiary.name)
                    findNavController().navigate(action)
                }
            },
            onEditClick = { apiary ->
                val action = HomeFragmentDirections.actionHomeFragmentToAddEditApiaryFragment(apiary.id)
                findNavController().navigate(action)
            },
            onLongClick = { apiary ->
                if (!apiaryAdapter.isMultiSelectMode()) {
                    setMultiSelectMode(true)
                    apiaryAdapter.toggleSelection(apiary)
                }
            },
            onSelectionChange = { count ->
                updateToolbarTitleForSelection(count)
            }
        )
        binding.recyclerViewApiaries.adapter = apiaryAdapter

        setupMenu()

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (searchView?.isIconified == false) {
                    searchMenuItem?.collapseActionView()
                    return
                }
                if (apiaryAdapter.isMultiSelectMode()) {
                    setMultiSelectMode(false)
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        homeViewModel.filteredApiaries.observe(viewLifecycleOwner) { apiaries ->
            apiaries?.let {
                apiaryAdapter.submitList(it)
                binding.textViewEmptyState.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerViewApiaries.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        homeViewModel.importStatus.observe(viewLifecycleOwner) { success ->
            success?.let {
                val message = if (it) "Apiary imported successfully." else "Import failed. Invalid file or format."
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                homeViewModel.onImportStatusHandled()
            }
        }

        binding.fabAddApiary.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToAddEditApiaryFragment(-1L)
            findNavController().navigate(action)
        }
    }

    override fun onResume() {
        super.onResume()
        if (searchView?.isIconified == false) {
            searchMenuItem?.collapseActionView()
        }
        if (!apiaryAdapter.isMultiSelectMode()) {
            activity?.title = getString(R.string.app_name)
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu)
                searchMenuItem = menu.findItem(R.id.action_search_apiaries)
                searchView = searchMenuItem?.actionView as? SearchView
                searchView?.setOnQueryTextListener(this@HomeFragment)
            }

            override fun onPrepareMenu(menu: Menu) {
                val inMultiSelectMode = apiaryAdapter.isMultiSelectMode()

                // Standard actions
                menu.findItem(R.id.action_import_apiary)?.isVisible = !inMultiSelectMode
                menu.findItem(R.id.action_search_apiaries)?.isVisible = !inMultiSelectMode

                // Multi-select actions
                menu.findItem(R.id.action_select_all_apiaries)?.isVisible = inMultiSelectMode
                menu.findItem(R.id.action_edit_selected_apiaries)?.isVisible = inMultiSelectMode
                menu.findItem(R.id.action_delete_selected_apiaries)?.isVisible = inMultiSelectMode
                menu.findItem(R.id.action_cancel_selection)?.isVisible = inMultiSelectMode

                // Other UI elements
                binding.fabAddApiary.visibility = if (inMultiSelectMode) View.GONE else View.VISIBLE

                if (inMultiSelectMode && searchMenuItem?.isActionViewExpanded == true) {
                    searchMenuItem?.collapseActionView()
                }
                super.onPrepareMenu(menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.action_import_apiary -> importApiaryLauncher.launch(arrayOf("application/json"))
                    R.id.action_select_all_apiaries -> apiaryAdapter.selectAll()
                    R.id.action_edit_selected_apiaries -> {
                        val selectedIds = apiaryAdapter.getSelectedItems().map { it.id }.toLongArray()
                        if (selectedIds.isNotEmpty()) {
                            val action = HomeFragmentDirections.actionHomeFragmentToBulkEditApiaryFragment(selectedIds)
                            findNavController().navigate(action)
                            setMultiSelectMode(false)
                        } else {
                            Toast.makeText(requireContext(), "No apiaries selected.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    R.id.action_delete_selected_apiaries -> {
                        val selectedApiaries = apiaryAdapter.getSelectedItems()
                        if (selectedApiaries.isNotEmpty()) {
                            showBulkDeleteConfirmationDialog(selectedApiaries)
                        } else {
                            Toast.makeText(requireContext(), "No apiaries selected.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    R.id.action_cancel_selection -> setMultiSelectMode(false)
                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setMultiSelectMode(enabled: Boolean) {
        apiaryAdapter.setMultiSelectMode(enabled)
        activity?.invalidateOptionsMenu()
    }

    private fun updateToolbarTitleForSelection(count: Int) {
        if (apiaryAdapter.isMultiSelectMode()) {
            activity?.title = if (count > 0) getString(R.string.selected_count_format, count) else "Select Apiaries"
        } else {
            activity?.title = getString(R.string.app_name)
        }
    }

    private fun showBulkDeleteConfirmationDialog(apiariesToDelete: List<Apiary>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_selected_apiaries_title))
            .setMessage(getString(R.string.delete_selected_apiaries_message, apiariesToDelete.size))
            .setPositiveButton(R.string.delete_button) { _, _ ->
                apiariesToDelete.forEach { homeViewModel.deleteApiary(it) }
                Toast.makeText(requireContext(), "${apiariesToDelete.size} apiaries deleted.", Toast.LENGTH_SHORT).show()
                setMultiSelectMode(false)
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        homeViewModel.setSearchQuery(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        homeViewModel.setSearchQuery(newText)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        onBackPressedCallback.remove()
    }
}