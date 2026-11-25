package com.example.meuassistente.ui.screen.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.meuassistente.domain.usecase.task.CreateTaskSeriesUseCase
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.TimeZone

private enum class QuickCategory(val label: String) {
    TASK("Tarefa"),
    EVENT("Agenda"),
    FINANCE("Financeiro"),
    NOTE("Nota")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddScreen(
    extractDraftUseCase: ExtractLocalDraftProposalUseCase,
    createTaskSeriesUseCase: CreateTaskSeriesUseCase,
    saveCalendarEventUseCase: SaveCalendarEventUseCase,
    createObligationUseCase: CreateFinancialObligationUseCase,
    saveNoteUseCase: SaveNoteUseCase,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var draftText by remember { mutableStateOf("") }
    var proposal by remember { mutableStateOf<DraftActionProposal?>(null) }

    var selectedCategory by remember { mutableStateOf(QuickCategory.TASK) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceType.NONE) }
    var titleInput by remember { mutableStateOf("") }
    var detailInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Adicionar",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Rascunho rápido offline ou criação guiada por módulo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Rascunho rápido (IA local)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = draftText,
                    onValueChange = {
                        draftText = it
                        proposal = null
                        feedbackMessage = null
                    },
                    label = { Text("Ex.: Pagar luz R$ 185,50 ou Reunião amanhã") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (draftText.isNotBlank()) {
                                val extracted = extractDraftUseCase(draftText)
                                proposal = extracted
                                titleInput = extracted.title
                                detailInput = extracted.rawInput
                                when (extracted.actionType) {
                                    ProposedActionType.CREATE_OBLIGATION -> {
                                        selectedCategory = QuickCategory.FINANCE
                                        val cents = extracted.suggestedAmountCents ?: 0L
                                        amountInput = String.format(
                                            java.util.Locale.forLanguageTag("pt-BR"),
                                            "%.2f",
                                            cents / 100.0
                                        )
                                    }
                                    ProposedActionType.CREATE_EVENT -> selectedCategory = QuickCategory.EVENT
                                    ProposedActionType.CREATE_NOTE -> selectedCategory = QuickCategory.NOTE
                                    ProposedActionType.CREATE_TASK -> selectedCategory = QuickCategory.TASK
                                }
                                isError = false
                                feedbackMessage = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Extrair rascunho")
                    }
                    if (proposal != null) {
                        Button(
                            onClick = {
                                val currentProposal = proposal ?: return@Button
                                scope.launch {
                                    saveProposal(
                                        proposal = currentProposal,
                                        createTaskSeriesUseCase = createTaskSeriesUseCase,
                                        saveCalendarEventUseCase = saveCalendarEventUseCase,
                                        createObligationUseCase = createObligationUseCase,
                                        saveNoteUseCase = saveNoteUseCase
                                    ).onSuccess {
                                        draftText = ""
                                        proposal = null
                                        titleInput = ""
                                        detailInput = ""
                                        amountInput = ""
                                        isError = false
                                        feedbackMessage = "Rascunho salvo com sucesso!"
                                    }.onFailure { err ->
                                        isError = true
                                        feedbackMessage = err.message ?: "Falha ao salvar rascunho."
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmar")
                        }
                    }
                }

                proposal?.let { item ->
                    val amountInfo = item.suggestedAmountCents?.let {
                        " • ${FinanceRules.formatCentsBrl(it)}"
                    } ?: ""
                    Text(
                        text = "Proposta: ${item.actionType.name}$amountInfo — \"${item.title}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Criação guiada",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = category
                                feedbackMessage = null
                            },
                            label = { Text(category.label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = {
                        titleInput = it
                        feedbackMessage = null
                    },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedCategory == QuickCategory.TASK) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            RecurrenceType.NONE to "Única",
                            RecurrenceType.DAILY to "Diária",
                            RecurrenceType.WEEKLY to "Semanal",
                            RecurrenceType.MONTHLY to "Mensal"
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = selectedRecurrence == type,
                                onClick = { selectedRecurrence = type },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                if (selectedCategory == QuickCategory.FINANCE) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it
                            feedbackMessage = null
                        },
                        label = { Text("Valor em R$ (ex.: 99,90)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedCategory == QuickCategory.EVENT || selectedCategory == QuickCategory.NOTE) {
                    OutlinedTextField(
                        value = detailInput,
                        onValueChange = { detailInput = it },
                        label = {
                            Text(
                                if (selectedCategory == QuickCategory.EVENT) {
                                    "Descrição do compromisso"
                                } else {
                                    "Conteúdo da nota"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            val now = System.currentTimeMillis()
                            val tz = TimeZone.getDefault().id
                            runCatching {
                                when (selectedCategory) {
                                    QuickCategory.TASK -> createTaskSeriesUseCase(
                                        series = TaskSeries(
                                            title = titleInput,
                                            recurrence = RecurrenceSpec(selectedRecurrence, 1),
                                            timezoneId = tz,
                                            createdAtUtc = now
                                        ),
                                        initialTriggerUtc = now + 3_600_000L
                                    )
                                    QuickCategory.EVENT -> saveCalendarEventUseCase(
                                        CalendarEvent(
                                            title = titleInput,
                                            description = detailInput.trim(),
                                            startAtUtc = now + 3_600_000L,
                                            endAtUtc = now + 7_200_000L,
                                            timezoneId = tz,
                                            status = "SCHEDULED",
                                            createdAtUtc = now
                                        )
                                    )
                                    QuickCategory.FINANCE -> {
                                        val cents = parseAmountCents(amountInput)
                                            ?: throw IllegalArgumentException("Informe um valor válido em R$.")
                                        createObligationUseCase(
                                            FinancialObligation(
                                                title = titleInput,
                                                amountCents = cents,
                                                dueAtUtc = now + 3L * 24 * 3600_000L,
                                                timezoneId = tz,
                                                status = ObligationStatus.OPEN,
                                                createdAtUtc = now
                                            )
                                        )
                                    }
                                    QuickCategory.NOTE -> saveNoteUseCase(
                                        NoteItem(
                                            title = titleInput,
                                            body = detailInput.trim(),
                                            updatedAtUtc = now
                                        )
                                    )
                                }
                            }.onSuccess {
                                titleInput = ""
                                detailInput = ""
                                amountInput = ""
                                isError = false
                                feedbackMessage = "${selectedCategory.label} salvo!"
                            }.onFailure { err ->
                                isError = true
                                feedbackMessage = err.message ?: "Não foi possível salvar."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar ${selectedCategory.label.lowercase()}")
                }

                feedbackMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ir para Início")
                }
            }
        }
    }
}

private suspend fun saveProposal(
    proposal: DraftActionProposal,
    createTaskSeriesUseCase: CreateTaskSeriesUseCase,
    saveCalendarEventUseCase: SaveCalendarEventUseCase,
    createObligationUseCase: CreateFinancialObligationUseCase,
    saveNoteUseCase: SaveNoteUseCase
): Result<Unit> = runCatching {
    val now = System.currentTimeMillis()
    val tz = TimeZone.getDefault().id
    when (proposal.actionType) {
        ProposedActionType.CREATE_OBLIGATION -> {
            createObligationUseCase(
                FinancialObligation(
                    title = proposal.title,
                    amountCents = proposal.suggestedAmountCents ?: 100L,
                    dueAtUtc = now + 3L * 24 * 3600_000L,
                    timezoneId = tz,
                    status = ObligationStatus.OPEN,
                    createdAtUtc = now
                )
            )
        }
        ProposedActionType.CREATE_EVENT -> {
            saveCalendarEventUseCase(
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
        }
        ProposedActionType.CREATE_NOTE -> {
            saveNoteUseCase(
                NoteItem(
                    title = proposal.title,
                    body = proposal.rawInput,
                    updatedAtUtc = now
                )
            )
        }
        ProposedActionType.CREATE_TASK -> {
            createTaskSeriesUseCase(
                series = TaskSeries(
                    title = proposal.title,
                    recurrence = RecurrenceSpec(RecurrenceType.NONE, 1),
                    timezoneId = tz,
                    createdAtUtc = now
                ),
                initialTriggerUtc = now + 3_600_000L
            )
        }
    }
}

private fun parseAmountCents(raw: String): Long? {
    val normalized = raw.trim()
        .replace("R$", "", ignoreCase = true)
        .replace(".", "")
        .replace(",", ".")
        .trim()
    if (normalized.isEmpty()) return null
    val bd = normalized.toBigDecimalOrNull() ?: return null
    if (bd <= BigDecimal.ZERO) return null
    return bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
}
