package com.pablo.btcmedio.ui.home

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.ui.theme.BtcMedioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun compra(id: String) = Transaction(
        id = id,
        type = TransactionType.BUY,
        occurredAt = 1_782_000_000_000L,
        fiatAmountCents = 150_000,
        feeCents = 2_250,
        satoshis = 468_094,
        unitPriceCents = 31_564_152,
        source = EntrySource.MANUAL,
        note = null,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun render(
        state: HomeState,
        onOpenInfo: () -> Unit = {},
        onOpenTransaction: (String) -> Unit = {},
        onReadReceipt: () -> Unit = {},
        onTypeValues: () -> Unit = {},
        onDictate: () -> Unit = {},
        onDelete: (Transaction) -> Unit = {},
        onUndoDelete: () -> Unit = {},
    ) = rule.setContent {
        BtcMedioTheme {
            HomeScreen(
                state = state,
                onOpenInfo = onOpenInfo,
                onOpenTransaction = onOpenTransaction,
                onReadReceipt = onReadReceipt,
                onTypeValues = onTypeValues,
                onDictate = onDictate,
                onDelete = onDelete,
                onUndoDelete = onUndoDelete,
            )
        }
    }

    private fun abrirFolhaDeRegistro() {
        rule.onNodeWithText("Registrar transação").performClick()
        rule.onNodeWithText("Como registrar?").assertIsDisplayed()
    }

    // --- A folha de registro ---------------------------------------------

    @Test
    fun ditar_por_voz_fica_apagado_quando_a_voz_nao_esta_utilizavel() {
        render(HomeState(voiceEnabled = false))
        abrirFolhaDeRegistro()

        rule.onNodeWithText("Ditar por voz").assertIsNotEnabled()
        rule.onNodeWithText("Indisponível neste aparelho").assertIsDisplayed()
    }

    @Test
    fun ditar_por_voz_fica_ativo_quando_voz_e_nano_estao_disponiveis() {
        render(HomeState(voiceEnabled = true))
        abrirFolhaDeRegistro()

        rule.onNodeWithText("Ditar por voz").assertIsEnabled()
    }

    @Test
    fun ditar_por_voz_apagado_nao_dispara_a_acao() {
        var chamou = false
        render(HomeState(voiceEnabled = false), onDictate = { chamou = true })
        abrirFolhaDeRegistro()

        rule.onNodeWithText("Ditar por voz").performClick()

        assertTrue("a opção apagada não deveria acionar a captura", !chamou)
    }

    /** A leitura de comprovante é OCR puro: não depende do Gemini Nano. */
    @Test
    fun ler_comprovante_esta_sempre_disponivel() {
        var chamou = false
        render(HomeState(voiceEnabled = false), onReadReceipt = { chamou = true })
        abrirFolhaDeRegistro()

        val opcao = rule.onNodeWithText("Ler comprovante")
        opcao.assertIsDisplayed()
        opcao.assertIsEnabled()
        opcao.performClick()

        assertTrue("ler comprovante não pode depender do Nano", chamou)
    }

    @Test
    fun digitar_valores_abre_o_formulario() {
        var chamou = false
        render(HomeState(), onTypeValues = { chamou = true })
        abrirFolhaDeRegistro()

        rule.onNodeWithText("Digitar valores").performClick()

        assertTrue(chamou)
    }

    // --- Barra superior ---------------------------------------------------

    /** A tela é só informativa: nada de "Ajustes" nem de engrenagem prometendo configuração. */
    @Test
    fun o_acesso_a_tela_informativa_nao_se_chama_ajustes() {
        var chamou = false
        render(HomeState(), onOpenInfo = { chamou = true })

        rule.onNodeWithContentDescription("Ajustes").assertDoesNotExist()
        rule.onNodeWithContentDescription("Mais opções").performClick()
        rule.onNodeWithText("Ajustes").assertDoesNotExist()
        rule.onNodeWithText("Informações").performClick()

        assertTrue(chamou)
    }

    // --- Abas -------------------------------------------------------------

    @Test
    fun o_resumo_abre_primeiro_e_a_aba_de_transacoes_mostra_o_historico() {
        render(HomeState(months = listOf(MonthGroup("Junho 2026", listOf(compra("a"))))))

        rule.onNodeWithText("Total comprado").assertIsDisplayed()
        rule.onNodeWithText("Compra").assertDoesNotExist()

        rule.onNodeWithText("Transações").performClick()

        rule.onNodeWithText("Compra").assertIsDisplayed()
        rule.onNodeWithText("Total comprado").assertDoesNotExist()
    }

    // --- Deslizar para excluir, na aba de transações ----------------------

    /**
     * O "Desfazer" devolve a linha à lista com o mesmo id. O estado de deslize
     * é guardado por chave pela LazyColumn, então ele não pode voltar junto —
     * se voltar, a linha se exclui sozinha e o desfazer parece não fazer nada.
     */
    @Test
    fun a_transacao_restaurada_nao_e_excluida_de_novo() {
        val transacoes = mutableStateListOf(compra("a"))
        var exclusoes = 0

        rule.setContent {
            BtcMedioTheme {
                HomeScreen(
                    state = HomeState(months = meses(transacoes.toList())),
                    onOpenInfo = {},
                    onOpenTransaction = {},
                    onReadReceipt = {},
                    onTypeValues = {},
                    onDictate = {},
                    onDelete = { exclusoes++ },
                    onUndoDelete = {},
                )
            }
        }

        rule.onNodeWithText("Transações").performClick()
        rule.onNodeWithText("Compra").performTouchInput { swipeLeft() }
        rule.waitUntil(5_000) { exclusoes == 1 }

        // A remoção acontece DEPOIS do gesto, como no app real: quem apaga é o
        // Flow do Room reagindo à exclusão, não o próprio callback. É essa folga
        // que dá à LazyColumn a chance de salvar o estado de deslize do item.
        rule.waitForIdle()
        transacoes.clear()
        rule.waitForIdle()

        // O desfazer devolve a mesma transação, com o mesmo id.
        transacoes.add(compra("a"))
        rule.waitForIdle()

        assertEquals("a transação restaurada foi excluída sozinha", 1, exclusoes)
        rule.onNodeWithText("Compra").assertExists()
    }

    @Test
    fun tocar_em_desfazer_no_snackbar_aciona_o_desfazer() {
        val transacoes = mutableStateListOf(compra("a"))
        var desfazeres = 0

        rule.setContent {
            BtcMedioTheme {
                HomeScreen(
                    state = HomeState(months = meses(transacoes.toList())),
                    onOpenInfo = {},
                    onOpenTransaction = {},
                    onReadReceipt = {},
                    onTypeValues = {},
                    onDictate = {},
                    onDelete = { transacoes.clear() },
                    onUndoDelete = { desfazeres++ },
                )
            }
        }

        rule.onNodeWithText("Transações").performClick()
        rule.onNodeWithText("Compra").performTouchInput { swipeLeft() }
        rule.waitUntil(5_000) { transacoes.isEmpty() }

        rule.onNodeWithText("Transação excluída").assertExists()
        rule.onNodeWithText("Desfazer").performClick()
        rule.waitUntil(5_000) { desfazeres == 1 }

        assertEquals(1, desfazeres)
    }

    @Test
    fun deslizar_exclui_uma_unica_vez() {
        val transacoes = mutableStateListOf(compra("a"))
        var exclusoes = 0

        rule.setContent {
            BtcMedioTheme {
                HomeScreen(
                    state = HomeState(months = meses(transacoes.toList())),
                    onOpenInfo = {},
                    onOpenTransaction = {},
                    onReadReceipt = {},
                    onTypeValues = {},
                    onDictate = {},
                    onDelete = {
                        exclusoes++
                        transacoes.clear()
                    },
                    onUndoDelete = {},
                )
            }
        }

        rule.onNodeWithText("Transações").performClick()
        rule.onNodeWithText("Compra").performTouchInput { swipeLeft() }
        rule.waitUntil(5_000) { exclusoes >= 1 }
        rule.waitForIdle()

        assertEquals(1, exclusoes)
    }

    private fun meses(transacoes: List<Transaction>): List<MonthGroup> =
        if (transacoes.isEmpty()) emptyList() else listOf(MonthGroup("Junho 2026", transacoes))
}
