package com.pablo.btcmedio.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.model.PortfolioSummary
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.data.TransactionRepository
import com.pablo.btcmedio.ingest.NanoExtractor
import com.pablo.btcmedio.ingest.OnDeviceSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SummaryState(
    val summary: PortfolioSummary = PortfolioSummary.EMPTY,
    val recent: List<Transaction> = emptyList(),
    /**
     * O ditado por voz exige **as duas** peças: o reconhecimento on-device para
     * transcrever, e o Gemini Nano para interpretar. Só as regras não dão conta
     * de fala — elas foram feitas para a forma de um comprovante, com um valor
     * rotulado por linha, e linguagem falada não tem rótulo nenhum.
     */
    val voiceEnabled: Boolean = false,
)

class SummaryViewModel(
    repository: TransactionRepository,
    private val speech: OnDeviceSpeech,
    private val nano: NanoExtractor,
) : ViewModel() {

    private val voiceEnabled = MutableStateFlow(false)

    val state: StateFlow<SummaryState> = combine(
        repository.summary,
        repository.transactions.map { it.take(5) },
        voiceEnabled,
    ) { summary, recent, voz -> SummaryState(summary, recent, voz) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryState())

    init {
        viewModelScope.launch {
            voiceEnabled.value = speech.isAvailable() && nano.isAvailable()
        }
    }

    companion object {
        fun factory(
            repository: TransactionRepository,
            speech: OnDeviceSpeech,
            nano: NanoExtractor,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SummaryViewModel(repository, speech, nano) as T
        }
    }
}
