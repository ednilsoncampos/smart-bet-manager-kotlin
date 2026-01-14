# Smart Bet Manager - Backend

Sistema de gerenciamento de apostas esportivas desenvolvido em **Kotlin + Spring Boot + PostgreSQL**, seguindo os princípios de **Clean Architecture**.

## Funcionalidades

- **Autenticação JWT**: Login, registro e refresh token
- **Importação de Bilhetes**: Importa bilhetes automaticamente via URL compartilhada (Superbet, Betano)
- **Gestão de Banca**: Controle de depósitos, saques e saldo por casa de apostas
- **Análise de Performance**: Métricas de ROI, taxa de acerto, performance por mercado/torneio
- **Evolução de Saldo**: Histórico de saldo para gráficos
- **Status Financeiro Inteligente**: 5 níveis de resultado (FULL_WIN, PARTIAL_WIN, BREAK_EVEN, PARTIAL_LOSS, TOTAL_LOSS)
- **Strategy Pattern**: Arquitetura extensível para adicionar novas casas de apostas

## Tecnologias

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| Kotlin | 1.9.21 | Linguagem principal |
| Spring Boot | 3.2.1 | Framework web |
| PostgreSQL | 16 | Banco de dados |
| Flyway | 10.4.1 | Migrations |
| JJWT | 0.12.3 | JWT Authentication |
| JUnit 5 | 5.10.1 | Testes unitários |
| MockK | 1.13.8 | Mocking para Kotlin |
| OkHttp | 4.12.0 | Cliente HTTP |
| Swagger/OpenAPI | 2.3.0 | Documentação da API |

## Arquitetura

```
src/main/kotlin/com/smartbet/
├── domain/                    # Camada de Domínio
│   ├── entity/               # Entidades de negócio
│   ├── enum/                 # Enums do domínio
│   └── service/              # Serviços de domínio (regras de negócio puras)
├── application/              # Camada de Aplicação
│   ├── dto/                  # Data Transfer Objects
│   └── usecase/              # Casos de uso / Services
├── infrastructure/           # Camada de Infraestrutura
│   ├── config/               # Configurações Spring
│   ├── persistence/          # JPA Entities e Repositories
│   ├── security/             # JWT Service e Filters
│   └── provider/             # Integrações com casas de apostas
│       ├── gateway/          # HTTP Gateway
│       └── strategy/         # Strategy Pattern para parsers
└── presentation/             # Camada de Apresentação
    ├── controller/           # REST Controllers
    └── exception/            # Exception Handlers
```

## Quick Start

### Pré-requisitos

- JDK 21+
- Docker e Docker Compose
- Gradle 8.5+ (ou use o wrapper incluído)

### Executando com Docker

```bash
# Subir todos os serviços
docker-compose up -d

# Ver logs
docker-compose logs -f api

# Parar serviços
docker-compose down
```

A API estará disponível em `http://localhost:8080`

### Executando Localmente

```bash
# 1. Subir apenas o banco de dados
docker-compose up -d postgres

# 2. Copiar variáveis de ambiente
cp .env.example .env

# 3. Executar a aplicação
./gradlew bootRun

# 4. Executar testes
./gradlew test
```

## API Endpoints

### Health Check (Público)
```
GET /api/health
```

### Autenticação (Público)
```
POST /api/auth/register    # Registrar novo usuário
POST /api/auth/login       # Login
POST /api/auth/refresh     # Renovar access token
GET  /api/auth/me          # Dados do usuário autenticado (requer JWT)
POST /api/auth/change-password  # Alterar senha (requer JWT)
POST /api/auth/logout      # Logout
```

### Tickets (Bilhetes) - Requer JWT
```
POST /api/tickets/import     # Importar via URL
POST /api/tickets            # Criar manualmente
GET  /api/tickets            # Listar (com filtros)
GET  /api/tickets/{id}       # Buscar por ID
PATCH /api/tickets/{id}/status  # Atualizar status
DELETE /api/tickets/{id}     # Deletar
```

### Bankrolls (Bancas) - Requer JWT
```
POST /api/bankrolls          # Criar banca
GET  /api/bankrolls          # Listar bancas
GET  /api/bankrolls/summary  # Resumo consolidado
GET  /api/bankrolls/{id}     # Buscar por ID
POST /api/bankrolls/{id}/transactions  # Registrar transação
GET  /api/bankrolls/{id}/transactions  # Listar transações
DELETE /api/bankrolls/{id}   # Desativar banca
```

### Analytics (Análise) - Requer JWT
```
GET /api/analytics/overall       # Performance geral
GET /api/analytics/by-tournament # Por torneio
GET /api/analytics/by-market     # Por tipo de mercado
GET /api/analytics/by-provider   # Por casa de apostas
GET /api/analytics/bankroll-evolution  # Evolução consolidada de saldo
GET /api/analytics/bankroll-evolution/{bankrollId}  # Evolução de uma banca
```

### Providers (Casas de Apostas) - Público
```
GET  /api/providers          # Listar todas
GET  /api/providers/active   # Listar ativas
GET  /api/providers/supported # Listar suportadas (com parser)
GET  /api/providers/{id}     # Buscar por ID
POST /api/providers/check-url # Verificar URL
```

## Documentação da API

Acesse a documentação Swagger em:
```
http://localhost:8080/swagger-ui.html
```

## Autenticação JWT

### Fluxo de Autenticação

1. **Registro**: `POST /api/auth/register`
2. **Login**: `POST /api/auth/login` → Retorna `accessToken` e `refreshToken`
3. **Usar API**: Enviar header `Authorization: Bearer {accessToken}`
4. **Renovar Token**: `POST /api/auth/refresh` com `refreshToken`

### Exemplo de Registro

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@email.com",
    "password": "senha123"
  }'
```

### Exemplo de Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@email.com",
    "password": "senha123"
  }'
```

**Resposta:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "name": "João Silva",
    "email": "joao@email.com",
    "createdAt": 1704067200000
  }
}
```

### Usando o Token

```bash
curl -X GET http://localhost:8080/api/tickets \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## Exemplos de Uso

### Importar Bilhete via URL

```bash
curl -X POST http://localhost:8080/api/tickets/import \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "url": "https://superbet.bet.br/bilhete-compartilhado/ABC123"
  }'
```

### Criar Bilhete Manual

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "providerId": 1,
    "betType": "SINGLE",
    "stake": 100.00,
    "totalOdd": 2.50,
    "selections": [
      {
        "eventName": "Flamengo x Palmeiras",
        "tournamentName": "Brasileirão",
        "marketType": "Resultado Final",
        "selection": "Flamengo",
        "odd": 2.50
      }
    ]
  }'
```

### Criar Banca
```bash
curl -X POST http://localhost:8080/api/bankrolls \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "name": "Banca Principal",
    "providerId": 1,
    "currency": "BRL",
    "initialDeposit": 1000.00
  }'
```

### Obter Evolução de Saldo

```bash
curl -X GET "http://localhost:8080/api/analytics/bankroll-evolution?granularity=day" \
  -H "Authorization: Bearer {token}"
```

## Status Financeiro

O sistema calcula automaticamente o status financeiro real de cada aposta:

| Status | Descrição | Condição |
|--------|-----------|----------|
| `FULL_WIN` | Ganho total | Retorno >= Potencial |
| `PARTIAL_WIN` | Ganho parcial | Stake < Retorno < Potencial |
| `BREAK_EVEN` | Empate | Retorno = Stake |
| `PARTIAL_LOSS` | Perda parcial | 0 < Retorno < Stake |
| `TOTAL_LOSS` | Perda total | Retorno = 0 |
| `PENDING` | Pendente | Bilhete aberto |

## Adicionando Nova Casa de Apostas

1. Crie uma nova Strategy em `infrastructure/provider/strategy/`:

```kotlin
@Component
class NovaCasaStrategy(
    private val objectMapper: ObjectMapper
) : BettingProviderStrategy {
    
    override val slug = "novacasa"
    override val name = "Nova Casa"
    
    override val urlPatterns = listOf(
        Regex("""novacasa\.com\.br/bilhete/([A-Za-z0-9]+)""")
    )
    
    override val defaultApiTemplate = 
        "https://api.novacasa.com.br/betslip/{CODE}"
    
    override fun extractTicketCode(url: String): String? {
        // Implementar extração do código
    }
    
    override fun parseResponse(responseBody: String): ParsedTicketData {
        // Implementar parsing da resposta
    }
}
```

2. Adicione o provider no banco via migration:

```sql
INSERT INTO betting_providers (slug, name, api_url_template, website_url) 
VALUES ('novacasa', 'Nova Casa', 'https://api.novacasa.com.br/betslip/{CODE}', 'https://novacasa.com.br');
```

## Testes

```bash
# Executar todos os testes
./gradlew test

# Executar com relatório
./gradlew test jacocoTestReport

# Executar testes específicos
./gradlew test --tests "BetStatusCalculatorTest"
```

## Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `DB_HOST` | Host do PostgreSQL | `localhost` |
| `DB_PORT` | Porta do PostgreSQL | `5432` |
| `DB_NAME` | Nome do banco | `smartbet` |
| `DB_USER` | Usuário do banco | `smartbet` |
| `DB_PASSWORD` | Senha do banco | `smartbet` |
| `SERVER_PORT` | Porta do servidor | `8080` |
| `JWT_SECRET` | Chave secreta para JWT | (gerada) |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Expiração do access token (ms) | `3600000` (1h) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Expiração do refresh token (ms) | `604800000` (7d) |

## Próximos Passos

- [x] Implementar autenticação JWT
- [x] Endpoint de evolução de saldo
- [ ] Adicionar mais casas de apostas (Bet365, Betfair, etc.)
- [ ] Implementar notificações de resultados
- [ ] Suporte a apostas de sistema (múltiplas combinações)
- [ ] Integração com APIs de resultados em tempo real

## Licença

Este projeto é privado e de uso pessoal.

---

Desenvolvido com Kotlin + Spring Boot
