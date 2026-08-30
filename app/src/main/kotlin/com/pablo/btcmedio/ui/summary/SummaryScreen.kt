package com.pablo.btcmedio.ui.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.ui.format.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    state: SummaryState,
    onAdd: () -> Unit,
    onMic: () -> Unit,
    onPickImages: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTransaction: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preço Médio BTC") },
                actions = {
                    IconButton(onClick = onPickImages) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Importar comprovante da galeria",
                        )
                    }
                    IconButton(onClick = onMic, enabled = state.voiceEnabled) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = if (state.voiceEnabled) {
                                "Registrar por voz"
                            } else {
                                "Registro por voz indisponível: depende do Gemini Nano"
                            },
                        )
                    }
                    IconButton(onClick = onOpenList) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Todas as transações")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Info, contentDescription = "Informações")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar transação")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HighlightCard(
                    label = "Preço médio",
                    value = Formatters.brl(state.summary.averagePriceCents),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        "Saldo",
                        "${Formatters.btc(state.summary.balanceSats)} BTC",
                        Modifier.weight(1f),
                    )
                    StatCard(
                        "Custo da posição",
                        Formatters.brl(state.summary.costCents),
                        Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        "Total comprado",
                        Formatters.brl(state.summary.totalBoughtCents),
                        Modifier.weight(1f),
                    )
                    StatCard(
                        "Total vendido",
                        Formatters.brl(state.summary.totalSoldCents),
                        Modifier.weight(1f),
                    )
                }
            }
            item {
                StatCard(
                    label = "Resultado realizado",
                    value = Formatters.brl(state.summary.realizedPnlCents),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.recent.isNotEmpty()) {
                item {
                    Text(
                        "Últimas transações",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(state.recent, key = { it.id }) { t ->
                    TransactionRow(t, onClick = { onOpenTransaction(t.id) })
                    HorizontalDivider()
                }
            } else {
                item {
                    Text(
                        "Nenhuma transação ainda. Toque em + para registrar a primeira, " +
                            "no ícone de imagem para importar um comprovante da galeria, " +
                            "ou compartilhe um comprovante da corretora com este app.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label: $value" }) {
        Column(Modifier.padding(20.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.semantics { contentDescription = "$label: $value" }) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                if (transaction.type == TransactionType.BUY) "Compra" else "Venda",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                Formatters.dateTime(transaction.occurredAt),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Formatters.brl(transaction.fiatAmountCents),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${Formatters.btc(transaction.satoshis)} BTC",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
