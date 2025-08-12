package com.example.chickencalculator.config

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
                    .ignoringRequestMatchers(
                        "/api/admin/auth/login", // Allow login without CSRF
                        "/api/admin/auth/csrf-token", // Allow CSRF token retrieval
                        "/api/health/**",
                        "/actuator/health",
                        "/api/calculator/**",
                        "/api/sales-data/**",
                        "/api/marination-log/**",
                        "/",
                        "/static/**",
                        "/admin/**",
                        "/*.js",
                        "/*.css",
                        "/*.html",
                        "/favicon.ico",
                        "/manifest.json",
                        "/{slug}",
                        "/{slug}/**"
                    )
            }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // Public endpoints - no authentication required
                auth.requestMatchers(
                    "/api/admin/auth/login",
                    "/api/admin/auth/register",
                    "/api/admin/auth/csrf-token", // Allow CSRF token retrieval
                    "/api/health/**",
                    "/actuator/health",
                    "/api/calculator/**",  // Public calculator endpoints
                    "/api/sales-data/**",  // Public sales data endpoints for main app
                    "/api/marination-log/**",  // Public marination log for main app
                    "/",
                    "/static/**",
                    "/admin/**",  // Admin portal static files
                    "/*.js",
                    "/*.css",
                    "/*.html",
                    "/favicon.ico",
                    "/manifest.json",
                    "/{slug}",  // Location-specific routes
                    "/{slug}/**"  // Location sub-routes
                ).permitAll()
                
                // Admin endpoints - require authentication
                auth.requestMatchers("/api/admin/**").authenticated()
                auth.requestMatchers("/api/locations/**").authenticated()
                
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