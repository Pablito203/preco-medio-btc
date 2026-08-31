package com.pablo.btcmedio.core.calc

import com.pablo.btcmedio.core.model.PortfolioSummary
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Custo médio ponderado: a venda reduz posição e custo proporcionalmente,
 * deixando o preço médio inalterado, e materializa o lucro ou prejuízo.
 */
object PortfolioCalculator {

    const val SATS_PER_BTC: Long = 100_000_000L

    fun summarize(transactions: List<Transaction>): PortfolioSummary {
        var costCents = 0L
        var balanceSats = 0L
        var totalBoughtCents = 0L
        var totalSoldCents = 0L
        var realizedPnlCents = 0L
        var feesPaidCents = 0L
        var firstBuyAt: Long? = null

        for (t in transactions.sortedWith(compareBy({ it.occurredAt }, { it.id }))) {
            // Fora do `when`: a venda sem saldo é ignorada logo abaixo, e a taxa
            // dela foi cobrada de qualquer jeito.
            feesPaidCents += t.feeCents ?: 0L

            when (t.type) {
                TransactionType.BUY -> {
                    if (firstBuyAt == null) firstBuyAt = t.occurredAt
                    costCents += t.fiatAmountCents
                    balanceSats += t.satoshis
                    totalBoughtCents += t.fiatAmountCents
                }

                TransactionType.SELL -> {
                    if (balanceSats <= 0L) continue
                    // Limitação defensiva: a validação impede que este estado seja gravado,
                    // mas a tela de resumo nunca deve travar por dado inconsistente em disco.
                    val soldSats = minOf(t.satoshis, balanceSats)
                    val soldCost = if (soldSats == balanceSats) {
                        costCents // zeragem exata, sem resíduo de arredondamento
                    } else {
                        BigDecimal.valueOf(costCents)
                            .multiply(BigDecimal.valueOf(soldSats))
                            .divide(BigDecimal.valueOf(balanceSats), 0, RoundingMode.HALF_UP)
                            .toLong()
                    }
                    realizedPnlCents += t.fiatAmountCents - soldCost
                    costCents -= soldCost
                    balanceSats -= soldSats
                    totalSoldCents += t.fiatAmountCents
                }
            }
        }

        val averagePriceCents = if (balanceSats > 0L) {
            BigDecimal.valueOf(costCents)
                .multiply(BigDecimal.valueOf(SATS_PER_BTC))
                .divide(BigDecimal.valueOf(balanceSats), 0, RoundingMode.HALF_UP)
                .toLong()
        } else {
            null
        }

        return PortfolioSummary(
            balanceSats = balanceSats,
            costCents = costCents,
            averagePriceCents = averagePriceCents,
            totalBoughtCents = totalBoughtCents,
            totalSoldCents = totalSoldCents,
            realizedPnlCents = realizedPnlCents,
            feesPaidCents = feesPaidCents,
            firstBuyAt = firstBuyAt,
        )
    }
}
