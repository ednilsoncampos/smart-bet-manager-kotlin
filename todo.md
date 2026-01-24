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
