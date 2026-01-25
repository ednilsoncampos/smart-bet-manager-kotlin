package com.smartbet.domain.service

import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Serviço de domínio responsável por calcular o status financeiro real de uma aposta.
 *
 * Este calculador implementa a lógica de negócio para determinar o resultado
 * financeiro real de uma aposta, considerando:
 * - Apostas de sistema com ganho parcial
 * - Cashout parcial
 * - Seleções anuladas
 *
 * Os 5 níveis de status financeiro são:
 * 1. FULL_WIN: Retorno >= Potencial máximo
 * 2. PARTIAL_WIN: Stake < Retorno < Potencial
 * 3. BREAK_EVEN: Retorno = Stake
 * 4. PARTIAL_LOSS: 0 < Retorno < Stake
 * 5. TOTAL_LOSS: Retorno = 0
 */
object BetStatusCalculator {

    /**
     * Input para cálculo do status financeiro
     */
    data class CalculateInput(
        val stake: BigDecimal,
        val actualPayout: BigDecimal?,
        val potentialPayout: BigDecimal?,
        val ticketStatus: TicketStatus
    )

    /**
     * Resultado do cálculo de status financeiro
     */
    data class CalculateResult(
        val financialStatus: FinancialStatus,
        val profitLoss: BigDecimal,
        val roi: BigDecimal
    )

    /**
     * Calcula o status financeiro de uma aposta.
     *
     * @param input Dados da aposta para cálculo
     * @return Resultado com status financeiro, lucro/prejuízo e ROI
     */
    fun calculate(input: CalculateInput): CalculateResult {
        val (stake, actualPayout, potentialPayout, ticketStatus) = input

        // Se o bilhete está aberto, retorna PENDING
        if (ticketStatus == TicketStatus.OPEN) {
            return CalculateResult(
                financialStatus = FinancialStatus.PENDING,
                profitLoss = BigDecimal.ZERO,
                roi = BigDecimal.ZERO
            )
        }

        // Se não temos o payout real, não podemos calcular
        if (actualPayout == null) {
            return CalculateResult(
                financialStatus = FinancialStatus.PENDING,
                profitLoss = BigDecimal.ZERO,
                roi = BigDecimal.ZERO
            )
        }

        // Calcula lucro/prejuízo
        val profitLoss = actualPayout - stake

        // Calcula ROI
        val roi = if (stake > BigDecimal.ZERO) {
            profitLoss
                .divide(stake, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else {
            BigDecimal.ZERO
        }

        // Determina o status financeiro
        val financialStatus = determineFinancialStatus(
            stake = stake,
            actualPayout = actualPayout,
            potentialPayout = potentialPayout
        )

        return CalculateResult(
            financialStatus = financialStatus,
            profitLoss = profitLoss,
            roi = roi
        )
    }

    /**
     * Determina o status financeiro baseado nos valores.
     */
    private fun determineFinancialStatus(
        stake: BigDecimal,
        actualPayout: BigDecimal,
        potentialPayout: BigDecimal?
    ): FinancialStatus {

        val normalizedActual = normalize(actualPayout)

        // TOTAL_LOSS
        if (normalizedActual.compareTo(BigDecimal.ZERO) == 0) {
            return FinancialStatus.TOTAL_LOSS
        }

        // BREAK_EVEN
        if (normalizedActual.compareTo(stake) == 0) {
            return FinancialStatus.BREAK_EVEN
        }

        // PARTIAL_LOSS
        if (normalizedActual < stake) {
            return FinancialStatus.PARTIAL_LOSS
        }

        // Houve lucro
        if (potentialPayout == null || potentialPayout <= BigDecimal.ZERO) {
            return FinancialStatus.FULL_WIN
        }

        val normalizedPotential = normalize(potentialPayout)

        return if (normalizedActual < normalizedPotential) {
            FinancialStatus.PARTIAL_WIN
        } else {
            FinancialStatus.FULL_WIN
        }
    }

    private fun normalize(value: BigDecimal): BigDecimal {
        return value.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calcula o status financeiro a partir de uma entidade BetTicket.
     */
    fun calculateFromTicket(
        stake: BigDecimal,
        actualPayout: BigDecimal?,
        potentialPayout: BigDecimal?,
        ticketStatus: TicketStatus
    ): CalculateResult {
        return calculate(
            CalculateInput(
                stake = stake,
                actualPayout = actualPayout,
                potentialPayout = potentialPayout,
                ticketStatus = ticketStatus
            )
        )
    }
}
