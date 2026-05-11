---
phase: 1
slug: backend-foundation
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-08
approved: 2026-05-11
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter + Spring Boot Test + Testcontainers |
| **Config file** | `backend-spring/pom.xml` |
| **Quick run command** | `cd backend-spring && ./mvnw test` |
| **Full suite command** | `cd backend-spring && ./mvnw verify` |
| **Estimated runtime** | ~90 seconds after dependencies are warm |

---

## Sampling Rate

- **After every task commit:** Run `cd backend-spring && ./mvnw test`
- **After every plan wave:** Run `cd backend-spring && ./mvnw verify`
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01-01 | 1 | QUAL-06 | T-01-01 | Backend starts without Redis and connects to MongoDB configuration | build/smoke | `cd backend-spring && ./mvnw test` | ✅ | ✅ green |
| 01-01-02 | 01-01 | 1 | QUAL-06 | T-01-02 | Docker Compose exposes MongoDB and backend local run path | build/smoke | `docker compose config && cd backend-spring && ./mvnw test` | ✅ | ✅ green |
| 01-02-01 | 01-02 | 2 | TEN-01 | T-02-01 | Tenant create/list/detail reject invalid input and produce server-generated IDs | integration | `cd backend-spring && ./mvnw test -Dtest=TenantApiIntegrationTest` | ✅ | ✅ green |
| 01-02-02 | 01-02 | 2 | TICK-01,TICK-02,TICK-03,TICK-05,ISO-01,ISO-02 | T-02-02 | Ticket create/list/detail always require tenant path scope | integration | `cd backend-spring && ./mvnw test -Dtest=TicketApiIntegrationTest` | ✅ | ✅ green |
| 01-03-01 | 01-03 | 3 | FLOW-01,FLOW-02 | T-03-01 | Invalid status transitions are rejected | unit | `cd backend-spring && ./mvnw test -Dtest=TicketStatusTransitionPolicyTest` | ✅ | ✅ green |
| 01-03-02 | 01-03 | 3 | ISO-01,ISO-02,ISO-04 | T-03-02 | Cross-tenant ticket reads and status updates return 404 | integration | `cd backend-spring && ./mvnw test -Dtest=TenantIsolationIntegrationTest` | ✅ | ✅ green |
| 01-04-01 | 01-04 | 4 | QUAL-01 | T-04-01 | OpenAPI docs expose only planned Phase 1 routes | integration | `cd backend-spring && ./mvnw test -Dtest=OpenApiDocumentationTest` | ✅ | ✅ green |
| 01-04-02 | 01-04 | 4 | QUAL-02,QUAL-03,QUAL-06 | T-04-02 | Full unit and integration suite verifies Phase 1 behavior | full suite | `cd backend-spring && ./mvnw verify` | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `backend-spring/pom.xml` — Maven test framework, Spring Boot, and Testcontainers dependencies.
- [x] `backend-spring/src/test/java/com/supportflow/**` — unit and integration test directories.
- [x] `backend-spring/mvnw` — Maven wrapper generated or copied through standard Maven wrapper tooling.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| None | N/A | N/A | All Phase 1 behaviors have automated verification. |

---

## Validation Sign-Off

- [x] All tasks have automated verify commands or Wave 0 dependencies.
- [x] Sampling continuity: no 3 consecutive tasks without automated verify.
- [x] Wave 0 covers all missing references.
- [x] No watch-mode flags.
- [x] Feedback latency target is under 120 seconds after dependency warmup.
- [x] `nyquist_compliant: true` set in frontmatter.

**Approval:** approved 2026-05-11 after `docker compose config` and Docker-backed `MAVEN_USER_HOME=.m2 ./mvnw -Dmaven.repo.local=.m2/repository verify` passed with 19 tests run, 0 failures, 0 errors, and 0 skipped.
