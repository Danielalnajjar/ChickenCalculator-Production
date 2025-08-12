package com.example.chickencalculator.config

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.sql.DataSource

/**
 * Flyway configuration that handles Railway PostgreSQL URL format.
 * This ensures Flyway gets the properly formatted JDBC URL.
 */
@Configuration
class FlywayConfig(private val environment: Environment) {

    @Bean
    fun flywayMigrationStrategy(): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { flyway ->
            // Get the original URL and convert if needed
            val databaseUrl = environment.getProperty("DATABASE_URL")
            
            if (!databaseUrl.isNullOrBlank() && databaseUrl.startsWith("postgresql://")) {
                // Convert Railway PostgreSQL URL to JDBC format
                val jdbcUrl = "jdbc:$databaseUrl"
                
                // Create new Flyway instance with corrected URL
                val correctedFlyway = Flyway.configure()
                    .dataSource(jdbcUrl, extractUsername(databaseUrl), extractPassword(databaseUrl))
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .validateOnMigrate(true)
                    .cleanDisabled(true) // Safety for production
                    .load()
                
                correctedFlyway.migrate()
            } else {
                // Use default flyway for H2 or other databases
                flyway.migrate()
            }
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