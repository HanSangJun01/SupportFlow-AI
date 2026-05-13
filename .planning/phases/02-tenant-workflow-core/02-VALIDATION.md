---
phase: 02
slug: tenant-workflow-core
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-13
---

# Phase 02 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter, Spring Boot Test, Mockito, AssertJ, Testcontainers |
| **Config file** | `backend-spring/pom.xml` |
| **Quick run command** | `cd backend-spring && ./mvnw test -Dtest=TenantServiceTest,OperationalUserServiceTest,TicketServiceTest,TenantApiIntegrationTest,OperationalUserApiIntegrationTest,TicketApiIntegrationTest` |
| **Full suite command** | `cd backend-spring && ./mvnw verify` |
| **Estimated runtime** | ~90 seconds with dependencies cached; longer when Docker/Testcontainers images are cold |

---

## Sampling Rate

- **After every task commit:** Run the task-specific `<automated>` command from the active plan.
- **After every plan wave:** Run `cd backend-spring && ./mvnw verify`.
- **Before `$gsd-verify-work`:** Full suite must be green.
- **Max feedback latency:** 120 seconds for unit/WebMvc slices; Docker-backed tests may exceed this on cold start.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | TEN-02, TEN-03, ISO-03 | T-02-01-01 | Slug remains immutable; inactive tenant mutations are blocked | unit + WebMvc | `cd backend-spring && ./mvnw test -Dtest=TenantServiceTest,TenantApiIntegrationTest` | yes | pending |
| 02-01-02 | 01 | 1 | TEN-04, ISO-03 | T-02-01-02 | Operational users are tenant-scoped and non-auth metadata only | unit + WebMvc | `cd backend-spring && ./mvnw test -Dtest=OperationalUserServiceTest,OperationalUserApiIntegrationTest` | no | pending |
| 02-02-01 | 02 | 2 | TICK-04, FLOW-03, FLOW-04, FLOW-05, ISO-03 | T-02-02-01 | Actor and assignee validation prevents cross-tenant or inactive-user assignment | unit + WebMvc | `cd backend-spring && ./mvnw test -Dtest=TicketServiceTest,TicketApiIntegrationTest` | yes | pending |
| 02-02-02 | 02 | 2 | FLOW-03, TICK-04 | T-02-02-02 | Status and metadata changes append immutable actor-attributed history | unit + WebMvc | `cd backend-spring && ./mvnw test -Dtest=TicketServiceTest,TicketApiIntegrationTest` | yes | pending |
| 02-03-01 | 03 | 3 | ISO-03, TEN-03, TICK-04 | T-02-03-01 | Cross-tenant ticket/user/workflow attempts do not leak or mutate data | integration | `cd backend-spring && ./mvnw test -Dtest=TenantWorkflowMongoIntegrationTest` | no | pending |
| 02-03-02 | 03 | 3 | QUAL evidence for Phase 2 scope | T-02-03-02 | API docs and contract docs expose exact Phase 2 routes and constraints | docs + full suite | `cd backend-spring && ./mvnw verify` | partial | pending |

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements.

---

## Manual-Only Verifications

All Phase 2 behaviors have automated verification.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency target documented
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending execution
