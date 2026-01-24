package com.smartbet.infrastructure.provider.strategy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.smartbet.domain.enum.BetType
import com.smartbet.domain.enum.SelectionStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.infrastructure.provider.gateway.HttpGateway
import com.smartbet.presentation.exception.InvalidTicketDataException
import org.slf4j.LoggerFactory
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
 * - win.isCashedOut: indica se foi feito cashout
 * - events[]: lista de eventos/seleções
 *   - name: ["Time1", "Time2"]
 *   - date: data do evento
 *   - status: win, lost, etc.
 *   - coefficient: odd do evento
 *   - eventComponents[]: mercados combinados (Bet Builder)
 *     - market.name: nome do mercado
 *     - oddComponent.name: seleção escolhida
 *     - oddComponent.oddStatus: WIN, LOST, etc.
 */
@Component
class SuperbetStrategy(
    private val objectMapper: ObjectMapper,
    private val httpGateway: HttpGateway
) : BettingProviderStrategy {

    private val logger = LoggerFactory.getLogger(SuperbetStrategy::class.java)

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
        
        // Cashout - verifica se foi feito cashout
        val isCashedOut = winNode.path("isCashedOut").asBoolean(false)
        
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
        
        // Seleções e componentes - estão em "events"
        val (selections, selectionComponents) = parseSelectionsAndComponents(data.path("events"))
        
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
            isCashedOut = isCashedOut,
            selections = selections,
            selectionComponents = selectionComponents
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
     * Parseia os eventos do bilhete e extrai componentes quando existirem.
     * 
     * Quando o array eventComponents não está vazio, indica que é uma aposta
     * do tipo "Criar Aposta" (Bet Builder), onde múltiplos mercados são
     * agrupados em um único evento.
     * 
     * @return Pair de (seleções, mapa de componentes por selectionId)
     */
    private fun parseSelectionsAndComponents(
        eventsNode: JsonNode
    ): Pair<List<ParsedSelectionData>, Map<String, List<ParsedSelectionComponentData>>> {
        if (eventsNode.isMissingNode || !eventsNode.isArray) {
            return Pair(emptyList(), emptyMap())
        }
        
        val selections = mutableListOf<ParsedSelectionData>()
        val componentsMap = mutableMapOf<String, List<ParsedSelectionComponentData>>()
        
        for (event in eventsNode) {
            // Nome do evento - array ["Time1", "Time2"]
            val nameArray = event.path("name")
            val eventName = if (nameArray.isArray && nameArray.size() >= 2) {
                "${nameArray[0].asText()} x ${nameArray[1].asText()}"
            } else {
                nameArray.asText().ifEmpty { "Evento desconhecido" }
            }
            
            // Data do evento
            val eventDateStr = event.path("date").asText().takeIf { it.isNotEmpty() }
            val eventDate = eventDateStr?.let { parseTimestamp(it) }
            
            // Status do evento
            val eventStatusStr = event.path("status").asText().lowercase()
            val eventStatus = mapSelectionStatus(eventStatusStr)
            
            // Odd do evento
            val eventOdd = event.path("coefficient").asDouble()
                .takeIf { it > 0 }
                ?: event.path("odd").path("coefficient").asDouble()
                .takeIf { it > 0 }
                ?: 1.0
            
            // Sport ID - ex: "5" para futebol
            val sportId = event.path("sportId").asText()
                .takeIf { it.isNotEmpty() }
            
            // Tournament ID - usado para buscar nome do torneio
            val tournamentId = event.path("tournamentId").asText()
                .takeIf { it.isNotEmpty() }
            
            // Verifica se tem eventComponents (mercados combinados)
            val eventComponents = event.path("eventComponents")
            
            // Detecta se é Bet Builder: array eventComponents não está vazio
            val hasBetBuilderComponents = !eventComponents.isMissingNode && 
                                          eventComponents.isArray && 
                                          eventComponents.size() > 0
            
            // ID único para a seleção
            val oddNode = event.path("odd")
            val selectionId = oddNode.path("oddUuid").asText()
                .ifEmpty { oddNode.path("oddId").asText() }
                .ifEmpty { event.path("eventId").asText() }
            
            if (hasBetBuilderComponents) {
                // É Bet Builder - criar uma seleção principal e extrair componentes
                val components = mutableListOf<ParsedSelectionComponentData>()
                
                // Construir nome da seleção combinando os componentes
                val selectionNames = mutableListOf<String>()
                
                for (component in eventComponents) {
                    val marketId = component.path("market").path("marketId").asText()
                        .takeIf { it.isNotEmpty() }
                    val marketName = component.path("market").path("name").asText()
                        .ifEmpty { "Mercado desconhecido" }
                    
                    val oddComponent = component.path("oddComponent")
                    val componentSelectionName = oddComponent.path("name").asText()
                        .ifEmpty { "Seleção desconhecida" }
                    
                    val componentStatusStr = oddComponent.path("oddStatus").asText().lowercase()
                        .ifEmpty { oddComponent.path("status").asText().lowercase() }
                        .ifEmpty { component.path("status").asText().lowercase() }
                    val componentStatus = mapSelectionStatus(componentStatusStr)
                    
                    // Adicionar ao nome combinado
                    selectionNames.add("$marketName: $componentSelectionName")
                    
                    // Criar componente
                    components.add(
                        ParsedSelectionComponentData(
                            marketId = marketId,
                            marketName = marketName,
                            selectionName = componentSelectionName,
                            status = componentStatus
                        )
                    )
                }
                
                // Criar seleção principal com nome combinado
                val combinedSelection = selectionNames.joinToString(" | ")
                
                selections.add(
                    ParsedSelectionData(
                        externalSelectionId = selectionId.takeIf { it.isNotEmpty() },
                        eventName = eventName,
                        externalTournamentId = tournamentId?.toIntOrNull(),
                        marketType = "Criar Aposta", // Market type fixo para Bet Builder
                        selection = combinedSelection,
                        odd = BigDecimal.valueOf(eventOdd),
                        status = eventStatus,
                        eventDate = eventDate,
                        eventResult = null,
                        sportId = sportId,
                        isBetBuilder = true
                    )
                )
                
                // Mapear componentes pelo selectionId
                if (selectionId.isNotEmpty() && components.isNotEmpty()) {
                    componentsMap[selectionId] = components
                }
            } else {
                // Evento simples sem eventComponents
                val marketName = event.path("market").path("name").asText()
                    .takeIf { it.isNotEmpty() }
                
                val selectionName = oddNode.path("name").asText()
                    .ifEmpty { event.path("outcomeName").asText() }
                    .ifEmpty { "Seleção desconhecida" }
                
                selections.add(
                    ParsedSelectionData(
                        externalSelectionId = selectionId.takeIf { it.isNotEmpty() },
                        eventName = eventName,
                        externalTournamentId = tournamentId?.toIntOrNull(),
                        marketType = marketName,
                        selection = selectionName,
                        odd = BigDecimal.valueOf(eventOdd),
                        status = eventStatus,
                        eventDate = eventDate,
                        eventResult = null,
                        sportId = sportId,
                        isBetBuilder = false
                    )
                )
            }
        }
        
        return Pair(selections, componentsMap)
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
