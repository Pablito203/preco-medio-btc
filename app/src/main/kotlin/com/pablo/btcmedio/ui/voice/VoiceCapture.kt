package com.pablo.btcmedio.ui.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.ingest.OnDeviceSpeech
import com.pablo.btcmedio.ingest.SpeechEvent
import java.time.LocalDate

/**
 * Captura por voz de ponta a ponta: permissão, escuta on-device, transcrição
 * e extração do rascunho.
 *
 * Toda saída — sucesso, silêncio, permissão negada, pacote de idioma ausente —
 * termina no formulário, com o que foi possível aproveitar.
 */
@Composable
fun VoiceCapture(
    speech: OnDeviceSpeech,
    chain: ExtractorChain,
    onDraft: (TransactionDraft) -> Unit,
    onManualEntry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasMicPermission(context)) }
    var event by remember { mutableStateOf<SpeechEvent?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { allowed ->
        granted = allowed
        if (!allowed) {
            event = SpeechEvent.Failed(
                "Sem permissão de microfone não dá para usar a voz. " +
                    "Você pode registrar a transação à mão."
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        speech.listen().collect { incoming ->
            event = incoming
            if (incoming is SpeechEvent.Final) {
                onDraft(chain.extract(incoming.text, EntrySource.AUDIO, LocalDate.now()))
            }
        }
    }

    val (titulo, corpo) = when (val e = event) {
        null, SpeechEvent.Ready -> "Ouvindo…" to
            "Diga, por exemplo: comprei mil e quinhentos reais de bitcoin hoje a trezentos e quinze mil."

        is SpeechEvent.Partial -> "Ouvindo…" to e.text
        is SpeechEvent.Final -> "Entendido" to e.text
        is SpeechEvent.Failed -> "Não foi possível usar a voz" to e.message
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = { Text(corpo) },
        confirmButton = {
            if (event is SpeechEvent.Failed) {
                TextButton(onClick = onManualEntry) { Text("Preencher à mão") }
            } else {
                TextButton(onClick = onDismiss) { Text("Fechar") }
            }
        },
        dismissButton = {
            if (event is SpeechEvent.Failed) {
                TextButton(onClick = onDismiss) { Text("Fechar") }
            }
        },
    )
}

private fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
