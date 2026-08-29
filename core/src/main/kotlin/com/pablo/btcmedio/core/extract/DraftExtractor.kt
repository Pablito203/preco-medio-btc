package com.pablo.btcmedio.core.extract

import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.EntrySource
import java.time.LocalDate

/**
 * Transforma texto em rascunho de transação.
 *
 * A interface vive em `:core` para que o extrator baseado em Gemini Nano —
 * que precisa do Android — possa ser substituído por um duplo nos testes.
 */
interface DraftExtractor {
    val name: String

    suspend fun isAvailable(): Boolean

    /** `null` quando não conseguiu extrair nada de aproveitável. */
    suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft?
}

/**
 * Executa os extratores em ordem, parando no primeiro rascunho completo.
 *
 * Nunca lança e nunca devolve `null`: no pior caso entrega um rascunho vazio
 * carregando o texto original, para que o usuário veja o que foi reconhecido
 * e complete à mão.
 */
class ExtractorChain(private val extractors: List<DraftExtractor>) {

    suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft {
        var best: TransactionDraft? = null

        for (extractor in extractors) {
            val candidate = try {
                if (!extractor.isAvailable()) continue
                extractor.extract(text, source, today)
            } catch (e: Exception) {
                continue
            } ?: continue

            best = merge(best, candidate)
            if (best.isComplete()) break
        }

        return (best ?: TransactionDraft()).copy(source = source, rawText = text)
    }

    /** Campos já preenchidos vencem: o primeiro extrator tem prioridade. */
    private fun merge(base: TransactionDraft?, next: TransactionDraft): TransactionDraft {
        if (base == null) return next
        return base.copy(
            occurredAt = base.occurredAt ?: next.occurredAt,
            fiatAmountCents = base.fiatAmountCents ?: next.fiatAmountCents,
            feeCents = base.feeCents ?: next.feeCents,
            satoshis = base.satoshis ?: next.satoshis,
            unitPriceCents = base.unitPriceCents ?: next.unitPriceCents,
            note = base.note ?: next.note,
        )
    }
}
