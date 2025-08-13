@echo off
echo Starting backend with production-like profile...

set JWT_SECRET=test-secret-key-for-testing-purposes-only-1234567890
set ADMIN_DEFAULT_PASSWORD=Admin123!
set SPRING_PROFILES_ACTIVE=prod

cd backend
java -jar target\chicken-calculator-1.0.0.jar