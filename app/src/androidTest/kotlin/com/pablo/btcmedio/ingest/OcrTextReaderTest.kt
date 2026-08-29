package com.pablo.btcmedio.ingest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OcrTextReaderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val reader = OcrTextReader(context)

    /** Gera uma imagem com texto real, para exercitar o OCR de verdade. */
    private fun bitmapComTexto(linhas: List<String>): Uri {
        val bitmap = Bitmap.createBitmap(900, 120 + linhas.size * 90, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 56f
                isAntiAlias = true
            }
            linhas.forEachIndexed { i, linha -> drawText(linha, 40f, 100f + i * 90f, paint) }
        }
        val file = File(context.cacheDir, "ocr-teste-${System.nanoTime()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun le_texto_de_um_comprovante_sintetico() = runTest {
        val uri = bitmapComTexto(listOf("Compra", "R\$ 1.500,00", "30/06/2026 22:57"))
        val texto = reader.read(uri)
        assertNotNull(texto)
        assertTrue("texto lido: $texto", texto!!.contains("1.500"))
        assertTrue("texto lido: $texto", texto.contains("30/06/2026"))
    }

    @Test
    fun imagem_em_branco_devolve_texto_vazio_ou_null() = runTest {
        val uri = bitmapComTexto(emptyList())
        val texto = reader.read(uri)
        assertTrue("texto lido: $texto", texto == null || texto.isBlank())
    }

    @Test
    fun uri_inexistente_devolve_null_sem_lancar() = runTest {
        assertNull(reader.read(Uri.parse("file:///nao/existe.png")))
    }
}
