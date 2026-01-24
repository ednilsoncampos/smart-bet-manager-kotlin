package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.infrastructure.persistence.entity.SportEntity
import com.smartbet.infrastructure.persistence.entity.TournamentEntity
import com.smartbet.infrastructure.persistence.repository.BettingProviderRepository
import com.smartbet.infrastructure.persistence.repository.SportRepository
import com.smartbet.infrastructure.persistence.repository.TournamentRepository
import com.smartbet.infrastructure.provider.tournament.SportNameMapper
import com.smartbet.infrastructure.provider.tournament.SuperbetTournamentClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TournamentService(
    private val tournamentRepository: TournamentRepository,
    private val sportRepository: SportRepository,
    private val providerRepository: BettingProviderRepository,
    private val superbetTournamentClient: SuperbetTournamentClient
) {
    private val logger = LoggerFactory.getLogger(TournamentService::class.java)

    /**
     * Importa torneios de um esporte específico para um provider.
     *
     * @param providerSlug Slug do provider (ex: "superbet")
     * @param sportExternalId ID externo do esporte (ex: 5 para Futebol)
     * @return Resultado da importação
     */
    @Transactional
    fun importTournaments(providerSlug: String, sportExternalId: Int): TournamentImportResponse {
        logger.info("Starting tournament import for provider {} and sport {}", providerSlug, sportExternalId)

        val provider = providerRepository.findBySlug(providerSlug)
            ?: throw IllegalArgumentException("Provider não encontrado: $providerSlug")

        val providerId = provider.id!!

        // Busca ou cria o esporte
        val sport = getOrCreateSport(providerId, sportExternalId)

        // Busca torneios da API
        val tournamentDataList = when (providerSlug) {
            "superbet" -> superbetTournamentClient.fetchTournaments(sportExternalId)
            else -> throw IllegalArgumentException("Provider não suportado para importação: $providerSlug")
        }

        if (tournamentDataList.isEmpty()) {
            return TournamentImportResponse(
                sportId = sportExternalId,
                sportName = sport.name,
                totalImported = 0,
                newTournaments = 0,
                updatedTournaments = 0,
                message = "Nenhum torneio encontrado na API"
            )
        }

        var newCount = 0
        var updatedCount = 0

        for (data in tournamentDataList) {
            val existing = tournamentRepository.findByProviderIdAndExternalId(providerId, data.externalId)

            if (existing != null) {
                // Atualiza se o nome ou localName mudou
                val nameChanged = existing.name != data.name
                val localNameChanged = existing.localName != data.localName

                if (nameChanged || localNameChanged) {
                    existing.name = data.name
                    existing.localName = data.localName
                    tournamentRepository.save(existing)
                    updatedCount++
                }
            } else {
                // Cria novo torneio
                val entity = TournamentEntity(
                    providerId = providerId,
                    sportId = sport.id!!,
                    externalId = data.externalId,
                    name = data.name,
                    localName = data.localName
                )
                tournamentRepository.save(entity)
                newCount++
            }
        }

        logger.info(
            "Tournament import completed: {} new, {} updated for sport {}",
            newCount, updatedCount, sportExternalId
        )

        return TournamentImportResponse(
            sportId = sportExternalId,
            sportName = sport.name,
            totalImported = tournamentDataList.size,
            newTournaments = newCount,
            updatedTournaments = updatedCount,
            message = "Importação concluída com sucesso"
        )
    }

    /**
     * Busca o nome de um torneio pelo ID externo e provider.
     *
     * @param providerId ID do provider
     * @param externalTournamentId ID externo do torneio
     * @return Nome do torneio ou null se não encontrado
     */
    @Transactional(readOnly = true)
    fun findTournamentName(providerId: Long, externalTournamentId: Int): String? {
        return tournamentRepository.findByProviderIdAndExternalId(providerId, externalTournamentId)?.name
    }

    /**
     * Lista todos os torneios de um provider.
     */
    @Transactional(readOnly = true)
    fun listTournaments(providerSlug: String): List<TournamentResponse> {
        val provider = providerRepository.findBySlug(providerSlug)
            ?: throw IllegalArgumentException("Provider não encontrado: $providerSlug")

        return tournamentRepository.findByProviderId(provider.id!!)
            .map { TournamentResponse.fromDomain(it.toDomain()) }
    }

    /**
     * Lista todos os esportes de um provider.
     */
    @Transactional(readOnly = true)
    fun listSports(providerSlug: String): List<SportResponse> {
        val provider = providerRepository.findBySlug(providerSlug)
            ?: throw IllegalArgumentException("Provider não encontrado: $providerSlug")

        return sportRepository.findByProviderId(provider.id!!)
            .map { SportResponse.fromDomain(it.toDomain()) }
    }

    /**
     * Retorna estatísticas de torneios de um provider.
     */
    @Transactional(readOnly = true)
    fun getStats(providerSlug: String): TournamentStatsResponse {
        val provider = providerRepository.findBySlug(providerSlug)
            ?: throw IllegalArgumentException("Provider não encontrado: $providerSlug")

        val providerId = provider.id!!
        val sports = sportRepository.findByProviderId(providerId)
        val totalTournaments = tournamentRepository.countByProviderId(providerId)

        val sportsWithCount = sports.map { sport ->
            SportWithTournamentCount(
                sportId = sport.id!!,
                sportName = sport.name,
                tournamentCount = tournamentRepository.findBySportId(sport.id!!).size
            )
        }

        return TournamentStatsResponse(
            providerId = providerId,
            providerName = provider.name,
            totalSports = sports.size,
            totalTournaments = totalTournaments,
            sports = sportsWithCount
        )
    }

    /**
     * Busca ou cria um esporte.
     */
    private fun getOrCreateSport(providerId: Long, externalId: Int): SportEntity {
        val existing = sportRepository.findByProviderIdAndExternalId(providerId, externalId)
        if (existing != null) {
            return existing
        }

        val sportName = SportNameMapper.getName(externalId)
        val entity = SportEntity(
            providerId = providerId,
            externalId = externalId,
            name = sportName
        )

        return sportRepository.save(entity)
    }
}
