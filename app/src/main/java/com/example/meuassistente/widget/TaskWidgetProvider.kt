package com.example.meuassistente.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.meuassistente.MainActivity
import com.example.meuassistente.R
import com.example.meuassistente.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun refreshAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, TaskWidgetProvider::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                updateWidgets(appContext, manager, ids)
            }
        }

        private suspend fun updateWidgets(
            context: Context,
            manager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val pendingTasks = AppDatabase.getInstance(context).taskDao().getPendingTasksSync()
            val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPi = PendingIntent.getActivity(
                context,
                7701,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val newTaskIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_new_task", true)
            }
            val newTaskPi = PendingIntent.getActivity(
                context,
                7702,
                newTaskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val itemIds = intArrayOf(R.id.widget_item_1, R.id.widget_item_2, R.id.widget_item_3)

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_tasks)
                views.setOnClickPendingIntent(R.id.widget_root, openAppPi)
                views.setOnClickPendingIntent(R.id.widget_add_btn, newTaskPi)

                if (pendingTasks.isEmpty()) {
                    views.setTextViewText(R.id.widget_subtitle, "Tudo em dia")
                    views.setTextViewText(R.id.widget_item_1, "Nenhum lembrete pendente")
                    views.setViewVisibility(R.id.widget_item_1, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_item_2, View.GONE)
                    views.setViewVisibility(R.id.widget_item_3, View.GONE)
                } else {
                    views.setTextViewText(R.id.widget_subtitle, "${pendingTasks.size} pendente(s)")
                    for (i in 0 until 3) {
                        if (i < pendingTasks.size) {
                            val t = pendingTasks[i]
                            val line = "• ${fmt.format(Date(t.triggerAtMillis))} — ${t.title}"
                            views.setTextViewText(itemIds[i], line)
                            views.setViewVisibility(itemIds[i], View.VISIBLE)
                        } else {
                            views.setViewVisibility(itemIds[i], View.GONE)
                        }
                    }
                }
                manager.updateAppWidget(widgetId, views)
            }
        }
    }
}
