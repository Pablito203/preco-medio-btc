package com.pablo.btcmedio.core.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BrazilianDateParserTest {

    private val hoje = LocalDate.of(2026, 8, 29)

    @Test
    fun `data com hora`() {
        assertEquals(
            LocalDateTime.of(2026, 6, 30, 22, 57),
            BrazilianDateParser.parse("30/06/2026 22:57", hoje),
        )
    }

    @Test
    fun `data com hora e segundos`() {
        assertEquals(
            LocalDateTime.of(2026, 6, 30, 22, 57, 13),
            BrazilianDateParser.parse("30/06/2026 22:57:13", hoje),
        )
    }

    @Test
    fun `data sem hora comeca a meia noite`() {
        assertEquals(
            LocalDateTime.of(2026, 2, 5, 0, 0),
            BrazilianDateParser.parse("05/02/2026", hoje),
        )
    }

    @Test
    fun `ano com dois digitos`() {
        assertEquals(
            LocalDateTime.of(2026, 2, 5, 0, 0),
            BrazilianDateParser.parse("05/02/26", hoje),
        )
    }

    @Test
    fun `data sem ano assume o ano corrente`() {
        assertEquals(
            LocalDateTime.of(2026, 2, 5, 0, 0),
            BrazilianDateParser.parse("05/02", hoje),
        )
    }

    @Test
    fun `data dentro de texto corrido com a preposicao as`() {
        assertEquals(
            LocalDateTime.of(2026, 6, 30, 22, 57),
            BrazilianDateParser.parse("Comprei em 30/06/2026 às 22:57 na corretora", hoje),
        )
    }

    @Test
    fun `separador por hifen ou ponto`() {
        assertEquals(LocalDateTime.of(2026, 6, 30, 0, 0), BrazilianDateParser.parse("30-06-2026", hoje))
        assertEquals(LocalDateTime.of(2026, 6, 30, 0, 0), BrazilianDateParser.parse("30.06.2026", hoje))
    }

    @Test
    fun `expressoes relativas`() {
        assertEquals(LocalDateTime.of(2026, 8, 29, 0, 0), BrazilianDateParser.parse("hoje", hoje))
        assertEquals(LocalDateTime.of(2026, 8, 28, 0, 0), BrazilianDateParser.parse("ontem", hoje))
        assertEquals(LocalDateTime.of(2026, 8, 27, 0, 0), BrazilianDateParser.parse("anteontem", hoje))
        assertEquals(
            LocalDateTime.of(2026, 8, 28, 0, 0),
            BrazilianDateParser.parse("Comprei ontem 500 reais", hoje),
        )
    }

    @Test
    fun `hora sozinha em expressao relativa`() {
        assertEquals(
            LocalDateTime.of(2026, 8, 29, 14, 30),
            BrazilianDateParser.parse("hoje às 14:30", hoje),
        )
    }

    @Test
    fun `texto sem data devolve null`() {
        assertNull(BrazilianDateParser.parse("Concluída", hoje))
        assertNull(BrazilianDateParser.parse("", hoje))
    }

    @Test
    fun `data impossivel devolve null`() {
        assertNull(BrazilianDateParser.parse("32/13/2026", hoje))
    }
}
