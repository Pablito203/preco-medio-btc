package com.pablo.btcmedio.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pablo.btcmedio.BtcMedioApp
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.ui.form.FormActions
import com.pablo.btcmedio.ui.form.TransactionFormScreen
import com.pablo.btcmedio.ui.form.TransactionFormViewModel
import com.pablo.btcmedio.ui.home.HomeScreen
import com.pablo.btcmedio.ui.home.HomeViewModel
import com.pablo.btcmedio.ui.imports.ImageImportViewModel
import com.pablo.btcmedio.ui.settings.SettingsScreen
import com.pablo.btcmedio.ui.settings.SettingsViewModel
import com.pablo.btcmedio.ui.theme.BtcMedioTheme
import com.pablo.btcmedio.ui.voice.VoiceCapture

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // O app é sempre escuro, então as barras do sistema também são: o
        // padrão adaptativo colocaria ícones escuros sobre o fundo ink.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = (application as BtcMedioApp).container

        setContent {
            BtcMedioTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        val vm: HomeViewModel = viewModel(
                            factory = HomeViewModel.factory(
                                container.repository,
                                container.onDeviceSpeech,
                                container.nanoExtractor,
                            )
                        )
                        val state by vm.state.collectAsStateWithLifecycle()
                        var ouvindo by remember { mutableStateOf(false) }

                        val importVm: ImageImportViewModel = viewModel(
                            factory = ImageImportViewModel.factory(
                                container.ocrTextReader,
                                container.extractorChain,
                            )
                        )
                        val importState by importVm.state.collectAsStateWithLifecycle()

                        val seletorDeFotos = rememberLauncherForActivityResult(
                            ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGENS)
                        ) { uris -> importVm.import(uris) }

                        LaunchedEffect(importState.drafts) {
                            importState.drafts?.let { drafts ->
                                container.stagePendingDrafts(drafts)
                                importVm.clear()
                                navController.navigate(Routes.form())
                            }
                        }

                        if (importState.loading) {
                            ImportProgressDialog()
                        }

                        importState.error?.let { mensagem ->
                            ImportErrorDialog(
                                mensagem = mensagem,
                                onManual = {
                                    importVm.clear()
                                    navController.navigate(Routes.form())
                                },
                                onDismiss = { importVm.clear() },
                            )
                        }

                        if (ouvindo) {
                            VoiceCapture(
                                speech = container.onDeviceSpeech,
                                chain = container.extractorChain,
                                onDraft = { draft ->
                                    container.stagePendingDrafts(listOf(draft))
                                    ouvindo = false
                                    navController.navigate(Routes.form())
                                },
                                onManualEntry = {
                                    ouvindo = false
                                    navController.navigate(Routes.form())
                                },
                                onDismiss = { ouvindo = false },
                            )
                        }

                        HomeScreen(
                            state = state,
                            onOpenInfo = { navController.navigate(Routes.INFO) },
                            onOpenTransaction = { id -> navController.navigate(Routes.form(id)) },
                            onReadReceipt = {
                                seletorDeFotos.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onTypeValues = { navController.navigate(Routes.form()) },
                            onDictate = { ouvindo = true },
                            onDelete = { vm.delete(it) },
                            onUndoDelete = { vm.undoDelete() },
                        )
                    }

                    composable(Routes.INFO) {
                        val vm: SettingsViewModel = viewModel(
                            factory = SettingsViewModel.factory(
                                container.onDeviceSpeech,
                                container.nanoExtractor,
                            )
                        )
                        val state by vm.state.collectAsStateWithLifecycle()
                        SettingsScreen(state = state, onBack = { navController.popBackStack() })
                    }

                    composable(
                        route = Routes.FORM_PATTERN,
                        arguments = listOf(
                            navArgument(Routes.ARG_ID) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        ),
                    ) { entry ->
                        val id = entry.arguments?.getString(Routes.ARG_ID)
                        val vm: TransactionFormViewModel = viewModel(
                            factory = TransactionFormViewModel.factory(
                                repository = container.repository,
                                transactionId = id,
                                initialDraft = if (id == null) {
                                    container.consumePendingDraft()
                                        ?: TransactionDraft(occurredAt = System.currentTimeMillis())
                                } else {
                                    null
                                },
                            )
                        )
                        val state by vm.state.collectAsStateWithLifecycle()

                        // Com várias imagens importadas de uma vez, salvar uma
                        // confirmação abre a próxima da fila em vez de voltar.
                        LaunchedEffect(state.saved) {
                            if (!state.saved) return@LaunchedEffect
                            if (container.hasPendingDrafts()) {
                                navController.navigate(Routes.form()) {
                                    popUpTo(Routes.FORM_PATTERN) { inclusive = true }
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }

                        TransactionFormScreen(
                            state = state,
                            actions = FormActions(
                                onTypeChange = vm::onTypeChange,
                                onDateChange = vm::onDateChange,
                                onFiatChange = vm::onFiatChange,
                                onFeeChange = vm::onFeeChange,
                                onSatsChange = vm::onSatsChange,
                                onPriceChange = vm::onPriceChange,
                                onNoteChange = vm::onNoteChange,
                                onCompleteMissing = vm::completeMissing,
                                onSave = vm::save,
                                onDelete = vm::delete,
                            ),
                            onDone = {
                                // Cancelar descarta a fila inteira: seguir para a
                                // próxima imagem depois de um cancelamento seria
                                // ignorar o que o usuário acabou de pedir.
                                container.stagePendingDrafts(emptyList())
                                navController.popBackStack()
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Teto do seletor de fotos: importar mais que isso de uma vez vira uma fila cansativa. */
private const val MAX_IMAGENS = 10

@Composable
private fun ImportProgressDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Lendo os comprovantes…") },
        text = { Text("Reconhecendo o texto das imagens neste aparelho.") },
        confirmButton = {},
    )
}

@Composable
private fun ImportErrorDialog(
    mensagem: String,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Não foi possível importar") },
        text = { Text(mensagem) },
        confirmButton = { TextButton(onClick = onManual) { Text("Preencher à mão") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}
