@echo off
set JWT_SECRET=test-secret-key-for-testing-purposes-only-1234567890
set ADMIN_DEFAULT_PASSWORD=Admin123!
mvn spring-boot:run -Dspring-boot.run.profiles=dev