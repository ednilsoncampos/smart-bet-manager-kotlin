package com.smartbet.domain.entity

import java.math.BigDecimal

/**
 * Entidade de domínio representando uma banca de apostas.
 * 
 * Uma banca pode ser geral ou específica de uma casa de apostas.
 */
data class Bankroll(
    val id: Long? = null,
    
    /** ID do usuário dono da banca */
    val userId: Long,
    
    /** ID da casa de apostas (null = banca geral) */
    val providerId: Long? = null,
    
    /** Nome da banca */
    val name: String,
    
    /** Moeda da banca */
    val currency: String = "BRL",
    
    /** Saldo atual */
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    
    /** Total depositado */
    val totalDeposited: BigDecimal = BigDecimal.ZERO,
    
    /** Total sacado */
    val totalWithdrawn: BigDecimal = BigDecimal.ZERO,
    
    /** Total apostado */
    val totalStaked: BigDecimal = BigDecimal.ZERO,
    
    /** Total de retornos */
    val totalReturns: BigDecimal = BigDecimal.ZERO,
    
    /** Se a banca está ativa */
    val isActive: Boolean = true,
    
    /** Timestamp de criação (milissegundos desde epoch UTC) */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** Timestamp de atualização (milissegundos desde epoch UTC) */
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Calcula o lucro/prejuízo total da banca
     */
    fun calculateProfitLoss(): BigDecimal {
        return totalReturns - totalStaked
    }
    
    /**
     * Calcula o ROI da banca
     */
    fun calculateRoi(): BigDecimal {
        if (totalStaked == BigDecimal.ZERO) return BigDecimal.ZERO
        return calculateProfitLoss()
            .divide(totalStaked, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
    }
}
