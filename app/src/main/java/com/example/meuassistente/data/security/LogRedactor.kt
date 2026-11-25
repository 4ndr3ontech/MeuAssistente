package com.example.meuassistente.data.security

object LogRedactor {

    private val bearerRegex = Regex("""(?i)\bBearer\s+[A-Za-z0-9\-._~+/]+=*""")
    private val openAiLikeRegex = Regex("""\bsk-[A-Za-z0-9_-]{12,}\b""")
    private val googleKeyRegex = Regex("""\bAIza[0-9A-Za-z\-_]{20,}\b""")

    fun sanitize(raw: String): String {
        return raw
            .replace(bearerRegex, "Bearer [REDACTED]")
            .replace(openAiLikeRegex, "[REDACTED_KEY]")
            .replace(googleKeyRegex, "[REDACTED_KEY]")
    }
}
