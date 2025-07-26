package com.stingsoftware.pasika.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.repository.ApiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: ApiaryRepository
) : ViewModel() {

    fun onTaskCheckedChanged(task: Task, isChecked: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = isChecked))
        }
    }

    fun deleteTasks(tasks: List<Task>) {
        viewModelScope.launch {
            repository.deleteTasks(tasks)
        }
    }

    fun updateTasksStatus(tasks: List<Task>, isCompleted: Boolean) {
        viewModelScope.launch {
            val taskIds = tasks.map { it.id }
            if (isCompleted) {
                repository.markTasksAsCompleted(taskIds)
            } else {
                repository.markTasksAsIncomplete(taskIds)
            }
        }
    }
}
