---
allowed-tools: Read(*), Edit(*), MultiEdit(*), Bash(git diff:*), Bash(git status:*), Bash(git log:*)
description: Session handoff - update docs for next Claude instance
argument-hint: [key accomplishments or blockers from session]
thinking-mode: ultrathink
model: opus
---

## Session Handoff Documentation Update

You are about to clear context. Let's ensure the next Claude has everything needed to continue effectively.

### 1. Detect What Changed This Session
!git diff --stat
!git status --short
!git log --oneline -5

### 2. Read Current Documentation
@CLAUDE.md
@KNOWN_ISSUES.md
@docs/quick-reference.md
@docs/testing-guide.md

### 3. Critical Updates to Make

#### A. Session Summary (Line 4 of CLAUDE.md)
**Replace** the "Last Session" line after the version line with:
- **Last Session**: Brief session summary including $ARGUMENTS
- Keep it concise - single line format only
- Include date, key accomplishments, and any critical blockers

#### B. Version & Status (Line 3)
- If deployed: Update version and date
- If critical fixes: Update status
- If major changes: Bump version

#### C. Sprint Focus Section
- Mark completed items with ✅ (but keep in place for now)
- Add any new priorities discovered
- Flag any blocked items with 🚧
- Clean up: Remove completed items older than 7 days to keep focus on active work
- Keep only 3-5 active items maximum

#### D. Quick Commands Section  
- Add any new commands discovered this session
- Update any commands that changed
- Remove any deprecated commands

#### E. Known Issues
- Mark fixed issues as ✅ Resolved
- Add any new issues discovered
- Update test coverage percentage if tests were run

#### F. Auto-Update Supporting Documents

**docs/quick-reference.md updates:**
- Update test coverage percentage (check all 3 locations if mentioned)
- Update last deploy date from git log
- Verify Railway IDs still work

**docs/testing-guide.md updates:**
- Update current coverage percentage in Current Status section
- Remove any test issues that were fixed this session
- Update target coverage if it changed

**KNOWN_ISSUES.md updates:**
- Update test coverage percentage if mentioned

#### G. Important Context
If there are critical blockers or incomplete work, add them to:
- Sprint Focus (as 🚧 blocked items)
- Known Issues (if they're bugs)
- Don't create separate context sections - use existing structure

### 4. Smart Change Detection & Updates

Check what changed this session:
- If test coverage changed: Update in all documentation files
- If new scripts added: Check if docs mention them
- If APIs changed: Flag for potential api-reference.md updates
- If deployment config changed: Note for deployment-guide.md

Auto-update these always:
- Last updated date → today
- Test coverage → latest percentage across all files
- Git information → recent commits for context

### 5. Response Format

"📋 Session Handoff & Documentation Update Complete

**Documentation Updated:**
✅ CLAUDE.md: [what changed]
✅ docs/quick-reference.md: [what changed]
✅ docs/testing-guide.md: [what changed]
✅ KNOWN_ISSUES.md: [what changed]

**Session Context:**
📌 Key context for next session: [critical points]
🚧 Incomplete work: [what needs finishing]
💡 The next Claude should start by: [suggested first action]

**Change Detection:**
📊 Test coverage: [old] → [new]
🔧 Code changes: [summary]
⚠️ Needs manual review: [any docs that couldn't be auto-updated]"

Then show the actual edits being made.

### 6. Ensure Session Continuity

The goal is seamless knowledge transfer. The next Claude instance should:
- Immediately understand current project state
- Know what was just completed (to avoid repetition)
- See any blockers or incomplete work clearly
- Have clear suggested next actions
- Understand any important context or decisions made

Make the handoff so smooth that the next session can continue as if there was no context break.

### 7. Documentation Pruning (Prevent Bloat)

CRITICAL: Always clean up old information to keep docs concise:

#### Before Adding New Content:
1. **Last Session Line**: Replace previous session info (single line only)
2. **Sprint Focus**: Remove completed items older than 7 days (max 5 active items)
3. **Known Issues**: Archive resolved issues older than 30 days
4. **Keep it Simple**: Don't create new sections - work with existing structure

#### Size Guidelines:
- CLAUDE.md should stay under 500 lines
- If approaching limit, archive old content to `docs/archive/`
- Focus on actionable, current information only
- Remove redundant or stale information

#### Rolling Window Approach:
- Last Session Line: Replace, never append  
- Sprint Focus: Keep only active work, remove completed items after 7 days
- Current priorities only: No historical tracking
- Minimalist approach: Use existing sections, don't create new ones

This ensures documentation follows Claude Code best practices of staying focused and actionable.