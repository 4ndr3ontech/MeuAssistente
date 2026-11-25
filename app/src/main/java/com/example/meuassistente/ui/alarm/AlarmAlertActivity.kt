package com.example.meuassistente.ui.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.meuassistente.scheduler.TaskAlarmReceiver
import com.example.meuassistente.scheduler.TaskScheduler
import com.example.meuassistente.scheduler.UrgentAlarmService
import com.example.meuassistente.ui.theme.MeuAssistenteTheme

class AlarmAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val taskId = intent.getLongExtra(TaskScheduler.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(UrgentAlarmService.EXTRA_TITLE) ?: "Lembrete Urgente"
        val description = intent.getStringExtra(UrgentAlarmService.EXTRA_DESCRIPTION) ?: ""

        setContent {
            MeuAssistenteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmOn,
                            contentDescription = null,
                            modifier = Modifier.size(84.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "LEMBRETE URGENTE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (description.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                        Spacer(Modifier.height(40.dp))
                        Button(
                            onClick = {
                                UrgentAlarmService.stop(this@AlarmAlertActivity)
                                if (taskId != -1L) {
                                    sendBroadcast(Intent(this@AlarmAlertActivity, TaskAlarmReceiver::class.java).apply {
                                        action = TaskAlarmReceiver.ACTION_COMPLETE_TASK
                                        putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
                                    })
                                }
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Concluir Tarefa", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                UrgentAlarmService.stop(this@AlarmAlertActivity)
                                if (taskId != -1L) {
                                    sendBroadcast(Intent(this@AlarmAlertActivity, TaskAlarmReceiver::class.java).apply {
                                        action = TaskAlarmReceiver.ACTION_SNOOZE_TASK
                                        putExtra(TaskScheduler.EXTRA_TASK_ID, taskId)
                                    })
                                }
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp)
                        ) {
                            Icon(Icons.Default.Snooze, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Adiar 5 minutos", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
