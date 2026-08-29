package com.pablo.btcmedio.core.draft

import com.pablo.btcmedio.core.calc.PortfolioCalculator.SATS_PER_BTC
import com.pablo.btcmedio.core.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Os quatro números (valor, taxa, quantidade, cotação) têm três graus de
 * liberdade. Quando exatamente um falta, ele é dedutível dos outros.
 */
object DraftCompleter {

    /** O único campo numérico ausente, ou `null` quando faltam zero ou mais de um. */
    fun missingField(d: TransactionDraft): DraftField? {
        val missing = buildList {
            if (d.fiatAmountCents == null) add(DraftField.FIAT)
            if (d.feeCents == null) add(DraftField.FEE)
            if (d.satoshis == null) add(DraftField.SATS)
            if (d.unitPriceCents == null) add(DraftField.PRICE)
        }
        return missing.singleOrNull()
    }

    /** Preenche o campo ausente. Nunca sobrescreve valor já presente. */
    fun complete(d: TransactionDraft): TransactionDraft = when (missingField(d)) {
        DraftField.SATS -> d.copy(satoshis = deriveSats(d))
        DraftField.PRICE -> d.copy(unitPriceCents = derivePrice(d))
        DraftField.FIAT -> d.copy(fiatAmountCents = deriveFiat(d))
        DraftField.FEE -> d.copy(feeCents = impliedFeeCents(d))
        else -> d
    }

    /**
     * Taxa deduzida dos outros três números:
     * `fiatAmount - satoshis * unitPrice / 1e8` na compra (invertido na venda).
     */
    fun impliedFeeCents(d: TransactionDraft): Long? {
        val fiat = d.fiatAmountCents ?: return null
        val traded = tradedFromSatsAndPrice(d) ?: return null
        return when (d.type) {
            TransactionType.BUY -> fiat - traded
            TransactionType.SELL -> traded - fiat
        }
    }

    private fun deriveSats(d: TransactionDraft): Long? {
        val traded = d.tradedFiatCents() ?: return null
        val price = d.unitPriceCents?.takeIf { it > 0 } ?: return null
        return BigDecimal.valueOf(traded)
            .multiply(BigDecimal.valueOf(SATS_PER_BTC))
            .divide(BigDecimal.valueOf(price), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun derivePrice(d: TransactionDraft): Long? {
        val traded = d.tradedFiatCents() ?: return null
        val sats = d.satoshis?.takeIf { it > 0 } ?: return null
        return BigDecimal.valueOf(traded)
            .multiply(BigDecimal.valueOf(SATS_PER_BTC))
            .divide(BigDecimal.valueOf(sats), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun deriveFiat(d: TransactionDraft): Long? {
        val traded = tradedFromSatsAndPrice(d) ?: return null
        val fee = d.feeCents ?: 0L
        return when (d.type) {
            TransactionType.BUY -> traded + fee
            TransactionType.SELL -> traded - fee
        }
    }

    /** `satoshis * unitPrice / 1e8`, em centavos. */
    fun tradedFromSatsAndPrice(d: TransactionDraft): Long? {
        val sats = d.satoshis ?: return null
        val price = d.unitPriceCents ?: return null
        return BigDecimal.valueOf(sats)
            .multiply(BigDecimal.valueOf(price))
            .divide(BigDecimal.valueOf(SATS_PER_BTC), 0, RoundingMode.HALF_UP)
            .toLong()
    }
}
