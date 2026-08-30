# App Android — Registro de transações de Bitcoin e preço médio

**Data:** 2026-08-29
**Status:** Design aprovado, pronto para plano de implementação

## 1. Objetivo

Substituir a planilha `compra com kyc.xlsx` por um app Android nativo em Kotlin que registra compras e vendas de Bitcoin, soma os totais e calcula o preço médio de aquisição.

O app deve funcionar **totalmente offline**, sem consumo de APIs e sem envio de dados para qualquer destino. Além do cadastro manual, deve aceitar transações a partir de três fontes: imagens compartilhadas por outros apps (comprovantes de exchange), ditado por voz e mensagens de texto.

### Dados de referência

A planilha atual tem as colunas `Data | R$ | B$ | cotação` e 9 compras, com totais `=SUM(B:B)` = R$ 14.600,00 e `=SUM(C:C)` = 0,04260456 BTC.

O comprovante de exemplo (última linha da planilha) traz: R$ 1.500,00 debitados, taxa de R$ 22,50, total comprado R$ 1.477,50, preço R$ 315.641,52, recebido ₿ 0,00468094. Confere: `1.477,50 ÷ 315.641,52 = 0,0046809…`

Isso estabelece a semântica central: **a coluna `R$` é o valor bruto desembolsado, com a taxa dentro**. O preço médio da planilha já embute as taxas, que é o comportamento correto para custo de aquisição.

## 2. Decisões tomadas

| Tema | Decisão |
|---|---|
| Papel do Gemini Nano | Opcional para imagem e texto. **Obrigatório para voz** (ver revisão de 2026-08-30). Pipeline base determinístico (OCR + regras) funciona em qualquer aparelho. |
| Regra de venda | Custo médio ponderado. Venda não altera o preço médio. |
| Escopo de ativos | Somente Bitcoin. |
| Cotação atual | Não exibida. Somente custo, sem valor de mercado nem lucro não realizado. |
| Backup / import / export | Fora do MVP. |
| minSdk | 33 (Android 13), para garantir reconhecimento de voz on-device nativo. |
| Arquitetura de ingestão | Pipeline em cadeia com extratores plugáveis. |

### Não objetivos (MVP)

- Múltiplas criptomoedas
- Cotação de mercado, valorização, lucro não realizado
- Importação da planilha, exportação, backup e restauração
- Relatórios de IR, DARF, cálculo de imposto
- Sincronização, nuvem, múltiplos dispositivos
- Suporte a Android anterior ao 13

## 3. Modelo de dados

**Princípio inegociável: nenhum `Double` para dinheiro ou BTC.** Reais em centavos (`Long`), Bitcoin em satoshis (`Long`, 1 BTC = 100.000.000 sats). Como as exchanges usam 8 casas decimais, o mapeamento para satoshi é exato (`0,00468094 BTC = 468.094 sats`). Cálculos derivados usam `BigDecimal` com escala e arredondamento `HALF_UP` explícitos.

```kotlin
enum class TransactionType { BUY, SELL }
enum class EntrySource { MANUAL, IMAGE, AUDIO, TEXT }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,        // UUID
    val type: TransactionType,
    val occurredAt: Long,              // epoch millis (instante na exchange)
    val fiatAmountCents: Long,         // movimento de caixa: saiu (compra) / entrou (venda)
    val feeCents: Long?,               // taxa cobrada; null quando desconhecida
    val satoshis: Long,                // BTC negociado
    val unitPriceCents: Long,          // cotação R$/BTC informada pela exchange
    val source: EntrySource,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)
```

### Semântica de `fiatAmountCents`

É **o dinheiro que efetivamente se moveu na conta do usuário**:

- **Compra:** valor debitado, já contendo a taxa (R$ 1.500,00 no comprovante de exemplo).
- **Venda:** valor creditado, já descontada a taxa.

Isso torna compra e venda simétricas, faz o custo de aquisição embutir as taxas e faz as alienações serem líquidas de taxas — exatamente o que a planilha faz e o que o regime de custo médio espera.

### Redundância e validação

Os quatro campos numéricos têm apenas três graus de liberdade:

```
valorNegociado = fiatAmount - fee   (compra)
valorNegociado = fiatAmount + fee   (venda)
valorNegociado / btc ≈ unitPrice
```

O app **armazena os quatro como foram informados ou extraídos** e valida a coerência, sinalizando divergência além da tolerância de arredondamento. Nunca recalcula em silêncio, porque isso mascararia um erro de leitura do OCR.

**Invariante de coerência** (quando os quatro campos estão presentes):

```
|satoshis − arredonda(valorNegociado × 100_000_000 ÷ unitPriceCents)| ≤ 2 sats
```

A tolerância de 2 satoshis cobre o arredondamento da exchange sem deixar passar erro de dígito. Conferindo com o comprovante de exemplo: `1.477,50 ÷ 315.641,52 × 10⁸ = 468.093,7` → 468.094, casando exatamente com o valor registrado.

**Caso da taxa ausente.** Oito das nove linhas da planilha não registram taxa, e as taxas variam (1% na linha de R$ 4.000; 1,5% no comprovante). Por isso `feeCents` é **anulável**: `null` significa "desconhecida" e `0` significa "sem taxa" — distinção necessária, porque com `0` a invariante acima reprovaria 8 das 9 linhas. Quando `feeCents` é `null`, a invariante **não é aplicada**. Em vez disso o app deriva a taxa implícita:

```
taxaImplícita = fiatAmountCents − arredonda(satoshis × unitPriceCents ÷ 100_000_000)
```

e a oferece como preenchimento sugerido, marcada como derivada. Na linha de R$ 4.000: `4.000,00 − 3.959,996 = R$ 40,00`. Só há aviso se a taxa implícita for negativa ou exceder 5% do valor da transação — aí o problema não é taxa, é leitura errada de algum número.

Divergências geram **aviso visível, não bloqueio**: o usuário pode ter um comprovante legitimamente atípico e é ele quem decide.

## 4. Cálculo da posição

Dobra (fold) sobre as transações ordenadas por `occurredAt` crescente:

| Evento | Efeito |
|---|---|
| Compra | `custo += fiatAmount` ; `saldoSats += satoshis` |
| Venda | `custoVendido = custo × (satsVendidos ÷ saldoSats)` ; `realizado += fiatAmount − custoVendido` ; `custo −= custoVendido` ; `saldoSats −= satsVendidos` |

```
precoMedioCents = custoCents × 100_000_000 ÷ saldoSats
```

`custoVendido` é calculado em `BigDecimal` e arredondado para centavos com `HALF_UP`.

### Casos de borda

- **Venda acima do saldo:** bloqueada na validação, com mensagem informando o saldo disponível naquela data. A validação roda sobre a **sequência inteira resultante**, não sobre a transação isolada — porque editar ou excluir uma compra antiga pode tornar inválida uma venda posterior que era válida antes.
- **Estado impossível em disco:** `PortfolioCalculator` nunca lança exceção. Se encontrar uma venda acima do saldo (estado que a validação impede de ser gravado), limita a venda ao saldo disponível e continua. Uma tela de resumo que trava é pior que um número defensivamente limitado.
- **Saldo zerado:** custo e preço médio são zerados junto, sem resíduo de arredondamento acumulado.
- **Saldo zero na tela:** preço médio exibido como `—`, nunca divisão por zero.
- **Edição ou exclusão retroativa:** recalcula a dobra inteira. Sem cache incremental — com ordens de grandeza de centenas de transações o recálculo é instantâneo e elimina toda uma classe de bugs de invalidação.

### Caso de referência

As 9 compras da planilha devem produzir:

| Métrica | Valor esperado |
|---|---|
| Custo total | 1.460.000 centavos (R$ 14.600,00) |
| Saldo | 4.260.456 sats (0,04260456 BTC) |
| Preço médio | 34.268.632 centavos (R$ 342.686,32) |

Derivação do preço médio: `1.460.000 × 100.000.000 ÷ 4.260.456 = 34.268.632,28` → 34.268.632 centavos com `HALF_UP`.

## 5. Arquitetura

### Módulos Gradle

**`:core`** — Kotlin/JVM puro, **zero dependências Android**:

- Modelo de domínio (`Transaction`, `TransactionType`, `EntrySource`, `PortfolioSummary`)
- `PortfolioCalculator` — a dobra descrita na seção 4
- `TransactionValidator` — coerência dos quatro números, venda acima do saldo
- `BrazilianNumberParser` — `R$ 1.500,00`, `315.641,52`, `0,00468094`, `₿`
- `BrazilianDateParser` — `30/06/2026 22:57`, `30/06/2026`, datas relativas em texto livre
- `TransactionDraft` — rascunho com campos opcionais e a origem de cada um
- `DraftExtractor` — interface; `ReceiptRuleExtractor` — implementação por regras
- `ExtractorChain` — orquestra a cadeia de extratores

**`:app`** — Android:

- Room (DAO, database, repositório)
- ML Kit Text Recognition (OCR)
- `SpeechRecognizer` on-device
- `NanoExtractor` — implementa `DraftExtractor` de `:core` via `com.google.mlkit:genai-prompt`
- Compose / Material 3, ViewModels, Activities, intent-filters

Toda a matemática financeira e toda a extração de números ficam do lado testável em JVM pura. O lado Android fica reduzido a I/O e apresentação.

### Injeção de dependência

Container manual (`AppContainer`, instanciado no `Application`). Hilt não entra: com cerca de cinco telas ele adicionaria configuração de build e geração de código sem resolver nenhum problema existente. A migração posterior, se necessária, é mecânica.

### Dependências principais

```
com.google.mlkit:text-recognition          // OCR, modelo embarcado, offline em qualquer aparelho
com.google.mlkit:genai-prompt:1.0.0-beta2  // Gemini Nano, opcional
androidx.room:room-runtime + room-ktx
androidx.compose (BOM) + material3
androidx.navigation:navigation-compose
androidx.lifecycle:lifecycle-viewmodel-compose
```

O modelo de OCR é **embarcado no APK** (`text-recognition`, não a variante via Play Services), para não depender de download algum.

## 6. Fluxos de entrada

### Cadeia de extração

```
fonte → texto → ReceiptRuleExtractor → completo? ──sim──→ TransactionDraft
                       │                   │
                       │                   └──não──→ NanoExtractor (se disponível) → TransactionDraft (parcial)
                       │                                                                    │
                       └────────────────────────────────────────────────────────────────────┘
                                                     │
                                                     ▼
                                    tela de confirmação → Room
```

**1. Imagem vinda de outro app.** `ShareReceiverActivity` com intent-filter `ACTION_SEND` e `ACTION_SEND_MULTIPLE` para `image/*`. OCR → cadeia → confirmação. Várias imagens viram uma fila de confirmações sequenciais.

**2. Texto.** Intent-filters `ACTION_SEND` para `text/plain` e `ACTION_PROCESS_TEXT` — este último faz o app aparecer no menu de seleção de texto do WhatsApp, e-mail e navegador.

**3. Voz.** Botão de microfone na tela Resumo, permissão `RECORD_AUDIO`, `SpeechRecognizer.createOnDeviceSpeechRecognizer()` com `EXTRA_PREFER_OFFLINE`. **O áudio não é gravado em arquivo** — a transcrição acontece em fluxo e apenas o texto sobrevive. Se o pacote de idioma pt-BR não estiver instalado, a mensagem de erro diz isso explicitamente e aponta o caminho nas configurações do Android.

**O microfone só fica ativo quando reconhecimento de voz e Gemini Nano estão ambos disponíveis** (ver revisão de 2026-08-30).

**4. Imagem escolhida dentro do app.** Botão na barra superior da tela Resumo abre o seletor de fotos do Android (`PickMultipleVisualMedia`, até 10 imagens). O seletor **não exige permissão de galeria**: o sistema devolve apenas os Uris escolhidos e o app nunca enxerga o resto das fotos. Daí em diante o caminho é idêntico ao do compartilhamento: OCR, cadeia de extração e uma confirmação por imagem.

**5. Manual.** Formulário completo com criação, edição e exclusão. É o mesmo composable usado como tela de confirmação das outras fontes.

### Confirmação obrigatória

**Nada é gravado sem confirmação do usuário.** A tela de confirmação marca cada campo pela origem (extraído ou vazio), destaca divergência entre os quatro números e deixa tudo editável. OCR confunde `8` com `B` e `0` com `O`; LLM alucina dígito. Em dado financeiro, gravação automática é imprudente.

### Contrato do NanoExtractor

Prompt pede JSON estrito com poucos exemplos (cabe no limite de 256 tokens de saída do `genai-prompt`). **A saída nunca é confiada diretamente:** os números voltam como string e passam pelo mesmo `BrazilianNumberParser` e pelas mesmas validações de coerência do `:core`.

Disponibilidade é consultada antes do uso. Se o modelo não estiver presente, o extractor se declara indisponível e a cadeia termina no rascunho parcial. O download do modelo, quando ocorre, é executado pelo Play Services/AICore no processo deles, não no nosso — **a validar em aparelho real** se isso funciona sem a permissão `INTERNET` no nosso manifesto. Se não funcionar, o app permanece plenamente funcional pelo caminho de regras.

## 7. Telas

**Resumo** (inicial) — cartões com saldo em BTC, custo da posição, preço médio, total comprado, total vendido e resultado realizado. Abaixo, as últimas transações. FAB de adicionar e botão de microfone.

**Transações** — lista agrupada por mês; cada linha com data, chip de compra/venda, valor em R$, BTC e cotação. Toque abre edição; deslizar exclui, com snackbar de desfazer.

**Editar/criar** — tipo, data e hora, valor em R$, taxa, BTC e cotação. Botão **"completar campo faltante"** calcula o campo vazio a partir dos outros três, **sem nunca sobrescrever valor já preenchido**. Validação inline mostra divergência.

**Confirmação de importação** — o formulário acima, pré-preenchido. O texto bruto do OCR fica guardado no rascunho mas **não é exibido** (ver revisão de 2026-08-30).

**Ajustes** — estado do Gemini Nano no aparelho, estado do reconhecimento de voz offline em pt-BR, e a informação de que o app não possui permissão de rede.

## 8. Tratamento de erros

Toda falha converge para o **formulário manual aberto com o que se conseguiu obter e uma explicação do que faltou**. Nunca um beco sem saída.

| Situação | Comportamento |
|---|---|
| OCR não encontra texto | Formulário vazio + mensagem explicando |
| Transcrição vazia | Formulário vazio + mensagem explicando |
| Nenhum campo extraído | Formulário vazio, com a data preenchida |
| Nano indisponível | Silencioso; cadeia termina no rascunho de regras |
| Voz offline indisponível | Mensagem apontando as configurações do Android |
| Venda acima do saldo | Bloqueio na validação, informando o saldo daquela data |
| Uri de imagem sem permissão | Mensagem clara, sem crash |

## 9. Garantia de operação offline

- **Única permissão declarada: `RECORD_AUDIO`.** Sem permissões de galeria — imagens chegam por Uri concedida pelo share ou pelo Photo Picker.
- **Remoção ativa das permissões de rede.** As bibliotecas do ML Kit trazem `play-services-basement` na árvore de dependências, que declara `INTERNET` e `ACCESS_NETWORK_STATE` no manifesto delas. O merge do Android as injetaria no nosso APK. Por isso o nosso `AndroidManifest.xml` as remove explicitamente:

  ```xml
  <uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />
  ```

  Sem isso, "app offline" seria só intenção: as permissões estariam no APK instalado.
- Modelo de OCR embarcado no APK.
- Sem cliente HTTP, sem analytics, sem crash reporting.
- **Tarefa Gradle `verifyNoInternetPermission`, ligada ao `check`**, lê o AndroidManifest mergeado pela API de artefatos do AGP e falha o build se qualquer permissão de rede sobreviver. É o que transforma a remoção acima de intenção em invariante verificada — inclusive contra uma dependência futura que volte a injetá-la.

## 10. Estratégia de testes

### `:core` — JUnit puro, sem emulador

- `PortfolioCalculator`: as 9 compras da planilha produzindo exatamente 1.460.000 centavos, 4.260.456 sats e 34.268.632 centavos de preço médio
- Vendas: parcial, total, sequência compra/venda/compra, zeragem sem resíduo de arredondamento, limitação defensiva de venda acima do saldo
- `SequenceValidator`: rejeição de venda acima do saldo, incluindo o caso em que a edição de uma compra antiga invalida uma venda posterior
- `BrazilianNumberParser`: `R$ 1.500,00`, `315.641,52`, `0,00468094`, `₿ 0,00468094`, `1.500`, entradas malformadas
- `BrazilianDateParser`: `30/06/2026 22:57`, `30/06/2026`, expressões relativas
- `ReceiptRuleExtractor`: alimentado com o texto que o OCR produz para o comprovante de exemplo
- `TransactionValidator`: invariante dos 2 satoshis dentro e fora da tolerância; taxa ausente derivando R$ 40,00 na linha de R$ 4.000 e R$ 22,50 no comprovante; taxa implícita negativa e acima de 5% gerando aviso

### `:app`

- DAO contra Room em memória
- Testes de Compose na tela de confirmação
- Tarefa `verifyNoInternetPermission` sobre o manifesto mergeado (seção 9)
- Nenhum teste depende de aparelho com Nano — ele entra pela interface `DraftExtractor` e é substituído por um duplo

### Método

Desenvolvimento guiado por testes: teste falhando antes da implementação, em todo o `:core`.

## 11. Revisão de 2026-08-30 — voz condicionada ao Nano

Depois de rodar o MVP em aparelho real, duas mudanças a pedido do usuário.

### O ditado por voz exige Gemini Nano

O primeiro ditado real — *"0,1 bitcoin por r$ 5.000"* — produziu `Valor em reais: 0,10` e todos os outros campos vazios. Rastreando as regras:

- `satsNearSymbol` procura `₿` ou `BTC`; a fala trazia **"bitcoin" por extenso**.
- `eightDecimals` exige 6 a 8 casas decimais; `0,1` tem uma.
- `naturalPrice` só reconhece a preposição **"a"**; a fala usou **"por"**.
- `fallbackFiat` então pegou **o primeiro** número da linha — que era a quantidade de bitcoin, não o valor.

A causa raiz não é um bug pontual: **as regras foram desenhadas para a forma de um comprovante**, onde cada linha traz um rótulo e um valor, e a posição resolve a ambiguidade. Linguagem falada não tem rótulo — o que atribui cada número ao seu campo é a preposição e a ordem das palavras. São duas gramáticas diferentes.

Diante disso, a decisão foi **condicionar a voz ao Nano** em vez de estender as regras. O microfone da tela Resumo só fica ativo quando `OnDeviceSpeech.isAvailable()` **e** `NanoExtractor.isAvailable()` são verdadeiros. Sem o modelo, um ditado produz número errado com aparência plausível — pior que não oferecer o recurso.

O custo aceito: em aparelho sem Nano o ditado desaparece. A tela de Ajustes ganhou um cartão "Ditado por voz" explicando o motivo, para que o microfone apagado não vire mistério.

Fica aberta, para quando fizer sentido, a alternativa descartada agora: ensinar as regras a ler fala — reconhecer "bitcoin"/"satoshi" por extenso e usar as preposições "por", "a" e "de" para atribuir cada número. É trabalho contido no `:core` e testável em JVM pura.

### Importação de imagens dentro do app

O compartilhamento vindo de outro app continua, e ganhou um par: um botão na barra superior abre o seletor de fotos do Android. Ver a fonte 4 da seção 6.

### Painel "Texto reconhecido" removido

A faixa recolhível que exibia o texto bruto do OCR saiu da tela de confirmação, a pedido do usuário: para quem usa o app ela é ruído, não informação.

O campo `rawText` continua no `TransactionDraft` — é preenchido pela `ExtractorChain` e usado nos testes que verificam a preservação da entrada original. O que mudou foi só a exibição.

O que se perde com isso, registrado para quando doer: quando um número sair errado, não há mais como distinguir na tela se o erro foi de leitura (OCR) ou de interpretação (regras). Foi exatamente esse painel que permitiu diagnosticar em minutos o caso do ditado por voz descrito acima. Se o diagnóstico voltar a ser necessário, o caminho barato é reintroduzi-lo atrás de um interruptor na tela de Ajustes, em vez de sempre visível.

## 12. Próximos passos após o MVP

Deliberadamente fora de escopo agora, na ordem provável de valor:

1. Exportação e importação CSV, e backup/restauração completa
2. Múltiplas criptomoedas
3. Relatórios para declaração de IR
