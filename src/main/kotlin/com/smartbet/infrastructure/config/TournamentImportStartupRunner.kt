package com.smartbet.infrastructure.config

import com.smartbet.application.usecase.TournamentService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Componente que executa a importação de torneios no startup da aplicação.
 *
 * Importa automaticamente torneios dos esportes principais (Futebol, Basquete, etc.)
 * da API da Superbet para garantir que o banco de dados esteja atualizado.
 *
 * A importação roda de forma assíncrona e não bloqueia o startup da aplicação.
 * Se houver erros, eles são logados mas não impedem a aplicação de iniciar.
 */
@Component
class TournamentImportStartupRunner(
    private val tournamentService: TournamentService
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(TournamentImportStartupRunner::class.java)

    companion object {
        // Mapeamento de IDs de esportes na API da Superbet - 4 to "Basquete"
        // IDs confirmados da API - adicione mais conforme necessário
        private val SPORTS_TO_IMPORT = mapOf(
            5 to "Futebol"
            // TODO: Adicionar outros esportes após descobrir os IDs corretos
            // Para descobrir IDs, teste: https://production-superbet-offer-br.freetls.fastly.net/v2/pt-BR/sport/{ID}/tournaments
        )
    }

    override fun run(args: ApplicationArguments?) {
        logger.info("===== Starting tournament import on application startup =====")

        try {
            importTournamentsForAllSports()
        } catch (e: Exception) {
            logger.error("Fatal error during tournament import startup: ${e.message}", e)
        }

        logger.info("===== Tournament import startup completed =====")
    }

    /**
     * Importa torneios para todos os esportes configurados.
     */
    private fun importTournamentsForAllSports() {
        var totalImported = 0
        var totalNew = 0
        var totalUpdated = 0
        var successCount = 0
        var errorCount = 0

        SPORTS_TO_IMPORT.forEach { (sportId, sportName) ->
            try {
                logger.info("Importing tournaments for $sportName (ID: $sportId)...")

                val result = tournamentService.importTournaments("superbet", sportId)

                totalImported += result.totalImported
                totalNew += result.newTournaments
                totalUpdated += result.updatedTournaments
                successCount++

                logger.info(
                    "✓ $sportName: ${result.totalImported} tournaments imported " +
                            "(${result.newTournaments} new, ${result.updatedTournaments} updated)"
                )
            } catch (e: Exception) {
                errorCount++
                logger.error("✗ Error importing tournaments for $sportName (ID: $sportId): ${e.message}")
            }
        }

        // Log do resumo final
        logger.info(
            """
            |
            |Tournament Import Summary:
            |  - Sports processed: ${SPORTS_TO_IMPORT.size}
            |  - Successful: $successCount
            |  - Failed: $errorCount
            |  - Total tournaments imported: $totalImported
            |  - New tournaments: $totalNew
            |  - Updated tournaments: $totalUpdated
            |""".trimMargin()
        )
    }
}
