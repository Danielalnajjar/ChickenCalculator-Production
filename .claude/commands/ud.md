---
allowed-tools: Read(CLAUDE.md), Read(docs/*.md), Read(KNOWN_ISSUES.md), Read(README.md), Grep, LS
description: Update and verify project documentation follows Claude Code best practices
argument-hint: [check|update|verify]
---

## Documentation Maintenance Check

Please verify our ChickenCalculator documentation follows Claude Code best practices:

### CLAUDE.md Structure Verification
- Version and status are current (line 3: Version 1.0.0)
- Personal preferences import exists: `@~/.claude/chicken-calculator-preferences.md`
- All documentation files use @ import syntax
- Critical instructions (Sentry-first debugging) are prominent
- Sprint focus items reflect current priorities

### Code Conventions Check
- Spring Boot patterns documented (@RestController, @Transactional, etc.)
- Kotlin conventions specified (data classes, val vs var, named parameters)
- Error handling patterns included (@ControllerAdvice, correlation IDs)
- Database patterns documented (JpaRepository, @Query, @Version)

### Commands Documentation Status
- Development, testing, building commands are current
- Windows scripts (.bat files) are listed
- Pre-deployment verification workflow included
- MCP commands have correct IDs:
  - Railway Project: 767deec0-30ac-4238-a57b-305f5470b318
  - Railway Service: fde8974b-10a3-4b70-b5f1-73c4c5cebbbe
  - Sentry Org: wok-to-walk

### Error Patterns Coverage
- Authentication errors (JWT, CSRF, 401/403)
- Database issues (connection pool, migrations)
- Railway deployment issues (port binding, env vars)
- Frontend issues (CORS, build errors)

### Import Integrity Check
- Verify all @ imports resolve to existing files:
  - @docs/sentry-integration.md
  - @docs/quick-reference.md
  - @docs/api-reference.md
  - @docs/development-workflow.md
  - @docs/deployment-guide.md
  - @docs/testing-guide.md
  - @KNOWN_ISSUES.md
  - @README.md

### Updates from Current Session
Check if any of these need updating based on this session's work:
1. New dependencies or version changes
2. New error patterns discovered
3. New commands or scripts created
4. Changed Railway/Sentry/Database configurations
5. Modified test coverage percentage (currently ~30%)
6. New code patterns or conventions established

### Action Required
$ARGUMENTS

Please respond with one of:
- "✅ Documentation verified - all best practices followed"
- "📝 Updates needed: [list specific updates required]"
- "🔄 Updates applied: [list changes made to documentation]"

If updates are needed, also indicate which files should be modified.