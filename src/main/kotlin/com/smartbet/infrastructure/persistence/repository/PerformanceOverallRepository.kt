package com.smartbet.infrastructure.persistence.repository

import com.smartbet.infrastructure.persistence.entity.PerformanceOverallEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para a tabela analytics.performance_overall
 */
@Repository
interface PerformanceOverallRepository : JpaRepository<PerformanceOverallEntity, Long> {
    /**
     * Busca a performance geral de um usuário.
     *
     * @param userId ID do usuário
     * @return Performance geral ou null se não existir
     */
    fun findByUserId(userId: Long): PerformanceOverallEntity?
}
