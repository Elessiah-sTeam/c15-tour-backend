---
name: fix-lint
description: Run the Maven build and Checkstyle, fix every reported issue. Use when CI fails on lint or before committing.
allowed-tools: Bash, Read, Edit, Glob, Grep
---

1. Run `cd backend && mvn checkstyle:check 2>&1` and capture the full output.
2. Fix every reported violation. Common patterns in this project:
   - Missing Javadoc on public methods → add a concise `/** ... */` comment
   - Import ordering → reorder to match Checkstyle config
   - Line length → break long lines
   - Unused imports → remove them
3. Re-run `mvn checkstyle:check 2>&1` to confirm zero violations before finishing.

Do not suppress warnings with `@SuppressWarnings` unless there is no other option.
