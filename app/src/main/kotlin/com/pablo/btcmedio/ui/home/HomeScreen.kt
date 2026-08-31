package com.pablo.btcmedio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pablo.btcmedio.core.model.PortfolioSummary
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.ui.format.Formatters
import com.pablo.btcmedio.ui.theme.AppMark
import com.pablo.btcmedio.ui.theme.AppText
import com.pablo.btcmedio.ui.theme.Brand
import kotlinx.coroutines.launch

enum class HomeTab { RESUMO, TRANSACOES }

/**
 * A tela única do app: o preço médio em destaque, e abaixo dele a mesma
 * carteira vista de dois jeitos — os totais, ou o extrato.
 *
 * Resumo e histórico eram duas telas com uma navegação entre elas. Como as
 * duas respondem à mesma pergunta ("como está a carteira?"), a ida e volta só
 * cobrava pedágio: viraram abas, e a navegação sobrou.
 */
@Composable
fun HomeScreen(
    state: HomeState,
    onOpenInfo: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onReadReceipt: () -> Unit,
    onTypeValues: () -> Unit,
    onDictate: () -> Unit,
    onDelete: (Transaction) -> Unit,
    onUndoDelete: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.RESUMO) }
    var sheetAberta by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Brand.Ink,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {

            TopBar(onOpenInfo = onOpenInfo)

            HeroCard(state.summary)

            Spacer(Modifier.height(20.dp))

            Tabs(selected = tab, onSelect = { tab = it })

            Box(Modifier.weight(1f).padding(horizontal = 22.dp)) {
                when (tab) {
                    HomeTab.RESUMO -> SummaryTab(state.summary)

                    HomeTab.TRANSACOES -> HistoryTab(
                        months = state.months,
                        onOpen = onOpenTransaction,
                        onDelete = { transacao ->
                            onDelete(transacao)
                            scope.launch {
                                val resultado = snackbarHostState.showSnackbar(
                                    message = "Transação excluída",
                                    actionLabel = "Desfazer",
                                )
                                if (resultado == SnackbarResult.ActionPerformed) onUndoDelete()
                            }
                        },
                    )
                }
            }

            RegisterButton(onClick = { sheetAberta = true })
        }
    }

    if (sheetAberta) {
        RegisterSheet(
            voiceEnabled = state.voiceEnabled,
            onReadReceipt = {
                sheetAberta = false
                onReadReceipt()
            },
            onTypeValues = {
                sheetAberta = false
                onTypeValues()
            },
            onDictate = {
                sheetAberta = false
                onDictate()
            },
            onDismiss = { sheetAberta = false },
        )
    }
}

@Composable
private fun TopBar(onOpenInfo: () -> Unit) {
    var menuAberto by remember { mutableStateOf(false) }

    Row(
        // A margem direita é menor que a esquerda porque o alvo de toque de
        // 48dp do ícone é bem maior que o glifo desenhado dentro dele.
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 10.dp, top = 12.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppMark(size = 32.dp)
        Spacer(Modifier.width(11.dp))
        Text(
            "Preço Médio",
            style = AppText.Title,
            color = Brand.Bone,
            modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menuAberto = true }) {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = "Mais opções",
                    tint = Brand.BoneMuted,
                )
            }
            DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                DropdownMenuItem(
                    text = { Text("Informações", style = AppText.RowLabel) },
                    onClick = {
                        menuAberto = false
                        onOpenInfo()
                    },
                )
            }
        }
    }
}

@Composable
private fun HeroCard(summary: PortfolioSummary) {
    val forma = RoundedCornerShape(22.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(forma)
            .background(Brush.verticalGradient(listOf(Brand.InkRaised, Brand.InkSunken)))
            .border(1.dp, Brand.AmberEdge, forma)
            .padding(22.dp)
    ) {
        Text("PREÇO MÉDIO", style = AppText.SectionLabel, color = Brand.BoneFaint)

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "Preço médio: " + Formatters.brl(summary.averagePriceCents)
            }
        ) {
            Text(
                "R$",
                style = AppText.HeroSymbol,
                color = Brand.Amber,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                Formatters.decimal(summary.averagePriceCents),
                style = AppText.HeroValue,
                color = Brand.Bone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alignByBaseline(),
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = Brand.Bone.copy(alpha = 0.10f))
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Saldo", Formatters.btcAmount(summary.balanceSats), Alignment.Start)
            Stat("Custo da posição", Formatters.brl(summary.costCents), Alignment.End)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, alinhamento: Alignment.Horizontal) {
    Column(
        horizontalAlignment = alinhamento,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $value"
        },
    ) {
        Text(label, style = AppText.StatLabel, color = Brand.BoneMuted)
        Spacer(Modifier.height(7.dp))
        Text(value, style = AppText.StatValue, color = Brand.Bone)
    }
}

@Composable
private fun Tabs(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brand.Raised)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TabItem("Resumo", selected == HomeTab.RESUMO, Modifier.weight(1f)) {
            onSelect(HomeTab.RESUMO)
        }
        TabItem("Transações", selected == HomeTab.TRANSACOES, Modifier.weight(1f)) {
            onSelect(HomeTab.TRANSACOES)
        }
    }
}

@Composable
private fun TabItem(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Brand.Bone else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = AppText.Tab, color = if (selected) Brand.Ink else Brand.BoneStrong)
    }
}

@Composable
private fun SummaryTab(summary: PortfolioSummary) {
    /*
     * O terceiro campo diz se o valor representa "nada aconteceu ainda". Um
     * zero apagado sai da varredura visual e deixa em primeiro plano os
     * números que têm história — que é o motivo de a pessoa ter aberto o
     * resumo.
     */
    val linhas = listOf(
        Triple(
            "Total comprado",
            Formatters.brl(summary.totalBoughtCents),
            summary.totalBoughtCents == 0L,
        ),
        Triple(
            "Total vendido",
            Formatters.brl(summary.totalSoldCents),
            summary.totalSoldCents == 0L,
        ),
        Triple(
            "Resultado realizado",
            Formatters.brl(summary.realizedPnlCents),
            summary.realizedPnlCents == 0L,
        ),
        Triple(
            "Taxas pagas",
            Formatters.brl(summary.feesPaidCents),
            summary.feesPaidCents == 0L,
        ),
        Triple(
            "Primeiro aporte",
            summary.firstBuyAt?.let { Formatters.date(it) } ?: "—",
            summary.firstBuyAt == null,
        ),
    )

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        linhas.forEachIndexed { indice, (label, valor, vazio) ->
            StatementRow(
                label = label,
                value = valor,
                dimmed = vazio,
                divider = indice < linhas.lastIndex,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatementRow(label: String, value: String, dimmed: Boolean, divider: Boolean) {
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = AppText.RowLabel, color = Brand.BoneStrong)
            Text(value, style = AppText.RowValue, color = if (dimmed) Brand.BoneFaint else Brand.Bone)
        }
        if (divider) HorizontalDivider(color = Brand.Divider)
    }
}

@Composable
private fun HistoryTab(
    months: List<MonthGroup>,
    onOpen: (String) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    if (months.isEmpty()) {
        Text(
            "Nenhuma transação ainda. Toque em Registrar transação para lançar a primeira, " +
                "ou compartilhe um comprovante da corretora com este app.",
            style = AppText.Body,
            color = Brand.BoneStrong,
            modifier = Modifier.padding(top = 24.dp),
        )
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        months.forEachIndexed { indice, grupo ->
            item(key = "cabecalho-" + grupo.label) {
                Text(
                    grupo.label.uppercase(),
                    style = AppText.SectionLabel,
                    color = Brand.BoneLabel,
                    modifier = Modifier.padding(
                        top = if (indice == 0) 16.dp else 20.dp,
                        bottom = 4.dp,
                    ),
                )
            }
            items(grupo.transactions, key = { it.id }) { transacao ->
                SwipeToDeleteRow(
                    transaction = transacao,
                    onOpen = { onOpen(transacao.id) },
                    onDelete = { onDelete(transacao) },
                )
                HorizontalDivider(color = Brand.Divider)
            }
        }
        item(key = "rodape") { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun RegisterButton(onClick: () -> Unit) {
    Box(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brand.Amber)
                .clickable(onClick = onClick, role = Role.Button)
                .padding(17.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = Brand.Ink,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text("Registrar transação", style = AppText.Cta, color = Brand.Ink)
        }
    }
}

@Composable
internal fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    val compra = transaction.type == TransactionType.BUY

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // O fundo é explícito porque esta linha desliza por cima da faixa
            // de exclusão: sem ele, a faixa apareceria através da linha.
            .background(Brand.Ink)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // O quadradinho marca o tipo sem gastar uma palavra: âmbar cheio para
        // compra, vazado para venda.
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .then(
                    if (compra) {
                        Modifier.background(Brand.Amber)
                    } else {
                        Modifier.border(1.5.dp, Brand.Amber, RoundedCornerShape(2.dp))
                    }
                )
        )

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (compra) "Compra" else "Venda",
                style = AppText.TxTitle,
                color = Brand.Bone,
            )
            Text(
                Formatters.shortDate(transaction.occurredAt) + " · " +
                    Formatters.brlWhole(transaction.unitPriceCents),
                style = AppText.TxMeta,
                color = Brand.BoneFaint,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                Formatters.brl(transaction.fiatAmountCents),
                style = AppText.RowValue,
                color = Brand.Bone,
            )
            Text(
                Formatters.btcAmount(transaction.satoshis),
                style = AppText.TxMeta,
                color = Brand.BoneFaint,
            )
        }
    }
}
