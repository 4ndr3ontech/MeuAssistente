package com.example.meuassistente.domain.usecase.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

@Serializable
data class BackupExportEnvelope(
    val schemaVersion: Int = 3,
    val exportedAtUtc: Long,
    val tasksCount: Int,
    val obligationsCount: Int,
    val notesCount: Int,
    val jsonBody: String,
    val sha256Checksum: String
)

class BuildBackupPayloadUseCase(
    private val json: Json = Json { prettyPrint = false }
) {
    operator fun invoke(
        exportedAtUtc: Long,
        tasksCount: Int,
        obligationsCount: Int,
        notesCount: Int,
        serializedItemsJson: String
    ): BackupExportEnvelope {
        val checksum = sha256Hex(serializedItemsJson)
        return BackupExportEnvelope(
            schemaVersion = 3,
            exportedAtUtc = exportedAtUtc,
            tasksCount = tasksCount,
            obligationsCount = obligationsCount,
            notesCount = notesCount,
            jsonBody = serializedItemsJson,
            sha256Checksum = checksum
        )
    }

    fun toJsonString(envelope: BackupExportEnvelope): String = json.encodeToString(envelope)

    companion object {
        fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

class VerifyAndParseBackupUseCase(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    operator fun invoke(rawJson: String): Result<BackupExportEnvelope> {
        return runCatching {
            val envelope = json.decodeFromString<BackupExportEnvelope>(rawJson)
            val actualChecksum = BuildBackupPayloadUseCase.sha256Hex(envelope.jsonBody)
            require(actualChecksum.equals(envelope.sha256Checksum, ignoreCase = true)) {
                "Checksum SHA-256 inválido: o conteúdo do backup foi alterado ou corrompido."
            }
            envelope
        }
    }
}
