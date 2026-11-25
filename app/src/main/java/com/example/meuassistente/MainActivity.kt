package com.example.meuassistente

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.meuassistente.scheduler.NotificationHelper
import com.example.meuassistente.scheduler.RescheduleWorker
import com.example.meuassistente.ui.screen.AssistantScreen
import com.example.meuassistente.ui.theme.MeuAssistenteTheme
import com.example.meuassistente.ui.viewmodel.TaskViewModel
import com.example.meuassistente.widget.TaskWidgetProvider

class MainActivity : ComponentActivity() {
    private val taskViewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        RescheduleWorker.enqueue(this)
        TaskWidgetProvider.refreshAll(this)

        if (intent?.getBooleanExtra("open_new_task", false) == true) {
            taskViewModel.openNewTaskDialog()
        }

        enableEdgeToEdge()
        setContent { MeuAssistenteTheme { AssistantScreen(viewModel = taskViewModel) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("open_new_task", false)) {
            taskViewModel.openNewTaskDialog()
        }
    }
}
