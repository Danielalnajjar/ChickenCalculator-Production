package com.example.chickencalculator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/api/admin/auth/login").permitAll()
                    .requestMatchers("/api/admin/auth/validate").permitAll()
                    .requestMatchers("/api/admin/test").permitAll()
                    .requestMatchers("/api/health/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    
                    // Static resources
                    .requestMatchers("/", "/index.html", "/test.html").permitAll()
                    .requestMatchers("/admin/**").permitAll()
                    .requestMatchers("/app/**").permitAll()
                    .requestMatchers("/static/**").permitAll()
                    .requestMatchers("/*.js", "/*.css", "/*.ico", "/*.png").permitAll()
                    
                    // Calculator API endpoints (public for now)
                    .requestMatchers("/api/calculator/**").permitAll()
                    .requestMatchers("/api/sales/**").permitAll()
                    .requestMatchers("/api/marination/**").permitAll()
                    
                    // All other endpoints require authentication
                    .anyRequest().authenticated()
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }

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
            "https://*.up.railway.app"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}