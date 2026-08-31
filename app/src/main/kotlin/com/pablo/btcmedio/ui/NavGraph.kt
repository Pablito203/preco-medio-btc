package com.pablo.btcmedio.ui

object Routes {
    /** Resumo e histórico são abas da mesma tela; não há rota separada. */
    const val HOME = "carteira"
    const val INFO = "informacoes"
    const val ARG_ID = "id"
    const val FORM_PATTERN = "form?$ARG_ID={$ARG_ID}"

    fun form(id: String? = null): String = if (id == null) "form" else "form?$ARG_ID=$id"
}
