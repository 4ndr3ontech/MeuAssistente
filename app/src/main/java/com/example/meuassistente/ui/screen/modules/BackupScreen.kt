package com.example.meuassistente.ui.screen.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meuassistente.domain.repository.FinanceRepository
import com.example.meuassistente.domain.repository.NoteRepository
import com.example.meuassistente.domain.repository.TaskDomainRepository
import com.example.meuassistente.domain.usecase.backup.BuildBackupPayloadUseCase
import com.example.meuassistente.domain.usecase.backup.VerifyAndParseBackupUseCase

@Composable
fun BackupScreen(
    taskDomainRepository: TaskDomainRepository,
    financeRepository: FinanceRepository,
    noteRepository: NoteRepository,
    buildBackupPayloadUseCase: BuildBackupPayloadUseCase,
    verifyAndParseBackupUseCase: VerifyAndParseBackupUseCase,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seriesList by taskDomainRepository.observeActiveSeries().collectAsState(initial = emptyList())
    val obligationsList by financeRepository.observeObligations().collectAsState(initial = emptyList())
    val notesList by noteRepository.observeActiveNotes().collectAsState(initial = emptyList())

    var payloadText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Backup e Integridade",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(onClick = onBack) {
                Text("Voltar")
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
                    text = "Resumo local: ${seriesList.size} séries • ${obligationsList.size} contas • ${notesList.size} notas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = payloadText,
                    onValueChange = {
                        payloadText = it
                        statusMessage = null
                    },
                    label = { Text("JSON de backup (gerado ou colado para verificar)") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val bodyJson = """{"series":${seriesList.size},"obligations":${obligationsList.size},"notes":${notesList.size}}"""
                            val envelope = buildBackupPayloadUseCase(
                                exportedAtUtc = System.currentTimeMillis(),
                                tasksCount = seriesList.size,
                                obligationsCount = obligationsList.size,
                                notesCount = notesList.size,
                                serializedItemsJson = bodyJson
                            )
                            payloadText = buildBackupPayloadUseCase.toJsonString(envelope)
                            isError = false
                            statusMessage = "Envelope gerado com SHA-256: ${envelope.sha256Checksum.take(12)}..."
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gerar backup")
                    }

                    OutlinedButton(
                        onClick = {
                            val result = verifyAndParseBackupUseCase(payloadText)
                            result.onSuccess { env ->
                                isError = false
                                statusMessage = "Íntegro (v${env.schemaVersion}): ${env.tasksCount} séries, ${env.obligationsCount} contas, ${env.notesCount} notas."
                            }.onFailure { err ->
                                isError = true
                                statusMessage = err.message ?: "Falha na verificação SHA-256."
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Verificar")
                    }
                }

                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
