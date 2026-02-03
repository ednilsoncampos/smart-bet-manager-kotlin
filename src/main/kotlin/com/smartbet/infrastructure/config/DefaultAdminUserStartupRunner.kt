package com.smartbet.infrastructure.config

import com.smartbet.domain.entity.UserRole
import com.smartbet.infrastructure.persistence.entity.UserEntity
import com.smartbet.infrastructure.persistence.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

/**
 * Componente que cria um usuário ADMIN padrão no startup da aplicação.
 *
 * Verifica se o usuário já existe antes de criar para evitar duplicatas.
 * Este componente roda com prioridade alta (Order = 1) para garantir que
 * o usuário ADMIN esteja disponível antes de outras inicializações.
 */
@Component
@Order(1)
class DefaultAdminUserStartupRunner(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(DefaultAdminUserStartupRunner::class.java)

    companion object {
        private const val ADMIN_NAME = "Ednilson"
        private const val ADMIN_EMAIL = "ednilsoncampos@gmail.com"
        private const val ADMIN_PASSWORD = "7%Jc*4A-rIFB"
    }

    override fun run(args: ApplicationArguments?) {
        logger.info("===== Checking default admin user =====")

        try {
            createDefaultAdminIfNotExists()
        } catch (e: Exception) {
            logger.error("Error creating default admin user: ${e.message}", e)
        }

        logger.info("===== Default admin user check completed =====")
    }

    /**
     * Cria o usuário ADMIN padrão se ele ainda não existir.
     */
    private fun createDefaultAdminIfNotExists() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            logger.info("Admin user already exists: $ADMIN_EMAIL")
            return
        }

        logger.info("Creating default admin user: $ADMIN_EMAIL")

        val now = System.currentTimeMillis()
        val adminDateOfBirth = LocalDate.of(1984, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val adminUser = UserEntity(
            name = ADMIN_NAME,
            email = ADMIN_EMAIL,
            passwordHash = passwordEncoder.encode(ADMIN_PASSWORD),
            dateOfBirth = adminDateOfBirth,
            role = UserRole.ADMIN,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )

        userRepository.save(adminUser)

        logger.info("✓ Default admin user created successfully")
        logger.info("  - Name: $ADMIN_NAME")
        logger.info("  - Email: $ADMIN_EMAIL")
        logger.info("  - Role: ADMIN")
    }
}
