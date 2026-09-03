package com.pablo.btcmedio.core.draft

import com.pablo.btcmedio.core.calc.PortfolioCalculator.SATS_PER_BTC
import com.pablo.btcmedio.core.format.BrlFormatter
import com.pablo.btcmedio.core.format.BtcFormatter
import java.math.BigDecimal
import java.math.RoundingMode

enum class Severity { ERROR, WARNING }

data class DraftIssue(
    val field: DraftField?,
    val severity: Severity,
    val message: String,
)

/**
 * Confere se os quatro números fecham entre si.
 *
 * ERROR impede gravar. WARNING é exibido mas não bloqueia — um comprovante
 * pode ser legitimamente atípico, e a decisão é do usuário.
 */
object DraftValidator {

    /** Folga aceita para o arredondamento da exchange. */
    const val SATS_TOLERANCE = 2L

    /** Acima disso, o que está errado não é a taxa: é a leitura de algum número. */
    private const val MAX_FEE_RATIO = 0.05

    fun validate(d: TransactionDraft): List<DraftIssue> = buildList {
        if (d.occurredAt == null) {
            add(DraftIssue(DraftField.DATE, Severity.ERROR, "Informe a data da transação."))
        }
        if (d.fiatAmountCents == null) {
            add(DraftIssue(DraftField.FIAT, Severity.ERROR, "Informe o valor em reais."))
        } else if (d.fiatAmountCents <= 0L) {
            add(DraftIssue(DraftField.FIAT, Severity.ERROR, "O valor em reais deve ser maior que zero."))
        }
        if (d.satoshis == null) {
            add(DraftIssue(DraftField.SATS, Severity.ERROR, "Informe a quantidade de Bitcoin."))
        } else if (d.satoshis <= 0L) {
            add(DraftIssue(DraftField.SATS, Severity.ERROR, "A quantidade de Bitcoin deve ser maior que zero."))
        }
        if (d.unitPriceCents == null) {
            add(DraftIssue(DraftField.PRICE, Severity.ERROR, "Informe a cotação."))
        } else if (d.unitPriceCents <= 0L) {
            add(DraftIssue(DraftField.PRICE, Severity.ERROR, "A cotação deve ser maior que zero."))
        }
        if (d.feeCents != null && d.feeCents < 0L) {
            add(DraftIssue(DraftField.FEE, Severity.ERROR, "A taxa não pode ser negativa."))
        }

        if (any { it.severity == Severity.ERROR }) return@buildList

        if (d.feeCents != null) {
            addAll(checkCoherence(d))
        } else {
            addAll(checkImpliedFee(d))
        }
    }

    /** |satoshis − arredonda(valorNegociado × 1e8 ÷ cotação)| ≤ 2 sats */
    private fun checkCoherence(d: TransactionDraft): List<DraftIssue> {
        val traded = d.tradedFiatCents() ?: return emptyList()
        val price = d.unitPriceCents ?: return emptyList()
        val sats = d.satoshis ?: return emptyList()
        if (traded <= 0L) {
            return listOf(
                DraftIssue(DraftField.FEE, Severity.WARNING, "A taxa é maior que o valor da transação.")
            )
        }
        val expected = BigDecimal.valueOf(traded)
            .multiply(BigDecimal.valueOf(SATS_PER_BTC))
            .divide(BigDecimal.valueOf(price), 0, RoundingMode.HALF_UP)
            .toLong()
        val diff = kotlin.math.abs(sats - expected)
        return if (diff > SATS_TOLERANCE) {
            listOf(
                DraftIssue(
                    DraftField.SATS,
                    Severity.WARNING,
                    "Os números não fecham: valor, taxa e cotação dariam " +
                        "${BtcFormatter.withSymbol(expected)}, não ${BtcFormatter.withSymbol(sats)}.",
                )
            )
        } else {
            emptyList()
        }
    }

    /** Sem taxa informada, o que se verifica é se a taxa implícita é plausível. */
    private fun checkImpliedFee(d: TransactionDraft): List<DraftIssue> {
        val implied = DraftCompleter.impliedFeeCents(d) ?: return emptyList()
        val fiat = d.fiatAmountCents ?: return emptyList()
        return when {
            implied < 0L -> listOf(
                DraftIssue(
                    DraftField.FEE,
                    Severity.WARNING,
                    "Os números não fecham: a quantidade e a cotação dariam mais reais do que o valor informado.",
                )
            )

            implied > (fiat * MAX_FEE_RATIO) -> listOf(
                DraftIssue(
                    DraftField.FEE,
                    Severity.WARNING,
                    "A taxa implícita seria de ${BrlFormatter.format(implied)}, " +
                        "acima de 5% do valor. Confira os números.",
                )
            )

            else -> emptyList()
        }
    }
}
