package com.pablo.btcmedio.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class BtcFormatterTest {

    @Test
    fun `satoshis viram bitcoin com oito casas`() {
        assertEquals("0,00468094", BtcFormatter.format(468_094))
        assertEquals("0,04260456", BtcFormatter.format(4_260_456))
        assertEquals("1,00000000", BtcFormatter.format(100_000_000))
        assertEquals("0,00000000", BtcFormatter.format(0))
    }

    /** Cortar zeros à direita esconderia a ordem de grandeza do valor. */
    @Test
    fun `zeros a direita sao preservados`() {
        assertEquals("0,00100000", BtcFormatter.format(100_000))
        assertEquals("0,00000001", BtcFormatter.format(1))
    }

    @Test
    fun `com simbolo usa o glifo e nao a sigla`() {
        assertEquals("0,00468094 ₿", BtcFormatter.withSymbol(468_094))
        assertEquals(false, BtcFormatter.withSymbol(468_094).contains("BTC"))
    }
}
