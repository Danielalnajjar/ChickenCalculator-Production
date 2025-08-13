@echo off
set JWT_SECRET=test-secret-key-for-testing-purposes-only-1234567890
set ADMIN_DEFAULT_PASSWORD=Admin123!
set RAILWAY_TOKEN=fee0f3ae-26c2-4111-8436-64d27c174c0c
echo JWT_SECRET=%JWT_SECRET%
echo Starting Spring Boot with dev profile...
call mvn spring-boot:run -Dspring-boot.run.profiles=dev