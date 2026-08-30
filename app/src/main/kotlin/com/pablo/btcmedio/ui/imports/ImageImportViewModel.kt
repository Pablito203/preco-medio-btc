package com.pablo.btcmedio.ui.imports

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.ingest.OcrTextReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ImageImportState(
    val loading: Boolean = false,
    /** Não nulo quando há confirmações prontas para a tela do formulário. */
    val drafts: List<TransactionDraft>? = null,
    val error: String? = null,
)

/**
 * Importação de comprovantes escolhidos dentro do app, pelo seletor de fotos
 * do Android.
 *
 * O seletor de fotos não exige permissão de galeria: o sistema devolve apenas
 * os Uris que o usuário escolheu, e o app nunca enxerga o resto das imagens.
 *
 * O caminho depois disso é o mesmo do compartilhamento vindo de outro app:
 * OCR, cadeia de extração, e uma confirmação por imagem.
 */
class ImageImportViewModel(
    private val ocr: OcrTextReader,
    private val chain: ExtractorChain,
) : ViewModel() {

    private val _state = MutableStateFlow(ImageImportState())
    val state: StateFlow<ImageImportState> = _state.asStateFlow()

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _state.value = ImageImportState(loading = true)
        viewModelScope.launch {
            val drafts = uris.mapNotNull { uri ->
                ocr.read(uri)?.let { texto ->
                    chain.extract(texto, EntrySource.IMAGE, LocalDate.now())
                }
            }
            _state.value = if (drafts.isEmpty()) {
                ImageImportState(
                    error = if (uris.size == 1) {
                        "Não foi possível ler texto nesta imagem. Registre a transação à mão."
                    } else {
                        "Não foi possível ler texto em nenhuma das imagens. " +
                            "Registre a transação à mão."
                    }
                )
            } else {
                ImageImportState(drafts = drafts)
            }
        }
    }

    /** Chamado depois que a interface consumiu os rascunhos ou dispensou o erro. */
    fun clear() {
        _state.value = ImageImportState()
    }

    companion object {
        fun factory(ocr: OcrTextReader, chain: ExtractorChain) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ImageImportViewModel(ocr, chain) as T
        }
    }
}
