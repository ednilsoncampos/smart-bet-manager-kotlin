package com.smartbet.infrastructure.persistence.repository

import com.smartbet.infrastructure.persistence.entity.PerformanceByMarketEntity
import com.smartbet.infrastructure.persistence.entity.PerformanceByMarketId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para a tabela analytics.performance_by_market
 */
@Repository
interface PerformanceByMarketRepository : JpaRepository<PerformanceByMarketEntity, PerformanceByMarketId> {
    /**
     * Busca a performance de um usuário em um tipo de mercado específico.
     *
     * @param userId ID do usuário
     * @param marketType Tipo de mercado
     * @return Performance no mercado ou null se não existir
     */
    fun findByIdUserIdAndIdMarketType(userId: Long, marketType: String): PerformanceByMarketEntity?

    /**
     * Lista todas as performances de um usuário por mercado.
     *
     * @param userId ID do usuário
     * @return Lista de performances por mercado
     */
    fun findByIdUserId(userId: Long): List<PerformanceByMarketEntity>
}
