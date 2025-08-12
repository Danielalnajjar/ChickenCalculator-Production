package com.example.chickencalculator

import com.example.chickencalculator.service.AdminService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication(exclude = [FlywayAutoConfiguration::class])
@EnableAsync
class ChickenCalculatorApplication {
    
    @Bean
    fun initializeAdminUser(adminService: AdminService) = CommandLineRunner {
        adminService.initializeDefaultAdmin()
    }
}

fun main(args: Array<String>) {
    runApplication<ChickenCalculatorApplication>(*args)
}