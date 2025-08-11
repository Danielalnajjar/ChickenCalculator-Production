package com.example.chickencalculator.service

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import com.example.chickencalculator.repository.LocationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LocationService(
    private val locationRepository: LocationRepository
) {
    
    private val logger = LoggerFactory.getLogger(LocationService::class.java)
    
    fun getAllLocations(): List<Location> {
        return locationRepository.findAll()
    }
    
    fun getLocationById(id: Long): Location? {
        return locationRepository.findById(id).orElse(null)
    }
    
    fun getLocationBySlug(slug: String): Location? {
        return locationRepository.findBySlug(slug)
    }
    
    fun getDefaultLocation(): Location? {
        return locationRepository.findByIsDefaultTrue()
    }
    
    @Transactional
    fun createLocation(
        name: String,
        address: String?,
        managerName: String,
        managerEmail: String
    ): Location {
        // Generate slug from name
        val slug = generateSlug(name)
        
        val location = Location(
            name = name,
            slug = slug,
            address = address,
            managerName = managerName,
            managerEmail = managerEmail,
            status = LocationStatus.ACTIVE,
            isDefault = false
        )
        
        logger.info("Creating new location: $name with slug: $slug")
        return locationRepository.save(location)
    }
    
    @Transactional
    fun updateLocation(
        id: Long,
        name: String? = null,
        address: String? = null,
        managerName: String? = null,
        managerEmail: String? = null,
        status: LocationStatus? = null
    ): Location? {
        val location = locationRepository.findById(id).orElse(null) ?: return null
        
        val updatedLocation = location.copy(
            name = name ?: location.name,
            slug = if (name != null) generateSlug(name) else location.slug,
            address = address ?: location.address,
            managerName = managerName ?: location.managerName,
            managerEmail = managerEmail ?: location.managerEmail,
            status = status ?: location.status,
            updatedAt = LocalDateTime.now()
        )
        
        return locationRepository.save(updatedLocation)
    }
    
    @Transactional
    fun deleteLocation(id: Long) {
        val location = locationRepository.findById(id).orElse(null)
        if (location != null && !location.isDefault) {
            logger.info("Deleting location: ${location.name}")
            locationRepository.deleteById(id)
        } else if (location?.isDefault == true) {
            logger.warn("Cannot delete default location")
            throw IllegalStateException("Cannot delete the default location")
        }
    }
    
    private fun generateSlug(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "") // Remove special characters
            .replace(Regex("\\s+"), "-") // Replace spaces with hyphens
            .replace(Regex("-+"), "-") // Replace multiple hyphens with single
            .trim('-') // Remove leading/trailing hyphens
    }
}