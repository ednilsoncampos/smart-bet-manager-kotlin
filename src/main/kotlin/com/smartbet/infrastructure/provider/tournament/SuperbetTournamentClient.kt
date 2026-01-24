package com.smartbet.infrastructure.provider.tournament

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartbet.infrastructure.provider.gateway.HttpGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Cliente para buscar torneios da API da Superbet.
 *
 * API: https://production-superbet-offer-br.freetls.fastly.net/v2/pt-BR/sport/{sportId}/tournaments
 */
@Component
class SuperbetTournamentClient(
    private val httpGateway: HttpGateway,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(SuperbetTournamentClient::class.java)

    companion object {
        private const val BASE_URL = "https://production-superbet-offer-br.freetls.fastly.net/v2/pt-BR/sport"
    }

    /**
     * Busca torneios de um esporte específico.
     *
     * @param sportId ID do esporte (5 = Futebol, 4 = Basquete)
     * @return Lista de torneios parseados
     */
    fun fetchTournaments(sportId: Int): List<TournamentData> {
        val url = "$BASE_URL/$sportId/tournaments"
        logger.info("Fetching tournaments from: {}", url)

        return try {
            val response = httpGateway.get(url)
            parseTournamentsResponse(response, sportId)
        } catch (e: Exception) {
            logger.error("Error fetching tournaments for sport {}: {}", sportId, e.message)
            emptyList()
        }
    }

    /**
     * Parseia a resposta da API de torneios.
     *
     * Estrutura esperada:
     * {
     *   "data": [
     *     {
     *       "sportId": 5,
     *       "categoryId": 31,
     *       "localNames": { "pt-BR": "Clubes Internacionais" },
     *       "competitions": [
     *         { "tournamentId": 389, "localNames": { "pt-BR": "Copa Libertadores" } }
     *       ]
     *     }
     *   ]
     * }
     */
    private fun parseTournamentsResponse(response: String, sportId: Int): List<TournamentData> {
        val root = objectMapper.readTree(response)
        val tournaments = mutableListOf<TournamentData>()

        val dataArray = root.path("data")
        if (!dataArray.isArray) {
            logger.warn("Unexpected response format: 'data' is not an array")
            return emptyList()
        }

        for (category in dataArray) {
            // Extrai o nome da categoria/país (ex: "Inglaterra", "Brasil")
            val categoryLocalNames = category.path("localNames")
            val localName = categoryLocalNames.path("pt-BR").asText()
                .ifEmpty { categoryLocalNames.path("en").asText() }
                .takeIf { it.isNotEmpty() }

            val competitions = category.path("competitions")
            if (!competitions.isArray) continue

            for (competition in competitions) {
                val tournamentId = competition.path("tournamentId").asInt()
                val tournamentLocalNames = competition.path("localNames")
                val name = tournamentLocalNames.path("pt-BR").asText()
                    .ifEmpty { tournamentLocalNames.path("en").asText() }
                    .ifEmpty { "Torneio $tournamentId" }

                if (tournamentId > 0) {
                    tournaments.add(
                        TournamentData(
                            externalId = tournamentId,
                            name = name,
                            localName = localName,
                            sportId = sportId
                        )
                    )
                }
            }
        }

        logger.info("Parsed {} tournaments for sport {}", tournaments.size, sportId)
        return tournaments
    }
}

/**
 * Dados de um torneio parseado da API.
 */
data class TournamentData(
    val externalId: Int,
    val name: String,
    val localName: String?,
    val sportId: Int
)
