package com.pablo.btcmedio

import android.content.Context
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.extract.ReceiptRuleExtractor
import com.pablo.btcmedio.data.AppDatabase
import com.pablo.btcmedio.data.TransactionRepository
import com.pablo.btcmedio.ingest.OcrTextReader

/**
 * Injeção de dependência manual. Com o tamanho deste app, um container
 * explícito é mais legível e mais rápido de compilar que um framework.
 */
class AppContainer(private val context: Context) {

    private val database by lazy { AppDatabase.build(context) }

    val repository by lazy { TransactionRepository(database.transactionDao()) }

    val ocrTextReader by lazy { OcrTextReader(context) }

    /**
     * As regras vêm primeiro: são determinísticas e universais.
     * O Gemini Nano é acrescentado na tarefa 15 e só recebe o que sobrar.
     */
    val extractorChain by lazy { ExtractorChain(listOf(ReceiptRuleExtractor())) }
}
