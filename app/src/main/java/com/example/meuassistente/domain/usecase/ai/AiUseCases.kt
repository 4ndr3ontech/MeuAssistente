package com.example.meuassistente.domain.usecase.ai

enum class ProposedActionType {
    CREATE_TASK,
    CREATE_EVENT,
    CREATE_OBLIGATION,
    CREATE_NOTE
}

data class DraftActionProposal(
    val actionType: ProposedActionType,
    val title: String,
    val rawInput: String,
    val suggestedAmountCents: Long? = null,
    val requiresUserConfirmation: Boolean = true
)

class ExtractLocalDraftProposalUseCase {

    operator fun invoke(rawInput: String): DraftActionProposal {
        val cleaned = rawInput.trim()
        require(cleaned.isNotBlank()) { "O texto para análise não pode estar em branco." }

        val lower = cleaned.lowercase()
        val centsMatch = Regex("""(?:r\$\s*)?(\d+)(?:[,.](\d{1,2}))?""").find(lower)
        val amountCents: Long? = centsMatch?.let { match ->
            val reais = match.groupValues[1].toLongOrNull() ?: 0L
            val centsRaw = match.groupValues[2]
            val cents = when (centsRaw.length) {
                0 -> 0L
                1 -> (centsRaw.toLongOrNull() ?: 0L) * 10L
                else -> centsRaw.take(2).toLongOrNull() ?: 0L
            }
            val total = reais * 100L + cents
            if (total > 0L) total else null
        }

        val actionType = when {
            amountCents != null && (lower.contains("pagar") || lower.contains("conta") || lower.contains("boleto") || lower.contains("fatura")) ->
                ProposedActionType.CREATE_OBLIGATION
            lower.contains("reunião") || lower.contains("reuniao") || lower.contains("consulta") || lower.contains("compromisso") || lower.contains("agenda") ->
                ProposedActionType.CREATE_EVENT
            lower.contains("nota") || lower.contains("lembrete de ideia") || lower.contains("anotar") ->
                ProposedActionType.CREATE_NOTE
            else -> ProposedActionType.CREATE_TASK
        }

        val normalizedTitle = cleaned
            .replace(Regex("""(?i)\b(pagar|conta de|boleto de|fatura de|reunião de|reuniao de|anotar)\b"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { cleaned }
            .take(80)

        return DraftActionProposal(
            actionType = actionType,
            title = normalizedTitle,
            rawInput = cleaned,
            suggestedAmountCents = if (actionType == ProposedActionType.CREATE_OBLIGATION) amountCents else null,
            requiresUserConfirmation = true
        )
    }
}
