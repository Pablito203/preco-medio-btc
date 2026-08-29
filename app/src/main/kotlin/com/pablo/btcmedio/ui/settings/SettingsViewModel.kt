package com.pablo.btcmedio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import com.pablo.btcmedio.ingest.NanoExtractor
import com.pablo.btcmedio.ingest.OnDeviceSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NanoStatus {
    VERIFICANDO,
    PRONTO,
    PRECISA_BAIXAR,
    BAIXANDO,
    NAO_SUPORTADO,
}

data class SettingsState(
    val nano: NanoStatus = NanoStatus.VERIFICANDO,
    val speechAvailable: Boolean = false,
)

class SettingsViewModel(
    speech: OnDeviceSpeech,
    private val nano: NanoExtractor,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(speechAvailable = speech.isAvailable()))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val status = when (nano.status()) {
                FeatureStatus.AVAILABLE -> NanoStatus.PRONTO
                FeatureStatus.DOWNLOADABLE -> NanoStatus.PRECISA_BAIXAR
                FeatureStatus.DOWNLOADING -> NanoStatus.BAIXANDO
                else -> NanoStatus.NAO_SUPORTADO
            }
            _state.update { it.copy(nano = status) }
        }
    }

    companion object {
        fun factory(speech: OnDeviceSpeech, nano: NanoExtractor) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(speech, nano) as T
        }
    }
}
