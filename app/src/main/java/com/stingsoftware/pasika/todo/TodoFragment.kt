package com.stingsoftware.pasika.ui.todo

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.databinding.FragmentTodoListBinding
import com.stingsoftware.pasika.viewmodel.TodoListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TodoListFragment : Fragment(R.layout.fragment_todo_list), SearchView.OnQueryTextListener {

    private val viewModel: TodoListViewModel by viewModels()
    private var _binding: FragmentTodoListBinding? = null
    private val binding get() = _binding!!

    private lateinit var todoAdapter: TodoAdapter
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var searchView: SearchView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTodoListBinding.bind(view)

        setupAdapter()
        setupMenu()
        setupObservers()
        setupListeners()
        setupSwipeToDelete()
        setupOnBackPressed()
    }

    private fun setupAdapter() {
        todoAdapter = TodoAdapter(
            onTaskClicked = { task ->
                val action = TodoListFragmentDirections.actionTodoListFragmentToAddEditTaskFragment(task.id, "Edit Task")
                findNavController().navigate(action)
            },
            onTaskChecked = { task, isChecked ->
                viewModel.onTaskCheckedChanged(task, isChecked)
            },
            onLongClick = {
                if (!todoAdapter.isMultiSelectMode()) {
                    setMultiSelectMode(true)
                    todoAdapter.toggleSelection(it)
                }
            },
            onSelectionChange = { count ->
                updateToolbarTitleForSelection(count)
            }
        )
        binding.recyclerViewTasks.adapter = todoAdapter
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_todo_list, menu)
                val searchItem = menu.findItem(R.id.action_search_tasks)
                searchView = searchItem.actionView as? SearchView
                searchView?.setOnQueryTextListener(this@TodoListFragment)
            }

            override fun onPrepareMenu(menu: Menu) {
                val inMultiSelectMode = todoAdapter.isMultiSelectMode()
                menu.findItem(R.id.action_search_tasks).isVisible = !inMultiSelectMode
                menu.findItem(R.id.action_select_all_tasks).isVisible = inMultiSelectMode
                menu.findItem(R.id.action_mark_complete_selected_tasks).isVisible = inMultiSelectMode
                menu.findItem(R.id.action_delete_selected_tasks).isVisible = inMultiSelectMode
                menu.findItem(R.id.action_cancel_selection).isVisible = inMultiSelectMode
                super.onPrepareMenu(menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_select_all_tasks -> {
                        todoAdapter.selectAll()
                        true
                    }
                    R.id.action_mark_complete_selected_tasks -> {
                        val selectedTasks = todoAdapter.getSelectedItems()
                        if (selectedTasks.isNotEmpty()) {
                            viewModel.onMarkCompleteClicked(selectedTasks)
                            setMultiSelectMode(false)
                        }
                        true
                    }
                    R.id.action_delete_selected_tasks -> {
                        val selectedTasks = todoAdapter.getSelectedItems()
                        if (selectedTasks.isNotEmpty()) {
                            showBulkDeleteConfirmationDialog(selectedTasks)
                        } else {
                            Toast.makeText(requireContext(), "No tasks selected.", Toast.LENGTH_SHORT).show()
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

    private fun setupObservers() {
        viewModel.filteredTasks.observe(viewLifecycleOwner) { tasks ->
            todoAdapter.submitList(tasks)
            binding.textViewEmptyStateTasks.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.onToastMessageShown()
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddTask.setOnClickListener {
            val action = TodoListFragmentDirections.actionTodoListFragmentToAddEditTaskFragment(-1L, "Add Task")
            findNavController().navigate(action)
        }
    }

    private fun setupSwipeToDelete() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (todoAdapter.isMultiSelectMode()) {
                    todoAdapter.notifyItemChanged(viewHolder.adapterPosition)
                    return
                }
                val task = todoAdapter.currentList[viewHolder.adapterPosition]
                viewModel.deleteTask(task)
                Snackbar.make(requireView(), "Task deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") { viewModel.onTaskCheckedChanged(task, task.isCompleted) }
                    .show()
            }
        }).attachToRecyclerView(binding.recyclerViewTasks)
    }

    private fun setupOnBackPressed() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (todoAdapter.isMultiSelectMode()) {
                    setMultiSelectMode(false)
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun setMultiSelectMode(enabled: Boolean) {
        todoAdapter.setMultiSelectMode(enabled)
        activity?.invalidateOptionsMenu()
    }

    private fun updateToolbarTitleForSelection(count: Int) {
        if (todoAdapter.isMultiSelectMode()) {
            activity?.title = if (count > 0) getString(R.string.selected_count_format, count) else "Select Tasks"
        } else {
            activity?.title = "To-Do List"
        }
    }

    private fun showBulkDeleteConfirmationDialog(tasksToDelete: List<Task>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Selected Tasks")
            .setMessage("Are you sure you want to delete ${tasksToDelete.size} selected tasks?")
            .setPositiveButton(R.string.delete_button) { _, _ ->
                viewModel.deleteTasks(tasksToDelete)
                Toast.makeText(requireContext(), "${tasksToDelete.size} tasks deleted.", Toast.LENGTH_SHORT).show()
                setMultiSelectMode(false)
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        viewModel.setSearchQuery(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        viewModel.setSearchQuery(newText)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onBackPressedCallback.remove()
        searchView?.setOnQueryTextListener(null)
        _binding = null
    }
}
