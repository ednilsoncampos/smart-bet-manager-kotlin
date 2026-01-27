# Contexto Geral
Você está trabalhando no backend do projeto.  
Implemente as correções abaixo com foco em **regra de negócio**, **consistência de dados** e **correta interpretação do status financeiro do bilhete**.

Leia atentamente os arquivos JSON mencionados antes de alterar o código.

---

## 01 - Correção de Identificação de Status Financeiro do Bilhete

### Objetivo
Corrigir a lógica que identifica se um bilhete resultou em:
- ganho total
- ganho parcial
- perda total
- perda parcial

### Dados Disponíveis
No JSON original do bilhete, utilize obrigatoriamente os campos:
- `win.estimated`
- `win.totalWinnings`

Esses campos devem ser a base para determinar o status financeiro e deve ser comparado com o valor de entrada da aposta.

### Arquivos para Análise
Leia e compare os seguintes arquivos:
- `bilhete-original.json` (estrutura completa)
- `ganho-total.json` (trecho representando ganho total)
- `ganho-parcial.json` (trecho representando ganho parcial)

### Local da Correção
- Classe: `SuperbetStrategy`
- Método: `mapSelectionStatus`

### Problema Atual
O método `mapSelectionStatus` **não está identificando corretamente** o status financeiro do bilhete, principalmente nos casos de ganho parcial.

### Requisitos
- Ajustar a lógica para diferenciar corretamente:
  - ganho total
  - ganho parcial
  - perda total
- A decisão deve ser baseada nos valores de `win.estimated` e `win.totalWinnings`
- Não criar regras arbitrárias: use apenas o que os dados permitem inferir
- Manter o método coeso e legível

---

## 02 - Importação de Bilhete – Regra de Dupla Chance

### Objetivo
Adicionar tratamento correto para o mercado de **dupla chance** durante a importação do bilhete.

### Local da Alteração
- Classe: `TicketService`
- Método: `importFromUrl`

### Nova Regra de Negócio
Ao importar o bilhete, mapear corretamente as seleções de dupla chance:

- `1X` deve ser interpretado como:
  - Time 1 ou Empate

- `2X` deve ser interpretado como:
  - Time 2 ou Empate

### Requisitos
- Garantir que essa regra seja aplicada apenas ao mercado de dupla chance
- Não impactar outros tipos de mercado
- Manter compatibilidade com bilhetes já existentes
- Caso já exista lógica similar, ajustar ao invés de duplicar

---

## Requisitos Finais
- Código limpo e alinhado ao padrão atual do projeto
- Nenhuma quebra de funcionalidade existente
- Comentários apenas onde a regra de negócio não for óbvia
- Ao final, explique resumidamente as alterações realizadas
