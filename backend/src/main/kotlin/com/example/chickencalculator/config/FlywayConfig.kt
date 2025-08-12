package com.example.chickencalculator.config

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment

/**
 * Flyway configuration that handles Railway PostgreSQL URL format.
 * This replaces Spring Boot's auto-configuration with a custom implementation
 * that properly converts Railway's PostgreSQL URL format to JDBC format.
 */
@Configuration
@ConditionalOnClass(Flyway::class)
@ConditionalOnProperty(prefix = "spring.flyway", name = ["enabled"], matchIfMissing = true)
@AutoConfigureAfter(DataSourceAutoConfiguration::class)
class FlywayConfig(private val environment: Environment) {

    @Bean
    @Primary
    fun flyway(): Flyway {
        val databaseUrl = environment.getProperty("DATABASE_URL")
        
        return if (!databaseUrl.isNullOrBlank() && databaseUrl.startsWith("postgresql://")) {
            // Convert Railway PostgreSQL URL to JDBC format
            val jdbcUrl = "jdbc:$databaseUrl"
            
            Flyway.configure()
                .dataSource(jdbcUrl, extractUsername(databaseUrl), extractPassword(databaseUrl))
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .cleanDisabled(true) // Safety for production
                .load()
        } else {
            // Use H2 or other databases with default settings
            val url = environment.getProperty("spring.datasource.url", 
                "jdbc:h2:file:/app/data/chicken-calculator-db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
            val username = environment.getProperty("spring.datasource.username", "sa")
            val password = environment.getProperty("spring.datasource.password", "")
            
            Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .load()
        }
    }

    private fun extractUsername(databaseUrl: String): String {
        return if (databaseUrl.contains("@")) {
            val authPart = databaseUrl.substringAfter("://").substringBefore("@")
            if (authPart.contains(":")) {
                authPart.split(":", limit = 2)[0]
            } else {
                "sa" // fallback
            }
        } else {
            "sa" // fallback
        }
    }

    private fun extractPassword(databaseUrl: String): String {
        return if (databaseUrl.contains("@")) {
            val authPart = databaseUrl.substringAfter("://").substringBefore("@")
            if (authPart.contains(":")) {
                authPart.split(":", limit = 2)[1]
            } else {
                "" // fallback
            }
        } else {
            "" // fallback
        }
    }
}