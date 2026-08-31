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
 * O ₿ vem de `ic_brand_glyph`, recortado do próprio arquivo do ícone e
 * guardado como máscara branca — o launcher o pinta de osso sobre ink, e aqui
 * ele é tingido de ink sobre osso. É o mesmo traçado nos dois lugares, então
 * a marca da barra superior não pode divergir do ícone.
 *
 * As proporções são frações do [size], medidas no ícone exportado, para que a
 * marca continue igual a si mesma em qualquer tamanho.
 */
@Composable
fun AppMark(
    size: Dp,
    modifier: Modifier = Modifier,
    background: Color = Brand.Bone,
    glyphColor: Color = Brand.Ink,
) {
    // Abaixo de ~30dp a barra cairia para menos de 2dp e sumiria na renderização.
    val espessuraDaBarra = (size * 0.065f).coerceAtLeast(2.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(size * 0.052f),
        ) {
            Box(
                Modifier
                    .width(size * 0.425f)
                    .height(espessuraDaBarra)
                    .clip(CircleShape)
                    .background(Brand.Amber)
            )
            Image(
                painter = painterResource(R.drawable.ic_brand_glyph),
                contentDescription = null,
                modifier = Modifier.height(size * 0.455f).aspectRatio(GLYPH_ASPECT),
                colorFilter = ColorFilter.tint(glyphColor),
            )
        }
    }
}

/** Largura sobre altura da caixa de tinta do ₿, medida no arquivo do ícone. */
private const val GLYPH_ASPECT = 0.6565f
