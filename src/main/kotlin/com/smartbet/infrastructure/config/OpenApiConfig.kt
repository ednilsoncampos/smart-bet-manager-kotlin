package com.smartbet.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Smart Bet Manager API")
                    .version("v1")
                    .description("""
                        API para gerenciamento de apostas esportivas.

                        ## Recursos Principais
                        - Importação de bilhetes de apostas de múltiplas casas
                        - Gerenciamento de bancas (bankrolls)
                        - Analytics avançados com métricas granulares
                        - Autenticação JWT
                        - Rastreamento de performance detalhado
                    """.trimIndent())
                    .contact(
                        Contact()
                            .name("Smart Bet Manager")
                            .email("contact@smartbet.com")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("Servidor de desenvolvimento")
                )
            )
    }
}
