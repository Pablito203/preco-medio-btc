package com.pablo.btcmedio.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    @ColumnInfo(index = true) val occurredAt: Long,
    val fiatAmountCents: Long,
    /** `null` = taxa desconhecida; `0` = sem taxa. */
    val feeCents: Long?,
    val satoshis: Long,
    val unitPriceCents: Long,
    val source: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    occurredAt = occurredAt,
    fiatAmountCents = fiatAmountCents,
    feeCents = feeCents,
    satoshis = satoshis,
    unitPriceCents = unitPriceCents,
    source = EntrySource.valueOf(source),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    type = type.name,
    occurredAt = occurredAt,
    fiatAmountCents = fiatAmountCents,
    feeCents = feeCents,
    satoshis = satoshis,
    unitPriceCents = unitPriceCents,
    source = source.name,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
