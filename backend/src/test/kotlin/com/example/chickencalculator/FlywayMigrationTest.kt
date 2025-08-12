package com.example.chickencalculator

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import javax.sql.DataSource

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private lateinit var dataSource: DataSource

    @Test
    fun `flyway migrations should execute successfully`() {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()

        // Clean database and run migrations
        flyway.clean()
        val migrationResult = flyway.migrate()
        
        // Verify migrations were applied
        assert(migrationResult.migrationsExecuted > 0) { 
            "Expected migrations to be executed, but none were found" 
        }
        
        // Verify schema history
        val info = flyway.info()
        val appliedMigrations = info.applied()
        
        assert(appliedMigrations.isNotEmpty()) { 
            "Expected applied migrations, but none were found" 
        }
        
        println("✅ Successfully applied ${appliedMigrations.size} migrations")
        appliedMigrations.forEach { migration ->
            println("   - ${migration.version}: ${migration.description}")
        }
    }
}