package com.pablo.btcmedio.core

import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Instante em UTC a partir de "dd/MM/yyyy". */
fun dia(d: Int, m: Int, y: Int): Long =
    LocalDate.of(y, m, d).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

fun instante(d: Int, m: Int, y: Int, h: Int, min: Int): Long =
    LocalDateTime.of(y, m, d, h, min).toInstant(ZoneOffset.UTC).toEpochMilli()

fun compra(
    at: Long,
    fiatCents: Long,
    sats: Long,
    priceCents: Long,
    feeCents: Long? = null,
    id: String = "t-$at-$sats",
) = Transaction(
    id = id,
    type = TransactionType.BUY,
    occurredAt = at,
    fiatAmountCents = fiatCents,
    feeCents = feeCents,
    satoshis = sats,
    unitPriceCents = priceCents,
    source = EntrySource.MANUAL,
    note = null,
    createdAt = at,
    updatedAt = at,
)

fun venda(
    at: Long,
    fiatCents: Long,
    sats: Long,
    priceCents: Long,
    feeCents: Long? = null,
    id: String = "v-$at-$sats",
) = compra(at, fiatCents, sats, priceCents, feeCents, id).copy(type = TransactionType.SELL)

/** As 9 compras da planilha `compra com kyc.xlsx`. */
fun planilhaCompleta(): List<Transaction> = listOf(
    compra(dia(5, 2, 2026), 400_000, 1_174_383, 33_719_831, id = "p1"),
    compra(dia(5, 2, 2026), 150_000, 438_067, 33_727_666, id = "p2"),
    compra(dia(8, 2, 2026), 120_000, 319_564, 36_987_875, id = "p3"),
    compra(dia(23, 2, 2026), 120_000, 346_617, 34_100_949, id = "p4"),
    compra(dia(28, 2, 2026), 100_000, 286_264, 34_408_716, id = "p5"),
    compra(dia(29, 3, 2026), 120_000, 333_755, 35_415_176, id = "p6"),
    compra(dia(2, 6, 2026), 150_000, 425_219, 34_746_796, id = "p7"),
    compra(dia(5, 6, 2026), 150_000, 468_493, 31_537_282, id = "p8"),
    compra(instante(30, 6, 2026, 22, 57), 150_000, 468_094, 31_564_152, feeCents = 2_250, id = "p9"),
)
