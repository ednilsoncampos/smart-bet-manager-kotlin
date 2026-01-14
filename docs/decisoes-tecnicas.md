# Decisões Técnicas - Smart Bet Manager

Este documento registra as principais decisões técnicas tomadas durante o desenvolvimento do Smart Bet Manager, incluindo o contexto, alternativas consideradas e justificativas.

## Backend

### Linguagem: Kotlin (JVM)

**Contexto:** Escolha da linguagem principal para o backend.

**Alternativas consideradas:**
- Java 21
- Kotlin (JVM)
- Node.js/TypeScript

**Decisão:** Kotlin (JVM)

**Justificativa:** Kotlin oferece sintaxe mais concisa que Java, null-safety nativo, e interoperabilidade total com o ecossistema Java/Spring. Além disso, permite compartilhamento de conhecimento com o desenvolvimento mobile (KMP).

### Framework: Spring Boot 3.2

**Contexto:** Escolha do framework web para o backend.

**Alternativas consideradas:**
- Spring Boot
- Ktor
- Quarkus

**Decisão:** Spring Boot 3.2

**Justificativa:** Spring Boot é o framework mais maduro para aplicações enterprise em JVM, com excelente documentação, comunidade ativa e vasto ecossistema de bibliotecas. A versão 3.2 traz suporte nativo a virtual threads e melhorias de performance.

### Banco de Dados: PostgreSQL

**Contexto:** Escolha do banco de dados relacional.

**Alternativas consideradas:**
- PostgreSQL
- MySQL
- MongoDB

**Decisão:** PostgreSQL 16

**Justificativa:** PostgreSQL oferece recursos avançados como JSONB, full-text search e extensões. É altamente confiável, tem excelente performance e é bem suportado por provedores cloud como DigitalOcean.

### Migrations: Flyway

**Contexto:** Ferramenta para versionamento de schema do banco.

**Alternativas consideradas:**
- Flyway
- Liquibase
- Manual

**Decisão:** Flyway

**Justificativa:** Flyway é simples, baseado em SQL puro, e integra nativamente com Spring Boot. Permite controle de versão do schema de forma transparente.

### Autenticação: JWT

**Contexto:** Mecanismo de autenticação para a API.

**Alternativas consideradas:**
- JWT (stateless)
- Sessions (stateful)
- OAuth2/OpenID Connect

**Decisão:** JWT com refresh tokens

**Justificativa:** JWT permite autenticação stateless, ideal para APIs REST e aplicações mobile. Refresh tokens garantem segurança sem forçar re-login frequente.

## Mobile

### Framework: Kotlin Multiplatform (KMP)

**Contexto:** Escolha da tecnologia para desenvolvimento mobile.

**Alternativas consideradas:**
- Kotlin Multiplatform + Compose
- Flutter
- React Native
- Nativo puro (Android + iOS separados)

**Decisão:** Kotlin Multiplatform com Jetpack Compose

**Justificativa:** KMP permite compartilhar lógica de negócio entre plataformas mantendo UI nativa. Compose oferece desenvolvimento declarativo moderno. A escolha alinha com o backend em Kotlin, maximizando reuso de conhecimento.

### UI: Jetpack Compose

**Contexto:** Framework de UI para Android.

**Alternativas consideradas:**
- Jetpack Compose
- XML Views tradicionais
- Compose Multiplatform

**Decisão:** Jetpack Compose

**Justificativa:** Compose é o futuro do desenvolvimento Android, oferece desenvolvimento declarativo, melhor testabilidade e integração nativa com Kotlin. Google recomenda Compose para novos projetos.

### HTTP Client: Ktor

**Contexto:** Cliente HTTP para o módulo shared.

**Alternativas consideradas:**
- Ktor
- Retrofit
- OkHttp direto

**Decisão:** Ktor

**Justificativa:** Ktor é multiplataforma (funciona em KMP), escrito em Kotlin puro, e oferece API moderna com coroutines. Ideal para o módulo shared.

### Cache Local: SQLDelight

**Contexto:** Persistência local no mobile.

**Alternativas consideradas:**
- SQLDelight
- Room
- Realm

**Decisão:** SQLDelight

**Justificativa:** SQLDelight é multiplataforma (funciona em KMP), gera código type-safe a partir de SQL, e oferece excelente performance. Room é Android-only.

### DI: Hilt

**Contexto:** Framework de injeção de dependências.

**Alternativas consideradas:**
- Hilt
- Koin
- Manual

**Decisão:** Hilt

**Justificativa:** Hilt é a solução oficial do Google para DI em Android, integra com ViewModels e Navigation, e oferece validação em tempo de compilação.

## Infraestrutura

### Deploy: DigitalOcean Kubernetes (DOKS)

**Contexto:** Plataforma de hospedagem para produção.

**Alternativas consideradas:**
- DigitalOcean Kubernetes
- DigitalOcean App Platform
- AWS EKS
- Google Cloud Run

**Decisão:** DigitalOcean Kubernetes

**Justificativa:** DOKS oferece Kubernetes gerenciado com custo acessível, boa documentação e integração com outros serviços DigitalOcean (Managed Database, Spaces). Kubernetes garante escalabilidade e portabilidade.

### Container: Docker

**Contexto:** Containerização da aplicação.

**Decisão:** Docker com multi-stage build

**Justificativa:** Docker é o padrão de mercado para containers. Multi-stage build reduz tamanho da imagem final e melhora segurança ao não incluir ferramentas de build.

### CI/CD: GitHub Actions

**Contexto:** Pipeline de integração e deploy contínuo.

**Alternativas consideradas:**
- GitHub Actions
- GitLab CI
- Jenkins

**Decisão:** GitHub Actions

**Justificativa:** GitHub Actions integra nativamente com o repositório, oferece runners gratuitos para projetos open-source, e tem marketplace rico de actions prontas.

## Observabilidade

### Logging: Logback + JSON

**Contexto:** Formato e estrutura de logs.

**Decisão:** Logback com formato JSON em produção

**Justificativa:** Logs em JSON são facilmente parseáveis por ferramentas de agregação (ELK, Loki). Logback é o padrão do Spring Boot e oferece configuração flexível por profile.

### Métricas: Prometheus

**Contexto:** Coleta e exposição de métricas.

**Decisão:** Micrometer com Prometheus registry

**Justificativa:** Prometheus é o padrão de facto para métricas em Kubernetes. Micrometer abstrai a coleta e integra nativamente com Spring Boot Actuator.

### Health Checks: Spring Actuator

**Contexto:** Verificação de saúde da aplicação.

**Decisão:** Spring Boot Actuator com probes Kubernetes

**Justificativa:** Actuator oferece endpoints prontos para liveness e readiness probes, essenciais para Kubernetes gerenciar o ciclo de vida dos pods.

## Segurança

### Validação: Jakarta Validation

**Contexto:** Validação de dados de entrada.

**Decisão:** `@Valid` com Jakarta Bean Validation

**Justificativa:** Validação declarativa com anotações é clara, testável e integra com Spring MVC para retornar erros padronizados.

### CORS: Configuração explícita

**Contexto:** Controle de acesso cross-origin.

**Decisão:** CORS configurado no SecurityConfig

**Justificativa:** Configuração explícita garante que apenas origens autorizadas acessem a API, prevenindo ataques CSRF.

### Senhas: BCrypt

**Contexto:** Armazenamento seguro de senhas.

**Decisão:** BCrypt com cost factor 10

**Justificativa:** BCrypt é resistente a ataques de força bruta e rainbow tables. Cost factor 10 oferece bom equilíbrio entre segurança e performance.
