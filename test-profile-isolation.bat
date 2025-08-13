@echo off
echo Testing Profile Isolation for Debug Components
echo ===============================================
echo.

echo Setting up environment variables...
set JWT_SECRET=test-secret-key-for-testing-purposes-only-1234567890
set ADMIN_DEFAULT_PASSWORD=Admin123!

echo.
echo Starting application with PRODUCTION profile...
cd backend
start /b cmd /c "mvn spring-boot:run -Dspring.profiles.active=production > production-test.log 2>&1"

echo Waiting for application to start (30 seconds)...
timeout /t 30 /nobreak >nul

echo.
echo Testing debug endpoints (should return 404)...
echo.
echo Testing /probe/ok endpoint:
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:8080/probe/ok

echo.
echo Testing /test endpoint:
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:8080/test

echo.
echo Testing /minimal endpoint:
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:8080/minimal

echo.
echo Testing /debug/converters endpoint:
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:8080/debug/converters

echo.
echo Testing production endpoints (should work)...
echo.
echo Testing /api/health endpoint:
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:8080/api/health

echo.
echo Stopping application...
taskkill /f /fi "WINDOWTITLE eq mvn*" >nul 2>&1
timeout /t 5 /nobreak >nul

echo.
echo ===============================================
echo Now testing with DEV profile...
echo.
start /b cmd /c "mvn spring-boot:run -Dspring.profiles.active=dev > dev-test.log 2>&1"

echo Waiting for application to start (30 seconds)...
timeout /t 30 /nobreak >nul

echo.
echo Testing debug endpoints (should return 200 or expected status)...
echo.
echo Testing /probe/ok endpoint:
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:8080/probe/ok

echo.
echo Testing /test endpoint:
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:8080/test

echo.
echo Testing /minimal endpoint:
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:8080/minimal

echo.
echo Testing /debug/converters endpoint:
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:8080/debug/converters

echo.
echo Stopping application...
taskkill /f /fi "WINDOWTITLE eq mvn*" >nul 2>&1

echo.
echo ===============================================
echo Profile isolation test complete!
echo.
echo Expected results:
echo - Production profile: Debug endpoints return 404
echo - Dev profile: Debug endpoints return 200
echo.
pause