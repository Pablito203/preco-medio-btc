package com.pablo.btcmedio.ingest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablo.btcmedio.BtcMedioApp
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.ui.form.FormActions
import com.pablo.btcmedio.ui.form.TransactionFormScreen
import com.pablo.btcmedio.ui.form.TransactionFormViewModel
import com.pablo.btcmedio.ui.theme.BtcMedioTheme

/**
 * Recebe conteúdo compartilhado por outros apps e o transforma numa fila de
 * confirmações. Vive em Activity própria para não interferir na navegação do
 * app principal — o usuário confirma e volta direto para onde estava.
 */
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BtcMedioApp).container

        setContent {
            BtcMedioTheme {
                val shareVm: ShareViewModel = viewModel(
                    factory = ShareViewModel.factory(container.extractorChain, container.ocrTextReader)
                )

                LaunchedEffect(Unit) { dispatch(shareVm) }

                val current by shareVm.current.collectAsStateWithLifecycle()
                val loading by shareVm.loading.collectAsStateWithLifecycle()

                if (loading) {
                    Loading()
                } else {
                    ConfirmationForm(
                        shareVm = shareVm,
                        draftKey = current.hashCode().toString(),
                        onFinished = { finish() },
                    )
                }
            }
        }
    }

    @Composable
    private fun ConfirmationForm(
        shareVm: ShareViewModel,
        draftKey: String,
        onFinished: () -> Unit,
    ) {
        val container = (application as BtcMedioApp).container
        val current by shareVm.current.collectAsStateWithLifecycle()
        val draft = shareVm.draftFor(current)

        val formVm: TransactionFormViewModel = viewModel(
            key = draftKey,
            factory = TransactionFormViewModel.factory(
                repository = container.repository,
                transactionId = null,
                initialDraft = draft,
            ),
        )
        val state by formVm.state.collectAsStateWithLifecycle()

        LaunchedEffect(state.saved) {
            if (state.saved && !shareVm.advance()) onFinished()
        }

        TransactionFormScreen(
            state = state,
            actions = FormActions(
                onTypeChange = formVm::onTypeChange,
                onDateChange = formVm::onDateChange,
                onFiatChange = formVm::onFiatChange,
                onFeeChange = formVm::onFeeChange,
                onSatsChange = formVm::onSatsChange,
                onPriceChange = formVm::onPriceChange,
                onNoteChange = formVm::onNoteChange,
                onCompleteMissing = formVm::completeMissing,
                onSave = formVm::save,
            ),
            onDone = { if (!shareVm.advance()) onFinished() },
        )
    }

    private fun dispatch(shareVm: ShareViewModel) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.parcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                when {
                    uri != null -> shareVm.loadImages(listOf(uri))
                    text != null -> shareVm.loadText(text, EntrySource.TEXT)
                    else -> shareVm.loadText("")
                }
            }

            Intent.ACTION_SEND_MULTIPLE ->
                shareVm.loadImages(intent.parcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty())

            Intent.ACTION_PROCESS_TEXT ->
                shareVm.loadText(
                    intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty(),
                    EntrySource.TEXT,
                )

            else -> shareVm.loadText("")
        }
    }

    private inline fun <reified T : Parcelable> Intent.parcelableExtra(name: String): T? =
        getParcelableExtra(name, T::class.java)

    private inline fun <reified T : Parcelable> Intent.parcelableArrayListExtra(
        name: String,
    ): ArrayList<T>? = getParcelableArrayListExtra(name, T::class.java)
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
