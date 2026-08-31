package com.pablo.btcmedio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.ui.theme.AppText
import com.pablo.btcmedio.ui.theme.Brand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeToDeleteRow(
    transaction: Transaction,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
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
                    .background(Brand.Amber.copy(alpha = 0.16f))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Excluir", style = AppText.RowValue, color = Brand.Amber)
            }
        },
    ) {
        TransactionRow(transaction, onClick = onOpen)
    }
}
