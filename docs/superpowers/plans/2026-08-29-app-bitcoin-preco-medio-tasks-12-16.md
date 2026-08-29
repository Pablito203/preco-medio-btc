# Tarefas 12–16 — Ingestão automática e ajustes

Ver `2026-08-29-app-bitcoin-preco-medio.md` para objetivo, arquitetura e **Global Constraints**.

---

## Task 12: OCR e recebimento de imagem compartilhada

**Files:**
- Modify: `app/build.gradle.kts` (dependência do ML Kit)
- Modify: `app/src/main/AndroidManifest.xml` (intent-filters de imagem)
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ingest/OcrTextReader.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ingest/IngestResult.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ingest/ShareReceiverActivity.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ingest/ShareViewModel.kt`
- Modify: `app/src/main/kotlin/com/pablo/btcmedio/AppContainer.kt`
- Test: `app/src/androidTest/kotlin/com/pablo/btcmedio/ingest/OcrTextReaderTest.kt`

**Interfaces:**
- Consumes: `ExtractorChain`, `ReceiptRuleExtractor`, `DraftExtractor` (Task 6); `TransactionDraft` (Task 5); `TransactionFormScreen`, `FormState`, `FormActions`, `TransactionFormViewModel` (Task 10); `AppContainer` (Task 8)
- Produces:
  - `class OcrTextReader(context: Context) { suspend fun read(uri: Uri): String? }`
  - `sealed interface IngestResult { data class Ready(val draft: TransactionDraft); data class Failed(val message: String) }`
  - `class ShareViewModel(chain, ocr)` com `val current: StateFlow<IngestResult?>`, `val remaining: StateFlow<List<IngestResult>>`, `val loading: StateFlow<Boolean>`, `fun loadImages(uris: List<Uri>)`, `fun loadText(text: String, source: EntrySource = EntrySource.TEXT)`, `fun advance(): Boolean`, `fun draftFor(result: IngestResult?): TransactionDraft`
  - `ShareReceiverActivity` — hospeda a confirmação e encerra ao fim da fila
  - `AppContainer.extractorChain: ExtractorChain`, `AppContainer.ocrTextReader: OcrTextReader`

- [ ] **Step 1: Adicionar a dependência do ML Kit**

Em `app/build.gradle.kts`, dentro de `dependencies`:

```kotlin
implementation(libs.mlkit.text.recognition)
```

O artefato `com.google.mlkit:text-recognition` traz o modelo **embarcado no APK** — não é a variante que baixa via Play Services. Isso adiciona alguns MB ao APK e é o que garante OCR sem rede em qualquer aparelho.

- [ ] **Step 2: Confirmar que a verificação de permissões continua passando**

Este é o momento de maior risco do projeto inteiro: o ML Kit arrasta `play-services-basement`, que declara `INTERNET`.

Run: `./gradlew :app:verifyNoInternetPermissionDebug`
Expected: BUILD SUCCESSFUL — a remoção via `tools:node="remove"` no manifesto neutraliza a injeção transitiva

Se falhar, o `tools:node="remove"` não está cobrindo a permissão listada na mensagem de erro: acrescente a linha correspondente ao manifesto e rode de novo.

- [ ] **Step 3: Escrever o teste do OCR**

`app/src/androidTest/kotlin/com/pablo/btcmedio/ingest/OcrTextReaderTest.kt`:

```kotlin
package com.pablo.btcmedio.ingest

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OcrTextReaderTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val reader = OcrTextReader(context)

    /** Gera uma imagem com texto real, para exercitar o OCR de verdade. */
    private fun bitmapComTexto(linhas: List<String>): Uri {
        val bitmap = Bitmap.createBitmap(900, 100 + linhas.size * 90, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 56f
                isAntiAlias = true
            }
            linhas.forEachIndexed { i, linha -> drawText(linha, 40f, 100f + i * 90f, paint) }
        }
        val file = File(context.cacheDir, "ocr-teste-${System.nanoTime()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun le_texto_de_um_comprovante_sintetico() = runTest {
        val uri = bitmapComTexto(listOf("Compra", "R\$ 1.500,00", "30/06/2026 22:57"))
        val texto = reader.read(uri)
        assertNotNull(texto)
        assertTrue("texto lido: $texto", texto!!.contains("1.500"))
        assertTrue("texto lido: $texto", texto.contains("30/06/2026"))
    }

    @Test
    fun imagem_em_branco_devolve_texto_vazio_ou_null() = runTest {
        val uri = bitmapComTexto(emptyList())
        val texto = reader.read(uri)
        assertTrue(texto == null || texto.isBlank())
    }

    @Test
    fun uri_inexistente_devolve_null_sem_lancar() = runTest {
        assertNull(reader.read(Uri.parse("file:///nao/existe.png")))
    }
}
```

- [ ] **Step 4: Rodar e confirmar que falha**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`
Expected: FAIL — `Unresolved reference: OcrTextReader`

- [ ] **Step 5: Implementar o OcrTextReader**

`app/src/main/kotlin/com/pablo/btcmedio/ingest/OcrTextReader.kt`:

```kotlin
package com.pablo.btcmedio.ingest

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Reconhecimento de texto do ML Kit com modelo embarcado no APK.
 *
 * Funciona em qualquer aparelho e sem rede — é por isso que o pipeline
 * determinístico depende dele, e não do Gemini Nano.
 */
class OcrTextReader(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** `null` quando a imagem não pôde ser lida ou nada foi reconhecido. */
    suspend fun read(uri: Uri): String? {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result -> continuation.resume(result.text.ifBlank { null }) }
                .addOnFailureListener { continuation.resume(null) }
        }
    }
}
```

- [ ] **Step 6: Implementar IngestResult e ShareViewModel**

`app/src/main/kotlin/com/pablo/btcmedio/ingest/IngestResult.kt`:

```kotlin
package com.pablo.btcmedio.ingest

import com.pablo.btcmedio.core.draft.TransactionDraft

/**
 * O resultado de interpretar uma entrada. `Failed` também é aproveitável:
 * a tela abre o formulário vazio com a explicação, nunca um beco sem saída.
 */
sealed interface IngestResult {
    data class Ready(val draft: TransactionDraft) : IngestResult
    data class Failed(val message: String) : IngestResult
}
```

`app/src/main/kotlin/com/pablo/btcmedio/ingest/ShareViewModel.kt`:

```kotlin
package com.pablo.btcmedio.ingest

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.model.EntrySource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ShareViewModel(
    private val chain: ExtractorChain,
    private val ocr: OcrTextReader,
) : ViewModel() {

    private val _queue = MutableStateFlow<List<IngestResult>>(emptyList())
    private val _current = MutableStateFlow<IngestResult?>(null)
    private val _loading = MutableStateFlow(false)

    val current: StateFlow<IngestResult?> = _current.asStateFlow()
    val remaining: StateFlow<List<IngestResult>> = _queue.asStateFlow()
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Várias imagens viram uma fila de confirmações, uma a uma. */
    fun loadImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _loading.value = true
        viewModelScope.launch {
            val results = uris.map { uri ->
                when (val text = ocr.read(uri)) {
                    null -> IngestResult.Failed(
                        "Não foi possível ler texto nesta imagem. Preencha os dados à mão."
                    )
                    else -> IngestResult.Ready(
                        chain.extract(text, EntrySource.IMAGE, LocalDate.now())
                    )
                }
            }
            _current.value = results.firstOrNull()
            _queue.value = results.drop(1)
            _loading.value = false
        }
    }

    fun loadText(text: String, source: EntrySource = EntrySource.TEXT) {
        if (text.isBlank()) {
            _current.value = IngestResult.Failed("Nenhum texto recebido. Preencha os dados à mão.")
            return
        }
        _loading.value = true
        viewModelScope.launch {
            _current.value = IngestResult.Ready(chain.extract(text, source, LocalDate.now()))
            _loading.value = false
        }
    }

    /** Avança para a próxima confirmação da fila. Devolve `false` quando acabou. */
    fun advance(): Boolean {
        val next = _queue.value.firstOrNull() ?: run {
            _current.value = null
            return false
        }
        _current.value = next
        _queue.value = _queue.value.drop(1)
        return true
    }

    fun draftFor(result: IngestResult?): TransactionDraft = when (result) {
        is IngestResult.Ready -> result.draft
        else -> TransactionDraft(occurredAt = System.currentTimeMillis())
    }

    companion object {
        fun factory(chain: ExtractorChain, ocr: OcrTextReader) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ShareViewModel(chain, ocr) as T
        }
    }
}
```

- [ ] **Step 7: Ampliar o AppContainer**

`app/src/main/kotlin/com/pablo/btcmedio/AppContainer.kt`:

```kotlin
package com.pablo.btcmedio

import android.content.Context
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.extract.ReceiptRuleExtractor
import com.pablo.btcmedio.data.AppDatabase
import com.pablo.btcmedio.data.TransactionRepository
import com.pablo.btcmedio.ingest.OcrTextReader

class AppContainer(private val context: Context) {

    private val database by lazy { AppDatabase.build(context) }

    val repository by lazy { TransactionRepository(database.transactionDao()) }

    val ocrTextReader by lazy { OcrTextReader(context) }

    /**
     * As regras vêm primeiro: são determinísticas e universais.
     * O Gemini Nano é acrescentado na Task 15 e só entra no que sobrar.
     */
    val extractorChain by lazy { ExtractorChain(listOf(ReceiptRuleExtractor())) }
}
```

- [ ] **Step 8: Implementar a ShareReceiverActivity**

`app/src/main/kotlin/com/pablo/btcmedio/ingest/ShareReceiverActivity.kt`:

```kotlin
package com.pablo.btcmedio.ingest

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablo.btcmedio.BtcMedioApp
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.ui.form.FormActions
import com.pablo.btcmedio.ui.form.TransactionFormScreen
import com.pablo.btcmedio.ui.form.TransactionFormViewModel
import com.pablo.btcmedio.ui.theme.BtcMedioTheme

/**
 * Recebe conteúdo compartilhado por outros apps e o transforma numa fila de
 * confirmações. Vive em Activity própria para não interferir na navegação
 * do app principal — o usuário confirma e volta direto para onde estava.
 */
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BtcMedioApp).container

        setContent {
            BtcMedioTheme {
                val shareVm: ShareViewModel = viewModel(
                    factory = ShareViewModel.factory(container.extractorChain, container.ocrTextReader)
                )
                val current by shareVm.current.collectAsStateWithLifecycle()
                val loading by shareVm.loading.collectAsStateWithLifecycle()

                androidx.compose.runtime.LaunchedEffect(Unit) { dispatch(shareVm) }

                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val draft = shareVm.draftFor(current)
                    val formVm: TransactionFormViewModel = viewModel(
                        key = draft.hashCode().toString(),
                        factory = TransactionFormViewModel.factory(
                            repository = container.repository,
                            transactionId = null,
                            initialDraft = draft,
                        ),
                    )
                    val state by formVm.state.collectAsStateWithLifecycle()

                    androidx.compose.runtime.LaunchedEffect(state.saved) {
                        if (state.saved && !shareVm.advance()) finish()
                    }

                    TransactionFormScreen(
                        state = state,
                        actions = FormActions(
                            onTypeChange = formVm::onTypeChange,
                            onDateChange = formVm::onDateChange,
                            onFiatChange = formVm::onFiatChange,
                            onFeeChange = formVm::onFeeChange,
                            onSatsChange = formVm::onSatsChange,
                            onPriceChange = formVm::onPriceChange,
                            onNoteChange = formVm::onNoteChange,
                            onCompleteMissing = formVm::completeMissing,
                            onSave = formVm::save,
                        ),
                        onDone = { if (!shareVm.advance()) finish() },
                    )
                }
            }
        }
    }

    private fun dispatch(shareVm: ShareViewModel) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.parcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                when {
                    uri != null -> shareVm.loadImages(listOf(uri))
                    text != null -> shareVm.loadText(text, EntrySource.TEXT)
                    else -> shareVm.loadText("")
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.parcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                shareVm.loadImages(uris)
            }

            Intent.ACTION_PROCESS_TEXT -> {
                val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
                shareVm.loadText(text, EntrySource.TEXT)
            }

            else -> shareVm.loadText("")
        }
    }

    private inline fun <reified T : android.os.Parcelable> Intent.parcelableExtra(name: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, T::class.java)
        } else {
            @Suppress("DEPRECATION") getParcelableExtra(name)
        }

    private inline fun <reified T : android.os.Parcelable> Intent.parcelableArrayListExtra(
        name: String,
    ): ArrayList<T>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(name, T::class.java)
        } else {
            @Suppress("DEPRECATION") getParcelableArrayListExtra(name)
        }
}
```

- [ ] **Step 9: Declarar os intent-filters de imagem**

Dentro de `<application>` em `app/src/main/AndroidManifest.xml`:

```xml
<activity
    android:name=".ingest.ShareReceiverActivity"
    android:exported="true"
    android:label="Registrar no Preço Médio BTC"
    android:windowSoftInputMode="adjustResize"
    android:theme="@style/Theme.BtcMedio">

    <intent-filter>
        <action android:name="android.intent.action.SEND" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="image/*" />
    </intent-filter>

    <intent-filter>
        <action android:name="android.intent.action.SEND_MULTIPLE" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:mimeType="image/*" />
    </intent-filter>
</activity>
```

- [ ] **Step 10: Rodar os testes**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*OcrTextReaderTest*"`
Expected: PASS

- [ ] **Step 11: Testar o fluxo real com o comprovante**

Run: `./gradlew :app:installDebug`

Copie `compra com kyc.png` (a captura do comprovante) para o aparelho, abra na galeria, toque em compartilhar e escolha "Registrar no Preço Médio BTC".

Expected: a tela de confirmação abre com valor `1.500,00`, taxa `22,50`, Bitcoin `0,00468094`, cotação `315.641,52` e data `30/06/2026 22:57`, sem nenhum aviso de incoerência.

- [ ] **Step 12: Commit**

```bash
git add -A && git commit -m "feat(app): OCR e recebimento de comprovantes compartilhados"
```

---

## Task 13: Texto compartilhado e ACTION_PROCESS_TEXT

O código da `ShareReceiverActivity` já trata os três casos; falta declará-los.

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/androidTest/kotlin/com/pablo/btcmedio/ingest/ShareViewModelTest.kt`

**Interfaces:**
- Consumes: `ShareViewModel`, `IngestResult` (Task 12)
- Produces: nenhuma API nova

- [ ] **Step 1: Escrever o teste que falha**

`app/src/androidTest/kotlin/com/pablo/btcmedio/ingest/ShareViewModelTest.kt`:

```kotlin
package com.pablo.btcmedio.ingest

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablo.btcmedio.core.extract.ExtractorChain
import com.pablo.btcmedio.core.extract.ReceiptRuleExtractor
import com.pablo.btcmedio.core.model.EntrySource
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareViewModelTest {

    private fun viewModel() = ShareViewModel(
        chain = ExtractorChain(listOf(ReceiptRuleExtractor())),
        ocr = OcrTextReader(ApplicationProvider.getApplicationContext()),
    )

    @Test
    fun texto_de_mensagem_vira_rascunho() = runTest {
        val vm = viewModel()
        vm.loadText("Compra confirmada: R$ 1.500,00 em 30/06/2026 22:57, cotação R$ 315.641,52, 0,00468094 BTC")
        val result = vm.current.first { it != null }
        assertTrue(result is IngestResult.Ready)
        val draft = (result as IngestResult.Ready).draft
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals(468_094L, draft.satoshis)
        assertEquals(31_564_152L, draft.unitPriceCents)
        assertEquals(EntrySource.TEXT, draft.source)
    }

    @Test
    fun texto_vazio_vira_falha_explicada() = runTest {
        val vm = viewModel()
        vm.loadText("")
        assertTrue(vm.current.first { it != null } is IngestResult.Failed)
    }

    @Test
    fun rascunho_de_falha_abre_formulario_com_data_preenchida() {
        val vm = viewModel()
        val draft = vm.draftFor(IngestResult.Failed("qualquer"))
        assertTrue(draft.occurredAt != null)
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha se o ShareViewModel ainda não existir**

Run: `./gradlew :app:connectedDebugAndroidTest --tests "*ShareViewModelTest*"`
Expected: PASS se a Task 12 estiver completa; caso contrário FAIL com `Unresolved reference`

- [ ] **Step 3: Declarar os intent-filters de texto**

Acrescente dentro do `<activity android:name=".ingest.ShareReceiverActivity">`:

```xml
<intent-filter>
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
</intent-filter>

<!-- Faz o app aparecer no menu de seleção de texto do WhatsApp, e-mail e navegador. -->
<intent-filter>
    <action android:name="android.intent.action.PROCESS_TEXT" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="text/plain" />
</intent-filter>
```

- [ ] **Step 4: Verificar as permissões e testar em aparelho**

Run: `./gradlew :app:verifyNoInternetPermissionDebug :app:installDebug`
Expected: BUILD SUCCESSFUL

No aparelho, selecione o texto `Compra de R$ 1.500,00 em 30/06/2026 às 22:57 por 0,00468094 BTC a R$ 315.641,52` numa conversa e escolha "Registrar no Preço Médio BTC" no menu de seleção.

Expected: a confirmação abre pré-preenchida.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(app): recebimento de texto compartilhado e seleção de texto"
```

---

## Task 14: Entrada por voz on-device

**Files:**
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ingest/OnDeviceSpeech.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/voice/VoiceCaptureDialog.kt`
- Modify: `app/src/main/kotlin/com/pablo/btcmedio/ui/MainActivity.kt`
- Modify: `app/src/main/kotlin/com/pablo/btcmedio/AppContainer.kt`

**Interfaces:**
- Consumes: `ExtractorChain` (Task 6), `Routes` (Task 9), `ShareViewModel` (Task 12)
- Produces:
  - `sealed interface SpeechEvent` com `data object Ready`, `data class Partial(text: String)`, `data class Final(text: String)`, `data class Failed(message: String)`
  - `class OnDeviceSpeech(context: Context) { fun isAvailable(): Boolean; fun listen(languageTag: String = "pt-BR"): Flow<SpeechEvent> }`
  - `@Composable fun VoiceCaptureDialog(events, onDismiss, onTranscribed: (String) -> Unit)`
  - `AppContainer.onDeviceSpeech: OnDeviceSpeech`

- [ ] **Step 1: Implementar o reconhecimento de voz**

`app/src/main/kotlin/com/pablo/btcmedio/ingest/OnDeviceSpeech.kt`:

```kotlin
package com.pablo.btcmedio.ingest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed interface SpeechEvent {
    data object Ready : SpeechEvent
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    data class Failed(val message: String) : SpeechEvent
}

/**
 * Reconhecimento de voz estritamente on-device.
 *
 * O áudio nunca é gravado em arquivo: a transcrição acontece em fluxo e só
 * o texto sobrevive. Não há arquivo para gerenciar nem para vazar.
 */
class OnDeviceSpeech(private val context: Context) {

    fun isAvailable(): Boolean =
        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun listen(languageTag: String = "pt-BR"): Flow<SpeechEvent> = callbackFlow {
        if (!isAvailable()) {
            trySend(
                SpeechEvent.Failed(
                    "O reconhecimento de voz offline não está disponível. " +
                        "Instale o pacote de idioma português em Ajustes do Android > " +
                        "Sistema > Idiomas e entrada > Reconhecimento de voz."
                )
            )
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { trySend(SpeechEvent.Ready) }
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { trySend(SpeechEvent.Partial(it)) }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (text.isNullOrBlank()) {
                    trySend(SpeechEvent.Failed("Não entendi. Tente de novo ou preencha à mão."))
                } else {
                    trySend(SpeechEvent.Final(text))
                }
                close()
            }

            override fun onError(error: Int) {
                trySend(SpeechEvent.Failed(messageFor(error)))
                close()
            }
        })

        recognizer.startListening(intent)
        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    private fun messageFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Erro ao capturar o áudio."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Permissão de microfone negada. Conceda-a nas configurações do app."
        SpeechRecognizer.ERROR_NO_MATCH -> "Não entendi. Tente de novo ou preencha à mão."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nenhuma fala detectada."
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->
            "O pacote de idioma português não está instalado neste aparelho. " +
                "Baixe-o em Ajustes do Android > Sistema > Idiomas e entrada > Reconhecimento de voz."
        else -> "Falha no reconhecimento de voz. Preencha os dados à mão."
    }
}
```

- [ ] **Step 2: Implementar o diálogo de captura**

`app/src/main/kotlin/com/pablo/btcmedio/ui/voice/VoiceCaptureDialog.kt`:

```kotlin
package com.pablo.btcmedio.ui.voice

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pablo.btcmedio.ingest.SpeechEvent

@Composable
fun VoiceCaptureDialog(
    event: SpeechEvent?,
    onDismiss: () -> Unit,
) {
    val (titulo, corpo) = when (event) {
        null, SpeechEvent.Ready -> "Ouvindo…" to "Diga, por exemplo: comprei mil e quinhentos reais de bitcoin hoje a trezentos e quinze mil."
        is SpeechEvent.Partial -> "Ouvindo…" to event.text
        is SpeechEvent.Final -> "Entendido" to event.text
        is SpeechEvent.Failed -> "Não foi possível usar a voz" to event.message
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = { Text(corpo) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}
```

- [ ] **Step 3: Ligar o botão de microfone**

No `AppContainer`, acrescente:

```kotlin
val onDeviceSpeech by lazy { OnDeviceSpeech(context) }
```

Na `MainActivity`, dentro do `composable(Routes.SUMMARY)`, substitua `onMic = { /* Task 14 */ }` por uma captura que:

1. pede `Manifest.permission.RECORD_AUDIO` com `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`;
2. ao ser concedida, coleta `container.onDeviceSpeech.listen()` num `LaunchedEffect`, exibindo `VoiceCaptureDialog` com o último evento;
3. ao receber `SpeechEvent.Final`, chama `container.extractorChain.extract(text, EntrySource.AUDIO, LocalDate.now())` e navega para o formulário com o rascunho resultante — reutilizando `TransactionFormViewModel.factory(initialDraft = draft)`;
4. ao receber `SpeechEvent.Failed`, mantém o diálogo com a mensagem e oferece "Preencher à mão", que navega para `Routes.form()`.

Passe o rascunho por um campo `pendingDraft` no `AppContainer` (`var pendingDraft: TransactionDraft? = null`), lido e limpo pela rota do formulário — rascunhos não cabem em argumentos de rota.

- [ ] **Step 4: Verificar as permissões**

Run: `./gradlew :app:verifyNoInternetPermissionDebug :app:assembleDebug`
Expected: BUILD SUCCESSFUL — `RECORD_AUDIO` presente, nenhuma permissão de rede

- [ ] **Step 5: Testar em aparelho**

Run: `./gradlew :app:installDebug`

Toque no microfone, conceda a permissão e diga: *"comprei mil e quinhentos reais de bitcoin ontem a trezentos e quinze mil seiscentos e quarenta e um"*.

Expected: o formulário abre com valor e cotação preenchidos e a data de ontem. Se o pacote de idioma não estiver instalado, a mensagem deve apontar o caminho nas configurações — e **não** deve haver nenhuma tentativa de rede.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(app): ditado por voz on-device sem gravação de áudio"
```

---

## Task 15: NanoExtractor (Gemini Nano, opcional)

Esta tarefa é isolada de propósito: se o artefato não resolver ou a API divergir, **todo o resto do app continua funcionando**.

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ingest/NanoExtractor.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ingest/NanoJsonParser.kt`
- Modify: `app/src/main/kotlin/com/pablo/btcmedio/AppContainer.kt`
- Test: `app/src/test/kotlin/com/pablo/btcmedio/ingest/NanoJsonParserTest.kt`

**Interfaces:**
- Consumes: `DraftExtractor`, `TransactionDraft`, `BrazilianNumberParser`, `BrazilianDateParser` (Tasks 3–6)
- Produces:
  - `object NanoJsonParser { fun parse(json: String, source: EntrySource, today: LocalDate): TransactionDraft? }`
  - `class NanoExtractor(context: Context) : DraftExtractor`

- [ ] **Step 1: Inspecionar a API real do artefato antes de escrever o adaptador**

O `genai-prompt` está em beta e sua superfície pública muda entre versões. Escrever o adaptador de memória seria adivinhação; resolva o artefato e leia as classes.

Adicione em `app/build.gradle.kts`:

```kotlin
implementation(libs.mlkit.genai.prompt)
```

Run: `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -i genai`
Expected: lista `com.google.mlkit:genai-prompt:1.0.0-beta2`

Localize o `.aar` no cache e liste as classes públicas:

```bash
find ~/.gradle/caches/modules-2/files-2.1/com.google.mlkit/genai-prompt -name "*.aar" | head -1
```

Extraia o `classes.jar` do `.aar` e rode `javap -classpath classes.jar` nas classes que aparecerem, anotando: como se obtém o cliente, como se consulta a disponibilidade do modelo, como se envia um prompt e como se lê a resposta.

**Se o artefato não resolver**, pule para o Step 6: registre o motivo, remova a dependência e siga sem Nano. O app está completo sem ele.

- [ ] **Step 2: Escrever o teste do parser de JSON (independe da API do ML Kit)**

`app/src/test/kotlin/com/pablo/btcmedio/ingest/NanoJsonParserTest.kt`:

```kotlin
package com.pablo.btcmedio.ingest

import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NanoJsonParserTest {

    private val hoje = LocalDate.of(2026, 8, 29)

    @Test
    fun interpreta_json_bem_formado() {
        val json = """
            {"tipo":"compra","data":"30/06/2026 22:57","valor":"1.500,00",
             "taxa":"22,50","bitcoin":"0,00468094","cotacao":"315.641,52"}
        """.trimIndent()
        val draft = NanoJsonParser.parse(json, EntrySource.IMAGE, hoje)!!
        assertEquals(TransactionType.BUY, draft.type)
        assertEquals(150_000L, draft.fiatAmountCents)
        assertEquals(2_250L, draft.feeCents)
        assertEquals(468_094L, draft.satoshis)
        assertEquals(31_564_152L, draft.unitPriceCents)
    }

    @Test
    fun ignora_texto_antes_e_depois_do_json() {
        val json = """Aqui está: {"tipo":"venda","valor":"1.500,00"} espero ter ajudado."""
        val draft = NanoJsonParser.parse(json, EntrySource.TEXT, hoje)!!
        assertEquals(TransactionType.SELL, draft.type)
        assertEquals(150_000L, draft.fiatAmountCents)
    }

    @Test
    fun campos_ausentes_viram_null_sem_lancar() {
        val draft = NanoJsonParser.parse("""{"tipo":"compra"}""", EntrySource.TEXT, hoje)!!
        assertNull(draft.fiatAmountCents)
        assertNull(draft.satoshis)
    }

    @Test
    fun numero_invalido_do_modelo_vira_null_em_vez_de_lixo() {
        val json = """{"valor":"mil e quinhentos","bitcoin":"zero virgula zero"}"""
        val draft = NanoJsonParser.parse(json, EntrySource.AUDIO, hoje)!!
        assertNull(draft.fiatAmountCents)
        assertNull(draft.satoshis)
    }

    @Test
    fun json_malformado_devolve_null() {
        assertNull(NanoJsonParser.parse("desculpe, não consegui", EntrySource.TEXT, hoje))
        assertNull(NanoJsonParser.parse("", EntrySource.TEXT, hoje))
    }

    @Test
    fun data_relativa_do_modelo_e_resolvida() {
        val draft = NanoJsonParser.parse("""{"data":"ontem"}""", EntrySource.AUDIO, hoje)!!
        assertEquals(
            LocalDate.of(2026, 8, 28).atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            draft.occurredAt,
        )
    }
}
```

- [ ] **Step 3: Rodar e confirmar que falha**

Run: `./gradlew :app:testDebugUnitTest --tests "*NanoJsonParserTest*"`
Expected: FAIL — `Unresolved reference: NanoJsonParser`

- [ ] **Step 4: Implementar o parser de JSON**

`app/src/main/kotlin/com/pablo/btcmedio/ingest/NanoJsonParser.kt`:

```kotlin
package com.pablo.btcmedio.ingest

import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.model.EntrySource
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.core.parse.BrazilianDateParser
import com.pablo.btcmedio.core.parse.BrazilianNumberParser
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/**
 * A saída do modelo nunca é confiada: cada número volta como string e passa
 * pelo mesmo parser determinístico usado no resto do app. Um LLM que invente
 * um dígito produz `null` aqui, não um valor plausível e errado.
 */
object NanoJsonParser {

    fun parse(raw: String, source: EntrySource, today: LocalDate): TransactionDraft? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        val json = try {
            JSONObject(raw.substring(start, end + 1))
        } catch (e: Exception) {
            return null
        }

        fun str(key: String): String? = json.optString(key, "").takeIf { it.isNotBlank() }

        val type = when (str("tipo")?.lowercase()) {
            "venda", "sell" -> TransactionType.SELL
            else -> TransactionType.BUY
        }

        return TransactionDraft(
            type = type,
            occurredAt = str("data")
                ?.let { BrazilianDateParser.parse(it, today) }
                ?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            fiatAmountCents = str("valor")?.let { BrazilianNumberParser.parseCents(it) },
            feeCents = str("taxa")?.let { BrazilianNumberParser.parseCents(it) },
            satoshis = str("bitcoin")?.let { BrazilianNumberParser.parseSatoshis(it) },
            unitPriceCents = str("cotacao")?.let { BrazilianNumberParser.parseCents(it) },
            source = source,
            rawText = raw,
        )
    }

    /** Prompt enviado ao modelo. Cabe folgado no limite de 256 tokens de saída. */
    fun promptFor(text: String): String = """
        Extraia os dados da transação de Bitcoin abaixo e responda APENAS com JSON,
        sem explicação. Use exatamente estas chaves, com os números no formato
        brasileiro e como texto. Omita a chave quando o dado não aparecer.

        {"tipo":"compra|venda","data":"dd/mm/aaaa hh:mm","valor":"","taxa":"","bitcoin":"","cotacao":""}

        Exemplo de entrada:
        Compra 30/06/2026 22:57 - R$ 1.500,00 + 0,00468094 Preço R$ 315.641,52 Taxa R$ 22,50
        Exemplo de saída:
        {"tipo":"compra","data":"30/06/2026 22:57","valor":"1.500,00","taxa":"22,50","bitcoin":"0,00468094","cotacao":"315.641,52"}

        Texto:
        $text
    """.trimIndent()
}
```

- [ ] **Step 5: Implementar o NanoExtractor contra a API observada no Step 1**

`app/src/main/kotlin/com/pablo/btcmedio/ingest/NanoExtractor.kt`:

```kotlin
package com.pablo.btcmedio.ingest

import android.content.Context
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.extract.DraftExtractor
import com.pablo.btcmedio.core.model.EntrySource
import java.time.LocalDate

/**
 * Extrator opcional baseado em Gemini Nano (ML Kit GenAI Prompt).
 *
 * Só é consultado quando as regras não completaram o rascunho, e apenas em
 * aparelhos com suporte. Toda falha vira indisponibilidade silenciosa: o
 * pipeline determinístico já entregou o que conseguiu.
 */
class NanoExtractor(private val context: Context) : DraftExtractor {

    override val name = "gemini-nano"

    // Substitua o corpo destes dois métodos pela API observada no Step 1.
    // O contrato exigido é apenas: `isAvailable()` responde sem lançar, e
    // `generate()` devolve o texto da resposta ou lança.

    override suspend fun isAvailable(): Boolean = try {
        modelStatusIsReady()
    } catch (e: Throwable) {
        false
    }

    override suspend fun extract(
        text: String,
        source: EntrySource,
        today: LocalDate,
    ): TransactionDraft? = try {
        NanoJsonParser.parse(generate(NanoJsonParser.promptFor(text)), source, today)
    } catch (e: Throwable) {
        null
    }

    /** TODO no Step 1: consulta de disponibilidade do modelo via genai-prompt. */
    private suspend fun modelStatusIsReady(): Boolean = false

    /** TODO no Step 1: envio do prompt e leitura da resposta via genai-prompt. */
    private suspend fun generate(prompt: String): String =
        throw UnsupportedOperationException("Adaptador do genai-prompt não implementado")
}
```

Depois de preencher os dois métodos, registre o extrator **depois** das regras no `AppContainer`:

```kotlin
val extractorChain by lazy {
    ExtractorChain(listOf(ReceiptRuleExtractor(), NanoExtractor(context)))
}
```

A ordem importa: as regras são determinísticas e vêm primeiro; o Nano só recebe o que sobrou.

- [ ] **Step 6: Verificar a promessa offline com o Nano presente**

Run: `./gradlew :app:verifyNoInternetPermissionDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL e testes verdes

Instale e teste no aparelho **com o modo avião ligado**, compartilhando um comprovante:

Run: `./gradlew :app:installDebug`
Expected: o registro funciona normalmente. Se o Nano não estiver disponível, nada muda no resultado — as regras já resolvem o comprovante.

Registre no commit o que foi observado sobre o download do modelo sem a permissão `INTERNET` no nosso manifesto. Este era o ponto marcado como "a validar em aparelho real" na spec.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat(app): extrator opcional por Gemini Nano com saída validada"
```

---

## Task 16: Tela de Ajustes

**Files:**
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/kotlin/com/pablo/btcmedio/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/kotlin/com/pablo/btcmedio/ui/MainActivity.kt`

**Interfaces:**
- Consumes: `OnDeviceSpeech` (Task 14), `NanoExtractor` (Task 15), `Routes` (Task 9)
- Produces:
  - `data class SettingsState(nanoAvailable: Boolean?, speechAvailable: Boolean)`
  - `class SettingsViewModel(speech, nano)` com `val state: StateFlow<SettingsState>`
  - `@Composable fun SettingsScreen(state, onBack)`

- [ ] **Step 1: Implementar o ViewModel**

`app/src/main/kotlin/com/pablo/btcmedio/ui/settings/SettingsViewModel.kt`:

```kotlin
package com.pablo.btcmedio.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pablo.btcmedio.core.extract.DraftExtractor
import com.pablo.btcmedio.ingest.OnDeviceSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    /** `null` enquanto a consulta ao modelo não terminou. */
    val nanoAvailable: Boolean? = null,
    val speechAvailable: Boolean = false,
)

class SettingsViewModel(
    speech: OnDeviceSpeech,
    private val nano: DraftExtractor?,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState(speechAvailable = speech.isAvailable()))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val available = try {
                nano?.isAvailable() ?: false
            } catch (e: Throwable) {
                false
            }
            _state.update { it.copy(nanoAvailable = available) }
        }
    }

    companion object {
        fun factory(speech: OnDeviceSpeech, nano: DraftExtractor?) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(speech, nano) as T
        }
    }
}
```

- [ ] **Step 2: Implementar a tela**

`app/src/main/kotlin/com/pablo/btcmedio/ui/settings/SettingsScreen.kt`:

```kotlin
package com.pablo.btcmedio.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: SettingsState, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(
                titulo = "Reconhecimento de voz offline",
                estado = if (state.speechAvailable) "Disponível" else "Indisponível",
                detalhe = if (state.speechAvailable) {
                    "O ditado por voz funciona sem rede neste aparelho."
                } else {
                    "Baixe o pacote de idioma português em Ajustes do Android > Sistema > " +
                        "Idiomas e entrada > Reconhecimento de voz. Sem ele, use o formulário manual."
                },
            )

            StatusCard(
                titulo = "Gemini Nano",
                estado = when (state.nanoAvailable) {
                    null -> "Verificando…"
                    true -> "Disponível"
                    false -> "Indisponível"
                },
                detalhe = when (state.nanoAvailable) {
                    null -> "Consultando o modelo no aparelho."
                    true -> "Usado como reforço quando a leitura por regras não completa a transação."
                    false -> "Este aparelho não tem suporte, ou o modelo não está instalado. " +
                        "Todas as funções do app continuam funcionando: a leitura de comprovantes " +
                        "por OCR e regras não depende dele."
                },
            )

            StatusCard(
                titulo = "Acesso à rede",
                estado = "Nenhum",
                detalhe = "O app não declara permissão de internet. Nada do que você registra sai " +
                    "deste aparelho, e nenhuma cotação é consultada online.",
            )

            Text(
                "Os dados ficam apenas neste aparelho. Desinstalar o app apaga o histórico.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun StatusCard(titulo: String, estado: String, detalhe: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(estado, style = MaterialTheme.typography.titleSmall)
            Text(detalhe, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
```

- [ ] **Step 3: Ligar a rota**

Adicione ao `NavHost` em `MainActivity.kt`:

```kotlin
composable(Routes.SETTINGS) {
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(container.onDeviceSpeech, container.nanoExtractor)
    )
    val state by vm.state.collectAsStateWithLifecycle()
    SettingsScreen(state = state, onBack = { navController.popBackStack() })
}
```

Exponha `nanoExtractor` no `AppContainer` (`val nanoExtractor: DraftExtractor? by lazy { NanoExtractor(context) }`, ou `null` se a Task 15 tiver sido pulada).

- [ ] **Step 4: Verificação final completa**

Run: `./gradlew clean :core:test :app:testDebugUnitTest :app:check :app:assembleDebug`
Expected: BUILD SUCCESSFUL, com `verifyNoInternetPermission` confirmando ausência de permissões de rede

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS

- [ ] **Step 5: Verificação manual do APK instalado**

```bash
adb shell dumpsys package com.pablo.btcmedio | grep -A 20 "requested permissions"
```

Expected: apenas `android.permission.RECORD_AUDIO`. Nenhuma linha com `INTERNET`.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(app): tela de ajustes com estado de voz, Nano e ausência de rede"
```
