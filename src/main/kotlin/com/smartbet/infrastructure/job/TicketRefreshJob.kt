package com.smartbet.infrastructure.job

import com.smartbet.application.usecase.TicketService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Job agendado para atualizar bilhetes em aberto.
 * Executa a cada 30 minutos para manter os bilhetes atualizados
 * sem depender do login do usuário no app.
 */
@Component
class TicketRefreshJob(
    private val ticketService: TicketService
) {
    private val logger = LoggerFactory.getLogger(TicketRefreshJob::class.java)
    
    /**
     * Executa refresh de todos os bilhetes em aberto a cada 30 minutos.
     * 
     * Configuração:
     * - fixedRate: 30 minutos (1800000 ms)
     * - initialDelay: 5 minutos (300000 ms) - aguarda aplicação inicializar
     */
    @Scheduled(
        fixedRateString = "\${app.jobs.ticket-refresh.rate:1800000}",
        initialDelayString = "\${app.jobs.ticket-refresh.initial-delay:300000}"
    )
    fun refreshAllOpenTickets() {
        logger.info("=== Starting scheduled ticket refresh job ===")
        
        val startTime = System.currentTimeMillis()
        
        try {
            val result = ticketService.refreshAllOpenTickets()
            
            val duration = System.currentTimeMillis() - startTime
            
            logger.info(
                "=== Scheduled ticket refresh completed in {}ms: total={}, updated={}, unchanged={}, errors={} ===",
                duration,
                result.totalProcessed,
                result.updated,
                result.unchanged,
                result.errors
            )
            
            if (result.errorDetails.isNotEmpty()) {
                logger.warn("Refresh errors: {}", result.errorDetails.map { 
                    "ticket=${it.ticketId}, error=${it.errorMessage}" 
                })
            }
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error(
                "=== Scheduled ticket refresh FAILED after {}ms: {} ===",
                duration,
                e.message,
                e
            )
        }
    }
}
