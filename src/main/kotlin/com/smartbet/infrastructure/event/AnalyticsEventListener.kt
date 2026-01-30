package com.smartbet.infrastructure.event

import com.smartbet.application.usecase.AnalyticsAggregationService
import com.smartbet.domain.event.TicketSettledEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Listener para eventos de liquidação de tickets.
 *
 * Processa eventos de forma assíncrona em uma transação separada,
 * permitindo que a liquidação do ticket complete independentemente
 * da atualização de analytics.
 *
 * Em caso de falha, o listener retenta automaticamente até 3 vezes
 * com backoff exponencial.
 */
@Component
class AnalyticsEventListener(
    private val analyticsService: AnalyticsAggregationService
) {
    private val logger = LoggerFactory.getLogger(AnalyticsEventListener::class.java)

    /**
     * Processa evento de liquidação de ticket.
     *
     * Características:
     * - @Async: Executado de forma assíncrona no thread pool "analyticsTaskExecutor"
     * - @Transactional(REQUIRES_NEW): Executa em transação separada da liquidação
     * - @Retryable: Retenta até 3 vezes com backoff exponencial (1s, 2s, 4s)
     *
     * @param event Evento de liquidação do ticket
     *
     * TODO: Atualizar AnalyticsAggregationService para usar campos corretos das entidades
     */
    // @EventListener
    // @Async("analyticsTaskExecutor")
    // @Transactional(propagation = Propagation.REQUIRES_NEW)
    // @Retryable(
    //     maxAttempts = 3,
    //     backoff = Backoff(delay = 1000, multiplier = 2.0),
    //     recover = "recoverAnalyticsUpdate"
    // )
    fun onTicketSettled(event: TicketSettledEvent) {
        logger.debug(
            "Received TicketSettledEvent: ticket={}, user={}, status={}",
            event.ticketId, event.userId, event.financialStatus
        )

        try {
            analyticsService.updateOnSettlement(event)
        } catch (e: Exception) {
            logger.error(
                "Error processing analytics for ticket {} (attempt will be retried): {}",
                event.ticketId, e.message, e
            )
            throw e // Permite que @Retryable funcione
        }
    }

    /**
     * Método de recuperação chamado quando todas as tentativas falharem.
     *
     * Este método é invocado automaticamente pelo @Retryable após esgotar
     * todas as tentativas. Loga o erro final sem lançar exceção para evitar
     * propagação.
     *
     * @param e Exceção que causou a falha
     * @param event Evento que estava sendo processado
     */
    fun recoverAnalyticsUpdate(e: Exception, event: TicketSettledEvent) {
        logger.error(
            "FAILED to update analytics after 3 attempts for ticket {}: {}. " +
                "Manual intervention may be required.",
            event.ticketId, e.message, e
        )

        // TODO: Considerar enviar alerta (email, Slack, etc.)
        // TODO: Considerar gravar em tabela de eventos falhados para reprocessamento posterior
    }
}
