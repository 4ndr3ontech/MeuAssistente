package com.example.meuassistente.ui.screen.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meuassistente.data.local.entity.TaskOccurrenceEntity
import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.RecurrenceSpec
import com.example.meuassistente.domain.model.RecurrenceType
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.model.TaskSeries
import com.example.meuassistente.domain.repository.TaskDomainRepository
import com.example.meuassistente.domain.usecase.task.CancelOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.CompleteTaskOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.CreateTaskSeriesUseCase
import com.example.meuassistente.domain.usecase.task.ReopenTaskOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.SnoozeTaskOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.UpdateTaskSeriesRuleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksHubScreen(
    taskDomainRepository: TaskDomainRepository,
    occurrencesFlow: Flow<List<TaskOccurrenceEntity>>,
    createTaskSeriesUseCase: CreateTaskSeriesUseCase,
    updateTaskSeriesRuleUseCase: UpdateTaskSeriesRuleUseCase,
    completeOccurrenceUseCase: CompleteTaskOccurrenceUseCase,
    snoozeOccurrenceUseCase: SnoozeTaskOccurrenceUseCase,
    reopenOccurrenceUseCase: ReopenTaskOccurrenceUseCase,
    cancelOccurrenceUseCase: CancelOccurrenceUseCase,
    onOpenLegacyTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val seriesList by taskDomainRepository.observeActiveSeries().collectAsState(initial = emptyList())
    val occurrenceEntities by occurrencesFlow.collectAsState(initial = emptyList())

    var titleInput by remember { mutableStateOf("") }
    var recurrenceType by remember { mutableStateOf(RecurrenceType.DAILY) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val draftRulesBySeriesId = remember { mutableStateMapOf<Long, RecurrenceType>() }

    val zoneId = remember { ZoneId.systemDefault() }
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")).withZone(zoneId)
    }

    val seriesById = remember(seriesList) {
        seriesList.associateBy { it.id }
    }

    val recurrenceOptions = remember {
        listOf(
            RecurrenceType.NONE to "Única",
            RecurrenceType.DAILY to "Diária",
            RecurrenceType.WEEKLY to "Semanal",
            RecurrenceType.MONTHLY to "Mensal"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tarefas e Séries",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(onClick = onOpenLegacyTasks) {
                Text("Legadas")
            }
        }

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
                        statusMessage = null
                    },
                    label = { Text("Nova série de tarefa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recurrenceOptions.forEach { (type, label) ->
                        FilterChip(
                            selected = recurrenceType == type,
                            onClick = { recurrenceType = type },
                            label = { Text(label) }
                        )
                    }
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                val now = System.currentTimeMillis()
                                val defaultTz = TimeZone.getDefault().id
                                createTaskSeriesUseCase(
                                    series = TaskSeries(
                                        title = titleInput,
                                        recurrence = RecurrenceSpec(recurrenceType, 1),
                                        timezoneId = defaultTz,
                                        createdAtUtc = now
                                    ),
                                    initialTriggerUtc = now + 3_600_000L
                                )
                            }.onSuccess {
                                titleInput = ""
                                errorMessage = null
                                statusMessage = "Série criada com sucesso!"
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Falha ao criar série."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Criar série e ocorrência")
                }
            }
        }

        Text(
            text = "Séries ativas: ${seriesList.size} • Ocorrências: ${occurrenceEntities.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(seriesList, key = { "series_${it.id}" }) { seriesItem ->
                val selectedType = draftRulesBySeriesId[seriesItem.id] ?: seriesItem.recurrence.type
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Série: ${seriesItem.title}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Recorrência atual: ${seriesItem.recurrence.type.name} (x${seriesItem.recurrence.interval})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recurrenceOptions.forEach { (type, label) ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { draftRulesBySeriesId[seriesItem.id] = type },
                                    label = { Text(label) }
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    runCatching {
                                        updateTaskSeriesRuleUseCase(
                                            series = seriesItem,
                                            newRecurrence = RecurrenceSpec(
                                                type = selectedType,
                                                interval = seriesItem.recurrence.interval.coerceAtLeast(1)
                                            )
                                        )
                                    }.onSuccess { updated ->
                                        draftRulesBySeriesId.remove(seriesItem.id)
                                        errorMessage = null
                                        statusMessage = "Regra de \"${updated.title}\" atualizada para ${updated.recurrence.type.name}."
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Falha ao atualizar regra."
                                    }
                                }
                            }
                        ) {
                            Text("Salvar regra")
                        }
                    }
                }
            }

            items(occurrenceEntities, key = { "occ_${it.id}" }) { occEntity ->
                val domainOcc = TaskOccurrence(
                    id = occEntity.id,
                    taskSeriesId = occEntity.taskSeriesId,
                    scheduledAtUtc = occEntity.scheduledAtUtc,
                    timezoneId = occEntity.timezoneId,
                    snoozedUntilUtc = occEntity.snoozedUntilUtc,
                    status = OccurrenceStatus.fromName(occEntity.status),
                    completedAtUtc = occEntity.completedAtUtc
                )
                val effectiveTime = domainOcc.snoozedUntilUtc ?: domainOcc.scheduledAtUtc
                val whenLabel = formatter.format(Instant.ofEpochMilli(effectiveTime))
                val matchingSeries = seriesById[domainOcc.taskSeriesId]
                val seriesTitle = matchingSeries?.title ?: "Série #${domainOcc.taskSeriesId}"
                val recurrenceLabel = matchingSeries?.recurrence?.type?.name

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = seriesTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = buildString {
                                append("$whenLabel • ${domainOcc.status.name}")
                                if (recurrenceLabel != null) {
                                    append(" • $recurrenceLabel")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (domainOcc.status != OccurrenceStatus.COMPLETED) {
                                OutlinedButton(
                                    onClick = {
                                        val fallbackSeries = matchingSeries ?: TaskSeries(
                                            id = domainOcc.taskSeriesId,
                                            title = seriesTitle,
                                            recurrence = RecurrenceSpec(RecurrenceType.NONE, 1),
                                            timezoneId = domainOcc.timezoneId,
                                            createdAtUtc = domainOcc.scheduledAtUtc
                                        )
                                        scope.launch {
                                            completeOccurrenceUseCase(
                                                series = fallbackSeries,
                                                occurrence = domainOcc,
                                                completedAtUtc = System.currentTimeMillis()
                                            )
                                        }
                                    }
                                ) {
                                    Text("Concluir")
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            snoozeOccurrenceUseCase(
                                                occurrence = domainOcc,
                                                snoozeMinutes = 15,
                                                nowUtc = System.currentTimeMillis()
                                            )
                                        }
                                    }
                                ) {
                                    Text("Adiar 15m")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            reopenOccurrenceUseCase(domainOcc)
                                        }
                                    }
                                ) {
                                    Text("Reabrir")
                                }
                            }
                            if (domainOcc.status != OccurrenceStatus.CANCELLED) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            cancelOccurrenceUseCase(domainOcc)
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
