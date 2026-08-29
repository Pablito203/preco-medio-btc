package com.pablo.btcmedio.core.parse

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Interpreta datas brasileiras em texto de comprovante ou fala.
 *
 * `today` é injetado em vez de lido do relógio para que expressões relativas
 * ("ontem") sejam testáveis de forma determinística.
 */
object BrazilianDateParser {

    private val DATE = Regex("""\b(\d{1,2})[/\-.](\d{1,2})(?:[/\-.](\d{2,4}))?\b""")
    private val TIME = Regex("""\b(\d{1,2}):(\d{2})(?::(\d{2}))?\b""")

    fun parse(raw: String, today: LocalDate): LocalDateTime? {
        if (raw.isBlank()) return null
        val time = TIME.find(raw)
        val date = DATE.find(raw)

        val day: LocalDate = when {
            date != null -> {
                val d = date.groupValues[1].toInt()
                val m = date.groupValues[2].toInt()
                val yearRaw = date.groupValues[3]
                val y = when {
                    yearRaw.isEmpty() -> today.year
                    yearRaw.length <= 2 -> 2000 + yearRaw.toInt()
                    else -> yearRaw.toInt()
                }
                try {
                    LocalDate.of(y, m, d)
                } catch (e: DateTimeException) {
                    return null
                }
            }

            else -> relativeDay(raw, today) ?: return null
        }

        if (time == null) return day.atStartOfDay()

        val h = time.groupValues[1].toInt()
        val min = time.groupValues[2].toInt()
        val sec = time.groupValues[3].ifEmpty { "0" }.toInt()
        return try {
            day.atTime(h, min, sec)
        } catch (e: DateTimeException) {
            day.atStartOfDay()
        }
    }

    private fun relativeDay(raw: String, today: LocalDate): LocalDate? {
        val normalized = raw.lowercase()
        return when {
            normalized.contains("anteontem") -> today.minusDays(2)
            normalized.contains("ontem") -> today.minusDays(1)
            normalized.contains("hoje") -> today
            else -> null
        }
    }
}
