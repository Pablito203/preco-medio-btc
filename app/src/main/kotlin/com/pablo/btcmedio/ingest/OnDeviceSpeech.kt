package com.pablo.btcmedio.ingest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed interface SpeechEvent {
    data object Ready : SpeechEvent
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    data class Failed(val message: String) : SpeechEvent
}

/**
 * Reconhecimento de voz estritamente on-device.
 *
 * O áudio nunca é gravado em arquivo: a transcrição acontece em fluxo e só o
 * texto sobrevive. Não há arquivo para gerenciar nem para vazar.
 */
class OnDeviceSpeech(private val context: Context) {

    fun isAvailable(): Boolean = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun listen(languageTag: String = "pt-BR"): Flow<SpeechEvent> = callbackFlow {
        if (!isAvailable()) {
            trySend(SpeechEvent.Failed(PACOTE_AUSENTE))
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechEvent.Ready)
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { trySend(SpeechEvent.Partial(it)) }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (text.isNullOrBlank()) {
                    trySend(SpeechEvent.Failed("Não entendi. Tente de novo ou preencha à mão."))
                } else {
                    trySend(SpeechEvent.Final(text))
                }
                close()
            }

            override fun onError(error: Int) {
                trySend(SpeechEvent.Failed(messageFor(error)))
                close()
            }
        })

        recognizer.startListening(intent)
        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    private fun messageFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Erro ao capturar o áudio."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Permissão de microfone negada. Conceda-a nas configurações do app."

        SpeechRecognizer.ERROR_NO_MATCH -> "Não entendi. Tente de novo ou preencha à mão."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nenhuma fala detectada."
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        -> PACOTE_AUSENTE

        else -> "Falha no reconhecimento de voz. Preencha os dados à mão."
    }

    companion object {
        const val PACOTE_AUSENTE =
            "O reconhecimento de voz offline não está disponível neste aparelho. " +
                "Baixe o pacote de idioma português em Ajustes do Android > Sistema > " +
                "Idiomas e entrada > Reconhecimento de voz. Enquanto isso, use o formulário manual."
    }
}
