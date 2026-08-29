# Tarefas 1–6 — Núcleo `:core`

Ver `2026-08-29-app-bitcoin-preco-medio.md` para objetivo, arquitetura e **Global Constraints**.

---

## Task 1: Esqueleto Gradle + modelo de domínio + PortfolioCalculator

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `.gitignore`
- Create: `core/build.gradle.kts`
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/model/Transaction.kt`
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/model/PortfolioSummary.kt`
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/calc/PortfolioCalculator.kt`
- Test: `core/src/test/kotlin/com/pablo/btcmedio/core/calc/PortfolioCalculatorTest.kt`
- Test: `core/src/test/kotlin/com/pablo/btcmedio/core/Fixtures.kt`

**Interfaces:**
- Consumes: nada (primeira tarefa)
- Produces:
  - `enum class TransactionType { BUY, SELL }`
  - `enum class EntrySource { MANUAL, IMAGE, AUDIO, TEXT }`
  - `data class Transaction(id: String, type: TransactionType, occurredAt: Long, fiatAmountCents: Long, feeCents: Long?, satoshis: Long, unitPriceCents: Long, source: EntrySource, note: String?, createdAt: Long, updatedAt: Long)`
  - `data class PortfolioSummary(balanceSats: Long, costCents: Long, averagePriceCents: Long?, totalBoughtCents: Long, totalSoldCents: Long, realizedPnlCents: Long)`
  - `object PortfolioCalculator { const val SATS_PER_BTC: Long; fun summarize(transactions: List<Transaction>): PortfolioSummary }`
  - Fixture de teste `planilhaCompleta(): List<Transaction>`

- [ ] **Step 1: Criar os arquivos de build**

`.gitignore`:

```gitignore
.gradle/
build/
local.properties
*.iml
.idea/
.kotlin/
captures/
.externalNativeBuild/
.cxx/
```

`gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "btcmedio"
include(":core")
include(":app")
```

`gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
navigation = "2.8.5"
room = "2.6.1"
mlkitTextRecognition = "16.0.1"
mlkitGenaiPrompt = "1.0.0-beta2"
junit = "4.13.2"
coroutines = "1.9.0"
androidxJunit = "1.2.1"
espresso = "3.6.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxJunit" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
mlkit-text-recognition = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkitTextRecognition" }
mlkit-genai-prompt = { group = "com.google.mlkit", name = "genai-prompt", version.ref = "mlkitGenaiPrompt" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

`build.gradle.kts` (raiz):

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

`core/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}

tasks.withType<Test>().configureEach {
    useJUnit()
    testLogging { events("passed", "failed", "skipped") }
}
```

- [ ] **Step 2: Gerar o Gradle wrapper**

O Gradle global é 8.7; o projeto usa 8.11.1 via wrapper.

Run: `gradle wrapper --gradle-version 8.11.1 --distribution-type bin`
Expected: cria `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 3: Escrever o teste que falha**

`core/src/test/kotlin/com/pablo/btcmedio/core/Fixtures.kt`:

```kotlin
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
```

`core/src/test/kotlin/com/pablo/btcmedio/core/calc/PortfolioCalculatorTest.kt`:

```kotlin
package com.pablo.btcmedio.core.calc

import com.pablo.btcmedio.core.compra
import com.pablo.btcmedio.core.dia
import com.pablo.btcmedio.core.planilhaCompleta
import com.pablo.btcmedio.core.venda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortfolioCalculatorTest {

    @Test
    fun `carteira vazia nao tem preco medio`() {
        val s = PortfolioCalculator.summarize(emptyList())
        assertEquals(0L, s.balanceSats)
        assertEquals(0L, s.costCents)
        assertNull(s.averagePriceCents)
        assertEquals(0L, s.realizedPnlCents)
    }

    @Test
    fun `as nove compras da planilha reproduzem os totais`() {
        val s = PortfolioCalculator.summarize(planilhaCompleta())
        assertEquals(4_260_456L, s.balanceSats)
        assertEquals(1_460_000L, s.costCents)
        assertEquals(34_268_632L, s.averagePriceCents)
        assertEquals(1_460_000L, s.totalBoughtCents)
        assertEquals(0L, s.totalSoldCents)
        assertEquals(0L, s.realizedPnlCents)
    }

    @Test
    fun `ordem de entrada nao altera o resultado`() {
        val s = PortfolioCalculator.summarize(planilhaCompleta().reversed())
        assertEquals(4_260_456L, s.balanceSats)
        assertEquals(34_268_632L, s.averagePriceCents)
    }

    @Test
    fun `venda parcial preserva o preco medio e realiza lucro`() {
        // Compra 1 BTC por R$ 100.000; vende 0,5 BTC por R$ 70.000.
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 7_000_000, 50_000_000, 14_000_000, id = "v1"),
        )
        val s = PortfolioCalculator.summarize(txs)
        assertEquals(50_000_000L, s.balanceSats)
        assertEquals(5_000_000L, s.costCents)
        assertEquals(10_000_000L, s.averagePriceCents)   // preço médio inalterado
        assertEquals(2_000_000L, s.realizedPnlCents)     // 70.000 - 50.000 = 20.000
        assertEquals(7_000_000L, s.totalSoldCents)
    }

    @Test
    fun `venda total zera custo e preco medio sem residuo`() {
        // Três compras com valores que não dividem exato, depois venda de tudo.
        val txs = listOf(
            compra(dia(1, 1, 2026), 100_001, 333_333, 30_000_090, id = "c1"),
            compra(dia(2, 1, 2026), 100_001, 333_333, 30_000_090, id = "c2"),
            compra(dia(3, 1, 2026), 100_001, 333_334, 30_000_000, id = "c3"),
            venda(dia(4, 1, 2026), 400_000, 1_000_000, 40_000_000, id = "v1"),
        )
        val s = PortfolioCalculator.summarize(txs)
        assertEquals(0L, s.balanceSats)
        assertEquals(0L, s.costCents)
        assertNull(s.averagePriceCents)
        assertEquals(400_000L - 300_003L, s.realizedPnlCents)
    }

    @Test
    fun `venda com prejuizo produz resultado negativo`() {
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 3_000_000, 50_000_000, 6_000_000, id = "v1"),
        )
        val s = PortfolioCalculator.summarize(txs)
        assertEquals(-2_000_000L, s.realizedPnlCents)
    }

    @Test
    fun `compra apos venda recalcula o preco medio`() {
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 7_000_000, 50_000_000, 14_000_000, id = "v1"),
            compra(dia(3, 1, 2026), 3_000_000, 20_000_000, 15_000_000, id = "c2"),
        )
        val s = PortfolioCalculator.summarize(txs)
        assertEquals(70_000_000L, s.balanceSats)
        assertEquals(8_000_000L, s.costCents)
        // 8.000.000 * 1e8 / 70.000.000 = 11.428.571,42 -> 11.428.571
        assertEquals(11_428_571L, s.averagePriceCents)
    }

    @Test
    fun `venda acima do saldo e limitada em vez de lancar excecao`() {
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 30_000_000, 200_000_000, 15_000_000, id = "v1"),
        )
        val s = PortfolioCalculator.summarize(txs)
        assertEquals(0L, s.balanceSats)
        assertEquals(0L, s.costCents)
    }
}
```

- [ ] **Step 4: Rodar o teste e confirmar que falha**

Run: `./gradlew :core:test`
Expected: FAIL — erros de compilação, `Unresolved reference: PortfolioCalculator`, `Unresolved reference: model`

- [ ] **Step 5: Implementar o modelo**

`core/src/main/kotlin/com/pablo/btcmedio/core/model/Transaction.kt`:

```kotlin
package com.pablo.btcmedio.core.model

enum class TransactionType { BUY, SELL }

enum class EntrySource { MANUAL, IMAGE, AUDIO, TEXT }

/**
 * Uma negociação de Bitcoin.
 *
 * Valores em reais são sempre centavos; quantidades de Bitcoin são sempre satoshis.
 * Ponto flutuante nunca entra aqui.
 */
data class Transaction(
    val id: String,
    val type: TransactionType,
    /** Instante da negociação na exchange, em epoch millis. */
    val occurredAt: Long,
    /**
     * O dinheiro que se moveu na conta do usuário: debitado na compra
     * (já contendo a taxa) e creditado na venda (já descontada a taxa).
     */
    val fiatAmountCents: Long,
    /** Taxa cobrada. `null` significa desconhecida; `0` significa sem taxa. */
    val feeCents: Long?,
    val satoshis: Long,
    /** Cotação R$/BTC informada pela exchange. */
    val unitPriceCents: Long,
    val source: EntrySource,
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
```

`core/src/main/kotlin/com/pablo/btcmedio/core/model/PortfolioSummary.kt`:

```kotlin
package com.pablo.btcmedio.core.model

/**
 * Estado agregado da carteira.
 *
 * @param averagePriceCents `null` quando o saldo é zero — não há preço médio a exibir.
 */
data class PortfolioSummary(
    val balanceSats: Long,
    val costCents: Long,
    val averagePriceCents: Long?,
    val totalBoughtCents: Long,
    val totalSoldCents: Long,
    val realizedPnlCents: Long,
) {
    companion object {
        val EMPTY = PortfolioSummary(0L, 0L, null, 0L, 0L, 0L)
    }
}
```

- [ ] **Step 6: Implementar o PortfolioCalculator**

`core/src/main/kotlin/com/pablo/btcmedio/core/calc/PortfolioCalculator.kt`:

```kotlin
package com.pablo.btcmedio.core.calc

import com.pablo.btcmedio.core.model.PortfolioSummary
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Custo médio ponderado: a venda reduz posição e custo proporcionalmente,
 * deixando o preço médio inalterado, e materializa o lucro ou prejuízo.
 */
object PortfolioCalculator {

    const val SATS_PER_BTC: Long = 100_000_000L

    fun summarize(transactions: List<Transaction>): PortfolioSummary {
        var costCents = 0L
        var balanceSats = 0L
        var totalBoughtCents = 0L
        var totalSoldCents = 0L
        var realizedPnlCents = 0L

        for (t in transactions.sortedWith(compareBy({ it.occurredAt }, { it.id }))) {
            when (t.type) {
                TransactionType.BUY -> {
                    costCents += t.fiatAmountCents
                    balanceSats += t.satoshis
                    totalBoughtCents += t.fiatAmountCents
                }

                TransactionType.SELL -> {
                    if (balanceSats <= 0L) continue
                    // Limitação defensiva: a validação impede que este estado seja gravado,
                    // mas a tela de resumo nunca deve travar por dado inconsistente em disco.
                    val soldSats = minOf(t.satoshis, balanceSats)
                    val soldCost = if (soldSats == balanceSats) {
                        costCents // zeragem exata, sem resíduo de arredondamento
                    } else {
                        BigDecimal.valueOf(costCents)
                            .multiply(BigDecimal.valueOf(soldSats))
                            .divide(BigDecimal.valueOf(balanceSats), 0, RoundingMode.HALF_UP)
                            .toLong()
                    }
                    realizedPnlCents += t.fiatAmountCents - soldCost
                    costCents -= soldCost
                    balanceSats -= soldSats
                    totalSoldCents += t.fiatAmountCents
                }
            }
        }

        val averagePriceCents = if (balanceSats > 0L) {
            BigDecimal.valueOf(costCents)
                .multiply(BigDecimal.valueOf(SATS_PER_BTC))
                .divide(BigDecimal.valueOf(balanceSats), 0, RoundingMode.HALF_UP)
                .toLong()
        } else {
            null
        }

        return PortfolioSummary(
            balanceSats = balanceSats,
            costCents = costCents,
            averagePriceCents = averagePriceCents,
            totalBoughtCents = totalBoughtCents,
            totalSoldCents = totalSoldCents,
            realizedPnlCents = realizedPnlCents,
        )
    }
}
```

- [ ] **Step 7: Rodar os testes e confirmar que passam**

Run: `./gradlew :core:test`
Expected: PASS — 8 testes verdes em `PortfolioCalculatorTest`

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat(core): esqueleto Gradle e cálculo de custo médio ponderado"
```

---

## Task 2: SequenceValidator

Detecta venda acima do saldo **na sequência inteira**, não na transação isolada — porque editar uma compra antiga pode invalidar uma venda posterior.

**Files:**
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/calc/SequenceValidator.kt`
- Test: `core/src/test/kotlin/com/pablo/btcmedio/core/calc/SequenceValidatorTest.kt`

**Interfaces:**
- Consumes: `Transaction`, `TransactionType` (Task 1)
- Produces:
  - `sealed interface SequenceCheck` com `data object Ok` e `data class Oversold(transactionId: String, occurredAt: Long, availableSats: Long, requestedSats: Long)`
  - `object SequenceValidator { fun check(transactions: List<Transaction>): SequenceCheck }`

- [ ] **Step 1: Escrever o teste que falha**

`core/src/test/kotlin/com/pablo/btcmedio/core/calc/SequenceValidatorTest.kt`:

```kotlin
package com.pablo.btcmedio.core.calc

import com.pablo.btcmedio.core.compra
import com.pablo.btcmedio.core.dia
import com.pablo.btcmedio.core.planilhaCompleta
import com.pablo.btcmedio.core.venda
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SequenceValidatorTest {

    @Test
    fun `sequencia so de compras e valida`() {
        assertEquals(SequenceCheck.Ok, SequenceValidator.check(planilhaCompleta()))
    }

    @Test
    fun `venda dentro do saldo e valida`() {
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 7_000_000, 50_000_000, 14_000_000, id = "v1"),
        )
        assertEquals(SequenceCheck.Ok, SequenceValidator.check(txs))
    }

    @Test
    fun `venda acima do saldo e reprovada com o saldo disponivel`() {
        val txs = listOf(
            compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
            venda(dia(2, 1, 2026), 30_000_000, 150_000_000, 20_000_000, id = "v1"),
        )
        val result = SequenceValidator.check(txs)
        assertTrue(result is SequenceCheck.Oversold)
        result as SequenceCheck.Oversold
        assertEquals("v1", result.transactionId)
        assertEquals(100_000_000L, result.availableSats)
        assertEquals(150_000_000L, result.requestedSats)
    }

    @Test
    fun `venda anterior a compra e reprovada mesmo com saldo final positivo`() {
        val txs = listOf(
            venda(dia(1, 1, 2026), 3_000_000, 20_000_000, 15_000_000, id = "v1"),
            compra(dia(2, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1"),
        )
        val result = SequenceValidator.check(txs)
        assertTrue(result is SequenceCheck.Oversold)
        assertEquals("v1", (result as SequenceCheck.Oversold).transactionId)
        assertEquals(0L, result.availableSats)
    }

    @Test
    fun `reduzir uma compra antiga invalida uma venda posterior`() {
        val compraOriginal = compra(dia(1, 1, 2026), 10_000_000, 100_000_000, 10_000_000, id = "c1")
        val vendaPosterior = venda(dia(2, 1, 2026), 7_000_000, 50_000_000, 14_000_000, id = "v1")
        assertEquals(SequenceCheck.Ok, SequenceValidator.check(listOf(compraOriginal, vendaPosterior)))

        val compraReduzida = compraOriginal.copy(satoshis = 10_000_000)
        val result = SequenceValidator.check(listOf(compraReduzida, vendaPosterior))
        assertTrue(result is SequenceCheck.Oversold)
        assertEquals("v1", (result as SequenceCheck.Oversold).transactionId)
        assertEquals(10_000_000L, result.availableSats)
    }

    @Test
    fun `reporta a primeira violacao quando ha varias`() {
        val txs = listOf(
            venda(dia(1, 1, 2026), 100, 1_000, 10_000_000, id = "v1"),
            venda(dia(2, 1, 2026), 100, 2_000, 10_000_000, id = "v2"),
        )
        assertEquals("v1", (SequenceValidator.check(txs) as SequenceCheck.Oversold).transactionId)
    }
}
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./gradlew :core:test --tests "*SequenceValidatorTest*"`
Expected: FAIL — `Unresolved reference: SequenceValidator`

- [ ] **Step 3: Implementar**

`core/src/main/kotlin/com/pablo/btcmedio/core/calc/SequenceValidator.kt`:

```kotlin
package com.pablo.btcmedio.core.calc

import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType

sealed interface SequenceCheck {
    data object Ok : SequenceCheck

    data class Oversold(
        val transactionId: String,
        val occurredAt: Long,
        val availableSats: Long,
        val requestedSats: Long,
    ) : SequenceCheck
}

/**
 * Verifica se a sequência inteira é consistente: nenhuma venda pode exceder
 * o saldo acumulado até o instante em que ocorre.
 *
 * Validar a sequência inteira (e não a transação isolada) é o que impede que
 * editar ou excluir uma compra antiga deixe uma venda posterior órfã de saldo.
 */
object SequenceValidator {

    fun check(transactions: List<Transaction>): SequenceCheck {
        var balanceSats = 0L
        for (t in transactions.sortedWith(compareBy({ it.occurredAt }, { it.id }))) {
            when (t.type) {
                TransactionType.BUY -> balanceSats += t.satoshis
                TransactionType.SELL -> {
                    if (t.satoshis > balanceSats) {
                        return SequenceCheck.Oversold(
                            transactionId = t.id,
                            occurredAt = t.occurredAt,
                            availableSats = balanceSats,
                            requestedSats = t.satoshis,
                        )
                    }
                    balanceSats -= t.satoshis
                }
            }
        }
        return SequenceCheck.Ok
    }
}
```

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew :core:test`
Expected: PASS — 14 testes verdes

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(core): validação de sequência contra venda acima do saldo"
```

---

## Task 3: BrazilianNumberParser

**Files:**
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/parse/BrazilianNumberParser.kt`
- Test: `core/src/test/kotlin/com/pablo/btcmedio/core/parse/BrazilianNumberParserTest.kt`

**Interfaces:**
- Consumes: nada
- Produces: `object BrazilianNumberParser` com
  - `fun parseDecimal(raw: String): BigDecimal?`
  - `fun parseCents(raw: String): Long?`
  - `fun parseSatoshis(raw: String): Long?`
  - `val NUMBER_PATTERN: Regex` — encontra números dentro de texto corrido

- [ ] **Step 1: Escrever o teste que falha**

`core/src/test/kotlin/com/pablo/btcmedio/core/parse/BrazilianNumberParserTest.kt`:

```kotlin
package com.pablo.btcmedio.core.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrazilianNumberParserTest {

    @Test
    fun `valor em reais com simbolo e separadores`() {
        assertEquals(150_000L, BrazilianNumberParser.parseCents("R$ 1.500,00"))
        assertEquals(150_000L, BrazilianNumberParser.parseCents("- R$ 1.500,00"))
        assertEquals(2_250L, BrazilianNumberParser.parseCents("R$ 22,50"))
        assertEquals(147_750L, BrazilianNumberParser.parseCents("R$ 1.477,50"))
        assertEquals(31_564_152L, BrazilianNumberParser.parseCents("R$ 315.641,52"))
    }

    @Test
    fun `milhar sem decimais e interpretado como inteiro`() {
        assertEquals(150_000L, BrazilianNumberParser.parseCents("1.500"))
        assertEquals(123_456_700L, BrazilianNumberParser.parseCents("1.234.567"))
    }

    @Test
    fun `ponto como separador decimal quando nao e padrao de milhar`() {
        assertEquals(150_050L, BrazilianNumberParser.parseCents("1500.50"))
        assertEquals(468_094L, BrazilianNumberParser.parseSatoshis("0.00468094"))
    }

    @Test
    fun `quantidade de bitcoin com simbolo`() {
        assertEquals(468_094L, BrazilianNumberParser.parseSatoshis("₿ 0,00468094"))
        assertEquals(468_094L, BrazilianNumberParser.parseSatoshis("+ ₿ 0,00468094"))
        assertEquals(1_174_383L, BrazilianNumberParser.parseSatoshis("0,01174383 BTC"))
        assertEquals(100_000_000L, BrazilianNumberParser.parseSatoshis("1"))
    }

    @Test
    fun `mais de oito casas decimais arredonda para satoshi`() {
        assertEquals(468_094L, BrazilianNumberParser.parseSatoshis("0,004680944"))
        assertEquals(468_095L, BrazilianNumberParser.parseSatoshis("0,004680945"))
    }

    @Test
    fun `arredondamento de centavos e half up`() {
        assertEquals(1_235L, BrazilianNumberParser.parseCents("12,345"))
        assertEquals(1_234L, BrazilianNumberParser.parseCents("12,344"))
    }

    @Test
    fun `valor negativo preserva o sinal em parseDecimal mas parseCents usa modulo`() {
        assertEquals(0, BrazilianNumberParser.parseDecimal("-1.500,00")!!.compareTo(java.math.BigDecimal("-1500.00")))
        assertEquals(150_000L, BrazilianNumberParser.parseCents("-1.500,00"))
    }

    @Test
    fun `entradas invalidas devolvem null`() {
        assertNull(BrazilianNumberParser.parseCents(""))
        assertNull(BrazilianNumberParser.parseCents("Concluída"))
        assertNull(BrazilianNumberParser.parseCents("R$"))
        assertNull(BrazilianNumberParser.parseSatoshis("abc"))
    }

    @Test
    fun `encontra numeros dentro de texto corrido`() {
        val achados = BrazilianNumberParser.NUMBER_PATTERN
            .findAll("Comprei R$ 1.500,00 de bitcoin a 315.641,52 e recebi 0,00468094")
            .map { it.value }
            .toList()
        assertEquals(listOf("1.500,00", "315.641,52", "0,00468094"), achados)
    }
}
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./gradlew :core:test --tests "*BrazilianNumberParserTest*"`
Expected: FAIL — `Unresolved reference: BrazilianNumberParser`

- [ ] **Step 3: Implementar**

`core/src/main/kotlin/com/pablo/btcmedio/core/parse/BrazilianNumberParser.kt`:

```kotlin
package com.pablo.btcmedio.core.parse

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Interpreta números no formato brasileiro, tolerante ao ruído do OCR e da fala.
 *
 * A ambiguidade real é o ponto: em "1.500" ele é separador de milhar, em
 * "0.00468094" é separador decimal. A regra usada é estrutural — só é milhar
 * quando o número inteiro casa exatamente com o padrão `d{1,3}(.d{3})+`.
 */
object BrazilianNumberParser {

    /** Encontra números dentro de texto corrido, com ou sem separadores. */
    val NUMBER_PATTERN = Regex("""\d{1,3}(?:\.\d{3})+(?:,\d+)?|\d+(?:[.,]\d+)?""")

    private val THOUSANDS_ONLY = Regex("""^\d{1,3}(?:\.\d{3})+$""")
    private val NOISE = Regex("""[^0-9.,\-]""")

    fun parseDecimal(raw: String): BigDecimal? {
        val negative = raw.trimStart().startsWith("-")
        var s = NOISE.replace(raw, "").replace("-", "")
        if (s.isEmpty()) return null

        s = when {
            s.contains(',') -> s.replace(".", "").replace(',', '.')
            THOUSANDS_ONLY.matches(s) -> s.replace(".", "")
            else -> s
        }

        if (s.count { it == '.' } > 1) return null
        if (s.isEmpty() || s == ".") return null

        val value = try {
            BigDecimal(s)
        } catch (e: NumberFormatException) {
            return null
        }
        return if (negative) value.negate() else value
    }

    /** Converte para centavos, em módulo — o sinal vem do tipo da transação, não do texto. */
    fun parseCents(raw: String): Long? =
        parseDecimal(raw)?.abs()?.setScale(2, RoundingMode.HALF_UP)?.movePointRight(2)?.toLong()

    /** Converte para satoshis, em módulo. */
    fun parseSatoshis(raw: String): Long? =
        parseDecimal(raw)?.abs()?.setScale(8, RoundingMode.HALF_UP)?.movePointRight(8)?.toLong()
}
```

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew :core:test`
Expected: PASS — todos verdes

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(core): parser de números no formato brasileiro"
```

---

## Task 4: BrazilianDateParser

**Files:**
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/parse/BrazilianDateParser.kt`
- Test: `core/src/test/kotlin/com/pablo/btcmedio/core/parse/BrazilianDateParserTest.kt`

**Interfaces:**
- Consumes: nada
- Produces: `object BrazilianDateParser { fun parse(raw: String, today: LocalDate): LocalDateTime? }`

- [ ] **Step 1: Escrever o teste que falha**

`core/src/test/kotlin/com/pablo/btcmedio/core/parse/BrazilianDateParserTest.kt`:

```kotlin
package com.pablo.btcmedio.core.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BrazilianDateParserTest {

    private val hoje = LocalDate.of(2026, 8, 29)

    @Test
    fun `data com hora`() {
        assertEquals(
            LocalDateTime.of(2026, 6, 30, 22, 57),
            BrazilianDateParser.parse("30/06/2026 22:57", hoje),
        )
    }

    @Test
    fun `data com hora e segundos`() {
        assertEquals(
            LocalDateTime.of(2026, 6, 30, 22, 57, 13),
            BrazilianDateParser.parse("30/06/2026 22:57:13", hoje),
        )
    }

    @Test
    fun `data sem hora comeca a meia noite`() {
        assertEquals(
            LocalDateTime.of(2026, 2, 5, 0, 0),
            BrazilianDateParser.parse("05/02/2026", hoje),
        )
    }

    @Test
    fun `ano com dois digitos`() {
        assertEquals(
            LocalDateTime.of(2026, 2, 5, 0, 0),
            BrazilianDateParser.parse("05/02/26", hoje),
        )
    }

    @Test
    fun `data sem ano assume o ano corrente`() {
        assertEquals(
            LocalDateTime.of(2026, 2, 5, 0, 0),
            BrazilianDateParser.parse("05/02", hoje),
        )
    }

    @Test
    fun `data dentro de texto corrido com a preposicao as`() {
        assertEquals(
            LocalDateTime.of(2026, 6, 30, 22, 57),
            BrazilianDateParser.parse("Comprei em 30/06/2026 às 22:57 na corretora", hoje),
        )
    }

    @Test
    fun `separador por hifen ou ponto`() {
        assertEquals(LocalDateTime.of(2026, 6, 30, 0, 0), BrazilianDateParser.parse("30-06-2026", hoje))
        assertEquals(LocalDateTime.of(2026, 6, 30, 0, 0), BrazilianDateParser.parse("30.06.2026", hoje))
    }

    @Test
    fun `expressoes relativas`() {
        assertEquals(LocalDateTime.of(2026, 8, 29, 0, 0), BrazilianDateParser.parse("hoje", hoje))
        assertEquals(LocalDateTime.of(2026, 8, 28, 0, 0), BrazilianDateParser.parse("ontem", hoje))
        assertEquals(LocalDateTime.of(2026, 8, 27, 0, 0), BrazilianDateParser.parse("anteontem", hoje))
        assertEquals(LocalDateTime.of(2026, 8, 28, 0, 0), BrazilianDateParser.parse("Comprei ontem 500 reais", hoje))
    }

    @Test
    fun `hora sozinha em expressao relativa`() {
        assertEquals(
            LocalDateTime.of(2026, 8, 29, 14, 30),
            BrazilianDateParser.parse("hoje às 14:30", hoje),
        )
    }

    @Test
    fun `texto sem data devolve null`() {
        assertNull(BrazilianDateParser.parse("Concluída", hoje))
        assertNull(BrazilianDateParser.parse("", hoje))
    }

    @Test
    fun `data impossivel devolve null`() {
        assertNull(BrazilianDateParser.parse("32/13/2026", hoje))
    }
}
```

- [ ] **Step 2: Rodar o teste e confirmar que falha**

Run: `./gradlew :core:test --tests "*BrazilianDateParserTest*"`
Expected: FAIL — `Unresolved reference: BrazilianDateParser`

- [ ] **Step 3: Implementar**

`core/src/main/kotlin/com/pablo/btcmedio/core/parse/BrazilianDateParser.kt`:

```kotlin
package com.pablo.btcmedio.core.parse

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Interpreta datas brasileiras em texto de comprovante ou fala.
 *
 * `today` é injetado em vez de lido do relógio para que expressões relativas
 * ("ontem") sejam testáveis de forma determinística.
 */
object BrazilianDateParser {

    private val DATE = Regex("""\b(\d{1,2})[/\-.](\d{1,2})(?:[/\-.](\d{2,4}))?\b""")
    private val TIME = Regex("""\b(\d{1,2}):(\d{2})(?::(\d{2}))?\b""")

    fun parse(raw: String, today: LocalDate): LocalDateTime? {
        if (raw.isBlank()) return null
        val time = TIME.find(raw)
        val date = DATE.find(raw)

        val day: LocalDate = when {
            date != null -> {
                val d = date.groupValues[1].toInt()
                val m = date.groupValues[2].toInt()
                val yearRaw = date.groupValues[3]
                val y = when {
                    yearRaw.isEmpty() -> today.year
                    yearRaw.length <= 2 -> 2000 + yearRaw.toInt()
                    else -> yearRaw.toInt()
                }
                try {
                    LocalDate.of(y, m, d)
                } catch (e: DateTimeException) {
                    return null
                }
            }

            else -> relativeDay(raw, today) ?: return null
        }

        if (time == null) return day.atStartOfDay()

        val h = time.groupValues[1].toInt()
        val min = time.groupValues[2].toInt()
        val sec = time.groupValues[3].ifEmpty { "0" }.toInt()
        return try {
            day.atTime(h, min, sec)
        } catch (e: DateTimeException) {
            day.atStartOfDay()
        }
    }

    private fun relativeDay(raw: String, today: LocalDate): LocalDate? {
        val normalized = raw.lowercase()
        return when {
            normalized.contains("anteontem") -> today.minusDays(2)
            normalized.contains("ontem") -> today.minusDays(1)
            normalized.contains("hoje") -> today
            else -> null
        }
    }
}
```

- [ ] **Step 4: Rodar os testes e confirmar que passam**

Run: `./gradlew :core:test`
Expected: PASS — todos verdes

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(core): parser de datas brasileiras e expressões relativas"
```

---

## Task 5: TransactionDraft, DraftCompleter e DraftValidator

**Files:**
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/draft/TransactionDraft.kt`
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/draft/DraftCompleter.kt`
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/draft/DraftValidator.kt`
- Test: `core/src/test/kotlin/com/pablo/btcmedio/core/draft/DraftCompleterTest.kt`
- Test: `core/src/test/kotlin/com/pablo/btcmedio/core/draft/DraftValidatorTest.kt`

**Interfaces:**
- Consumes: `TransactionType`, `EntrySource`, `PortfolioCalculator.SATS_PER_BTC` (Task 1)
- Produces:
  - `data class TransactionDraft(type, occurredAt: Long?, fiatAmountCents: Long?, feeCents: Long?, satoshis: Long?, unitPriceCents: Long?, source, note, rawText: String?)` com `fun tradedFiatCents(): Long?` e `fun isComplete(): Boolean`
  - `enum class DraftField { DATE, FIAT, FEE, SATS, PRICE }`
  - `object DraftCompleter { fun missingField(d: TransactionDraft): DraftField?; fun complete(d: TransactionDraft): TransactionDraft; fun impliedFeeCents(d: TransactionDraft): Long? }`
  - `enum class Severity { ERROR, WARNING }`
  - `data class DraftIssue(val field: DraftField?, val severity: Severity, val message: String)`
  - `object DraftValidator { const val SATS_TOLERANCE = 2L; fun validate(d: TransactionDraft): List<DraftIssue> }`

- [ ] **Step 1: Escrever os testes que falham**

`core/src/test/kotlin/com/pablo/btcmedio/core/draft/DraftCompleterTest.kt`:

```kotlin
package com.pablo.btcmedio.core.draft

import com.pablo.btcmedio.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftCompleterTest {

    /** O comprovante anexo, completo. */
    private val comprovante = TransactionDraft(
        type = TransactionType.BUY,
        occurredAt = 1_782_000_000_000L,
        fiatAmountCents = 150_000,
        feeCents = 2_250,
        satoshis = 468_094,
        unitPriceCents = 31_564_152,
    )

    @Test
    fun `valor negociado desconta a taxa na compra`() {
        assertEquals(147_750L, comprovante.tradedFiatCents())
    }

    @Test
    fun `valor negociado soma a taxa na venda`() {
        val v = comprovante.copy(type = TransactionType.SELL)
        assertEquals(152_250L, v.tradedFiatCents())
    }

    @Test
    fun `completa a quantidade de bitcoin`() {
        val d = comprovante.copy(satoshis = null)
        assertEquals(DraftField.SATS, DraftCompleter.missingField(d))
        assertEquals(468_094L, DraftCompleter.complete(d).satoshis)
    }

    @Test
    fun `completa a cotacao`() {
        val d = comprovante.copy(unitPriceCents = null)
        assertEquals(DraftField.PRICE, DraftCompleter.missingField(d))
        // 147.750 * 1e8 / 468.094 = 31.564.144,7 -> 31.564.145
        assertEquals(31_564_145L, DraftCompleter.complete(d).unitPriceCents)
    }

    @Test
    fun `completa o valor em reais`() {
        val d = comprovante.copy(fiatAmountCents = null)
        assertEquals(DraftField.FIAT, DraftCompleter.missingField(d))
        assertEquals(150_000L, DraftCompleter.complete(d).fiatAmountCents)
    }

    @Test
    fun `completa a taxa`() {
        val d = comprovante.copy(feeCents = null)
        assertEquals(DraftField.FEE, DraftCompleter.missingField(d))
        assertEquals(2_250L, DraftCompleter.complete(d).feeCents)
    }

    @Test
    fun `deriva a taxa implicita da primeira linha da planilha`() {
        // R$ 4.000,00 por 0,01174383 BTC a R$ 337.198,31 -> taxa implícita R$ 40,00
        val d = TransactionDraft(
            type = TransactionType.BUY,
            occurredAt = 1_770_000_000_000L,
            fiatAmountCents = 400_000,
            feeCents = null,
            satoshis = 1_174_383,
            unitPriceCents = 33_719_831,
        )
        assertEquals(4_000L, DraftCompleter.impliedFeeCents(d))
    }

    @Test
    fun `nao completa quando faltam dois campos`() {
        val d = comprovante.copy(satoshis = null, unitPriceCents = null)
        assertNull(DraftCompleter.missingField(d))
        assertEquals(d, DraftCompleter.complete(d))
    }

    @Test
    fun `nunca sobrescreve campo ja preenchido`() {
        val adulterado = comprovante.copy(satoshis = 999_999)
        assertEquals(999_999L, DraftCompleter.complete(adulterado).satoshis)
    }

    @Test
    fun `data faltante nao impede completar os numeros`() {
        val d = comprovante.copy(occurredAt = null, satoshis = null)
        assertEquals(468_094L, DraftCompleter.complete(d).satoshis)
    }
}
```

`core/src/test/kotlin/com/pablo/btcmedio/core/draft/DraftValidatorTest.kt`:

```kotlin
package com.pablo.btcmedio.core.draft

import com.pablo.btcmedio.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftValidatorTest {

    private val comprovante = TransactionDraft(
        type = TransactionType.BUY,
        occurredAt = 1_782_000_000_000L,
        fiatAmountCents = 150_000,
        feeCents = 2_250,
        satoshis = 468_094,
        unitPriceCents = 31_564_152,
    )

    @Test
    fun `comprovante real e coerente`() {
        assertEquals(emptyList<DraftIssue>(), DraftValidator.validate(comprovante))
    }

    @Test
    fun `taxa desconhecida nao dispara a invariante`() {
        val d = TransactionDraft(
            type = TransactionType.BUY,
            occurredAt = 1_770_000_000_000L,
            fiatAmountCents = 400_000,
            feeCents = null,
            satoshis = 1_174_383,
            unitPriceCents = 33_719_831,
        )
        assertEquals(emptyList<DraftIssue>(), DraftValidator.validate(d))
    }

    @Test
    fun `divergencia acima da tolerancia vira aviso`() {
        val d = comprovante.copy(satoshis = 468_094 + 50)
        val issues = DraftValidator.validate(d)
        assertTrue(issues.any { it.severity == Severity.WARNING && it.field == DraftField.SATS })
    }

    @Test
    fun `divergencia de dois satoshis passa`() {
        assertEquals(emptyList<DraftIssue>(), DraftValidator.validate(comprovante.copy(satoshis = 468_096)))
    }

    @Test
    fun `taxa implicita negativa vira aviso`() {
        val d = comprovante.copy(feeCents = null, satoshis = 600_000)
        assertTrue(DraftValidator.validate(d).any { it.severity == Severity.WARNING })
    }

    @Test
    fun `taxa implicita acima de cinco por cento vira aviso`() {
        val d = comprovante.copy(feeCents = null, satoshis = 400_000)
        assertTrue(DraftValidator.validate(d).any { it.severity == Severity.WARNING })
    }

    @Test
    fun `campos obrigatorios ausentes viram erro`() {
        val vazio = TransactionDraft(type = TransactionType.BUY)
        val issues = DraftValidator.validate(vazio)
        val erros = issues.filter { it.severity == Severity.ERROR }.mapNotNull { it.field }.toSet()
        assertEquals(setOf(DraftField.DATE, DraftField.FIAT, DraftField.SATS, DraftField.PRICE), erros)
    }

    @Test
    fun `valores nao positivos viram erro`() {
        val d = comprovante.copy(fiatAmountCents = 0, satoshis = 0, unitPriceCents = 0)
        val erros = DraftValidator.validate(d).filter { it.severity == Severity.ERROR }
        assertEquals(3, erros.size)
    }

    @Test
    fun `taxa negativa vira erro`() {
        val d = comprovante.copy(feeCents = -1)
        assertTrue(DraftValidator.validate(d).any { it.severity == Severity.ERROR && it.field == DraftField.FEE })
    }
}
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew :core:test --tests "*Draft*"`
Expected: FAIL — `Unresolved reference: TransactionDraft`

- [ ] **Step 3: Implementar TransactionDraft**

`core/src/main/kotlin/com/pablo/btcmedio/core/draft/TransactionDraft.kt`:

```kotlin
package com.pablo.btcmedio.core.draft

import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import java.util.UUID

enum class DraftField { DATE, FIAT, FEE, SATS, PRICE }

/**
 * Rascunho de transação: o formato intermediário entre qualquer fonte de
 * entrada e a gravação. Todo campo é opcional porque uma extração parcial
 * ainda é útil — ela chega ao usuário como formulário pré-preenchido.
 */
data class TransactionDraft(
    val type: TransactionType = TransactionType.BUY,
    val occurredAt: Long? = null,
    val fiatAmountCents: Long? = null,
    val feeCents: Long? = null,
    val satoshis: Long? = null,
    val unitPriceCents: Long? = null,
    val source: EntrySource = EntrySource.MANUAL,
    val note: String? = null,
    /** Texto original reconhecido, exibido na confirmação para conferência. */
    val rawText: String? = null,
) {
    /** O valor efetivamente convertido em Bitcoin, sem a taxa. */
    fun tradedFiatCents(): Long? {
        val fiat = fiatAmountCents ?: return null
        val fee = feeCents ?: 0L
        return when (type) {
            TransactionType.BUY -> fiat - fee
            TransactionType.SELL -> fiat + fee
        }
    }

    fun isComplete(): Boolean =
        occurredAt != null && fiatAmountCents != null && satoshis != null && unitPriceCents != null

    fun toTransaction(id: String = UUID.randomUUID().toString(), now: Long): Transaction = Transaction(
        id = id,
        type = type,
        occurredAt = requireNotNull(occurredAt) { "occurredAt é obrigatório" },
        fiatAmountCents = requireNotNull(fiatAmountCents) { "fiatAmountCents é obrigatório" },
        feeCents = feeCents,
        satoshis = requireNotNull(satoshis) { "satoshis é obrigatório" },
        unitPriceCents = requireNotNull(unitPriceCents) { "unitPriceCents é obrigatório" },
        source = source,
        note = note,
        createdAt = now,
        updatedAt = now,
    )

    companion object {
        fun from(t: Transaction): TransactionDraft = TransactionDraft(
            type = t.type,
            occurredAt = t.occurredAt,
            fiatAmountCents = t.fiatAmountCents,
            feeCents = t.feeCents,
            satoshis = t.satoshis,
            unitPriceCents = t.unitPriceCents,
            source = t.source,
            note = t.note,
        )
    }
}
```

- [ ] **Step 4: Implementar DraftCompleter**

`core/src/main/kotlin/com/pablo/btcmedio/core/draft/DraftCompleter.kt`:

```kotlin
package com.pablo.btcmedio.core.draft

import com.pablo.btcmedio.core.calc.PortfolioCalculator.SATS_PER_BTC
import com.pablo.btcmedio.core.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Os quatro números (valor, taxa, quantidade, cotação) têm três graus de
 * liberdade. Quando exatamente um falta, ele é dedutível dos outros.
 */
object DraftCompleter {

    /** O único campo numérico ausente, ou `null` quando faltam zero ou mais de um. */
    fun missingField(d: TransactionDraft): DraftField? {
        val missing = buildList {
            if (d.fiatAmountCents == null) add(DraftField.FIAT)
            if (d.feeCents == null) add(DraftField.FEE)
            if (d.satoshis == null) add(DraftField.SATS)
            if (d.unitPriceCents == null) add(DraftField.PRICE)
        }
        return missing.singleOrNull()
    }

    /** Preenche o campo ausente. Nunca sobrescreve valor já presente. */
    fun complete(d: TransactionDraft): TransactionDraft = when (missingField(d)) {
        DraftField.SATS -> d.copy(satoshis = deriveSats(d))
        DraftField.PRICE -> d.copy(unitPriceCents = derivePrice(d))
        DraftField.FIAT -> d.copy(fiatAmountCents = deriveFiat(d))
        DraftField.FEE -> d.copy(feeCents = impliedFeeCents(d))
        else -> d
    }

    /**
     * Taxa deduzida dos outros três números:
     * `fiatAmount - satoshis * unitPrice / 1e8` na compra (invertido na venda).
     */
    fun impliedFeeCents(d: TransactionDraft): Long? {
        val fiat = d.fiatAmountCents ?: return null
        val traded = tradedFromSatsAndPrice(d) ?: return null
        return when (d.type) {
            TransactionType.BUY -> fiat - traded
            TransactionType.SELL -> traded - fiat
        }
    }

    private fun deriveSats(d: TransactionDraft): Long? {
        val traded = d.tradedFiatCents() ?: return null
        val price = d.unitPriceCents?.takeIf { it > 0 } ?: return null
        return BigDecimal.valueOf(traded)
            .multiply(BigDecimal.valueOf(SATS_PER_BTC))
            .divide(BigDecimal.valueOf(price), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun derivePrice(d: TransactionDraft): Long? {
        val traded = d.tradedFiatCents() ?: return null
        val sats = d.satoshis?.takeIf { it > 0 } ?: return null
        return BigDecimal.valueOf(traded)
            .multiply(BigDecimal.valueOf(SATS_PER_BTC))
            .divide(BigDecimal.valueOf(sats), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun deriveFiat(d: TransactionDraft): Long? {
        val traded = tradedFromSatsAndPrice(d) ?: return null
        val fee = d.feeCents ?: 0L
        return when (d.type) {
            TransactionType.BUY -> traded + fee
            TransactionType.SELL -> traded - fee
        }
    }

    /** `satoshis * unitPrice / 1e8`, em centavos. */
    fun tradedFromSatsAndPrice(d: TransactionDraft): Long? {
        val sats = d.satoshis ?: return null
        val price = d.unitPriceCents ?: return null
        return BigDecimal.valueOf(sats)
            .multiply(BigDecimal.valueOf(price))
            .divide(BigDecimal.valueOf(SATS_PER_BTC), 0, RoundingMode.HALF_UP)
            .toLong()
    }
}
```

- [ ] **Step 5: Implementar DraftValidator**

`core/src/main/kotlin/com/pablo/btcmedio/core/draft/DraftValidator.kt`:

```kotlin
package com.pablo.btcmedio.core.draft

import com.pablo.btcmedio.core.calc.PortfolioCalculator.SATS_PER_BTC
import java.math.BigDecimal
import java.math.RoundingMode

enum class Severity { ERROR, WARNING }

data class DraftIssue(
    val field: DraftField?,
    val severity: Severity,
    val message: String,
)

/**
 * Confere se os quatro números fecham entre si.
 *
 * ERROR impede gravar. WARNING é exibido mas não bloqueia — um comprovante
 * pode ser legitimamente atípico, e a decisão é do usuário.
 */
object DraftValidator {

    /** Folga aceita para o arredondamento da exchange. */
    const val SATS_TOLERANCE = 2L

    /** Acima disso, o que está errado não é a taxa: é a leitura de algum número. */
    private const val MAX_FEE_RATIO = 0.05

    fun validate(d: TransactionDraft): List<DraftIssue> = buildList {
        if (d.occurredAt == null) {
            add(DraftIssue(DraftField.DATE, Severity.ERROR, "Informe a data da transação."))
        }
        if (d.fiatAmountCents == null) {
            add(DraftIssue(DraftField.FIAT, Severity.ERROR, "Informe o valor em reais."))
        } else if (d.fiatAmountCents <= 0L) {
            add(DraftIssue(DraftField.FIAT, Severity.ERROR, "O valor em reais deve ser maior que zero."))
        }
        if (d.satoshis == null) {
            add(DraftIssue(DraftField.SATS, Severity.ERROR, "Informe a quantidade de Bitcoin."))
        } else if (d.satoshis <= 0L) {
            add(DraftIssue(DraftField.SATS, Severity.ERROR, "A quantidade de Bitcoin deve ser maior que zero."))
        }
        if (d.unitPriceCents == null) {
            add(DraftIssue(DraftField.PRICE, Severity.ERROR, "Informe a cotação."))
        } else if (d.unitPriceCents <= 0L) {
            add(DraftIssue(DraftField.PRICE, Severity.ERROR, "A cotação deve ser maior que zero."))
        }
        if (d.feeCents != null && d.feeCents < 0L) {
            add(DraftIssue(DraftField.FEE, Severity.ERROR, "A taxa não pode ser negativa."))
        }

        if (any { it.severity == Severity.ERROR }) return@buildList

        if (d.feeCents != null) {
            addAll(checkCoherence(d))
        } else {
            addAll(checkImpliedFee(d))
        }
    }

    /** |satoshis − arredonda(valorNegociado × 1e8 ÷ cotação)| ≤ 2 sats */
    private fun checkCoherence(d: TransactionDraft): List<DraftIssue> {
        val traded = d.tradedFiatCents() ?: return emptyList()
        val price = d.unitPriceCents ?: return emptyList()
        val sats = d.satoshis ?: return emptyList()
        if (traded <= 0L) {
            return listOf(
                DraftIssue(DraftField.FEE, Severity.WARNING, "A taxa é maior que o valor da transação.")
            )
        }
        val expected = BigDecimal.valueOf(traded)
            .multiply(BigDecimal.valueOf(SATS_PER_BTC))
            .divide(BigDecimal.valueOf(price), 0, RoundingMode.HALF_UP)
            .toLong()
        val diff = kotlin.math.abs(sats - expected)
        return if (diff > SATS_TOLERANCE) {
            listOf(
                DraftIssue(
                    DraftField.SATS,
                    Severity.WARNING,
                    "Os números não fecham: valor, taxa e cotação dariam $expected satoshis, não $sats.",
                )
            )
        } else {
            emptyList()
        }
    }

    /** Sem taxa informada, o que se verifica é se a taxa implícita é plausível. */
    private fun checkImpliedFee(d: TransactionDraft): List<DraftIssue> {
        val implied = DraftCompleter.impliedFeeCents(d) ?: return emptyList()
        val fiat = d.fiatAmountCents ?: return emptyList()
        return when {
            implied < 0L -> listOf(
                DraftIssue(
                    DraftField.FEE,
                    Severity.WARNING,
                    "Os números não fecham: a quantidade e a cotação dariam mais reais do que o valor informado.",
                )
            )

            implied > (fiat * MAX_FEE_RATIO) -> listOf(
                DraftIssue(
                    DraftField.FEE,
                    Severity.WARNING,
                    "A taxa implícita seria de ${implied} centavos, acima de 5% do valor. Confira os números.",
                )
            )

            else -> emptyList()
        }
    }
}
```

- [ ] **Step 6: Rodar os testes e confirmar que passam**

Run: `./gradlew :core:test`
Expected: PASS — todos verdes

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat(core): rascunho de transação, dedução do campo faltante e validação"
```

---

## Task 6: DraftExtractor, ExtractorChain e ReceiptRuleExtractor

**Files:**
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/extract/DraftExtractor.kt`
- Create: `core/src/main/kotlin/com/pablo/btcmedio/core/extract/ReceiptRuleExtractor.kt`
- Test: `core/src/test/kotlin/com/pablo/btcmedio/core/extract/ReceiptRuleExtractorTest.kt`
- Test: `core/src/test/kotlin/com/pablo/btcmedio/core/extract/ExtractorChainTest.kt`

**Interfaces:**
- Consumes: `TransactionDraft`, `DraftCompleter` (Task 5), `BrazilianNumberParser`, `BrazilianDateParser` (Tasks 3–4), `EntrySource`, `TransactionType` (Task 1)
- Produces:
  - `interface DraftExtractor { val name: String; suspend fun isAvailable(): Boolean; suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft? }`
  - `class ExtractorChain(private val extractors: List<DraftExtractor>) { suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft }`
  - `class ReceiptRuleExtractor : DraftExtractor`

Nota: `:core` precisa de `kotlinx-coroutines-core` para `suspend`. Adicione em `core/build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 1: Escrever o teste que falha**

`core/src/test/kotlin/com/pablo/btcmedio/core/extract/ReceiptRuleExtractorTest.kt`:

```kotlin
package com.pablo.btcmedio.core.extract

import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReceiptRuleExtractorTest {

    private val extractor = ReceiptRuleExtractor()
    private val hoje = LocalDate.of(2026, 8, 29)

    /** O texto que o OCR produz para o comprovante anexo. */
    private val comprovanteOcr = """
        Detalhes da transação
        - R$ 1.500,00
        + ₿ 0,00468094
        Concluída
        Compra
        30/06/2026 22:57
        Detalhes
        Preço
        R$ 315.641,52
        Taxa
        R$ 22,50
        Total Comprado
        R$ 1.477,50
    """.trimIndent()

    private fun epoch(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        LocalDateTime.of(y, m, d, h, min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `extrai o comprovante inteiro`() = runTest {
        val draft = extractor.extract(comprovanteOcr, EntrySource.IMAGE, hoje)!!
        assertEquals(TransactionType.BUY, draft.type)
        assertEquals(epoch(2026, 6, 30, 22, 57), draft.occurredAt)
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals(2_250L, draft.feeCents)
        assertEquals(468_094L, draft.satoshis)
        assertEquals(31_564_152L, draft.unitPriceCents)
        assertEquals(EntrySource.IMAGE, draft.source)
        assertEquals(comprovanteOcr, draft.rawText)
    }

    @Test
    fun `reconhece venda pelo rotulo`() = runTest {
        val texto = comprovanteOcr
            .replace("Compra", "Venda")
            .replace("Total Comprado", "Total Vendido")
        assertEquals(TransactionType.SELL, extractor.extract(texto, EntrySource.IMAGE, hoje)!!.type)
    }

    @Test
    fun `deduz o valor em reais a partir do total e da taxa quando falta a linha assinada`() = runTest {
        val texto = """
            Compra
            30/06/2026 22:57
            Preço
            R$ 315.641,52
            Taxa
            R$ 22,50
            Total Comprado
            R$ 1.477,50
            0,00468094
        """.trimIndent()
        val draft = extractor.extract(texto, EntrySource.IMAGE, hoje)!!
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals(2_250L, draft.feeCents)
    }

    @Test
    fun `reconhece bitcoin por numero com oito casas decimais sem simbolo`() = runTest {
        val texto = """
            Compra 30/06/2026
            R$ 1.500,00
            0,00468094
            R$ 315.641,52
        """.trimIndent()
        assertEquals(468_094L, extractor.extract(texto, EntrySource.IMAGE, hoje)!!.satoshis)
    }

    @Test
    fun `aceita rotulos alternativos de cotacao e taxa`() = runTest {
        val texto = """
            Compra 30/06/2026
            - R$ 1.500,00
            + BTC 0,00468094
            Cotação: R$ 315.641,52
            Tarifa: R$ 22,50
        """.trimIndent()
        val draft = extractor.extract(texto, EntrySource.IMAGE, hoje)!!
        assertEquals(31_564_152L, draft.unitPriceCents)
        assertEquals(2_250L, draft.feeCents)
    }

    @Test
    fun `interpreta linguagem natural com data relativa`() = runTest {
        val draft = extractor.extract(
            "comprei 500 reais de bitcoin ontem a 315.641,52",
            EntrySource.AUDIO,
            hoje,
        )!!
        assertEquals(TransactionType.BUY, draft.type)
        assertEquals(epoch(2026, 8, 28, 0, 0), draft.occurredAt)
        assertEquals(50_000L, draft.fiatAmountCents)
        assertEquals(31_564_152L, draft.unitPriceCents)
    }

    @Test
    fun `extracao parcial devolve o que conseguiu`() = runTest {
        val draft = extractor.extract("R$ 1.500,00", EntrySource.TEXT, hoje)!!
        assertEquals(150_000L, draft.fiatAmountCents)
        assertNull(draft.satoshis)
        assertNull(draft.occurredAt)
    }

    @Test
    fun `texto sem nenhum numero devolve null`() = runTest {
        assertNull(extractor.extract("Concluída", EntrySource.IMAGE, hoje))
        assertNull(extractor.extract("", EntrySource.IMAGE, hoje))
    }

    @Test
    fun `esta sempre disponivel`() = runTest {
        assertEquals(true, extractor.isAvailable())
    }
}
```

`core/src/test/kotlin/com/pablo/btcmedio/core/extract/ExtractorChainTest.kt`:

```kotlin
package com.pablo.btcmedio.core.extract

import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.EntrySource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExtractorChainTest {

    private val hoje = LocalDate.of(2026, 8, 29)

    private class Fake(
        override val name: String,
        private val available: Boolean,
        private val result: TransactionDraft?,
        val calls: MutableList<String> = mutableListOf(),
    ) : DraftExtractor {
        override suspend fun isAvailable() = available
        override suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft? {
            calls.add(text)
            return result
        }
    }

    private val completo = TransactionDraft(
        occurredAt = 1L, fiatAmountCents = 150_000, satoshis = 468_094, unitPriceCents = 31_564_152,
    )
    private val parcial = TransactionDraft(fiatAmountCents = 150_000)

    @Test
    fun `para no primeiro extrator que devolve rascunho completo`() = runTest {
        val segundo = Fake("nano", true, completo)
        val chain = ExtractorChain(listOf(Fake("regras", true, completo), segundo))
        val draft = chain.extract("qualquer", EntrySource.IMAGE, hoje)
        assertEquals(150_000L, draft.fiatAmountCents)
        assertTrue("o segundo extrator não deveria ser chamado", segundo.calls.isEmpty())
    }

    @Test
    fun `avanca para o proximo quando o rascunho e parcial`() = runTest {
        val segundo = Fake("nano", true, completo)
        val chain = ExtractorChain(listOf(Fake("regras", true, parcial), segundo))
        val draft = chain.extract("qualquer", EntrySource.IMAGE, hoje)
        assertEquals(468_094L, draft.satoshis)
        assertEquals(1, segundo.calls.size)
    }

    @Test
    fun `pula extrator indisponivel`() = runTest {
        val indisponivel = Fake("nano", false, completo)
        val chain = ExtractorChain(listOf(Fake("regras", true, parcial), indisponivel))
        val draft = chain.extract("qualquer", EntrySource.IMAGE, hoje)
        assertEquals(150_000L, draft.fiatAmountCents)
        assertNull(draft.satoshis)
        assertTrue(indisponivel.calls.isEmpty())
    }

    @Test
    fun `mantem o melhor rascunho parcial quando nenhum extrator completa`() = runTest {
        val chain = ExtractorChain(listOf(Fake("a", true, parcial), Fake("b", true, null)))
        val draft = chain.extract("qualquer", EntrySource.TEXT, hoje)
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals("qualquer", draft.rawText)
    }

    @Test
    fun `extrator que lanca excecao nao derruba a cadeia`() = runTest {
        val explosivo = object : DraftExtractor {
            override val name = "explosivo"
            override suspend fun isAvailable() = true
            override suspend fun extract(text: String, source: EntrySource, today: LocalDate) =
                throw IllegalStateException("modelo indisponível")
        }
        val chain = ExtractorChain(listOf(explosivo, Fake("regras", true, completo)))
        assertEquals(468_094L, chain.extract("qualquer", EntrySource.IMAGE, hoje).satoshis)
    }

    @Test
    fun `cadeia vazia devolve rascunho so com o texto bruto`() = runTest {
        val draft = ExtractorChain(emptyList()).extract("nada aqui", EntrySource.TEXT, hoje)
        assertNull(draft.fiatAmountCents)
        assertEquals("nada aqui", draft.rawText)
        assertEquals(EntrySource.TEXT, draft.source)
    }
}
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew :core:test --tests "*extract*"`
Expected: FAIL — `Unresolved reference: DraftExtractor`

- [ ] **Step 3: Implementar DraftExtractor e ExtractorChain**

`core/src/main/kotlin/com/pablo/btcmedio/core/extract/DraftExtractor.kt`:

```kotlin
package com.pablo.btcmedio.core.extract

import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.EntrySource
import java.time.LocalDate

/**
 * Transforma texto em rascunho de transação.
 *
 * A interface vive em `:core` para que o extrator baseado em Gemini Nano —
 * que precisa do Android — possa ser substituído por um duplo nos testes.
 */
interface DraftExtractor {
    val name: String

    suspend fun isAvailable(): Boolean

    /** `null` quando não conseguiu extrair nada de aproveitável. */
    suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft?
}

/**
 * Executa os extratores em ordem, parando no primeiro rascunho completo.
 *
 * Nunca lança e nunca devolve `null`: no pior caso entrega um rascunho vazio
 * carregando o texto original, para que o usuário veja o que foi reconhecido
 * e complete à mão.
 */
class ExtractorChain(private val extractors: List<DraftExtractor>) {

    suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft {
        var best: TransactionDraft? = null

        for (extractor in extractors) {
            val candidate = try {
                if (!extractor.isAvailable()) continue
                extractor.extract(text, source, today)
            } catch (e: Exception) {
                continue
            } ?: continue

            best = merge(best, candidate)
            if (best.isComplete()) break
        }

        return (best ?: TransactionDraft()).copy(source = source, rawText = text)
    }

    /** Campos já preenchidos vencem: o primeiro extrator tem prioridade. */
    private fun merge(base: TransactionDraft?, next: TransactionDraft): TransactionDraft {
        if (base == null) return next
        return base.copy(
            occurredAt = base.occurredAt ?: next.occurredAt,
            fiatAmountCents = base.fiatAmountCents ?: next.fiatAmountCents,
            feeCents = base.feeCents ?: next.feeCents,
            satoshis = base.satoshis ?: next.satoshis,
            unitPriceCents = base.unitPriceCents ?: next.unitPriceCents,
            note = base.note ?: next.note,
        )
    }
}
```

- [ ] **Step 4: Implementar ReceiptRuleExtractor**

`core/src/main/kotlin/com/pablo/btcmedio/core/extract/ReceiptRuleExtractor.kt`:

```kotlin
package com.pablo.btcmedio.core.extract

import com.pablo.btcmedio.core.draft.DraftCompleter
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.core.parse.BrazilianDateParser
import com.pablo.btcmedio.core.parse.BrazilianNumberParser
import java.time.LocalDate
import java.time.ZoneId

/**
 * Extração determinística por rótulos e heurísticas.
 *
 * É o caminho principal, não o plano B: funciona em qualquer aparelho, é
 * reproduzível e não alucina. O Gemini Nano só entra no que sobrar.
 */
class ReceiptRuleExtractor(
    private val zone: ZoneId = ZoneId.systemDefault(),
) : DraftExtractor {

    override val name = "regras"

    override suspend fun isAvailable() = true

    private val sellWords = listOf("venda", "vendi", "vender", "vendido", "sell", "sold")
    private val buyWords = listOf("compra", "comprei", "comprar", "comprado", "buy", "bought")

    private val priceLabels = listOf("preço", "preco", "cotação", "cotacao", "price", "valor unitário", "valor unitario")
    private val feeLabels = listOf("taxa", "tarifa", "fee", "comissão", "comissao")
    private val totalLabels = listOf("total comprado", "total vendido", "total negociado", "valor total", "total")

    override suspend fun extract(text: String, source: EntrySource, today: LocalDate): TransactionDraft? {
        if (text.isBlank()) return null
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.none { BrazilianNumberParser.NUMBER_PATTERN.containsMatchIn(it) }) return null

        val lower = text.lowercase()
        val type = when {
            sellWords.any { lower.contains(it) } -> TransactionType.SELL
            buyWords.any { lower.contains(it) } -> TransactionType.BUY
            else -> TransactionType.BUY
        }

        val occurredAt = BrazilianDateParser.parse(text, today)
            ?.atZone(zone)?.toInstant()?.toEpochMilli()

        val unitPriceCents = labeledCents(lines, priceLabels)
        val feeCents = labeledCents(lines, feeLabels)
        val totalCents = labeledCents(lines, totalLabels)
        val satoshis = findSatoshis(lines)
        val signedFiat = findSignedFiat(lines)

        val fiatAmountCents = signedFiat
            ?: totalCents?.let { total ->
                when (type) {
                    TransactionType.BUY -> total + (feeCents ?: 0L)
                    TransactionType.SELL -> total - (feeCents ?: 0L)
                }
            }
            ?: fallbackFiat(lines, unitPriceCents, feeCents, totalCents)

        var draft = TransactionDraft(
            type = type,
            occurredAt = occurredAt,
            fiatAmountCents = fiatAmountCents,
            feeCents = feeCents,
            satoshis = satoshis,
            unitPriceCents = unitPriceCents,
            source = source,
            rawText = text,
        )

        // Se sobrou exatamente um número dedutível, preenche.
        if (!draft.isComplete()) draft = DraftCompleter.complete(draft)

        val achouAlgo = draft.fiatAmountCents != null || draft.satoshis != null ||
            draft.unitPriceCents != null || draft.occurredAt != null
        return if (achouAlgo) draft else null
    }

    /** Valor que acompanha um rótulo, na mesma linha ou na linha seguinte. */
    private fun labeledCents(lines: List<String>, labels: List<String>): Long? {
        lines.forEachIndexed { index, line ->
            val lower = line.lowercase()
            val label = labels.firstOrNull { lower.contains(it) } ?: return@forEachIndexed
            val afterLabel = line.substring(lower.indexOf(label) + label.length)
            BrazilianNumberParser.NUMBER_PATTERN.find(afterLabel)?.let {
                return BrazilianNumberParser.parseCents(it.value)
            }
            lines.getOrNull(index + 1)?.let { next ->
                if (labels.none { next.lowercase().contains(it) }) {
                    BrazilianNumberParser.NUMBER_PATTERN.find(next)?.let {
                        return BrazilianNumberParser.parseCents(it.value)
                    }
                }
            }
        }
        return null
    }

    /**
     * Quantidade de Bitcoin: linha com ₿ ou BTC, ou — porque o OCR erra o
     * símbolo com frequência — um número com 6 a 8 casas decimais.
     */
    private fun findSatoshis(lines: List<String>): Long? {
        lines.firstOrNull { it.contains("₿") || it.contains("BTC", ignoreCase = true) }?.let { line ->
            BrazilianNumberParser.NUMBER_PATTERN.find(line)?.let {
                return BrazilianNumberParser.parseSatoshis(it.value)
            }
        }
        val eightDecimals = Regex("""\b\d+[.,]\d{6,8}\b""")
        for (line in lines) {
            if (line.contains("R$")) continue
            eightDecimals.find(line)?.let {
                return BrazilianNumberParser.parseSatoshis(it.value)
            }
        }
        return null
    }

    /** A linha com sinal explícito e `R$` é o movimento de caixa. */
    private fun findSignedFiat(lines: List<String>): Long? {
        val signed = Regex("""^[-+−]\s*R\$""")
        return lines.firstOrNull { signed.containsMatchIn(it) }
            ?.let { line -> BrazilianNumberParser.NUMBER_PATTERN.find(line)?.value }
            ?.let { BrazilianNumberParser.parseCents(it) }
    }

    /**
     * Sem rótulo nem sinal: o primeiro valor em reais que não seja a cotação,
     * a taxa nem o total. Cobre linguagem natural ("comprei 500 reais").
     */
    private fun fallbackFiat(
        lines: List<String>,
        priceCents: Long?,
        feeCents: Long?,
        totalCents: Long?,
    ): Long? {
        val excluded = setOfNotNull(priceCents, feeCents, totalCents)
        val moneyWords = Regex("""(?:R\$|reais?|real)""", RegexOption.IGNORE_CASE)
        for (line in lines) {
            if (!moneyWords.containsMatchIn(line)) continue
            for (match in BrazilianNumberParser.NUMBER_PATTERN.findAll(line)) {
                val cents = BrazilianNumberParser.parseCents(match.value) ?: continue
                if (cents in excluded) continue
                return cents
            }
        }
        return null
    }
}
```

- [ ] **Step 5: Rodar os testes e confirmar que passam**

Run: `./gradlew :core:test`
Expected: PASS — toda a suíte de `:core` verde

- [ ] **Step 6: Rodar a suíte inteira e verificar o tempo**

Run: `./gradlew :core:test --rerun-tasks`
Expected: PASS, execução em poucos segundos — é a garantia de que `:core` não arrastou dependência Android

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat(core): cadeia de extração e extrator de comprovantes por regras"
```
