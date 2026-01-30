package com.smartbet.domain.service

import com.smartbet.domain.enum.FinancialStatus

/**
 * Centraliza as regras de negócio relacionadas ao FinancialStatus.
 *
 * REGRAS DE NEGÓCIO:
 *
 * 1. TicketStatus.WIN pode ter:
 *    - FULL_WIN: Retorno >= Potencial máximo
 *    - PARTIAL_WIN: Stake < Retorno < Potencial (sistema parcial OU cashout)
 *
 * 2. TicketStatus.LOST sempre resulta em:
 *    - TOTAL_LOSS: Retorno = 0
 *    - PARTIAL_LOSS: 0 < Retorno < Stake (via cashout ou sistema com margem)
 *
 * 3. Apostas de Sistema permitem ganhos/perdas parciais SEM cashout (COMUM):
 *    - Sistema 4/5 (precisa de 4 acertos em 5 jogos):
 *      - Errou 1 jogo: ainda pode ganhar (PARTIAL_WIN se retorno < potencial, ou FULL_WIN)
 *      - Errou 2 jogos: perde tudo (TOTAL_LOSS)
 *    - Para tolerar 2 erros, precisaria ser sistema 3/5
 *    - Exemplo: Sistema 4/5, stake R$ 50, errou 1, retorno R$ 40 = PARTIAL_LOSS
 *
 * 4. Cashout:
 *    - Campo separado: isCashedOut
 *    - Cashout pode resultar em PARTIAL_WIN, PARTIAL_LOSS ou BREAK_EVEN
 *    - Cashout NÃO é a única forma de ter resultado parcial
 *
 * 5. BREAK_EVEN:
 *    - Retorno = Stake
 *    - Pode ocorrer via: seleções anuladas, cashout exato, ou sistema com retorno igual
 *    - Não é vitória nem derrota
 */
object FinancialStatusRules {

    /**
     * Verifica se o status representa algum tipo de vitória.
     * Inclui tanto vitórias completas quanto parciais.
     */
    fun isWin(status: FinancialStatus): Boolean {
        return status == FinancialStatus.FULL_WIN || status == FinancialStatus.PARTIAL_WIN
    }

    /**
     * Verifica se o status representa algum tipo de perda.
     * Inclui tanto perdas totais quanto parciais.
     */
    fun isLoss(status: FinancialStatus): Boolean {
        return status == FinancialStatus.TOTAL_LOSS || status == FinancialStatus.PARTIAL_LOSS
    }

    /**
     * Verifica se o status representa vitória completa.
     * Apenas FULL_WIN, não inclui PARTIAL_WIN.
     */
    fun isFullWin(status: FinancialStatus): Boolean {
        return status == FinancialStatus.FULL_WIN
    }

    /**
     * Verifica se o status representa vitória parcial.
     * Pode ser via cashout OU via sistema com acerto parcial.
     */
    fun isPartialWin(status: FinancialStatus): Boolean {
        return status == FinancialStatus.PARTIAL_WIN
    }

    /**
     * Verifica se o status representa perda total.
     * Retorno = 0, perdeu tudo.
     */
    fun isTotalLoss(status: FinancialStatus): Boolean {
        return status == FinancialStatus.TOTAL_LOSS
    }

    /**
     * Verifica se o status representa perda parcial.
     * COMUM em apostas de sistema quando não acerta todas as combinações.
     * Pode ocorrer via:
     * - Sistema com erro dentro da margem (retorno < stake)
     * - Cashout com prejuízo antes de perder tudo
     *
     * Exemplo: Sistema 4/5, errou 1 jogo, stake R$ 50, retorno R$ 40.
     */
    fun isPartialLoss(status: FinancialStatus): Boolean {
        return status == FinancialStatus.PARTIAL_LOSS
    }

    /**
     * Verifica se o status é break even (empate).
     * Retorno = Stake, pode ocorrer via:
     * - Seleções anuladas
     * - Cashout no valor exato da entrada
     * - Sistema com retorno igual à entrada
     */
    fun isBreakEven(status: FinancialStatus): Boolean {
        return status == FinancialStatus.BREAK_EVEN
    }

    /**
     * Verifica se o status é pendente (ainda não liquidado).
     */
    fun isPending(status: FinancialStatus): Boolean {
        return status == FinancialStatus.PENDING
    }

    /**
     * Verifica se o status é settled (liquidado).
     * Qualquer status exceto PENDING.
     */
    fun isSettled(status: FinancialStatus): Boolean {
        return status != FinancialStatus.PENDING
    }

    /**
     * Verifica se o status representa resultado parcial (não completo).
     * PARTIAL_WIN ou PARTIAL_LOSS.
     */
    fun isPartialResult(status: FinancialStatus): Boolean {
        return status == FinancialStatus.PARTIAL_WIN || status == FinancialStatus.PARTIAL_LOSS
    }

    /**
     * Verifica se o status representa resultado completo.
     * FULL_WIN ou TOTAL_LOSS (não inclui parciais).
     */
    fun isCompleteResult(status: FinancialStatus): Boolean {
        return status == FinancialStatus.FULL_WIN || status == FinancialStatus.TOTAL_LOSS
    }

    /**
     * Verifica se o status gera lucro.
     * FULL_WIN ou PARTIAL_WIN (profit > 0).
     */
    fun generatesProfitLoss(status: FinancialStatus): Boolean {
        return when (status) {
            FinancialStatus.FULL_WIN, FinancialStatus.PARTIAL_WIN -> true  // Lucro
            FinancialStatus.TOTAL_LOSS, FinancialStatus.PARTIAL_LOSS -> true  // Prejuízo
            FinancialStatus.BREAK_EVEN -> false  // Zero
            FinancialStatus.PENDING -> false  // Ainda não resolvido
        }
    }

    /**
     * Determina se o status deve contar para win rate.
     * Para win rate básico: FULL_WIN e PARTIAL_WIN contam como vitória.
     *
     * @param includePartial Se false, conta apenas FULL_WIN
     */
    fun countsAsWinForRate(status: FinancialStatus, includePartial: Boolean = true): Boolean {
        return if (includePartial) {
            isWin(status)
        } else {
            isFullWin(status)
        }
    }

    /**
     * Determina se o status deve contar para loss rate.
     * Para loss rate básico: TOTAL_LOSS e PARTIAL_LOSS contam como perda.
     *
     * @param includePartial Se false, conta apenas TOTAL_LOSS
     */
    fun countsAsLossForRate(status: FinancialStatus, includePartial: Boolean = true): Boolean {
        return if (includePartial) {
            isLoss(status)
        } else {
            isTotalLoss(status)
        }
    }

    /**
     * Determina se o status deve ser contado em métricas de gamificação (streaks).
     * BREAK_EVEN e PENDING não contam para streaks.
     */
    fun countsForStreak(status: FinancialStatus): Boolean {
        return status != FinancialStatus.BREAK_EVEN && status != FinancialStatus.PENDING
    }

    /**
     * Retorna uma descrição amigável do status financeiro.
     */
    fun getDisplayName(status: FinancialStatus): String {
        return when (status) {
            FinancialStatus.PENDING -> "Pendente"
            FinancialStatus.FULL_WIN -> "Vitória Completa"
            FinancialStatus.PARTIAL_WIN -> "Vitória Parcial"
            FinancialStatus.BREAK_EVEN -> "Empate"
            FinancialStatus.PARTIAL_LOSS -> "Perda Parcial"
            FinancialStatus.TOTAL_LOSS -> "Perda Total"
        }
    }

    /**
     * Retorna uma descrição do contexto que pode gerar esse status.
     *
     * @param isCashedOut Se o ticket teve cashout
     * @param isSystemBet Se é aposta de sistema
     */
    fun getContextDescription(
        status: FinancialStatus,
        isCashedOut: Boolean = false,
        isSystemBet: Boolean = false
    ): String {
        return when (status) {
            FinancialStatus.FULL_WIN -> {
                "Acertou todas as seleções e recebeu o pagamento máximo"
            }

            FinancialStatus.PARTIAL_WIN -> {
                when {
                    isCashedOut -> "Fez cashout com lucro antes do resultado final"
                    isSystemBet -> "Sistema com acerto parcial (ex: sistema 4/5, errou 1)"
                    else -> "Recebeu pagamento parcial (seleções anuladas ou sistema)"
                }
            }

            FinancialStatus.BREAK_EVEN -> {
                when {
                    isCashedOut -> "Fez cashout no valor exato da entrada (sem lucro nem prejuízo)"
                    isSystemBet -> "Sistema com retorno igual à entrada"
                    else -> "Uma ou mais seleções foram anuladas, valor devolvido"
                }
            }

            FinancialStatus.PARTIAL_LOSS -> {
                when {
                    isCashedOut -> "Fez cashout com prejuízo antes de perder tudo"
                    isSystemBet -> "Sistema com erro dentro da margem (ex: 4/5, errou 1, retorno < entrada)"
                    else -> "Recebeu parte do valor de volta"
                }
            }

            FinancialStatus.TOTAL_LOSS -> {
                "Perdeu todas as seleções ou valor total"
            }

            FinancialStatus.PENDING -> {
                "Aguardando resultado das seleções"
            }
        }
    }
}
