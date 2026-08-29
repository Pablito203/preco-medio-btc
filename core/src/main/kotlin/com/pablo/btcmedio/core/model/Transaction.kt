package com.pablo.btcmedio.core.model

enum class TransactionType { BUY, SELL }

enum class EntrySource { MANUAL, IMAGE, AUDIO, TEXT }

/**
 * Uma negociação de Bitcoin.
 *
 * Valores em reais são sempre centavos; quantidades de Bitcoin são sempre satoshis.
 * Ponto flutuante nunca entra aqui.
 */
data class Transaction(
    val id: String,
    val type: TransactionType,
    /** Instante da negociação na exchange, em epoch millis. */
    val occurredAt: Long,
    /**
     * O dinheiro que se moveu na conta do usuário: debitado na compra
     * (já contendo a taxa) e creditado na venda (já descontada a taxa).
     */
    val fiatAmountCents: Long,
    /** Taxa cobrada. `null` significa desconhecida; `0` significa sem taxa. */
    val feeCents: Long?,
    val satoshis: Long,
    /** Cotação R$/BTC informada pela exchange. */
    val unitPriceCents: Long,
    val source: EntrySource,
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
