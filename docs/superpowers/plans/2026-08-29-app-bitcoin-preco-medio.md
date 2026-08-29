# App Bitcoin Preço Médio — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App Android nativo em Kotlin que registra compras e vendas de Bitcoin, calcula preço médio por custo médio ponderado, e aceita entradas por comprovante compartilhado (OCR), voz, texto e formulário manual — tudo 100% offline.

**Architecture:** Dois módulos Gradle. `:core` é Kotlin/JVM puro (domínio, cálculo, validação, parsing) e concentra toda a lógica de risco em testes JUnit rápidos. `:app` é a camada Android (Room, ML Kit, Compose) reduzida a I/O e apresentação. As fontes de entrada convergem para texto, passam por uma cadeia de extratores plugáveis (`DraftExtractor`) e desembocam sempre na mesma tela de confirmação.

**Tech Stack:** Kotlin 2.0.21, Gradle 8.11.1, AGP 8.7.3, Jetpack Compose (BOM 2024.12.01) + Material 3, Room 2.6.1 (KSP), ML Kit Text Recognition 16.0.1, ML Kit GenAI Prompt 1.0.0-beta2 (opcional), `SpeechRecognizer` on-device.

**Spec:** `docs/superpowers/specs/2026-08-29-app-bitcoin-preco-medio-design.md`

## Global Constraints

Todo requisito abaixo vale implicitamente para todas as tarefas.

- `minSdk = 33`, `compileSdk = 35`, `targetSdk = 35`, `jvmTarget = 17`.
- Package raiz: `com.pablo.btcmedio`. `:core` usa `com.pablo.btcmedio.core.*`, `:app` usa `com.pablo.btcmedio.*`.
- **Nunca `Double` ou `Float` para dinheiro ou BTC.** Reais em centavos (`Long`), Bitcoin em satoshis (`Long`). Cálculos derivados em `BigDecimal` com `RoundingMode.HALF_UP` e escala explícita.
- `1 BTC = 100_000_000 satoshis`.
- `:core` **não pode** ter nenhuma dependência Android. Só stdlib do Kotlin e `java.time`/`java.math`.
- O APK **não pode** conter `android.permission.INTERNET` nem `android.permission.ACCESS_NETWORK_STATE`. Única permissão: `android.permission.RECORD_AUDIO`.
- Sem cliente HTTP, sem analytics, sem crash reporting, sem Firebase.
- Toda transação vinda de OCR, voz, texto ou Nano passa obrigatoriamente por confirmação do usuário antes de ser gravada.
- Textos de interface em português do Brasil, com acentuação correta.
- Cada tarefa termina com commit. Mensagens de commit em português, com `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>` na última linha.

### Dados de referência (usados como asserções de teste)

As 9 compras da planilha `compra com kyc.xlsx`:

| # | Data | R$ | BTC | Cotação |
|---|---|---|---|---|
| 1 | 05/02/2026 | 4000,00 | 0,01174383 | 337198,31 |
| 2 | 05/02/2026 | 1500,00 | 0,00438067 | 337276,66 |
| 3 | 08/02/2026 | 1200,00 | 0,00319564 | 369878,75 |
| 4 | 23/02/2026 | 1200,00 | 0,00346617 | 341009,49 |
| 5 | 28/02/2026 | 1000,00 | 0,00286264 | 344087,16 |
| 6 | 29/03/2026 | 1200,00 | 0,00333755 | 354151,76 |
| 7 | 02/06/2026 | 1500,00 | 0,00425219 | 347467,96 |
| 8 | 05/06/2026 | 1500,00 | 0,00468493 | 315372,82 |
| 9 | 30/06/2026 22:57 | 1500,00 | 0,00468094 | 315641,52 |

Resultado esperado: custo `1_460_000` centavos, saldo `4_260_456` sats, preço médio `34_268_632` centavos.

---

## Estrutura de arquivos

### `:core` (Kotlin/JVM puro)

| Arquivo | Responsabilidade |
|---|---|
| `core/src/main/kotlin/com/pablo/btcmedio/core/model/Transaction.kt` | `Transaction`, `TransactionType`, `EntrySource` |
| `core/src/main/kotlin/com/pablo/btcmedio/core/model/PortfolioSummary.kt` | Resultado agregado da carteira |
| `core/src/main/kotlin/com/pablo/btcmedio/core/calc/PortfolioCalculator.kt` | A dobra de custo médio ponderado |
| `core/src/main/kotlin/com/pablo/btcmedio/core/calc/SequenceValidator.kt` | Detecta venda acima do saldo na sequência |
| `core/src/main/kotlin/com/pablo/btcmedio/core/parse/BrazilianNumberParser.kt` | `R$ 1.500,00`, `0,00468094`, `315.641,52` |
| `core/src/main/kotlin/com/pablo/btcmedio/core/parse/BrazilianDateParser.kt` | `30/06/2026 22:57`, `hoje`, `ontem` |
| `core/src/main/kotlin/com/pablo/btcmedio/core/draft/TransactionDraft.kt` | Rascunho com campos opcionais + origem |
| `core/src/main/kotlin/com/pablo/btcmedio/core/draft/DraftCompleter.kt` | Completa o campo faltante entre os quatro |
| `core/src/main/kotlin/com/pablo/btcmedio/core/draft/DraftValidator.kt` | Invariante dos 2 sats, taxa implícita |
| `core/src/main/kotlin/com/pablo/btcmedio/core/extract/DraftExtractor.kt` | Interface + `ExtractorChain` |
| `core/src/main/kotlin/com/pablo/btcmedio/core/extract/ReceiptRuleExtractor.kt` | Extração por regras a partir de texto |
| `core/src/main/kotlin/com/pablo/btcmedio/core/format/BrlFormatter.kt` | Formatação para exibição |

### `:app` (Android)

| Arquivo | Responsabilidade |
|---|---|
| `app/src/main/AndroidManifest.xml` | Permissões, remoção das de rede, intent-filters |
| `app/build.gradle.kts` | Build + tarefa `verifyNoInternetPermission` |
| `.../data/TransactionEntity.kt` | Entidade Room + mapeadores |
| `.../data/TransactionDao.kt` | DAO |
| `.../data/AppDatabase.kt` | Banco Room |
| `.../data/TransactionRepository.kt` | Fluxos + validação de sequência na escrita |
| `.../AppContainer.kt`, `.../BtcMedioApp.kt` | DI manual |
| `.../ui/summary/SummaryScreen.kt` + `SummaryViewModel.kt` | Tela Resumo |
| `.../ui/list/TransactionListScreen.kt` | Lista de transações |
| `.../ui/form/TransactionFormScreen.kt` + `TransactionFormViewModel.kt` | Formulário (criar/editar/confirmar) |
| `.../ui/settings/SettingsScreen.kt` | Ajustes |
| `.../ui/MainActivity.kt`, `.../ui/NavGraph.kt`, `.../ui/theme/` | Navegação e tema |
| `.../ingest/OcrTextReader.kt` | ML Kit Text Recognition |
| `.../ingest/ShareReceiverActivity.kt` | `ACTION_SEND` / `ACTION_PROCESS_TEXT` |
| `.../ingest/OnDeviceSpeech.kt` | Reconhecimento de voz offline |
| `.../ingest/NanoExtractor.kt` | Gemini Nano (opcional) |

---

## Tarefas

- **Task 1** — Esqueleto Gradle + modelo de domínio + `PortfolioCalculator`
- **Task 2** — `SequenceValidator`
- **Task 3** — `BrazilianNumberParser`
- **Task 4** — `BrazilianDateParser`
- **Task 5** — `TransactionDraft` + `DraftCompleter` + `DraftValidator`
- **Task 6** — `DraftExtractor` + `ExtractorChain` + `ReceiptRuleExtractor`
- **Task 7** — Módulo `:app`, manifesto sem rede, `verifyNoInternetPermission`
- **Task 8** — Room + repositório
- **Task 9** — Tema, navegação, tela Resumo
- **Task 10** — Formulário de transação (criar/editar/excluir)
- **Task 11** — Lista de transações
- **Task 12** — OCR + recebimento de imagem compartilhada
- **Task 13** — Recebimento de texto compartilhado e `ACTION_PROCESS_TEXT`
- **Task 14** — Entrada por voz on-device
- **Task 15** — `NanoExtractor` (Gemini Nano, opcional)
- **Task 16** — Tela de Ajustes

O detalhamento de cada tarefa está nos arquivos por bloco:

- `2026-08-29-app-bitcoin-preco-medio-tasks-01-06.md` — núcleo `:core`
- `2026-08-29-app-bitcoin-preco-medio-tasks-07-11.md` — `:app`, dados e UI
- `2026-08-29-app-bitcoin-preco-medio-tasks-12-16.md` — ingestão e ajustes
