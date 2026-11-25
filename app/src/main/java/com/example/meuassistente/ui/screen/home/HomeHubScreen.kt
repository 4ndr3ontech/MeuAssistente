package com.example.meuassistente.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

    var draftInput by remember { mutableStateOf("") }
    var currentProposal by remember { mutableStateOf<DraftActionProposal?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Meu Assistente",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Resumo operacional offline-first",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Pendentes",
                value = snapshot.pendingOccurrencesCount.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Atrasadas",
                value = snapshot.overdueOccurrencesCount.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Concluídas",
                value = "${snapshot.completionRatePercent}%",
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Financeiro em aberto",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FinanceRules.formatCentsBrl(snapshot.openObligationsCents),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Rascunho rápido (IA local)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = draftInput,
                    onValueChange = {
                        draftInput = it
                        currentProposal = null
                        feedbackMessage = null
                    },
                    label = { Text("Ex.: Pagar internet R$ 119,90 ou Reunião às 15h") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (draftInput.isNotBlank()) {
                                currentProposal = extractDraftUseCase(draftInput)
                                feedbackMessage = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Analisar texto")
                    }
                    if (currentProposal != null) {
                        Button(
                            onClick = {
                                val proposal = currentProposal ?: return@Button
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
                                        feedbackMessage = "Item salvo com confirmação!"
                                    }.onFailure { err ->
                                        feedbackMessage = err.message ?: "Erro ao salvar rascunho."
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmar")
                        }
                    }
                }

                currentProposal?.let { proposal ->
                    val typeLabel = when (proposal.actionType) {
                        ProposedActionType.CREATE_OBLIGATION -> "Conta financeira (${FinanceRules.formatCentsBrl(proposal.suggestedAmountCents ?: 0L)})"
                        ProposedActionType.CREATE_EVENT -> "Compromisso na Agenda"
                        ProposedActionType.CREATE_NOTE -> "Nota rápida"
                        ProposedActionType.CREATE_TASK -> "Tarefa"
                    }
                    Text(
                        text = "Sugestão: $typeLabel — \"${proposal.title}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                feedbackMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateToAdd,
                modifier = Modifier.weight(1f)
            ) {
                Text("Novo item")
            }
            OutlinedButton(
                onClick = onNavigateToTasks,
                modifier = Modifier.weight(1f)
            ) {
                Text("Ver tarefas")
            }
            OutlinedButton(
                onClick = onNavigateToMore,
                modifier = Modifier.weight(1f)
            ) {
                Text("Mais")
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
