package com.smartbet.application.usecase

import com.smartbet.application.dto.*
import com.smartbet.domain.entity.BetSelection
import com.smartbet.domain.entity.BetTicket
import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.domain.service.BetStatusCalculator
import com.smartbet.infrastructure.persistence.entity.BetSelectionEntity
import com.smartbet.infrastructure.persistence.entity.BetTicketEntity
import com.smartbet.infrastructure.persistence.repository.BetSelectionRepository
import com.smartbet.infrastructure.persistence.repository.BetTicketRepository
import com.smartbet.infrastructure.persistence.repository.BettingProviderRepository
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
    private val providerRepository: BettingProviderRepository,
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
        
        // Verifica se o bilhete já existe
        val existingTicket = ticketRepository.findByExternalTicketIdAndProviderId(ticketCode, provider.id!!)
        if (existingTicket != null) {
            logger.info("Ticket already exists, returning existing: {}", existingTicket.id)
            return TicketResponse.fromDomain(existingTicket.toDomain(), provider.name)
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
            providerId = provider.id,
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
            settledAt = parsedData.settledAt
        )
        
        // Salva o bilhete
        val savedTicket = ticketRepository.save(ticketEntity)
        
        // Cria as seleções
        val selectionEntities = parsedData.selections.map { selectionData ->
            BetSelectionEntity(
                ticket = savedTicket,
                externalSelectionId = selectionData.externalSelectionId,
                eventName = selectionData.eventName,
                tournamentName = selectionData.tournamentName,
                marketType = selectionData.marketType,
                selection = selectionData.selection,
                odd = selectionData.odd,
                status = selectionData.status,
                eventDate = selectionData.eventDate,
                eventResult = selectionData.eventResult
            )
        }
        
        selectionRepository.saveAll(selectionEntities)
        savedTicket.selections.addAll(selectionEntities)
        
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
            BetSelectionEntity(
                ticket = savedTicket,
                eventName = selectionRequest.eventName,
                tournamentName = selectionRequest.tournamentName,
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
            request.pageSize, 
            Sort.by(Sort.Direction.DESC, "createdAt")
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
        val ticket = ticketRepository.findById(ticketId)
            .orElseThrow { IllegalArgumentException("Bilhete não encontrado: $ticketId") }
        
        if (ticket.userId != userId) {
            throw IllegalAccessException("Acesso negado ao bilhete: $ticketId")
        }
        
        val provider = providerRepository.findById(ticket.providerId).orElse(null)
        
        return TicketResponse.fromDomain(ticket.toDomain(), provider?.name)
    }
    
    /**
     * Atualiza o status de um bilhete.
     */
    @Transactional
    fun updateStatus(userId: Long, request: UpdateTicketStatusRequest): TicketResponse {
        logger.info("Updating ticket status: {}", request.ticketId)
        
        val ticket = ticketRepository.findById(request.ticketId)
            .orElseThrow { IllegalArgumentException("Bilhete não encontrado: ${request.ticketId}") }
        
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
}
