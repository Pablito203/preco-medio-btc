package com.pablo.btcmedio.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: SettingsState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informações") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(
                titulo = "Reconhecimento de voz offline",
                estado = if (state.speechAvailable) "Serviço presente" else "Indisponível",
                detalhe = if (state.speechAvailable) {
                    "O aparelho tem o serviço de reconhecimento on-device. Isso não garante que " +
                        "o idioma português esteja baixado — se o ditado falhar, baixe o pacote em " +
                        "Ajustes do Android > Sistema > Idiomas e entrada > Reconhecimento de voz."
                } else {
                    "Baixe o pacote de idioma português em Ajustes do Android > Sistema > " +
                        "Idiomas e entrada > Reconhecimento de voz. Sem ele, use o formulário manual."
                },
            )

            StatusCard(
                titulo = "Gemini Nano",
                estado = when (state.nano) {
                    NanoStatus.VERIFICANDO -> "Verificando…"
                    NanoStatus.PRONTO -> "Pronto"
                    NanoStatus.PRECISA_BAIXAR -> "Modelo não baixado"
                    NanoStatus.BAIXANDO -> "Baixando"
                    NanoStatus.NAO_SUPORTADO -> "Não suportado"
                },
                detalhe = when (state.nano) {
                    NanoStatus.VERIFICANDO -> "Consultando o modelo no aparelho."
                    NanoStatus.PRONTO ->
                        "Usado como reforço quando a leitura por regras não completa a transação."

                    NanoStatus.PRECISA_BAIXAR ->
                        "Este aparelho tem suporte, mas o modelo ainda não foi baixado. O app não " +
                            "dispara esse download por conta própria, porque seria atividade de rede " +
                            "que você não pediu. Sem ele, o ditado por voz fica indisponível."

                    NanoStatus.BAIXANDO -> "O sistema está baixando o modelo. Nada a fazer."
                    NanoStatus.NAO_SUPORTADO ->
                        "Este aparelho não tem o hardware necessário, então o ditado por voz fica " +
                            "indisponível. A leitura de comprovantes por OCR e regras não depende " +
                            "dele e continua funcionando normalmente."
                },
            )

            StatusCard(
                titulo = "Ditado por voz",
                estado = if (state.voiceEnabled) "Disponível" else "Indisponível",
                detalhe = if (state.voiceEnabled) {
                    "O microfone na tela inicial está ativo."
                } else {
                    "O microfone na tela inicial fica apagado. O ditado exige as duas peças: o " +
                        "reconhecimento de voz para transcrever e o Gemini Nano para interpretar. " +
                        "As regras de leitura foram feitas para a forma de um comprovante, com um " +
                        "valor rotulado por linha; linguagem falada não tem rótulo nenhum, e sem o " +
                        "modelo o resultado sai errado em vez de sair vazio."
                },
            )

            StatusCard(
                titulo = "Acesso à rede",
                estado = "Nenhum",
                detalhe = "O app não declara permissão de internet. Nada do que você registra sai " +
                    "deste aparelho, e nenhuma cotação é consultada online.",
            )

            Text(
                "Os dados ficam apenas neste aparelho. Desinstalar o app apaga o histórico.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun StatusCard(titulo: String, estado: String, detalhe: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(estado, style = MaterialTheme.typography.titleSmall)
            Text(detalhe, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
