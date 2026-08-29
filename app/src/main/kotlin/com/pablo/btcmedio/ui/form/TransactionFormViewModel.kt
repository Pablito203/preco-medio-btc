package com.pablo.btcmedio.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.draft.DraftCompleter
import com.pablo.btcmedio.core.draft.DraftIssue
import com.pablo.btcmedio.core.draft.DraftValidator
import com.pablo.btcmedio.core.draft.Severity
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.core.parse.BrazilianNumberParser
import com.pablo.btcmedio.data.SaveResult
import com.pablo.btcmedio.data.TransactionRepository
import com.pablo.btcmedio.ui.format.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * O estado guarda o texto digitado **e** o valor interpretado. Guardar só o
 * número quebraria a digitação (apagar a vírgula reformataria o campo);
 * guardar só o texto empurraria parsing para dentro dos composables.
 */
data class FormState(
    val draft: TransactionDraft = TransactionDraft(),
    val fiatText: String = "",
    val feeText: String = "",
    val satsText: String = "",
    val priceText: String = "",
    val noteText: String = "",
    val issues: List<DraftIssue> = emptyList(),
    val saveError: String? = null,
    val saved: Boolean = false,
    val isEditing: Boolean = false,
    val canComplete: Boolean = false,
) {
    val canSave: Boolean get() = issues.none { it.severity == Severity.ERROR }

    fun issueFor(field: com.pablo.btcmedio.core.draft.DraftField): DraftIssue? =
        issues.firstOrNull { it.field == field }

    companion object {
        fun from(draft: TransactionDraft, isEditing: Boolean = false): FormState = FormState(
            draft = draft,
            fiatText = draft.fiatAmountCents?.let { plainCents(it) } ?: "",
            feeText = draft.feeCents?.let { plainCents(it) } ?: "",
            satsText = draft.satoshis?.let { Formatters.btc(it) } ?: "",
            priceText = draft.unitPriceCents?.let { plainCents(it) } ?: "",
            noteText = draft.note.orEmpty(),
            issues = DraftValidator.validate(draft),
            isEditing = isEditing,
            canComplete = DraftCompleter.missingField(draft) != null,
        )

        /** "R$ 1.500,00" -> "1.500,00" (o ICU usa espaço não separável). */
        private fun plainCents(cents: Long) =
            Formatters.brl(cents).replace("R$", "").replace(' ', ' ').trim()
    }
}

/** Callbacks do formulário, agrupados para manter o composable testável sem ViewModel. */
data class FormActions(
    val onTypeChange: (TransactionType) -> Unit = {},
    val onDateChange: (Long) -> Unit = {},
    val onFiatChange: (String) -> Unit = {},
    val onFeeChange: (String) -> Unit = {},
    val onSatsChange: (String) -> Unit = {},
    val onPriceChange: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onCompleteMissing: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

class TransactionFormViewModel(
    private val repository: TransactionRepository,
    private val transactionId: String?,
    initialDraft: TransactionDraft?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        FormState.from(initialDraft ?: TransactionDraft(), isEditing = transactionId != null)
    )
    val state: StateFlow<FormState> = _state.asStateFlow()

    init {
        if (transactionId != null) {
            viewModelScope.launch {
                repository.byId(transactionId)?.let { existing ->
                    _state.value = FormState.from(TransactionDraft.from(existing), isEditing = true)
                }
            }
        }
    }

    fun onTypeChange(type: TransactionType) = mutate { it.copy(type = type) }

    fun onDateChange(epochMillis: Long) = mutate { it.copy(occurredAt = epochMillis) }

    fun onFiatChange(text: String) = mutateText(text, { s, t -> s.copy(fiatText = t) }) { d, v ->
        d.copy(fiatAmountCents = v?.let { BrazilianNumberParser.parseCents(it) })
    }

    fun onFeeChange(text: String) = mutateText(text, { s, t -> s.copy(feeText = t) }) { d, v ->
        d.copy(feeCents = v?.let { BrazilianNumberParser.parseCents(it) })
    }

    fun onSatsChange(text: String) = mutateText(text, { s, t -> s.copy(satsText = t) }) { d, v ->
        d.copy(satoshis = v?.let { BrazilianNumberParser.parseSatoshis(it) })
    }

    fun onPriceChange(text: String) = mutateText(text, { s, t -> s.copy(priceText = t) }) { d, v ->
        d.copy(unitPriceCents = v?.let { BrazilianNumberParser.parseCents(it) })
    }

    fun onNoteChange(text: String) {
        _state.update { it.copy(noteText = text, draft = it.draft.copy(note = text.ifBlank { null })) }
    }

    fun completeMissing() {
        val current = _state.value
        val completed = DraftCompleter.complete(current.draft)
        _state.value = FormState.from(completed, isEditing = current.isEditing)
            .copy(noteText = current.noteText)
    }

    fun save() {
        val draft = _state.value.draft
        if (!draft.isComplete()) return
        viewModelScope.launch {
            val transaction = draft.toTransaction(
                id = transactionId ?: UUID.randomUUID().toString(),
                now = System.currentTimeMillis(),
            )
            when (val result = repository.save(transaction)) {
                is SaveResult.Success -> _state.update { it.copy(saved = true, saveError = null) }
                is SaveResult.Oversold -> _state.update {
                    it.copy(
                        saveError = "Saldo insuficiente nessa data: você tinha " +
                            "${Formatters.btc(result.availableSats)} BTC e está vendendo " +
                            "${Formatters.btc(result.requestedSats)} BTC."
                    )
                }
            }
        }
    }

    fun delete() {
        val id = transactionId ?: return
        viewModelScope.launch {
            repository.delete(id)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun mutate(transform: (TransactionDraft) -> TransactionDraft) {
        _state.update { current ->
            val draft = transform(current.draft)
            current.copy(
                draft = draft,
                issues = DraftValidator.validate(draft),
                canComplete = DraftCompleter.missingField(draft) != null,
                saveError = null,
            )
        }
    }

    private fun mutateText(
        text: String,
        setText: (FormState, String) -> FormState,
        transform: (TransactionDraft, String?) -> TransactionDraft,
    ) {
        _state.update { current ->
            val draft = transform(current.draft, text.ifBlank { null })
            setText(current, text).copy(
                draft = draft,
                issues = DraftValidator.validate(draft),
                canComplete = DraftCompleter.missingField(draft) != null,
                saveError = null,
            )
        }
    }

    companion object {
        fun factory(
            repository: TransactionRepository,
            transactionId: String?,
            initialDraft: TransactionDraft?,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TransactionFormViewModel(repository, transactionId, initialDraft) as T
        }
    }
}
