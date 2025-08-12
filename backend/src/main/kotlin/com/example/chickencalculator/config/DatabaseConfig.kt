package com.example.chickencalculator.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment

/**
 * Database configuration class that handles Railway PostgreSQL URL conversion.
 * Railway provides PostgreSQL URLs in format: postgresql://user:pass@host:port/db
 * But Spring Boot/Flyway needs: jdbc:postgresql://user:pass@host:port/db
 */
@Configuration
@AutoConfigureBefore(DataSourceAutoConfiguration::class)
class DatabaseConfig(private val environment: Environment) {

    @Bean
    @Primary
    fun dataSource(): HikariDataSource {
        val databaseUrl = environment.getProperty("DATABASE_URL")
        
        val hikariConfig = HikariConfig()
        
        if (!databaseUrl.isNullOrBlank() && (databaseUrl.startsWith("postgresql://") || databaseUrl.startsWith("jdbc:postgresql://"))) {
            // Handle both Railway format (postgresql://) and JDBC format (jdbc:postgresql://)
            val jdbcUrl = if (databaseUrl.startsWith("jdbc:")) {
                databaseUrl
            } else {
                "jdbc:$databaseUrl"
            }
            
            // For JDBC URLs with embedded credentials, we need to extract them and remove from URL
            if (jdbcUrl.contains("@")) {
                // Extract credentials from URL
                val urlAfterProtocol = jdbcUrl.substringAfter("://")
                val authPart = urlAfterProtocol.substringBefore("@")
                val hostAndPath = urlAfterProtocol.substringAfter("@")
                
                if (authPart.contains(":")) {
                    val (username, password) = authPart.split(":", limit = 2)
                    hikariConfig.username = username
                    hikariConfig.password = password
                    // Reconstruct URL without credentials
                    hikariConfig.jdbcUrl = "jdbc:postgresql://$hostAndPath"
                } else {
                    hikariConfig.jdbcUrl = jdbcUrl
                }
            } else {
                hikariConfig.jdbcUrl = jdbcUrl
            }
            
            // Railway provides credentials as separate env vars - use them if available
            val pgUser = environment.getProperty("PGUSER")
            val pgPassword = environment.getProperty("PGPASSWORD")
            
            if (!pgUser.isNullOrBlank()) {
                hikariConfig.username = pgUser
            }
            if (!pgPassword.isNullOrBlank()) {
                hikariConfig.password = pgPassword
            }
            
            hikariConfig.driverClassName = "org.postgresql.Driver"
        } else {
            // Use H2 for local development
            hikariConfig.jdbcUrl = environment.getProperty("spring.datasource.url", 
                "jdbc:h2:file:./data/chicken-calculator-db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
            hikariConfig.username = environment.getProperty("spring.datasource.username", "sa")
            hikariConfig.password = environment.getProperty("spring.datasource.password", "")
            hikariConfig.driverClassName = "org.h2.Driver"
        }
        
        // Set HikariCP connection pool properties
        hikariConfig.maximumPoolSize = environment.getProperty("spring.datasource.hikari.maximum-pool-size", "15").toInt()
        hikariConfig.minimumIdle = environment.getProperty("spring.datasource.hikari.minimum-idle", "5").toInt()
        hikariConfig.connectionTimeout = environment.getProperty("spring.datasource.hikari.connection-timeout", "30000").toLong()
        hikariConfig.idleTimeout = environment.getProperty("spring.datasource.hikari.idle-timeout", "600000").toLong()
        hikariConfig.maxLifetime = environment.getProperty("spring.datasource.hikari.max-lifetime", "1800000").toLong()
        hikariConfig.poolName = environment.getProperty("spring.datasource.hikari.pool-name", "ChickenCalculatorPool-Prod")
        hikariConfig.leakDetectionThreshold = environment.getProperty("spring.datasource.hikari.leak-detection-threshold", "0").toLong()
        
        return HikariDataSource(hikariConfig)
    }
}