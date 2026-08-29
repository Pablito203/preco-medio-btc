package com.pablo.btcmedio

import android.content.Context
import com.pablo.btcmedio.data.AppDatabase
import com.pablo.btcmedio.data.TransactionRepository

/**
 * Injeção de dependência manual. Com o tamanho deste app, um container
 * explícito é mais legível e mais rápido de compilar que um framework.
 */
class AppContainer(private val context: Context) {

    private val database by lazy { AppDatabase.build(context) }

    val repository by lazy { TransactionRepository(database.transactionDao()) }
}
