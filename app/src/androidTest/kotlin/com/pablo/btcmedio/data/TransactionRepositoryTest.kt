package com.pablo.btcmedio.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TransactionRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).build()
        repo = TransactionRepository(db.transactionDao())
    }

    @After
    fun tearDown() = db.close()

    private fun tx(
        id: String,
        type: TransactionType,
        at: Long,
        fiat: Long,
        sats: Long,
    ) = Transaction(
        id = id, type = type, occurredAt = at, fiatAmountCents = fiat, feeCents = null,
        satoshis = sats, unitPriceCents = 10_000_000, source = EntrySource.MANUAL,
        note = null, createdAt = 1L, updatedAt = 1L,
    )

    @Test
    fun resumo_reflete_as_transacoes_gravadas() = runTest {
        repo.save(tx("c1", TransactionType.BUY, 1_000L, 10_000_000, 100_000_000))
        val s = repo.summary.first()
        assertEquals(100_000_000L, s.balanceSats)
        assertEquals(10_000_000L, s.costCents)
        assertEquals(10_000_000L, s.averagePriceCents)
    }

    @Test
    fun venda_acima_do_saldo_e_recusada_e_nao_grava() = runTest {
        repo.save(tx("c1", TransactionType.BUY, 1_000L, 10_000_000, 100_000_000))
        val result = repo.save(tx("v1", TransactionType.SELL, 2_000L, 30_000_000, 200_000_000))
        assertTrue(result is SaveResult.Oversold)
        assertEquals(100_000_000L, (result as SaveResult.Oversold).availableSats)
        assertEquals(1, repo.transactions.first().size)
    }

    @Test
    fun editar_compra_antiga_que_invalidaria_venda_posterior_e_recusado() = runTest {
        repo.save(tx("c1", TransactionType.BUY, 1_000L, 10_000_000, 100_000_000))
        assertEquals(
            SaveResult.Success,
            repo.save(tx("v1", TransactionType.SELL, 2_000L, 7_000_000, 50_000_000)),
        )

        val reduzida = tx("c1", TransactionType.BUY, 1_000L, 1_000_000, 10_000_000)
        assertTrue(repo.save(reduzida) is SaveResult.Oversold)

        // A compra original permanece intacta.
        assertEquals(100_000_000L, repo.byId("c1")!!.satoshis)
    }

    @Test
    fun excluir_compra_que_invalidaria_venda_posterior_nao_corrompe_o_resumo() = runTest {
        repo.save(tx("c1", TransactionType.BUY, 1_000L, 10_000_000, 100_000_000))
        repo.save(tx("v1", TransactionType.SELL, 2_000L, 7_000_000, 50_000_000))
        repo.delete("c1")
        // A exclusão é permitida; o cálculo limita defensivamente e não trava.
        val s = repo.summary.first()
        assertEquals(0L, s.balanceSats)
    }
}
