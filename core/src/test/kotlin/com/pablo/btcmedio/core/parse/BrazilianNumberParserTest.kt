package com.pablo.btcmedio.core.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrazilianNumberParserTest {

    @Test
    fun `valor em reais com simbolo e separadores`() {
        assertEquals(150_000L, BrazilianNumberParser.parseCents("R$ 1.500,00"))
        assertEquals(150_000L, BrazilianNumberParser.parseCents("- R$ 1.500,00"))
        assertEquals(2_250L, BrazilianNumberParser.parseCents("R$ 22,50"))
        assertEquals(147_750L, BrazilianNumberParser.parseCents("R$ 1.477,50"))
        assertEquals(31_564_152L, BrazilianNumberParser.parseCents("R$ 315.641,52"))
    }

    @Test
    fun `milhar sem decimais e interpretado como inteiro`() {
        assertEquals(150_000L, BrazilianNumberParser.parseCents("1.500"))
        assertEquals(123_456_700L, BrazilianNumberParser.parseCents("1.234.567"))
    }

    @Test
    fun `ponto como separador decimal quando nao e padrao de milhar`() {
        assertEquals(150_050L, BrazilianNumberParser.parseCents("1500.50"))
        assertEquals(468_094L, BrazilianNumberParser.parseSatoshis("0.00468094"))
    }

    @Test
    fun `quantidade de bitcoin com simbolo`() {
        assertEquals(468_094L, BrazilianNumberParser.parseSatoshis("₿ 0,00468094"))
        assertEquals(468_094L, BrazilianNumberParser.parseSatoshis("+ ₿ 0,00468094"))
        assertEquals(1_174_383L, BrazilianNumberParser.parseSatoshis("0,01174383 BTC"))
        assertEquals(100_000_000L, BrazilianNumberParser.parseSatoshis("1"))
    }

    @Test
    fun `mais de oito casas decimais arredonda para satoshi`() {
        assertEquals(468_094L, BrazilianNumberParser.parseSatoshis("0,004680944"))
        assertEquals(468_095L, BrazilianNumberParser.parseSatoshis("0,004680945"))
    }

    @Test
    fun `arredondamento de centavos e half up`() {
        assertEquals(1_235L, BrazilianNumberParser.parseCents("12,345"))
        assertEquals(1_234L, BrazilianNumberParser.parseCents("12,344"))
    }

    @Test
    fun `valor negativo preserva o sinal em parseDecimal mas parseCents usa modulo`() {
        assertEquals(
            0,
            BrazilianNumberParser.parseDecimal("-1.500,00")!!.compareTo(java.math.BigDecimal("-1500.00")),
        )
        assertEquals(150_000L, BrazilianNumberParser.parseCents("-1.500,00"))
    }

    @Test
    fun `entradas invalidas devolvem null`() {
        assertNull(BrazilianNumberParser.parseCents(""))
        assertNull(BrazilianNumberParser.parseCents("Concluída"))
        assertNull(BrazilianNumberParser.parseCents("R$"))
        assertNull(BrazilianNumberParser.parseSatoshis("abc"))
    }

    @Test
    fun `encontra numeros dentro de texto corrido`() {
        val achados = BrazilianNumberParser.NUMBER_PATTERN
            .findAll("Comprei R$ 1.500,00 de bitcoin a 315.641,52 e recebi 0,00468094")
            .map { it.value }
            .toList()
        assertEquals(listOf("1.500,00", "315.641,52", "0,00468094"), achados)
    }
}
