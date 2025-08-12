package com.example.chickencalculator.config

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import javax.sql.DataSource

/**
 * Flyway configuration that uses the configured datasource and runs before Hibernate.
 * This ensures database migrations execute before entity validation.
 */
@Configuration
@ConditionalOnClass(Flyway::class)
@ConditionalOnProperty(prefix = "spring.flyway", name = ["enabled"], matchIfMissing = true)
@AutoConfigureBefore(HibernateJpaAutoConfiguration::class)
class FlywayConfig {

    @Bean(initMethod = "migrate")
    @DependsOn("dataSource")
    fun flyway(dataSource: DataSource): Flyway {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .validateOnMigrate(false) // Don't validate in production - we trust our migrations
            .cleanDisabled(true) // Safety for production
            .load()
    }
}