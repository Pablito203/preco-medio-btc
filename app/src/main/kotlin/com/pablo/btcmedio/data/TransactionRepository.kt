package com.pablo.btcmedio.data

import com.pablo.btcmedio.core.calc.PortfolioCalculator
import com.pablo.btcmedio.core.calc.SequenceCheck
import com.pablo.btcmedio.core.calc.SequenceValidator
import com.pablo.btcmedio.core.model.PortfolioSummary
import com.pablo.btcmedio.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface SaveResult {
    data object Success : SaveResult
    data class Oversold(val availableSats: Long, val requestedSats: Long) : SaveResult
}

/**
 * Única porta de escrita. Valida a sequência **resultante** antes de gravar,
 * o que impede que editar uma compra antiga deixe uma venda posterior sem saldo.
 */
class TransactionRepository(private val dao: TransactionDao) {

    val transactions: Flow<List<Transaction>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    val summary: Flow<PortfolioSummary> =
        transactions.map { PortfolioCalculator.summarize(it) }

    suspend fun byId(id: String): Transaction? = dao.getById(id)?.toDomain()

    suspend fun save(transaction: Transaction): SaveResult {
        val projected = dao.getAll()
            .map { it.toDomain() }
            .filterNot { it.id == transaction.id } + transaction

        return when (val check = SequenceValidator.check(projected)) {
            is SequenceCheck.Ok -> {
                dao.upsert(transaction.toEntity())
                SaveResult.Success
            }

            is SequenceCheck.Oversold ->
                SaveResult.Oversold(check.availableSats, check.requestedSats)
        }
    }

    suspend fun delete(id: String) = dao.deleteById(id)
}
