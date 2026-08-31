package com.pablo.btcmedio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta fixa do app.
 *
 * Os tons claros não são cores próprias: são o [Bone] com alfa sobre o [Ink].
 * Escrever assim mantém a hierarquia visível no código — `BoneMuted` é
 * literalmente "meio apagado" — e garante que qualquer superfície empilhada
 * continue coerente sem uma tabela de cinzas paralela.
 */
object Brand {

    /** Fundo da tela. Também é o `windowBackground`, em `values/colors.xml`. */
    val Ink = Color(0xFF171512)

    /** Topo do cartão em destaque e fundo da folha inferior. */
    val InkRaised = Color(0xFF231F1A)

    /** Base do gradiente do cartão em destaque. */
    val InkSunken = Color(0xFF1C1916)

    val Amber = Color(0xFFE2873A)
    val AmberPressed = Color(0xFFEFA25C)

    val Bone = Color(0xFFFAF7F2)

    val BoneStrong = Bone.copy(alpha = 0.65f)
    val BoneMuted = Bone.copy(alpha = 0.50f)
    val BoneFaint = Bone.copy(alpha = 0.45f)
    val BoneLabel = Bone.copy(alpha = 0.40f)

    /** Traço de 1dp entre linhas de lista. */
    val Divider = Bone.copy(alpha = 0.09f)

    /** Superfície discreta: trilho das abas e cartões de opção da folha. */
    val Raised = Bone.copy(alpha = 0.07f)
    val RaisedPressed = Bone.copy(alpha = 0.12f)

    /** Borda do cartão de preço médio: âmbar quase apagado. */
    val AmberEdge = Amber.copy(alpha = 0.22f)

    /** O véu atrás da folha inferior: 62% — a tela continua legível por baixo. */
    val Scrim = Color(0x9E0A0908)
}
