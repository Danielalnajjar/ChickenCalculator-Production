package com.example.chickencalculator.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PasswordChangeRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,
    
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    val newPassword: String,
    
    @field:NotBlank(message = "Password confirmation is required")
    val confirmPassword: String
)