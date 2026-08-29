package com.pablo.btcmedio.core.extract

import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.EntrySource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExtractorChainTest {

    private val hoje = LocalDate.of(2026, 8, 29)

    private class Fake(
        override val name: String,
        private val available: Boolean,
        private val result: TransactionDraft?,
        val calls: MutableList<String> = mutableListOf(),
    ) : DraftExtractor {
        override suspend fun isAvailable() = available
        override suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft? {
            calls.add(text)
            return result
        }
    }

    private val completo = TransactionDraft(
        occurredAt = 1L, fiatAmountCents = 150_000, satoshis = 468_094, unitPriceCents = 31_564_152,
    )
    private val parcial = TransactionDraft(fiatAmountCents = 150_000)

    @Test
    fun `para no primeiro extrator que devolve rascunho completo`() = runTest {
        val segundo = Fake("nano", true, completo)
        val chain = ExtractorChain(listOf(Fake("regras", true, completo), segundo))
        val draft = chain.extract("qualquer", EntrySource.IMAGE, hoje)
        assertEquals(150_000L, draft.fiatAmountCents)
        assertTrue("o segundo extrator não deveria ser chamado", segundo.calls.isEmpty())
    }

    @Test
    fun `avanca para o proximo quando o rascunho e parcial`() = runTest {
        val segundo = Fake("nano", true, completo)
        val chain = ExtractorChain(listOf(Fake("regras", true, parcial), segundo))
        val draft = chain.extract("qualquer", EntrySource.IMAGE, hoje)
        assertEquals(468_094L, draft.satoshis)
        assertEquals(1, segundo.calls.size)
    }

    @Test
    fun `pula extrator indisponivel`() = runTest {
        val indisponivel = Fake("nano", false, completo)
        val chain = ExtractorChain(listOf(Fake("regras", true, parcial), indisponivel))
        val draft = chain.extract("qualquer", EntrySource.IMAGE, hoje)
        assertEquals(150_000L, draft.fiatAmountCents)
        assertNull(draft.satoshis)
        assertTrue(indisponivel.calls.isEmpty())
    }

    @Test
    fun `mantem o melhor rascunho parcial quando nenhum extrator completa`() = runTest {
        val chain = ExtractorChain(listOf(Fake("a", true, parcial), Fake("b", true, null)))
        val draft = chain.extract("qualquer", EntrySource.TEXT, hoje)
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals("qualquer", draft.rawText)
    }

    @Test
    fun `extrator que lanca excecao nao derruba a cadeia`() = runTest {
        val explosivo = object : DraftExtractor {
            override val name = "explosivo"
            override suspend fun isAvailable() = true
            override suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft =
                throw IllegalStateException("modelo indisponível")
        }
        val chain = ExtractorChain(listOf(explosivo, Fake("regras", true, completo)))
        assertEquals(468_094L, chain.extract("qualquer", EntrySource.IMAGE, hoje).satoshis)
    }

    @Test
    fun `cadeia vazia devolve rascunho so com o texto bruto`() = runTest {
        val draft = ExtractorChain(emptyList()).extract("nada aqui", EntrySource.TEXT, hoje)
        assertNull(draft.fiatAmountCents)
        assertEquals("nada aqui", draft.rawText)
        assertEquals(EntrySource.TEXT, draft.source)
    }
}
