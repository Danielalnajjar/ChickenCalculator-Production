# ChickenCalculator — Development Subagents (Security-First)

**Manual selection only. No auto-triggers.**

## Agents:
- `dev-architect` — proposal-first diffs; backups under .claude/backups/
- `test-generator` — tests only; no shell
- `config-doctor` — config/security proposals; no shell
- `dev-orchestrator` — whitelisted local commands (ONLY agent with Bash)
- `dev-deployer` — DEV-only Railway deploys; DRY-RUN → EXECUTE [plan-id]; refuses PROD
- `dev-logs` — read-only log tail

## Environment Setup (use placeholders, never hardcode):
```bash
export RAILWAY_PROJECT_ID=<your-project-id>
export DEV_SERVICE_ID=<your-dev-service>
export DEV_ENVIRONMENT_ID=<your-dev-env>
export DEV_BASE_URL=https://<dev-app>.up.railway.app
export ALLOWED_ENV_VARS=FEATURE_FLAG_X,API_BASE_URL   # non-secrets only
export PROD_SERVICE_ID=<prod-id-to-block>  # for safety blocking
```

## Security Features:
- ✅ No shell access except dev-orchestrator
- ✅ File-based backups (no shell cp)
- ✅ Policy-based timeouts (not GNU timeout)
- ✅ Prod service blocking
- ✅ Env updates OFF by default
- ✅ Double confirmation (plan-id required)
- ✅ Secret redaction in all outputs

## Usage Flow:
1. `dev-architect` → propose → APPROVE [plan-id]
2. `test-generator` → create tests (fix application-test.yml:3)
3. `config-doctor` → validate Spring Boot 3 config
4. `dev-orchestrator` → run whitelisted builds/tests
5. `dev-deployer` → DRY-RUN → EXECUTE [plan-id] (dev only)
6. `dev-logs` → check deployment results

## Backup Directory:
All file edits are backed up to: `.claude/backups/`

## Version Control
All project subagents in `.claude/agents/` are tracked in Git for team collaboration and history.
- Commit message format: `feat(agents): Add/Update [agent-name] subagent`
- Review changes before committing: `git diff .claude/agents/`
- Share improvements with team via pull requests

## Known Issues Addressed:
- Test config broken (application-test.yml:3)
- Low coverage (30% → 80% target)
- Missing jest-environment-jsdom
- Spring 6 path patterns
- Security hardening

## Usage Options (Router)

**Option 1: Convenience Router (Optional)**
- Use `dev-router` when you want the system to pick the agent for you.
- The router suggests an agent and forwards a safe instruction (proposal/dry-run).
- You still need to confirm changes: `APPROVE <plan-id>` for code; `EXECUTE <plan-id>` for dev deploys.

**Option 2: Direct Agent Selection (Recommended for precision)**
- Call a specific agent yourself: `dev-architect`, `test-generator`, `config-doctor`, `dev-orchestrator`, `dev-deployer`, `dev-logs`.
- Full control, no routing layer.

**Safety Reminders**
- Production operations are always blocked.
- Router is read-only and cannot execute writes or deploys.
- Defaults to proposals/dry-runs/read-only.
- If ambiguous, the router asks one clarifying question or defaults to a proposal.