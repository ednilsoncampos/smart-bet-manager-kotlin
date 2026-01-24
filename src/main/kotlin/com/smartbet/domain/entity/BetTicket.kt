package com.smartbet.domain.entity

import com.smartbet.domain.enum.BetSide
import com.smartbet.domain.enum.BetType
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import java.math.BigDecimal

/**
 * Entidade de domínio representando um bilhete de aposta.
 * 
 * Esta é a entidade principal do sistema, contendo todas as informações
 * sobre uma aposta realizada.
 */
data class BetTicket(
    val id: Long? = null,
    
    /** ID do usuário dono do bilhete */
    val userId: Long,
    
    /** ID da casa de apostas */
    val providerId: Long,
    
    /** ID da banca associada (opcional) */
    val bankrollId: Long? = null,
    
    /** Código externo do bilhete na casa de apostas */
    val externalTicketId: String? = null,
    
    /** URL original de onde o bilhete foi importado */
    val sourceUrl: String? = null,
    
    /** Tipo de aposta */
    val betType: BetType = BetType.SINGLE,
    
    /** Lado da aposta (BACK/LAY) */
    val betSide: BetSide = BetSide.BACK,
    
    /** Valor apostado */
    val stake: BigDecimal,
    
    /** Odd total do bilhete */
    val totalOdd: BigDecimal,
    
    /** Retorno potencial máximo */
    val potentialPayout: BigDecimal? = null,
    
    /** Retorno real (após resultado) */
    val actualPayout: BigDecimal? = null,
    
    /** Status do bilhete na casa de apostas */
    val ticketStatus: TicketStatus = TicketStatus.OPEN,
    
    /** Status financeiro calculado */
    val financialStatus: FinancialStatus = FinancialStatus.PENDING,
    
    /** Lucro/prejuízo calculado */
    val profitLoss: BigDecimal = BigDecimal.ZERO,
    
    /** ROI calculado (em percentual) */
    val roi: BigDecimal = BigDecimal.ZERO,
    
    /** Descrição do sistema (para apostas de sistema) - ex: "2/3" */
    val systemDescription: String? = null,
    
    /** Timestamp em que a aposta foi realizada (milissegundos desde epoch UTC) */
    val placedAt: Long? = null,
    
    /** Timestamp em que o bilhete foi resolvido (milissegundos desde epoch UTC) */
    val settledAt: Long? = null,
    
    /** Indica se o bilhete foi encerrado via cashout */
    val isCashedOut: Boolean = false,
    
    /** Seleções do bilhete */
    val selections: List<BetSelection> = emptyList(),
    
    /** Timestamp de criação (milissegundos desde epoch UTC) */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** Timestamp de atualização (milissegundos desde epoch UTC) */
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Verifica se o bilhete está aberto (aguardando resultado)
     */
    fun isOpen(): Boolean = ticketStatus == TicketStatus.OPEN
    
    /**
     * Verifica se o bilhete está resolvido
     */
    fun isSettled(): Boolean = ticketStatus != TicketStatus.OPEN
    
    /**
     * Retorna o número de seleções no bilhete
     */
    fun selectionCount(): Int = selections.size
}
