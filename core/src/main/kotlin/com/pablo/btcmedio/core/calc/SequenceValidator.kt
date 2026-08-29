package com.pablo.btcmedio.core.calc

import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType

sealed interface SequenceCheck {
    data object Ok : SequenceCheck

    data class Oversold(
        val transactionId: String,
        val occurredAt: Long,
        val availableSats: Long,
        val requestedSats: Long,
    ) : SequenceCheck
}

/**
 * Verifica se a sequência inteira é consistente: nenhuma venda pode exceder
 * o saldo acumulado até o instante em que ocorre.
 *
 * Validar a sequência inteira (e não a transação isolada) é o que impede que
 * editar ou excluir uma compra antiga deixe uma venda posterior órfã de saldo.
 */
object SequenceValidator {

    fun check(transactions: List<Transaction>): SequenceCheck {
        var balanceSats = 0L
        for (t in transactions.sortedWith(compareBy({ it.occurredAt }, { it.id }))) {
            when (t.type) {
                TransactionType.BUY -> balanceSats += t.satoshis
                TransactionType.SELL -> {
                    if (t.satoshis > balanceSats) {
                        return SequenceCheck.Oversold(
                            transactionId = t.id,
                            occurredAt = t.occurredAt,
                            availableSats = balanceSats,
                            requestedSats = t.satoshis,
                        )
                    }
                    balanceSats -= t.satoshis
                }
            }
        }
        return SequenceCheck.Ok
    }
}
