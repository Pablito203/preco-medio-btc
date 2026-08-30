package com.pablo.btcmedio.ui.imports

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.extract.ReceiptRuleExtractor
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.ingest.OcrTextReader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Importação de comprovantes escolhidos dentro do app.
 *
 * As imagens são desenhadas aqui reproduzindo o comprovante real, para que o
 * teste exercite OCR e regras de verdade, não texto já pronto.
 */
@RunWith(AndroidJUnit4::class)
class ImageImportViewModelTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun viewModel() = ImageImportViewModel(
        ocr = OcrTextReader(context),
        chain = ExtractorChain(listOf(ReceiptRuleExtractor())),
    )

    private fun comprovante(valor: String, bitcoin: String, cotacao: String, taxa: String): Uri {
        val bitmap = Bitmap.createBitmap(1080, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val rotulo = Paint().apply {
            color = Color.rgb(150, 150, 150); textSize = 40f; isAntiAlias = true
        }
        val valorPaint = Paint().apply {
            color = Color.WHITE
            textSize = 52f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Compra", 60f, 100f, rotulo)
        canvas.drawText("30/06/2026 22:57", 60f, 170f, rotulo)
        canvas.drawText("- R\$ $valor", 60f, 290f, valorPaint)
        canvas.drawText("+ BTC $bitcoin", 60f, 370f, valorPaint)
        canvas.drawText("Preço", 60f, 500f, rotulo)
        canvas.drawText("R\$ $cotacao", 60f, 570f, valorPaint)
        canvas.drawText("Taxa", 60f, 700f, rotulo)
        canvas.drawText("R\$ $taxa", 60f, 770f, valorPaint)

        val file = File(context.cacheDir, "import-${System.nanoTime()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    private fun imagemEmBranco(): Uri {
        val bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.WHITE)
        val file = File(context.cacheDir, "branco-${System.nanoTime()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun uma_imagem_vira_um_rascunho() = runTest {
        val vm = viewModel()
        vm.import(listOf(comprovante("1.500,00", "0,00468094", "315.641,52", "22,50")))

        val drafts = vm.state.first { it.drafts != null }.drafts!!
        assertEquals(1, drafts.size)
        val draft = drafts.single()
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals(468_094L, draft.satoshis)
        assertEquals(31_564_152L, draft.unitPriceCents)
        assertEquals(2_250L, draft.feeCents)
        assertEquals(EntrySource.IMAGE, draft.source)
    }

    @Test
    fun varias_imagens_viram_uma_fila_de_rascunhos() = runTest {
        val vm = viewModel()
        vm.import(
            listOf(
                comprovante("1.500,00", "0,00468094", "315.641,52", "22,50"),
                comprovante("1.000,00", "0,00286264", "344.087,16", "10,00"),
            )
        )

        val drafts = vm.state.first { it.drafts != null }.drafts!!
        assertEquals(2, drafts.size)
        assertEquals(150_000L, drafts[0].fiatAmountCents)
        assertEquals(100_000L, drafts[1].fiatAmountCents)
    }

    @Test
    fun imagem_sem_texto_vira_erro_explicado_em_vez_de_rascunho_vazio() = runTest {
        val vm = viewModel()
        vm.import(listOf(imagemEmBranco()))

        val state = vm.state.first { !it.loading && (it.error != null || it.drafts != null) }
        assertNull(state.drafts)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("à mão"))
    }

    @Test
    fun lista_vazia_nao_faz_nada() = runTest {
        val vm = viewModel()
        vm.import(emptyList())
        val state = vm.state.first()
        assertNull(state.drafts)
        assertNull(state.error)
        assertTrue(!state.loading)
    }

    @Test
    fun clear_devolve_o_estado_inicial() = runTest {
        val vm = viewModel()
        vm.import(listOf(comprovante("1.500,00", "0,00468094", "315.641,52", "22,50")))
        vm.state.first { it.drafts != null }

        vm.clear()
        val state = vm.state.first()
        assertNull(state.drafts)
        assertNull(state.error)
        assertTrue(!state.loading)
    }
}
