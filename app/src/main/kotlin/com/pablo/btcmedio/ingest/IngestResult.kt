package com.pablo.btcmedio.ingest

import com.pablo.btcmedio.core.draft.TransactionDraft

/**
 * O resultado de interpretar uma entrada. `Failed` também é aproveitável:
 * a tela abre o formulário vazio com a explicação, nunca um beco sem saída.
 */
sealed interface IngestResult {
    data class Ready(val draft: TransactionDraft) : IngestResult
    data class Failed(val message: String) : IngestResult
}
