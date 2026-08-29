package com.pablo.btcmedio.ingest

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.Generation
import com.pablo.btcmedio.core.draft.TransactionDraft
import com.pablo.btcmedio.core.extract.DraftExtractor
import com.pablo.btcmedio.core.extract.LlmDraftJson
import com.pablo.btcmedio.core.model.EntrySource
import java.time.LocalDate

/**
 * Extrator opcional baseado em Gemini Nano (ML Kit GenAI Prompt).
 *
 * Só é consultado quando as regras não completaram o rascunho, e apenas em
 * aparelhos com suporte. Toda falha vira indisponibilidade silenciosa: o
 * pipeline determinístico já entregou o que conseguiu.
 *
 * O modelo é aberto e fechado a cada uso. Manter o cliente vivo economizaria
 * alguns milissegundos, mas o uso aqui é esporádico e vazar o recurso num app
 * que fica aberto o dia todo custa mais caro.
 */
class NanoExtractor : DraftExtractor {

    override val name = "gemini-nano"

    /**
     * Status bruto do modelo neste aparelho, para a tela de Ajustes.
     * `null` quando nem o cliente pôde ser criado.
     */
    suspend fun status(): Int? = withModel { it.checkStatus() }

    /**
     * Só `AVAILABLE` conta. `DOWNLOADABLE` significa que o modelo existe para
     * este aparelho mas ainda não foi baixado — e disparar esse download por
     * conta própria seria atividade de rede que o usuário não pediu.
     */
    override suspend fun isAvailable(): Boolean = status() == FeatureStatus.AVAILABLE

    override suspend fun extract(
        text: String,
        source: EntrySource,
        today: LocalDate,
    ): TransactionDraft? = withModel { model ->
        val resposta = model.generateContent(LlmDraftJson.promptFor(text))
        val saida = resposta.candidates.firstOrNull()?.text ?: return@withModel null
        LlmDraftJson.parse(saida, source, today)
    }

    private suspend fun <T> withModel(block: suspend (GenerativeModel) -> T): T? {
        val model = try {
            Generation.getClient()
        } catch (e: Throwable) {
            return null
        }
        return try {
            block(model)
        } catch (e: Throwable) {
            null
        } finally {
            runCatching { model.close() }
        }
    }
}
