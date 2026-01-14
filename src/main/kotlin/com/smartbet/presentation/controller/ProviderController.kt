package com.smartbet.presentation.controller

import com.smartbet.application.usecase.CheckUrlResponse
import com.smartbet.application.usecase.ProviderResponse
import com.smartbet.application.usecase.ProviderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/providers")
@Tag(name = "Providers", description = "Casas de apostas suportadas")
class ProviderController(
    private val providerService: ProviderService
) {
    
    @GetMapping
    @Operation(summary = "Listar provedores", description = "Lista todas as casas de apostas cadastradas")
    fun list(): ResponseEntity<List<ProviderResponse>> {
        val providers = providerService.list()
        return ResponseEntity.ok(providers)
    }
    
    @GetMapping("/active")
    @Operation(summary = "Listar provedores ativos", description = "Lista apenas as casas de apostas ativas")
    fun listActive(): ResponseEntity<List<ProviderResponse>> {
        val providers = providerService.listActive()
        return ResponseEntity.ok(providers)
    }
    
    @GetMapping("/supported")
    @Operation(summary = "Listar provedores suportados", description = "Lista os slugs dos provedores com parser implementado")
    fun getSupportedProviders(): ResponseEntity<List<String>> {
        val supported = providerService.getSupportedProviders()
        return ResponseEntity.ok(supported)
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Buscar provedor por ID", description = "Retorna os detalhes de um provedor específico")
    fun getById(@PathVariable id: Long): ResponseEntity<ProviderResponse> {
        val provider = providerService.getById(id)
        return ResponseEntity.ok(provider)
    }
    
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Buscar provedor por slug", description = "Retorna os detalhes de um provedor pelo slug")
    fun getBySlug(@PathVariable slug: String): ResponseEntity<ProviderResponse> {
        val provider = providerService.getBySlug(slug)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(provider)
    }
    
    @PostMapping("/check-url")
    @Operation(summary = "Verificar URL", description = "Verifica se uma URL é suportada e extrai informações")
    fun checkUrl(@RequestBody request: CheckUrlRequest): ResponseEntity<CheckUrlResponse> {
        val result = providerService.checkUrl(request.url)
        return ResponseEntity.ok(result)
    }
}

data class CheckUrlRequest(val url: String)
