package com.pablo.btcmedio.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pablo.btcmedio.R

/**
 * A Space Grotesk vem como fonte variável: um único arquivo cobre o eixo de
 * peso de 300 a 700, cujo padrão é 300. Sem declarar o eixo, todo texto sai
 * Light — por isso cada peso precisa da sua `variationSettings`.
 */
@OptIn(ExperimentalTextApi::class)
private fun grotesk(peso: Int) = Font(
    resId = R.font.space_grotesk,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
)

val SpaceGrotesk = FontFamily(grotesk(400), grotesk(500), grotesk(600), grotesk(700))

/**
 * Números sempre em monoespaçada. Dígitos de mesma largura impedem que a
 * coluna de valores dance a cada centavo que muda, e é o que separa um saldo
 * de um rótulo à primeira batida de olho.
 */
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

/**
 * Os estilos do layout, nomeados pelo papel que cumprem na tela.
 *
 * A escala do Material3 fica abaixo, em [AppTypography], só para o que o
 * próprio Material desenha — diálogos, campos de texto, snackbar.
 */
object AppText {

    /** Título da barra superior. */
    val Title = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)

    /** Rótulo de seção em versal, espaçado: "PREÇO MÉDIO", "AGOSTO 2026". */
    val SectionLabel = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.14.em,
    )

    /** O "R$" que antecede o preço médio. */
    val HeroSymbol = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 19.sp)

    /** O preço médio. Levemente apertado: em 40sp o padrão abre demais. */
    val HeroValue = TextStyle(
        fontFamily = PlexMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        letterSpacing = (-0.02).em,
    )

    val StatLabel = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 11.sp)
    val StatValue = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 15.sp)

    val Tab = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 13.sp)

    val RowLabel = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 13.sp)
    val RowValue = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Medium, fontSize = 13.sp)

    val TxTitle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    val TxMeta = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Normal, fontSize = 11.sp)

    val Cta = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

    val SheetTitle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    val OptionTitle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    val OptionBody = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.4.sp,
    )

    /** Corpo de texto longo — a tela de informações. */
    val Body = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
}

/** A escala do Material3, redesenhada nas duas famílias do app. */
val AppTypography = Typography().run {
    fun TextStyle.grotesk() = copy(fontFamily = SpaceGrotesk)
    Typography(
        displayLarge = displayLarge.grotesk(),
        displayMedium = displayMedium.grotesk(),
        displaySmall = displaySmall.grotesk(),
        headlineLarge = headlineLarge.grotesk(),
        headlineMedium = headlineMedium.grotesk(),
        headlineSmall = headlineSmall.grotesk(),
        titleLarge = titleLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.grotesk(),
        titleSmall = titleSmall.grotesk(),
        bodyLarge = bodyLarge.grotesk(),
        bodyMedium = bodyMedium.grotesk(),
        bodySmall = bodySmall.grotesk(),
        labelLarge = labelLarge.grotesk(),
        labelMedium = labelMedium.grotesk(),
        labelSmall = labelSmall.grotesk(),
    )
}
