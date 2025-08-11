package com.example.chickencalculator.config

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import com.example.chickencalculator.repository.LocationRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DefaultDataInitializer(
    private val locationRepository: LocationRepository
) : ApplicationRunner {
    
    private val logger = LoggerFactory.getLogger(DefaultDataInitializer::class.java)
    
    @Transactional
    override fun run(args: ApplicationArguments?) {
        initializeDefaultLocation()
    }
    
    private fun initializeDefaultLocation() {
        // Check if default location already exists
        val existingDefault = locationRepository.findByIsDefaultTrue()
        if (existingDefault != null) {
            logger.info("Default location already exists: ${existingDefault.name}")
            return
        }
        
        // Create default location for the main calculator
        val defaultLocation = Location(
            name = "Main Calculator",
            slug = "main",
            address = "Online",
            managerName = "System",
            managerEmail = "admin@yourcompany.com",
            status = LocationStatus.ACTIVE,
            isDefault = true
        )
        
        try {
            val saved = locationRepository.save(defaultLocation)
            logger.info("✅ Default location created: ${saved.name} (ID: ${saved.id})")
        } catch (e: Exception) {
            logger.error("Failed to create default location: ${e.message}")
        }
    }
}