package com.pablo.btcmedio

import android.content.Context
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.extract.ReceiptRuleExtractor
import com.pablo.btcmedio.data.AppDatabase
import com.pablo.btcmedio.data.TransactionRepository
import com.pablo.btcmedio.ingest.OcrTextReader
import com.pablo.btcmedio.ingest.OnDeviceSpeech

/**
 * Injeção de dependência manual. Com o tamanho deste app, um container
 * explícito é mais legível e mais rápido de compilar que um framework.
 */
class AppContainer(private val context: Context) {

    private val database by lazy { AppDatabase.build(context) }

    val repository by lazy { TransactionRepository(database.transactionDao()) }

    val ocrTextReader by lazy { OcrTextReader(context) }

    val onDeviceSpeech by lazy { OnDeviceSpeech(context) }

    /**
     * Rascunho aguardando confirmação, vindo da voz.
     *
     * Rascunho não cabe em argumento de rota (são sete campos, alguns nulos),
     * então trafega por aqui e é consumido uma única vez pela tela do formulário.
     */
    private var pendingDraft: TransactionDraft? = null

    fun stagePendingDraft(draft: TransactionDraft) {
        pendingDraft = draft
    }

    fun consumePendingDraft(): TransactionDraft? = pendingDraft.also { pendingDraft = null }

    /**
     * As regras vêm primeiro: são determinísticas e universais.
     * O Gemini Nano é acrescentado na tarefa 15 e só recebe o que sobrar.
     */
    val extractorChain by lazy { ExtractorChain(listOf(ReceiptRuleExtractor())) }
}
