# Tarefas 7–11 — Módulo `:app`, dados e interface

Ver `2026-08-29-app-bitcoin-preco-medio.md` para objetivo, arquitetura e **Global Constraints**.

---

## Task 7: Módulo `:app`, manifesto sem rede e verifyNoInternetPermission

Esta tarefa entrega um APK que compila, instala e abre — e cujo build **falha** se qualquer permissão de rede entrar no manifesto mergeado.

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/MainActivity.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/theme/Theme.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`
- Create: `local.properties` (se ausente)

**Interfaces:**
- Consumes: `:core` (Tasks 1–6)
- Produces: `MainActivity`, tema `BtcMedioTheme`, tarefa Gradle `verifyNoInternetPermission<Variant>`

- [ ] **Step 1: Garantir o local.properties**

```bash
test -f local.properties || echo "sdk.dir=C\\:\\\\Users\\\\pablo\\\\AppData\\\\Local\\\\Android\\\\Sdk" > local.properties
```

- [ ] **Step 2: Escrever o build do :app com a verificação de permissões**

`app/build.gradle.kts`:

```kotlin
import com.android.build.api.artifact.SingleArtifact

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pablo.btcmedio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pablo.btcmedio"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    sourceSets["main"].java.srcDirs("src/main/kotlin")
    sourceSets["test"].java.srcDirs("src/test/kotlin")
    sourceSets["androidTest"].java.srcDirs("src/androidTest/kotlin")

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

/**
 * O app promete funcionar offline. Bibliotecas do ML Kit arrastam
 * `play-services-basement`, que declara INTERNET e ACCESS_NETWORK_STATE.
 * O manifesto remove essas permissões; esta tarefa confirma que a remoção
 * sobreviveu ao merge — transformando a promessa em invariante do build.
 */
abstract class VerifyNoInternetPermission : DefaultTask() {

    @get:org.gradle.api.tasks.InputFile
    abstract val mergedManifest: org.gradle.api.file.RegularFileProperty

    @TaskAction
    fun verify() {
        val text = mergedManifest.get().asFile.readText()
        val forbidden = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
        )
        val found = forbidden.filter { text.contains(it) }
        if (found.isNotEmpty()) {
            throw GradleException(
                "O manifesto mergeado declara permissões de rede proibidas: $found\n" +
                    "Arquivo: ${mergedManifest.get().asFile.absolutePath}"
            )
        }
        logger.lifecycle("verifyNoInternetPermission: nenhuma permissão de rede no manifesto mergeado.")
    }
}

androidComponents {
    onVariants { variant ->
        val capitalized = variant.name.replaceFirstChar { it.uppercase() }
        val task = tasks.register<VerifyNoInternetPermission>("verifyNoInternetPermission$capitalized") {
            group = "verification"
            description = "Falha se alguma permissão de rede entrar no manifesto de ${variant.name}."
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
        }
        tasks.named("check").configure { dependsOn(task) }
    }
}
```

- [ ] **Step 3: Escrever o manifesto que remove as permissões de rede**

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <!--
      O app funciona inteiramente offline. As bibliotecas do ML Kit trazem
      play-services-basement, que declara estas permissões no manifesto delas.
      Sem a remoção explícita, elas entrariam no APK instalado.
    -->
    <uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" tools:node="remove" />

    <application
        android:name=".BtcMedioApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.BtcMedio">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.BtcMedio">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Preço Médio BTC</string>
</resources>
```

`app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.BtcMedio" parent="android:Theme.Material.NoActionBar" />
</resources>
```

`app/src/main/res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content />
```

`app/src/main/res/xml/data_extraction_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup />
    <device-transfer />
</data-extraction-rules>
```

- [ ] **Step 4: Escrever o tema, o Application e a MainActivity mínimos**

`app/src/main/kotlin/com/pablo/btcmedio/BtcMedioApp.kt`:

```kotlin
package com.pablo.btcmedio

import android.app.Application

class BtcMedioApp : Application()
```

`app/src/main/kotlin/com/pablo/btcmedio/ui/theme/Theme.kt`:

```kotlin
package com.pablo.btcmedio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFFF7931A),          // laranja do Bitcoin
    onPrimary = Color.White,
    secondary = Color(0xFF2E7D32),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF7931A),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFF66BB6A),
    error = Color(0xFFF2B8B5),
)

@Composable
fun BtcMedioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

`app/src/main/kotlin/com/pablo/btcmedio/ui/MainActivity.kt`:

```kotlin
package com.pablo.btcmedio.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.pablo.btcmedio.ui.theme.BtcMedioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BtcMedioTheme {
                Surface { Text("Preço Médio BTC") }
            }
        }
    }
}
```

Ícones: gere `ic_launcher` com o Asset Studio do Android Studio, ou crie `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` apontando para um `adaptive-icon` simples. Sem os ícones o build falha na referência do manifesto.

- [ ] **Step 5: Compilar e rodar a verificação de permissões**

Run: `./gradlew :app:assembleDebug :app:verifyNoInternetPermissionDebug`
Expected: BUILD SUCCESSFUL, com a linha `verifyNoInternetPermission: nenhuma permissão de rede no manifesto mergeado.`

- [ ] **Step 6: Provar que a verificação realmente pega uma regressão**

Remova temporariamente a linha `<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />` e adicione `<uses-permission android:name="android.permission.INTERNET" />`.

Run: `./gradlew :app:verifyNoInternetPermissionDebug`
Expected: FAIL com `O manifesto mergeado declara permissões de rede proibidas: [android.permission.INTERNET]`

Depois **restaure o manifesto** e rode de novo:

Run: `./gradlew :app:verifyNoInternetPermissionDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat(app): módulo Android com remoção verificada das permissões de rede"
```

---

## Task 8: Room e repositório

**Files:**
- Create: `app/src/main/kotlin/com/pablo/btcmedio/data/TransactionEntity.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/data/TransactionDao.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/data/AppDatabase.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/data/TransactionRepository.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/AppContainer.kt`
- Modify: `app/src/main/kotlin/com/pablo/btcmedio/BtcMedioApp.kt`
- Test: `app/src/androidTest/kotlin/com/pablo/btcmedio/data/TransactionDaoTest.kt`
- Test: `app/src/androidTest/kotlin/com/pablo/btcmedio/data/TransactionRepositoryTest.kt`

**Interfaces:**
- Consumes: `Transaction`, `PortfolioCalculator`, `SequenceValidator`, `SequenceCheck` (Tasks 1–2)
- Produces:
  - `@Entity TransactionEntity` + `fun TransactionEntity.toDomain(): Transaction` + `fun Transaction.toEntity(): TransactionEntity`
  - `interface TransactionDao` com `observeAll(): Flow<List<TransactionEntity>>`, `getAll(): List<TransactionEntity>`, `getById(id: String): TransactionEntity?`, `upsert(e: TransactionEntity)`, `deleteById(id: String)`
  - `abstract class AppDatabase : RoomDatabase()` com `fun transactionDao(): TransactionDao` e `companion object { fun build(context: Context): AppDatabase }`
  - `sealed interface SaveResult { data object Success; data class Oversold(availableSats: Long, requestedSats: Long) }`
  - `class TransactionRepository(dao)` com `val transactions: Flow<List<Transaction>>`, `val summary: Flow<PortfolioSummary>`, `suspend fun byId(id: String): Transaction?`, `suspend fun save(t: Transaction): SaveResult`, `suspend fun delete(id: String)`
  - `class AppContainer(context: Context)` expondo `repository`

- [ ] **Step 1: Escrever os testes que falham**

`app/src/androidTest/kotlin/com/pablo/btcmedio/data/TransactionDaoTest.kt`:

```kotlin
package com.pablo.btcmedio.data

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
            ApplicationProvider.getApplicationContext(),
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
```

`app/src/androidTest/kotlin/com/pablo/btcmedio/data/TransactionRepositoryTest.kt`:

```kotlin
package com.pablo.btcmedio.data

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
            ApplicationProvider.getApplicationContext(),
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
        assertEquals(SaveResult.Success, repo.save(tx("v1", TransactionType.SELL, 2_000L, 7_000_000, 50_000_000)))

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
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`
Expected: FAIL — `Unresolved reference: AppDatabase`

- [ ] **Step 3: Implementar a entidade e os mapeadores**

`app/src/main/kotlin/com/pablo/btcmedio/data/TransactionEntity.kt`:

```kotlin
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
```

- [ ] **Step 4: Implementar DAO e banco**

`app/src/main/kotlin/com/pablo/btcmedio/data/TransactionDao.kt`:

```kotlin
package com.pablo.btcmedio.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt ASC, id ASC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Upsert
    suspend fun upsert(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

`app/src/main/kotlin/com/pablo/btcmedio/data/AppDatabase.kt`:

```kotlin
package com.pablo.btcmedio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "btcmedio.db").build()
    }
}
```

- [ ] **Step 5: Implementar o repositório**

`app/src/main/kotlin/com/pablo/btcmedio/data/TransactionRepository.kt`:

```kotlin
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
```

- [ ] **Step 6: Implementar o AppContainer**

`app/src/main/kotlin/com/pablo/btcmedio/AppContainer.kt`:

```kotlin
package com.pablo.btcmedio

import android.content.Context
import com.pablo.btcmedio.data.AppDatabase
import com.pablo.btcmedio.data.TransactionRepository

/**
 * Injeção de dependência manual. Com o tamanho deste app, um container
 * explícito é mais legível e mais rápido de compilar que um framework.
 */
class AppContainer(context: Context) {
    private val database by lazy { AppDatabase.build(context) }
    val repository by lazy { TransactionRepository(database.transactionDao()) }
}
```

`app/src/main/kotlin/com/pablo/btcmedio/BtcMedioApp.kt`:

```kotlin
package com.pablo.btcmedio

import android.app.Application

class BtcMedioApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 7: Rodar os testes instrumentados**

Com um emulador ou aparelho conectado (`adb devices` deve listar um dispositivo):

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS — 9 testes verdes

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat(app): persistência Room e repositório com validação de sequência"
```

---

## Task 9: Tema, navegação e tela Resumo

**Files:**
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/format/Formatters.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/summary/SummaryViewModel.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/summary/SummaryScreen.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/NavGraph.kt`
- Modify: `app/src/main/kotlin/com/pablo/btcmedio/ui/MainActivity.kt`
- Test: `app/src/test/kotlin/com/pablo/btcmedio/ui/format/FormattersTest.kt`

**Interfaces:**
- Consumes: `TransactionRepository`, `PortfolioSummary`, `Transaction` (Task 8)
- Produces:
  - `object Formatters { fun brl(cents: Long?): String; fun btc(sats: Long?): String; fun date(epochMillis: Long): String; fun dateTime(epochMillis: Long): String }`
  - `class SummaryViewModel(repo)` com `val state: StateFlow<SummaryState>` e `companion object { fun factory(repo): ViewModelProvider.Factory }`
  - `data class SummaryState(summary: PortfolioSummary, recent: List<Transaction>)`
  - `@Composable fun SummaryScreen(state, onAdd, onMic, onOpenList, onOpenSettings, onOpenTransaction)`
  - `object Routes { const val SUMMARY; const val LIST; const val SETTINGS; fun form(id: String?): String; const val FORM_PATTERN; const val ARG_ID }`

- [ ] **Step 1: Escrever o teste de formatação que falha**

`app/src/test/kotlin/com/pablo/btcmedio/ui/format/FormattersTest.kt`:

```kotlin
package com.pablo.btcmedio.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    /** Espaço não separável: é o que o ICU usa entre "R$" e o número. */
    private fun norm(s: String) = s.replace(' ', ' ')

    @Test
    fun formata_reais() {
        assertEquals("R$ 1.500,00", norm(Formatters.brl(150_000)))
        assertEquals("R$ 342.686,32", norm(Formatters.brl(34_268_632)))
        assertEquals("R$ 0,00", norm(Formatters.brl(0)))
        assertEquals("-R$ 20.000,00", norm(Formatters.brl(-2_000_000)))
    }

    @Test
    fun valor_nulo_vira_travessao() {
        assertEquals("—", Formatters.brl(null))
        assertEquals("—", Formatters.btc(null))
    }

    @Test
    fun formata_bitcoin_com_oito_casas() {
        assertEquals("0,04260456", Formatters.btc(4_260_456))
        assertEquals("0,00468094", Formatters.btc(468_094))
        assertEquals("1,00000000", Formatters.btc(100_000_000))
        assertEquals("0,00000000", Formatters.btc(0))
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

Run: `./gradlew :app:testDebugUnitTest`
Expected: FAIL — `Unresolved reference: Formatters`

- [ ] **Step 3: Implementar os formatadores**

`app/src/main/kotlin/com/pablo/btcmedio/ui/format/Formatters.kt`:

```kotlin
package com.pablo.btcmedio.ui.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {

    private val PT_BR = Locale.forLanguageTag("pt-BR")
    private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR)
    private val DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PT_BR)

    fun brl(cents: Long?): String {
        if (cents == null) return "—"
        val format = NumberFormat.getCurrencyInstance(PT_BR)
        return format.format(BigDecimal.valueOf(cents, 2))
    }

    fun btc(sats: Long?): String {
        if (sats == null) return "—"
        val format = NumberFormat.getNumberInstance(PT_BR).apply {
            minimumFractionDigits = 8
            maximumFractionDigits = 8
        }
        return format.format(BigDecimal.valueOf(sats, 8))
    }

    fun date(epochMillis: Long): String =
        DATE.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    fun dateTime(epochMillis: Long): String =
        DATE_TIME.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}
```

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Implementar o ViewModel do resumo**

`app/src/main/kotlin/com/pablo/btcmedio/ui/summary/SummaryViewModel.kt`:

```kotlin
package com.pablo.btcmedio.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.model.PortfolioSummary
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SummaryState(
    val summary: PortfolioSummary = PortfolioSummary.EMPTY,
    val recent: List<Transaction> = emptyList(),
)

class SummaryViewModel(repository: TransactionRepository) : ViewModel() {

    val state: StateFlow<SummaryState> = combine(
        repository.summary,
        repository.transactions.map { it.take(5) },
    ) { summary, recent -> SummaryState(summary, recent) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryState())

    companion object {
        fun factory(repository: TransactionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SummaryViewModel(repository) as T
        }
    }
}
```

- [ ] **Step 6: Implementar a tela Resumo**

`app/src/main/kotlin/com/pablo/btcmedio/ui/summary/SummaryScreen.kt`:

```kotlin
package com.pablo.btcmedio.ui.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.ui.format.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    state: SummaryState,
    onAdd: () -> Unit,
    onMic: () -> Unit,
    onOpenList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTransaction: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preço Médio BTC") },
                actions = {
                    IconButton(onClick = onMic) {
                        Icon(Icons.Default.Mic, contentDescription = "Registrar por voz")
                    }
                    IconButton(onClick = onOpenList) {
                        Icon(Icons.Default.List, contentDescription = "Todas as transações")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar transação")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HighlightCard(
                    label = "Preço médio",
                    value = Formatters.brl(state.summary.averagePriceCents),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Saldo", Formatters.btc(state.summary.balanceSats) + " BTC", Modifier.weight(1f))
                    StatCard("Custo da posição", Formatters.brl(state.summary.costCents), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Total comprado", Formatters.brl(state.summary.totalBoughtCents), Modifier.weight(1f))
                    StatCard("Total vendido", Formatters.brl(state.summary.totalSoldCents), Modifier.weight(1f))
                }
            }
            item {
                StatCard(
                    label = "Resultado realizado",
                    value = Formatters.brl(state.summary.realizedPnlCents),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.recent.isNotEmpty()) {
                item {
                    Text(
                        "Últimas transações",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(state.recent, key = { it.id }) { t ->
                    TransactionRow(t, onClick = { onOpenTransaction(t.id) })
                    HorizontalDivider()
                }
            } else {
                item {
                    Text(
                        "Nenhuma transação ainda. Toque em + para registrar a primeira, " +
                            "ou compartilhe um comprovante da corretora com este app.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label: $value" }) {
        Column(Modifier.padding(20.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.semantics { contentDescription = "$label: $value" }) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                if (transaction.type == TransactionType.BUY) "Compra" else "Venda",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(Formatters.dateTime(transaction.occurredAt), style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Formatters.brl(transaction.fiatAmountCents), style = MaterialTheme.typography.titleSmall)
            Text("${Formatters.btc(transaction.satoshis)} BTC", style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

- [ ] **Step 7: Implementar a navegação**

`app/src/main/kotlin/com/pablo/btcmedio/ui/NavGraph.kt`:

```kotlin
package com.pablo.btcmedio.ui

object Routes {
    const val SUMMARY = "resumo"
    const val LIST = "transacoes"
    const val SETTINGS = "ajustes"
    const val ARG_ID = "id"
    const val FORM_PATTERN = "form?$ARG_ID={$ARG_ID}"

    fun form(id: String? = null): String = if (id == null) "form" else "form?$ARG_ID=$id"
}
```

`app/src/main/kotlin/com/pablo/btcmedio/ui/MainActivity.kt` (substitui o conteúdo do Step 4 da Task 7):

```kotlin
package com.pablo.btcmedio.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pablo.btcmedio.BtcMedioApp
import com.pablo.btcmedio.ui.summary.SummaryScreen
import com.pablo.btcmedio.ui.summary.SummaryViewModel
import com.pablo.btcmedio.ui.theme.BtcMedioTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BtcMedioApp).container

        setContent {
            BtcMedioTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Routes.SUMMARY) {
                    composable(Routes.SUMMARY) {
                        val vm: SummaryViewModel =
                            viewModel(factory = SummaryViewModel.factory(container.repository))
                        val state by vm.state.collectAsStateWithLifecycle()
                        SummaryScreen(
                            state = state,
                            onAdd = { navController.navigate(Routes.form()) },
                            onMic = { /* Task 14 */ },
                            onOpenList = { navController.navigate(Routes.LIST) },
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenTransaction = { id -> navController.navigate(Routes.form(id)) },
                        )
                    }
                }
            }
        }
    }
}
```

Adicione a dependência `androidx.lifecycle:lifecycle-runtime-compose` ao `libs.versions.toml` e ao `app/build.gradle.kts` para `collectAsStateWithLifecycle`:

```toml
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
```

```kotlin
implementation(libs.androidx.lifecycle.runtime.compose)
```

- [ ] **Step 8: Compilar e conferir a verificação de permissões**

Run: `./gradlew :app:assembleDebug :app:check`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "feat(app): tela de resumo com preço médio, saldo e totais"
```

---

## Task 10: Formulário de transação

O composable central do app: cria, edita e serve de tela de confirmação para as três fontes automáticas.

**Files:**
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/form/TransactionFormViewModel.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/form/TransactionFormScreen.kt`
- Modify: `app/src/main/kotlin/com/pablo/btcmedio/ui/MainActivity.kt`
- Test: `app/src/androidTest/kotlin/com/pablo/btcmedio/ui/form/TransactionFormScreenTest.kt`

**Interfaces:**
- Consumes: `TransactionDraft`, `DraftCompleter`, `DraftValidator`, `DraftIssue`, `DraftField`, `Severity` (Task 5); `TransactionRepository`, `SaveResult` (Task 8); `Formatters` (Task 9)
- Produces:
  - `data class FormState(draft: TransactionDraft, fiatText: String, feeText: String, satsText: String, priceText: String, issues: List<DraftIssue>, saveError: String?, saved: Boolean, isEditing: Boolean, rawText: String?)`
  - `class TransactionFormViewModel(repo, transactionId: String?, initialDraft: TransactionDraft?)` com `state: StateFlow<FormState>`, `fun onTypeChange`, `onDateChange(epochMillis: Long)`, `onFiatChange(String)`, `onFeeChange(String)`, `onSatsChange(String)`, `onPriceChange(String)`, `onNoteChange(String)`, `fun completeMissing()`, `fun save()`, `fun delete()`
  - `@Composable fun TransactionFormScreen(state, actions..., onDone: () -> Unit)`

- [ ] **Step 1: Escrever o teste de UI que falha**

`app/src/androidTest/kotlin/com/pablo/btcmedio/ui/form/TransactionFormScreenTest.kt`:

```kotlin
package com.pablo.btcmedio.ui.form

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.ui.theme.BtcMedioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionFormScreenTest {

    @get:Rule val rule = createComposeRule()

    private val comprovante = TransactionDraft(
        type = TransactionType.BUY,
        occurredAt = 1_782_000_000_000L,
        fiatAmountCents = 150_000,
        feeCents = 2_250,
        satoshis = 468_094,
        unitPriceCents = 31_564_152,
        rawText = "Detalhes da transação\n- R$ 1.500,00",
    )

    private fun render(
        state: FormState,
        actions: FormActions = FormActions(),
    ) = rule.setContent {
        BtcMedioTheme { TransactionFormScreen(state = state, actions = actions, onDone = {}) }
    }

    @Test
    fun exibe_os_valores_pre_preenchidos() {
        render(FormState.from(comprovante))
        rule.onNodeWithText("1.500,00").assertIsDisplayed()
        rule.onNodeWithText("22,50").assertIsDisplayed()
        rule.onNodeWithText("0,00468094").assertIsDisplayed()
        rule.onNodeWithText("315.641,52").assertIsDisplayed()
    }

    @Test
    fun exibe_o_texto_reconhecido_quando_ha_origem_automatica() {
        render(FormState.from(comprovante))
        rule.onNodeWithText("Texto reconhecido").performClick()
        rule.onNodeWithText("Detalhes da transação\n- R$ 1.500,00").assertIsDisplayed()
    }

    @Test
    fun botao_salvar_desabilitado_quando_faltam_campos() {
        render(FormState.from(TransactionDraft()))
        rule.onNodeWithText("Salvar").assertIsNotEnabled()
    }

    @Test
    fun aviso_de_incoerencia_aparece() {
        render(FormState.from(comprovante.copy(satoshis = 600_000)))
        rule.onNodeWithText("Os números não fecham", substring = true).assertIsDisplayed()
    }

    @Test
    fun completar_campo_faltante_dispara_a_acao() {
        var chamou = false
        render(
            FormState.from(comprovante.copy(satoshis = null)),
            FormActions(onCompleteMissing = { chamou = true }),
        )
        rule.onNodeWithText("Completar campo faltante").performClick()
        assertTrue(chamou)
    }

    @Test
    fun digitar_valor_propaga_o_texto_bruto() {
        val digitados = mutableListOf<String>()
        render(FormState.from(TransactionDraft()), FormActions(onFiatChange = { digitados.add(it) }))
        rule.onNodeWithText("Valor em reais").performTextInput("1500")
        assertEquals("1500", digitados.last())
    }

    @Test
    fun alternar_para_venda_dispara_a_acao() {
        var tipo: TransactionType? = null
        render(FormState.from(comprovante), FormActions(onTypeChange = { tipo = it }))
        rule.onNodeWithText("Venda").performClick()
        assertEquals(TransactionType.SELL, tipo)
    }

    @Test
    fun erro_de_venda_acima_do_saldo_e_exibido() {
        render(FormState.from(comprovante).copy(saveError = "Saldo insuficiente: você tem 0,00100000 BTC."))
        rule.onNodeWithText("Saldo insuficiente", substring = true).assertIsDisplayed()
    }

    @Test
    fun botao_excluir_so_aparece_na_edicao() {
        render(FormState.from(comprovante))
        rule.onNodeWithText("Excluir").assertDoesNotExist()
        render(FormState.from(comprovante).copy(isEditing = true))
        rule.onNodeWithText("Excluir").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`
Expected: FAIL — `Unresolved reference: FormState`

- [ ] **Step 3: Implementar FormState, FormActions e o ViewModel**

`app/src/main/kotlin/com/pablo/btcmedio/ui/form/TransactionFormViewModel.kt`:

```kotlin
package com.pablo.btcmedio.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.draft.DraftCompleter
import com.pablo.btcmedio.core.draft.DraftIssue
import com.pablo.btcmedio.core.draft.DraftValidator
import com.pablo.btcmedio.core.draft.Severity
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.core.parse.BrazilianNumberParser
import com.pablo.btcmedio.data.SaveResult
import com.pablo.btcmedio.data.TransactionRepository
import com.pablo.btcmedio.ui.format.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * O estado guarda o texto digitado **e** o valor interpretado. Guardar só o
 * número quebraria a digitação (apagar a vírgula reformataria o campo);
 * guardar só o texto empurraria parsing para dentro dos composables.
 */
data class FormState(
    val draft: TransactionDraft = TransactionDraft(),
    val fiatText: String = "",
    val feeText: String = "",
    val satsText: String = "",
    val priceText: String = "",
    val noteText: String = "",
    val issues: List<DraftIssue> = emptyList(),
    val saveError: String? = null,
    val saved: Boolean = false,
    val isEditing: Boolean = false,
    val canComplete: Boolean = false,
) {
    val canSave: Boolean get() = issues.none { it.severity == Severity.ERROR }

    companion object {
        fun from(draft: TransactionDraft, isEditing: Boolean = false): FormState = FormState(
            draft = draft,
            fiatText = draft.fiatAmountCents?.let { plainCents(it) } ?: "",
            feeText = draft.feeCents?.let { plainCents(it) } ?: "",
            satsText = draft.satoshis?.let { plainSats(it) } ?: "",
            priceText = draft.unitPriceCents?.let { plainCents(it) } ?: "",
            noteText = draft.note.orEmpty(),
            issues = DraftValidator.validate(draft),
            isEditing = isEditing,
            canComplete = DraftCompleter.missingField(draft) != null,
        )

        private fun plainCents(cents: Long) = Formatters.brl(cents)
            .replace("R$", "").replace(' ', ' ').trim()

        private fun plainSats(sats: Long) = Formatters.btc(sats)
    }
}

/** Callbacks do formulário, agrupados para manter o composable testável sem ViewModel. */
data class FormActions(
    val onTypeChange: (TransactionType) -> Unit = {},
    val onDateChange: (Long) -> Unit = {},
    val onFiatChange: (String) -> Unit = {},
    val onFeeChange: (String) -> Unit = {},
    val onSatsChange: (String) -> Unit = {},
    val onPriceChange: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onCompleteMissing: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

class TransactionFormViewModel(
    private val repository: TransactionRepository,
    private val transactionId: String?,
    initialDraft: TransactionDraft?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        FormState.from(initialDraft ?: TransactionDraft(), isEditing = transactionId != null)
    )
    val state: StateFlow<FormState> = _state.asStateFlow()

    init {
        if (transactionId != null) {
            viewModelScope.launch {
                repository.byId(transactionId)?.let { existing ->
                    _state.value = FormState.from(TransactionDraft.from(existing), isEditing = true)
                }
            }
        }
    }

    fun onTypeChange(type: TransactionType) = mutate { it.copy(type = type) }
    fun onDateChange(epochMillis: Long) = mutate { it.copy(occurredAt = epochMillis) }

    fun onFiatChange(text: String) = mutateText(text, { s, t -> s.copy(fiatText = t) }) { d, v ->
        d.copy(fiatAmountCents = v?.let { BrazilianNumberParser.parseCents(it) })
    }

    fun onFeeChange(text: String) = mutateText(text, { s, t -> s.copy(feeText = t) }) { d, v ->
        d.copy(feeCents = v?.let { BrazilianNumberParser.parseCents(it) })
    }

    fun onSatsChange(text: String) = mutateText(text, { s, t -> s.copy(satsText = t) }) { d, v ->
        d.copy(satoshis = v?.let { BrazilianNumberParser.parseSatoshis(it) })
    }

    fun onPriceChange(text: String) = mutateText(text, { s, t -> s.copy(priceText = t) }) { d, v ->
        d.copy(unitPriceCents = v?.let { BrazilianNumberParser.parseCents(it) })
    }

    fun onNoteChange(text: String) {
        _state.update { it.copy(noteText = text, draft = it.draft.copy(note = text.ifBlank { null })) }
    }

    fun completeMissing() {
        val completed = DraftCompleter.complete(_state.value.draft)
        _state.value = FormState.from(completed, isEditing = _state.value.isEditing)
            .copy(noteText = _state.value.noteText)
    }

    fun save() {
        val draft = _state.value.draft
        if (!draft.isComplete()) return
        viewModelScope.launch {
            val transaction = draft.toTransaction(
                id = transactionId ?: UUID.randomUUID().toString(),
                now = System.currentTimeMillis(),
            )
            when (val result = repository.save(transaction)) {
                is SaveResult.Success -> _state.update { it.copy(saved = true, saveError = null) }
                is SaveResult.Oversold -> _state.update {
                    it.copy(
                        saveError = "Saldo insuficiente nessa data: você tinha " +
                            "${Formatters.btc(result.availableSats)} BTC e está vendendo " +
                            "${Formatters.btc(result.requestedSats)} BTC."
                    )
                }
            }
        }
    }

    fun delete() {
        val id = transactionId ?: return
        viewModelScope.launch {
            repository.delete(id)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun mutate(transform: (TransactionDraft) -> TransactionDraft) {
        _state.update { current ->
            val draft = transform(current.draft)
            current.copy(
                draft = draft,
                issues = DraftValidator.validate(draft),
                canComplete = DraftCompleter.missingField(draft) != null,
                saveError = null,
            )
        }
    }

    private fun mutateText(
        text: String,
        setText: (FormState, String) -> FormState,
        transform: (TransactionDraft, String?) -> TransactionDraft,
    ) {
        _state.update { current ->
            val draft = transform(current.draft, text.ifBlank { null })
            setText(current, text).copy(
                draft = draft,
                issues = DraftValidator.validate(draft),
                canComplete = DraftCompleter.missingField(draft) != null,
                saveError = null,
            )
        }
    }

    companion object {
        fun factory(
            repository: TransactionRepository,
            transactionId: String?,
            initialDraft: TransactionDraft?,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TransactionFormViewModel(repository, transactionId, initialDraft) as T
        }
    }
}
```

- [ ] **Step 4: Implementar a tela**

`app/src/main/kotlin/com/pablo/btcmedio/ui/form/TransactionFormScreen.kt`:

```kotlin
package com.pablo.btcmedio.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.pablo.btcmedio.core.draft.DraftField
import com.pablo.btcmedio.core.draft.Severity
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.ui.format.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    state: FormState,
    actions: FormActions,
    onDone: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar transação" else "Nova transação") },
                navigationIcon = { TextButton(onClick = onDone) { Text("Cancelar") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.draft.rawText?.let { RawTextPanel(it) }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.draft.type == TransactionType.BUY,
                    onClick = { actions.onTypeChange(TransactionType.BUY) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Compra") }
                SegmentedButton(
                    selected = state.draft.type == TransactionType.SELL,
                    onClick = { actions.onTypeChange(TransactionType.SELL) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Venda") }
            }

            DateField(state, actions)

            NumberField("Valor em reais", state.fiatText, actions.onFiatChange, state.issueFor(DraftField.FIAT))
            NumberField("Taxa", state.feeText, actions.onFeeChange, state.issueFor(DraftField.FEE))
            NumberField("Quantidade de Bitcoin", state.satsText, actions.onSatsChange, state.issueFor(DraftField.SATS))
            NumberField("Cotação", state.priceText, actions.onPriceChange, state.issueFor(DraftField.PRICE))

            OutlinedTextField(
                value = state.noteText,
                onValueChange = actions.onNoteChange,
                label = { Text("Observação") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.canComplete) {
                OutlinedButton(onClick = actions.onCompleteMissing, modifier = Modifier.fillMaxWidth()) {
                    Text("Completar campo faltante")
                }
            }

            state.saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = actions.onSave,
                    enabled = state.canSave,
                    modifier = Modifier.weight(1f),
                ) { Text("Salvar") }

                if (state.isEditing) {
                    OutlinedButton(onClick = actions.onDelete, modifier = Modifier.weight(1f)) {
                        Text("Excluir")
                    }
                }
            }
        }
    }
}

@Composable
private fun RawTextPanel(rawText: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            TextButton(onClick = { expanded = !expanded }) { Text("Texto reconhecido") }
            if (expanded) {
                Text(
                    rawText,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Visible,
                )
            }
        }
    }
}

@Composable
private fun DateField(state: FormState, actions: FormActions) {
    // O seletor de data do Material 3 é aberto pelo composable pai na
    // integração real; aqui o campo exibe o valor formatado.
    OutlinedTextField(
        value = state.draft.occurredAt?.let { Formatters.dateTime(it) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text("Data e hora") },
        isError = state.issueFor(DraftField.DATE)?.severity == Severity.ERROR,
        supportingText = { state.issueFor(DraftField.DATE)?.let { Text(it.message) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    issue: com.pablo.btcmedio.core.draft.DraftIssue?,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = issue?.severity == Severity.ERROR,
        supportingText = { issue?.let { Text(it.message) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun FormState.issueFor(field: DraftField) = issues.firstOrNull { it.field == field }
```

Nota sobre o seletor de data: ligue um `DatePickerDialog` + `TimePicker` do Material 3 ao `DateField`, chamando `actions.onDateChange(epochMillis)` na confirmação. Enquanto ele não existir, um novo rascunho manual precisa da data preenchida pelo chamador — inicialize `TransactionDraft(occurredAt = System.currentTimeMillis())` ao abrir o formulário vazio.

- [ ] **Step 5: Ligar a rota do formulário na MainActivity**

Adicione dentro do `NavHost` em `MainActivity.kt`:

```kotlin
composable(
    route = Routes.FORM_PATTERN,
    arguments = listOf(navArgument(Routes.ARG_ID) { nullable = true; defaultValue = null; type = NavType.StringType }),
) { entry ->
    val id = entry.arguments?.getString(Routes.ARG_ID)
    val vm: TransactionFormViewModel = viewModel(
        factory = TransactionFormViewModel.factory(
            repository = container.repository,
            transactionId = id,
            initialDraft = if (id == null) TransactionDraft(occurredAt = System.currentTimeMillis()) else null,
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) navController.popBackStack() }
    TransactionFormScreen(
        state = state,
        actions = FormActions(
            onTypeChange = vm::onTypeChange,
            onDateChange = vm::onDateChange,
            onFiatChange = vm::onFiatChange,
            onFeeChange = vm::onFeeChange,
            onSatsChange = vm::onSatsChange,
            onPriceChange = vm::onPriceChange,
            onNoteChange = vm::onNoteChange,
            onCompleteMissing = vm::completeMissing,
            onSave = vm::save,
            onDelete = vm::delete,
        ),
        onDone = { navController.popBackStack() },
    )
}
```

Importe `androidx.navigation.NavType`, `androidx.navigation.navArgument`, `androidx.compose.runtime.LaunchedEffect` e as classes de `ui.form`.

- [ ] **Step 6: Rodar os testes**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat(app): formulário de transação com dedução de campo e validação"
```

---

## Task 11: Lista de transações

**Files:**
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/list/TransactionListViewModel.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/list/TransactionListScreen.kt`
- Modify: `app/src/main/kotlin/com/pablo/btcmedio/ui/MainActivity.kt`

**Interfaces:**
- Consumes: `TransactionRepository` (Task 8), `TransactionRow`, `Formatters` (Task 9)
- Produces:
  - `class TransactionListViewModel(repo)` com `val months: StateFlow<List<MonthGroup>>`, `fun delete(id: String)`, `fun undoDelete()`
  - `data class MonthGroup(val label: String, val transactions: List<Transaction>)`
  - `@Composable fun TransactionListScreen(months, onBack, onOpen, onDelete, onUndo)`

- [ ] **Step 1: Implementar o ViewModel com desfazer**

`app/src/main/kotlin/com/pablo/btcmedio/ui/list/TransactionListViewModel.kt`:

```kotlin
package com.pablo.btcmedio.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MonthGroup(val label: String, val transactions: List<Transaction>)

class TransactionListViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val monthFormat =
        DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))

    /** Guarda a última exclusão para permitir desfazer sem tocar no banco. */
    private var lastDeleted: Transaction? = null

    val months: StateFlow<List<MonthGroup>> = repository.transactions
        .map { list ->
            list.groupBy { t ->
                monthFormat.format(Instant.ofEpochMilli(t.occurredAt).atZone(ZoneId.systemDefault()))
            }.map { (label, txs) ->
                MonthGroup(label.replaceFirstChar { it.uppercase() }, txs)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(transaction: Transaction) {
        lastDeleted = transaction
        viewModelScope.launch { repository.delete(transaction.id) }
    }

    fun undoDelete() {
        val restored = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repository.save(restored) }
    }

    companion object {
        fun factory(repository: TransactionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TransactionListViewModel(repository) as T
        }
    }
}
```

- [ ] **Step 2: Implementar a tela com deslizar para excluir**

`app/src/main/kotlin/com/pablo/btcmedio/ui/list/TransactionListScreen.kt`:

```kotlin
package com.pablo.btcmedio.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pablo.btcmedio.core.model.Transaction
import com.pablo.btcmedio.ui.summary.TransactionRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    months: List<MonthGroup>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (Transaction) -> Unit,
    onUndo: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transações") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            months.forEach { group ->
                item(key = "cabecalho-${group.label}") {
                    Text(
                        group.label,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(group.transactions, key = { it.id }) { transaction ->
                    SwipeRow(
                        transaction = transaction,
                        onOpen = { onOpen(transaction.id) },
                        onDelete = {
                            onDelete(transaction)
                            // O snackbar é disparado pelo LaunchedEffect abaixo.
                        },
                    )
                    HorizontalDivider()
                }
            }
            if (months.isEmpty()) {
                item {
                    Text(
                        "Nenhuma transação registrada.",
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(transaction: Transaction, onOpen: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) onDelete()
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Excluir", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
            TransactionRow(transaction, onClick = onOpen)
        }
    }
}
```

Ligue o snackbar de desfazer no chamador da tela (`MainActivity`), chamando `snackbarHostState.showSnackbar("Transação excluída", actionLabel = "Desfazer")` e invocando `vm.undoDelete()` quando o resultado for `SnackbarResult.ActionPerformed`.

- [ ] **Step 3: Ligar a rota da lista**

Adicione ao `NavHost`:

```kotlin
composable(Routes.LIST) {
    val vm: TransactionListViewModel =
        viewModel(factory = TransactionListViewModel.factory(container.repository))
    val months by vm.months.collectAsStateWithLifecycle()
    TransactionListScreen(
        months = months,
        onBack = { navController.popBackStack() },
        onOpen = { id -> navController.navigate(Routes.form(id)) },
        onDelete = { vm.delete(it) },
        onUndo = { vm.undoDelete() },
    )
}
```

- [ ] **Step 4: Verificar em aparelho**

Run: `./gradlew :app:installDebug`
Expected: instala; registre duas transações manualmente, confira que o preço médio bate com o cálculo manual, exclua uma e desfaça

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(app): lista de transações agrupada por mês com exclusão e desfazer"
```
