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
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Lembrete Urgente"
        val description = intent?.getStringExtra(EXTRA_DESCRIPTION) ?: ""

        startForegroundNotification(taskId, title, description)
        startAlarmAudioAndVibration()

        val alertIntent = Intent(this, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
        }
        runCatching { startActivity(alertIntent) }

        return START_NOT_STICKY
    }

    private fun startForegroundNotification(taskId: Long, title: String, description: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            URGENT_CHANNEL_ID,
            "Alarmes Urgentes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)

        val fullScreenIntent = Intent(this, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this,
            (taskId + 5000).toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, UrgentAlarmService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(
            this,
            (taskId + 6000).toInt(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, URGENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("URGENTE: $title")
            .setContentText(description.ifBlank { "Toque para abrir ou parar o alarme" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPi, true)
            .addAction(0, "Parar Som", stopPi)
            .build()

        startForeground(URGENT_NOTIFICATION_ID, notification)
    }

    private fun startAlarmAudioAndVibration() {
        stopAlarmMedia()
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
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
        val pattern = longArrayOf(0, 600, 400, 600, 400)
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

        fun start(context: Context, taskId: Long, title: String, description: String) {
            val intent = Intent(context, UrgentAlarmService::class.java).apply {
                putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
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
