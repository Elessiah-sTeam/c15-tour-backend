---
name: check-security
description: Audit all endpoints against SecurityConfig to find accidentally public routes, missing auth, or incorrect role requirements
allowed-tools: Read, Grep, Glob
context: fork
---

Audit the Spring Security configuration against all controllers.

**Files to read:**
- `backend/src/main/java/com/c15tour/backend/security/SecurityConfig.java` — the authorizeHttpRequests rules
- All files in `backend/src/main/java/com/c15tour/backend/controller/`

**Check for:**

1. **Accidentally public endpoints** — routes not listed in `permitAll()` that should be protected, or routes listed in `permitAll()` that should require auth
2. **Missing role checks** — endpoints that deal with sensitive data but only require `isAuthenticated()` instead of `hasRole("ADMIN")`
3. **Overly broad wildcards** — patterns like `/**` that may unintentionally expose endpoints
4. **Actuator / internal endpoints** — check if any management endpoints are exposed
5. **CORS mismatches** — allowed origins in `CorsConfig.java` vs actual frontend URLs

Output a clear list of findings grouped as:
- 🔴 **Breaking** — endpoint is exposed when it should not be
- 🟡 **Warning** — endpoint access is broader than necessary
- 🟢 **OK** — everything correctly configured
