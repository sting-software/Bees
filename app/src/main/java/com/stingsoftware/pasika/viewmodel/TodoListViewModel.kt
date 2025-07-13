package com.stingsoftware.pasika.viewmodel

import androidx.lifecycle.*
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: ApiaryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow<String?>(null)

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTasks = _searchQuery.flatMapLatest { query ->
        repository.getAllTasks().map { tasks ->
            if (query.isNullOrBlank()) {
                tasks
            } else {
                tasks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.description?.contains(query, ignoreCase = true) == true
                }
            }
        }
    }.asLiveData()

    fun setSearchQuery(query: String?) {
        _searchQuery.value = query
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

    fun deleteTasks(tasks: List<Task>) {
        viewModelScope.launch {
            tasks.forEach { repository.deleteTask(it) }
        }
    }

    fun onMarkCompleteClicked(tasks: List<Task>) {
        if (tasks.isEmpty()) return

        val allCompleted = tasks.all { it.isCompleted }
        val allIncomplete = tasks.all { !it.isCompleted }

        viewModelScope.launch {
            when {
                allCompleted -> {
                    val taskIds = tasks.map { it.id }
                    repository.markTasksAsIncomplete(taskIds)
                }
                allIncomplete -> {
                    val taskIds = tasks.map { it.id }
                    repository.markTasksAsCompleted(taskIds)
                }
                else -> {
                    _toastMessage.postValue("Cannot change status for a mixed selection of tasks.")
                }
            }
        }
    }

    fun onToastMessageShown() {
        _toastMessage.value = null
    }
}
