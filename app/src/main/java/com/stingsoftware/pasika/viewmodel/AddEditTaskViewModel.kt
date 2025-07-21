package com.stingsoftware.pasika.viewmodel

import android.content.Context
import androidx.lifecycle.*
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.repository.ApiaryRepository
import com.stingsoftware.pasika.util.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val repository: ApiaryRepository,
    savedStateHandle: SavedStateHandle,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val alarmScheduler = AlarmScheduler(context)

    private val taskId = savedStateHandle.get<Long>("taskId")

    val task = liveData {
        if (taskId != null && taskId != -1L) {
            emit(repository.getTaskById(taskId))
        } else {
            emit(null)
        }
    }

    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus

    fun onSaveClick(task: Task) {
        viewModelScope.launch {
            if (task.id == 0L) {
                val newId = repository.insertTask(task)
                if (task.reminderEnabled) {
                    alarmScheduler.schedule(task.copy(id = newId))
                }
            } else {
                repository.updateTask(task)
                if (task.reminderEnabled) {
                    alarmScheduler.schedule(task)
                } else {
                    alarmScheduler.cancel(task)
                }
            }
            _saveStatus.value = true
        }
    }
}