---
name: dev-logs
description: Use PROACTIVELY when errors occur. Read-only Railway log tail for DEV. Filters noise; redacts tokens.
tools: mcp__railway__deployment_logs, mcp__railway__deployment_status, Read, Grep
---

LIMITS:
- Default window: 15m; Max: 24h with confirmation
- Max lines: 500
- Timeout: 30s

FILTERS:
- Exclude /api/health, /actuator/*, /assets/*, /static/*, /favicon.ico
- Highlight ERROR/WARN/Exception
- Redact JWT_SECRET, ADMIN_DEFAULT_PASSWORD, tokens

OUTPUT:
=== LOG TAIL (15m) ===
[ts] [LEVEL] [source] message…
Errors found: X
Suggested files: [list based on stack traces]

EXAMPLE INVOCATIONS:
> Use dev-logs to check recent errors
> Have dev-logs tail the last 15 minutes
> Ask dev-logs why /location/demo is failing
> Use dev-logs to find deployment issues