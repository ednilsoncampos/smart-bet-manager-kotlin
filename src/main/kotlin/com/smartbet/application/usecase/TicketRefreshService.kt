package com.smartbet.application.usecase

import com.smartbet.application.dto.AnalyticsProcessingResult
import com.smartbet.application.dto.RefreshResult
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

/**
 * Serviço dedicado para refresh assíncrono de bilhetes.
 * Separado do TicketService para evitar problemas com proxy do Spring AOP.
 */
@Service
class TicketRefreshService(
    private val ticketService: TicketService
) {
    private val logger = LoggerFactory.getLogger(TicketRefreshService::class.java)

    /**
     * Executa refresh de bilhetes em aberto de forma assíncrona.
     * Chamado pelo endpoint quando o usuário faz login.
     *
     * @param userId ID do usuário
     * @return CompletableFuture com o resultado do processamento
     */
    @Async("taskExecutor")
    fun refreshOpenTicketsAsync(userId: Long): CompletableFuture<RefreshResult> {
        logger.info("Async refresh started for user: {}", userId)

        return try {
            val result = ticketService.refreshOpenTickets(userId)
            logger.info("Async refresh completed for user {}: updated={}, errors={}",
                userId, result.updated, result.errors)
            CompletableFuture.completedFuture(result)
        } catch (e: Exception) {
            logger.error("Async refresh failed for user {}: {}", userId, e.message, e)
            CompletableFuture.failedFuture(e)
        }
    }

    /**
     * Executa processamento de analytics de bilhetes liquidados de forma assíncrona.
     * Busca todos os bilhetes liquidados do usuário e gera/atualiza dados de performance.
     *
     * @param userId ID do usuário
     * @return CompletableFuture com o resultado do processamento
     */
    @Async("taskExecutor")
    fun processSettledTicketsAnalyticsAsync(userId: Long): CompletableFuture<AnalyticsProcessingResult> {
        logger.info("Async analytics processing started for user: {}", userId)

        return try {
            val result = ticketService.processSettledTicketsAnalytics(userId)
            logger.info("Async analytics processing completed for user {}: successful={}, errors={}",
                userId, result.successful, result.errors)
            CompletableFuture.completedFuture(result)
        } catch (e: Exception) {
            logger.error("Async analytics processing failed for user {}: {}", userId, e.message, e)
            CompletableFuture.failedFuture(e)
        }
    }
}
