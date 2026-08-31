package com.pablo.btcmedio.ui.home

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.data.AppDatabase
import com.pablo.btcmedio.data.TransactionRepository
import com.pablo.btcmedio.ingest.NanoExtractor
import com.pablo.btcmedio.ingest.OnDeviceSpeech
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
class HomeViewModelTest {

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

    /**
     * Voz e Nano entram como estão no aparelho de teste. Nenhum destes casos
     * depende deles — só do repositório.
     */
    private fun viewModel() = HomeViewModel(
        repository = repo,
        speech = OnDeviceSpeech(ApplicationProvider.getApplicationContext()),
        nano = NanoExtractor(),
    )

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

        val vm = viewModel()
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

        val vm = viewModel()
        vm.delete(original)
        repo.transactions.first { it.isEmpty() }

        vm.undoDelete()
        repo.transactions.first { it.isNotEmpty() }
        vm.undoDelete()

        assertEquals(1, repo.transactions.first().size)
    }

    @Test
    fun desfazer_sem_exclusao_previa_nao_faz_nada() = runTest {
        viewModel().undoDelete()
        assertEquals(emptyList<Transaction>(), repo.transactions.first())
    }

    @Test
    fun desfazer_restaura_apenas_a_ultima_exclusao() = runTest {
        val primeira = compra("a", 1_000L)
        val segunda = compra("b", 2_000L)
        repo.save(primeira)
        repo.save(segunda)

        val vm = viewModel()
        vm.delete(primeira)
        repo.transactions.first { it.size == 1 }
        vm.delete(segunda)
        repo.transactions.first { it.isEmpty() }

        vm.undoDelete()
        val restauradas = repo.transactions.first { it.isNotEmpty() }
        assertEquals(segunda, restauradas.single())
    }

    /** Um mês por cabeçalho, e as transações do mês debaixo dele. */
    @Test
    fun as_transacoes_sao_agrupadas_por_mes() = runTest {
        repo.save(compra("a", dia(15, 6, 2026)))
        repo.save(compra("b", dia(20, 6, 2026)))
        repo.save(compra("c", dia(3, 8, 2026)))

        val estado = viewModel().state.first { it.months.size == 2 }
        val rotulos = estado.months.map { it.label }

        assertEquals(listOf("Agosto 2026", "Junho 2026"), rotulos)
        assertEquals(1, estado.months.first { it.label == "Agosto 2026" }.transactions.size)
        assertEquals(2, estado.months.first { it.label == "Junho 2026" }.transactions.size)
    }

    private fun dia(d: Int, m: Int, y: Int): Long =
        java.time.LocalDate.of(y, m, d)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
}
