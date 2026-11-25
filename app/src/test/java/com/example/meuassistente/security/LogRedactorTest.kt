package com.example.meuassistente.security

import com.example.meuassistente.data.security.LogRedactor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRedactorTest {

    @Test
    fun masksApiKeysAndBearerTokensInDiagnosticMessages() {
        val raw = "Falha HTTP com Bearer abc123xyz987 e chave sk-proj1234567890abcdef ou AIzaSyD12345678901234567890"
        val sanitized = LogRedactor.sanitize(raw)

        assertFalse(sanitized.contains("abc123xyz987"))
        assertFalse(sanitized.contains("sk-proj1234567890abcdef"))
        assertFalse(sanitized.contains("AIzaSyD12345678901234567890"))
        assertTrue(sanitized.contains("[REDACTED]"))
        assertTrue(sanitized.contains("[REDACTED_KEY]"))
    }
}
