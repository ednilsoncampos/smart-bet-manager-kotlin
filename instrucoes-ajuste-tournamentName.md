# Contexto
Você tem acesso ao projeto atual. A tarefa é ajudar a modificar e ajustar o código até que ele compile corretamente.

# Regras gerais
1. Use a branch atual; **não precisa criar nova branch nem comitar nada**.
2. Sua tarefa termina **quando o projeto estiver compilando sem erros**.
3. Foque em corrigir problemas, ajustes ou dependências que impeçam a compilação.
4. Sempre explique brevemente o que foi alterado e por quê, mas sem gerar commits.
5. Você pode sugerir mudanças de código ou aplicar ajustes diretamente no projeto, mas não precisa se preocupar com versionamento.
6. Antes de implementar mostre a solução e as alterações que vão ser realizadas, para eu validar e permitir o inicio das alterações.
7. Não implemente nada antes de listar e validar comigo as alterações.

# Objetivo
- O projeto deve compilar sem erros.
- Nenhum commit ou branch extra é necessário.
- A tarefa termina somente quando o projeto estiver compilando.

---

# Atualização específica de código Kotlin
Atualize o método Kotlin `parseSelectionsAndComponents` conforme abaixo.

## Problema atual
- O campo `tournamentName` está sendo preenchido incorretamente com `tournamentId`.

## Fonte correta
- Endpoint editorial:
  `https://superbet-content.freetls.fastly.net/cached-superbet/hot-tournaments/offer/br/{date}`

## Derivação da data
- Usar o campo `event.date`
- Exemplo:
  `"2026-01-24T14:30:00.000Z"` → `"2026-01-24"`

## Formato do catálogo
- Retorna um array de objetos, ex:
```json
[
  {
    "tournamentIds": [245, 1568],
    "title": "Bundesliga"
  },
  {
    "tournamentIds": [320],
    "title": "Premier League"
  }
]
```

## Regra de resolução
Para cada evento:
1. Ler `event.tournamentId` (string).
2. Converter para `Int`.
3. Procurar no catálogo um item cujo `tournamentIds` contenha esse ID.
4. Usar o campo `title` como `tournamentName`.

## Cache em memória
O catálogo deve ser carregado apenas uma vez por data.
Criar cache em memória:
`Map<String, Map<Int, String>>` // `date` -> (`tournamentId` -> `title`)
Analise se esta é a melhor solução.

## Regras adicionais
- Se houver múltiplos itens com o mesmo `tournamentId`, usar o primeiro.
- Se não encontrar correspondência, usar `"Torneio não identificado"`.
- Não alterar a assinatura do método.
- Não refatorar a lógica existente; altere apenas o necessário.

## Método atual
com.smartbet.infrastructure.provider.strategy.SuperbetStrategy#parseSelectionsAndComponents

## Instruções de execução
1. Altere o código conforme necessário para corrigir o preenchimento de `tournamentName` e garantir que o projeto compile.
2. Teste a compilação localmente.
3. Explique brevemente cada alteração realizada.
4. Não crie commits nem branches extras; use a branch atual.
5. Informe apenas quando o projeto estiver compilando sem erros.

