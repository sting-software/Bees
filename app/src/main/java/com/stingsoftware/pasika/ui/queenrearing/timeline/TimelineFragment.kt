package com.stingsoftware.pasika.ui.queenrearing.timeline

import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.databinding.FragmentTimelineBinding
import com.stingsoftware.pasika.todo.TodoAdapter
import com.stingsoftware.pasika.todo.TodoListItem
import com.stingsoftware.pasika.todo.TodoViewModel
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingFragmentDirections
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingViewModel
import com.stingsoftware.pasika.ui.queenrearing.SearchableFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimelineFragment : Fragment(), SearchableFragment {

    private var _binding: FragmentTimelineBinding? = null
    private val binding get() = _binding!!

    private val queenRearingViewModel: QueenRearingViewModel by viewModels({ requireParentFragment() })
    private lateinit var todoViewModel: TodoViewModel
    private lateinit var todoAdapter: TodoAdapter
    private var isMultiSelectMode = false
    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimelineBinding.inflate(inflater, container, false)
        todoViewModel = ViewModelProvider(this)[TodoViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeTasks()
        setupMenu()
        setupOnBackPressed()
    }

    private fun setupOnBackPressed() {
        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                finishMultiSelectMode()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            onTaskClicked = { task ->
                if (isMultiSelectMode) {
                    todoAdapter.toggleSelection(task)
                } else {
                    val action = QueenRearingFragmentDirections.actionQueenRearingFragmentToAddEditTaskFragment(
                        taskId = task.id,
                        title = getString(R.string.title_edit_task)
                    )
                    findNavController().navigate(action)
                }
            },
            onTaskChecked = { task, isChecked ->
                todoViewModel.onTaskCheckedChanged(task, isChecked)
            },
            onLongClick = { task ->
                if (!isMultiSelectMode) {
                    isMultiSelectMode = true
                    backPressedCallback.isEnabled = true
                    activity?.invalidateOptionsMenu()
                    todoAdapter.setMultiSelectMode(true)
                    todoAdapter.toggleSelection(task)
                }
            },
            onSelectionChange = { count ->
                if (isMultiSelectMode) {
                    if (count == 0) {
                        finishMultiSelectMode()
                    } else {
                        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.selected_count_format, count)
                    }
                }
            },
            onHeaderClicked = { /* No headers in this fragment, so do nothing. */ }
        )

        binding.recyclerViewTimeline.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeTasks() {
        queenRearingViewModel.queenRearingTasks.observe(viewLifecycleOwner) { tasks ->
            if (tasks.isNullOrEmpty()) {
                binding.recyclerViewTimeline.visibility = View.GONE
                binding.emptyState.root.visibility = View.VISIBLE
                binding.emptyState.textViewEmptyMessage.text =
                    getString(R.string.no_tasks_scheduled)
            } else {
                binding.recyclerViewTimeline.visibility = View.VISIBLE
                binding.emptyState.root.visibility = View.GONE
                // Wrap the list of Tasks into a list of TodoListItem.TaskItem
                todoAdapter.submitList(tasks.map { TodoListItem.TaskItem(it) })
            }
        }
    }

    override fun search(query: String?) {
        queenRearingViewModel.setSearchQuery(query)
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                if (isMultiSelectMode) {
                    menuInflater.inflate(R.menu.menu_todo_list, menu)
                }
            }

            override fun onPrepareMenu(menu: Menu) {
                if (isResumed) {
                    val searchItem = activity?.findViewById<View>(R.id.action_search)
                    searchItem?.visibility = if (isMultiSelectMode) View.GONE else View.VISIBLE
                }
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                if (item.itemId == android.R.id.home) {
                    if (isMultiSelectMode) {
                        finishMultiSelectMode()
                        return true
                    }
                }

                if (isMultiSelectMode) {
                    val selectedTasks = todoAdapter.getSelectedItems()
                    return when (item.itemId) {
                        R.id.action_delete_selected_tasks -> {
                            showDeleteConfirmationDialog(selectedTasks)
                            true
                        }
                        R.id.action_select_all_tasks -> {
                            todoAdapter.selectAll()
                            true
                        }
                        R.id.action_mark_complete_selected_tasks -> {
                            todoViewModel.updateTasksStatus(selectedTasks, true)
                            finishMultiSelectMode()
                            true
                        }
                        R.id.action_cancel_selection -> {
                            finishMultiSelectMode()
                            true
                        }
                        else -> false
                    }
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun finishMultiSelectMode() {
        if (!isMultiSelectMode) return
        isMultiSelectMode = false
        backPressedCallback.isEnabled = false
        todoAdapter.clearSelections()
        todoAdapter.setMultiSelectMode(false)
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.queen_rearing)
        activity?.invalidateOptionsMenu()
    }

    private fun showDeleteConfirmationDialog(tasks: List<Task>) {
        val quantity = tasks.size
        val title = resources.getQuantityString(R.plurals.dialog_title_delete_selected_tasks, quantity, quantity)
        val message = resources.getQuantityString(R.plurals.dialog_message_delete_selected_tasks, quantity, quantity)

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_yes) { _, _ ->
                todoViewModel.deleteTasks(tasks)
                finishMultiSelectMode()
            }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
