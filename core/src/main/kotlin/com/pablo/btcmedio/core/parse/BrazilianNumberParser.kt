package com.pablo.btcmedio.core.parse

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Interpreta números no formato brasileiro, tolerante ao ruído do OCR e da fala.
 *
 * A ambiguidade real é o ponto: em "1.500" ele é separador de milhar, em
 * "0.00468094" é separador decimal. A regra usada é estrutural — só é milhar
 * quando o número inteiro casa exatamente com o padrão `d{1,3}(.d{3})+`.
 */
object BrazilianNumberParser {

    /** Encontra números dentro de texto corrido, com ou sem separadores. */
    val NUMBER_PATTERN = Regex("""\d{1,3}(?:\.\d{3})+(?:,\d+)?|\d+(?:[.,]\d+)?""")

    private val THOUSANDS_ONLY = Regex("""^\d{1,3}(?:\.\d{3})+$""")
    private val NOISE = Regex("""[^0-9.,\-]""")

    fun parseDecimal(raw: String): BigDecimal? {
        val negative = raw.trimStart().startsWith("-")
        var s = NOISE.replace(raw, "").replace("-", "")
        if (s.isEmpty()) return null

        s = when {
            s.contains(',') -> s.replace(".", "").replace(',', '.')
            THOUSANDS_ONLY.matches(s) -> s.replace(".", "")
            else -> s
        }

        if (s.count { it == '.' } > 1) return null
        if (s.isEmpty() || s == ".") return null

        val value = try {
            BigDecimal(s)
        } catch (e: NumberFormatException) {
            return null
        }
        return if (negative) value.negate() else value
    }

    /** Converte para centavos, em módulo — o sinal vem do tipo da transação, não do texto. */
    fun parseCents(raw: String): Long? =
        parseDecimal(raw)?.abs()?.setScale(2, RoundingMode.HALF_UP)?.movePointRight(2)?.toLong()

    /** Converte para satoshis, em módulo. */
    fun parseSatoshis(raw: String): Long? =
        parseDecimal(raw)?.abs()?.setScale(8, RoundingMode.HALF_UP)?.movePointRight(8)?.toLong()
}
