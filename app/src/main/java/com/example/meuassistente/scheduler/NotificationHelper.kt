package com.example.meuassistente.scheduler

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
import com.example.meuassistente.R
import com.example.meuassistente.data.local.entity.TaskCategory
import com.example.meuassistente.data.local.entity.TaskEntity
import com.example.meuassistente.ui.alarm.AlarmAlertActivity

object NotificationHelper {
    const val CHANNEL_ID = "meu_assistente_reminders_v3"
    private const val CHANNEL_NAME = "Lembretes e Alarmes Persistentes"
    private const val CHANNEL_DESC = "Notificacoes fixas que exigem acao de concluir ou adiar"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            setSound(null, null)
            enableVibration(false)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showTaskNotification(context: Context, task: TaskEntity) {
        showPersistentReminder(
            context = context,
            notificationId = task.id.toInt(),
            taskId = task.id,
            occurrenceId = -1L,
            categoryLabel = TaskCategory.fromName(task.category).label,
            title = task.title,
            description = task.description
        )
    }

    fun showPersistentReminder(
        context: Context,
        notificationId: Int,
        taskId: Long = -1L,
        occurrenceId: Long = -1L,
        categoryLabel: String = "Tarefa",
        title: String,
        description: String
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(UrgentAlarmService.EXTRA_TITLE, title)
            putExtra(UrgentAlarmService.EXTRA_DESCRIPTION, description)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze5Intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_SNOOZE_TASK
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(TaskAlarmReceiver.EXTRA_SNOOZE_MINUTES, 5)
        }
        val snooze5PendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            snooze5Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze15Intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_SNOOZE_TASK
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(TaskAlarmReceiver.EXTRA_SNOOZE_MINUTES, 15)
        }
        val snooze15PendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            snooze15Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_COMPLETE_TASK
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, occurrenceId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = description.ifBlank { "Conclua ou adie para encerrar este alerta." }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSubText(categoryLabel)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Adiar 5 min", snooze5PendingIntent)
            .addAction(0, "Adiar 15 min", snooze15PendingIntent)
            .addAction(0, "Concluir", completePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelNotification(context: Context, taskId: Long) {
        NotificationManagerCompat.from(context).cancel(taskId.toInt())
    }
}
