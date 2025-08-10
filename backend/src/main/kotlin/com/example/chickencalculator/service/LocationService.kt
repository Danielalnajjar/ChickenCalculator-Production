package com.example.chickencalculator.service

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import com.example.chickencalculator.repository.LocationRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class LocationService(
    private val locationRepository: LocationRepository,
    private val deploymentService: DeploymentService
) {
    
    private val logger = LoggerFactory.getLogger(LocationService::class.java)
    
    fun getAllLocations(): List<Location> {
        return locationRepository.findAll()
    }
    
    fun getLocationById(id: Long): Location? {
        return locationRepository.findById(id).orElse(null)
    }
    
    fun createLocation(
        name: String,
        domain: String,
        address: String?,
        managerName: String,
        managerEmail: String,
        cloudProvider: String,
        region: String
    ): Location {
        val location = Location(
            name = name,
            domain = domain,
            address = address,
            managerName = managerName,
            managerEmail = managerEmail,
            cloudProvider = cloudProvider,
            region = region,
            status = LocationStatus.DEPLOYING
        )
        return locationRepository.save(location)
    }
    
    @Async
    fun deployLocation(location: Location) {
        logger.info("Starting deployment for location: ${location.name}")
        try {
            // Update status to deploying
            val deployingLocation = location.copy(status = LocationStatus.DEPLOYING)
            locationRepository.save(deployingLocation)
            
            // Start deployment process
            val deploymentResult = deploymentService.deployToCloud(location)
            
            if (deploymentResult.success) {
                // Update location with deployment details
                val deployedLocation = location.copy(
                    status = LocationStatus.ACTIVE,
                    serverIp = deploymentResult.serverIp,
                    databaseUrl = deploymentResult.databaseUrl,
                    deployedAt = LocalDateTime.now(),
                    lastSeenAt = LocalDateTime.now(),
                    deploymentLogs = deploymentResult.logs
                )
                locationRepository.save(deployedLocation)
                
                logger.info("Successfully deployed location: ${location.name}")
            } else {
                // Update status to error
                val errorLocation = location.copy(
                    status = LocationStatus.ERROR,
                    deploymentLogs = deploymentResult.errorMessage
                )
                locationRepository.save(errorLocation)
                
                logger.error("Failed to deploy location: ${location.name}, Error: ${deploymentResult.errorMessage}")
            }
        } catch (e: Exception) {
            logger.error("Deployment failed for location: ${location.name}", e)
            val errorLocation = location.copy(
                status = LocationStatus.ERROR,
                deploymentLogs = "Deployment failed: ${e.message}"
            )
            locationRepository.save(errorLocation)
        }
    }
    
    fun deleteLocation(id: Long) {
        val location = getLocationById(id) ?: throw RuntimeException("Location not found")
        
        // Mark as deleted and clean up resources
        val deletedLocation = location.copy(
            status = LocationStatus.DELETED,
            lastSeenAt = LocalDateTime.now()
        )
        locationRepository.save(deletedLocation)
        
        // Trigger cleanup of cloud resources
        deploymentService.cleanupLocation(location)
        
        logger.info("Deleted location: ${location.name}")
    }
    
    fun updateLocationHealth(domain: String) {
        val location = locationRepository.findByDomain(domain)
        if (location != null) {
            val updatedLocation = location.copy(lastSeenAt = LocalDateTime.now())
            locationRepository.save(updatedLocation)
        }
    }
}