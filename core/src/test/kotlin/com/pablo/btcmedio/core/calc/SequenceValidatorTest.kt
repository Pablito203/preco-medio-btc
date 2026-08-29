package com.pablo.btcmedio.core.calc

import com.pablo.btcmedio.core.compra
import com.pablo.btcmedio.core.dia
import com.pablo.btcmedio.core.planilhaCompleta
import com.pablo.btcmedio.core.venda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SequenceValidatorTest {

    @Test
    fun `sequencia so de compras e valida`() {
        assertEquals(SequenceCheck.Ok, SequenceValidator.check(planilhaCompleta()))
    }

    @Test
    fun `venda dentro do saldo e valida`() {
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 7_000_000, 50_000_000, 14_000_000, id = "v1"),
        )
        assertEquals(SequenceCheck.Ok, SequenceValidator.check(txs))
    }

    @Test
    fun `venda acima do saldo e reprovada com o saldo disponivel`() {
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 30_000_000, 150_000_000, 20_000_000, id = "v1"),
        )
        val result = SequenceValidator.check(txs)
        assertTrue(result is SequenceCheck.Oversold)
        result as SequenceCheck.Oversold
        assertEquals("v1", result.transactionId)
        assertEquals(100_000_000L, result.availableSats)
        assertEquals(150_000_000L, result.requestedSats)
    }

    @Test
    fun `venda anterior a compra e reprovada mesmo com saldo final positivo`() {
        val txs = listOf(
            venda(dia(1, 1, 2026), 3_000_000, 20_000_000, 15_000_000, id = "v1"),
            compra(dia(2, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
        )
        val result = SequenceValidator.check(txs)
        assertTrue(result is SequenceCheck.Oversold)
        assertEquals("v1", (result as SequenceCheck.Oversold).transactionId)
        assertEquals(0L, result.availableSats)
    }

    @Test
    fun `reduzir uma compra antiga invalida uma venda posterior`() {
        val compraOriginal = compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1")
        val vendaPosterior = venda(dia(2, 1, 2026), 7_000_000, 50_000_000, 14_000_000, id = "v1")
        assertEquals(SequenceCheck.Ok, SequenceValidator.check(listOf(compraOriginal, vendaPosterior)))

        val compraReduzida = compraOriginal.copy(satoshis = 10_000_000)
        val result = SequenceValidator.check(listOf(compraReduzida, vendaPosterior))
        assertTrue(result is SequenceCheck.Oversold)
        assertEquals("v1", (result as SequenceCheck.Oversold).transactionId)
        assertEquals(10_000_000L, result.availableSats)
    }

    @Test
    fun `reporta a primeira violacao quando ha varias`() {
        val txs = listOf(
            venda(dia(1, 1, 2026), 100, 1_000, 10_000_000, id = "v1"),
            venda(dia(2, 1, 2026), 100, 2_000, 10_000_000, id = "v2"),
        )
        assertEquals("v1", (SequenceValidator.check(txs) as SequenceCheck.Oversold).transactionId)
    }
}
