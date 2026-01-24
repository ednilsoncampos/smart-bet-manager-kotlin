# Technical Debt - Smart Bet Manager

Este documento lista os problemas técnicos identificados na revisão de código, organizados por severidade e categoria.

---

## Críticos (Corrigir Imediatamente)

### 1. Configuração CORS Insegura

**Arquivo:** `src/main/kotlin/com/smartbet/infrastructure/config/SecurityConfig.kt:77-81`

```kotlin
allowedOriginPatterns = listOf("*")  // Permite QUALQUER origem
allowCredentials = true               // Envia credenciais para qualquer origem!
```

**Problema:** Esta combinação é uma vulnerabilidade de segurança. Quando `allowCredentials = true`, não se deve usar `*` para origens - isso permite ataques CSRF de qualquer site.

**Solução:**
```kotlin
// Em produção, restringir para origens específicas
allowedOrigins = listOf(
    "https://app.smartbet.com.br",
    "https://smartbet.com.br"
)
allowCredentials = true
```

**Impacto:** Vulnerabilidade de segurança em produção.

---

### 2. OutOfMemoryError no Analytics

**Arquivo:** `src/main/kotlin/com/smartbet/application/usecase/AnalyticsService.kt:25-26`

```kotlin
val pageable = PageRequest.of(0, Int.MAX_VALUE)  // Carrega TODOS os tickets!
val allTickets = ticketRepository.findByUserId(userId, pageable).content
```

**Problema:** Carrega todos os tickets do usuário em memória. Um usuário com milhares de apostas causa OutOfMemoryError.

**Ocorrências:**
- Linha 25: `getOverallPerformance()`
- Linha 165: `getPerformanceByProvider()`

**Solução:** Usar agregação no banco de dados:
```kotlin
// Criar query no repository
@Query("""
    SELECT
        COUNT(*) as totalBets,
        SUM(CASE WHEN t.ticketStatus != 'OPEN' THEN 1 ELSE 0 END) as settledBets,
        SUM(CASE WHEN t.financialStatus IN ('FULL_WIN', 'PARTIAL_WIN') THEN 1 ELSE 0 END) as wins,
        SUM(t.stake) as totalStaked,
        SUM(t.actualPayout) as totalReturns
    FROM BetTicketEntity t
    WHERE t.userId = :userId
""")
fun getPerformanceStats(userId: Long): PerformanceStatsProjection
```

**Impacto:** Crash da aplicação com usuários ativos.

---

### 3. Force Unwrap (NullPointerException)

**Arquivos:**
- `src/main/kotlin/com/smartbet/application/usecase/TicketService.kt:52` - `provider.id!!`
- `src/main/kotlin/com/smartbet/application/usecase/TicketService.kt:140` - `provider.id!!`
- `src/main/kotlin/com/smartbet/application/dto/TicketDtos.kt:83` - `ticket.id!!`

**Problema:** Se o ID for null (entidade não persistida), a aplicação crasha com NullPointerException.

**Solução:**
```kotlin
// Opção 1: Validar antes
val providerId = provider.id
    ?: throw IllegalStateException("Provider deve ter ID válido")

// Opção 2: Usar requireNotNull com mensagem
val providerId = requireNotNull(provider.id) { "Provider ID não pode ser null" }
```

**Impacto:** Crash em runtime.

---

## Problemas de Design

### 4. Violação de Clean Architecture

**Arquivo:** `src/main/kotlin/com/smartbet/application/usecase/TicketService.kt:9-10`

```kotlin
import com.smartbet.infrastructure.persistence.entity.BetSelectionEntity
import com.smartbet.infrastructure.persistence.entity.BetTicketEntity
```

**Problema:** A camada de Application importa diretamente entidades de Infrastructure. Isso viola o princípio de inversão de dependência.

**Fluxo correto:**
```
Application → Domain ← Infrastructure
```

**Solução:**
1. Criar interfaces de Repository no Domain
2. Application depende apenas de entidades Domain
3. Infrastructure implementa as interfaces

```kotlin
// domain/port/TicketRepository.kt
interface TicketRepository {
    fun save(ticket: BetTicket): BetTicket
    fun findById(id: Long): BetTicket?
    fun findByUserId(userId: Long, pageable: Pageable): Page<BetTicket>
}

// infrastructure/persistence/JpaTicketRepository.kt
@Repository
class JpaTicketRepository(
    private val jpaRepository: BetTicketJpaRepository
) : TicketRepository {
    override fun save(ticket: BetTicket): BetTicket {
        val entity = BetTicketEntity.fromDomain(ticket)
        return jpaRepository.save(entity).toDomain()
    }
}
```

---

### 5. Código Duplicado - Refresh de Tickets

**Arquivo:** `src/main/kotlin/com/smartbet/application/usecase/TicketService.kt:303-412`

**Problema:** `refreshOpenTickets()` e `refreshAllOpenTickets()` têm ~90% de código idêntico.

**Solução:**
```kotlin
fun refreshOpenTickets(userId: Long): RefreshResult {
    val tickets = ticketRepository.findOpenTicketsByUserId(userId)
    return processRefresh(tickets, "user $userId")
}

fun refreshAllOpenTickets(): RefreshResult {
    val tickets = ticketRepository.findAllOpenTicketsWithSourceUrl()
    return processRefresh(tickets, "all users")
}

private fun processRefresh(tickets: List<BetTicketEntity>, context: String): RefreshResult {
    logger.info("Starting refresh of {} open tickets for {}", tickets.size, context)

    if (tickets.isEmpty()) {
        return RefreshResult(totalProcessed = 0, updated = 0, unchanged = 0, errors = 0)
    }

    var updated = 0
    var unchanged = 0
    val errorDetails = mutableListOf<RefreshError>()

    for (ticket in tickets) {
        try {
            if (refreshSingleTicket(ticket)) updated++ else unchanged++
        } catch (e: Exception) {
            logger.error("Error refreshing ticket {}: {}", ticket.id, e.message)
            errorDetails.add(RefreshError(ticket.id!!, ticket.externalTicketId, e.message ?: "Unknown"))
        }
    }

    return RefreshResult(tickets.size, updated, unchanged, errorDetails.size, errorDetails)
}
```

---

### 6. Violação de SRP (Single Responsibility Principle)

**Arquivo:** `src/main/kotlin/com/smartbet/application/usecase/TicketService.kt` (484 linhas)

**Problema:** Uma única classe fazendo múltiplas responsabilidades:
- Importação de bilhetes (parsing, validação, persistência)
- CRUD de bilhetes
- Refresh de status
- Integração com APIs externas

**Solução:** Separar em classes focadas:
```
TicketImportService    - importFromUrl()
TicketCrudService      - create(), list(), getById(), update(), delete()
TicketRefreshService   - refreshOpenTickets(), refreshAllOpenTickets()
```

---

## Problemas de Performance

### 7. N+1 Query no Analytics

**Arquivo:** `src/main/kotlin/com/smartbet/application/usecase/AnalyticsService.kt:167`

```kotlin
val providers = providerRepository.findAll().associateBy { it.id }
```

**Problema:** Para cada chamada de analytics, carrega TODOS os providers do sistema.

**Solução:** Usar JOIN FETCH ou carregar apenas providers necessários:
```kotlin
@Query("""
    SELECT DISTINCT t.providerId FROM BetTicketEntity t
    WHERE t.userId = :userId AND t.ticketStatus != 'OPEN'
""")
fun findDistinctProviderIdsByUserId(userId: Long): List<Long>
```

---

### 8. Falta de Índices no Banco de Dados

**Arquivo:** `src/main/kotlin/com/smartbet/infrastructure/persistence/entity/BetTicketEntity.kt`

**Campos sem índice que são frequentemente filtrados:**
- `userId` - toda listagem filtra por usuário
- `ticketStatus` - job de refresh filtra por OPEN
- `providerId` - analytics agrupa por provider
- `(externalTicketId, providerId)` - verificação de duplicatas

**Solução:** Criar migration V4:
```sql
-- V4__add_performance_indexes.sql

CREATE INDEX idx_bet_tickets_user_id
    ON betting.bet_tickets(user_id);

CREATE INDEX idx_bet_tickets_user_status
    ON betting.bet_tickets(user_id, ticket_status);

CREATE INDEX idx_bet_tickets_external_provider
    ON betting.bet_tickets(external_ticket_id, provider_id);

CREATE INDEX idx_bet_selections_ticket_id
    ON betting.bet_selections(ticket_id);
```

---

### 9. Chamadas HTTP Síncronas Sequenciais

**Arquivo:** `src/main/kotlin/com/smartbet/application/usecase/TicketService.kt:383-399`

```kotlin
for (ticket in allOpenTickets) {
    try {
        val wasUpdated = refreshSingleTicket(ticket)  // HTTP call síncrono
        // ...
    }
}
```

**Problema:** Se houver 100 tickets para atualizar, pode levar 100 × 30s timeout = 50 minutos.

**Solução:** Usar coroutines com limite de concorrência:
```kotlin
suspend fun refreshAllOpenTicketsAsync(): RefreshResult = coroutineScope {
    val tickets = ticketRepository.findAllOpenTicketsWithSourceUrl()

    val results = tickets
        .map { ticket ->
            async(Dispatchers.IO) {
                runCatching { refreshSingleTicket(ticket) }
            }
        }
        .awaitAll()

    // Agregar resultados...
}
```

---

## Problemas de Validação

### 10. DTOs de Ticket sem Validação

**Arquivo:** `src/main/kotlin/com/smartbet/application/dto/TicketDtos.kt:12-50`

```kotlin
data class ImportTicketRequest(
    val url: String,           // Sem @NotBlank - aceita string vazia
    val bankrollId: Long? = null
)

data class CreateManualTicketRequest(
    val stake: BigDecimal,     // Sem @DecimalMin - aceita valores negativos!
    val totalOdd: BigDecimal,  // Sem validação de range
    // ...
)
```

**Solução:**
```kotlin
data class ImportTicketRequest(
    @field:NotBlank(message = "URL é obrigatória")
    @field:Pattern(
        regexp = "^https?://.+",
        message = "URL deve ser válida"
    )
    val url: String,

    @field:Positive(message = "Bankroll ID deve ser positivo")
    val bankrollId: Long? = null
)

data class CreateManualTicketRequest(
    @field:Positive(message = "Provider ID é obrigatório")
    val providerId: Long,

    @field:DecimalMin(value = "0.01", message = "Stake deve ser maior que 0")
    val stake: BigDecimal,

    @field:DecimalMin(value = "1.01", message = "Odd deve ser maior que 1")
    val totalOdd: BigDecimal,

    @field:NotEmpty(message = "Deve ter pelo menos uma seleção")
    val selections: List<CreateSelectionRequest>
)
```

---

## Problemas de Segurança

### 11. Exceção de Autorização Inadequada

**Arquivo:** `src/main/kotlin/com/smartbet/application/usecase/TicketService.kt:220, 239, 280`

```kotlin
throw IllegalAccessException("Acesso negado ao bilhete: $ticketId")
```

**Problema:** `IllegalAccessException` é uma exceção de reflexão Java, não semântica de segurança.

**Solução:**
```kotlin
// presentation/exception/Exceptions.kt
class UnauthorizedAccessException(message: String) : RuntimeException(message)
class ResourceNotFoundException(message: String) : RuntimeException(message)

// GlobalExceptionHandler
@ExceptionHandler(UnauthorizedAccessException::class)
fun handleUnauthorized(e: UnauthorizedAccessException): ResponseEntity<ErrorResponse> {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse("ACCESS_DENIED", e.message))
}
```

---

### 12. Falta de Rate Limiting

**Arquivo:** `src/main/kotlin/com/smartbet/infrastructure/config/SecurityConfig.kt:39-41`

```kotlin
.requestMatchers("/api/auth/login").permitAll()
.requestMatchers("/api/auth/register").permitAll()
```

**Problema:** Endpoints públicos sem rate limiting permitem:
- Ataques de força bruta em login
- Spam de registros
- DoS

**Solução:** Adicionar Bucket4j ou similar:
```kotlin
// build.gradle.kts
implementation("com.giffing.bucket4j.spring.boot.starter:bucket4j-spring-boot-starter:0.9.1")

// application.yml
bucket4j:
  enabled: true
  filters:
    - cache-name: rate-limit
      url: /api/auth/login
      rate-limits:
        - bandwidths:
            - capacity: 5
              time: 1
              unit: minutes
```

---

## Problemas de Código

### 13. Magic Numbers

**Arquivo:** `src/main/kotlin/com/smartbet/infrastructure/config/SecurityConfig.kt:82`

```kotlin
maxAge = 3600L  // O que significa 3600?
```

**Solução:**
```kotlin
companion object {
    const val CORS_MAX_AGE_SECONDS = 3600L
}

// uso
maxAge = CORS_MAX_AGE_SECONDS
```

---

### 14. Tratamento de Erro Genérico

**Arquivo:** `src/main/kotlin/com/smartbet/application/usecase/TicketService.kt:332-339`

```kotlin
catch (e: Exception) {
    logger.error("Error refreshing ticket {}: {}", ticket.id, e.message)
}
```

**Problema:** Captura toda exceção sem diferenciar tipos.

**Solução:**
```kotlin
catch (e: ProviderApiException) {
    logger.warn("Provider API error for ticket {}: {}", ticket.id, e.message)
    errorDetails.add(RefreshError(ticket.id!!, ticket.externalTicketId, "API indisponível"))
} catch (e: SocketTimeoutException) {
    logger.warn("Timeout refreshing ticket {}", ticket.id)
    errorDetails.add(RefreshError(ticket.id!!, ticket.externalTicketId, "Timeout"))
} catch (e: Exception) {
    logger.error("Unexpected error refreshing ticket {}", ticket.id, e)
    errorDetails.add(RefreshError(ticket.id!!, ticket.externalTicketId, "Erro inesperado"))
}
```

---

### 15. Falta de Optimistic Locking

**Arquivo:** `src/main/kotlin/com/smartbet/infrastructure/persistence/entity/BetTicketEntity.kt`

**Problema:** Sem campo `@Version`, duas requisições simultâneas de refresh podem sobrescrever dados.

**Solução:**
```kotlin
@Entity
@Table(name = "bet_tickets", schema = "betting")
class BetTicketEntity(
    // ... outros campos

    @Version
    @Column(name = "version")
    val version: Long = 0,

    // ...
)
```

Migration:
```sql
ALTER TABLE betting.bet_tickets ADD COLUMN version BIGINT DEFAULT 0;
```

---

## Resumo por Prioridade

| Prioridade | Item | Esforço | Risco |
|------------|------|---------|-------|
| P0 | CORS Inseguro | Baixo | Crítico |
| P0 | OOM no Analytics | Médio | Crítico |
| P0 | Force Unwrap (!!) | Baixo | Alto |
| P1 | Índices no Banco | Baixo | Médio |
| P1 | Validação DTOs | Baixo | Médio |
| P1 | Rate Limiting | Médio | Médio |
| P2 | Clean Architecture | Alto | Baixo |
| P2 | Código Duplicado | Médio | Baixo |
| P2 | HTTP Assíncrono | Médio | Baixo |
| P3 | SRP/Refatoração | Alto | Baixo |
| P3 | Optimistic Locking | Baixo | Baixo |

---

## Checklist de Correção

- [ ] Corrigir configuração CORS para produção
- [ ] Implementar agregação no banco para Analytics
- [ ] Substituir `!!` por validações explícitas
- [ ] Criar migration com índices de performance
- [ ] Adicionar validações nos DTOs de Ticket
- [ ] Implementar rate limiting nos endpoints de auth
- [ ] Extrair código duplicado de refresh
- [ ] Criar exceções customizadas de autorização
- [ ] Adicionar @Version para optimistic locking
- [ ] Extrair constantes de magic numbers
