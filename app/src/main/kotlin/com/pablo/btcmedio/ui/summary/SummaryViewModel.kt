package com.pablo.btcmedio.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.model.PortfolioSummary
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SummaryState(
    val summary: PortfolioSummary = PortfolioSummary.EMPTY,
    val recent: List<Transaction> = emptyList(),
)

class SummaryViewModel(repository: TransactionRepository) : ViewModel() {

    val state: StateFlow<SummaryState> = combine(
        repository.summary,
        repository.transactions.map { it.take(5) },
    ) { summary, recent -> SummaryState(summary, recent) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryState())

    companion object {
        fun factory(repository: TransactionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SummaryViewModel(repository) as T
        }
    }
}
