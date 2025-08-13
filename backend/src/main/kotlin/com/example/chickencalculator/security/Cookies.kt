package com.example.chickencalculator.security

import org.springframework.http.ResponseCookie
import java.time.Duration

/**
 * Build a secure JWT cookie header with proper SameSite configuration
 * 
 * @param name Cookie name (e.g., "jwt_token" or "location_token_slug")
 * @param token JWT token value (empty string to expire)
 * @param maxAge Cookie duration (Duration.ZERO to expire immediately)
 * @param sameSite SameSite policy: "Strict", "Lax", or "None"
 * @return Set-Cookie header value as String
 */
fun buildJwtSetCookieHeader(
    name: String,
    token: String,
    maxAge: Duration = Duration.ofHours(24),
    sameSite: String = System.getenv("JWT_COOKIE_SAMESITE") ?: "Strict"
): String = ResponseCookie.from(name, token)
    .httpOnly(true)
    .secure(true)  // Always use HTTPS in production
    .path("/")
    .maxAge(maxAge)
    .sameSite(sameSite)
    .build()
    .toString()