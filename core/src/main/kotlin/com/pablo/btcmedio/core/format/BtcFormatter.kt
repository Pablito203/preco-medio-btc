package com.pablo.btcmedio.core.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Satoshis em Bitcoin legível.
 *
 * Mesma razão do [BrlFormatter]: satoshi é a unidade de armazenamento, e as
 * mensagens de validação nascem no `:core` precisando falar em BTC. A camada
 * Android reaproveita daqui em vez de manter uma segunda implementação.
 */
object BtcFormatter {

    /** Símbolo da unidade. O app usa o glifo, nunca a sigla "BTC". */
    const val SYMBOL: String = "₿"

    private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

    /** `468094` → `0,00468094` — sempre as oito casas, sem cortar zeros. */
    fun format(sats: Long): String {
        val format = NumberFormat.getNumberInstance(PT_BR).apply {
            minimumFractionDigits = 8
            maximumFractionDigits = 8
        }
        return format.format(BigDecimal.valueOf(sats, 8))
    }

    /** `468094` → `0,00468094 ₿` */
    fun withSymbol(sats: Long): String = "${format(sats)} $SYMBOL"
}
