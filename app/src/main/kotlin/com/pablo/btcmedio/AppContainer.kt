package com.pablo.btcmedio

import android.content.Context
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.extract.ReceiptRuleExtractor
import com.pablo.btcmedio.data.AppDatabase
import com.pablo.btcmedio.data.TransactionRepository
import com.pablo.btcmedio.ingest.NanoExtractor
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

    val nanoExtractor = NanoExtractor()

    /**
     * A ordem importa: as regras são determinísticas e universais e vêm
     * primeiro; o Gemini Nano só recebe o que elas não conseguiram completar.
     */
    val extractorChain by lazy { ExtractorChain(listOf(ReceiptRuleExtractor(), nanoExtractor)) }
}
