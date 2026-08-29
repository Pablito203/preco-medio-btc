package com.pablo.btcmedio.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * O histórico só existe neste aparelho: se o banco em arquivo não sobreviver a
 * fechar e reabrir o app, o produto inteiro é inútil. Os outros testes de dados
 * usam banco em memória e não cobrem isso.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabasePersistenceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun limpar() = context.deleteDatabase("btcmedio.db").let { }

    @After
    fun apagar() = context.deleteDatabase("btcmedio.db").let { }

    private val compra = Transaction(
        id = "persistente",
        type = TransactionType.BUY,
        occurredAt = 1_782_000_000_000L,
        fiatAmountCents = 150_000,
        feeCents = 2_250,
        satoshis = 468_094,
        unitPriceCents = 31_564_152,
        source = EntrySource.IMAGE,
        note = "comprovante",
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun transacao_sobrevive_a_fechar_e_reabrir_o_banco() = runTest {
        val primeira = AppDatabase.build(context)
        primeira.transactionDao().upsert(compra.toEntity())
        primeira.close()

        val segunda = AppDatabase.build(context)
        val lida = segunda.transactionDao().getById("persistente")?.toDomain()
        segunda.close()

        assertEquals(compra, lida)
    }

    @Test
    fun preco_medio_e_recalculado_do_banco_reaberto() = runTest {
        val primeira = AppDatabase.build(context)
        primeira.transactionDao().upsert(compra.toEntity())
        primeira.close()

        val segunda = AppDatabase.build(context)
        val resumo = TransactionRepository(segunda.transactionDao()).summary.first()
        segunda.close()

        assertEquals(468_094L, resumo.balanceSats)
        assertEquals(150_000L, resumo.costCents)
        // 150.000 * 1e8 / 468.094 = 32.044.845,7 -> 32.044.846
        assertEquals(32_044_846L, resumo.averagePriceCents)
    }
}
