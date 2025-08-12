package com.example.chickencalculator.service

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.entity.LocationStatus
import com.example.chickencalculator.exception.LocationNotFoundException
import com.example.chickencalculator.repository.LocationRepository
import com.example.chickencalculator.repository.SalesDataRepository
import com.example.chickencalculator.repository.MarinationLogRepository
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.assertThrows
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class LocationManagementServiceTest {

    @Mock
    private lateinit var locationRepository: LocationRepository
    
    @Mock
    private lateinit var salesDataRepository: SalesDataRepository
    
    @Mock
    private lateinit var marinationLogRepository: MarinationLogRepository

    @InjectMocks
    private lateinit var locationManagementService: LocationManagementService

    private lateinit var testLocation: Location

    @BeforeEach
    fun setup() {
        testLocation = Location(
            id = 1L,
            name = "Test Restaurant",
            slug = "test-restaurant",
            managerName = "Test Manager",
            managerEmail = "manager@test.com",
            address = "123 Test Street",
            status = LocationStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            isDefault = false
        )
    }
    
    // Helper function to create test locations
    private fun createTestLocation(
        id: Long = 1L,
        name: String = "Test Location",
        slug: String = "test-location",
        managerName: String = "Test Manager",
        managerEmail: String = "manager@test.com",
        address: String? = "123 Test Street"
    ): Location {
        return Location(
            id = id,
            name = name,
            slug = slug,
            managerName = managerName,
            managerEmail = managerEmail,
            address = address,
            status = LocationStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            isDefault = false
        )
    }

    @Nested
    @DisplayName("Find Location by Slug Tests")
    inner class FindLocationBySlugTests {

        @Test
        @DisplayName("Should find location by valid slug")
        fun testFindByValidSlug() {
            // Given
            val slug = "test-restaurant"
            whenever(locationRepository.findBySlug(slug)).thenReturn(testLocation)

            // When
            val result = locationManagementService.getLocationBySlug(slug)

            // Then
            assertNotNull(result)
            assertEquals(testLocation.slug, result?.slug)
            assertEquals(testLocation.name, result?.name)
            verify(locationRepository).findBySlug(slug)
        }

        @Test
        @DisplayName("Should return null for non-existent slug")
        fun testFindByNonExistentSlug() {
            // Given
            val slug = "non-existent-slug"
            whenever(locationRepository.findBySlug(slug)).thenReturn(null)

            // When
            val result = locationManagementService.getLocationBySlug(slug)

            // Then
            assertNull(result)
            verify(locationRepository).findBySlug(slug)
        }

        @Test
        @DisplayName("Should handle empty slug")
        fun testFindByEmptySlug() {
            // Given
            val slug = ""
            whenever(locationRepository.findBySlug(slug)).thenReturn(null)

            // When
            val result = locationManagementService.getLocationBySlug(slug)

            // Then
            assertNull(result)
            verify(locationRepository).findBySlug(slug)
        }

        @Test
        @DisplayName("Should handle case sensitivity correctly")
        fun testFindBySlugCaseSensitivity() {
            // Given
            val slug = "Test-Restaurant"
            whenever(locationRepository.findBySlug(slug)).thenReturn(null)

            // When
            val result = locationManagementService.getLocationBySlug(slug)

            // Then
            assertNull(result) // Assuming slugs are case-sensitive
            verify(locationRepository).findBySlug(slug)
        }
    }

    @Nested
    @DisplayName("Get All Locations Tests")
    inner class GetAllLocationsTests {

        @Test
        @DisplayName("Should return all locations sorted by name")
        fun testGetAllLocations() {
            // Given
            val locations = listOf(
                testLocation, // "Test Restaurant"
                createTestLocation(2L, "Another Restaurant", "another-restaurant")
            )
            whenever(locationRepository.findAll()).thenReturn(locations)

            // When
            val result = locationManagementService.getAllLocations()

            // Then
            assertNotNull(result)
            assertEquals(2, result.size)
            // Results are sorted by name, so "Another Restaurant" comes first
            assertEquals("Another Restaurant", result[0].name)
            assertEquals("Test Restaurant", result[1].name)
            verify(locationRepository).findAll()
        }

        @Test
        @DisplayName("Should return empty list when no locations exist")
        fun testGetAllLocationsEmpty() {
            // Given
            whenever(locationRepository.findAll()).thenReturn(emptyList())

            // When
            val result = locationManagementService.getAllLocations()

            // Then
            assertNotNull(result)
            assertTrue(result.isEmpty())
            verify(locationRepository).findAll()
        }
    }

    @Nested
    @DisplayName("Get Location by ID Tests")
    inner class GetLocationByIdTests {

        @Test
        @DisplayName("Should find location by valid ID")
        fun testFindByValidId() {
            // Given
            val locationId = 1L
            whenever(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation))

            // When
            val result = locationManagementService.getLocationById(locationId)

            // Then
            assertNotNull(result)
            assertEquals(testLocation.id, result?.id)
            assertEquals(testLocation.name, result?.name)
            verify(locationRepository).findById(locationId)
        }

        @Test
        @DisplayName("Should throw exception for non-existent ID")
        fun testFindByNonExistentId() {
            // Given
            val locationId = 999L
            whenever(locationRepository.findById(locationId)).thenReturn(Optional.empty())

            // When & Then
            val exception = assertThrows<ResponseStatusException> {
                locationManagementService.getLocationByIdOrThrow(locationId)
            }
            assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
            verify(locationRepository).findById(locationId)
        }

        @Test
        @DisplayName("Should handle negative ID")
        fun testFindByNegativeId() {
            // Given
            val locationId = -1L
            whenever(locationRepository.findById(locationId)).thenReturn(Optional.empty())

            // When & Then
            val exception = assertThrows<ResponseStatusException> {
                locationManagementService.getLocationByIdOrThrow(locationId)
            }
            assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
            verify(locationRepository).findById(locationId)
        }
    }

    @Nested
    @DisplayName("Create Location Tests")
    inner class CreateLocationTests {

        @Test
        @DisplayName("Should create location successfully")
        fun testCreateLocation() {
            // Given
            val locationName = "New Restaurant"
            val expectedSlug = "new-restaurant"
            val savedLocation = createTestLocation(
                id = 2L,
                name = locationName,
                slug = expectedSlug
            )
            
            whenever(locationRepository.findByNameIgnoreCase(locationName)).thenReturn(null)
            whenever(locationRepository.findBySlug(expectedSlug)).thenReturn(null)
            whenever(locationRepository.save(any<Location>())).thenReturn(savedLocation)

            // When
            val result = locationManagementService.createLocation(
                name = locationName,
                address = "Test Address",
                managerName = "Test Manager",
                managerEmail = "test@example.com"
            )

            // Then
            assertNotNull(result)
            assertEquals(locationName, result.name)
            assertEquals(expectedSlug, result.slug)
            verify(locationRepository).findBySlug(expectedSlug)
            verify(locationRepository).save(any<Location>())
        }

        @Test
        @DisplayName("Should handle duplicate slug by generating unique slug")
        fun testCreateLocationWithDuplicateSlug() {
            // Given
            val locationName = "Test Restaurant"
            val baseSlug = "test-restaurant"
            val uniqueSlug = "test-restaurant-1"
            
            whenever(locationRepository.findByNameIgnoreCase(locationName)).thenReturn(null)
            whenever(locationRepository.findBySlug(baseSlug)).thenReturn(testLocation) // Slug is taken
            whenever(locationRepository.findBySlug(uniqueSlug)).thenReturn(null)
            
            val savedLocation = createTestLocation(
                id = 3L,
                name = locationName,
                slug = uniqueSlug
            )
            whenever(locationRepository.save(any<Location>())).thenReturn(savedLocation)

            // When
            val result = locationManagementService.createLocation(
                name = locationName,
                address = "Test Address",
                managerName = "Test Manager",
                managerEmail = "test@example.com"
            )

            // Then
            assertNotNull(result)
            assertEquals(locationName, result.name)
            assertEquals(uniqueSlug, result.slug)
            verify(locationRepository).findBySlug(baseSlug)
            verify(locationRepository).findBySlug(uniqueSlug)
        }

        @Test
        @DisplayName("Should handle special characters in location name")
        fun testCreateLocationWithSpecialCharacters() {
            // Given
            val locationName = "Mike's Chicken & Grill!"
            val expectedSlug = "mikes-chicken-grill"
            
            whenever(locationRepository.findByNameIgnoreCase(locationName)).thenReturn(null)
            whenever(locationRepository.findBySlug(expectedSlug)).thenReturn(null)
            
            val savedLocation = createTestLocation(
                id = 4L,
                name = locationName,
                slug = expectedSlug
            )
            whenever(locationRepository.save(any<Location>())).thenReturn(savedLocation)

            // When
            val result = locationManagementService.createLocation(
                name = locationName,
                address = "Test Address",
                managerName = "Test Manager",
                managerEmail = "test@example.com"
            )

            // Then
            assertNotNull(result)
            assertEquals(locationName, result.name)
            assertEquals(expectedSlug, result.slug)
        }

        @Test
        @DisplayName("Should handle empty location name")
        fun testCreateLocationWithEmptyName() {
            // Given
            val locationName = ""

            // When & Then
            val exception = assertThrows<ResponseStatusException> {
                locationManagementService.createLocation(
                    name = locationName,
                    address = "Test Address",
                    managerName = "Test Manager",
                    managerEmail = "test@example.com"
                )
            }
            assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        }

        @Test
        @DisplayName("Should handle whitespace-only location name")
        fun testCreateLocationWithWhitespaceOnlyName() {
            // Given
            val locationName = "   "

            // When & Then
            val exception = assertThrows<ResponseStatusException> {
                locationManagementService.createLocation(
                    name = locationName,
                    address = "Test Address",
                    managerName = "Test Manager",
                    managerEmail = "test@example.com"
                )
            }
            assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        }
    }

    @Nested
    @DisplayName("Delete Location Tests")
    inner class DeleteLocationTests {

        @Test
        @DisplayName("Should delete location successfully")
        fun testDeleteLocation() {
            // Given
            val locationId = 1L
            val location = testLocation.copy(id = locationId, isDefault = false)
            whenever(locationRepository.findById(locationId)).thenReturn(Optional.of(location))
            whenever(salesDataRepository.findByLocationIdOrderByDateDesc(locationId)).thenReturn(emptyList())
            whenever(marinationLogRepository.findByLocationOrderByTimestampDesc(location)).thenReturn(emptyList())

            // When
            val result = locationManagementService.deleteLocation(locationId)

            // Then
            assertTrue(result.success)
            assertFalse(result.softDelete)
            verify(locationRepository).findById(locationId)
            verify(locationRepository).deleteById(locationId)
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent location")
        fun testDeleteNonExistentLocation() {
            // Given
            val locationId = 999L
            whenever(locationRepository.findById(locationId)).thenReturn(Optional.empty())

            // When & Then
            val exception = assertThrows<ResponseStatusException> {
                locationManagementService.deleteLocation(locationId)
            }
            assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
            verify(locationRepository).findById(locationId)
            verify(locationRepository, never()).deleteById(any())
        }
    }

    @Nested
    @DisplayName("Slug Generation Tests")
    inner class SlugGenerationTests {

        @Test
        @DisplayName("Should generate simple slug correctly")
        fun testGenerateSimpleSlug() {
            // This would test the slug generation logic if it was exposed as a public method
            // For now, we test it indirectly through createLocation
            
            val locationName = "Simple Name"
            val expectedSlug = "simple-name"
            
            whenever(locationRepository.findByNameIgnoreCase(locationName)).thenReturn(null)
            whenever(locationRepository.findBySlug(expectedSlug)).thenReturn(null)
            whenever(locationRepository.save(any<Location>())).thenAnswer { 
                val location = it.arguments[0] as Location
                location.copy(id = 1L)
            }

            val result = locationManagementService.createLocation(
                name = locationName,
                address = "Test Address",
                managerName = "Test Manager",
                managerEmail = "test@example.com"
            )

            assertEquals(expectedSlug, result.slug)
        }

        @Test
        @DisplayName("Should handle multiple consecutive spaces")
        fun testSlugWithMultipleSpaces() {
            val locationName = "Multiple    Spaces"
            val expectedSlug = "multiple-spaces"
            
            whenever(locationRepository.findByNameIgnoreCase(locationName)).thenReturn(null)
            whenever(locationRepository.findBySlug(expectedSlug)).thenReturn(null)
            whenever(locationRepository.save(any<Location>())).thenAnswer { 
                val location = it.arguments[0] as Location
                location.copy(id = 1L)
            }

            val result = locationManagementService.createLocation(
                name = locationName,
                address = "Test Address",
                managerName = "Test Manager",
                managerEmail = "test@example.com"
            )

            assertEquals(expectedSlug, result.slug)
        }
    }
}