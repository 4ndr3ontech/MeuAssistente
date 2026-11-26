package com.example.meuassistente.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.meuassistente.domain.model.CalendarEvent
import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.NoteItem
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.model.RecurrenceSpec
import com.example.meuassistente.domain.model.RecurrenceType
import com.example.meuassistente.domain.model.TaskSeries
import com.example.meuassistente.domain.rule.FinanceRules
import com.example.meuassistente.domain.usecase.agenda.SaveCalendarEventUseCase
import com.example.meuassistente.domain.usecase.ai.DraftActionProposal
import com.example.meuassistente.domain.usecase.ai.ExtractLocalDraftProposalUseCase
import com.example.meuassistente.domain.usecase.ai.ProposedActionType
import com.example.meuassistente.domain.usecase.finance.CreateFinancialObligationUseCase
import com.example.meuassistente.domain.usecase.list.SaveNoteUseCase
import com.example.meuassistente.domain.usecase.statistics.ComputeProductivitySnapshotUseCase
import com.example.meuassistente.domain.usecase.statistics.ProductivitySnapshot
import com.example.meuassistente.domain.usecase.task.CreateTaskSeriesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun HomeHubScreen(
    snapshotFlow: Flow<ProductivitySnapshot>,
    extractDraftUseCase: ExtractLocalDraftProposalUseCase,
    createTaskSeriesUseCase: CreateTaskSeriesUseCase,
    saveCalendarEventUseCase: SaveCalendarEventUseCase,
    createObligationUseCase: CreateFinancialObligationUseCase,
    saveNoteUseCase: SaveNoteUseCase,
    onNavigateToTasks: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val computeSnapshot = remember { ComputeProductivitySnapshotUseCase() }
    val snapshot by snapshotFlow.collectAsState(
        initial = computeSnapshot(
            occurrences = emptyList(),
            routineExecutions = emptyList(),
            obligations = emptyList(),
            payments = emptyList(),
            nowUtc = 0L
        )
    )

    val formattedToday = remember {
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))
        sdf.format(Date()).replaceFirstChar { it.uppercase() }
    }

    var draftInput by remember { mutableStateOf("") }
    var currentProposal by remember { mutableStateOf<DraftActionProposal?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Meu Assistente",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedToday,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Offline",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Pendentes",
                value = snapshot.pendingOccurrencesCount.toString(),
                onClick = onNavigateToTasks,
                modifier = Modifier.weight(1f)
            )

            val hasOverdue = snapshot.overdueOccurrencesCount > 0
            MetricCard(
                title = "Atrasadas",
                value = snapshot.overdueOccurrencesCount.toString(),
                containerColor = if (hasOverdue) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (hasOverdue) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                borderColor = if (hasOverdue) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                onClick = onNavigateToTasks,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Concluídas",
                value = "${snapshot.completionRatePercent}%",
                progress = snapshot.completionRatePercent / 100f,
                onClick = onNavigateToTasks,
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onNavigateToMore)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Financeiro em aberto",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FinanceRules.formatCentsBrl(snapshot.openObligationsCents),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                FilledTonalIconButton(onClick = onNavigateToMore) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Ver módulos"
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Rascunho rápido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedTextField(
                    value = draftInput,
                    onValueChange = {
                        draftInput = it
                        currentProposal = null
                        feedbackMessage = null
                    },
                    placeholder = {
                        Text(
                            text = "Ex.: Pagar internet R$ 119,90 ou Reunião às 15h",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (draftInput.isNotBlank()) {
                            currentProposal = extractDraftUseCase(draftInput)
                            feedbackMessage = null
                        }
                    },
                    enabled = draftInput.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Analisar texto",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                currentProposal?.let { proposal ->
                    val typeLabel = when (proposal.actionType) {
                        ProposedActionType.CREATE_OBLIGATION ->
                            "Conta financeira • ${FinanceRules.formatCentsBrl(proposal.suggestedAmountCents ?: 0L)}"
                        ProposedActionType.CREATE_EVENT ->
                            "Compromisso na Agenda"
                        ProposedActionType.CREATE_NOTE ->
                            "Nota rápida"
                        ProposedActionType.CREATE_TASK ->
                            "Tarefa"
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = proposal.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { currentProposal = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Descartar")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val now = System.currentTimeMillis()
                                            val tz = TimeZone.getDefault().id
                                            runCatching {
                                                when (proposal.actionType) {
                                                    ProposedActionType.CREATE_OBLIGATION -> createObligationUseCase(
                                                        FinancialObligation(
                                                            title = proposal.title,
                                                            amountCents = proposal.suggestedAmountCents ?: 100L,
                                                            dueAtUtc = now + 3L * 24 * 3600_000L,
                                                            timezoneId = tz,
                                                            status = ObligationStatus.OPEN,
                                                            createdAtUtc = now
                                                        )
                                                    )
                                                    ProposedActionType.CREATE_EVENT -> saveCalendarEventUseCase(
                                                        CalendarEvent(
                                                            title = proposal.title,
                                                            description = proposal.rawInput,
                                                            startAtUtc = now + 3_600_000L,
                                                            endAtUtc = now + 7_200_000L,
                                                            timezoneId = tz,
                                                            status = "SCHEDULED",
                                                            createdAtUtc = now
                                                        )
                                                    )
                                                    ProposedActionType.CREATE_NOTE -> saveNoteUseCase(
                                                        NoteItem(
                                                            title = proposal.title,
                                                            body = proposal.rawInput,
                                                            updatedAtUtc = now
                                                        )
                                                    )
                                                    ProposedActionType.CREATE_TASK -> createTaskSeriesUseCase(
                                                        series = TaskSeries(
                                                            title = proposal.title,
                                                            recurrence = RecurrenceSpec(RecurrenceType.NONE, 1),
                                                            timezoneId = tz,
                                                            createdAtUtc = now
                                                        ),
                                                        initialTriggerUtc = now + 3_600_000L
                                                    )
                                                }
                                            }.onSuccess {
                                                draftInput = ""
                                                currentProposal = null
                                                isFeedbackError = false
                                                feedbackMessage = "Item salvo com sucesso!"
                                            }.onFailure { err ->
                                                isFeedbackError = true
                                                feedbackMessage = err.message ?: "Erro ao salvar rascunho."
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Confirmar")
                                }
                            }
                        }
                    }
                }

                feedbackMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFeedbackError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isFeedbackError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isFeedbackError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNavigateToAdd,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.weight(1.15f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Novo item",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = onNavigateToTasks,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.TaskAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Tarefas",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = onNavigateToMore,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier.weight(0.85f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "Mais",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}
