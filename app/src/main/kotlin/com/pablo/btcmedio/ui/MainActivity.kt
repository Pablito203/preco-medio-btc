package com.pablo.btcmedio.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.pablo.btcmedio.ui.summary.SummaryScreen
import com.pablo.btcmedio.ui.summary.SummaryViewModel
import com.pablo.btcmedio.ui.theme.BtcMedioTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BtcMedioApp).container

        setContent {
            BtcMedioTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Routes.SUMMARY) {
                    composable(Routes.SUMMARY) {
                        val vm: SummaryViewModel =
                            viewModel(factory = SummaryViewModel.factory(container.repository))
                        val state by vm.state.collectAsStateWithLifecycle()
                        SummaryScreen(
                            state = state,
                            onAdd = { navController.navigate(Routes.form()) },
                            onMic = { /* tarefa 14 */ },
                            onOpenList = { navController.navigate(Routes.LIST) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenTransaction = { id -> navController.navigate(Routes.form(id)) },
                        )
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
                                    TransactionDraft(occurredAt = System.currentTimeMillis())
                                } else {
                                    null
                                },
                            )
                        )
                        val state by vm.state.collectAsStateWithLifecycle()
                        LaunchedEffect(state.saved) { if (state.saved) navController.popBackStack() }

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
                            onDone = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
