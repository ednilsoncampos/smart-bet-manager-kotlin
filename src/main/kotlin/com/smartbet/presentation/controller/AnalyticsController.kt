package com.smartbet.presentation.controller

import com.smartbet.application.dto.*
import com.smartbet.application.usecase.PerformanceAnalyticService
import com.smartbet.application.usecase.BankrollEvolutionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Análise de performance de apostas")
class AnalyticsController(
    private val performanceAnalyticService: PerformanceAnalyticService,
    private val bankrollEvolutionService: BankrollEvolutionService
) {
    
    @GetMapping("/overall")
    @Operation(summary = "Performance geral", description = "Retorna métricas gerais de performance do usuário")
    fun getOverallPerformance(
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<OverallPerformanceResponse> {
        val performance = performanceAnalyticService.getOverallPerformance(userId)
        return ResponseEntity.ok(performance)
    }
    
    @GetMapping("/by-tournament")
    @Operation(summary = "Performance por torneio", description = "Retorna performance agrupada por campeonato/torneio")
    fun getPerformanceByTournament(
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<List<PerformanceByTournamentResponse>> {
        val performance = performanceAnalyticService.getPerformanceByTournament(userId)
        return ResponseEntity.ok(performance)
    }
    
    @GetMapping("/by-market")
    @Operation(
        summary = "Performance por mercado",
        description = "Retorna performance agrupada por tipo de mercado. Use expandBetBuilder=true para expandir componentes do Bet Builder e agregá-los com mercados normais."
    )
    fun getPerformanceByMarket(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(defaultValue = "false") expandBetBuilder: Boolean
    ): ResponseEntity<List<PerformanceByMarketResponse>> {
        val performance = performanceAnalyticService.getPerformanceByMarket(userId, expandBetBuilder)
        return ResponseEntity.ok(performance)
    }

    @GetMapping("/by-month")
    @Operation(summary = "Performance por mês", description = "Retorna performance agrupada por mês (ordenado do mais recente para o mais antigo)")
    fun getPerformanceByMonth(
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<List<PerformanceByMonthResponse>> {
        val performance = performanceAnalyticService.getPerformanceByMonth(userId)
        return ResponseEntity.ok(performance)
    }

    @GetMapping("/by-provider")
    @Operation(summary = "Performance por casa", description = "Retorna performance agrupada por casa de apostas")
    fun getPerformanceByProvider(
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<List<PerformanceByProviderResponse>> {
        val performance = performanceAnalyticService.getPerformanceByProvider(userId)
        return ResponseEntity.ok(performance)
    }
    
    @GetMapping("/bankroll-evolution")
    @Operation(
        summary = "Evolução consolidada de saldo", 
        description = "Retorna a evolução do saldo consolidado de todas as bancas do usuário"
    )
    fun getConsolidatedEvolution(
        @AuthenticationPrincipal userId: Long,
        @RequestParam(required = false) startDate: Long?,
        @RequestParam(required = false) endDate: Long?,
        @RequestParam(required = false, defaultValue = "day") granularity: String
    ): ResponseEntity<ConsolidatedEvolutionResponse> {
        val params = EvolutionQueryParams(
            startDate = startDate,
            endDate = endDate,
            granularity = granularity
        )
        val evolution = bankrollEvolutionService.getConsolidatedEvolution(userId, params)
        return ResponseEntity.ok(evolution)
    }
    
    @GetMapping("/bankroll-evolution/{bankrollId}")
    @Operation(
        summary = "Evolução de saldo de uma banca", 
        description = "Retorna a evolução do saldo de uma banca específica"
    )
    fun getBankrollEvolution(
        @AuthenticationPrincipal userId: Long,
        @PathVariable bankrollId: Long,
        @RequestParam(required = false) startDate: Long?,
        @RequestParam(required = false) endDate: Long?,
        @RequestParam(required = false, defaultValue = "day") granularity: String
    ): ResponseEntity<BankrollEvolutionResponse> {
        val params = EvolutionQueryParams(
            startDate = startDate,
            endDate = endDate,
            granularity = granularity
        )
        val evolution = bankrollEvolutionService.getBankrollEvolution(userId, bankrollId, params)
        return ResponseEntity.ok(evolution)
    }
}
