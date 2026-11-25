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
import androidx.compose.material3.FilterChip
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
import com.example.meuassistente.domain.model.Checklist
import com.example.meuassistente.domain.model.NoteItem
import com.example.meuassistente.domain.repository.ChecklistRepository
import com.example.meuassistente.domain.repository.NoteRepository
import com.example.meuassistente.domain.usecase.list.CreateChecklistUseCase
import com.example.meuassistente.domain.usecase.list.SaveNoteUseCase
import kotlinx.coroutines.launch

@Composable
fun ListsAndNotesScreen(
    checklistRepository: ChecklistRepository,
    noteRepository: NoteRepository,
    createChecklistUseCase: CreateChecklistUseCase,
    saveNoteUseCase: SaveNoteUseCase,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val checklists by checklistRepository.observeActiveChecklists().collectAsState(initial = emptyList())
    val notes by noteRepository.observeActiveNotes().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf("LISTAS") }
    var listTitleInput by remember { mutableStateOf("") }
    var noteTitleInput by remember { mutableStateOf("") }
    var noteBodyInput by remember { mutableStateOf("") }

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
                text = "Listas e Notas",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(onClick = onBack) {
                Text("Voltar")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedTab == "LISTAS",
                onClick = { selectedTab = "LISTAS" },
                label = { Text("Listas (${checklists.size})") }
            )
            FilterChip(
                selected = selectedTab == "NOTAS",
                onClick = { selectedTab = "NOTAS" },
                label = { Text("Notas (${notes.size})") }
            )
        }

        if (selectedTab == "LISTAS") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = listTitleInput,
                        onValueChange = { listTitleInput = it },
                        label = { Text("Nome da lista") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (listTitleInput.isNotBlank()) {
                                scope.launch {
                                    createChecklistUseCase(
                                        Checklist(
                                            title = listTitleInput,
                                            createdAtUtc = System.currentTimeMillis()
                                        )
                                    )
                                    listTitleInput = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Criar lista")
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(checklists, key = { it.id }) { checklist ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = checklist.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = noteTitleInput,
                        onValueChange = { noteTitleInput = it },
                        label = { Text("Título da nota") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteBodyInput,
                        onValueChange = { noteBodyInput = it },
                        label = { Text("Conteúdo da nota") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (noteTitleInput.isNotBlank() || noteBodyInput.isNotBlank()) {
                                scope.launch {
                                    saveNoteUseCase(
                                        NoteItem(
                                            title = noteTitleInput,
                                            body = noteBodyInput,
                                            updatedAtUtc = System.currentTimeMillis()
                                        )
                                    )
                                    noteTitleInput = ""
                                    noteBodyInput = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salvar nota")
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (note.body.isNotBlank()) {
                                Text(
                                    text = note.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
