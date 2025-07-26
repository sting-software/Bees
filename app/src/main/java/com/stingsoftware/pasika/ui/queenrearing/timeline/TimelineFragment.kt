package com.stingsoftware.pasika.ui.queenrearing.timeline

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.databinding.FragmentTimelineBinding
import com.stingsoftware.pasika.todo.TodoAdapter
import com.stingsoftware.pasika.todo.TodoViewModel
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingFragmentDirections
import com.stingsoftware.pasika.ui.queenrearing.QueenRearingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimelineFragment : Fragment() {

    private var _binding: FragmentTimelineBinding? = null
    private val binding get() = _binding!!

    private val queenRearingViewModel: QueenRearingViewModel by viewModels({ requireParentFragment() })
    private lateinit var todoViewModel: TodoViewModel
    private lateinit var todoAdapter: TodoAdapter
    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimelineBinding.inflate(inflater, container, false)
        todoViewModel = ViewModelProvider(this).get(TodoViewModel::class.java)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeTasks()
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            onTaskClicked = { task ->
                if (todoAdapter.isMultiSelectMode()) {
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
                if (!todoAdapter.isMultiSelectMode()) {
                    todoAdapter.setMultiSelectMode(true)
                    actionMode = (activity as? androidx.appcompat.app.AppCompatActivity)?.startSupportActionMode(ActionModeCallback())
                    todoAdapter.toggleSelection(task)
                }
            },
            onSelectionChange = { count ->
                if (count == 0) {
                    actionMode?.finish()
                } else {
                    actionMode?.title = getString(R.string.selected_count_format, count)
                    actionMode?.invalidate()
                }
            }
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
                todoAdapter.submitList(tasks)
            }
        }
    }

    private inner class ActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_todo_list, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.findItem(R.id.action_delete_selected_tasks).isVisible = true
            menu.findItem(R.id.action_select_all_tasks).isVisible = true
            menu.findItem(R.id.action_mark_complete_selected_tasks).isVisible = true
            menu.findItem(R.id.action_cancel_selection).isVisible = true
            menu.findItem(R.id.action_search_tasks).isVisible = false
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val selectedTasks = todoAdapter.getSelectedItems()
            when (item.itemId) {
                R.id.action_delete_selected_tasks -> {
                    showDeleteConfirmationDialog(selectedTasks)
                }
                R.id.action_select_all_tasks -> {
                    todoAdapter.selectAll()
                }
                R.id.action_mark_complete_selected_tasks -> {
                    todoViewModel.updateTasksStatus(selectedTasks, true)
                    mode.finish()
                }
                R.id.action_cancel_selection -> {
                    mode.finish()
                }
                else -> return false
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            todoAdapter.setMultiSelectMode(false)
            actionMode = null
        }
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
                actionMode?.finish()
            }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
