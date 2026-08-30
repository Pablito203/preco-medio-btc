package com.pablo.btcmedio.ui.list

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionListScreenTest {

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
                TransactionListScreen(
                    months = if (transacoes.isEmpty()) {
                        emptyList()
                    } else {
                        listOf(MonthGroup("Junho de 2026", transacoes.toList()))
                    },
                    onBack = {},
                    onOpen = {},
                    onDelete = { exclusoes++ },
                    onUndo = {},
                )
            }
        }

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
                TransactionListScreen(
                    months = if (transacoes.isEmpty()) {
                        emptyList()
                    } else {
                        listOf(MonthGroup("Junho de 2026", transacoes.toList()))
                    },
                    onBack = {},
                    onOpen = {},
                    onDelete = { transacoes.clear() },
                    onUndo = { desfazeres++ },
                )
            }
        }

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
                TransactionListScreen(
                    months = if (transacoes.isEmpty()) {
                        emptyList()
                    } else {
                        listOf(MonthGroup("Junho de 2026", transacoes.toList()))
                    },
                    onBack = {},
                    onOpen = {},
                    onDelete = {
                        exclusoes++
                        transacoes.clear()
                    },
                    onUndo = {},
                )
            }
        }

        rule.onNodeWithText("Compra").performTouchInput { swipeLeft() }
        rule.waitUntil(5_000) { exclusoes >= 1 }
        rule.waitForIdle()

        assertEquals(1, exclusoes)
    }
}
