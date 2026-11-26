package com.example.meuassistente.ui.screen.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meuassistente.domain.model.CalendarEvent
import com.example.meuassistente.domain.usecase.agenda.CancelCalendarEventUseCase
import com.example.meuassistente.domain.usecase.agenda.ObserveAgendaRangeUseCase
import com.example.meuassistente.domain.usecase.agenda.RescheduleCalendarEventUseCase
import com.example.meuassistente.domain.usecase.agenda.SaveCalendarEventUseCase
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class QuickStartPreset(val label: String) {
    IN_30M("Em 30 min"),
    IN_1H("Em 1 hora"),
    TOMORROW_9AM("Amanhã 09:00"),
    CUSTOM("Horário...")
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedPreset by remember { mutableStateOf(QuickStartPreset.IN_1H) }
    var durationMinutes by remember { mutableIntStateOf(60) }
    var customTime by remember { mutableStateOf(LocalTime.now().plusHours(1).withMinute(0)) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCancelled by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val zoneId = remember { ZoneId.systemDefault() }
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM • HH:mm", Locale.forLanguageTag("pt-BR")).withZone(zoneId)
    }
    val timeOnlyFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR")).withZone(zoneId)
    }

    val activeEvents = remember(events) { events.filter { it.status != "CANCELLED" } }
    val displayedEvents = remember(events, showCancelled) {
        if (showCancelled) events else activeEvents
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = customTime.hour,
            initialMinute = customTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Horário de início") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        customTime = LocalTime.of(pickerState.hour, pickerState.minute)
                        selectedPreset = QuickStartPreset.CUSTOM
                        showTimePicker = false
                    }
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabeçalho
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Agenda",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (activeEvents.isEmpty()) {
                            "Nenhum compromisso ativo"
                        } else {
                            "${activeEvents.size} compromisso(s) ativo(s)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilterChip(
                    selected = showCancelled,
                    onClick = { showCancelled = !showCancelled },
                    label = {
                        Text(if (showCancelled) "Mostrando todos" else "Ocultar cancelados")
                    }
                )
            }
        }

        // Card de criação rápida com presets de horário e duração
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Novo compromisso",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = {
                            titleInput = it
                            errorMessage = null
                        },
                        label = { Text("Título do compromisso") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("Observações ou local (opcional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Presets de início
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Quando",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickStartPreset.entries.forEach { preset ->
                                val chipLabel = if (preset == QuickStartPreset.CUSTOM && selectedPreset == QuickStartPreset.CUSTOM) {
                                    "Hoje às %02d:%02d".format(customTime.hour, customTime.minute)
                                } else {
                                    preset.label
                                }
                                FilterChip(
                                    selected = selectedPreset == preset,
                                    onClick = {
                                        if (preset == QuickStartPreset.CUSTOM) {
                                            showTimePicker = true
                                        } else {
                                            selectedPreset = preset
                                        }
                                    },
                                    label = { Text(chipLabel) }
                                )
                            }
                        }
                    }

                    // Duração
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Duração",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30 to "30 min", 60 to "1 hora", 120 to "2 horas").forEach { (mins, label) ->
                                FilterChip(
                                    selected = durationMinutes == mins,
                                    onClick = { durationMinutes = mins },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

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
                                    val nowZoned = ZonedDateTime.now(zoneId)
                                    val startZoned = when (selectedPreset) {
                                        QuickStartPreset.IN_30M -> nowZoned.plusMinutes(30)
                                        QuickStartPreset.IN_1H -> nowZoned.plusHours(1)
                                        QuickStartPreset.TOMORROW_9AM -> nowZoned.plusDays(1)
                                            .withHour(9).withMinute(0).withSecond(0).withNano(0)
                                        QuickStartPreset.CUSTOM -> {
                                            val candidate = nowZoned
                                                .withHour(customTime.hour)
                                                .withMinute(customTime.minute)
                                                .withSecond(0)
                                                .withNano(0)
                                            if (candidate.isBefore(nowZoned)) candidate.plusDays(1) else candidate
                                        }
                                    }
                                    val startUtc = startZoned.toInstant().toEpochMilli()
                                    val endUtc = startUtc + durationMinutes * 60_000L
                                    saveCalendarEventUseCase(
                                        CalendarEvent(
                                            title = titleInput.trim(),
                                            description = descriptionInput.trim(),
                                            startAtUtc = startUtc,
                                            endAtUtc = endUtc,
                                            timezoneId = zoneId.id,
                                            status = "SCHEDULED",
                                            createdAtUtc = System.currentTimeMillis()
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
                        enabled = titleInput.isNotBlank(),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Agendar compromisso", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Empty state ou lista de compromissos
        if (displayedEvents.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Agenda livre por aqui",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Adicione um compromisso acima para acompanhar horários e lembretes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(displayedEvents, key = { it.id }) { event ->
                val startFormatted = dateTimeFormatter.format(Instant.ofEpochMilli(event.startAtUtc))
                val endFormatted = timeOnlyFormatter.format(Instant.ofEpochMilli(event.endAtUtc))
                val isCancelled = event.status == "CANCELLED"

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCancelled) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = event.title.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isCancelled) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ) {
                                Text(
                                    text = if (isCancelled) "Cancelado" else "Agendado",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCancelled) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$startFormatted – $endFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (event.description.isNotBlank()) {
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!isCancelled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            rescheduleCalendarEventUseCase(
                                                event = event,
                                                newStartAtUtc = event.startAtUtc + 3_600_000L,
                                                newEndAtUtc = event.endAtUtc + 3_600_000L
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Adiar +1h", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            cancelCalendarEventUseCase(event)
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Cancelar", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
