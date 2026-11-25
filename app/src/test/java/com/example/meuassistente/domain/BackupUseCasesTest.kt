package com.example.meuassistente.domain

import com.example.meuassistente.domain.usecase.backup.BuildBackupPayloadUseCase
import com.example.meuassistente.domain.usecase.backup.VerifyAndParseBackupUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupUseCasesTest {

    @Test
    fun buildsVerifiesIntactBackupAndRejectsTamperedPayload() {
        val builder = BuildBackupPayloadUseCase()
        val verifier = VerifyAndParseBackupUseCase()

        val envelope = builder(
            exportedAtUtc = 1_800_000_000_000L,
            tasksCount = 3,
            obligationsCount = 2,
            notesCount = 1,
            serializedItemsJson = """{"items":["task-1"]}"""
        )
        val rawJson = builder.toJsonString(envelope)

        val validResult = verifier(rawJson)
        assertTrue(validResult.isSuccess)
        assertEquals(3, validResult.getOrThrow().schemaVersion)

        val tamperedJson = rawJson.replace("task-1", "task-corrupted")
        val invalidResult = verifier(tamperedJson)
        assertTrue(invalidResult.isFailure)
    }
}
