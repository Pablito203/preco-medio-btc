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
     * Fila de rascunhos aguardando confirmação, vindos da voz ou da importação
     * de imagens.
     *
     * Rascunho não cabe em argumento de rota (são sete campos, alguns nulos),
     * então trafega por aqui. É uma fila porque a importação aceita várias
     * imagens de uma vez, e cada uma vira uma confirmação separada.
     */
    private val pendingDrafts = ArrayDeque<TransactionDraft>()

    fun stagePendingDrafts(drafts: List<TransactionDraft>) {
        pendingDrafts.clear()
        pendingDrafts.addAll(drafts)
    }

    fun consumePendingDraft(): TransactionDraft? = pendingDrafts.removeFirstOrNull()

    fun hasPendingDrafts(): Boolean = pendingDrafts.isNotEmpty()

    val nanoExtractor = NanoExtractor()

    /**
     * A ordem importa: as regras são determinísticas e universais e vêm
     * primeiro; o Gemini Nano só recebe o que elas não conseguiram completar.
     */
    val extractorChain by lazy { ExtractorChain(listOf(ReceiptRuleExtractor(), nanoExtractor)) }
}
