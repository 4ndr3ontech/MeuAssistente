package com.example.meuassistente.domain

import com.example.meuassistente.domain.model.Checklist
import com.example.meuassistente.domain.model.ChecklistItem
import com.example.meuassistente.domain.model.NoteItem
import com.example.meuassistente.domain.repository.ChecklistRepository
import com.example.meuassistente.domain.repository.NoteRepository
import com.example.meuassistente.domain.usecase.list.AddChecklistItemUseCase
import com.example.meuassistente.domain.usecase.list.CreateChecklistUseCase
import com.example.meuassistente.domain.usecase.list.SaveNoteUseCase
import com.example.meuassistente.domain.usecase.list.ToggleChecklistItemUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeChecklistRepository : ChecklistRepository {
    private val checklists = MutableStateFlow<List<Checklist>>(emptyList())
    private val items = MutableStateFlow<List<ChecklistItem>>(emptyList())
    private var nextListId = 1L
    private var nextItemId = 1L

    override fun observeActiveChecklists(): Flow<List<Checklist>> = checklists

    override fun observeItems(checklistId: Long): Flow<List<ChecklistItem>> =
        MutableStateFlow(items.value.filter { it.checklistId == checklistId })

    override suspend fun createChecklist(checklist: Checklist): Long {
        val id = nextListId++
        checklists.value = checklists.value + checklist.copy(id = id)
        return id
    }

    override suspend fun addItem(item: ChecklistItem): Long {
        val id = nextItemId++
        items.value = items.value + item.copy(id = id)
        return id
    }

    override suspend fun updateItem(item: ChecklistItem) {
        items.value = items.value.map { if (it.id == item.id) item else it }
    }

    fun currentItems(): List<ChecklistItem> = items.value
}

private class FakeNoteRepository : NoteRepository {
    private val notes = MutableStateFlow<List<NoteItem>>(emptyList())
    private var nextId = 1L

    override fun observeActiveNotes(): Flow<List<NoteItem>> = notes

    override suspend fun saveNote(note: NoteItem): Long {
        val id = nextId++
        notes.value = notes.value + note.copy(id = id)
        return id
    }

    override suspend fun updateNote(note: NoteItem) {
        notes.value = notes.value.map { if (it.id == note.id) note else it }
    }

    fun currentNotes(): List<NoteItem> = notes.value
}

class ListAndNoteUseCasesTest {

    @Test
    fun managesChecklistItemsAndNotesCleanly() = runTest {
        val checklistRepo = FakeChecklistRepository()
        val noteRepo = FakeNoteRepository()
        val createChecklist = CreateChecklistUseCase(checklistRepo)
        val addItem = AddChecklistItemUseCase(checklistRepo)
        val toggleItem = ToggleChecklistItemUseCase(checklistRepo)
        val saveNote = SaveNoteUseCase(noteRepo)

        val now = 1_800_000_000_000L
        val listId = createChecklist(Checklist(title = "Compras da semana", createdAtUtc = now))
        val itemId = addItem(checklistId = listId, label = "Café moído", sortOrder = 0)
        val createdItem = checklistRepo.currentItems().first { it.id == itemId }
        val toggled = toggleItem(createdItem)
        assertTrue(toggled.isChecked)

        val noteId = saveNote(
            NoteItem(
                title = "  Checklist de deploy  ",
                body = "Validar backup antes do release",
                updatedAtUtc = now
            )
        )
        assertEquals(1L, noteId)
        assertEquals("Checklist de deploy", noteRepo.currentNotes().first().title)
    }
}
