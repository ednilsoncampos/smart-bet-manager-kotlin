package com.smartbet.infrastructure.persistence.repository

import com.smartbet.domain.enum.FinancialStatus
import com.smartbet.domain.enum.TicketStatus
import com.smartbet.infrastructure.persistence.entity.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByEmail(email: String): UserEntity?
    fun findByExternalId(externalId: String): UserEntity?
    fun existsByEmail(email: String): Boolean
}

@Repository
interface BettingProviderRepository : JpaRepository<BettingProviderEntity, Long> {
    fun findBySlug(slug: String): BettingProviderEntity?
    fun findByIsActiveTrue(): List<BettingProviderEntity>
    fun existsBySlug(slug: String): Boolean
}

@Repository
interface BetTicketRepository : JpaRepository<BetTicketEntity, Long> {
    fun findByUserId(userId: Long, pageable: Pageable): Page<BetTicketEntity>
    
    fun findByUserIdAndTicketStatus(
        userId: Long, 
        status: TicketStatus, 
        pageable: Pageable
    ): Page<BetTicketEntity>
    
    fun findByUserIdAndFinancialStatus(
        userId: Long, 
        status: FinancialStatus, 
        pageable: Pageable
    ): Page<BetTicketEntity>
    
    fun findByUserIdAndProviderId(
        userId: Long, 
        providerId: Long, 
        pageable: Pageable
    ): Page<BetTicketEntity>
    
    fun findByExternalTicketIdAndProviderId(
        externalTicketId: String, 
        providerId: Long
    ): BetTicketEntity?
    
    fun countByUserIdAndTicketStatus(userId: Long, status: TicketStatus): Long

    /**
     * Busca um ticket por ID carregando as selections (evita LazyInitializationException).
     */
    @Query("""
        SELECT DISTINCT t FROM BetTicketEntity t
        LEFT JOIN FETCH t.selections
        WHERE t.id = :id
    """)
    fun findByIdWithSelections(@Param("id") id: Long): BetTicketEntity?

    /**
     * Busca todos os tickets de um usuário carregando as selections (evita LazyInitializationException).
     * Usado em analytics que precisam acessar as selections.
     */
    @Query("""
        SELECT DISTINCT t FROM BetTicketEntity t
        LEFT JOIN FETCH t.selections
        WHERE t.userId = :userId
    """)
    fun findByUserIdWithSelections(@Param("userId") userId: Long): List<BetTicketEntity>

    @Query("""
        SELECT t FROM BetTicketEntity t
        WHERE t.userId = :userId
        AND (:status IS NULL OR t.ticketStatus = :status)
        AND (:financialStatus IS NULL OR t.financialStatus = :financialStatus)
        AND (:providerId IS NULL OR t.providerId = :providerId)
        ORDER BY t.createdAt DESC
    """,
    countQuery = """
        SELECT COUNT(t) FROM BetTicketEntity t
        WHERE t.userId = :userId
        AND (:status IS NULL OR t.ticketStatus = :status)
        AND (:financialStatus IS NULL OR t.financialStatus = :financialStatus)
        AND (:providerId IS NULL OR t.providerId = :providerId)
    """)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = ["selections"])
    fun findByFilters(
        @Param("userId") userId: Long,
        @Param("status") status: TicketStatus?,
        @Param("financialStatus") financialStatus: FinancialStatus?,
        @Param("providerId") providerId: Long?,
        pageable: Pageable
    ): Page<BetTicketEntity>
    
    @Query("""
        SELECT SUM(t.stake) FROM BetTicketEntity t 
        WHERE t.userId = :userId AND t.ticketStatus != 'OPEN'
    """)
    fun sumStakeByUserId(@Param("userId") userId: Long): java.math.BigDecimal?
    
    @Query("""
        SELECT SUM(t.profitLoss) FROM BetTicketEntity t 
        WHERE t.userId = :userId AND t.ticketStatus != 'OPEN'
    """)
    fun sumProfitLossByUserId(@Param("userId") userId: Long): java.math.BigDecimal?
    
    @Query("""
        SELECT COUNT(t) FROM BetTicketEntity t 
        WHERE t.userId = :userId 
        AND t.financialStatus IN ('FULL_WIN', 'PARTIAL_WIN')
    """)
    fun countWinsByUserId(@Param("userId") userId: Long): Long
    
    @Query("""
        SELECT COUNT(t) FROM BetTicketEntity t 
        WHERE t.userId = :userId 
        AND t.ticketStatus != 'OPEN'
    """)
    fun countSettledByUserId(@Param("userId") userId: Long): Long
    
    /**
     * Busca bilhetes em aberto de um usuário que foram importados (têm sourceUrl).
     * Usado para refresh de bilhetes no login.
     */
    @Query("""
        SELECT DISTINCT t FROM BetTicketEntity t
        LEFT JOIN FETCH t.selections
        WHERE t.userId = :userId
        AND t.ticketStatus = 'OPEN'
        AND t.sourceUrl IS NOT NULL
    """)
    fun findOpenTicketsByUserId(@Param("userId") userId: Long): List<BetTicketEntity>

    /**
     * Busca todos os bilhetes em aberto que foram importados (têm sourceUrl).
     * Usado pelo job agendado.
     */
    @Query("""
        SELECT DISTINCT t FROM BetTicketEntity t
        LEFT JOIN FETCH t.selections
        WHERE t.ticketStatus = 'OPEN'
        AND t.sourceUrl IS NOT NULL
        ORDER BY t.userId, t.providerId
    """)
    fun findAllOpenTicketsWithSourceUrl(): List<BetTicketEntity>
    
    /**
     * Verifica se já existe um bilhete com o mesmo external_ticket_id para o usuário.
     * Usado para evitar importação duplicada.
     */
    fun findByUserIdAndExternalTicketId(userId: Long, externalTicketId: String): BetTicketEntity?
}

@Repository
interface BetSelectionRepository : JpaRepository<BetSelectionEntity, Long> {
    fun findByTicketId(ticketId: Long): List<BetSelectionEntity>
    
    @Query("""
        SELECT s.tournament.name, COUNT(s),
               SUM(CASE WHEN s.status = 'WON' THEN 1 ELSE 0 END)
        FROM BetSelectionEntity s
        WHERE s.ticket.userId = :userId
        AND s.tournament IS NOT NULL
        GROUP BY s.tournament.name
        ORDER BY COUNT(s) DESC
    """)
    fun getStatsByTournament(@Param("userId") userId: Long): List<Array<Any>>
    
    @Query("""
        SELECT s.marketType, COUNT(s), 
               SUM(CASE WHEN s.status = 'WON' THEN 1 ELSE 0 END)
        FROM BetSelectionEntity s
        WHERE s.ticket.userId = :userId
        AND s.marketType IS NOT NULL
        GROUP BY s.marketType
        ORDER BY COUNT(s) DESC
    """)
    fun getStatsByMarket(@Param("userId") userId: Long): List<Array<Any>>
}

@Repository
interface BetSelectionComponentRepository : JpaRepository<BetSelectionComponentEntity, Long> {
    fun findBySelectionId(selectionId: Long): List<BetSelectionComponentEntity>

    fun deleteBySelectionId(selectionId: Long)

    /**
     * Busca todos os componentes de Bet Builder de um usuário.
     * Usado para analytics de performance por mercado.
     * JOIN FETCH garante que o eventName da selection esteja disponível.
     */
    @Query("""
        SELECT DISTINCT c FROM BetSelectionComponentEntity c
        JOIN FETCH c.selection s
        WHERE s.ticket.userId = :userId
        AND s.ticket.ticketStatus != 'OPEN'
    """)
    fun findByUserId(@Param("userId") userId: Long): List<BetSelectionComponentEntity>
}

@Repository
interface BankrollRepository : JpaRepository<BankrollEntity, Long> {
    fun findByUserId(userId: Long): List<BankrollEntity>
    fun findByUserIdAndIsActiveTrue(userId: Long): List<BankrollEntity>
    fun findByUserIdAndProviderId(userId: Long, providerId: Long): BankrollEntity?
}

@Repository
interface BankrollTransactionRepository : JpaRepository<BankrollTransactionEntity, Long> {
    fun findByBankrollIdOrderByCreatedAtDesc(
        bankrollId: Long,
        pageable: Pageable
    ): Page<BankrollTransactionEntity>

    fun findByBankrollId(bankrollId: Long): List<BankrollTransactionEntity>
}

@Repository
interface SportRepository : JpaRepository<SportEntity, Long> {
    fun findByProviderIdAndExternalId(providerId: Long, externalId: Int): SportEntity?
    fun findByProviderId(providerId: Long): List<SportEntity>
}

@Repository
interface TournamentRepository : JpaRepository<TournamentEntity, Long> {
    fun findByProviderIdAndExternalId(providerId: Long, externalId: Int): TournamentEntity?
    fun findByProviderId(providerId: Long): List<TournamentEntity>
    fun findBySportId(sportId: Long): List<TournamentEntity>
    fun countByProviderId(providerId: Long): Long
}
