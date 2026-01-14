# Arquitetura do Mobile - Smart Bet Manager

## Visão Geral

O aplicativo mobile do Smart Bet Manager é construído com **Kotlin Multiplatform (KMP)** e **Jetpack Compose**, permitindo compartilhamento de código entre Android e futuras plataformas. A arquitetura segue os princípios de Clean Architecture adaptados para mobile.

## Stack Tecnológica

| Componente | Tecnologia | Versão |
|------------|------------|--------|
| Framework | Kotlin Multiplatform | 1.9.x |
| UI | Jetpack Compose | 1.5.x |
| DI | Hilt | 2.48 |
| HTTP Client | Ktor | 2.3.x |
| Cache Local | SQLDelight | 2.0.x |
| Serialização | Kotlinx Serialization | 1.6.x |
| Navigation | Compose Navigation | 2.7.x |

## Estrutura do Projeto

```
smart-bet-manager-mobile/
├── shared/                    # Código compartilhado (KMP)
│   └── src/
│       └── commonMain/
│           └── kotlin/com/smartbet/shared/
│               ├── models/    # DTOs e modelos
│               ├── network/   # Cliente HTTP
│               └── repository/# Repositórios
├── androidApp/               # Aplicação Android
│   └── src/main/kotlin/com/smartbet/android/
│       ├── ui/               # Componentes de UI
│       │   ├── components/   # Componentes reutilizáveis
│       │   ├── navigation/   # Rotas e navegação
│       │   ├── screens/      # Telas
│       │   └── theme/        # Tema e cores
│       └── viewmodel/        # ViewModels
└── iosApp/                   # Aplicação iOS (futuro)
```

## Camadas da Arquitetura

### Camada Shared (KMP)

O módulo `shared` contém código que pode ser compartilhado entre plataformas.

**Models (`shared/models/`):**
Os modelos representam os dados que trafegam entre o app e a API. São anotados com `@Serializable` para serialização JSON.

| Arquivo | Conteúdo |
|---------|----------|
| `Enums.kt` | TicketStatus, FinancialStatus, TransactionType, etc. |
| `TicketModels.kt` | TicketResponse, SelectionResponse, etc. |
| `BankrollModels.kt` | BankrollResponse, TransactionResponse, etc. |
| `AnalyticsModels.kt` | OverallPerformanceResponse, etc. |
| `AuthModels.kt` | LoginRequest, AuthResponse, etc. |

**Network (`shared/network/`):**
O cliente HTTP é configurado com Ktor e inclui interceptors para autenticação.

**Repository (`shared/repository/`):**
Os repositórios abstraem o acesso a dados, combinando chamadas de rede com cache local.

| Repositório | Responsabilidade |
|-------------|------------------|
| `AuthRepository` | Autenticação e tokens |
| `TicketRepository` | Operações com bilhetes |
| `BankrollRepository` | Operações com bancas |
| `AnalyticsRepository` | Dados de performance |
| `ProviderRepository` | Casas de apostas |

### Camada Android

**ViewModels (`androidApp/viewmodel/`):**
Os ViewModels gerenciam o estado da UI e orquestram operações assíncronas.

| ViewModel | Responsabilidade |
|-----------|------------------|
| `AuthViewModel` | Login, registro, logout |
| `TicketsViewModel` | Lista e filtros de bilhetes |
| `BankrollViewModel` | Gerenciamento de bancas |
| `AnalyticsViewModel` | Dados de analytics |
| `DashboardViewModel` | Resumo do dashboard |

**Screens (`androidApp/ui/screens/`):**
As telas são composables que consomem estado dos ViewModels.

| Tela | Descrição |
|------|-----------|
| `DashboardScreen` | Tela inicial com resumo |
| `TicketsScreen` | Lista de bilhetes |
| `TicketDetailScreen` | Detalhes do bilhete |
| `BankrollsScreen` | Lista de bancas |
| `BankrollDetailScreen` | Detalhes da banca |
| `AnalyticsScreen` | Gráficos e métricas |
| `SettingsScreen` | Configurações |

**Components (`androidApp/ui/components/`):**
Componentes reutilizáveis de UI.

| Componente | Uso |
|------------|-----|
| `TicketComponents.kt` | Cards e itens de bilhete |
| `BankrollComponents.kt` | Cards e itens de banca |

**Navigation (`androidApp/ui/navigation/`):**
A navegação é gerenciada com Compose Navigation.

| Arquivo | Conteúdo |
|---------|----------|
| `Routes.kt` | Definição das rotas |
| `SmartBetNavHost.kt` | Configuração do NavHost |

## Fluxo de Dados

```
UI (Compose) → ViewModel → Repository → API/Cache
      ↑                         ↓
   StateFlow ← State ← Result
```

1. A **UI** observa StateFlows do ViewModel
2. O **ViewModel** chama métodos do Repository
3. O **Repository** decide se busca da API ou cache
4. O resultado é emitido via StateFlow
5. A **UI** recompõe automaticamente

## Gerenciamento de Estado

O estado é gerenciado com `StateFlow` e `MutableStateFlow`:

```kotlin
class TicketsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _tickets = MutableStateFlow<List<TicketResponse>>(emptyList())
    val tickets: StateFlow<List<TicketResponse>> = _tickets.asStateFlow()
}
```

**Estados de UI:**
- `Loading` - Carregando dados
- `Success` - Dados carregados com sucesso
- `Error(message)` - Erro com mensagem
- `Empty` - Sem dados para exibir

## Cache e Sincronização

O app implementa uma estratégia de cache-first com sincronização em background:

1. **Primeira carga:** Busca da API e salva no cache
2. **Cargas subsequentes:** Retorna do cache imediatamente
3. **Pull-to-refresh:** Força atualização da API
4. **Background sync:** Sincroniza periodicamente

**SQLDelight** é usado para persistência local, gerando código Kotlin type-safe a partir de schemas SQL.

## Injeção de Dependências

O Hilt é usado para DI no Android:

```kotlin
@HiltViewModel
class TicketsViewModel @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val authRepository: AuthRepository
) : ViewModel()
```

**Módulos Hilt:**
- `NetworkModule` - Configuração do Ktor
- `RepositoryModule` - Repositórios
- `DatabaseModule` - SQLDelight

## Tema e Design

O tema segue Material Design 3 com cores customizadas:

| Cor | Uso |
|-----|-----|
| `success` | Apostas ganhas, lucro |
| `error` | Apostas perdidas, prejuízo |
| `warning` | Alertas, pendências |
| `primary` | Ações principais |

**Modo escuro:** Suportado nativamente com cores adaptativas.

## Testes

Os testes são organizados em:
- **Unit tests:** ViewModels e Repositories
- **UI tests:** Compose testing
- **Integration tests:** Fluxos completos

**Cobertura mínima:** 80%
