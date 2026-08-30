package com.pablo.btcmedio.ui

object Routes {
    const val SUMMARY = "resumo"
    const val LIST = "transacoes"
    const val SETTINGS = "informacoes"
    const val ARG_ID = "id"
    const val FORM_PATTERN = "form?$ARG_ID={$ARG_ID}"

    fun form(id: String? = null): String = if (id == null) "form" else "form?$ARG_ID=$id"
}
