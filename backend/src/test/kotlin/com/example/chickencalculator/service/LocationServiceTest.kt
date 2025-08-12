package com.example.chickencalculator.service

import com.example.chickencalculator.entity.Location
import com.example.chickencalculator.exception.LocationNotFoundException
import com.example.chickencalculator.repository.LocationRepository
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
class LocationServiceTest {

    @Mock
    private lateinit var locationRepository: LocationRepository

    @InjectMocks
    private lateinit var locationService: LocationService

    private lateinit var testLocation: Location

    @BeforeEach
    fun setup() {
        testLocation = Location(
            id = 1L,
            name = "Test Restaurant",
            slug = "test-restaurant",
            createdAt = LocalDateTime.now()
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
            val result = locationService.findBySlug(slug)

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
            val result = locationService.findBySlug(slug)

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
            val result = locationService.findBySlug(slug)

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
            val result = locationService.findBySlug(slug)

            // Then
            assertNull(result) // Assuming slugs are case-sensitive
            verify(locationRepository).findBySlug(slug)
        }
    }

    @Nested
    @DisplayName("Get All Locations Tests")
    inner class GetAllLocationsTests {

        @Test
        @DisplayName("Should return all locations")
        fun testGetAllLocations() {
            // Given
            val locations = listOf(
                testLocation,
                Location(2L, "Another Restaurant", "another-restaurant", LocalDateTime.now())
            )
            whenever(locationRepository.findAll()).thenReturn(locations)

            // When
            val result = locationService.getAllLocations()

            // Then
            assertNotNull(result)
            assertEquals(2, result.size)
            assertEquals(locations[0].name, result[0].name)
            assertEquals(locations[1].name, result[1].name)
            verify(locationRepository).findAll()
        }

        @Test
        @DisplayName("Should return empty list when no locations exist")
        fun testGetAllLocationsEmpty() {
            // Given
            whenever(locationRepository.findAll()).thenReturn(emptyList())

            // When
            val result = locationService.getAllLocations()

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
            val result = locationService.getLocationById(locationId)

            // Then
            assertNotNull(result)
            assertEquals(testLocation.id, result.id)
            assertEquals(testLocation.name, result.name)
            verify(locationRepository).findById(locationId)
        }

        @Test
        @DisplayName("Should throw exception for non-existent ID")
        fun testFindByNonExistentId() {
            // Given
            val locationId = 999L
            whenever(locationRepository.findById(locationId)).thenReturn(Optional.empty())

            // When & Then
            assertThrows<LocationNotFoundException> {
                locationService.getLocationById(locationId)
            }
            verify(locationRepository).findById(locationId)
        }

        @Test
        @DisplayName("Should handle negative ID")
        fun testFindByNegativeId() {
            // Given
            val locationId = -1L
            whenever(locationRepository.findById(locationId)).thenReturn(Optional.empty())

            // When & Then
            assertThrows<LocationNotFoundException> {
                locationService.getLocationById(locationId)
            }
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
            val savedLocation = Location(
                id = 2L,
                name = locationName,
                slug = expectedSlug,
                createdAt = LocalDateTime.now()
            )
            
            whenever(locationRepository.existsBySlug(expectedSlug)).thenReturn(false)
            whenever(locationRepository.save(any<Location>())).thenReturn(savedLocation)

            // When
            val result = locationService.createLocation(locationName)

            // Then
            assertNotNull(result)
            assertEquals(locationName, result.name)
            assertEquals(expectedSlug, result.slug)
            verify(locationRepository).existsBySlug(expectedSlug)
            verify(locationRepository).save(any<Location>())
        }

        @Test
        @DisplayName("Should handle duplicate slug by generating unique slug")
        fun testCreateLocationWithDuplicateSlug() {
            // Given
            val locationName = "Test Restaurant"
            val baseSlug = "test-restaurant"
            val uniqueSlug = "test-restaurant-1"
            
            whenever(locationRepository.existsBySlug(baseSlug)).thenReturn(true)
            whenever(locationRepository.existsBySlug(uniqueSlug)).thenReturn(false)
            
            val savedLocation = Location(
                id = 3L,
                name = locationName,
                slug = uniqueSlug,
                createdAt = LocalDateTime.now()
            )
            whenever(locationRepository.save(any<Location>())).thenReturn(savedLocation)

            // When
            val result = locationService.createLocation(locationName)

            // Then
            assertNotNull(result)
            assertEquals(locationName, result.name)
            assertEquals(uniqueSlug, result.slug)
            verify(locationRepository).existsBySlug(baseSlug)
            verify(locationRepository).existsBySlug(uniqueSlug)
        }

        @Test
        @DisplayName("Should handle special characters in location name")
        fun testCreateLocationWithSpecialCharacters() {
            // Given
            val locationName = "Mike's Chicken & Grill!"
            val expectedSlug = "mikes-chicken-grill"
            
            whenever(locationRepository.existsBySlug(expectedSlug)).thenReturn(false)
            
            val savedLocation = Location(
                id = 4L,
                name = locationName,
                slug = expectedSlug,
                createdAt = LocalDateTime.now()
            )
            whenever(locationRepository.save(any<Location>())).thenReturn(savedLocation)

            // When
            val result = locationService.createLocation(locationName)

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
            assertThrows<IllegalArgumentException> {
                locationService.createLocation(locationName)
            }
        }

        @Test
        @DisplayName("Should handle whitespace-only location name")
        fun testCreateLocationWithWhitespaceOnlyName() {
            // Given
            val locationName = "   "

            // When & Then
            assertThrows<IllegalArgumentException> {
                locationService.createLocation(locationName)
            }
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
            whenever(locationRepository.existsById(locationId)).thenReturn(true)

            // When
            locationService.deleteLocation(locationId)

            // Then
            verify(locationRepository).existsById(locationId)
            verify(locationRepository).deleteById(locationId)
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent location")
        fun testDeleteNonExistentLocation() {
            // Given
            val locationId = 999L
            whenever(locationRepository.existsById(locationId)).thenReturn(false)

            // When & Then
            assertThrows<LocationNotFoundException> {
                locationService.deleteLocation(locationId)
            }
            verify(locationRepository).existsById(locationId)
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
            
            whenever(locationRepository.existsBySlug(expectedSlug)).thenReturn(false)
            whenever(locationRepository.save(any<Location>())).thenAnswer { 
                val location = it.arguments[0] as Location
                location.copy(id = 1L)
            }

            val result = locationService.createLocation(locationName)

            assertEquals(expectedSlug, result.slug)
        }

        @Test
        @DisplayName("Should handle multiple consecutive spaces")
        fun testSlugWithMultipleSpaces() {
            val locationName = "Multiple    Spaces"
            val expectedSlug = "multiple-spaces"
            
            whenever(locationRepository.existsBySlug(expectedSlug)).thenReturn(false)
            whenever(locationRepository.save(any<Location>())).thenAnswer { 
                val location = it.arguments[0] as Location
                location.copy(id = 1L)
            }

            val result = locationService.createLocation(locationName)

            assertEquals(expectedSlug, result.slug)
        }
    }
}