package com.pablo.btcmedio.ingest

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
import com.pablo.btcmedio.core.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Pipeline completo de ingestão: imagem de comprovante -> OCR -> regras -> rascunho.
 *
 * A imagem é desenhada aqui reproduzindo o layout do comprovante real da
 * corretora (tema escuro, rótulos e valores nas mesmas posições), para que o
 * teste exercite o OCR de verdade em vez de partir de texto já pronto.
 */
@RunWith(AndroidJUnit4::class)
class ReceiptIngestionEndToEndTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val chain = ExtractorChain(listOf(ReceiptRuleExtractor()))

    private fun comprovanteRenderizado(): Uri {
        val largura = 1080
        val altura = 1500
        val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)

        val titulo = Paint().apply {
            color = Color.WHITE
            textSize = 62f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rotulo = Paint().apply {
            color = Color.rgb(150, 150, 150)
            textSize = 40f
            isAntiAlias = true
        }
        val valor = Paint().apply {
            color = Color.WHITE
            textSize = 52f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Detalhes da transação", 60f, 120f, titulo)
        canvas.drawText("- R$ 1.500,00", 60f, 260f, valor)
        canvas.drawText("+ BTC 0,00468094", 60f, 340f, valor)
        canvas.drawText("Compra", 60f, 470f, rotulo)
        canvas.drawText("30/06/2026 22:57", 60f, 540f, rotulo)
        canvas.drawText("Preço", 60f, 700f, rotulo)
        canvas.drawText("R$ 315.641,52", 60f, 780f, valor)
        canvas.drawText("Taxa", 60f, 920f, rotulo)
        canvas.drawText("R$ 22,50", 60f, 1000f, valor)
        canvas.drawText("Total Comprado", 60f, 1140f, rotulo)
        canvas.drawText("R$ 1.477,50", 60f, 1220f, valor)

        val file = File(context.cacheDir, "comprovante-${System.nanoTime()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun comprovante_da_corretora_vira_rascunho_completo() = runTest {
        val texto = OcrTextReader(context).read(comprovanteRenderizado())
        requireNotNull(texto) { "o OCR não leu nada da imagem do comprovante" }

        val draft = chain.extract(texto, EntrySource.IMAGE, LocalDate.of(2026, 8, 29))

        assertEquals("texto reconhecido: $texto", TransactionType.BUY, draft.type)
        assertEquals("texto reconhecido: $texto", 150_000L, draft.fiatAmountCents)
        assertEquals("texto reconhecido: $texto", 2_250L, draft.feeCents)
        assertEquals("texto reconhecido: $texto", 468_094L, draft.satoshis)
        assertEquals("texto reconhecido: $texto", 31_564_152L, draft.unitPriceCents)
        assertEquals(EntrySource.IMAGE, draft.source)
    }
}
