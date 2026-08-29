package com.pablo.btcmedio.core.extract

import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReceiptRuleExtractorTest {

    private val extractor = ReceiptRuleExtractor()
    private val hoje = LocalDate.of(2026, 8, 29)

    /** O texto que o OCR produz para o comprovante anexo. */
    private val comprovanteOcr = """
        Detalhes da transação
        - R$ 1.500,00
        + ₿ 0,00468094
        Concluída
        Compra
        30/06/2026 22:57
        Detalhes
        Preço
        R$ 315.641,52
        Taxa
        R$ 22,50
        Total Comprado
        R$ 1.477,50
    """.trimIndent()

    private fun epoch(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        LocalDateTime.of(y, m, d, h, min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `extrai o comprovante inteiro`() = runTest {
        val draft = extractor.extract(comprovanteOcr, EntrySource.IMAGE, hoje)!!
        assertEquals(TransactionType.BUY, draft.type)
        assertEquals(epoch(2026, 6, 30, 22, 57), draft.occurredAt)
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals(2_250L, draft.feeCents)
        assertEquals(468_094L, draft.satoshis)
        assertEquals(31_564_152L, draft.unitPriceCents)
        assertEquals(EntrySource.IMAGE, draft.source)
        assertEquals(comprovanteOcr, draft.rawText)
    }

    @Test
    fun `reconhece venda pelo rotulo`() = runTest {
        val texto = comprovanteOcr
            .replace("Compra", "Venda")
            .replace("Total Comprado", "Total Vendido")
        assertEquals(TransactionType.SELL, extractor.extract(texto, EntrySource.IMAGE, hoje)!!.type)
    }

    @Test
    fun `deduz o valor em reais a partir do total e da taxa quando falta a linha assinada`() = runTest {
        val texto = """
            Compra
            30/06/2026 22:57
            Preço
            R$ 315.641,52
            Taxa
            R$ 22,50
            Total Comprado
            R$ 1.477,50
            0,00468094
        """.trimIndent()
        val draft = extractor.extract(texto, EntrySource.IMAGE, hoje)!!
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals(2_250L, draft.feeCents)
    }

    @Test
    fun `reconhece bitcoin por numero com oito casas decimais sem simbolo`() = runTest {
        val texto = """
            Compra 30/06/2026
            R$ 1.500,00
            0,00468094
            R$ 315.641,52
        """.trimIndent()
        assertEquals(468_094L, extractor.extract(texto, EntrySource.IMAGE, hoje)!!.satoshis)
    }

    @Test
    fun `aceita rotulos alternativos de cotacao e taxa`() = runTest {
        val texto = """
            Compra 30/06/2026
            - R$ 1.500,00
            + BTC 0,00468094
            Cotação: R$ 315.641,52
            Tarifa: R$ 22,50
        """.trimIndent()
        val draft = extractor.extract(texto, EntrySource.IMAGE, hoje)!!
        assertEquals(31_564_152L, draft.unitPriceCents)
        assertEquals(2_250L, draft.feeCents)
    }

    @Test
    fun `interpreta linguagem natural com data relativa`() = runTest {
        val draft = extractor.extract(
            "comprei 500 reais de bitcoin ontem a 315.641,52",
            EntrySource.AUDIO,
            hoje,
        )!!
        assertEquals(TransactionType.BUY, draft.type)
        assertEquals(epoch(2026, 8, 28, 0, 0), draft.occurredAt)
        assertEquals(50_000L, draft.fiatAmountCents)
        assertEquals(31_564_152L, draft.unitPriceCents)
    }

    @Test
    fun `extracao parcial devolve o que conseguiu`() = runTest {
        val draft = extractor.extract("R$ 1.500,00", EntrySource.TEXT, hoje)!!
        assertEquals(150_000L, draft.fiatAmountCents)
        assertNull(draft.satoshis)
        assertNull(draft.occurredAt)
    }

    @Test
    fun `texto sem nenhum numero devolve null`() = runTest {
        assertNull(extractor.extract("Concluída", EntrySource.IMAGE, hoje))
        assertNull(extractor.extract("", EntrySource.IMAGE, hoje))
    }

    @Test
    fun `esta sempre disponivel`() = runTest {
        assertEquals(true, extractor.isAvailable())
    }
}
