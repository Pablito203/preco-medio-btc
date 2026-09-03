package com.pablo.btcmedio.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class BrlFormatterTest {

    /**
     * O separador entre "R$" e o número não é espaço comum, e varia com a
     * versão do CLDR: no-break, narrow no-break ou figure space. Normalizar por
     * escape explícito evita depender de um caractere invisível no código-fonte.
     */
    private fun norm(s: String) = s.replace(Regex("[\\u00A0\\u202F\\u2007]"), " ")

    @Test
    fun `centavos viram reais no formato brasileiro`() {
        assertEquals("R$ 1.500,00", norm(BrlFormatter.format(150_000)))
        assertEquals("R$ 22,50", norm(BrlFormatter.format(2_250)))
        assertEquals("R$ 342.686,32", norm(BrlFormatter.format(34_268_632)))
        assertEquals("R$ 0,00", norm(BrlFormatter.format(0)))
    }

    @Test
    fun `um centavo nao vira zero`() {
        assertEquals("R$ 0,01", norm(BrlFormatter.format(1)))
    }

    @Test
    fun `valor negativo preserva o sinal`() {
        val formatado = norm(BrlFormatter.format(-2_000_000))
        assertEquals(true, formatado.contains("20.000,00"))
        assertEquals(true, formatado.contains("-"))
    }
}
