package com.example.meuassistente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.meuassistente.scheduler.NotificationHelper
import com.example.meuassistente.scheduler.RescheduleWorker
import com.example.meuassistente.ui.screen.AssistantScreen
import com.example.meuassistente.ui.theme.MeuAssistenteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        RescheduleWorker.enqueue(this)
        enableEdgeToEdge()
        setContent { MeuAssistenteTheme { AssistantScreen() } }
    }
}
