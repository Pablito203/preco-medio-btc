package com.pablo.btcmedio.core.calc

import com.pablo.btcmedio.core.compra
import com.pablo.btcmedio.core.dia
import com.pablo.btcmedio.core.planilhaCompleta
import com.pablo.btcmedio.core.venda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortfolioCalculatorTest {

    @Test
    fun `carteira vazia nao tem preco medio`() {
        val s = PortfolioCalculator.summarize(emptyList())
        assertEquals(0L, s.balanceSats)
        assertEquals(0L, s.costCents)
        assertNull(s.averagePriceCents)
        assertEquals(0L, s.realizedPnlCents)
    }

    @Test
    fun `as nove compras da planilha reproduzem os totais`() {
        val s = PortfolioCalculator.summarize(planilhaCompleta())
        assertEquals(4_260_456L, s.balanceSats)
        assertEquals(1_460_000L, s.costCents)
        assertEquals(34_268_632L, s.averagePriceCents)
        assertEquals(1_460_000L, s.totalBoughtCents)
        assertEquals(0L, s.totalSoldCents)
        assertEquals(0L, s.realizedPnlCents)
    }

    @Test
    fun `ordem de entrada nao altera o resultado`() {
        val s = PortfolioCalculator.summarize(planilhaCompleta().reversed())
        assertEquals(4_260_456L, s.balanceSats)
        assertEquals(34_268_632L, s.averagePriceCents)
    }

    @Test
    fun `venda parcial preserva o preco medio e realiza lucro`() {
        // Compra 1 BTC por R$ 100.000; vende 0,5 BTC por R$ 70.000.
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 7_000_000, 50_000_000, 14_000_000, id = "v1"),
        )
        val s = PortfolioCalculator.summarize(txs)
        assertEquals(50_000_000L, s.balanceSats)
        assertEquals(5_000_000L, s.costCents)
        assertEquals(10_000_000L, s.averagePriceCents)   // preço médio inalterado
        assertEquals(2_000_000L, s.realizedPnlCents)     // 70.000 - 50.000 = 20.000
        assertEquals(7_000_000L, s.totalSoldCents)
    }

    @Test
    fun `venda total zera custo e preco medio sem residuo`() {
        // Três compras com valores que não dividem exato, depois venda de tudo.
        val txs = listOf(
            compra(dia(1, 1, 2026), 100_001, 333_333, 30_000_090, id = "c1"),
            compra(dia(2, 1, 2026), 100_001, 333_333, 30_000_090, id = "c2"),
            compra(dia(3, 1, 2026), 100_001, 333_334, 30_000_000, id = "c3"),
            venda(dia(4, 1, 2026), 400_000, 1_000_000, 40_000_000, id = "v1"),
        )
        val s = PortfolioCalculator.summarize(txs)
        assertEquals(0L, s.balanceSats)
        assertEquals(0L, s.costCents)
        assertNull(s.averagePriceCents)
        assertEquals(400_000L - 300_003L, s.realizedPnlCents)
    }

    @Test
    fun `venda com prejuizo produz resultado negativo`() {
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 3_000_000, 50_000_000, 6_000_000, id = "v1"),
        )
        val s = PortfolioCalculator.summarize(txs)
        assertEquals(-2_000_000L, s.realizedPnlCents)
    }

    @Test
    fun `compra apos venda recalcula o preco medio`() {
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 7_000_000, 50_000_000, 14_000_000, id = "v1"),
            compra(dia(3, 1, 2026), 3_000_000, 20_000_000, 15_000_000, id = "c2"),
        )
        val s = PortfolioCalculator.summarize(txs)
        assertEquals(70_000_000L, s.balanceSats)
        assertEquals(8_000_000L, s.costCents)
        // 8.000.000 * 1e8 / 70.000.000 = 11.428.571,42 -> 11.428.571
        assertEquals(11_428_571L, s.averagePriceCents)
    }

    @Test
    fun `venda acima do saldo e limitada em vez de lancar excecao`() {
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 30_000_000, 200_000_000, 15_000_000, id = "v1"),
        )
        val s = PortfolioCalculator.summarize(txs)
        assertEquals(0L, s.balanceSats)
        assertEquals(0L, s.costCents)
    }
}
