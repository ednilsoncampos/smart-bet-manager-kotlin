package com.smartbet.infrastructure.persistence.repository

import com.smartbet.infrastructure.persistence.entity.PerformanceByMonthEntity
import com.smartbet.infrastructure.persistence.entity.PerformanceByMonthId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para a tabela analytics.performance_by_month
 */
@Repository
interface PerformanceByMonthRepository : JpaRepository<PerformanceByMonthEntity, PerformanceByMonthId> {
    /**
     * Busca a performance de um usuário em um mês específico.
     *
     * @param userId ID do usuário
     * @param year Ano
     * @param month Mês (1-12)
     * @return Performance do mês ou null se não existir
     */
    fun findByIdUserIdAndIdYearAndIdMonth(userId: Long, year: Int, month: Int): PerformanceByMonthEntity?

    /**
     * Lista todas as performances de um usuário ordenadas por ano e mês.
     *
     * @param userId ID do usuário
     * @return Lista de performances mensais
     */
    fun findByIdUserIdOrderByIdYearDescIdMonthDesc(userId: Long): List<PerformanceByMonthEntity>
}
