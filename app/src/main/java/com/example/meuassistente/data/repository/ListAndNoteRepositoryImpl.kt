package com.example.meuassistente.data.repository

import com.example.meuassistente.data.local.dao.ChecklistDao
import com.example.meuassistente.data.local.dao.NoteDao
import com.example.meuassistente.data.local.entity.ChecklistEntity
import com.example.meuassistente.data.local.entity.ChecklistItemEntity
import com.example.meuassistente.data.local.entity.NoteItemEntity
import com.example.meuassistente.domain.model.Checklist
import com.example.meuassistente.domain.model.ChecklistItem
import com.example.meuassistente.domain.model.NoteItem
import com.example.meuassistente.domain.repository.ChecklistRepository
import com.example.meuassistente.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChecklistRepositoryImpl(
    private val checklistDao: ChecklistDao
) : ChecklistRepository {

    override fun observeActiveChecklists(): Flow<List<Checklist>> {
        return checklistDao.observeActiveChecklists().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeItems(checklistId: Long): Flow<List<ChecklistItem>> {
        return checklistDao.observeItems(checklistId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun createChecklist(checklist: Checklist): Long {
        return checklistDao.insertChecklist(checklist.toEntity())
    }

    override suspend fun addItem(item: ChecklistItem): Long {
        return checklistDao.insertItem(item.toEntity())
    }

    override suspend fun updateItem(item: ChecklistItem) {
        checklistDao.updateItem(item.toEntity())
    }

    private fun ChecklistEntity.toDomain(): Checklist = Checklist(
        id = id,
        title = title,
        isArchived = isArchived,
        createdAtUtc = createdAtUtc
    )

    private fun Checklist.toEntity(): ChecklistEntity = ChecklistEntity(
        id = id,
        title = title,
        isArchived = isArchived,
        createdAtUtc = createdAtUtc
    )

    private fun ChecklistItemEntity.toDomain(): ChecklistItem = ChecklistItem(
        id = id,
        checklistId = checklistId,
        label = label,
        isChecked = isChecked,
        sortOrder = sortOrder
    )

    private fun ChecklistItem.toEntity(): ChecklistItemEntity = ChecklistItemEntity(
        id = id,
        checklistId = checklistId,
        label = label,
        isChecked = isChecked,
        sortOrder = sortOrder
    )
}

class NoteRepositoryImpl(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun observeActiveNotes(): Flow<List<NoteItem>> {
        return noteDao.observeActiveNotes().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveNote(note: NoteItem): Long {
        return noteDao.insertNote(note.toEntity())
    }

    override suspend fun updateNote(note: NoteItem) {
        noteDao.updateNote(note.toEntity())
    }

    private fun NoteItemEntity.toDomain(): NoteItem = NoteItem(
        id = id,
        title = title,
        body = body,
        updatedAtUtc = updatedAtUtc,
        isArchived = isArchived
    )

    private fun NoteItem.toEntity(): NoteItemEntity = NoteItemEntity(
        id = id,
        title = title,
        body = body,
        updatedAtUtc = updatedAtUtc,
        isArchived = isArchived
    )
}
