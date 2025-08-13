$env:JWT_SECRET="test-secret-key-for-testing-purposes-only-1234567890"
$env:ADMIN_DEFAULT_PASSWORD="Admin123!"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"