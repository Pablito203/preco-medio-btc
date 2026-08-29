package com.pablo.btcmedio.core.draft

import com.pablo.btcmedio.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftCompleterTest {

    /** O comprovante anexo, completo. */
    private val comprovante = TransactionDraft(
        type = TransactionType.BUY,
        occurredAt = 1_782_000_000_000L,
        fiatAmountCents = 150_000,
        feeCents = 2_250,
        satoshis = 468_094,
        unitPriceCents = 31_564_152,
    )

    @Test
    fun `valor negociado desconta a taxa na compra`() {
        assertEquals(147_750L, comprovante.tradedFiatCents())
    }

    @Test
    fun `valor negociado soma a taxa na venda`() {
        val v = comprovante.copy(type = TransactionType.SELL)
        assertEquals(152_250L, v.tradedFiatCents())
    }

    @Test
    fun `completa a quantidade de bitcoin`() {
        val d = comprovante.copy(satoshis = null)
        assertEquals(DraftField.SATS, DraftCompleter.missingField(d))
        assertEquals(468_094L, DraftCompleter.complete(d).satoshis)
    }

    @Test
    fun `completa a cotacao`() {
        val d = comprovante.copy(unitPriceCents = null)
        assertEquals(DraftField.PRICE, DraftCompleter.missingField(d))
        // 147.750 * 1e8 / 468.094 = 31.564.173,0 -> 31.564.173
        // Difere em R$ 0,21 da cotação impressa: a exchange trunca os satoshis.
        assertEquals(31_564_173L, DraftCompleter.complete(d).unitPriceCents)
    }

    @Test
    fun `completa o valor em reais`() {
        val d = comprovante.copy(fiatAmountCents = null)
        assertEquals(DraftField.FIAT, DraftCompleter.missingField(d))
        assertEquals(150_000L, DraftCompleter.complete(d).fiatAmountCents)
    }

    @Test
    fun `completa a taxa`() {
        val d = comprovante.copy(feeCents = null)
        assertEquals(DraftField.FEE, DraftCompleter.missingField(d))
        assertEquals(2_250L, DraftCompleter.complete(d).feeCents)
    }

    @Test
    fun `deriva a taxa implicita da primeira linha da planilha`() {
        // R$ 4.000,00 por 0,01174383 BTC a R$ 337.198,31 -> taxa implícita R$ 40,00
        val d = TransactionDraft(
            type = TransactionType.BUY,
            occurredAt = 1_770_000_000_000L,
            fiatAmountCents = 400_000,
            feeCents = null,
            satoshis = 1_174_383,
            unitPriceCents = 33_719_831,
        )
        assertEquals(4_000L, DraftCompleter.impliedFeeCents(d))
    }

    @Test
    fun `nao completa quando faltam dois campos`() {
        val d = comprovante.copy(satoshis = null, unitPriceCents = null)
        assertNull(DraftCompleter.missingField(d))
        assertEquals(d, DraftCompleter.complete(d))
    }

    @Test
    fun `nunca sobrescreve campo ja preenchido`() {
        val adulterado = comprovante.copy(satoshis = 999_999)
        assertEquals(999_999L, DraftCompleter.complete(adulterado).satoshis)
    }

    @Test
    fun `data faltante nao impede completar os numeros`() {
        val d = comprovante.copy(occurredAt = null, satoshis = null)
        assertEquals(468_094L, DraftCompleter.complete(d).satoshis)
    }
}
