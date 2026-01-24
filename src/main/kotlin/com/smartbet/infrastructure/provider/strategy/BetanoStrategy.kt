package com.smartbet.infrastructure.provider.strategy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartbet.domain.enum.BetType
import com.smartbet.domain.enum.SelectionStatus
import com.smartbet.domain.enum.TicketStatus
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

/**
 * Strategy para parser de bilhetes da Betano.
 * 
 * URLs suportadas:
 * - https://www.betano.bet.br/mybets/{ID}
 * - https://betano.bet.br/mybets/{ID}
 * - https://betano.com.br/mybets/{ID}
 * - URLs com parâmetro bets={CODE}
 */
@Component
class BetanoStrategy(
    private val objectMapper: ObjectMapper
) : BettingProviderStrategy {
    
    override val slug: String = "betano"
    override val name: String = "Betano"
    
    override val urlPatterns: List<Regex> = listOf(
        Regex("""betano\.(?:bet|com)\.br/mybets/(\d+)"""),
        Regex("""betano\.(?:bet|com)\.br/.*[?&]bets=([A-Za-z0-9-]+)""")
    )
    
    override val defaultApiTemplate: String = 
        "https://www.betano.bet.br/api/betslip/v3/getBetslipById/{CODE}"
    
    override fun extractTicketCode(url: String): String? {
        for (pattern in urlPatterns) {
            val match = pattern.find(url)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }
    
    override fun parseResponse(responseBody: String): ParsedTicketData {
        // Parser da Betano em desenvolvimento - a API não retorna bilhetes já realizados
        throw UnsupportedOperationException(
            "Parser da Betano em desenvolvimento. " +
            "A API da Betano não disponibiliza endpoint público para consulta de bilhetes já realizados. " +
            "Por favor, utilize outra casa de apostas ou aguarde atualizações."
        )
        
        // Código original mantido para referência futura
        @Suppress("UNREACHABLE_CODE")
        val root = objectMapper.readTree(responseBody)
        
        // Betano pode retornar dados em diferentes estruturas
        val data = root.path("data").takeIf { !it.isMissingNode }
            ?: root.path("betslip").takeIf { !it.isMissingNode }
            ?: root
        
        val ticketId = data.path("betslipId").asText()
            .ifEmpty { data.path("id").asText() }
            .ifEmpty { data.path("betId").asText() }
        
        val stake = data.path("stake").asDouble()
            .takeIf { it > 0 }
            ?: data.path("totalStake").asDouble()
            .takeIf { it > 0 }
            ?: data.path("amount").asDouble()
        
        val totalOdd = data.path("totalOdds").asDouble()
            .takeIf { it > 0 }
            ?: data.path("combinedOdds").asDouble()
            .takeIf { it > 0 }
            ?: calculateTotalOdd(data.path("legs"))
        
        val potentialPayout = data.path("potentialWinnings").asDouble()
            .takeIf { it > 0 }
            ?: data.path("potentialPayout").asDouble()
            .takeIf { it > 0 }
            ?: data.path("maxWin").asDouble()
            .takeIf { it > 0 }
            ?: (stake * totalOdd)
        
        val actualPayout = data.path("payout").asDouble()
            .takeIf { it > 0 }
            ?: data.path("winnings").asDouble()
            .takeIf { it > 0 }
            ?: data.path("cashoutValue").asDouble()
            .takeIf { it > 0 }
        
        val statusStr = data.path("status").asText().lowercase()
            .ifEmpty { data.path("betStatus").asText().lowercase() }
        val ticketStatus = mapTicketStatus(statusStr)
        
        val betType = determineBetType(data)
        val systemDescription = data.path("systemBetType").asText()
            .ifEmpty { data.path("systemType").asText() }
            .takeIf { it.isNotEmpty() && betType == BetType.SYSTEM }
        
        val placedAt = data.path("placedAt").asText()
            .ifEmpty { data.path("createdAt").asText() }
            .ifEmpty { data.path("betDate").asText() }
            .takeIf { it.isNotEmpty() }
            ?.let { parseTimestamp(it) }
        
        val settledAt = data.path("settledAt").asText()
            .ifEmpty { data.path("resultDate").asText() }
            .takeIf { it.isNotEmpty() }
            ?.let { parseTimestamp(it) }
        
        // Betano usa "legs" ou "selections" para as seleções
        val selectionsNode = data.path("legs").takeIf { !it.isMissingNode && it.isArray }
            ?: data.path("selections").takeIf { !it.isMissingNode && it.isArray }
            ?: data.path("bets")
        
        val selections = parseSelections(selectionsNode)
        
        return ParsedTicketData(
            externalTicketId = ticketId,
            betType = betType,
            stake = BigDecimal.valueOf(stake),
            totalOdd = BigDecimal.valueOf(totalOdd),
            potentialPayout = BigDecimal.valueOf(potentialPayout),
            actualPayout = actualPayout?.let { BigDecimal.valueOf(it) },
            ticketStatus = ticketStatus,
            systemDescription = systemDescription,
            placedAt = placedAt,
            settledAt = settledAt,
            selections = selections
        )
    }
    
    private fun calculateTotalOdd(legs: JsonNode): Double {
        if (legs.isMissingNode || !legs.isArray) return 1.0
        
        var totalOdd = 1.0
        for (leg in legs) {
            val odd = leg.path("odds").asDouble()
                .takeIf { it > 0 }
                ?: leg.path("price").asDouble()
                .takeIf { it > 0 }
                ?: 1.0
            totalOdd *= odd
        }
        return totalOdd
    }
    
    private fun determineBetType(data: JsonNode): BetType {
        val typeStr = data.path("betType").asText().lowercase()
            .ifEmpty { data.path("type").asText().lowercase() }
        val systemType = data.path("systemBetType").asText()
        val legsCount = data.path("legs").size()
            .takeIf { it > 0 }
            ?: data.path("selections").size()
        
        return when {
            typeStr.contains("system") || systemType.isNotEmpty() -> BetType.SYSTEM
            typeStr.contains("combo") || typeStr.contains("multiple") || legsCount > 1 -> BetType.MULTIPLE
            else -> BetType.SINGLE
        }
    }
    
    private fun mapTicketStatus(status: String): TicketStatus {
        return when {
            status.contains("open") || status.contains("pending") || status.contains("active") -> TicketStatus.OPEN
            status.contains("won") || status.contains("win") || status.contains("ganhou") -> TicketStatus.WON
            status.contains("lost") || status.contains("lose") || status.contains("perdeu") -> TicketStatus.LOST
            status.contains("void") || status.contains("cancelled") || status.contains("anulad") -> TicketStatus.VOID
            status.contains("cashout") || status.contains("cashed") -> TicketStatus.CASHOUT
            else -> TicketStatus.OPEN
        }
    }
    
    private fun parseSelections(selectionsNode: JsonNode): List<ParsedSelectionData> {
        if (selectionsNode.isMissingNode || !selectionsNode.isArray) {
            return emptyList()
        }
        
        return selectionsNode.map { node ->
            val eventName = node.path("eventName").asText()
                .ifEmpty { node.path("event").path("name").asText() }
                .ifEmpty { node.path("matchName").asText() }
                .ifEmpty { 
                    val home = node.path("homeTeam").asText()
                    val away = node.path("awayTeam").asText()
                    if (home.isNotEmpty() && away.isNotEmpty()) "$home x $away" else ""
                }
            
            // Betano não usa o mesmo sistema de tournamentId que a Superbet
            // Será necessário um mapeamento futuro para resolver o externalTournamentId
            val externalTournamentId: Int? = null
            
            val marketType = node.path("marketName").asText()
                .ifEmpty { node.path("market").path("name").asText() }
                .ifEmpty { node.path("betTypeName").asText() }
                .takeIf { it.isNotEmpty() }
            
            val selection = node.path("outcomeName").asText()
                .ifEmpty { node.path("outcome").path("name").asText() }
                .ifEmpty { node.path("selectionName").asText() }
                .ifEmpty { node.path("pick").asText() }
            
            val odd = node.path("odds").asDouble()
                .takeIf { it > 0 }
                ?: node.path("price").asDouble()
                .takeIf { it > 0 }
                ?: 1.0
            
            val statusStr = node.path("status").asText().lowercase()
                .ifEmpty { node.path("legStatus").asText().lowercase() }
            val status = mapSelectionStatus(statusStr)
            
            val eventDate = node.path("eventDate").asText()
                .ifEmpty { node.path("startTime").asText() }
                .ifEmpty { node.path("matchDate").asText() }
                .takeIf { it.isNotEmpty() }
                ?.let { parseTimestamp(it) }
            
            val eventResult = node.path("result").asText()
                .ifEmpty { node.path("score").asText() }
                .ifEmpty { node.path("finalScore").asText() }
                .takeIf { it.isNotEmpty() }
            
            ParsedSelectionData(
                externalSelectionId = node.path("legId").asText()
                    .ifEmpty { node.path("id").asText() }
                    .takeIf { it.isNotEmpty() },
                eventName = eventName,
                externalTournamentId = externalTournamentId,
                marketType = marketType,
                selection = selection,
                odd = BigDecimal.valueOf(odd),
                status = status,
                eventDate = eventDate,
                eventResult = eventResult
            )
        }
    }
    
    private fun mapSelectionStatus(status: String): SelectionStatus {
        return when {
            status.contains("pending") || status.contains("open") || status.contains("active") || status.isEmpty() -> SelectionStatus.PENDING
            status.contains("won") || status.contains("win") || status.contains("ganhou") -> SelectionStatus.WON
            status.contains("lost") || status.contains("lose") || status.contains("perdeu") -> SelectionStatus.LOST
            status.contains("void") || status.contains("cancelled") || status.contains("anulad") -> SelectionStatus.VOID
            status.contains("cashout") -> SelectionStatus.CASHOUT
            else -> SelectionStatus.PENDING
        }
    }
    
    /**
     * Parseia timestamp para milissegundos (epoch).
     */
    private fun parseTimestamp(timestamp: String): Long? {
        return try {
            when {
                timestamp.contains("T") -> Instant.parse(timestamp).toEpochMilli()
                timestamp.matches(Regex("\\d+")) -> timestamp.toLong()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
