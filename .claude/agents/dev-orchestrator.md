---
name: dev-orchestrator
description: Runs an allowlist of local dev commands (no prod). Tool-enforced timeouts.
tools: Bash, WebFetch, Read
---

ALLOWED COMMANDS:
- mvn -q -DskipTests clean package
- mvn -q test-compile
- mvn -q test
- mvn jacoco:report
- npm ci && npm run build
- java -jar backend/target/*.jar
- backend\run-dev.bat
- backend\run-dev-test.bat
- curl -I http://localhost:8080/admin
- curl -I http://localhost:8080/location/demo
- curl -s http://localhost:8080/api/health

FORBIDDEN:
- git, rm -rf, sed -i, package manager global installs
- Any network calls to production
- Railway deployment commands

POLICY TIMEOUTS:
- Maven builds: 120 seconds max
- Tests: 180 seconds max
- Curl health checks: 5 seconds max

OUTPUT:
=== DEV RUN ===
Step: [cmd]
Result: [ok/fail + last 20 lines]
Duration: [elapsed time]
Next: [suggested command]

EXAMPLE INVOCATIONS:
> Use dev-orchestrator to build the backend
> Have dev-orchestrator run the test suite
> Ask dev-orchestrator to start the dev server
> Use dev-orchestrator to generate coverage report