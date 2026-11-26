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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

private fun RecurrenceType.toPtLabel(): String = when (this) {
    RecurrenceType.NONE -> "Única"
    RecurrenceType.DAILY -> "Diária"
    RecurrenceType.WEEKLY -> "Semanal"
    RecurrenceType.MONTHLY -> "Mensal"
    RecurrenceType.YEARLY -> "Anual"
    RecurrenceType.CUSTOM_DAYS -> "Personalizada"
}

private fun OccurrenceStatus.toPtLabel(): String = when (this) {
    OccurrenceStatus.PENDING -> "Pendente"
    OccurrenceStatus.SNOOZED -> "Adiada"
    OccurrenceStatus.COMPLETED -> "Concluída"
    OccurrenceStatus.CANCELLED -> "Cancelada"
}

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

    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Ocorrências, 1 = Séries
    var titleInput by remember { mutableStateOf("") }
    var recurrenceType by remember { mutableStateOf(RecurrenceType.DAILY) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val draftRulesBySeriesId = remember { mutableStateMapOf<Long, RecurrenceType>() }

    val zoneId = remember { ZoneId.systemDefault() }
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM • HH:mm", Locale.forLanguageTag("pt-BR")).withZone(zoneId)
    }

    val seriesById = remember(seriesList) { seriesList.associateBy { it.id } }

    val recurrenceOptions = remember {
        listOf(
            RecurrenceType.NONE to "Única",
            RecurrenceType.DAILY to "Diária",
            RecurrenceType.WEEKLY to "Semanal",
            RecurrenceType.MONTHLY to "Mensal"
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tarefas",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${seriesList.size} série(s) • ${occurrenceEntities.size} ocorrência(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onOpenLegacyTasks,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Legadas", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Card de criação rápida
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
                        text = "Nova tarefa ou série",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = {
                            titleInput = it
                            errorMessage = null
                            statusMessage = null
                        },
                        label = { Text("Título da tarefa") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = statusMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    val now = System.currentTimeMillis()
                                    val defaultTz = TimeZone.getDefault().id
                                    createTaskSeriesUseCase(
                                        series = TaskSeries(
                                            title = titleInput.trim(),
                                            recurrence = RecurrenceSpec(recurrenceType, 1),
                                            timezoneId = defaultTz,
                                            createdAtUtc = now
                                        ),
                                        initialTriggerUtc = now + 3_600_000L
                                    )
                                }.onSuccess {
                                    titleInput = ""
                                    errorMessage = null
                                    statusMessage = "Tarefa criada com sucesso!"
                                }.onFailure { err ->
                                    errorMessage = err.message ?: "Falha ao criar série."
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
                        Text("Criar tarefa", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Seletor limpo entre Ocorrências e Séries
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Ocorrências (${occurrenceEntities.size})") }
                )
                SegmentedButton(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Séries (${seriesList.size})") }
                )
            }
        }

        if (selectedSection == 0) {
            if (occurrenceEntities.isEmpty()) {
                item {
                    EmptyTasksPlaceholder(
                        title = "Nenhuma ocorrência agendada",
                        subtitle = "Crie uma tarefa acima para gerar sua próxima ocorrência."
                    )
                }
            } else {
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
                    val seriesTitle = matchingSeries?.title ?: "Tarefa #${domainOcc.taskSeriesId}"
                    val recurrenceLabel = matchingSeries?.recurrence?.type?.toPtLabel()

                    val (badgeBg, badgeFg) = when (domainOcc.status) {
                        OccurrenceStatus.COMPLETED ->
                            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        OccurrenceStatus.CANCELLED ->
                            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                        OccurrenceStatus.SNOOZED ->
                            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                        OccurrenceStatus.PENDING ->
                            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = seriesTitle.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = badgeBg
                                ) {
                                    Text(
                                        text = domainOcc.status.toPtLabel(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = badgeFg,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = whenLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (recurrenceLabel != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Repeat,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = recurrenceLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (domainOcc.status != OccurrenceStatus.COMPLETED) {
                                    Button(
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
                                        },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text("Concluir", style = MaterialTheme.typography.labelMedium)
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
                                        },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("Adiar 15m", style = MaterialTheme.typography.labelMedium)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                reopenOccurrenceUseCase(domainOcc)
                                            }
                                        },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text("Reabrir", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                if (domainOcc.status != OccurrenceStatus.CANCELLED) {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                cancelOccurrenceUseCase(domainOcc)
                                            }
                                        },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text("Cancelar", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (seriesList.isEmpty()) {
                item {
                    EmptyTasksPlaceholder(
                        title = "Nenhuma série ativa",
                        subtitle = "Séries recorrentes permitem gerar ocorrências automáticas."
                    )
                }
            } else {
                items(seriesList, key = { "series_${it.id}" }) { seriesItem ->
                    val selectedType = draftRulesBySeriesId[seriesItem.id] ?: seriesItem.recurrence.type
                    val hasChanges = selectedType != seriesItem.recurrence.type

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = seriesItem.title.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${seriesItem.recurrence.type.toPtLabel()} (x${seriesItem.recurrence.interval})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                            statusMessage = "Regra de \"${updated.title}\" atualizada para ${updated.recurrence.type.toPtLabel()}."
                                        }.onFailure { err ->
                                            errorMessage = err.message ?: "Falha ao atualizar regra."
                                        }
                                    }
                                },
                                enabled = hasChanges,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("Salvar alteração")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksPlaceholder(
    title: String,
    subtitle: String
) {
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
                imageVector = Icons.Rounded.TaskAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
