package com.pablo.btcmedio.ingest

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.extract.ReceiptRuleExtractor
import com.pablo.btcmedio.core.model.EntrySource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareViewModelTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun viewModel() = ShareViewModel(
        chain = ExtractorChain(listOf(ReceiptRuleExtractor())),
        ocr = OcrTextReader(context),
    )

    @Test
    fun texto_de_mensagem_vira_rascunho() = runTest {
        val vm = viewModel()
        vm.loadText(
            "Compra confirmada: R$ 1.500,00 em 30/06/2026 22:57, " +
                "Preço R$ 315.641,52, Taxa R$ 22,50, 0,00468094 BTC"
        )
        val result = vm.current.first { it != null }
        assertTrue(result is IngestResult.Ready)
        val draft = (result as IngestResult.Ready).draft
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals(2_250L, draft.feeCents)
        assertEquals(468_094L, draft.satoshis)
        assertEquals(31_564_152L, draft.unitPriceCents)
        assertEquals(EntrySource.TEXT, draft.source)
    }

    @Test
    fun texto_vazio_vira_falha_explicada() = runTest {
        val vm = viewModel()
        vm.loadText("")
        assertTrue(vm.current.first { it != null } is IngestResult.Failed)
    }

    @Test
    fun rascunho_de_falha_abre_formulario_com_data_preenchida() {
        val draft = viewModel().draftFor(IngestResult.Failed("qualquer"))
        assertTrue(draft.occurredAt != null)
    }

    @Test
    fun fila_de_imagens_avanca_uma_a_uma() = runTest {
        val vm = viewModel()
        vm.loadImages(emptyList())
        // Fila vazia: nada a avançar, e o resultado é uma falha explicada.
        assertTrue(vm.current.first { it != null } is IngestResult.Failed)
        assertEquals(false, vm.advance())
    }

    /** O app precisa aparecer no menu de compartilhar e no de seleção de texto. */
    @Test
    fun intent_filters_de_texto_estao_registrados() {
        val pm = context.packageManager

        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
        val processText = Intent(Intent.ACTION_PROCESS_TEXT).apply { type = "text/plain" }
        val sendImage = Intent(Intent.ACTION_SEND).apply { type = "image/png" }

        listOf("SEND text" to send, "PROCESS_TEXT" to processText, "SEND image" to sendImage)
            .forEach { (nome, intent) ->
                val resolvido = pm.queryIntentActivities(intent, 0)
                    .any { it.activityInfo.packageName == context.packageName }
                assertTrue("$nome não resolve para o app", resolvido)
            }
    }
}
