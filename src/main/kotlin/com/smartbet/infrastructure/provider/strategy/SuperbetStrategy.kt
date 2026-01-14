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
 * Strategy para parser de bilhetes da Superbet.
 * 
 * URLs suportadas:
 * - https://superbet.bet.br/bilhete-compartilhado/{CODE}
 * - https://superbet.com.br/bilhete-compartilhado/{CODE}
 */
@Component
class SuperbetStrategy(
    private val objectMapper: ObjectMapper
) : BettingProviderStrategy {
    
    override val slug: String = "superbet"
    override val name: String = "Superbet"
    
    override val urlPatterns: List<Regex> = listOf(
        Regex("""superbet\.(?:bet|com)\.br/bilhete-compartilhado/([A-Za-z0-9-]+)"""),
        Regex("""superbet\.(?:bet|com)\.br/.*[?&]code=([A-Za-z0-9-]+)""")
    )
    
    override val defaultApiTemplate: String = 
        "https://superbet.bet.br/api/content/betslip/{CODE}"
    
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
        val root = objectMapper.readTree(responseBody)
        
        // Superbet pode retornar dados em diferentes estruturas
        val data = root.path("data").takeIf { !it.isMissingNode } ?: root
        
        val ticketId = data.path("ticketId").asText()
            .ifEmpty { data.path("id").asText() }
            .ifEmpty { data.path("code").asText() }
        
        val stake = data.path("stake").asDouble()
            .takeIf { it > 0 }
            ?: data.path("totalStake").asDouble()
        
        val totalOdd = data.path("totalOdds").asDouble()
            .takeIf { it > 0 }
            ?: data.path("odds").asDouble()
            .takeIf { it > 0 }
            ?: calculateTotalOdd(data.path("selections"))
        
        val potentialPayout = data.path("potentialWin").asDouble()
            .takeIf { it > 0 }
            ?: data.path("potentialPayout").asDouble()
            .takeIf { it > 0 }
            ?: (stake * totalOdd)
        
        val actualPayout = data.path("payout").asDouble()
            .takeIf { it > 0 }
            ?: data.path("winnings").asDouble()
            .takeIf { it > 0 }
        
        val statusStr = data.path("status").asText().lowercase()
        val ticketStatus = mapTicketStatus(statusStr)
        
        val betType = determineBetType(data)
        val systemDescription = data.path("systemType").asText()
            .takeIf { it.isNotEmpty() && betType == BetType.SYSTEM }
        
        val placedAt = data.path("placedAt").asText()
            .takeIf { it.isNotEmpty() }
            ?.let { parseTimestamp(it) }
            ?: data.path("createdAt").asText()
                .takeIf { it.isNotEmpty() }
                ?.let { parseTimestamp(it) }
        
        val settledAt = data.path("settledAt").asText()
            .takeIf { it.isNotEmpty() }
            ?.let { parseTimestamp(it) }
        
        val selections = parseSelections(data.path("selections"))
        
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
    
    private fun calculateTotalOdd(selections: JsonNode): Double {
        if (selections.isMissingNode || !selections.isArray) return 1.0
        
        var totalOdd = 1.0
        for (selection in selections) {
            val odd = selection.path("odds").asDouble()
                .takeIf { it > 0 }
                ?: selection.path("odd").asDouble()
                .takeIf { it > 0 }
                ?: 1.0
            totalOdd *= odd
        }
        return totalOdd
    }
    
    private fun determineBetType(data: JsonNode): BetType {
        val typeStr = data.path("betType").asText().lowercase()
        val systemType = data.path("systemType").asText()
        val selectionsCount = data.path("selections").size()
        
        return when {
            typeStr.contains("system") || systemType.isNotEmpty() -> BetType.SYSTEM
            selectionsCount > 1 -> BetType.MULTIPLE
            else -> BetType.SINGLE
        }
    }
    
    private fun mapTicketStatus(status: String): TicketStatus {
        return when {
            status.contains("open") || status.contains("pending") -> TicketStatus.OPEN
            status.contains("won") || status.contains("win") -> TicketStatus.WON
            status.contains("lost") || status.contains("lose") -> TicketStatus.LOST
            status.contains("void") || status.contains("cancelled") -> TicketStatus.VOID
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
                .ifEmpty { "${node.path("homeTeam").asText()} x ${node.path("awayTeam").asText()}" }
            
            val tournamentName = node.path("tournamentName").asText()
                .ifEmpty { node.path("competition").path("name").asText() }
                .ifEmpty { node.path("league").asText() }
                .takeIf { it.isNotEmpty() }
            
            val marketType = node.path("marketName").asText()
                .ifEmpty { node.path("market").path("name").asText() }
                .ifEmpty { node.path("betType").asText() }
                .takeIf { it.isNotEmpty() }
            
            val selection = node.path("outcomeName").asText()
                .ifEmpty { node.path("outcome").path("name").asText() }
                .ifEmpty { node.path("selection").asText() }
            
            val odd = node.path("odds").asDouble()
                .takeIf { it > 0 }
                ?: node.path("odd").asDouble()
                .takeIf { it > 0 }
                ?: 1.0
            
            val statusStr = node.path("status").asText().lowercase()
            val status = mapSelectionStatus(statusStr)
            
            val eventDate = node.path("eventDate").asText()
                .takeIf { it.isNotEmpty() }
                ?.let { parseTimestamp(it) }
                ?: node.path("startTime").asText()
                    .takeIf { it.isNotEmpty() }
                    ?.let { parseTimestamp(it) }
            
            val eventResult = node.path("result").asText()
                .ifEmpty { node.path("score").asText() }
                .takeIf { it.isNotEmpty() }
            
            ParsedSelectionData(
                externalSelectionId = node.path("id").asText().takeIf { it.isNotEmpty() },
                eventName = eventName,
                tournamentName = tournamentName,
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
            status.contains("pending") || status.contains("open") || status.isEmpty() -> SelectionStatus.PENDING
            status.contains("won") || status.contains("win") -> SelectionStatus.WON
            status.contains("lost") || status.contains("lose") -> SelectionStatus.LOST
            status.contains("void") || status.contains("cancelled") -> SelectionStatus.VOID
            status.contains("cashout") -> SelectionStatus.CASHOUT
            else -> SelectionStatus.PENDING
        }
    }
    
    private fun parseTimestamp(timestamp: String): Instant? {
        return try {
            // Tenta diferentes formatos
            when {
                timestamp.contains("T") -> Instant.parse(timestamp)
                timestamp.matches(Regex("\\d+")) -> Instant.ofEpochMilli(timestamp.toLong())
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
