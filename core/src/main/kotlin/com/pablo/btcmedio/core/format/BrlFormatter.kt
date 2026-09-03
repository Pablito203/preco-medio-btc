package com.pablo.btcmedio.core.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Centavos em reais legíveis.
 *
 * Vive no `:core` porque as mensagens de validação nascem aqui e precisam
 * falar em reais — centavos são a unidade de armazenamento, não algo para
 * mostrar a alguém. A camada Android reaproveita esta função em vez de manter
 * uma segunda implementação que pode divergir.
 */
object BrlFormatter {

    private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

    /** `150000` → `R$ 1.500,00` */
    fun format(cents: Long): String =
        NumberFormat.getCurrencyInstance(PT_BR).format(BigDecimal.valueOf(cents, 2))
}
