package com.pablo.btcmedio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pablo.btcmedio.ui.theme.AppText
import com.pablo.btcmedio.ui.theme.Brand

/**
 * A folha que abre no "Registrar transação".
 *
 * As três entradas do app viram três opções rotuladas, em vez dos ícones
 * mudos que ficavam na barra superior: "ler comprovante" e "ditar" não são
 * gestos evidentes, e o subtítulo de cada uma diz de antemão que nada é salvo
 * sem conferência — que é a dúvida que faz alguém não tocar no botão.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSheet(
    voiceEnabled: Boolean,
    onReadReceipt: () -> Unit,
    onTypeValues: () -> Unit,
    onDictate: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Brand.InkRaised,
        contentColor = Brand.Bone,
        scrimColor = Brand.Scrim,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { Grabber() },
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            Text("Como registrar?", style = AppText.SheetTitle, color = Brand.Bone)

            Spacer(Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Option(
                    icon = Icons.Default.PhotoLibrary,
                    iconBackground = Brand.Amber,
                    iconTint = Brand.Ink,
                    title = "Ler comprovante",
                    body = "Você confere os valores antes de salvar",
                    onClick = onReadReceipt,
                )
                Option(
                    icon = Icons.Default.Keyboard,
                    iconBackground = Brand.Bone.copy(alpha = 0.15f),
                    iconTint = Brand.Bone,
                    title = "Digitar valores",
                    body = "Reais e quantidade — a cotação é calculada",
                    onClick = onTypeValues,
                )
                Option(
                    icon = Icons.Default.Mic,
                    iconBackground = Brand.Bone.copy(alpha = 0.10f),
                    iconTint = Brand.Bone,
                    title = "Ditar por voz",
                    body = if (voiceEnabled) {
                        "Fale os valores e confira antes de salvar"
                    } else {
                        "Indisponível neste aparelho"
                    },
                    enabled = voiceEnabled,
                    onClick = onDictate,
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("Fechar", style = AppText.Tab, color = Brand.BoneStrong)
            }
        }
    }
}

@Composable
private fun Grabber() {
    Box(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp), Alignment.Center) {
        Box(
            Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Brand.Bone.copy(alpha = 0.2f))
        )
    }
}

/**
 * `enabled = false` deixa a opção visível e apagada, em vez de sumir com ela.
 * Uma opção ausente parece um app incompleto; uma opção apagada com o motivo
 * escrito embaixo explica o aparelho.
 */
@Composable
private fun Option(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    body: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // A opacidade vem antes do fundo para valer também sobre ele.
            .alpha(if (enabled) 1f else 0.55f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Brand.Raised else Brand.Bone.copy(alpha = 0.03f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = AppText.OptionTitle, color = Brand.Bone)
            Text(body, style = AppText.OptionBody, color = Brand.BoneMuted)
        }
    }
}
