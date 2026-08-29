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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() = db.close()

    private fun tx(id: String, at: Long, sats: Long) = Transaction(
        id = id,
        type = TransactionType.BUY,
        occurredAt = at,
        fiatAmountCents = 150_000,
        feeCents = 2_250,
        satoshis = sats,
        unitPriceCents = 31_564_152,
        source = EntrySource.MANUAL,
        note = "nota",
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun grava_e_le_preservando_todos_os_campos() = runTest {
        val original = tx("a", 1_000L, 468_094)
        dao.upsert(original.toEntity())
        assertEquals(original, dao.getById("a")?.toDomain())
    }

    @Test
    fun taxa_nula_sobrevive_ao_round_trip() = runTest {
        val semTaxa = tx("a", 1_000L, 468_094).copy(feeCents = null)
        dao.upsert(semTaxa.toEntity())
        assertNull(dao.getById("a")?.toDomain()?.feeCents)
    }

    @Test
    fun upsert_com_mesmo_id_substitui() = runTest {
        dao.upsert(tx("a", 1_000L, 468_094).toEntity())
        dao.upsert(tx("a", 2_000L, 999_999).toEntity())
        assertEquals(1, dao.getAll().size)
        assertEquals(999_999L, dao.getById("a")?.satoshis)
    }

    @Test
    fun observeAll_devolve_em_ordem_decrescente_de_data() = runTest {
        dao.upsert(tx("a", 1_000L, 1).toEntity())
        dao.upsert(tx("b", 3_000L, 2).toEntity())
        dao.upsert(tx("c", 2_000L, 3).toEntity())
        assertEquals(listOf("b", "c", "a"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun deleteById_remove() = runTest {
        dao.upsert(tx("a", 1_000L, 1).toEntity())
        dao.deleteById("a")
        assertNull(dao.getById("a"))
    }
}
