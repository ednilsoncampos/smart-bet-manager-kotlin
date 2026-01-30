# Smart Bet Manager Kotlin - TODO

## Feature: Bet Builder (Criar Aposta)

- [x] 01. Criar migração V4 com tabela bet_selection_components
- [x] 02. Criar entidade de domínio BetSelectionComponent
- [x] 03. Criar entidade JPA BetSelectionComponentEntity
- [x] 04. Criar repository BetSelectionComponentRepository
- [x] 05. Atualizar SuperbetStrategy para extrair eventComponents quando array não estiver vazio
- [x] 06. Atualizar TicketService para salvar components

## Feature: Tratamento de Cashout

- [x] 07. Adicionar campo is_cashed_out em bet_tickets (migração V4)
- [x] 08. Atualizar BetTicket e BetTicketEntity com campo isCashedOut
- [x] 09. Atualizar SuperbetStrategy para extrair win.isCashedOut e win.totalWinnings
- [x] 10. Implementar lógica de FinancialStatus para cashout (BREAK_EVEN, PARTIAL_WIN, PARTIAL_LOSS)

## Feature: Validação de Bilhete Duplicado

- [x] 11. Adicionar constraint UNIQUE (user_id, external_ticket_id) na migração V4
- [x] 12. Criar DuplicateTicketException
- [x] 13. Adicionar query findByUserIdAndExternalTicketId no repository
- [x] 14. Implementar validação de duplicata no TicketService
- [x] 15. Adicionar tratamento de DuplicateTicketException no GlobalExceptionHandler (HTTP 409)

## Testes

- [x] 16. Adicionar testes para extração de eventComponents
- [x] 17. Adicionar testes para lógica de cashout
- [x] 18. Adicionar testes para validação de duplicata

## Build

- [x] 19. BUILD SUCCESSFUL - Todos os testes passaram

## Cores de Referência (Frontend)

| Status | Cor | Código Hex |
|--------|-----|------------|
| FULL_WIN | Verde | #22C55E |
| PARTIAL_WIN | Verde Água | #2DD4BF |
| BREAK_EVEN | Cinza | #9CA3AF |
| PARTIAL_LOSS | Laranja Claro | #FB923C |
| TOTAL_LOSS | Vermelho | #EF4444 |
| PENDING | Azul | #3B82F6 |

## Feature: Status Parciais e Refatoração de Analytics

### Enums
- [ ] 20. Adicionar PARTIAL_WIN e PARTIAL_LOSS em TicketStatus.kt
- [ ] 21. Adicionar HALF_WON e HALF_LOST em SelectionStatus.kt

### Renomear Classes
- [ ] 22. Renomear AnalyticsDtos.kt para PerformanceAnalyticDto.kt
- [ ] 23. Renomear AnalyticsService.kt para PerformanceAnalyticService.kt
- [ ] 24. Renomear AnalyticsServiceTest.kt para PerformanceAnalyticServiceTest.kt
- [ ] 25. Atualizar imports em AnalyticsController.kt

### DTOs de Performance
- [ ] 26. Adicionar isCashedOut em TicketResponse
- [ ] 27. Adicionar campos detalhados em OverallPerformanceResponse
- [ ] 28. Adicionar campos detalhados em PerformanceByMarketResponse
- [ ] 29. Adicionar campos detalhados em PerformanceByTournamentResponse

### Lógica de Parsing
- [ ] 30. Atualizar mapeamento de status em SuperbetStrategy.kt
- [ ] 31. Atualizar mapeamento de status em BetanoStrategy.kt

### Lógica de Analytics
- [ ] 32. Atualizar PerformanceAnalyticService para calcular status parciais

### Testes
- [ ] 33. Atualizar testes existentes
- [ ] 34. Adicionar testes para novos status

### Build
- [ ] 35. Compilar e testar
