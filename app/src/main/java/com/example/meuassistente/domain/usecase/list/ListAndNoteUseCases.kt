package com.example.meuassistente.domain.usecase.list

import com.example.meuassistente.domain.model.Checklist
import com.example.meuassistente.domain.model.ChecklistItem
import com.example.meuassistente.domain.model.NoteItem
import com.example.meuassistente.domain.repository.ChecklistRepository
import com.example.meuassistente.domain.repository.NoteRepository

class CreateChecklistUseCase(
    private val repository: ChecklistRepository
) {
    suspend operator fun invoke(checklist: Checklist): Long {
        require(checklist.title.isNotBlank()) { "O título da lista não pode estar em branco." }
        return repository.createChecklist(checklist.copy(title = checklist.title.trim()))
    }
}

class AddChecklistItemUseCase(
    private val repository: ChecklistRepository
) {
    suspend operator fun invoke(
        checklistId: Long,
        label: String,
        sortOrder: Int = 0
    ): Long {
        require(checklistId > 0L) { "A lista precisa ter um ID válido." }
        require(label.isNotBlank()) { "O texto do item da lista não pode estar em branco." }
        return repository.addItem(
            ChecklistItem(
                checklistId = checklistId,
                label = label.trim(),
                isChecked = false,
                sortOrder = sortOrder
            )
        )
    }
}

class ToggleChecklistItemUseCase(
    private val repository: ChecklistRepository
) {
    suspend operator fun invoke(item: ChecklistItem): ChecklistItem {
        val toggled = item.copy(isChecked = !item.isChecked)
        repository.updateItem(toggled)
        return toggled
    }
}

class SaveNoteUseCase(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: NoteItem): Long {
        require(note.title.isNotBlank() || note.body.isNotBlank()) {
            "A nota precisa conter título ou conteúdo."
        }
        val normalizedTitle = if (note.title.isBlank()) "Nota rápida" else note.title.trim()
        val prepared = note.copy(
            title = normalizedTitle,
            body = note.body.trim()
        )
        return if (note.id == 0L) {
            repository.saveNote(prepared)
        } else {
            repository.updateNote(prepared)
            note.id
        }
    }
}
