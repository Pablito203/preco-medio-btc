package com.pablo.btcmedio.ui.form

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.ui.theme.BtcMedioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionFormScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val comprovante = TransactionDraft(
        type = TransactionType.BUY,
        occurredAt = 1_782_000_000_000L,
        fiatAmountCents = 150_000,
        feeCents = 2_250,
        satoshis = 468_094,
        unitPriceCents = 31_564_152,
        rawText = "Detalhes da transação",
    )

    private fun render(state: FormState, actions: FormActions = FormActions()) =
        rule.setContent {
            BtcMedioTheme { TransactionFormScreen(state = state, actions = actions, onDone = {}) }
        }

    @Test
    fun exibe_os_valores_pre_preenchidos() {
        render(FormState.from(comprovante))
        rule.onNodeWithText("1.500,00").assertIsDisplayed()
        rule.onNodeWithText("22,50").assertIsDisplayed()
        rule.onNodeWithText("0,00468094").assertIsDisplayed()
        rule.onNodeWithText("315.641,52").assertIsDisplayed()
    }

    /**
     * O texto bruto do OCR continua no rascunho, mas não é exibido: para quem
     * usa o app ele é ruído, não informação.
     */
    @Test
    fun nao_exibe_o_texto_reconhecido() {
        render(FormState.from(comprovante))
        rule.onNodeWithText("Texto reconhecido").assertDoesNotExist()
        rule.onNodeWithText("Detalhes da transação").assertDoesNotExist()
    }

    @Test
    fun botao_salvar_desabilitado_quando_faltam_campos() {
        render(FormState.from(TransactionDraft()))
        rule.onNodeWithText("Salvar").assertIsNotEnabled()
    }

    @Test
    fun aviso_de_incoerencia_aparece() {
        render(FormState.from(comprovante.copy(satoshis = 600_000)))
        rule.onNodeWithText("Os números não fecham", substring = true).assertIsDisplayed()
    }

    @Test
    fun completar_campo_faltante_dispara_a_acao() {
        var chamou = false
        render(
            FormState.from(comprovante.copy(satoshis = null)),
            FormActions(onCompleteMissing = { chamou = true }),
        )
        rule.onNodeWithText("Completar campo faltante").performScrollTo().performClick()
        assertTrue(chamou)
    }

    @Test
    fun digitar_valor_propaga_o_texto_bruto() {
        val digitados = mutableListOf<String>()
        render(FormState.from(TransactionDraft()), FormActions(onFiatChange = { digitados.add(it) }))
        rule.onNodeWithText("Valor em reais").performTextInput("1500")
        assertEquals("1500", digitados.last())
    }

    @Test
    fun alternar_para_venda_dispara_a_acao() {
        var tipo: TransactionType? = null
        render(FormState.from(comprovante), FormActions(onTypeChange = { tipo = it }))
        rule.onNodeWithText("Venda").performClick()
        assertEquals(TransactionType.SELL, tipo)
    }

    @Test
    fun erro_de_venda_acima_do_saldo_e_exibido() {
        render(
            FormState.from(comprovante).copy(saveError = "Saldo insuficiente: você tem 0,00100000 BTC.")
        )
        rule.onNodeWithText("Saldo insuficiente", substring = true).assertIsDisplayed()
    }

    @Test
    fun botao_excluir_so_aparece_na_edicao() {
        render(FormState.from(comprovante).copy(isEditing = true))
        rule.onNodeWithText("Excluir").performScrollTo().assertIsDisplayed()
    }
}
