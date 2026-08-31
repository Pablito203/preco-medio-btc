package com.pablo.btcmedio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * O app tem **um** tema, escuro, com a paleta de [Brand].
 *
 * Não há variante clara nem cor dinâmica do sistema: o âmbar sobre o ink é a
 * identidade do produto, e deixar o Material repintar tudo com o papel de
 * parede do usuário devolveria uma tela genérica. O ícone tem uma versão
 * clara, mas essa é uma exigência do launcher monocromático, não da interface.
 */
private val AppColorScheme = darkColorScheme(
    primary = Brand.Amber,
    onPrimary = Brand.Ink,
    primaryContainer = Brand.Amber,
    onPrimaryContainer = Brand.Ink,

    secondary = Brand.Amber,
    onSecondary = Brand.Ink,
    secondaryContainer = Brand.Raised,
    onSecondaryContainer = Brand.Bone,

    tertiary = Brand.Amber,
    onTertiary = Brand.Ink,

    background = Brand.Ink,
    onBackground = Brand.Bone,

    surface = Brand.Ink,
    onSurface = Brand.Bone,
    surfaceVariant = Brand.InkRaised,
    onSurfaceVariant = Brand.BoneStrong,

    // Diálogos, menus e a folha inferior saem do mesmo tom elevado.
    surfaceContainerLowest = Brand.Ink,
    surfaceContainerLow = Brand.InkSunken,
    surfaceContainer = Brand.InkRaised,
    surfaceContainerHigh = Brand.InkRaised,
    surfaceContainerHighest = Brand.InkRaised,

    outline = Brand.Bone.copy(alpha = 0.28f),
    outlineVariant = Brand.Divider,

    error = Color(0xFFE5796B),
    onError = Brand.Ink,
    errorContainer = Color(0xFF43201A),
    onErrorContainer = Color(0xFFF3B4A6),

    // O snackbar do Material inverte a superfície: fica claro sobre a tela
    // escura, o mesmo contraste da aba ativa.
    inverseSurface = Brand.Bone,
    inverseOnSurface = Brand.Ink,
    inversePrimary = Color(0xFF8A4E12),

    scrim = Brand.Scrim,
)

@Composable
fun BtcMedioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content,
    )
}
