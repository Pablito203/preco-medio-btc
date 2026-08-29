package com.pablo.btcmedio.ingest

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Reconhecimento de texto do ML Kit com modelo embarcado no APK.
 *
 * Funciona em qualquer aparelho e sem rede — é por isso que o pipeline
 * determinístico depende dele, e não do Gemini Nano.
 */
class OcrTextReader(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** `null` quando a imagem não pôde ser lida ou nada foi reconhecido. */
    suspend fun read(uri: Uri): String? {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result -> continuation.resume(result.text.ifBlank { null }) }
                .addOnFailureListener { continuation.resume(null) }
        }
    }
}
