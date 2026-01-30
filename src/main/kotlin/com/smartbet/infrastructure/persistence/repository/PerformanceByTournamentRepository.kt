package com.smartbet.infrastructure.persistence.repository

import com.smartbet.infrastructure.persistence.entity.PerformanceByTournamentEntity
import com.smartbet.infrastructure.persistence.entity.PerformanceByTournamentId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Repository para a tabela analytics.performance_by_tournament
 */
@Repository
interface PerformanceByTournamentRepository : JpaRepository<PerformanceByTournamentEntity, PerformanceByTournamentId> {
    /**
     * Busca a performance de um usuário em um torneio específico.
     *
     * @param userId ID do usuário
     * @param tournamentId ID do torneio
     * @return Performance no torneio ou null se não existir
     */
    fun findByIdUserIdAndIdTournamentId(userId: Long, tournamentId: Long): PerformanceByTournamentEntity?

    /**
     * Lista todas as performances de um usuário por torneio ordenadas por ROI descendente.
     *
     * @param userId ID do usuário
     * @return Lista de performances por torneio
     */
    @Query("""
        SELECT p FROM PerformanceByTournamentEntity p
        WHERE p.id.userId = :userId
        ORDER BY p.roi DESC
    """)
    fun findByIdUserIdOrderByRoiDesc(userId: Long): List<PerformanceByTournamentEntity>

    /**
     * Lista todas as performances de um usuário por torneio.
     *
     * @param userId ID do usuário
     * @return Lista de performances por torneio
     */
    fun findByIdUserId(userId: Long): List<PerformanceByTournamentEntity>
}
