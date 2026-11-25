package com.example.meuassistente.ui.screen.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meuassistente.domain.model.CalendarEvent
import com.example.meuassistente.domain.usecase.agenda.CancelCalendarEventUseCase
import com.example.meuassistente.domain.usecase.agenda.ObserveAgendaRangeUseCase
import com.example.meuassistente.domain.usecase.agenda.RescheduleCalendarEventUseCase
import com.example.meuassistente.domain.usecase.agenda.SaveCalendarEventUseCase
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AgendaScreen(
    observeAgendaRangeUseCase: ObserveAgendaRangeUseCase,
    saveCalendarEventUseCase: SaveCalendarEventUseCase,
    rescheduleCalendarEventUseCase: RescheduleCalendarEventUseCase,
    cancelCalendarEventUseCase: CancelCalendarEventUseCase,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val rangeFlow = remember { observeAgendaRangeUseCase(0L, Long.MAX_VALUE) }
    val events by rangeFlow.collectAsState(initial = emptyList())

    var titleInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val zoneId = remember { ZoneId.systemDefault() }
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")).withZone(zoneId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Agenda",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Compromissos com início e fim em UTC/fuso local",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = {
                        titleInput = it
                        errorMessage = null
                    },
                    label = { Text("Título do compromisso") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Observações (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                val now = System.currentTimeMillis()
                                val start = now + 3_600_000L
                                val end = start + 3_600_000L
                                saveCalendarEventUseCase(
                                    CalendarEvent(
                                        title = titleInput,
                                        description = descriptionInput.trim(),
                                        startAtUtc = start,
                                        endAtUtc = end,
                                        timezoneId = zoneId.id,
                                        status = "SCHEDULED",
                                        createdAtUtc = now
                                    )
                                )
                            }.onSuccess {
                                titleInput = ""
                                descriptionInput = ""
                                errorMessage = null
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Não foi possível salvar o compromisso."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agendar (+1h, duração 1h)")
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(events, key = { it.id }) { event ->
                val startFormatted = formatter.format(Instant.ofEpochMilli(event.startAtUtc))
                val endFormatted = formatter.format(Instant.ofEpochMilli(event.endAtUtc))
                val isCancelled = event.status == "CANCELLED"

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$startFormatted até $endFormatted • ${event.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCancelled) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (event.description.isNotBlank()) {
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (!isCancelled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            rescheduleCalendarEventUseCase(
                                                event = event,
                                                newStartAtUtc = event.startAtUtc + 3_600_000L,
                                                newEndAtUtc = event.endAtUtc + 3_600_000L
                                            )
                                        }
                                    }
                                ) {
                                    Text("Adiar +1h")
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            cancelCalendarEventUseCase(event)
                                        }
                                    }
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
