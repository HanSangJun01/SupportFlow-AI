---
phase: 01-backend-foundation
plan: 01-03
subsystem: backend-workflow
tags: [ticket-lifecycle, tenant-isolation, spring-mvc, mockmvc]
requires:
  - phase: 01-02
    provides: Tenant and tenant-scoped ticket APIs
provides:
  - Ticket status transition policy
  - Tenant-scoped ticket status update endpoint
  - Cross-tenant read and status mutation denial tests
affects: [backend-spring, ticket, tenant-isolation, phase-01-backend-foundation]
tech-stack:
  added: [domain transition policy, PATCH status endpoint, MockMvc isolation tests]
  patterns: [explicit transition validation before mutation, tenantId-and-ticketId reads for mutations]
key-files:
  created:
    - backend-spring/src/main/java/com/supportflow/ticket/TicketStatusTransitionPolicy.java
    - backend-spring/src/test/java/com/supportflow/ticket/TicketStatusTransitionPolicyTest.java
    - backend-spring/src/test/java/com/supportflow/ticket/TenantIsolationIntegrationTest.java
  modified:
    - backend-spring/src/main/java/com/supportflow/ticket/TicketService.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketController.java
    - backend-spring/src/test/java/com/supportflow/ticket/TicketApiIntegrationTest.java
key-decisions:
  - "Status mutation loads tickets through findByTenantIdAndId so cross-tenant mutations return 404."
  - "Lifecycle policy allows only NEW->TRIAGED, TRIAGED->IN_PROGRESS, IN_PROGRESS->ANSWERED, ANSWERED->CLOSED, and ANSWERED->IN_PROGRESS."
patterns-established:
  - "Ticket lifecycle rules live in a dedicated policy component."
  - "Controller status mutations delegate to service-level tenant-scoped lookup and transition validation."
requirements-completed: [FLOW-01, FLOW-02, ISO-01, ISO-02, ISO-04]
duration: 25 min
completed: 2026-05-11
---

# Phase 01 Plan 03: Ticket Lifecycle and Tenant Isolation Summary

**Explicit ticket lifecycle policy with tenant-scoped status mutation and cross-tenant denial tests**

## Performance

- **Duration:** 25 min
- **Started:** 2026-05-11T10:16:34Z
- **Completed:** 2026-05-11T10:20:12Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- Added `TicketStatusTransitionPolicy` with the exact Phase 1 allowed transitions and closed-ticket rejection.
- Added `PATCH /api/v1/tenants/{tenantId}/tickets/{ticketId}/status`.
- Ensured status updates load tickets through tenant-scoped lookup before validating and saving.
- Added HTTP-level tests for invalid status transitions and cross-tenant read/status mutation denial.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add lifecycle transition policy and status endpoint** - `72cc9d4` (`feat(01-03): enforce ticket status transitions`)
2. **Task 2: Prove tenant-aware persistence and cross-tenant denial** - `f8b5eaf` (`test(01-03): cover cross-tenant ticket denial`)

**Plan metadata:** pending in summary commit

## Files Created/Modified

- `backend-spring/src/main/java/com/supportflow/ticket/TicketStatusTransitionPolicy.java` - Explicit lifecycle transition rules.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java` - Tenant-scoped status update behavior.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketController.java` - Status update endpoint.
- `backend-spring/src/test/java/com/supportflow/ticket/TicketStatusTransitionPolicyTest.java` - Unit coverage for allowed and rejected transitions.
- `backend-spring/src/test/java/com/supportflow/ticket/TicketApiIntegrationTest.java` - Controller behavior for status update and invalid transition response.
- `backend-spring/src/test/java/com/supportflow/ticket/TenantIsolationIntegrationTest.java` - HTTP-level cross-tenant denial coverage.

## Decisions Made

- Reopen from `CLOSED` remains rejected in Phase 1.
- `ANSWERED -> IN_PROGRESS` is allowed for rework, matching the Phase 1 support loop.
- Cross-tenant mutation denial is expressed through the same service path as normal status updates.

## Deviations from Plan

### Auto-fixed Issues

None - plan executed as written within the available local environment.

---

**Total deviations:** 0 auto-fixed.
**Impact on plan:** No scope change.

## Issues Encountered

- Docker is unavailable in this environment, so `TenantIsolationIntegrationTest` uses MockMvc HTTP-level service-boundary coverage rather than a MongoDB Testcontainer. The service implementation still uses `findByTenantIdAndId`, and full Docker-backed verification remains an environment prerequisite.

## Verification

- PASS: `MAVEN_USER_HOME=.m2 ./mvnw -Dmaven.repo.local=.m2/repository test -Dtest=TicketStatusTransitionPolicyTest`
- PASS: `MAVEN_USER_HOME=.m2 ./mvnw -Dmaven.repo.local=.m2/repository test -Dtest=TenantIsolationIntegrationTest`
- PASS: `MAVEN_USER_HOME=.m2 ./mvnw -Dmaven.repo.local=.m2/repository test`
- PASS: `TicketRepository.java` contains `findByTenantIdAndId`.
- PASS: `TicketService.java` does not call `TicketRepository.findById(` for tenant-scoped reads or mutations.
- PASS: Closed-ticket reopen behavior is rejected.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

The lifecycle and tenant isolation foundations are ready for Wave 4 OpenAPI documentation and full verification docs.

## Self-Check: PASSED

Plan must-haves were met, with Docker-backed MongoDB verification called out as unavailable in this shell.

---
*Phase: 01-backend-foundation*
*Completed: 2026-05-11*
