package com.example.meuassistente.scheduler

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.meuassistente.MainActivity
import com.example.meuassistente.R
import com.example.meuassistente.data.local.entity.TaskCategory
import com.example.meuassistente.data.local.entity.TaskEntity

object NotificationHelper {
    const val CHANNEL_ID = "meu_assistente_reminders_v2"
    private const val CHANNEL_NAME = "Lembretes e Alarmes"
    private const val CHANNEL_DESC = "Notificacoes prioritarias com acoes rapidas"

    fun createNotificationChannel(context: Context) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            setSound(soundUri, audioAttrs)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showTaskNotification(context: Context, task: TaskEntity) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("task_id", task.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_SNOOZE_TASK
            putExtra(TaskScheduler.EXTRA_TASK_ID, task.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 1).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_COMPLETE_TASK
            putExtra(TaskScheduler.EXTRA_TASK_ID, task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 2).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val categoryLabel = TaskCategory.fromName(task.category).label
        val contentText = task.description.ifBlank { "Horário agendado atingido." }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSubText(categoryLabel)
            .setContentTitle(task.title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "Adiar 5 min", snoozePendingIntent)
            .addAction(0, "Concluir", completePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(task.id.toInt(), notification)
    }

    fun cancelNotification(context: Context, taskId: Long) {
        NotificationManagerCompat.from(context).cancel(taskId.toInt())
    }
}
