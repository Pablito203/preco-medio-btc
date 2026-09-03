package com.pablo.btcmedio.core.draft

import com.pablo.btcmedio.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftValidatorTest {

    private val comprovante = TransactionDraft(
        type = TransactionType.BUY,
        occurredAt = 1_782_000_000_000L,
        fiatAmountCents = 150_000,
        feeCents = 2_250,
        satoshis = 468_094,
        unitPriceCents = 31_564_152,
    )

    @Test
    fun `comprovante real e coerente`() {
        assertEquals(emptyList<DraftIssue>(), DraftValidator.validate(comprovante))
    }

    @Test
    fun `taxa desconhecida nao dispara a invariante`() {
        val d = TransactionDraft(
            type = TransactionType.BUY,
            occurredAt = 1_770_000_000_000L,
            fiatAmountCents = 400_000,
            feeCents = null,
            satoshis = 1_174_383,
            unitPriceCents = 33_719_831,
        )
        assertEquals(emptyList<DraftIssue>(), DraftValidator.validate(d))
    }

    @Test
    fun `divergencia acima da tolerancia vira aviso`() {
        val d = comprovante.copy(satoshis = 468_094 + 50)
        val issues = DraftValidator.validate(d)
        assertTrue(issues.any { it.severity == Severity.WARNING && it.field == DraftField.SATS })
    }

    /** Satoshi é unidade de armazenamento; a mensagem fala com uma pessoa. */
    @Test
    fun `o aviso de incoerencia fala em bitcoin e nao em satoshis`() {
        val d = comprovante.copy(satoshis = 468_144)
        val aviso = DraftValidator.validate(d).single { it.severity == Severity.WARNING }

        assertTrue("a mensagem foi: ${aviso.message}", aviso.message.contains("0,00468094 ₿"))
        assertTrue("a mensagem foi: ${aviso.message}", aviso.message.contains("0,00468144 ₿"))
        assertFalse("a mensagem foi: ${aviso.message}", aviso.message.contains("satoshi"))
        assertFalse("a mensagem foi: ${aviso.message}", aviso.message.contains("468094,"))
    }

    @Test
    fun `divergencia de dois satoshis passa`() {
        assertEquals(emptyList<DraftIssue>(), DraftValidator.validate(comprovante.copy(satoshis = 468_096)))
    }

    @Test
    fun `taxa implicita negativa vira aviso`() {
        val d = comprovante.copy(feeCents = null, satoshis = 600_000)
        assertTrue(DraftValidator.validate(d).any { it.severity == Severity.WARNING })
    }

    @Test
    fun `taxa implicita acima de cinco por cento vira aviso`() {
        val d = comprovante.copy(feeCents = null, satoshis = 400_000)
        assertTrue(DraftValidator.validate(d).any { it.severity == Severity.WARNING })
    }

    /** Centavos são unidade de armazenamento; a mensagem fala com uma pessoa. */
    @Test
    fun `o aviso da taxa implicita fala em reais e nao em centavos`() {
        // 400.000 sats a R$ 315.641,52 dão R$ 1.262,57; sobram R$ 237,43 de "taxa".
        val d = comprovante.copy(feeCents = null, satoshis = 400_000)
        val aviso = DraftValidator.validate(d).single { it.severity == Severity.WARNING }

        assertTrue("a mensagem foi: ${aviso.message}", aviso.message.contains("R$"))
        assertTrue("a mensagem foi: ${aviso.message}", aviso.message.contains("237,43"))
        assertFalse("a mensagem foi: ${aviso.message}", aviso.message.contains("centavos"))
    }

    @Test
    fun `campos obrigatorios ausentes viram erro`() {
        val vazio = TransactionDraft(type = TransactionType.BUY)
        val issues = DraftValidator.validate(vazio)
        val erros = issues.filter { it.severity == Severity.ERROR }.mapNotNull { it.field }.toSet()
        assertEquals(setOf(DraftField.DATE, DraftField.FIAT, DraftField.SATS, DraftField.PRICE), erros)
    }

    @Test
    fun `valores nao positivos viram erro`() {
        val d = comprovante.copy(fiatAmountCents = 0, satoshis = 0, unitPriceCents = 0)
        val erros = DraftValidator.validate(d).filter { it.severity == Severity.ERROR }
        assertEquals(3, erros.size)
    }

    @Test
    fun `taxa negativa vira erro`() {
        val d = comprovante.copy(feeCents = -1)
        assertTrue(DraftValidator.validate(d).any { it.severity == Severity.ERROR && it.field == DraftField.FEE })
    }
}
