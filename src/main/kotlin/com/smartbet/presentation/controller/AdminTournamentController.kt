package com.smartbet.presentation.controller

import com.smartbet.application.dto.SportResponse
import com.smartbet.application.dto.TournamentImportResponse
import com.smartbet.application.dto.TournamentResponse
import com.smartbet.application.dto.TournamentStatsResponse
import com.smartbet.application.usecase.TournamentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/tournaments")
@Tag(name = "Admin - Tournaments", description = "Gerenciamento de torneios (somente ADMIN)")
@PreAuthorize("hasRole('ADMIN')")
class AdminTournamentController(
    private val tournamentService: TournamentService
) {

    @PostMapping("/import")
    @Operation(
        summary = "Importar torneios",
        description = "Importa torneios de um esporte específico da API do provider"
    )
    fun importTournaments(
        @RequestParam(defaultValue = "superbet") provider: String,
        @RequestParam(defaultValue = "5") sportId: Int
    ): ResponseEntity<TournamentImportResponse> {
        val result = tournamentService.importTournaments(provider, sportId)
        return ResponseEntity.ok(result)
    }

    @GetMapping
    @Operation(
        summary = "Listar torneios",
        description = "Lista todos os torneios importados de um provider"
    )
    fun listTournaments(
        @RequestParam(defaultValue = "superbet") provider: String
    ): ResponseEntity<List<TournamentResponse>> {
        val tournaments = tournamentService.listTournaments(provider)
        return ResponseEntity.ok(tournaments)
    }

    @GetMapping("/sports")
    @Operation(
        summary = "Listar esportes",
        description = "Lista todos os esportes de um provider"
    )
    fun listSports(
        @RequestParam(defaultValue = "superbet") provider: String
    ): ResponseEntity<List<SportResponse>> {
        val sports = tournamentService.listSports(provider)
        return ResponseEntity.ok(sports)
    }

    @GetMapping("/stats")
    @Operation(
        summary = "Estatísticas de torneios",
        description = "Retorna estatísticas de torneios importados"
    )
    fun getStats(
        @RequestParam(defaultValue = "superbet") provider: String
    ): ResponseEntity<TournamentStatsResponse> {
        val stats = tournamentService.getStats(provider)
        return ResponseEntity.ok(stats)
    }
}
