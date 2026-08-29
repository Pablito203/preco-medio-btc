package com.pablo.btcmedio.ingest

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.model.EntrySource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ShareViewModel(
    private val chain: ExtractorChain,
    private val ocr: OcrTextReader,
) : ViewModel() {

    private val _queue = MutableStateFlow<List<IngestResult>>(emptyList())
    private val _current = MutableStateFlow<IngestResult?>(null)
    private val _loading = MutableStateFlow(true)

    val current: StateFlow<IngestResult?> = _current.asStateFlow()
    val remaining: StateFlow<List<IngestResult>> = _queue.asStateFlow()
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Várias imagens viram uma fila de confirmações, uma a uma. */
    fun loadImages(uris: List<Uri>) {
        if (uris.isEmpty()) {
            failWith("Nenhuma imagem recebida. Preencha os dados à mão.")
            return
        }
        _loading.value = true
        viewModelScope.launch {
            val results = uris.map { uri ->
                when (val text = ocr.read(uri)) {
                    null -> IngestResult.Failed(
                        "Não foi possível ler texto nesta imagem. Preencha os dados à mão."
                    )

                    else -> IngestResult.Ready(chain.extract(text, EntrySource.IMAGE, LocalDate.now()))
                }
            }
            _current.value = results.firstOrNull()
            _queue.value = results.drop(1)
            _loading.value = false
        }
    }

    fun loadText(text: String, source: EntrySource = EntrySource.TEXT) {
        if (text.isBlank()) {
            failWith("Nenhum texto recebido. Preencha os dados à mão.")
            return
        }
        _loading.value = true
        viewModelScope.launch {
            _current.value = IngestResult.Ready(chain.extract(text, source, LocalDate.now()))
            _loading.value = false
        }
    }

    /** Avança para a próxima confirmação da fila. `false` quando a fila acabou. */
    fun advance(): Boolean {
        val next = _queue.value.firstOrNull() ?: run {
            _current.value = null
            return false
        }
        _current.value = next
        _queue.value = _queue.value.drop(1)
        return true
    }

    /**
     * O rascunho a exibir. Uma falha ainda abre o formulário — com a data de
     * hoje preenchida — em vez de deixar o usuário sem saída.
     */
    fun draftFor(result: IngestResult?): TransactionDraft = when (result) {
        is IngestResult.Ready -> result.draft
        else -> TransactionDraft(occurredAt = System.currentTimeMillis())
    }

    private fun failWith(message: String) {
        _current.value = IngestResult.Failed(message)
        _loading.value = false
    }

    companion object {
        fun factory(chain: ExtractorChain, ocr: OcrTextReader) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ShareViewModel(chain, ocr) as T
        }
    }
}
