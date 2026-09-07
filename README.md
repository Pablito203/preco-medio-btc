<div align="center">

# ₿ Preço Médio BTC

**Registre suas compras e vendas de Bitcoin e saiba, na hora, quanto você realmente pagou por cada satoshi — sem depender de planilha, sem enviar um único byte para a internet.**

App Android nativo · Kotlin · 100% offline · Sem conta, sem login, sem nuvem

</div>

---

## Por que este app existe

Este projeto nasceu para substituir uma planilha pessoal de compras de Bitcoin — a mesma dor de sempre: cada aporte novo pedia uma linha nova, uma conta manual e a torcida para não errar uma vírgula. O app faz o que a planilha fazia, só que sozinho: soma o que você comprou, o que vendeu, calcula o **preço médio de aquisição pelo método do custo médio ponderado** e mantém o histórico completo, sem que você precise abrir o Excel de novo.

Não é uma corretora, não é uma carteira e não guarda Bitcoin nenhum. É um caderno de registros que sabe fazer a média — o mesmo papel de uma planilha, só que mais rápido de preencher e sem chance de fórmula quebrada.

## O diferencial: tudo isso sem internet

Boa parte dos apps de finanças pede login, sincroniza com um servidor e manda telemetria a cada toque. Este não pode: **o `AndroidManifest.xml` não declara a permissão `INTERNET`**, e essa ausência é verificada automaticamente a cada build — não é uma promessa de política de privacidade, é uma restrição imposta pelo próprio sistema operacional. Se alguma dependência tentasse reintroduzir acesso à rede por baixo dos panos, o build falharia antes de chegar ao seu aparelho.

Isso significa, na prática:

- Nenhum dado seu — valores, datas, holdings — sai do aparelho, nunca
- Nenhuma conta, nenhum login, nenhum "criar senha"
- Nenhum anúncio, nenhum rastreador, nenhuma telemetria
- Nada de nuvem: perdeu o aparelho, perdeu o histórico — não há o que "recuperar" de um servidor, porque nunca existiu um

## Reconhecimento de comprovantes, sem enviar nada a lugar nenhum

A parte mais trabalhosa de registrar uma compra é sempre a mesma: abrir o app da corretora, ler o comprovante e digitar tudo de novo à mão. Este app resolve isso com **compartilhamento direto**:

1. Você compra Bitcoin normalmente, na sua corretora de sempre
2. A corretora mostra o comprovante da operação
3. Você toca em **Compartilhar** → escolhe **Preço Médio BTC**
4. O app lê o texto da imagem, extrai valor, taxa, quantidade e cotação, e te mostra um formulário já preenchido para você conferir e salvar

Todo esse reconhecimento acontece **dentro do aparelho**, com o [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition) do Google — uma biblioteca cujo modelo vem embarcado no próprio APK. Não existe chamada de rede, não existe API de nuvem, não existe LLM remoto processando sua imagem. A foto do seu comprovante nunca sai do seu celular.

Depois do OCR, um interpretador de regras (também 100% local, escrito em Kotlin puro) lê o texto reconhecido e identifica cada campo — mesmo quando a cotação, a taxa ou a quantidade estão em posições diferentes de uma corretora para outra. Se um valor não bate ou algo ficou ambíguo, o app avisa antes de salvar, em vez de gravar um número errado silenciosamente.

> Também dá para importar comprovantes direto da galeria, sem precisar compartilhar de outro app — útil para lançar comprovantes antigos.

## Ditado por voz, para quando digitar é chato

Em aparelhos com suporte a **Gemini Nano** (o modelo de linguagem que roda localmente em alguns Android mais recentes), dá para simplesmente falar a transação — "comprei mil reais de Bitcoin agora a 350 mil" — e o app preenche o formulário sozinho, novamente sem nenhuma chamada de rede: o Gemini Nano roda inteiramente no chip do aparelho.

Esse recurso é opcional e aparece condicionado à disponibilidade do modelo no seu hardware — em aparelhos sem suporte, ele simplesmente não é oferecido, e o app continua funcionando normalmente pelos outros três métodos de entrada.

## As quatro formas de registrar uma transação

| Método | Como funciona |
|---|---|
| 📷 **Comprovante compartilhado** | Compartilhe a imagem de outro app; o texto é lido e os campos, preenchidos |
| 🖼️ **Importar da galeria** | Escolha uma imagem já salva, sem sair do app |
| 🎙️ **Ditado por voz** | Fale a transação em português (requer Gemini Nano) |
| ⌨️ **Manual** | Digite dois valores quaisquer — o terceiro é calculado sozinho |

Em todos os casos, **você sempre confirma antes de salvar**. Nada é gravado automaticamente: o formulário chega preenchido, mas a decisão final é sempre sua.

## O que o app calcula

- **Preço médio de aquisição**, pelo custo médio ponderado — o método aceito pela Receita Federal para apuração de ganho de capital
- Saldo atual em Bitcoin e custo total da posição
- Total comprado, total vendido e resultado realizado em cada venda
- Total de taxas pagas às corretoras
- Histórico completo, agrupado por mês, com edição e exclusão de qualquer lançamento

A venda reduz posição e custo proporcionalmente, sem alterar o preço médio das unidades restantes — e registra o lucro ou prejuízo daquela alienação especificamente.

---

## Requisitos

### Para usar o app

- **Android 13 (API 33) ou superior**
- Ditado por voz: aparelho com suporte a **Gemini Nano** (recurso opcional; o restante do app funciona sem ele)

### Para compilar o projeto

| Ferramenta | Versão |
|---|---|
| JDK | 17 (compilação testada também com JDK 21, gerando bytecode nível 17) |
| Android SDK | Platform 36, Build-Tools mais recente |
| Gradle | via wrapper incluso — nenhuma instalação separada necessária |
| Kotlin | 2.2.0 (gerenciado pelo Gradle) |

Não é necessário instalar Gradle manualmente: o repositório já inclui o `gradlew` / `gradlew.bat`, que baixa a versão correta na primeira execução.

## Como rodar

Clone o repositório e, na raiz do projeto:

```bash
# Rodar os testes do módulo de domínio (puro Kotlin, sem emulador — segundos)
./gradlew :core:test

# Rodar os testes de unidade do app
./gradlew :app:testDebugUnitTest

# Instalar em um emulador ou aparelho físico conectado (modo debug)
./gradlew :app:installDebug

# Rodar os testes instrumentados (exige emulador ou aparelho conectado via adb)
./gradlew :app:connectedDebugAndroidTest

# Gerar o pacote de distribuição (App Bundle) para a Play Store
./gradlew :app:bundleRelease
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

Alternativamente, abra a pasta no **Android Studio** (Ladybug ou mais recente) e deixe a sincronização do Gradle configurar tudo — o botão de *Run* já aparece assim que o projeto terminar de indexar.

### Verificando a promessa de "zero rede"

O projeto inclui uma tarefa de build própria que falha caso qualquer permissão de rede apareça no manifesto final do APK — inclusive se ela vier de uma dependência transitiva:

```bash
./gradlew :app:check
```

Procure por `verifyNoInternetPermissionDebug` / `verifyNoInternetPermissionRelease` na saída: se passar, o app literalmente não tem como acessar a internet.

## Arquitetura, em uma frase

O projeto é dividido em dois módulos: **`:core`**, Kotlin puro sem nenhuma dependência de Android — onde vive todo o cálculo financeiro, os parsers de texto e a lógica de extração de comprovantes, testável em segundos e sem emulador — e **`:app`**, a camada Android com Room, Compose e a integração com ML Kit e Gemini Nano. Essa separação é o que permite ao `:core` ter 90 testes de unidade rodando em puro JVM, sem qualquer dependência do Android SDK.

## Testes

| Módulo | O que cobre | Quantidade |
|---|---|---|
| `:core` | Cálculo do preço médio, parsers de data/número em português, extração de comprovantes, validações | 90 testes |
| `:app` (unidade) | Formatação de valores para exibição | 10 testes |
| `:app` (instrumentado) | Persistência (Room), reconhecimento de imagem real via OCR, fluxos de UI | 49 testes |

## Privacidade

A política de privacidade completa está em [`docs/play-store/politica-de-privacidade.html`](docs/play-store/politica-de-privacidade.html). Em resumo: nada é coletado, nada é enviado, nada sai do aparelho.

---

<div align="center">

Feito para resolver um problema pessoal — compartilhado caso resolva o seu também.

</div>
