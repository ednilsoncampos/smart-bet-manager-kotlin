package com.smartbet.infrastructure.provider.strategy

import org.springframework.stereotype.Component
import java.net.URI

/**
 * Factory para seleção da strategy apropriada baseada na URL.
 * 
 * Implementa o padrão Factory para abstrair a seleção de parsers
 * de casas de apostas.
 */
@Component
class BettingProviderFactory(
    strategies: List<BettingProviderStrategy>
) {
    private val strategyMap: Map<String, BettingProviderStrategy> = 
        strategies.associateBy { it.slug }
    
    /**
     * Encontra a strategy apropriada para processar a URL.
     * 
     * @param url URL do bilhete compartilhado
     * @return Strategy que pode processar a URL, ou null se não suportada
     */
    fun findStrategyForUrl(url: String): BettingProviderStrategy? {
        return strategyMap.values.find { it.canHandle(url) }
    }
    
    /**
     * Obtém uma strategy pelo slug.
     * 
     * @param slug Slug da casa de apostas
     * @return Strategy correspondente, ou null se não encontrada
     */
    fun getStrategy(slug: String): BettingProviderStrategy? {
        return strategyMap[slug]
    }
    
    /**
     * Verifica se a URL é de uma casa suportada.
     * 
     * @param url URL para verificar
     * @return true se a URL é suportada
     */
    fun isSupported(url: String): Boolean {
        return findStrategyForUrl(url) != null
    }
    
    /**
     * Extrai o nome do provedor da URL (para tracking de não suportados).
     * 
     * @param url URL para extrair o nome
     * @return Nome do provedor ou null se não identificado
     */
    fun extractProviderNameFromUrl(url: String): String? {
        return try {
            val uri = URI(url)
            val hostname = uri.host?.lowercase() ?: return null
            
            // Padrões conhecidos de casas de apostas
            val knownProviders = mapOf(
                "superbet" to "Superbet",
                "betano" to "Betano",
                "bet365" to "Bet365",
                "betfair" to "Betfair",
                "sportingbet" to "Sportingbet",
                "pixbet" to "Pixbet",
                "novibet" to "Novibet",
                "betsson" to "Betsson",
                "bwin" to "Bwin",
                "1xbet" to "1xBet",
                "pinnacle" to "Pinnacle",
                "stake" to "Stake",
                "blaze" to "Blaze",
                "estrelabet" to "EstrelaBet",
                "kto" to "KTO",
                "rivalo" to "Rivalo"
            )
            
            for ((pattern, name) in knownProviders) {
                if (hostname.contains(pattern)) {
                    return name
                }
            }
            
            // Retorna o domínio como fallback
            hostname.removePrefix("www.").split(".").firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Retorna lista de slugs de provedores registrados.
     */
    fun getRegisteredProviders(): List<String> {
        return strategyMap.keys.toList()
    }
    
    /**
     * Retorna todas as strategies registradas.
     */
    fun getAllStrategies(): List<BettingProviderStrategy> {
        return strategyMap.values.toList()
    }
}
