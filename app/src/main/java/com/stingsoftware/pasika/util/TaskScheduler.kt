package com.stingsoftware.pasika.util

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.stingsoftware.pasika.data.Task
import com.stingsoftware.pasika.service.NotificationWorker
import java.util.concurrent.TimeUnit

class TaskScheduler(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(task: Task) {
        val dueDate = task.dueDate ?: return // Exit if no due date
        val now = System.currentTimeMillis()

        // Calculate the delay from now until the task's due date
        val delay = dueDate - now

        if (delay <= 0) return // Don't schedule tasks in the past

        // Create a Data object to pass info to the Worker
        val inputData = workDataOf(
            NotificationWorker.KEY_TASK_ID to task.id,
            NotificationWorker.KEY_TASK_TITLE to task.title,
            NotificationWorker.KEY_TASK_DESC to task.description
        )

        // Create a unique tag to find and cancel this work later
        val workTag = "notification_${task.id}"

        // Build the WorkRequest for our NotificationWorker
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(workTag)
            .build()

        // Enqueue the work
        workManager.enqueue(workRequest)
    }

    fun cancel(task: Task) {
        // Cancel the work using the same unique tag
        val workTag = "notification_${task.id}"
        workManager.cancelAllWorkByTag(workTag)
    }
}