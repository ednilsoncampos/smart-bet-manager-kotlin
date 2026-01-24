package com.smartbet.application.dto

import com.smartbet.domain.entity.Sport
import com.smartbet.domain.entity.Tournament

/**
 * Resposta da importação de torneios.
 */
data class TournamentImportResponse(
    val sportId: Int,
    val sportName: String,
    val totalImported: Int,
    val newTournaments: Int,
    val updatedTournaments: Int,
    val message: String
)

/**
 * Resposta com dados de um esporte.
 */
data class SportResponse(
    val id: Long,
    val externalId: Int,
    val name: String,
    val providerId: Long
) {
    companion object {
        fun fromDomain(sport: Sport): SportResponse = SportResponse(
            id = sport.id!!,
            externalId = sport.externalId,
            name = sport.name,
            providerId = sport.providerId
        )
    }
}

/**
 * Resposta com dados de um torneio.
 */
data class TournamentResponse(
    val id: Long,
    val externalId: Int,
    val name: String,
    val localName: String?,
    val sportId: Long,
    val providerId: Long
) {
    companion object {
        fun fromDomain(tournament: Tournament): TournamentResponse = TournamentResponse(
            id = tournament.id!!,
            externalId = tournament.externalId,
            name = tournament.name,
            localName = tournament.localName,
            sportId = tournament.sportId,
            providerId = tournament.providerId
        )
    }
}

/**
 * Resposta com estatísticas de torneios.
 */
data class TournamentStatsResponse(
    val providerId: Long,
    val providerName: String,
    val totalSports: Int,
    val totalTournaments: Long,
    val sports: List<SportWithTournamentCount>
)

data class SportWithTournamentCount(
    val sportId: Long,
    val sportName: String,
    val tournamentCount: Int
)
