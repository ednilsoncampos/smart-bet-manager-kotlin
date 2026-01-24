package com.smartbet.presentation.exception

import com.smartbet.domain.exception.DuplicateTicketException
import com.smartbet.infrastructure.provider.gateway.HttpGatewayException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        logger.warn("Validation error: {}", ex.message)
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ValidationErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Validation Error",
                message = "Dados inválidos na requisição",
                errors = errors,
                timestamp = Instant.now()
            ))
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Bad request: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                error = "Bad Request",
                message = ex.message ?: "Invalid request",
                timestamp = Instant.now()
            ))
    }
    
    @ExceptionHandler(IllegalAccessException::class)
    fun handleIllegalAccess(ex: IllegalAccessException): ResponseEntity<ErrorResponse> {
        logger.warn("Access denied: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse(
                status = HttpStatus.FORBIDDEN.value(),
                error = "Forbidden",
                message = ex.message ?: "Access denied",
                timestamp = Instant.now()
            ))
    }
    
    @ExceptionHandler(InvalidTicketDataException::class)
    fun handleInvalidTicketData(ex: InvalidTicketDataException): ResponseEntity<ValidationErrorResponse> {
        logger.warn("Invalid ticket data: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ValidationErrorResponse(
                status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
                error = "Unprocessable Entity",
                message = ex.message ?: "Dados do bilhete inválidos",
                errors = ex.details,
                timestamp = Instant.now()
            ))
    }
    
    @ExceptionHandler(DuplicateTicketException::class)
    fun handleDuplicateTicket(ex: DuplicateTicketException): ResponseEntity<ErrorResponse> {
        logger.warn("Duplicate ticket: {}", ex.ticketId)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(
                status = HttpStatus.CONFLICT.value(),
                error = "Conflict",
                message = ex.message ?: "Bilhete já importado",
                timestamp = Instant.now()
            ))
    }
    
    @ExceptionHandler(HttpGatewayException::class)
    fun handleHttpGateway(ex: HttpGatewayException): ResponseEntity<ErrorResponse> {
        logger.error("HTTP Gateway error: {} - Status: {}", ex.message, ex.statusCode)
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse(
                status = HttpStatus.BAD_GATEWAY.value(),
                error = "Bad Gateway",
                message = "Erro ao comunicar com a casa de apostas: ${ex.message}",
                timestamp = Instant.now()
            ))
    }
    
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
        logger.warn("Resource not found: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                status = HttpStatus.NOT_FOUND.value(),
                error = "Not Found",
                message = ex.message ?: "Resource not found",
                timestamp = Instant.now()
            ))
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                error = "Internal Server Error",
                message = "Ocorreu um erro inesperado. Por favor, tente novamente.",
                timestamp = Instant.now()
            ))
    }
}

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: Instant
)

data class ValidationErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val errors: Map<String, String>,
    val timestamp: Instant
)
