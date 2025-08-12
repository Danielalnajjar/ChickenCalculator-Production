package com.example.chickencalculator.config

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import javax.sql.DataSource

/**
 * Database configuration class that handles Railway PostgreSQL URL conversion.
 * Railway provides PostgreSQL URLs in format: postgresql://user:pass@host:port/db
 * But Spring Boot/Flyway needs: jdbc:postgresql://user:pass@host:port/db
 */
@Configuration
class DatabaseConfig(private val environment: Environment) {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    fun dataSourceProperties(): DataSourceProperties {
        val properties = DataSourceProperties()
        
        // Get the DATABASE_URL from environment
        val databaseUrl = environment.getProperty("DATABASE_URL")
        
        if (!databaseUrl.isNullOrBlank() && databaseUrl.startsWith("postgresql://")) {
            // Convert Railway PostgreSQL URL to JDBC format
            val jdbcUrl = "jdbc:$databaseUrl"
            properties.url = jdbcUrl
            
            // Extract username and password from URL if not provided separately
            if (databaseUrl.contains("@")) {
                val authPart = databaseUrl.substringAfter("://").substringBefore("@")
                if (authPart.contains(":")) {
                    val (username, password) = authPart.split(":", limit = 2)
                    properties.username = username
                    properties.password = password
                }
            }
            
            // Set PostgreSQL driver
            properties.driverClassName = "org.postgresql.Driver"
        } else {
            // Use default H2 configuration for local development
            properties.url = environment.getProperty("spring.datasource.url", 
                "jdbc:h2:file:./data/chicken-calculator-db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
            properties.username = environment.getProperty("spring.datasource.username", "sa")
            properties.password = environment.getProperty("spring.datasource.password", "")
            properties.driverClassName = environment.getProperty("spring.datasource.driver-class-name", "org.h2.Driver")
        }
        
        return properties
    }

    @Bean
    @Primary
    fun dataSource(): DataSource {
        return dataSourceProperties().initializeDataSourceBuilder().build()
    }
}