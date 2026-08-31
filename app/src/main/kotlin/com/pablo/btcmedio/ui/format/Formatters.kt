package com.pablo.btcmedio.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {

    private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
    private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR)
    private val SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yy", PT_BR)
    private val DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PT_BR)
    private val MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", PT_BR)

    /** Símbolo da unidade. O layout usa o glifo, nunca a sigla "BTC". */
    const val BTC_SYMBOL: String = "₿"

    fun brl(cents: Long?): String {
        if (cents == null) return "—"
        return NumberFormat.getCurrencyInstance(PT_BR).format(BigDecimal.valueOf(cents, 2))
    }

    /**
     * Reais sem os centavos, arredondando: a cotação numa linha de transação
     * é contexto, e seis dígitos mais dois centavos competem com o valor que
     * de fato importa na mesma linha.
     */
    fun brlWhole(cents: Long?): String {
        if (cents == null) return "—"
        val format = NumberFormat.getCurrencyInstance(PT_BR).apply {
            maximumFractionDigits = 0
            roundingMode = RoundingMode.HALF_UP
        }
        return format.format(BigDecimal.valueOf(cents, 2))
    }

    /**
     * O número em reais **sem** o "R$". O cartão de destaque pinta o símbolo
     * em âmbar e o valor em osso, então precisa das duas metades separadas.
     */
    fun decimal(cents: Long?): String {
        if (cents == null) return "—"
        val format = NumberFormat.getNumberInstance(PT_BR).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return format.format(BigDecimal.valueOf(cents, 2))
    }

    fun btc(sats: Long?): String {
        if (sats == null) return "—"
        val format = NumberFormat.getNumberInstance(PT_BR).apply {
            minimumFractionDigits = 8
            maximumFractionDigits = 8
        }
        return format.format(BigDecimal.valueOf(sats, 8))
    }

    /** A quantidade já com o símbolo: `0,00468094 ₿`. */
    fun btcAmount(sats: Long?): String {
        if (sats == null) return "—"
        return "${btc(sats)} $BTC_SYMBOL"
    }

    fun date(epochMillis: Long): String =
        DATE.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    /** `31/08/26` — o ano curto cabe na linha da transação. */
    fun shortDate(epochMillis: Long): String =
        SHORT_DATE.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    fun dateTime(epochMillis: Long): String =
        DATE_TIME.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    /** `Agosto 2026` — cabeçalho de mês da lista. */
    fun month(epochMillis: Long): String =
        MONTH.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
            .replaceFirstChar { it.uppercase() }
}
