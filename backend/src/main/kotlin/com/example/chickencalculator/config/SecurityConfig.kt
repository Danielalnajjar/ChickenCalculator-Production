package com.example.chickencalculator.config

import com.example.chickencalculator.config.ApiVersionConfig
import com.example.chickencalculator.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@Order(1)  // Ensure this config loads before any defaults
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    @Primary
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
                    .requireCsrfProtectionMatcher { request ->
                        // Only require CSRF for state-changing operations that aren't in our ignore list
                        val path = request.servletPath
                        val method = request.method
                        
                        // Skip CSRF for GET, HEAD, TRACE, OPTIONS
                        if (method in listOf("GET", "HEAD", "TRACE", "OPTIONS")) {
                            return@requireCsrfProtectionMatcher false
                        }
                        
                        // Skip CSRF for specific paths
                        when {
                            path.startsWith("/api/v1/admin/auth/") -> false
                            path.startsWith("/api/v1/location/") && path.contains("/auth/") -> false
                            path.startsWith("/api/v1/calculator/") -> false
                            path.startsWith("/api/v1/sales-data/") -> false
                            path.startsWith("/api/v1/marination-log/") -> false
                            path.startsWith("/api/v1/debug/") -> false
                            path.startsWith("/api/admin/auth/") -> false
                            path.startsWith("/api/calculator/") -> false
                            path.startsWith("/api/sales-data/") -> false
                            path.startsWith("/api/marination-log/") -> false
                            path.startsWith("/api/debug/") -> false
                            path.startsWith("/api/health/") -> false
                            path.startsWith("/actuator/") -> false
                            path.startsWith("/static/") -> false
                            path.startsWith("/admin") -> false
                            path == "/" -> false
                            path.endsWith(".js") -> false
                            path.endsWith(".css") -> false
                            path.endsWith(".html") -> false
                            path == "/favicon.ico" -> false
                            path == "/manifest.json" -> false
                            else -> true
                        }
                    }
            }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // Create a custom matcher for public endpoints
                val publicMatcher = RequestMatcher { request ->
                    val path = request.servletPath
                    when {
                        path == "/" -> true
                        path.startsWith("/api/v1/admin/auth/") -> true
                        path.startsWith("/api/v1/location/") && path.contains("/auth/") -> true
                        path.startsWith("/api/v1/calculator/") -> true
                        path.startsWith("/api/v1/sales-data/") -> true
                        path.startsWith("/api/v1/marination-log/") -> true
                        path.startsWith("/api/v1/debug/") -> true
                        path.startsWith("/api/admin/auth/") -> true
                        path.startsWith("/api/calculator/") -> true
                        path.startsWith("/api/sales-data/") -> true
                        path.startsWith("/api/marination-log/") -> true
                        path.startsWith("/api/debug/") -> true
                        path.startsWith("/api/health/") -> true
                        path == "/api/health" -> true
                        path.startsWith("/actuator/") -> true
                        path.startsWith("/static/") -> true
                        path.startsWith("/admin") -> true
                        path.endsWith(".js") -> true
                        path.endsWith(".css") -> true
                        path.endsWith(".html") -> true
                        path == "/favicon.ico" -> true
                        path == "/manifest.json" -> true
                        path.matches(Regex("^/[^/]+$")) -> true  // /{slug} pattern
                        path.matches(Regex("^/[^/]+/.*$")) -> true  // /{slug}/** pattern
                        else -> false
                    }
                }
                
                // Create a custom matcher for admin endpoints
                val adminMatcher = RequestMatcher { request ->
                    val path = request.servletPath
                    when {
                        path.startsWith("/api/v1/admin/") && !path.startsWith("/api/v1/admin/auth/") -> true
                        path.startsWith("/api/admin/") && !path.startsWith("/api/admin/auth/") -> true
                        path.startsWith("/api/locations/") -> true
                        else -> false
                    }
                }
                
                // Apply the matchers
                auth.requestMatchers(publicMatcher).permitAll()
                auth.requestMatchers(adminMatcher).authenticated()
                
                // All other requests require authentication
                auth.anyRequest().authenticated()
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(10)
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:*",
            "https://*.railway.app",
            "https://*.up.railway.app",
            "https://chickencalculator-production-production-2953.up.railway.app"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "X-XSRF-TOKEN",
            "X-Location-Id"
        )
        configuration.exposedHeaders = listOf("Authorization", "X-Location-Id", "X-Location-Name", "X-Location-Slug")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}