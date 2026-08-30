package com.pablo.btcmedio.ui.summary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.ui.theme.BtcMedioTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SummaryScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun render(
        state: SummaryState,
        onMic: () -> Unit = {},
        onPickImages: () -> Unit = {},
    ) = rule.setContent {
        BtcMedioTheme {
            SummaryScreen(
                state = state,
                onAdd = {},
                onMic = onMic,
                onPickImages = onPickImages,
                onOpenList = {},
                onOpenSettings = {},
                onOpenTransaction = {},
            )
        }
    }

    @Test
    fun microfone_desabilitado_quando_a_voz_nao_esta_utilizavel() {
        render(SummaryState(voiceEnabled = false))
        rule.onNodeWithContentDescription(
            "Registro por voz indisponível: depende do Gemini Nano"
        ).assertIsNotEnabled()
    }

    @Test
    fun microfone_habilitado_quando_voz_e_nano_estao_disponiveis() {
        render(SummaryState(voiceEnabled = true))
        rule.onNodeWithContentDescription("Registrar por voz").assertIsEnabled()
    }

    @Test
    fun microfone_desabilitado_nao_dispara_a_acao() {
        var chamou = false
        render(SummaryState(voiceEnabled = false), onMic = { chamou = true })
        rule.onNodeWithContentDescription(
            "Registro por voz indisponível: depende do Gemini Nano"
        ).performClick()
        assertTrue("o microfone desabilitado não deveria acionar a captura", !chamou)
    }

    @Test
    fun importar_da_galeria_esta_sempre_disponivel() {
        var chamou = false
        render(SummaryState(voiceEnabled = false), onPickImages = { chamou = true })
        val botao = rule.onNodeWithContentDescription("Importar comprovante da galeria")
        botao.assertIsDisplayed()
        botao.assertIsEnabled()
        botao.performClick()
        assertTrue("importar imagem não pode depender do Nano", chamou)
    }
}
