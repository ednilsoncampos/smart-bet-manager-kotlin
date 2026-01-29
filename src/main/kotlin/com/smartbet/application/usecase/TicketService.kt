package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.domain.entity.BetSelection
import com.smartbet.domain.entity.BetSelectionComponent
import com.smartbet.domain.entity.BetTicket
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.domain.exception.DuplicateTicketException
import com.smartbet.domain.service.BetStatusCalculator
import com.smartbet.infrastructure.persistence.entity.BetSelectionComponentEntity
import com.smartbet.infrastructure.persistence.entity.BetSelectionEntity
import com.smartbet.infrastructure.persistence.entity.BetTicketEntity
import com.smartbet.infrastructure.persistence.entity.TournamentEntity
import com.smartbet.infrastructure.persistence.repository.BetSelectionComponentRepository
import com.smartbet.infrastructure.persistence.repository.BetSelectionRepository
import com.smartbet.infrastructure.persistence.repository.BetTicketRepository
import com.smartbet.infrastructure.persistence.repository.BettingProviderRepository
import com.smartbet.infrastructure.persistence.repository.TournamentRepository
import com.smartbet.infrastructure.provider.gateway.HttpGateway
import com.smartbet.infrastructure.provider.strategy.BettingProviderFactory
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TicketService(
    private val ticketRepository: BetTicketRepository,
    private val selectionRepository: BetSelectionRepository,
    private val selectionComponentRepository: BetSelectionComponentRepository,
    private val providerRepository: BettingProviderRepository,
    private val tournamentRepository: TournamentRepository,
    private val providerFactory: BettingProviderFactory,
    private val httpGateway: HttpGateway
) {
    private val logger = LoggerFactory.getLogger(TicketService::class.java)
    
    /**
     * Importa um bilhete a partir de uma URL compartilhada.
     */
    @Transactional
    fun importFromUrl(userId: Long, request: ImportTicketRequest): TicketResponse {
        logger.info("Importing ticket from URL: {} for user: {}", request.url, userId)
        
        // Encontra a strategy apropriada
        val strategy = providerFactory.findStrategyForUrl(request.url)
            ?: throw IllegalArgumentException("URL não suportada: ${request.url}")
        
        // Busca o provider no banco
        val provider = providerRepository.findBySlug(strategy.slug)
            ?: throw IllegalStateException("Provider não encontrado: ${strategy.slug}")
        
        // Extrai o código do bilhete
        val ticketCode = strategy.extractTicketCode(request.url)
            ?: throw IllegalArgumentException("Não foi possível extrair o código do bilhete da URL")
        
        // Verifica se o bilhete já existe para este usuário
        val existingTicket = ticketRepository.findByUserIdAndExternalTicketId(userId, ticketCode)
        if (existingTicket != null) {
            logger.warn("Duplicate ticket detected: {} for user: {}", ticketCode, userId)
            throw DuplicateTicketException(
                ticketId = ticketCode,
                message = "Bilhete $ticketCode já foi importado anteriormente"
            )
        }
        
        // Busca os dados da API
        val apiUrl = strategy.buildApiUrl(ticketCode, provider.apiUrlTemplate)
        val responseBody = httpGateway.get(apiUrl)
        
        // Parseia a resposta
        val parsedData = strategy.parseResponse(responseBody)
        
        // Calcula o status financeiro
        val statusResult = BetStatusCalculator.calculateFromTicket(
            stake = parsedData.stake,
            actualPayout = parsedData.actualPayout,
            potentialPayout = parsedData.potentialPayout,
            ticketStatus = parsedData.ticketStatus
        )
        
        // Cria a entidade do bilhete
        val ticketEntity = BetTicketEntity(
            userId = userId,
            providerId = provider.id!!,
            bankrollId = request.bankrollId,
            externalTicketId = parsedData.externalTicketId,
            sourceUrl = request.url,
            betType = parsedData.betType,
            stake = parsedData.stake,
            totalOdd = parsedData.totalOdd,
            potentialPayout = parsedData.potentialPayout,
            actualPayout = parsedData.actualPayout,
            ticketStatus = parsedData.ticketStatus,
            financialStatus = statusResult.financialStatus,
            profitLoss = statusResult.profitLoss,
            roi = statusResult.roi,
            systemDescription = parsedData.systemDescription,
            placedAt = parsedData.placedAt,
            settledAt = parsedData.settledAt,
            isCashedOut = parsedData.isCashedOut
        )
        
        // Salva o bilhete
        val savedTicket = ticketRepository.save(ticketEntity)
        
        // Cria as seleções (resolvendo Tournament pelo externalTournamentId)
        val selectionEntities = parsedData.selections.map { selectionData ->
            val tournament = selectionData.externalTournamentId?.let { extId ->
                tournamentRepository.findByProviderIdAndExternalId(provider.id!!, extId)
            }

            BetSelectionEntity(
                ticket = savedTicket,
                externalSelectionId = selectionData.externalSelectionId,
                eventName = selectionData.eventName,
                tournament = tournament,
                marketType = selectionData.marketType,
                selection = selectionData.selection,
                odd = selectionData.odd,
                status = selectionData.status,
                eventDate = selectionData.eventDate,
                eventResult = selectionData.eventResult,
                sportId = selectionData.sportId,
                isBetBuilder = selectionData.isBetBuilder
            )
        }
        
        val savedSelections = selectionRepository.saveAll(selectionEntities)
        savedTicket.selections.addAll(savedSelections)
        
        // Salva os componentes das seleções (para Bet Builder)
        for (savedSelection in savedSelections) {
            val externalId = savedSelection.externalSelectionId ?: continue
            val components = parsedData.selectionComponents[externalId] ?: continue
            
            if (components.isNotEmpty()) {
                val componentEntities = components.map { componentData ->
                    BetSelectionComponentEntity(
                        selection = savedSelection,
                        marketId = componentData.marketId,
                        marketName = componentData.marketName,
                        selectionName = componentData.selectionName,
                        status = componentData.status
                    )
                }
                selectionComponentRepository.saveAll(componentEntities)
                logger.debug("Saved {} components for selection {}", components.size, savedSelection.id)
            }
        }
        
        logger.info("Ticket imported successfully: {}", savedTicket.id)
        
        return TicketResponse.fromDomain(savedTicket.toDomain(), provider.name)
    }
    
    /**
     * Cria um bilhete manualmente.
     */
    @Transactional
    fun createManual(userId: Long, request: CreateManualTicketRequest): TicketResponse {
        logger.info("Creating manual ticket for user: {}", userId)
        
        val provider = providerRepository.findById(request.providerId)
            .orElseThrow { IllegalArgumentException("Provider não encontrado: ${request.providerId}") }
        
        val potentialPayout = request.potentialPayout 
            ?: (request.stake * request.totalOdd)
        
        val now = System.currentTimeMillis()
        
        val ticketEntity = BetTicketEntity(
            userId = userId,
            providerId = provider.id!!,
            bankrollId = request.bankrollId,
            betType = request.betType,
            stake = request.stake,
            totalOdd = request.totalOdd,
            potentialPayout = potentialPayout,
            ticketStatus = TicketStatus.OPEN,
            financialStatus = FinancialStatus.PENDING,
            systemDescription = request.systemDescription,
            placedAt = request.placedAt ?: now
        )
        
        val savedTicket = ticketRepository.save(ticketEntity)
        
        val selectionEntities = request.selections.map { selectionRequest ->
            val tournament = selectionRequest.tournamentId?.let { tournamentId ->
                tournamentRepository.findById(tournamentId).orElse(null)
            }

            BetSelectionEntity(
                ticket = savedTicket,
                eventName = selectionRequest.eventName,
                tournament = tournament,
                marketType = selectionRequest.marketType,
                selection = selectionRequest.selection,
                odd = selectionRequest.odd,
                eventDate = selectionRequest.eventDate
            )
        }
        
        selectionRepository.saveAll(selectionEntities)
        savedTicket.selections.addAll(selectionEntities)
        
        logger.info("Manual ticket created: {}", savedTicket.id)
        
        return TicketResponse.fromDomain(savedTicket.toDomain(), provider.name)
    }
    
    /**
     * Lista bilhetes do usuário com filtros.
     */
    @Transactional(readOnly = true)
    fun listTickets(userId: Long, request: ListTicketsRequest): PagedResponse<TicketResponse> {
        val pageable = PageRequest.of(
            request.page, 
            request.pageSize
        )
        
        val page = ticketRepository.findByFilters(
            userId = userId,
            status = request.status,
            financialStatus = request.financialStatus,
            providerId = request.providerId,
            pageable = pageable
        )
        
        val providerMap = providerRepository.findAll().associateBy { it.id }
        
        return PagedResponse(
            content = page.content.map { entity ->
                TicketResponse.fromDomain(
                    entity.toDomain(),
                    providerMap[entity.providerId]?.name
                )
            },
            page = page.number,
            pageSize = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious()
        )
    }
    
    /**
     * Busca um bilhete por ID.
     */
    @Transactional(readOnly = true)
    fun getById(userId: Long, ticketId: Long): TicketResponse {
        val ticket = ticketRepository.findByIdWithSelections(ticketId)
            ?: throw IllegalArgumentException("Bilhete não encontrado: $ticketId")

        if (ticket.userId != userId) {
            throw IllegalAccessException("Acesso negado ao bilhete: $ticketId")
        }

        val provider = providerRepository.findById(ticket.providerId).orElse(null)

        // Buscar componentes de todas as seleções
        val selectionComponentsMap = ticket.selections
            .mapNotNull { it.id }
            .associateWith { selectionId ->
                selectionComponentRepository.findBySelectionId(selectionId)
                    .map { it.toDomain() }
            }

        return TicketResponse.fromDomain(ticket.toDomain(), provider?.name, selectionComponentsMap)
    }
    
    /**
     * Atualiza o status de um bilhete.
     */
    @Transactional
    fun updateStatus(userId: Long, request: UpdateTicketStatusRequest): TicketResponse {
        logger.info("Updating ticket status: {}", request.ticketId)

        val ticket = ticketRepository.findByIdWithSelections(request.ticketId)
            ?: throw IllegalArgumentException("Bilhete não encontrado: ${request.ticketId}")

        if (ticket.userId != userId) {
            throw IllegalAccessException("Acesso negado ao bilhete: ${request.ticketId}")
        }

        // Atualiza os campos
        request.actualPayout?.let { ticket.actualPayout = it }
        request.ticketStatus?.let {
            ticket.ticketStatus = it
            if (it != TicketStatus.OPEN) {
                ticket.settledAt = System.currentTimeMillis()
            }
        }

        // Recalcula o status financeiro
        val statusResult = BetStatusCalculator.calculateFromTicket(
            stake = ticket.stake,
            actualPayout = ticket.actualPayout,
            potentialPayout = ticket.potentialPayout,
            ticketStatus = ticket.ticketStatus
        )

        ticket.financialStatus = statusResult.financialStatus
        ticket.profitLoss = statusResult.profitLoss
        ticket.roi = statusResult.roi

        val savedTicket = ticketRepository.save(ticket)
        val provider = providerRepository.findById(ticket.providerId).orElse(null)

        logger.info("Ticket status updated: {} -> {}", request.ticketId, statusResult.financialStatus)

        return TicketResponse.fromDomain(savedTicket.toDomain(), provider?.name)
    }
    
    /**
     * Deleta um bilhete.
     */
    @Transactional
    fun delete(userId: Long, ticketId: Long) {
        val ticket = ticketRepository.findById(ticketId)
            .orElseThrow { IllegalArgumentException("Bilhete não encontrado: $ticketId") }
        
        if (ticket.userId != userId) {
            throw IllegalAccessException("Acesso negado ao bilhete: $ticketId")
        }
        
        ticketRepository.delete(ticket)
        logger.info("Ticket deleted: {}", ticketId)
    }
    
    /**
     * Conta bilhetes em aberto de um usuário que podem ser atualizados.
     * Usado para retornar a quantidade antes de iniciar o refresh assíncrono.
     */
    fun countOpenTicketsToRefresh(userId: Long): Int {
        return ticketRepository.findOpenTicketsByUserId(userId).size
    }
    
    /**
     * Atualiza bilhetes em aberto de um usuário.
     * Executa de forma assíncrona quando chamado via @Async.
     * 
     * @param userId ID do usuário
     * @return Resultado do processamento
     */
    @Transactional
    fun refreshOpenTickets(userId: Long): RefreshResult {
        logger.info("Starting refresh of open tickets for user: {}", userId)
        
        val openTickets = ticketRepository.findOpenTicketsByUserId(userId)
        
        if (openTickets.isEmpty()) {
            logger.info("No open tickets to refresh for user: {}", userId)
            return RefreshResult(
                totalProcessed = 0,
                updated = 0,
                unchanged = 0,
                errors = 0
            )
        }
        
        logger.info("Found {} open tickets to refresh for user: {}", openTickets.size, userId)
        
        var updated = 0
        var unchanged = 0
        val errorDetails = mutableListOf<RefreshError>()
        
        for (ticket in openTickets) {
            try {
                val wasUpdated = refreshSingleTicket(ticket)
                if (wasUpdated) {
                    updated++
                } else {
                    unchanged++
                }
            } catch (e: Exception) {
                logger.error("Error refreshing ticket {}: {}", ticket.id, e.message)
                errorDetails.add(RefreshError(
                    ticketId = ticket.id!!,
                    externalTicketId = ticket.externalTicketId,
                    errorMessage = e.message ?: "Unknown error"
                ))
            }
        }
        
        val result = RefreshResult(
            totalProcessed = openTickets.size,
            updated = updated,
            unchanged = unchanged,
            errors = errorDetails.size,
            errorDetails = errorDetails
        )
        
        logger.info("Refresh completed for user {}: {}", userId, result)
        
        return result
    }
    
    /**
     * Atualiza todos os bilhetes em aberto de todos os usuários.
     * Usado pelo job agendado.
     * 
     * @return Resultado do processamento
     */
    @Transactional
    fun refreshAllOpenTickets(): RefreshResult {
        logger.info("Starting scheduled refresh of all open tickets")
        
        val allOpenTickets = ticketRepository.findAllOpenTicketsWithSourceUrl()
        
        if (allOpenTickets.isEmpty()) {
            logger.info("No open tickets to refresh")
            return RefreshResult(
                totalProcessed = 0,
                updated = 0,
                unchanged = 0,
                errors = 0
            )
        }
        
        logger.info("Found {} open tickets to refresh", allOpenTickets.size)
        
        var updated = 0
        var unchanged = 0
        val errorDetails = mutableListOf<RefreshError>()
        
        for (ticket in allOpenTickets) {
            try {
                val wasUpdated = refreshSingleTicket(ticket)
                if (wasUpdated) {
                    updated++
                } else {
                    unchanged++
                }
            } catch (e: Exception) {
                logger.error("Error refreshing ticket {}: {}", ticket.id, e.message)
                errorDetails.add(RefreshError(
                    ticketId = ticket.id!!,
                    externalTicketId = ticket.externalTicketId,
                    errorMessage = e.message ?: "Unknown error"
                ))
            }
        }
        
        val result = RefreshResult(
            totalProcessed = allOpenTickets.size,
            updated = updated,
            unchanged = unchanged,
            errors = errorDetails.size,
            errorDetails = errorDetails
        )
        
        logger.info("Scheduled refresh completed: {}", result)
        
        return result
    }
    
    /**
     * Atualiza um único bilhete consultando a API do provider.
     * 
     * @param ticket Entidade do bilhete a ser atualizado
     * @return true se o bilhete foi atualizado, false se permaneceu igual
     */
    private fun refreshSingleTicket(ticket: BetTicketEntity): Boolean {
        val sourceUrl = ticket.sourceUrl 
            ?: throw IllegalStateException("Ticket ${ticket.id} has no sourceUrl")
        
        // Encontra a strategy apropriada
        val strategy = providerFactory.findStrategyForUrl(sourceUrl)
            ?: throw IllegalStateException("No strategy found for URL: $sourceUrl")
        
        // Busca o provider no banco
        val provider = providerRepository.findById(ticket.providerId).orElse(null)
            ?: throw IllegalStateException("Provider not found: ${ticket.providerId}")
        
        // Extrai o código do bilhete
        val ticketCode = strategy.extractTicketCode(sourceUrl)
            ?: throw IllegalStateException("Could not extract ticket code from URL: $sourceUrl")
        
        // Busca os dados atualizados da API
        val apiUrl = strategy.buildApiUrl(ticketCode, provider.apiUrlTemplate)
        val responseBody = httpGateway.get(apiUrl)
        val parsedData = strategy.parseResponse(responseBody)
        
        // Verifica se houve mudança no status
        if (parsedData.ticketStatus == ticket.ticketStatus) {
            logger.debug("Ticket {} status unchanged: {}", ticket.id, ticket.ticketStatus)
            return false
        }
        
        logger.info("Ticket {} status changed: {} -> {}", 
            ticket.id, ticket.ticketStatus, parsedData.ticketStatus)
        
        // Atualiza os campos do bilhete
        ticket.ticketStatus = parsedData.ticketStatus
        ticket.actualPayout = parsedData.actualPayout
        ticket.settledAt = parsedData.settledAt ?: System.currentTimeMillis()
        ticket.isCashedOut = parsedData.isCashedOut
        
        // Recalcula o status financeiro
        val statusResult = BetStatusCalculator.calculateFromTicket(
            stake = ticket.stake,
            actualPayout = ticket.actualPayout,
            potentialPayout = ticket.potentialPayout,
            ticketStatus = ticket.ticketStatus
        )
        
        ticket.financialStatus = statusResult.financialStatus
        ticket.profitLoss = statusResult.profitLoss
        ticket.roi = statusResult.roi
        
        // Atualiza as seleções se houver dados
        if (parsedData.selections.isNotEmpty()) {
            val existingSelections = ticket.selections.associateBy { it.externalSelectionId }
            
            for (selectionData in parsedData.selections) {
                val existingSelection = existingSelections[selectionData.externalSelectionId]
                if (existingSelection != null) {
                    existingSelection.status = selectionData.status
                    existingSelection.eventResult = selectionData.eventResult
                }
            }
        }
        
        ticketRepository.save(ticket)
        
        return true
    }
}
