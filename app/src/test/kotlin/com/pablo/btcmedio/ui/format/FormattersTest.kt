package com.pablo.btcmedio.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    /** O ICU usa espaço não separável entre "R$" e o número. */
    private fun norm(s: String) = s.replace(' ', ' ')

    @Test
    fun formata_reais() {
        assertEquals("R$ 1.500,00", norm(Formatters.brl(150_000)))
        assertEquals("R$ 342.686,32", norm(Formatters.brl(34_268_632)))
        assertEquals("R$ 0,00", norm(Formatters.brl(0)))
    }

    @Test
    fun valor_negativo_e_formatado_com_sinal() {
        val formatado = norm(Formatters.brl(-2_000_000))
        assertEquals(true, formatado.contains("20.000,00"))
        assertEquals(true, formatado.contains("-"))
    }

    @Test
    fun valor_nulo_vira_travessao() {
        assertEquals("—", Formatters.brl(null))
        assertEquals("—", Formatters.btc(null))
        assertEquals("—", Formatters.brlWhole(null))
        assertEquals("—", Formatters.decimal(null))
        assertEquals("—", Formatters.btcAmount(null))
    }

    /** O cartao de destaque pinta "R$" e o numero em cores diferentes. */
    @Test
    fun decimal_sai_sem_o_simbolo_da_moeda() {
        assertEquals("320.448,46", Formatters.decimal(32_044_846))
        assertEquals("0,00", Formatters.decimal(0))
        assertEquals("3.000,00", Formatters.decimal(300_000))
    }

    @Test
    fun cotacao_na_linha_da_transacao_perde_os_centavos() {
        assertEquals("R$ 320.448", norm(Formatters.brlWhole(32_044_846)))
        // 320.448,60 arredonda para cima; truncar mentiria sobre a cotacao.
        assertEquals("R$ 320.449", norm(Formatters.brlWhole(32_044_860)))
    }

    @Test
    fun quantidade_de_bitcoin_leva_o_glifo() {
        assertEquals("0,00468094 ₿", Formatters.btcAmount(468_094))
    }

    @Test
    fun formata_bitcoin_com_oito_casas() {
        assertEquals("0,04260456", Formatters.btc(4_260_456))
        assertEquals("0,00468094", Formatters.btc(468_094))
        assertEquals("1,00000000", Formatters.btc(100_000_000))
        assertEquals("0,00000000", Formatters.btc(0))
    }
}
