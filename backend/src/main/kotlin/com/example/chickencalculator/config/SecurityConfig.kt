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
                        // Versioned API endpoints
                        "${ApiVersionConfig.API_VERSION}/admin/auth/login",
                        "${ApiVersionConfig.API_VERSION}/admin/auth/validate",
                        "${ApiVersionConfig.API_VERSION}/admin/auth/logout",
                        "${ApiVersionConfig.API_VERSION}/admin/auth/csrf-token",
                        "${ApiVersionConfig.API_VERSION}/admin/auth/change-password",
                        "${ApiVersionConfig.API_VERSION}/calculator/**",
                        "${ApiVersionConfig.API_VERSION}/sales-data/**",
                        "${ApiVersionConfig.API_VERSION}/marination-log/**",
                        "${ApiVersionConfig.API_VERSION}/debug/**",
                        // Legacy API endpoints (for backward compatibility)
                        "/api/admin/auth/login",
                        "/api/admin/auth/validate",
                        "/api/admin/auth/logout",
                        "/api/admin/auth/csrf-token",
                        "/api/admin/auth/change-password",
                        "/api/calculator/**",
                        "/api/sales-data/**",
                        "/api/marination-log/**",
                        "/api/debug/**",
                        // Health endpoints (unversioned)
                        "/api/health/**",
                        "/actuator/health",
                        "/actuator/prometheus",
                        "/actuator/metrics/**",
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
                    // Versioned API endpoints
                    "${ApiVersionConfig.API_VERSION}/admin/auth/login",
                    "${ApiVersionConfig.API_VERSION}/admin/auth/register",
                    "${ApiVersionConfig.API_VERSION}/admin/auth/csrf-token",
                    "${ApiVersionConfig.API_VERSION}/admin/auth/validate",  // Allow manual auth handling
                    "${ApiVersionConfig.API_VERSION}/calculator/**",
                    "${ApiVersionConfig.API_VERSION}/sales-data/**",
                    "${ApiVersionConfig.API_VERSION}/marination-log/**",
                    "${ApiVersionConfig.API_VERSION}/debug/**",
                    // Legacy API endpoints (for backward compatibility)
                    "/api/admin/auth/login",
                    "/api/admin/auth/register",
                    "/api/admin/auth/csrf-token",
                    "/api/admin/auth/validate",  // Allow manual auth handling
                    "/api/calculator/**",
                    "/api/sales-data/**",
                    "/api/marination-log/**",
                    "/api/debug/**",
                    // Health endpoints (unversioned)
                    "/api/health/**",
                    "/actuator/health",
                    "/actuator/prometheus",
                    "/actuator/metrics/**",
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
                auth.requestMatchers("${ApiVersionConfig.API_VERSION}/admin/**").authenticated()
                auth.requestMatchers("/api/admin/**").authenticated() // Legacy support
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