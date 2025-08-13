# Sentry Integration Guide

## ALWAYS USE FOR DEBUGGING

### Automatic Usage Triggers

#### 1. Before Any Debugging Session
- **Always run first**: `mcp__sentry__search_issues` with "recent errors last hour"
- Check if current issue already exists in Sentry before investigating

#### 2. When User Reports Problems
Common phrases that should trigger Sentry checks:
- "Something is broken" → Check Sentry for recent errors
- "The app crashed" → Search for exceptions in last hour
- "Users are seeing errors" → Get error counts and patterns
- "Production issue" → Check all unresolved issues
- "It's not working" → Search for related error messages
- "Debug this" → Start with Sentry error analysis

#### 3. After Code Changes
- **Post-deployment**: Always run `mcp__sentry__search_events` "error count last hour"
- **After fixes**: Verify error is resolved with `mcp__sentry__get_issue_details`
- **After refactoring**: Check for new error patterns

#### 4. During Development
- **Before starting work**: Check if feature has existing errors
- **Testing new code**: Monitor for new error signatures
- **Performance issues**: Check for timeout/slow query errors

### Sentry MCP Commands

```bash
# Always start debugging with:
mcp__sentry__search_issues - organizationSlug: wok-to-walk, naturalLanguageQuery: "unresolved errors from last hour"

# Get error statistics:
mcp__sentry__search_events - organizationSlug: wok-to-walk, naturalLanguageQuery: "how many errors today"

# Analyze specific issues:
mcp__sentry__get_issue_details - organizationSlug: wok-to-walk, issueId: [from search results]

# Check deployment impact:
mcp__sentry__search_events - naturalLanguageQuery: "error rate before and after [timestamp]"
```

### Connection Details
- **Organization**: wok-to-walk
- **Project**: java-spring-boot
- **Region URL**: https://us.sentry.io
- **DSN**: Configured in application.yml