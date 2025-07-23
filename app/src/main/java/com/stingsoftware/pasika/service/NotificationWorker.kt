package com.stingsoftware.pasika.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.stingsoftware.pasika.R

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        // These keys are used to pass data to the Worker
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_TASK_DESC = "task_desc"
        private const val CHANNEL_ID = "task_reminders"
    }

    override fun doWork(): Result {
        // Retrieve data from inputData instead of an Intent
        val taskId = inputData.getLong(KEY_TASK_ID, -1)
        val title = inputData.getString(KEY_TASK_TITLE) ?: context.getString(R.string.title_task_reminder)
        val description = inputData.getString(KEY_TASK_DESC)

        if (taskId == -1L) {
            return Result.failure() // Or Result.success() if this isn't a critical failure
        }

        // The notification logic is the same as your onReceive method
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hexagon)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId.toInt(), notification)

        // Indicate that the work finished successfully
        return Result.success()
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.title_task_reminder),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}