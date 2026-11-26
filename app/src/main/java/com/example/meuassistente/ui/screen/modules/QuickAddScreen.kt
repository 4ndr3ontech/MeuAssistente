package com.example.meuassistente.ui.screen.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import java.util.Locale
import java.util.TimeZone

private enum class QuickCategory(val label: String, val icon: ImageVector) {
    TASK("Tarefa", Icons.Rounded.TaskAlt),
    EVENT("Agenda", Icons.Rounded.Event),
    FINANCE("Financeiro", Icons.Rounded.Payments),
    NOTE("Nota", Icons.Rounded.Description)
}

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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho
        Column {
            Text(
                text = "Adicionar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Crie por texto livre ou escolha o tipo de registro",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Card 1: Rascunho inteligente
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
                        text = "Rascunho inteligente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedTextField(
                    value = draftText,
                    onValueChange = {
                        draftText = it
                        proposal = null
                        feedbackMessage = null
                    },
                    placeholder = {
                        Text("Ex.: Pagar luz R$ 185,50 ou Reunião amanhã")
                    },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
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
                                        Locale.forLanguageTag("pt-BR"),
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
                    enabled = draftText.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Extrair rascunho", fontWeight = FontWeight.SemiBold)
                }

                proposal?.let { item ->
                    val humanType = when (item.actionType) {
                        ProposedActionType.CREATE_OBLIGATION ->
                            "Conta financeira • ${FinanceRules.formatCentsBrl(item.suggestedAmountCents ?: 0L)}"
                        ProposedActionType.CREATE_EVENT -> "Compromisso na Agenda"
                        ProposedActionType.CREATE_NOTE -> "Nota rápida"
                        ProposedActionType.CREATE_TASK -> "Tarefa"
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
                                text = humanType,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { proposal = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Descartar")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            saveProposal(
                                                proposal = item,
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
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Salvar direto")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 2: Criação guiada com grade 2x2 simétrica
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
                    text = "Criação guiada",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Grade 2x2 para os 4 módulos
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickCategory.entries.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { category ->
                                val isSelected = selectedCategory == category
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.background
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            selectedCategory = category
                                            feedbackMessage = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Text(
                                            text = category.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedCategory == QuickCategory.TASK) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Repetição",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                }

                if (selectedCategory == QuickCategory.FINANCE) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it
                            feedbackMessage = null
                        },
                        label = { Text("Valor em R$ (ex.: 99,90)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
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
                                    "Observações do compromisso (opcional)"
                                } else {
                                    "Conteúdo da nota"
                                }
                            )
                        },
                        minLines = if (selectedCategory == QuickCategory.NOTE) 3 else 1,
                        shape = RoundedCornerShape(14.dp),
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
                                            title = titleInput.trim(),
                                            recurrence = RecurrenceSpec(selectedRecurrence, 1),
                                            timezoneId = tz,
                                            createdAtUtc = now
                                        ),
                                        initialTriggerUtc = now + 3_600_000L
                                    )
                                    QuickCategory.EVENT -> saveCalendarEventUseCase(
                                        CalendarEvent(
                                            title = titleInput.trim(),
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
                                                title = titleInput.trim(),
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
                                            title = titleInput.trim(),
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
                                feedbackMessage = "${selectedCategory.label} salvo com sucesso!"
                            }.onFailure { err ->
                                isError = true
                                feedbackMessage = err.message ?: "Não foi possível salvar."
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
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Salvar ${selectedCategory.label.lowercase()}",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                feedbackMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isError) {
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
                                tint = if (isError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isError) {
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

        TextButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text("Voltar para Início")
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
