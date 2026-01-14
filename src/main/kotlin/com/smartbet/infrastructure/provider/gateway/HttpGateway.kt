package com.smartbet.infrastructure.provider.gateway

import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gateway HTTP para chamadas às APIs das casas de apostas.
 */
@Component
class HttpGateway {
    
    private val logger = LoggerFactory.getLogger(HttpGateway::class.java)
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    
    /**
     * Faz uma requisição GET para a URL especificada.
     * 
     * @param url URL para fazer a requisição
     * @param headers Headers adicionais (opcional)
     * @return Corpo da resposta como String
     * @throws HttpGatewayException se a requisição falhar
     */
    fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        logger.debug("Making GET request to: {}", url)
        
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
        
        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }
        
        val request = requestBuilder.build()
        
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.error("HTTP request failed: {} - {}", response.code, response.message)
                    throw HttpGatewayException(
                        "HTTP request failed with status ${response.code}: ${response.message}",
                        response.code
                    )
                }
                
                response.body?.string() ?: throw HttpGatewayException(
                    "Empty response body",
                    response.code
                )
            }
        } catch (e: IOException) {
            logger.error("Network error during HTTP request", e)
            throw HttpGatewayException("Network error: ${e.message}", 0, e)
        }
    }
    
    /**
     * Faz uma requisição GET e retorna o resultado como Result.
     * 
     * @param url URL para fazer a requisição
     * @param headers Headers adicionais (opcional)
     * @return Result contendo o corpo da resposta ou erro
     */
    fun getAsResult(url: String, headers: Map<String, String> = emptyMap()): Result<String> {
        return try {
            Result.success(get(url, headers))
        } catch (e: HttpGatewayException) {
            Result.failure(e)
        }
    }
    
    companion object {
        private const val USER_AGENT = 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}

/**
 * Exceção para erros do gateway HTTP.
 */
class HttpGatewayException(
    message: String,
    val statusCode: Int,
    cause: Throwable? = null
) : RuntimeException(message, cause)
