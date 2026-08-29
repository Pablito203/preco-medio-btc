package com.pablo.btcmedio.core.extract

import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.core.parse.BrazilianDateParser
import com.pablo.btcmedio.core.parse.BrazilianNumberParser
import java.time.LocalDate
import java.time.ZoneId

/**
 * Prompt enviado ao modelo on-device e leitura da resposta.
 *
 * A saída do modelo nunca é confiada: cada número volta como texto e passa
 * pelo mesmo parser determinístico usado no resto do app. Um LLM que invente
 * um dígito produz `null` aqui, não um valor plausível e errado.
 *
 * A leitura do JSON é feita campo a campo por expressão regular em vez de um
 * parser completo. O formato é um objeto plano de seis campos de texto, e
 * assim isto vive no módulo puro, onde os testes rodam em milissegundos e sem
 * depender do `org.json` do Android.
 */
object LlmDraftJson {

    private val FIELDS = listOf("tipo", "data", "valor", "taxa", "bitcoin", "cotacao")

    fun promptFor(text: String): String = """
        Extraia os dados da transação de Bitcoin abaixo e responda APENAS com JSON,
        sem explicação. Use exatamente estas chaves, com os números no formato
        brasileiro e como texto. Omita a chave quando o dado não aparecer.

        {"tipo":"compra|venda","data":"dd/mm/aaaa hh:mm","valor":"","taxa":"","bitcoin":"","cotacao":""}

        Exemplo de entrada:
        Compra 30/06/2026 22:57 - R$ 1.500,00 + 0,00468094 Preço R$ 315.641,52 Taxa R$ 22,50
        Exemplo de saída:
        {"tipo":"compra","data":"30/06/2026 22:57","valor":"1.500,00","taxa":"22,50","bitcoin":"0,00468094","cotacao":"315.641,52"}

        Texto:
        $text
    """.trimIndent()

    fun parse(
        raw: String,
        source: EntrySource,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): TransactionDraft? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val json = raw.substring(start, end + 1)

        val values = FIELDS.associateWith { field(json, it) }
        if (values.values.all { it == null }) return null

        val type = when (values["tipo"]?.lowercase()) {
            "venda", "sell" -> TransactionType.SELL
            else -> TransactionType.BUY
        }

        return TransactionDraft(
            type = type,
            occurredAt = values["data"]
                ?.let { BrazilianDateParser.parse(it, today) }
                ?.atZone(zone)?.toInstant()?.toEpochMilli(),
            fiatAmountCents = values["valor"]?.let { BrazilianNumberParser.parseCents(it) },
            feeCents = values["taxa"]?.let { BrazilianNumberParser.parseCents(it) },
            satoshis = values["bitcoin"]?.let { BrazilianNumberParser.parseSatoshis(it) },
            unitPriceCents = values["cotacao"]?.let { BrazilianNumberParser.parseCents(it) },
            source = source,
            rawText = raw,
        )
    }

    /** Lê `"chave": "valor"` respeitando aspas escapadas. */
    private fun field(json: String, key: String): String? =
        Regex(""""$key"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            .find(json)
            ?.groupValues
            ?.get(1)
            ?.replace("\\\"", "\"")
            ?.takeIf { it.isNotBlank() }
}
