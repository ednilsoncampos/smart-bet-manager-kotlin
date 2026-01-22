package com.smartbet.domain.entity

import com.smartbet.domain.enum.SelectionStatus

/**
 * Entidade de domínio representando um componente individual de uma aposta combinada (Bet Builder).
 * 
 * Quando o usuário usa a funcionalidade "Criar Aposta" da casa de apostas,
 * múltiplos mercados são agrupados em uma única seleção. Cada mercado
 * individual é representado por um BetSelectionComponent.
 * 
 * Exemplo: Uma aposta "Criar Aposta" no jogo Bayern x Union pode conter:
 * - Resultado Final: Bayern vence (1)
 * - Total de Gols Bayern: Mais de 1.5
 * - Jogador Chutes no Gol: Kane - Mais de 0.5
 */
data class BetSelectionComponent(
    val id: Long? = null,
    
    /** ID da seleção pai (BetSelection) */
    val selectionId: Long,
    
    /** ID do mercado na casa de apostas */
    val marketId: String? = null,
    
    /** Nome do mercado - ex: "Resultado Final", "Total de Gols" */
    val marketName: String,
    
    /** Nome da seleção escolhida - ex: "1", "Mais de 1.5", "Kane - Mais de 0.5" */
    val selectionName: String,
    
    /** Status do componente */
    val status: SelectionStatus = SelectionStatus.PENDING,
    
    /** Timestamp de criação (milissegundos desde epoch UTC) */
    val createdAt: Long = System.currentTimeMillis()
)
