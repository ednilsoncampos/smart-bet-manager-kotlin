package com.smartbet.infrastructure.persistence.repository

import com.smartbet.infrastructure.persistence.entity.PerformanceByProviderEntity
import com.smartbet.infrastructure.persistence.entity.PerformanceByProviderId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para a tabela analytics.performance_by_provider
 */
@Repository
interface PerformanceByProviderRepository : JpaRepository<PerformanceByProviderEntity, PerformanceByProviderId> {
    /**
     * Busca a performance de um usuário com um provider específico.
     *
     * @param userId ID do usuário
     * @param providerId ID do provider
     * @return Performance com o provider ou null se não existir
     */
    fun findByIdUserIdAndIdProviderId(userId: Long, providerId: Long): PerformanceByProviderEntity?

    /**
     * Lista todas as performances de um usuário por provider.
     *
     * @param userId ID do usuário
     * @return Lista de performances por provider
     */
    fun findByIdUserId(userId: Long): List<PerformanceByProviderEntity>
}
