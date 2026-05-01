package com.qualitytool.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qualitytool.app.MainActivity
import com.qualitytool.app.data.AppDatabase

class DeadlineWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "deadline_reminder"
        const val CHANNEL_NAME = "截止日期提醒"
        const val TASK_ID_KEY = "task_id"
        const val TASK_TITLE_KEY = "task_title"
    }

    override suspend fun doWork(): Result {
        val taskTitle = inputData.getString(TASK_TITLE_KEY) ?: return Result.failure()
        val taskId = inputData.getString(TASK_ID_KEY) ?: return Result.failure()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return Result.failure()
            }
        }

        val task = AppDatabase.getInstance(applicationContext).taskDao()
        val tasks = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var snapshot: List<com.qualitytool.app.data.TaskEntity> = emptyList()
            AppDatabase.getInstance(applicationContext).taskDao().getAllTasks().collect {
                snapshot = it; return@collect
            }
            snapshot
        }
        val entity = tasks.find { it.id == taskId } ?: return Result.success()
        if (entity.isCompleted) return Result.success()

        createNotificationChannel()
        showNotification(taskId, taskTitle)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "任务截止日期提醒" }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(taskId: String, title: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⏰ 任务截止日期已到")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(taskId.hashCode(), notification)
    }
}
