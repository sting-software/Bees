package com.stingsoftware.pasika.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.stingsoftware.pasika.R
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.repository.ApiaryRepository
import com.stingsoftware.pasika.todo.TodoListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    private val savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private object HeaderKeys {
        const val OVERDUE = "overdue"
        const val TODAY = "today"
        const val UPCOMING = "upcoming"
        const val NO_DATE = "no_date"
        const val COMPLETED = "completed"
    }

    companion object {
        private const val EXPANDED_STATE_KEY = "expandedState"
    }

    private val _searchQuery = MutableStateFlow("")
    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _headerExpandedState: StateFlow<Map<String, Boolean>> =
        savedStateHandle.getStateFlow(EXPANDED_STATE_KEY, mapOf(
            HeaderKeys.OVERDUE to false,
            HeaderKeys.TODAY to false,
            HeaderKeys.UPCOMING to false,
            HeaderKeys.NO_DATE to false,
            HeaderKeys.COMPLETED to false
        ))

    @OptIn(ExperimentalCoroutinesApi::class)
    val groupedTasks: LiveData<List<TodoListItem>> = combine(
        repository.getAllTasks(),
        _searchQuery,
        _headerExpandedState
    ) { tasks, query, expandedState ->
        val finalItems = mutableListOf<TodoListItem>()

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startOfTomorrow = (startOfToday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        // Partition all tasks to know which sections should exist
        val (allCompleted, allIncomplete) = tasks.partition { it.isCompleted }
        val allOverdue = allIncomplete.filter { it.dueDate != null && it.dueDate < startOfToday.timeInMillis }
        val allToday = allIncomplete.filter { it.dueDate != null && it.dueDate >= startOfToday.timeInMillis && it.dueDate < startOfTomorrow.timeInMillis }
        val allUpcoming = allIncomplete.filter { it.dueDate != null && it.dueDate >= startOfTomorrow.timeInMillis }
        val allNoDate = allIncomplete.filter { it.dueDate == null }

        // Filter tasks based on the search query
        val filteredTasks = if (query.isBlank()) {
            tasks
        } else {
            tasks.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description?.contains(query, ignoreCase = true) == true
            }
        }
        // Partition the filtered tasks
        val (filteredCompleted, filteredIncomplete) = filteredTasks.partition { it.isCompleted }
        val filteredOverdue = filteredIncomplete.filter { it.dueDate != null && it.dueDate < startOfToday.timeInMillis }
        val filteredToday = filteredIncomplete.filter { it.dueDate != null && it.dueDate >= startOfToday.timeInMillis && it.dueDate < startOfTomorrow.timeInMillis }
        val filteredUpcoming = filteredIncomplete.filter { it.dueDate != null && it.dueDate >= startOfTomorrow.timeInMillis }
        val filteredNoDate = filteredIncomplete.filter { it.dueDate == null }

        // Helper to build the final list
        fun addSection(headerKey: String, headerTitle: String, initialTaskList: List<Task>, filteredTaskList: List<Task>) {
            if (initialTaskList.isNotEmpty()) { // Only show a section if it ever has tasks
                val isExpanded = expandedState[headerKey] ?: false
                val header = TodoListItem.HeaderItem(headerKey, headerTitle, isExpandable = true, isExpanded = isExpanded)
                finalItems.add(header)
                if (isExpanded) {
                    finalItems.addAll(filteredTaskList.map { TodoListItem.TaskItem(it) })
                }
            }
        }

        addSection(HeaderKeys.OVERDUE, context.getString(R.string.header_overdue), allOverdue, filteredOverdue)
        addSection(HeaderKeys.TODAY, context.getString(R.string.header_today), allToday, filteredToday)
        addSection(HeaderKeys.UPCOMING, context.getString(R.string.header_upcoming), allUpcoming, filteredUpcoming)
        addSection(HeaderKeys.NO_DATE, context.getString(R.string.header_no_due_date), allNoDate, filteredNoDate)

        if (allCompleted.isNotEmpty()) {
            val completedHeaderTitle = context.getString(R.string.header_completed, allCompleted.size)
            val isCompletedExpanded = expandedState[HeaderKeys.COMPLETED] ?: false
            val completedHeader = TodoListItem.HeaderItem(
                key = HeaderKeys.COMPLETED,
                title = completedHeaderTitle,
                isExpandable = true,
                isExpanded = isCompletedExpanded
            )
            finalItems.add(completedHeader)
            if (isCompletedExpanded) {
                finalItems.addAll(filteredCompleted.map { TodoListItem.TaskItem(it) })
            }
        }

        finalItems
    }.asLiveData()


    fun toggleHeaderExpanded(headerKey: String) {
        val currentMap = _headerExpandedState.value.toMutableMap()
        val currentState = currentMap[headerKey] ?: false
        currentMap[headerKey] = !currentState
        savedStateHandle[EXPANDED_STATE_KEY] = currentMap
    }

    fun setSearchQuery(query: String?) {
        _searchQuery.value = query ?: ""
    }

    fun onTaskCheckedChanged(task: Task, isChecked: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = isChecked))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun insertTask(task: Task) = viewModelScope.launch {
        repository.insertTask(task)
    }

    fun deleteTasks(tasks: List<Task>) {
        viewModelScope.launch {
            repository.deleteTasks(tasks)
        }
    }

    fun onMarkCompleteClicked(tasks: List<Task>) {
        if (tasks.isEmpty()) return
        viewModelScope.launch {
            val taskIds = tasks.map { it.id }
            repository.markTasksAsCompleted(taskIds)
        }
    }

    fun onToastMessageShown() {
        _toastMessage.value = null
    }
}
