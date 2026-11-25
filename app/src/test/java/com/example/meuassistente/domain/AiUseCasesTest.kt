package com.example.meuassistente.domain

import com.example.meuassistente.domain.usecase.ai.ExtractLocalDraftProposalUseCase
import com.example.meuassistente.domain.usecase.ai.ProposedActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiUseCasesTest {

    @Test
    fun extractsTaskEventObligationAndNoteProposalsWithConfirmationFlag() {
        val extractor = ExtractLocalDraftProposalUseCase()

        val obligation = extractor("Pagar conta de luz R$ 142,50")
        assertEquals(ProposedActionType.CREATE_OBLIGATION, obligation.actionType)
        assertEquals(14250L, obligation.suggestedAmountCents)
        assertTrue(obligation.requiresUserConfirmation)

        val event = extractor("Reunião de arquitetura amanhã às 15h")
        assertEquals(ProposedActionType.CREATE_EVENT, event.actionType)

        val note = extractor("Anotar ideias para o release v2.3")
        assertEquals(ProposedActionType.CREATE_NOTE, note.actionType)

        val task = extractor("Atualizar documentação técnica")
        assertEquals(ProposedActionType.CREATE_TASK, task.actionType)
    }
}
