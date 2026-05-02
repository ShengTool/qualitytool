package com.qualitytool.app.worker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.qualitytool.app.MainActivity
import com.qualitytool.app.R
import com.qualitytool.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, TaskWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(android.content.ComponentName(context, TaskWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entities = AppDatabase.getInstance(context).taskDao().getAllTasks().first()
                val tasks = entities.map { it.toTask() }
                val total = tasks.size
                val completed = tasks.count { it.isCompleted }
                val overdue = tasks.count {
                    it.deadline != null && it.deadline!! < System.currentTimeMillis() && !it.isCompleted
                }

                val views = RemoteViews(context.packageName, R.layout.task_widget)

                views.setTextViewText(R.id.widget_title, "📋 开发任务")
                views.setTextViewText(R.id.widget_progress,
                    "已完成 $completed / $total  ·  ${if (total > 0) (completed * 100 / total) else 0}%")
                views.setTextViewText(R.id.widget_overdue,
                    if (overdue > 0) "⚠️ $overdue 个任务已过期" else "✅ 没有过期任务")

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (_: Exception) {}
        }
    }
}
