package com.pablo.btcmedio.ui.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {

    private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
    private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR)
    private val DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PT_BR)

    fun brl(cents: Long?): String {
        if (cents == null) return "—"
        return NumberFormat.getCurrencyInstance(PT_BR).format(BigDecimal.valueOf(cents, 2))
    }

    fun btc(sats: Long?): String {
        if (sats == null) return "—"
        val format = NumberFormat.getNumberInstance(PT_BR).apply {
            minimumFractionDigits = 8
            maximumFractionDigits = 8
        }
        return format.format(BigDecimal.valueOf(sats, 8))
    }

    fun date(epochMillis: Long): String =
        DATE.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    fun dateTime(epochMillis: Long): String =
        DATE_TIME.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}
