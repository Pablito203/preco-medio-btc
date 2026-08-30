package com.pablo.btcmedio.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.ui.summary.TransactionRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    months: List<MonthGroup>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (Transaction) -> Unit,
    onUndo: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transações") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            months.forEach { group ->
                item(key = "cabecalho-${group.label}") {
                    Text(
                        group.label,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(group.transactions, key = { it.id }) { transaction ->
                    SwipeRow(
                        transaction = transaction,
                        onOpen = { onOpen(transaction.id) },
                        onDelete = {
                            onDelete(transaction)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Transação excluída",
                                    actionLabel = "Desfazer",
                                )
                                if (result == SnackbarResult.ActionPerformed) onUndo()
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
            if (months.isEmpty()) {
                item {
                    Text(
                        "Nenhuma transação registrada.",
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(transaction: Transaction, onOpen: () -> Unit, onDelete: () -> Unit) {
    val excluir by rememberUpdatedState(onDelete)

    /*
     * A exclusão é disparada aqui, e a mudança de estado é **recusada**.
     *
     * Deixar o estado assentar em "deslizado" parece natural, mas a LazyColumn
     * guarda o estado de cada item por chave e o devolve quando o item reaparece.
     * Como o desfazer restaura a transação com o mesmo id, a linha voltava já
     * deslizada e se excluía sozinha — o desfazer parecia não fazer nada.
     */
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { alvo ->
            if (alvo == SwipeToDismissBoxValue.Settled) {
                true
            } else {
                excluir()
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Excluir", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
            TransactionRow(transaction, onClick = onOpen)
        }
    }
}
