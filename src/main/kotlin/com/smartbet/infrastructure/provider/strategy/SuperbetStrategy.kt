package com.smartbet.infrastructure.provider.strategy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartbet.domain.enum.BetType
import com.smartbet.domain.enum.SelectionStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.presentation.exception.InvalidTicketDataException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

/**
 * Strategy para parser de bilhetes da Superbet.
 * 
 * URLs suportadas:
 * - https://superbet.bet.br/bilhete-compartilhado/{CODE}
 * - https://superbet.com.br/bilhete-compartilhado/{CODE}
 * 
 * Estrutura do JSON:
 * - ticketId: ID do bilhete
 * - status: win, lost, open, etc.
 * - coefficient: odd total
 * - payment.stake: valor apostado
 * - win.totalWinnings: ganho total
 * - events[]: lista de eventos/seleções
 *   - name: ["Time1", "Time2"]
 *   - date: data do evento
 *   - status: win, lost, etc.
 *   - coefficient: odd do evento
 *   - eventComponents[]: mercados combinados
 *     - market.name: nome do mercado
 *     - oddComponent.name: seleção escolhida
 *     - oddComponent.oddStatus: WIN, LOST, etc.
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
        val data = when {
            root.path("data").isObject -> root.path("data")
            root.path("ticket").isObject -> root.path("ticket")
            else -> root
        }
        
        // ID do bilhete
        val ticketId = data.path("ticketId").asText()
            .ifEmpty { data.path("id").asText() }
            .ifEmpty { data.path("code").asText() }
        
        // Valor apostado - está em payment.stake
        val stake = data.path("payment").path("stake").asDouble()
            .takeIf { it > 0 }
            ?: data.path("stake").asDouble()
            .takeIf { it > 0 }
            ?: data.path("totalStake").asDouble()
        
        // Odd total - está em coefficient
        val totalOdd = data.path("coefficient").asDouble()
            .takeIf { it > 0 }
            ?: data.path("totalOdds").asDouble()
            .takeIf { it > 0 }
            ?: calculateTotalOdd(data.path("events"))
        
        // Ganho potencial - está em win.potentialTotalWinnings ou win.potentialPayoff
        val winNode = data.path("win")
        val potentialPayout = winNode.path("potentialTotalWinnings").asDouble()
            .takeIf { it > 0 }
            ?: winNode.path("potentialPayoff").asDouble()
            .takeIf { it > 0 }
            ?: data.path("potentialWin").asDouble()
            .takeIf { it > 0 }
            ?: data.path("potentialPayout").asDouble()
            .takeIf { it > 0 }
            ?: (stake * totalOdd)
        
        // Ganho real - está em win.payoff ou win.totalWinnings
        val actualPayout = winNode.path("payoff").asDouble()
            .takeIf { it > 0 }
            ?: winNode.path("totalWinnings").asDouble()
            .takeIf { it > 0 }
            ?: data.path("payout").asDouble()
            .takeIf { it > 0 }
        
        // Status do bilhete
        val statusStr = data.path("status").asText().lowercase()
        val ticketStatus = mapTicketStatus(statusStr)
        
        // Tipo de aposta - verifica se tem system
        val betType = determineBetType(data)
        val systemDescription = if (betType == BetType.SYSTEM) {
            val systemNode = data.path("system")
            
            val selected = when {
                systemNode.path("selected").isArray && systemNode.path("selected").size() > 0 ->
                    systemNode.path("selected")[0].asInt()
                systemNode.path("fixed").asInt() > 0 ->
                    systemNode.path("fixed").asInt()
                else -> 0
            }
            
            val count = systemNode.path("count").asInt()
            
            if (selected > 0 && count > 0) "$selected/$count" else null
        } else null
        
        // Datas
        val placedAt = data.path("dateReceived").asText()
            .takeIf { it.isNotEmpty() }
            ?.let { parseTimestamp(it) }
        
        val settledAt = data.path("dateLastModified").asText()
            .takeIf { it.isNotEmpty() && ticketStatus != TicketStatus.OPEN }
            ?.let { parseTimestamp(it) }
        
        // Seleções - estão em "events"
        val selections = parseSelections(data.path("events"))
        
        // Validar que stake e odd total não estão zerados
        validateTicketData(ticketId, stake, totalOdd)
        
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
    
    private fun calculateTotalOdd(events: JsonNode): Double {
        if (events.isMissingNode || !events.isArray) return 1.0
        
        var totalOdd = 1.0
        for (event in events) {
            val odd = event.path("coefficient").asDouble()
                .takeIf { it > 0 }
                ?: event.path("odd").path("coefficient").asDouble()
                .takeIf { it > 0 }
                ?: 1.0
            totalOdd *= odd
        }
        return totalOdd
    }
    
    private fun determineBetType(data: JsonNode): BetType {
        val systemNode = data.path("system")
        val hasSystem = !systemNode.isMissingNode && systemNode.path("count").asInt() > 0
        val eventsCount = data.path("events").size()
        
        return when {
            hasSystem -> BetType.SYSTEM
            eventsCount > 1 -> BetType.MULTIPLE
            else -> BetType.SINGLE
        }
    }
    
    private fun mapTicketStatus(status: String): TicketStatus {
        return when {
            status.contains("open") || status.contains("pending") || status.contains("active") -> TicketStatus.OPEN
            status.contains("won") || status == "win" -> TicketStatus.WON
            status.contains("lost") || status == "lose" -> TicketStatus.LOST
            status.contains("void") || status.contains("cancelled") -> TicketStatus.VOID
            status.contains("cashout") || status.contains("cashed") -> TicketStatus.CASHOUT
            else -> TicketStatus.OPEN
        }
    }
    
    /**
     * Parseia os eventos do bilhete.
     * Cada evento pode ter múltiplos eventComponents (mercados combinados).
     * Nesse caso, criamos uma seleção para cada eventComponent.
     */
    private fun parseSelections(eventsNode: JsonNode): List<ParsedSelectionData> {
        if (eventsNode.isMissingNode || !eventsNode.isArray) {
            return emptyList()
        }
        
        val selections = mutableListOf<ParsedSelectionData>()
        
        for (event in eventsNode) {
            // Nome do evento - array ["Time1", "Time2"]
            val nameArray = event.path("name")
            val eventName = if (nameArray.isArray && nameArray.size() >= 2) {
                "${nameArray[0].asText()} x ${nameArray[1].asText()}"
            } else {
                nameArray.asText().ifEmpty { "Evento desconhecido" }
            }
            
            // Data do evento
            val eventDate = event.path("date").asText()
                .takeIf { it.isNotEmpty() }
                ?.let { parseTimestamp(it) }
            
            // Status do evento
            val eventStatusStr = event.path("status").asText().lowercase()
            val eventStatus = mapSelectionStatus(eventStatusStr)
            
            // Odd do evento
            val eventOdd = event.path("coefficient").asDouble()
                .takeIf { it > 0 }
                ?: event.path("odd").path("coefficient").asDouble()
                .takeIf { it > 0 }
                ?: 1.0
            
            // Verifica se tem eventComponents (mercados combinados)
            val eventComponents = event.path("eventComponents")
            
            if (!eventComponents.isMissingNode && eventComponents.isArray && eventComponents.size() > 0) {
                // Múltiplos mercados no mesmo evento - criar uma seleção para cada
                for (component in eventComponents) {
                    val marketName = component.path("market").path("name").asText()
                        .takeIf { it.isNotEmpty() }
                    
                    val oddComponent = component.path("oddComponent")
                    val selectionName = oddComponent.path("name").asText()
                        .ifEmpty { "Seleção desconhecida" }
                    
                    val componentStatusStr = oddComponent.path("oddStatus").asText().lowercase()
                        .ifEmpty { oddComponent.path("status").asText().lowercase() }
                        .ifEmpty { component.path("status").asText().lowercase() }
                    val componentStatus = mapSelectionStatus(componentStatusStr)
                    
                    // Usa a odd do evento (não divide)
                    val componentOdd = eventOdd.takeIf { it > 0 } ?: 1.0
                    
                    selections.add(
                        ParsedSelectionData(
                            externalSelectionId = oddComponent.path("oddUuid").asText()
                                .ifEmpty { oddComponent.path("oddId").asText() }
                                .takeIf { it.isNotEmpty() },
                            eventName = eventName,
                            tournamentName = null, // Superbet não retorna torneio diretamente
                            marketType = marketName,
                            selection = selectionName,
                            odd = BigDecimal.valueOf(componentOdd),
                            status = componentStatus,
                            eventDate = eventDate,
                            eventResult = null
                        )
                    )
                }
            } else {
                // Evento simples sem eventComponents
                val marketName = event.path("market").path("name").asText()
                    .takeIf { it.isNotEmpty() }
                
                val oddNode = event.path("odd")
                val selectionName = oddNode.path("name").asText()
                    .ifEmpty { event.path("outcomeName").asText() }
                    .ifEmpty { "Seleção desconhecida" }
                
                selections.add(
                    ParsedSelectionData(
                        externalSelectionId = oddNode.path("oddUuid").asText()
                            .ifEmpty { oddNode.path("oddId").asText() }
                            .ifEmpty { event.path("eventId").asText() }
                            .takeIf { it.isNotEmpty() },
                        eventName = eventName,
                        tournamentName = null,
                        marketType = marketName,
                        selection = selectionName,
                        odd = BigDecimal.valueOf(eventOdd),
                        status = eventStatus,
                        eventDate = eventDate,
                        eventResult = null
                    )
                )
            }
        }
        
        return selections
    }
    
    private fun mapSelectionStatus(status: String): SelectionStatus {
        return when {
            status.contains("pending") || status.contains("open") || status.contains("active") || status.isEmpty() -> SelectionStatus.PENDING
            status.contains("won") || status == "win" -> SelectionStatus.WON
            status.contains("lost") || status == "lose" -> SelectionStatus.LOST
            status.contains("void") || status.contains("cancelled") -> SelectionStatus.VOID
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
    
    private fun validateTicketData(ticketId: String, stake: Double, totalOdd: Double) {
        val errors = mutableMapOf<String, String>()
        
        if (stake <= 0) {
            errors["stake"] = "Valor apostado deve ser maior que zero (recebido: $stake)"
        }
        
        if (totalOdd <= 0 || totalOdd == 1.0) {
            errors["totalOdd"] = "Odd total deve ser maior que 1.0 (recebido: $totalOdd)"
        }
        
        if (errors.isNotEmpty()) {
            throw InvalidTicketDataException(
                "Bilhete $ticketId contém dados inválidos: ${errors.values.joinToString(", ")}",
                errors
            )
        }
    }
}
