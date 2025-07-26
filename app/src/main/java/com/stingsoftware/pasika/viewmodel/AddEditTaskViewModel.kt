package com.stingsoftware.pasika.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.repository.ApiaryRepository
import com.stingsoftware.pasika.util.TaskScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    private val savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val taskScheduler = TaskScheduler(context)

    // Private MutableLiveData to hold the task, and a public immutable LiveData to expose it.
    private val _task = MutableLiveData<Task?>()
    val task: LiveData<Task?> = _task

    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus

    init {
        // Fetch the task as soon as the ViewModel is created.
        val taskId = savedStateHandle.get<Long>("taskId")
        if (taskId != null && taskId != -1L) {
            viewModelScope.launch {
                _task.value = repository.getTaskById(taskId)
            }
        } else {
            _task.value = null
        }
    }

    /**
     * Saves the task to the repository and schedules or cancels reminders.
     */
    fun onSaveClick(task: Task) {
        viewModelScope.launch {
            if (task.id == 0L) {
                val newId = repository.insertTask(task)
                if (task.reminderEnabled) {
                    taskScheduler.schedule(task.copy(id = newId))
                }
            } else {
                repository.updateTask(task)
                if (task.reminderEnabled) {
                    taskScheduler.schedule(task)
                } else {
                    taskScheduler.cancel(task)
                }
            }
            _saveStatus.value = true
        }
    }
}
