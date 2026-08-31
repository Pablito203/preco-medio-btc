package com.pablo.btcmedio.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pablo.btcmedio.R

/**
 * A marca do app: a barra da média sobre o ₿, no mesmo desenho do ícone do
 * launcher.
 *
 * Tudo é proporcional ao [size] para que a marca continue igual a si mesma nos
 * 32dp da barra superior e em qualquer outro tamanho. O ₿ vem do vetor, e não
 * de um `Text("₿")`: a métrica de um glifo tipográfico traz entrelinha e
 * sidebearing próprios, que desalinhariam a barra de um jeito diferente a cada
 * tamanho.
 */
@Composable
fun AppMark(
    size: Dp,
    modifier: Modifier = Modifier,
    background: Color = Brand.Bone,
    glyphColor: Color = Brand.Ink,
) {
    val espessuraDaBarra = (size * 0.094f).coerceAtLeast(2.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(size * 0.094f),
        ) {
            Box(
                Modifier
                    .width(size * 0.44f)
                    .height(espessuraDaBarra)
                    .clip(CircleShape)
                    .background(Brand.Amber)
            )
            Image(
                painter = painterResource(R.drawable.ic_brand_glyph),
                contentDescription = null,
                modifier = Modifier.height(size * 0.36f).aspectRatio(39f / 64f),
                colorFilter = ColorFilter.tint(glyphColor),
            )
        }
    }
}
