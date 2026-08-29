package com.pablo.btcmedio.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MonthGroup(val label: String, val transactions: List<Transaction>)

class TransactionListViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val monthFormat =
        DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))

    /** Guarda a última exclusão para permitir desfazer sem tocar no banco. */
    private var lastDeleted: Transaction? = null

    val months: StateFlow<List<MonthGroup>> = repository.transactions
        .map { list ->
            list.groupBy { t ->
                monthFormat.format(Instant.ofEpochMilli(t.occurredAt).atZone(ZoneId.systemDefault()))
            }.map { (label, txs) ->
                MonthGroup(label.replaceFirstChar { it.uppercase() }, txs)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
        fun factory(repository: TransactionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TransactionListViewModel(repository) as T
        }
    }
}
