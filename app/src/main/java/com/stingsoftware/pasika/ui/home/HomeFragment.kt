package com.stingsoftware.pasika.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                    searchMenuItem?.collapseActionView()
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

                searchMenuItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        menu.findItem(R.id.action_delete_selected_apiaries)?.isVisible = false
                        menu.findItem(R.id.action_cancel_selection)?.isVisible = false
                        binding.fabAddApiary.visibility = View.GONE
                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        homeViewModel.setSearchQuery(null)
                        if (!apiaryAdapter.isMultiSelectMode()) {
                            binding.fabAddApiary.visibility = View.VISIBLE
                        }
                        requireActivity().invalidateOptionsMenu()
                        return true
                    }
                })
            }

            override fun onPrepareMenu(menu: Menu) {
                val inMultiSelectMode = apiaryAdapter.isMultiSelectMode()
                menu.findItem(R.id.action_delete_selected_apiaries)?.isVisible = inMultiSelectMode
                menu.findItem(R.id.action_cancel_selection)?.isVisible = inMultiSelectMode
                menu.findItem(R.id.action_search_apiaries)?.isVisible = !inMultiSelectMode

                binding.fabAddApiary.visibility = if (inMultiSelectMode) View.GONE else View.VISIBLE

                if (inMultiSelectMode) {
                    searchMenuItem?.collapseActionView()
                }
                super.onPrepareMenu(menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete_selected_apiaries -> {
                        val selectedApiaries = apiaryAdapter.getSelectedItems()
                        if (selectedApiaries.isNotEmpty()) {
                            showBulkDeleteConfirmationDialog(selectedApiaries)
                        } else {
                            Toast.makeText(requireContext(), "No apiaries selected for deletion.", Toast.LENGTH_SHORT).show()
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

    private fun setMultiSelectMode(enabled: Boolean) {
        apiaryAdapter.setMultiSelectMode(enabled)
        if (!enabled) {
            activity?.title = getString(R.string.app_name)
        }
        requireActivity().invalidateOptionsMenu()
    }

    private fun updateToolbarTitleForSelection(count: Int) {
        if (count > 0) {
            activity?.title = getString(R.string.selected_count_format, count)
        } else if (apiaryAdapter.isMultiSelectMode()) {
            activity?.title = getString(R.string.select_apiaries_title)
        } else {
            activity?.title = getString(R.string.app_name)
        }
    }

    private fun showBulkDeleteConfirmationDialog(apiariesToDelete: List<Apiary>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Selected Apiaries")
            .setMessage("Are you sure you want to delete ${apiariesToDelete.size} selected apiaries? All associated hives will also be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                apiariesToDelete.forEach { apiary ->
                    homeViewModel.deleteApiary(apiary)
                }
                Toast.makeText(requireContext(), "${apiariesToDelete.size} apiaries deleted.", Toast.LENGTH_SHORT).show()
                setMultiSelectMode(false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        homeViewModel.setSearchQuery(query)
        binding.recyclerViewApiaries.scrollToPosition(0)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        homeViewModel.setSearchQuery(newText)
        binding.recyclerViewApiaries.scrollToPosition(0)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        onBackPressedCallback.remove()
    }
}