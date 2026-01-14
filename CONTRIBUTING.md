# Guia de Contribuição - Smart Bet Manager

Obrigado por considerar contribuir com o Smart Bet Manager! Este documento descreve as diretrizes para desenvolvimento e contribuição.

## Setup do Ambiente

### Pré-requisitos

| Ferramenta | Versão Mínima |
|------------|---------------|
| JDK | 17 |
| Kotlin | 1.9.x |
| Gradle | 8.5 |
| Docker | 24.x |
| PostgreSQL | 16 |

### Configuração Local

1. Clone o repositório:

```bash
git clone https://github.com/ednilsoncampos/smart-bet-manager-kotlin.git
cd smart-bet-manager-kotlin
```

2. Copie o arquivo de ambiente:

```bash
cp .env.example .env
```

3. Edite o `.env` com suas configurações locais.

4. Inicie o banco de dados com Docker:

```bash
docker-compose up -d postgres
```

5. Execute as migrations:

```bash
./gradlew flywayMigrate
```

6. Inicie a aplicação:

```bash
./gradlew bootRun
```

7. Acesse a documentação da API:

```
http://localhost:8080/swagger-ui.html
```

## Padrões de Código

### Estilo de Código

O projeto segue as convenções oficiais do Kotlin. Principais pontos:

- Indentação com 4 espaços
- Nomes de classes em PascalCase
- Nomes de funções e variáveis em camelCase
- Constantes em SCREAMING_SNAKE_CASE

### Estrutura de Commits

Utilizamos **Conventional Commits** para mensagens de commit:

```
<tipo>(<escopo>): <descrição>

[corpo opcional]

[rodapé opcional]
```

**Tipos permitidos:**

| Tipo | Descrição |
|------|-----------|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `docs` | Documentação |
| `style` | Formatação (sem mudança de código) |
| `refactor` | Refatoração |
| `test` | Adição/correção de testes |
| `chore` | Manutenção (build, deps, etc.) |

**Exemplos:**

```
feat(tickets): adiciona endpoint de importação via URL

fix(auth): corrige validação de refresh token expirado

docs(readme): atualiza instruções de instalação
```

## Fluxo de Desenvolvimento

### Branches

| Branch | Propósito |
|--------|-----------|
| `main` | Produção, sempre estável |
| `develop` | Desenvolvimento, integração |
| `feature/*` | Novas funcionalidades |
| `fix/*` | Correções de bugs |
| `hotfix/*` | Correções urgentes em produção |

### Processo de PR

1. Crie uma branch a partir de `develop`:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/minha-feature
```

2. Desenvolva e faça commits seguindo Conventional Commits.

3. Garanta que os testes passam:

```bash
./gradlew test
```

4. Garanta que o build está verde:

```bash
./gradlew build
```

5. Abra um Pull Request para `develop`.

6. Aguarde revisão de código.

7. Após aprovação, faça merge.

### Critérios de Aceitação para PRs

Antes de aprovar um PR, verifique:

- [ ] Código compila sem erros
- [ ] Todos os testes passam
- [ ] Cobertura de testes >= 80%
- [ ] Sem warnings críticos
- [ ] Documentação atualizada (se aplicável)
- [ ] Commit messages seguem Conventional Commits
- [ ] Código segue padrões do projeto

## Testes

### Executando Testes

```bash
# Todos os testes
./gradlew test

# Testes específicos
./gradlew test --tests "BetanoStrategyTest"

# Com cobertura
./gradlew test jacocoTestReport
```

### Estrutura de Testes

Os testes estão em `src/test/kotlin/` espelhando a estrutura de `src/main/kotlin/`.

**Convenções:**
- Nome do arquivo: `{Classe}Test.kt`
- Métodos: `should{Comportamento}When{Condição}`

**Exemplo:**

```kotlin
class BetStatusCalculatorTest {
    @Test
    fun `should return WON when all selections are won`() {
        // Arrange
        val selections = listOf(
            createSelection(SelectionStatus.WON),
            createSelection(SelectionStatus.WON)
        )
        
        // Act
        val result = calculator.calculate(selections)
        
        // Assert
        assertEquals(TicketStatus.WON, result)
    }
}
```

## Arquitetura

Consulte os documentos de arquitetura em `docs/`:

- `arquitetura-backend.md` - Arquitetura do backend
- `arquitetura-mobile.md` - Arquitetura do mobile
- `decisoes-tecnicas.md` - Decisões técnicas

## Dúvidas

Se tiver dúvidas, abra uma issue ou entre em contato com os mantenedores.
