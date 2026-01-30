package com.smartbet.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.lang.reflect.Method
import java.util.concurrent.Executor

/**
 * Configuração para execução de tarefas assíncronas e agendadas.
 * 
 * - @EnableAsync: Habilita métodos @Async
 * - @EnableScheduling: Habilita métodos @Scheduled
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableRetry
class AsyncConfig : AsyncConfigurer {
    
    private val logger = LoggerFactory.getLogger(AsyncConfig::class.java)
    
    /**
     * Executor padrão para tarefas assíncronas.
     * Configurado com pool de threads para processar refresh de bilhetes.
     */
    @Bean(name = ["taskExecutor"])
    override fun getAsyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("async-ticket-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()
        
        logger.info("AsyncConfig: ThreadPoolTaskExecutor initialized with corePoolSize={}, maxPoolSize={}", 
            executor.corePoolSize, executor.maxPoolSize)
        
        return executor
    }

    /**
     * Executor dedicado para processamento de analytics.
     * Configurado com pool de threads separado para não impactar outras operações assíncronas.
     */
    @Bean(name = ["analyticsTaskExecutor"])
    fun analyticsTaskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("analytics-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()

        logger.info("AsyncConfig: Analytics ThreadPoolTaskExecutor initialized with corePoolSize={}, maxPoolSize={}",
            executor.corePoolSize, executor.maxPoolSize)

        return executor
    }

    /**
     * Handler para exceções não capturadas em métodos @Async.
     */
    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return AsyncUncaughtExceptionHandler { ex: Throwable, method: Method, params: Array<Any> ->
            logger.error("Async exception in method '{}' with params {}: {}", 
                method.name, params.contentToString(), ex.message, ex)
        }
    }
}
