package com.pablo.btcmedio.core.extract

import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class LlmDraftJsonTest {

    private val hoje = LocalDate.of(2026, 8, 29)

    private fun parse(raw: String, source: EntrySource = EntrySource.IMAGE) =
        LlmDraftJson.parse(raw, source, hoje, ZoneOffset.UTC)

    @Test
    fun `interpreta json bem formado`() {
        val draft = parse(
            """{"tipo":"compra","data":"30/06/2026 22:57","valor":"1.500,00",
               "taxa":"22,50","bitcoin":"0,00468094","cotacao":"315.641,52"}"""
        )!!
        assertEquals(TransactionType.BUY, draft.type)
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals(2_250L, draft.feeCents)
        assertEquals(468_094L, draft.satoshis)
        assertEquals(31_564_152L, draft.unitPriceCents)
        assertEquals(EntrySource.IMAGE, draft.source)
    }

    @Test
    fun `ignora texto antes e depois do json`() {
        val draft = parse("""Aqui está: {"tipo":"venda","valor":"1.500,00"} espero ter ajudado.""")!!
        assertEquals(TransactionType.SELL, draft.type)
        assertEquals(150_000L, draft.fiatAmountCents)
    }

    @Test
    fun `campos ausentes viram null sem lancar`() {
        val draft = parse("""{"tipo":"compra"}""")!!
        assertEquals(TransactionType.BUY, draft.type)
        assertNull(draft.fiatAmountCents)
        assertNull(draft.satoshis)
        assertNull(draft.occurredAt)
    }

    @Test
    fun `numero invalido do modelo vira null em vez de lixo`() {
        val draft = parse("""{"valor":"mil e quinhentos","bitcoin":"zero virgula zero"}""")!!
        assertNull(draft.fiatAmountCents)
        assertNull(draft.satoshis)
    }

    @Test
    fun `campo vazio e tratado como ausente`() {
        val draft = parse("""{"tipo":"compra","valor":"","taxa":"22,50"}""")!!
        assertNull(draft.fiatAmountCents)
        assertEquals(2_250L, draft.feeCents)
    }

    @Test
    fun `json malformado ou sem campos conhecidos devolve null`() {
        assertNull(parse("desculpe, não consegui"))
        assertNull(parse(""))
        assertNull(parse("""{"outra":"coisa"}"""))
    }

    @Test
    fun `data relativa do modelo e resolvida`() {
        val draft = parse("""{"data":"ontem","valor":"100,00"}""")!!
        assertEquals(
            LocalDate.of(2026, 8, 28).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            draft.occurredAt,
        )
    }

    @Test
    fun `o prompt carrega o texto e pede json estrito`() {
        val prompt = LlmDraftJson.promptFor("Compra R$ 1.500,00")
        assertTrue(prompt.contains("Compra R$ 1.500,00"))
        assertTrue(prompt.contains("APENAS com JSON"))
        assertTrue(prompt.contains("\"cotacao\""))
    }
}
