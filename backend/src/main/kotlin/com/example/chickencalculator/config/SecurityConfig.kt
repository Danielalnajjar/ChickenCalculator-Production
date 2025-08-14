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

    companion object {
        // Centralized path patterns using AntPathRequestMatcher for better maintainability
        val PUBLIC_API_PATTERNS = arrayOf(
            "/api/health",
            "/api/health/**",
            "/api/v1/admin/auth/**",
            "/api/v1/location/*/auth/**",
            "/api/v1/calculator/locations",
            "/actuator/health",
            "/actuator/info"
        )
        
        val PUBLIC_STATIC_PATTERNS = arrayOf(
            "/",
            "/admin",
            "/admin/**",
            "/admin/static/**",
            "/location/**",
            "/location/*/static/**",
            "/static/**",
            "/assets/**",
            "/favicon.ico",
            "/manifest.json",
            "/*.js",
            "/*.css",
            "/*.html"
        )
        
        val ADMIN_API_PATTERNS = arrayOf(
            "/api/v1/admin/locations",
            "/api/v1/admin/locations/**",
            "/api/v1/admin/stats",
            "/api/v1/admin/stats/**"
        )
        
        val CSRF_IGNORE_PATTERNS = arrayOf(
            "/api/v1/admin/auth/**",
            "/api/v1/location/*/auth/**",
            "/api/health",
            "/api/health/**",
            "/actuator/**"
        )
    }

    @Bean
    @Primary
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
                    .requireCsrfProtectionMatcher { request ->
                        // Only require CSRF for state-changing operations that aren't in our ignore list
                        val method = request.method
                        
                        // Skip CSRF for GET, HEAD, TRACE, OPTIONS
                        if (method in listOf("GET", "HEAD", "TRACE", "OPTIONS")) {
                            return@requireCsrfProtectionMatcher false
                        }
                        
                        // Check if path matches any ignore pattern
                        val path = request.servletPath
                        !CSRF_IGNORE_PATTERNS.any { pattern ->
                            if (pattern.contains("*")) {
                                val regex = pattern.replace("/", "\\/").replace("**", ".*").replace("*", "[^/]*")
                                path.matches(Regex(regex))
                            } else {
                                path == pattern || path.startsWith("$pattern/")
                            }
                        }
                    }
            }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // Public API endpoints
                auth.requestMatchers(*PUBLIC_API_PATTERNS).permitAll()
                
                // Public static resources - be explicit about static file paths
                auth.requestMatchers(*PUBLIC_STATIC_PATTERNS).permitAll()
                auth.requestMatchers("/admin/static/**").permitAll()
                auth.requestMatchers("/location/*/static/**").permitAll()
                
                // Location-specific endpoints (handled by LocationAuthFilter)
                auth.requestMatchers(
                    "/api/v1/calculator/calculate",
                    "/api/v1/sales-data",
                    "/api/v1/sales-data/**",
                    "/api/v1/marination-log",
                    "/api/v1/marination-log/**"
                ).permitAll()  // LocationAuthFilter handles actual auth
                
                // Admin API endpoints require authentication
                auth.requestMatchers(*ADMIN_API_PATTERNS).authenticated()
                
                // All other requests require authentication
                auth.anyRequest().authenticated()
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .headers { headers ->
                headers
                    .contentTypeOptions { }  // X-Content-Type-Options: nosniff
                    .xssProtection { }
                    .frameOptions { it.sameOrigin() }
                    .contentSecurityPolicy { csp ->
                        csp.policyDirectives(
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self' data:; " +
                            "connect-src 'self' https://*.railway.app; " +
                            "frame-ancestors 'none'"
                        )
                    }
            }

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