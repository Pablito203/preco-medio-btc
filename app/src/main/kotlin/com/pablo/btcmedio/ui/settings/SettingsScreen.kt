package com.pablo.btcmedio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pablo.btcmedio.ui.theme.AppText
import com.pablo.btcmedio.ui.theme.Brand

/**
 * Tela informativa: diz o que o aparelho consegue fazer e o que não consegue.
 * Não há nada para configurar aqui — daí não se chamar "Ajustes".
 */
@Composable
fun SettingsScreen(state: SettingsState, onBack: () -> Unit) {
    Scaffold(containerColor = Brand.Ink) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 10.dp, end = 22.dp, top = 12.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Brand.BoneStrong,
                    )
                }
                Spacer(Modifier.width(2.dp))
                Text("Informações", style = AppText.Title, color = Brand.Bone)
            }

            Column(
                Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusCard(
                    titulo = "Reconhecimento de voz offline",
                    estado = if (state.speechAvailable) "Serviço presente" else "Indisponível",
                    favoravel = state.speechAvailable,
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
                    favoravel = state.nano == NanoStatus.PRONTO,
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
                    favoravel = state.voiceEnabled,
                    detalhe = if (state.voiceEnabled) {
                        "A opção \"Ditar por voz\" está ativa em Registrar transação."
                    } else {
                        "A opção \"Ditar por voz\" fica apagada em Registrar transação. O ditado exige " +
                            "as duas peças: o reconhecimento de voz para transcrever e o Gemini Nano " +
                            "para interpretar. As regras de leitura foram feitas para a forma de um " +
                            "comprovante, com um valor rotulado por linha; linguagem falada não tem " +
                            "rótulo nenhum, e sem o modelo o resultado sai errado em vez de sair vazio."
                    },
                )

                StatusCard(
                    titulo = "Acesso à rede",
                    estado = "Nenhum",
                    favoravel = true,
                    detalhe = "O app não declara permissão de internet. Nada do que você registra sai " +
                        "deste aparelho, e nenhuma cotação é consultada online.",
                )
            }

            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 32.dp)) {
                Text(
                    "Os dados ficam apenas neste aparelho. Desinstalar o app apaga o histórico.",
                    style = AppText.OptionBody,
                    color = Brand.BoneMuted,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tipografia: Space Grotesk e IBM Plex Mono, sob a SIL Open Font License 1.1. " +
                        "O texto das licenças acompanha o app.",
                    style = AppText.OptionBody,
                    color = Brand.BoneLabel,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(titulo: String, estado: String, favoravel: Boolean, detalhe: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brand.Raised)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$titulo: $estado. $detalhe" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(titulo, style = AppText.OptionTitle, color = Brand.Bone, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            StatusChip(estado, favoravel)
        }
        Text(detalhe, style = AppText.Body, color = Brand.BoneStrong)
    }
}

/**
 * O âmbar aqui não quer dizer "ligado", e sim "está como deveria" — por isso
 * "Acesso à rede: Nenhum" também é âmbar: nenhum acesso é a promessa do app.
 */
@Composable
private fun StatusChip(estado: String, favoravel: Boolean) {
    val cor = if (favoravel) Brand.Amber else Brand.BoneFaint
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (favoravel) Brand.Amber.copy(alpha = 0.14f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(estado.uppercase(), style = AppText.SectionLabel, color = cor)
    }
}
