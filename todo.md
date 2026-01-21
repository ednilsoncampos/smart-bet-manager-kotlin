# Smart Bet Manager Kotlin - TODO

## Feature: Correções Parser Betano + Schemas + Analytics

- [x] 01. Adicionar exception no parser Betano indicando que está em desenvolvimento
- [x] 02. Criar migração V2 para adicionar campos sport_id e is_bet_builder em bet_selections
- [x] 03. Criar migração V3 para schemas (core, betting, log)
- [x] 04. Atualizar entidades JPA para novos campos (sport_id, is_bet_builder)
- [x] 05. Atualizar ParsedSelectionData para incluir novos campos
- [x] 06. Atualizar SuperbetStrategy para extrair sport_id e is_bet_builder
- [x] 07. Atualizar BetanoStrategy para extrair sport_id e is_bet_builder (com exception)
- [x] 08. Substituir averageOdd por medianOdd no AnalyticsService
- [x] 09. Corrigir contagem de status no getPerformanceByProvider (incluir todos os status)
- [x] 10. Atualizar DTOs de response para medianOdd
- [x] 11. Compilar e testar (BUILD SUCCESSFUL)

## Feature: Correção de Testes

- [x] 12. Corrigir testes do BetanoStrategy (5 testes falhando devido à exception)
- [x] 13. Adicionar testes para novos campos sport_id e is_bet_builder
- [x] 14. Adicionar testes para medianOdd no AnalyticsService
