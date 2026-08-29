package com.pablo.btcmedio.core.draft

import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import java.util.UUID

enum class DraftField { DATE, FIAT, FEE, SATS, PRICE }

/**
 * Rascunho de transação: o formato intermediário entre qualquer fonte de
 * entrada e a gravação. Todo campo é opcional porque uma extração parcial
 * ainda é útil — ela chega ao usuário como formulário pré-preenchido.
 */
data class TransactionDraft(
    val type: TransactionType = TransactionType.BUY,
    val occurredAt: Long? = null,
    val fiatAmountCents: Long? = null,
    val feeCents: Long? = null,
    val satoshis: Long? = null,
    val unitPriceCents: Long? = null,
    val source: EntrySource = EntrySource.MANUAL,
    val note: String? = null,
    /** Texto original reconhecido, exibido na confirmação para conferência. */
    val rawText: String? = null,
) {
    /** O valor efetivamente convertido em Bitcoin, sem a taxa. */
    fun tradedFiatCents(): Long? {
        val fiat = fiatAmountCents ?: return null
        val fee = feeCents ?: 0L
        return when (type) {
            TransactionType.BUY -> fiat - fee
            TransactionType.SELL -> fiat + fee
        }
    }

    fun isComplete(): Boolean =
        occurredAt != null && fiatAmountCents != null && satoshis != null && unitPriceCents != null

    fun toTransaction(id: String = UUID.randomUUID().toString(), now: Long): Transaction = Transaction(
        id = id,
        type = type,
        occurredAt = requireNotNull(occurredAt) { "occurredAt é obrigatório" },
        fiatAmountCents = requireNotNull(fiatAmountCents) { "fiatAmountCents é obrigatório" },
        feeCents = feeCents,
        satoshis = requireNotNull(satoshis) { "satoshis é obrigatório" },
        unitPriceCents = requireNotNull(unitPriceCents) { "unitPriceCents é obrigatório" },
        source = source,
        note = note,
        createdAt = now,
        updatedAt = now,
    )

    companion object {
        fun from(t: Transaction): TransactionDraft = TransactionDraft(
            type = t.type,
            occurredAt = t.occurredAt,
            fiatAmountCents = t.fiatAmountCents,
            feeCents = t.feeCents,
            satoshis = t.satoshis,
            unitPriceCents = t.unitPriceCents,
            source = t.source,
            note = t.note,
        )
    }
}
