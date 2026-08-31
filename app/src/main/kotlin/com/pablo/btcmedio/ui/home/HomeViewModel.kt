package com.pablo.btcmedio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.model.PortfolioSummary
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.data.TransactionRepository
import com.pablo.btcmedio.ingest.NanoExtractor
import com.pablo.btcmedio.ingest.OnDeviceSpeech
import com.pablo.btcmedio.ui.format.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** As transações de um mês, na ordem em que a lista as mostra. */
data class MonthGroup(val label: String, val transactions: List<Transaction>)

data class HomeState(
    val summary: PortfolioSummary = PortfolioSummary.EMPTY,
    val months: List<MonthGroup> = emptyList(),
    /**
     * O ditado por voz exige **as duas** peças: o reconhecimento on-device para
     * transcrever, e o Gemini Nano para interpretar. Só as regras não dão conta
     * de fala — elas foram feitas para a forma de um comprovante, com um valor
     * rotulado por linha, e linguagem falada não tem rótulo nenhum.
     */
    val voiceEnabled: Boolean = false,
)

/**
 * Resumo e histórico saem do mesmo ViewModel porque agora são duas abas da
 * mesma tela: separá-los faria duas assinaturas do mesmo Flow do Room, e a
 * troca de aba recalcularia o que já estava na mão.
 */
class HomeViewModel(
    private val repository: TransactionRepository,
    private val speech: OnDeviceSpeech,
    private val nano: NanoExtractor,
) : ViewModel() {

    private val voiceEnabled = MutableStateFlow(false)

    /** Guarda a última exclusão para permitir desfazer sem tocar no banco. */
    private var lastDeleted: Transaction? = null

    val state: StateFlow<HomeState> = combine(
        repository.summary,
        repository.transactions,
        voiceEnabled,
    ) { summary, transactions, voz ->
        HomeState(summary = summary, months = agrupar(transactions), voiceEnabled = voz)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    init {
        viewModelScope.launch {
            voiceEnabled.value = speech.isAvailable() && nano.isAvailable()
        }
    }

    private fun agrupar(transactions: List<Transaction>): List<MonthGroup> =
        transactions
            .groupBy { Formatters.month(it.occurredAt) }
            .map { (label, txs) -> MonthGroup(label, txs) }

    fun delete(transaction: Transaction) {
        lastDeleted = transaction
        viewModelScope.launch { repository.delete(transaction.id) }
    }

    fun undoDelete() {
        val restored = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repository.save(restored) }
    }

    companion object {
        fun factory(
            repository: TransactionRepository,
            speech: OnDeviceSpeech,
            nano: NanoExtractor,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(repository, speech, nano) as T
        }
    }
}
