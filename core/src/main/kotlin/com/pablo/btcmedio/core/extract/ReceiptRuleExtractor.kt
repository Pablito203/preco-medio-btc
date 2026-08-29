package com.pablo.btcmedio.core.extract

import com.pablo.btcmedio.core.draft.DraftCompleter
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.core.parse.BrazilianDateParser
import com.pablo.btcmedio.core.parse.BrazilianNumberParser
import java.time.LocalDate
import java.time.ZoneId

/**
 * Extração determinística por rótulos e heurísticas.
 *
 * É o caminho principal, não o plano B: funciona em qualquer aparelho, é
 * reproduzível e não alucina. O Gemini Nano só entra no que sobrar.
 */
class ReceiptRuleExtractor(
    private val zone: ZoneId = ZoneId.systemDefault(),
) : DraftExtractor {

    override val name = "regras"

    override suspend fun isAvailable() = true

    private val sellWords = listOf("venda", "vendi", "vender", "vendido", "sell", "sold")
    private val buyWords = listOf("compra", "comprei", "comprar", "comprado", "buy", "bought")

    private val priceLabels =
        listOf("preço", "preco", "cotação", "cotacao", "price", "valor unitário", "valor unitario")
    private val feeLabels = listOf("taxa", "tarifa", "fee", "comissão", "comissao")
    private val totalLabels =
        listOf("total comprado", "total vendido", "total negociado", "valor total", "total")

    private val eightDecimals = Regex("""\b\d+[.,]\d{6,8}\b""")

    /**
     * Quantidade colada ao símbolo, antes ("0,004 BTC") ou depois ("₿ 0,004").
     *
     * Usa lookarounds em vez de `\b` porque não existe fronteira de palavra
     * entre um dígito e uma letra: `\bBTC` nunca casaria em "0,00468094BTC".
     */
    private val satsNearSymbol = Regex(
        """(\d[\d.,]*)\s*(?:₿|BTC(?![A-Za-z]))|(?:₿|(?<![A-Za-z])BTC)\s*[:=]?\s*(\d[\d.,]*)""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Em fala e mensagem, a preposição "a" antecede a cotação:
     * "comprei 500 reais de bitcoin a 315.641,52". Já "por" antecede o total,
     * então só "a" entra aqui.
     */
    private val naturalPrice = Regex(
        """(?:^|\s)a\s+(?:R\$\s*)?(\d{1,3}(?:\.\d{3})+(?:,\d+)?|\d+(?:[.,]\d+)?)""",
        RegexOption.IGNORE_CASE,
    )
    private val signedFiat = Regex("""^[-+−]\s*R\$""")
    private val moneyWords = Regex("""(?:R\$|reais?|real)""", RegexOption.IGNORE_CASE)

    override suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft? {
        if (text.isBlank()) return null
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.none { BrazilianNumberParser.NUMBER_PATTERN.containsMatchIn(it) }) return null

        val lower = text.lowercase()
        val type = when {
            sellWords.any { lower.contains(it) } -> TransactionType.SELL
            buyWords.any { lower.contains(it) } -> TransactionType.BUY
            else -> TransactionType.BUY
        }

        val occurredAt = BrazilianDateParser.parse(text, today)
            ?.atZone(zone)?.toInstant()?.toEpochMilli()

        val unitPriceCents = labeledCents(lines, priceLabels) ?: naturalPriceCents(text)
        val feeCents = labeledCents(lines, feeLabels)
        val totalCents = labeledCents(lines, totalLabels)
        val satoshis = findSatoshis(lines)
        val signed = findSignedFiat(lines)

        val fiatAmountCents = signed
            ?: totalCents?.let { total ->
                when (type) {
                    TransactionType.BUY -> total + (feeCents ?: 0L)
                    TransactionType.SELL -> total - (feeCents ?: 0L)
                }
            }
            ?: fallbackFiat(lines, unitPriceCents, feeCents, totalCents)

        var draft = TransactionDraft(
            type = type,
            occurredAt = occurredAt,
            fiatAmountCents = fiatAmountCents,
            feeCents = feeCents,
            satoshis = satoshis,
            unitPriceCents = unitPriceCents,
            source = source,
            rawText = text,
        )

        // Se sobrou exatamente um número dedutível, preenche.
        if (!draft.isComplete()) draft = DraftCompleter.complete(draft)

        val achouAlgo = draft.fiatAmountCents != null || draft.satoshis != null ||
            draft.unitPriceCents != null || draft.occurredAt != null
        return if (achouAlgo) draft else null
    }

    /** Valor que acompanha um rótulo, na mesma linha ou na linha seguinte. */
    private fun labeledCents(lines: List<String>, labels: List<String>): Long? {
        lines.forEachIndexed { index, line ->
            val lower = line.lowercase()
            val label = labels.firstOrNull { lower.contains(it) } ?: return@forEachIndexed
            val afterLabel = line.substring(lower.indexOf(label) + label.length)
            BrazilianNumberParser.NUMBER_PATTERN.find(afterLabel)?.let {
                return BrazilianNumberParser.parseCents(it.value)
            }
            lines.getOrNull(index + 1)?.let { next ->
                if (labels.none { next.lowercase().contains(it) }) {
                    BrazilianNumberParser.NUMBER_PATTERN.find(next)?.let {
                        return BrazilianNumberParser.parseCents(it.value)
                    }
                }
            }
        }
        return null
    }

    /**
     * Cotação em texto corrido, marcada pela preposição "a".
     *
     * Exige ao menos dois números no texto: com um só, "comprei a 500 reais"
     * é o valor da compra, não uma cotação — e errar isso é pior que não achar.
     */
    private fun naturalPriceCents(text: String): Long? {
        if (BrazilianNumberParser.NUMBER_PATTERN.findAll(text).count() < 2) return null
        val match = naturalPrice.find(text) ?: return null
        return BrazilianNumberParser.parseCents(match.groupValues[1])
    }

    /**
     * Quantidade de Bitcoin: linha com ₿ ou BTC, ou — porque o OCR erra o
     * símbolo com frequência — um número com 6 a 8 casas decimais.
     */
    private fun findSatoshis(lines: List<String>): Long? {
        // Precisa ser o número *adjacente* ao símbolo. Pegar o primeiro número
        // da linha funciona em comprovante (um valor por linha) mas quebra em
        // texto corrido: "R$ 1.500,00 ... 0,00468094 BTC" daria 1.500 BTC.
        for (line in lines) {
            satsNearSymbol.find(line)?.let { match ->
                val numero = match.groupValues[1].ifEmpty { match.groupValues[2] }
                if (numero.isNotEmpty()) return BrazilianNumberParser.parseSatoshis(numero)
            }
        }
        for (line in lines) {
            if (line.contains("R$")) continue
            eightDecimals.find(line)?.let {
                return BrazilianNumberParser.parseSatoshis(it.value)
            }
        }
        return null
    }

    /** A linha com sinal explícito e `R$` é o movimento de caixa. */
    private fun findSignedFiat(lines: List<String>): Long? =
        lines.firstOrNull { signedFiat.containsMatchIn(it) }
            ?.let { line -> BrazilianNumberParser.NUMBER_PATTERN.find(line)?.value }
            ?.let { BrazilianNumberParser.parseCents(it) }

    /**
     * Sem rótulo nem sinal: o primeiro valor em reais que não seja a cotação,
     * a taxa nem o total. Cobre linguagem natural ("comprei 500 reais").
     */
    private fun fallbackFiat(
        lines: List<String>,
        priceCents: Long?,
        feeCents: Long?,
        totalCents: Long?,
    ): Long? {
        val excluded = setOfNotNull(priceCents, feeCents, totalCents)
        for (line in lines) {
            if (!moneyWords.containsMatchIn(line)) continue
            for (match in BrazilianNumberParser.NUMBER_PATTERN.findAll(line)) {
                val cents = BrazilianNumberParser.parseCents(match.value) ?: continue
                if (cents in excluded) continue
                return cents
            }
        }
        return null
    }
}
