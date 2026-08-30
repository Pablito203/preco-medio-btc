package com.pablo.btcmedio.ui.list

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.data.AppDatabase
import com.pablo.btcmedio.data.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Isola a camada de dados do desfazer: se estes testes passam, um "Desfazer"
 * que não funciona na tela é problema de interface, não de ViewModel.
 */
@RunWith(AndroidJUnit4::class)
class TransactionListViewModelTest {

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

    private fun compra(id: String, at: Long) = Transaction(
        id = id,
        type = TransactionType.BUY,
        occurredAt = at,
        fiatAmountCents = 150_000,
        feeCents = 2_250,
        satoshis = 468_094,
        unitPriceCents = 31_564_152,
        source = EntrySource.MANUAL,
        note = null,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun desfazer_restaura_a_transacao_excluida_com_todos_os_campos() = runTest {
        val original = compra("a", 1_000L)
        repo.save(original)

        val vm = TransactionListViewModel(repo)
        vm.delete(original)
        assertEquals(emptyList<Transaction>(), repo.transactions.first { it.isEmpty() })

        vm.undoDelete()
        val restauradas = repo.transactions.first { it.isNotEmpty() }
        assertEquals(original, restauradas.single())
    }

    @Test
    fun desfazer_duas_vezes_seguidas_nao_duplica() = runTest {
        val original = compra("a", 1_000L)
        repo.save(original)

        val vm = TransactionListViewModel(repo)
        vm.delete(original)
        repo.transactions.first { it.isEmpty() }

        vm.undoDelete()
        repo.transactions.first { it.isNotEmpty() }
        vm.undoDelete()

        assertEquals(1, repo.transactions.first().size)
    }

    @Test
    fun desfazer_sem_exclusao_previa_nao_faz_nada() = runTest {
        val vm = TransactionListViewModel(repo)
        vm.undoDelete()
        assertEquals(emptyList<Transaction>(), repo.transactions.first())
    }

    @Test
    fun desfazer_restaura_apenas_a_ultima_exclusao() = runTest {
        val primeira = compra("a", 1_000L)
        val segunda = compra("b", 2_000L)
        repo.save(primeira)
        repo.save(segunda)

        val vm = TransactionListViewModel(repo)
        vm.delete(primeira)
        repo.transactions.first { it.size == 1 }
        vm.delete(segunda)
        repo.transactions.first { it.isEmpty() }

        vm.undoDelete()
        val restauradas = repo.transactions.first { it.isNotEmpty() }
        assertEquals(segunda, restauradas.single())
    }
}
