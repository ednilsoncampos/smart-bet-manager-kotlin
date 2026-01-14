# Arquitetura do Backend - Smart Bet Manager

## Visão Geral

O backend do Smart Bet Manager segue os princípios da **Clean Architecture**, separando responsabilidades em camadas bem definidas. A aplicação é construída com Kotlin e Spring Boot 3.2, utilizando PostgreSQL como banco de dados.

## Stack Tecnológica

| Componente | Tecnologia | Versão |
|------------|------------|--------|
| Linguagem | Kotlin | 1.9.x |
| Framework | Spring Boot | 3.2.1 |
| Build Tool | Gradle | 8.5 |
| Banco de Dados | PostgreSQL | 16 |
| Migrations | Flyway | 10.x |
| Autenticação | JWT | - |
| Documentação | OpenAPI/Swagger | 3.x |
| HTTP Client | OkHttp | 4.12 |

## Estrutura de Camadas

```
src/main/kotlin/com/smartbet/
├── domain/           # Entidades e regras de negócio
├── application/      # Casos de uso e DTOs
├── infrastructure/   # Implementações técnicas
└── presentation/     # Controllers e handlers
```

### Camada de Domínio (`domain/`)

A camada de domínio contém as entidades de negócio e regras que são independentes de frameworks. Esta camada não possui dependências externas e representa o núcleo da aplicação.

**Entidades principais:**
- `User` - Representa um usuário do sistema
- `Bankroll` - Banca de apostas vinculada a uma casa
- `BankrollTransaction` - Transações financeiras na banca
- `BetTicket` - Bilhete de aposta
- `BetSelection` - Seleção individual dentro de um bilhete
- `BettingProvider` - Casa de apostas

**Enums de domínio:**
- `TicketStatus` - OPEN, WON, LOST, VOID, PARTIAL, CASHOUT
- `FinancialStatus` - PENDING, SETTLED
- `TransactionType` - DEPOSIT, WITHDRAWAL, BONUS, BET_WIN, BET_LOSS, CASHBACK
- `SelectionStatus` - PENDING, WON, LOST, VOID, HALF_WON, HALF_LOST

### Camada de Aplicação (`application/`)

A camada de aplicação contém os casos de uso (services) e os DTOs para transferência de dados. Esta camada orquestra as operações de negócio.

**Services:**
- `AuthService` - Autenticação e gerenciamento de usuários
- `BankrollService` - Operações com bancas
- `TicketService` - Operações com bilhetes
- `ProviderService` - Gerenciamento de casas de apostas
- `AnalyticsService` - Cálculos de performance

**DTOs:**
Os DTOs são organizados por domínio e seguem o padrão Request/Response:
- `AuthDtos.kt` - LoginRequest, RegisterRequest, AuthResponse
- `BankrollDtos.kt` - CreateBankrollRequest, BankrollResponse
- `TicketDtos.kt` - ImportTicketRequest, TicketResponse

### Camada de Infraestrutura (`infrastructure/`)

A camada de infraestrutura contém as implementações técnicas, incluindo persistência, segurança e integrações externas.

**Persistência:**
- Entidades JPA em `persistence/entity/`
- Repositórios Spring Data em `persistence/repository/`
- Mapeamento entre entidades de domínio e JPA

**Segurança:**
- `SecurityConfig` - Configuração do Spring Security
- `JwtService` - Geração e validação de tokens JWT
- `JwtAuthenticationFilter` - Filtro de autenticação

**Providers (Parsers):**
- `BettingProviderFactory` - Factory para estratégias de parsing
- `BettingProviderStrategy` - Interface para parsers
- `BetanoStrategy` - Parser específico para Betano
- `SuperbetStrategy` - Parser específico para Superbet

### Camada de Apresentação (`presentation/`)

A camada de apresentação contém os controllers REST e handlers de exceção.

**Controllers:**
- `AuthController` - `/api/auth/*`
- `BankrollController` - `/api/bankrolls/*`
- `TicketController` - `/api/tickets/*`
- `ProviderController` - `/api/providers/*`
- `AnalyticsController` - `/api/analytics/*`
- `HealthController` - `/health`

**Exception Handling:**
- `GlobalExceptionHandler` - Tratamento centralizado de exceções
- `ErrorResponse` - Formato padronizado de erros

## Fluxo de Dados

```
Request → Controller → Service → Repository → Database
                ↓
            Response ← DTO ← Entity
```

1. O **Controller** recebe a requisição HTTP e valida os dados de entrada
2. O **Service** executa a lógica de negócio
3. O **Repository** persiste ou recupera dados do banco
4. O resultado é convertido para **DTO** e retornado

## Padrões de Projeto

| Padrão | Uso |
|--------|-----|
| Factory | BettingProviderFactory para criar parsers |
| Strategy | BettingProviderStrategy para diferentes casas |
| Repository | Abstração de acesso a dados |
| DTO | Transferência de dados entre camadas |
| Dependency Injection | Spring IoC Container |

## Segurança

A autenticação é baseada em JWT (JSON Web Tokens):

1. Usuário faz login com email/senha
2. Backend valida credenciais e gera token JWT
3. Token é enviado em todas as requisições subsequentes
4. Filter intercepta requisições e valida token
5. Usuário autenticado é injetado no contexto

**Configurações de segurança:**
- Tokens expiram em 24 horas
- Refresh tokens expiram em 7 dias
- Senhas são hasheadas com BCrypt
- CORS configurado para origens permitidas

## Migrations (Flyway)

As migrations estão em `src/main/resources/db/migration/`:

| Versão | Descrição |
|--------|-----------|
| V1 | Schema inicial (users, bankrolls, tickets, etc.) |

**Convenção de nomes:** `V{versão}__{descricao}.sql`

## Multi-tenancy

O sistema implementa multi-tenancy por coluna, onde cada registro possui um `user_id` que identifica o proprietário. Todas as queries são filtradas pelo usuário autenticado, garantindo isolamento de dados.

**Tabelas com user_id:**
- `bankrolls`
- `bet_tickets`
- `bankroll_transactions`

## Testes

Os testes estão organizados em:
- `BetStatusCalculatorTest` - Testes de lógica de domínio
- `BetanoStrategyTest` - Testes do parser Betano
- `SuperbetStrategyTest` - Testes do parser Superbet
- `BettingProviderFactoryTest` - Testes da factory
- `TicketServiceTest` - Testes do service de tickets

**Cobertura mínima:** 80%
