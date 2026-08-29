package com.pablo.btcmedio.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pablo.btcmedio.BtcMedioApp
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
                }
            }
        }
    }
}
