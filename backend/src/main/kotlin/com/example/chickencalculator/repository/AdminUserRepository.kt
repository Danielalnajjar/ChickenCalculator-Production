package com.example.chickencalculator.repository

import com.example.chickencalculator.entity.AdminUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminUserRepository : JpaRepository<AdminUser, Long> {
    fun findByEmail(email: String): AdminUser?
}