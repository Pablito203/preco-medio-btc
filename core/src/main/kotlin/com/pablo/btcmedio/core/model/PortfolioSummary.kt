package com.pablo.btcmedio.core.model

/**
 * Estado agregado da carteira.
 *
 * @param averagePriceCents `null` quando o saldo é zero — não há preço médio a exibir.
 * @param feesPaidCents soma das taxas **conhecidas**. Taxa `null` é desconhecida,
 *   não zero, então fica de fora em vez de baixar o total artificialmente.
 * @param firstBuyAt instante da compra mais antiga, ou `null` sem nenhuma compra.
 */
data class PortfolioSummary(
    val balanceSats: Long,
    val costCents: Long,
    val averagePriceCents: Long?,
    val totalBoughtCents: Long,
    val totalSoldCents: Long,
    val realizedPnlCents: Long,
    val feesPaidCents: Long = 0L,
    val firstBuyAt: Long? = null,
) {
    companion object {
        val EMPTY = PortfolioSummary(0L, 0L, null, 0L, 0L, 0L)
    }
}
