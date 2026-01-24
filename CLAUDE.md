# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests "BetStatusCalculatorTest"

# Run tests with coverage report
./gradlew test jacocoTestReport

# Build the project
./gradlew build

# Start PostgreSQL with Docker (required for local development)
docker-compose up -d postgres

# Full stack with Docker
docker-compose up -d
```

## Architecture Overview

This is a **Kotlin + Spring Boot** sports betting management system following **Clean Architecture** with 4 layers:

```
Presentation → Application → Domain → Infrastructure
(Controllers)  (Use Cases)   (Entities) (Persistence/External)
```

### Layer Responsibilities

- **Domain** (`domain/`): Pure business logic with no framework dependencies
  - `entity/`: Business entities (BetTicket, BetSelection, Bankroll, User, etc.)
  - `enum/`: Domain enums (TicketStatus, FinancialStatus, SelectionStatus, BetType, BetSide)
  - `service/`: Domain services (`BetStatusCalculator` - calculates financial outcomes)

- **Application** (`application/`): Use cases orchestrating domain logic
  - `usecase/`: Services like TicketService, BankrollService, AuthService, AnalyticsService
  - `dto/`: Request/Response DTOs

- **Infrastructure** (`infrastructure/`): Technical implementations
  - `persistence/`: JPA entities (separate from domain), repositories, mappers
  - `security/`: JWT authentication (JwtService, JwtAuthenticationFilter)
  - `provider/strategy/`: **Strategy Pattern** for betting site parsers (SuperbetStrategy, BetanoStrategy)
  - `provider/gateway/`: HTTP client (HttpGateway using OkHttp)
  - `job/`: Scheduled tasks (TicketRefreshJob)

- **Presentation** (`presentation/`): REST API layer
  - `controller/`: REST controllers
  - `exception/`: GlobalExceptionHandler

### Key Patterns

**Strategy Pattern for Betting Providers**: Each betting site (Superbet, Betano) has its own parser strategy implementing `BettingProviderStrategy`. To add a new provider:
1. Create a new strategy class in `infrastructure/provider/strategy/`
2. Implement `BettingProviderStrategy` interface
3. Add the provider to the database via Flyway migration

**Dual Entity Model**: Domain entities (`domain/entity/`) are separate from JPA entities (`infrastructure/persistence/entity/`). Mappers convert between them to keep the domain layer framework-agnostic.

**Financial Status Calculation**: `BetStatusCalculator` determines the real financial outcome (FULL_WIN, PARTIAL_WIN, BREAK_EVEN, PARTIAL_LOSS, TOTAL_LOSS) based on actual returns vs stake, handling complex scenarios like system bets and partial wins/losses.

## Database

- **PostgreSQL 16** with Flyway migrations in `src/main/resources/db/migration/`
- Tables use the `betting` schema (configured in V3 migration)
- Connection pool: HikariCP (max 10 connections)

## Testing

- **JUnit 5** with **MockK** for Kotlin mocking
- **TestContainers** for integration tests with real PostgreSQL
- Test naming convention: `should{Behavior}When{Condition}` using backtick syntax
- Test files mirror source structure in `src/test/kotlin/`

## API Documentation

Swagger UI available at `http://localhost:8080/swagger-ui.html` when running locally.

## Commit Convention

This project uses **Conventional Commits**:
- `feat`: New functionality
- `fix`: Bug fix
- `docs`: Documentation
- `refactor`: Code refactoring
- `test`: Adding/fixing tests
- `chore`: Maintenance (build, deps)

Example: `feat(tickets): add bulk import endpoint`

## Environment Variables

Key configuration in `.env`:
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`: PostgreSQL connection
- `JWT_SECRET`: Secret for signing JWT tokens
- `JWT_ACCESS_TOKEN_EXPIRATION`: Access token lifetime (default: 3600000ms / 1h)
- `JWT_REFRESH_TOKEN_EXPIRATION`: Refresh token lifetime (default: 604800000ms / 7d)

Profiles: `dev`, `staging`, `prod` (configured in `application-{profile}.yml`)
