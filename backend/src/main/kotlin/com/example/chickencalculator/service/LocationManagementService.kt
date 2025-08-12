package com.example.chickencalculator.service

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import com.example.chickencalculator.repository.LocationRepository
import com.example.chickencalculator.repository.SalesDataRepository
import com.example.chickencalculator.repository.MarinationLogRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class LocationManagementService(
    private val locationRepository: LocationRepository,
    private val salesDataRepository: SalesDataRepository,
    private val marinationLogRepository: MarinationLogRepository
) {
    
    private val logger = LoggerFactory.getLogger(LocationManagementService::class.java)
    
    /**
     * Get all locations with additional metadata
     */
    fun getAllLocations(): List<Location> {
        return locationRepository.findAll().sortedBy { it.name }
    }
    
    /**
     * Get locations by status
     */
    fun getLocationsByStatus(status: LocationStatus): List<Location> {
        return locationRepository.findByStatusOrderByNameAsc(status)
    }
    
    /**
     * Get active locations only
     */
    fun getActiveLocations(): List<Location> {
        return getLocationsByStatus(LocationStatus.ACTIVE)
    }
    
    /**
     * Get location by ID with validation
     */
    fun getLocationById(id: Long): Location? {
        return locationRepository.findById(id).orElse(null)
    }
    
    /**
     * Get location by ID or throw exception
     */
    fun getLocationByIdOrThrow(id: Long): Location {
        return locationRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Location with ID $id not found")
        }
    }
    
    /**
     * Get location by slug with validation
     */
    fun getLocationBySlug(slug: String): Location? {
        return locationRepository.findBySlug(slug)
    }
    
    /**
     * Get location by slug or throw exception
     */
    fun getLocationBySlugOrThrow(slug: String): Location {
        return locationRepository.findBySlug(slug) ?: throw ResponseStatusException(
            HttpStatus.NOT_FOUND, "Location with slug '$slug' not found"
        )
    }
    
    /**
     * Get default location
     */
    fun getDefaultLocation(): Location? {
        return locationRepository.findByIsDefaultTrue()
    }
    
    /**
     * Get default location or throw exception
     */
    fun getDefaultLocationOrThrow(): Location {
        return locationRepository.findByIsDefaultTrue() ?: throw ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "No default location configured"
        )
    }
    
    /**
     * Create a new location with full validation
     */
    @Transactional
    fun createLocation(
        name: String,
        address: String?,
        managerName: String,
        managerEmail: String
    ): Location {
        // Validate input parameters
        validateLocationData(name, managerName, managerEmail)
        
        // Check if location with similar name already exists
        val existingLocation = locationRepository.findByNameIgnoreCase(name)
        if (existingLocation != null) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT, 
                "Location with name '$name' already exists"
            )
        }
        
        // Generate and validate slug uniqueness
        val slug = generateUniqueSlug(name)
        
        val location = Location(
            name = name.trim(),
            slug = slug,
            address = address?.trim(),
            managerName = managerName.trim(),
            managerEmail = managerEmail.trim().lowercase(),
            status = LocationStatus.ACTIVE,
            isDefault = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        logger.info("Creating new location: $name with slug: $slug")
        return locationRepository.save(location)
    }
    
    /**
     * Update an existing location
     */
    @Transactional
    fun updateLocation(
        id: Long,
        name: String? = null,
        address: String? = null,
        managerName: String? = null,
        managerEmail: String? = null,
        status: LocationStatus? = null
    ): Location {
        val location = getLocationByIdOrThrow(id)
        
        // Validate updates
        name?.let { validateLocationName(it) }
        managerName?.let { validateManagerName(it) }
        managerEmail?.let { validateManagerEmail(it) }
        
        // Check for name conflicts if name is being updated
        if (name != null && name != location.name) {
            val existingLocation = locationRepository.findByNameIgnoreCase(name)
            if (existingLocation != null && existingLocation.id != id) {
                throw ResponseStatusException(
                    HttpStatus.CONFLICT, 
                    "Location with name '$name' already exists"
                )
            }
        }
        
        val updatedLocation = location.copy(
            name = name?.trim() ?: location.name,
            slug = if (name != null) generateUniqueSlug(name) else location.slug,
            address = address?.trim() ?: location.address,
            managerName = managerName?.trim() ?: location.managerName,
            managerEmail = managerEmail?.trim()?.lowercase() ?: location.managerEmail,
            status = status ?: location.status,
            updatedAt = LocalDateTime.now()
        )
        
        logger.info("Updating location ID: $id - ${location.name}")
        return locationRepository.save(updatedLocation)
    }
    
    /**
     * Delete a location with proper validation and cleanup
     */
    @Transactional
    fun deleteLocation(id: Long): LocationDeletionResult {
        val location = getLocationByIdOrThrow(id)
        
        // Prevent deletion of default location
        if (location.isDefault) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "Cannot delete the default location"
            )
        }
        
        // Check for associated data
        val salesDataCount = salesDataRepository.findByLocationIdOrderByDateDesc(id).size
        val marinationLogCount = marinationLogRepository.findByLocationOrderByTimestampDesc(location).size
        
        if (salesDataCount > 0 || marinationLogCount > 0) {
            logger.warn("Attempting to delete location with associated data - Sales: $salesDataCount, Logs: $marinationLogCount")
            
            // For safety, we'll mark as inactive instead of hard delete
            val inactiveLocation = location.copy(
                status = LocationStatus.INACTIVE,
                updatedAt = LocalDateTime.now()
            )
            locationRepository.save(inactiveLocation)
            
            return LocationDeletionResult(
                success = true,
                softDelete = true,
                message = "Location marked as inactive due to associated data",
                associatedSalesData = salesDataCount,
                associatedMarinationLogs = marinationLogCount
            )
        }
        
        // Hard delete if no associated data
        logger.info("Deleting location: ${location.name}")
        locationRepository.deleteById(id)
        
        return LocationDeletionResult(
            success = true,
            softDelete = false,
            message = "Location deleted successfully"
        )
    }
    
    /**
     * Force delete a location and all associated data
     */
    @Transactional
    fun forceDeleteLocation(id: Long): LocationDeletionResult {
        val location = getLocationByIdOrThrow(id)
        
        // Prevent deletion of default location
        if (location.isDefault) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "Cannot delete the default location"
            )
        }
        
        // Delete associated data first
        val salesDataCount = salesDataRepository.findByLocationIdOrderByDateDesc(id).size
        val marinationLogCount = marinationLogRepository.findByLocationOrderByTimestampDesc(location).size
        
        salesDataRepository.deleteByLocationId(id)
        marinationLogRepository.deleteByLocation(location)
        
        // Delete the location
        logger.warn("Force deleting location: ${location.name} with $salesDataCount sales records and $marinationLogCount marination logs")
        locationRepository.deleteById(id)
        
        return LocationDeletionResult(
            success = true,
            softDelete = false,
            message = "Location and all associated data deleted",
            associatedSalesData = salesDataCount,
            associatedMarinationLogs = marinationLogCount
        )
    }
    
    /**
     * Activate or deactivate a location
     */
    @Transactional
    fun toggleLocationStatus(id: Long): Location {
        val location = getLocationByIdOrThrow(id)
        
        val newStatus = when (location.status) {
            LocationStatus.ACTIVE -> LocationStatus.INACTIVE
            LocationStatus.INACTIVE -> LocationStatus.ACTIVE
        }
        
        return updateLocation(id, status = newStatus)
    }
    
    /**
     * Check if a slug is available
     */
    fun isSlugAvailable(slug: String): Boolean {
        return locationRepository.findBySlug(slug) == null
    }
    
    /**
     * Validate location exists
     */
    fun validateLocationExists(id: Long): Boolean {
        return locationRepository.existsById(id)
    }
    
    /**
     * Generate a unique slug from a name
     */
    private fun generateUniqueSlug(name: String): String {
        val baseSlug = generateSlug(name)
        var uniqueSlug = baseSlug
        var counter = 1
        
        while (!isSlugAvailable(uniqueSlug)) {
            uniqueSlug = "$baseSlug-$counter"
            counter++
        }
        
        return uniqueSlug
    }
    
    /**
     * Generate slug from name
     */
    private fun generateSlug(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "") // Remove special characters
            .replace(Regex("\\s+"), "-") // Replace spaces with hyphens
            .replace(Regex("-+"), "-") // Replace multiple hyphens with single
            .trim('-') // Remove leading/trailing hyphens
            .take(50) // Limit length
    }
    
    /**
     * Validate location data
     */
    private fun validateLocationData(name: String, managerName: String, managerEmail: String) {
        validateLocationName(name)
        validateManagerName(managerName)
        validateManagerEmail(managerEmail)
    }
    
    /**
     * Validate location name
     */
    private fun validateLocationName(name: String) {
        if (name.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Location name cannot be empty")
        }
        if (name.length < 2) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Location name must be at least 2 characters")
        }
        if (name.length > 100) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Location name must be less than 100 characters")
        }
    }
    
    /**
     * Validate manager name
     */
    private fun validateManagerName(managerName: String) {
        if (managerName.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Manager name cannot be empty")
        }
        if (managerName.length < 2) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Manager name must be at least 2 characters")
        }
        if (managerName.length > 100) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Manager name must be less than 100 characters")
        }
    }
    
    /**
     * Validate manager email
     */
    private fun validateManagerEmail(managerEmail: String) {
        if (managerEmail.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Manager email cannot be empty")
        }
        val emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$".toRegex()
        if (!emailRegex.matches(managerEmail)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format")
        }
    }
}

/**
 * Result data class for location deletion operations
 */
data class LocationDeletionResult(
    val success: Boolean,
    val softDelete: Boolean = false,
    val message: String,
    val associatedSalesData: Int = 0,
    val associatedMarinationLogs: Int = 0
)