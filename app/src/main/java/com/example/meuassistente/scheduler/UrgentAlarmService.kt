package com.example.meuassistente.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.meuassistente.R
import com.example.meuassistente.ui.alarm.AlarmAlertActivity

class UrgentAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarmAndSelf()
            return START_NOT_STICKY
        }

        val taskId = intent?.getLongExtra(TaskScheduler.EXTRA_TASK_ID, -1L) ?: -1L
        val occurrenceId = intent?.getLongExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Lembrete Agendado"
        val description = intent?.getStringExtra(EXTRA_DESCRIPTION) ?: ""

        startForegroundNotification(taskId, occurrenceId, title, description)
        startAlarmAudioAndVibration()

        val alertIntent = Intent(this, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
        }
        runCatching { startActivity(alertIntent) }

        return START_STICKY
    }

    private fun startForegroundNotification(
        taskId: Long,
        occurrenceId: Long,
        title: String,
        description: String
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            URGENT_CHANNEL_ID,
            "Alertas Persistentes de Tarefas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)

        val uniqueKey = if (occurrenceId != -1L) (occurrenceId + 200_000L).toInt() else taskId.toInt()

        val fullScreenIntent = Intent(this, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this,
            uniqueKey + 5000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze5Intent = Intent(this, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_SNOOZE_TASK
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(TaskAlarmReceiver.EXTRA_SNOOZE_MINUTES, 5)
        }
        val snooze5Pi = PendingIntent.getBroadcast(
            this,
            uniqueKey + 6001,
            snooze5Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze15Intent = Intent(this, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_SNOOZE_TASK
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(TaskAlarmReceiver.EXTRA_SNOOZE_MINUTES, 15)
        }
        val snooze15Pi = PendingIntent.getBroadcast(
            this,
            uniqueKey + 6002,
            snooze15Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(this, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_COMPLETE_TASK
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, occurrenceId)
        }
        val completePi = PendingIntent.getBroadcast(
            this,
            uniqueKey + 6003,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, URGENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description.ifBlank { "Conclua ou adie para encerrar o alerta" })
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    description.ifBlank { "Toque em Concluir ou Adiar para encerrar o alerta sonoro e esta notificação." }
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPi, true)
            .addAction(0, "Adiar 5 min", snooze5Pi)
            .addAction(0, "Adiar 15 min", snooze15Pi)
            .addAction(0, "Concluir", completePi)
            .build()

        startForeground(URGENT_NOTIFICATION_ID, notification)
    }

    private fun startAlarmAudioAndVibration() {
        stopAlarmMedia()
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 700, 400, 700, 400)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopAlarmMedia() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        vibrator?.cancel()
    }

    private fun stopAlarmAndSelf() {
        stopAlarmMedia()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAlarmMedia()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val URGENT_CHANNEL_ID = "meu_assistente_urgent_alarm"
        const val URGENT_NOTIFICATION_ID = 9901
        const val ACTION_STOP = "com.example.meuassistente.ACTION_STOP_URGENT_ALARM"
        const val EXTRA_TITLE = "extra_urgent_title"
        const val EXTRA_DESCRIPTION = "extra_urgent_desc"

        fun start(
            context: Context,
            taskId: Long = -1L,
            occurrenceId: Long = -1L,
            title: String,
            description: String
        ) {
            val intent = Intent(context, UrgentAlarmService::class.java).apply {
                putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
                putExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, occurrenceId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DESCRIPTION, description)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, UrgentAlarmService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
        }
    }
}
