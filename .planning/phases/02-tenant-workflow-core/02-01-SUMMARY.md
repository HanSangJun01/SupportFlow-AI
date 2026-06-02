---
phase: 02-tenant-workflow-core
plan: 02-01
subsystem: api
tags: [spring-boot, mongodb, tenants, operational-users, webmvc]

requires:
  - phase: 01-backend-foundation
    provides: Tenant workspace APIs, ticket APIs, Mongo repository patterns, WebMvc test conventions
provides:
  - Tenant metadata update API with immutable slug behavior
  - ACTIVE/INACTIVE tenant status and service-level active-tenant guard
  - Tenant-local operational user metadata model, repository, service, and API
  - Active actor and active support-agent validation helpers for later ticket workflow plans
affects: [02-tenant-workflow-core, ticket-workflow, tenant-isolation, openapi]

tech-stack:
  added: []
  patterns:
    - Tenant-scoped repository lookups use tenantId in every route-backed operational user lookup
    - Operational users remain metadata-only until post-v1 authentication work

key-files:
  created:
    - backend-spring/src/main/java/com/supportflow/user/OperationalUser.java
    - backend-spring/src/main/java/com/supportflow/user/OperationalUserRole.java
    - backend-spring/src/main/java/com/supportflow/user/OperationalUserStatus.java
    - backend-spring/src/main/java/com/supportflow/user/OperationalUserRepository.java
    - backend-spring/src/main/java/com/supportflow/user/OperationalUserService.java
    - backend-spring/src/main/java/com/supportflow/user/OperationalUserController.java
    - backend-spring/src/test/java/com/supportflow/user/OperationalUserServiceTest.java
    - backend-spring/src/test/java/com/supportflow/user/OperationalUserApiIntegrationTest.java
  modified:
    - backend-spring/src/main/java/com/supportflow/tenant/TenantStatus.java
    - backend-spring/src/main/java/com/supportflow/tenant/TenantService.java
    - backend-spring/src/main/java/com/supportflow/tenant/TenantController.java
    - backend-spring/src/test/java/com/supportflow/tenant/TenantServiceTest.java
    - backend-spring/src/test/java/com/supportflow/tenant/TenantApiIntegrationTest.java
    - backend-spring/src/test/java/com/supportflow/SupportFlowApplicationTests.java
    - backend-spring/src/test/java/com/supportflow/common/OpenApiDocumentationTest.java

key-decisions:
  - "Tenant slug remains immutable; tenant updates accept only name, description, and status."
  - "Operational users are tenant-local metadata only and do not include password, credential, session, or auth fields."

patterns-established:
  - "Mutation guard: TenantService.requireActiveTenant separates inactive-tenant mutation checks from read helpers."
  - "Validation helpers: OperationalUserService validates active same-tenant actors and support-agent assignees for downstream ticket workflow operations."

requirements-completed: [TEN-02, TEN-03, TEN-04, ISO-03]

duration: 13 min
completed: 2026-05-13
---

# Phase 02 Plan 01: Tenant Administration and Operational User Metadata Summary

**Tenant metadata updates with inactive-tenant guard plus tenant-local operational user profiles for workflow attribution and assignment validation**

## Performance

- **Duration:** 13 min
- **Started:** 2026-05-13T12:47:40Z
- **Completed:** 2026-05-13T13:01:03Z
- **Tasks:** 2
- **Files modified:** 15

## Accomplishments

- Added `PATCH /api/v1/tenants/{tenantId}` for partial tenant metadata/status updates while excluding slug updates.
- Added `TenantStatus.INACTIVE` and `TenantService.requireActiveTenant(...)` for later mutation-path enforcement.
- Added tenant-scoped operational user metadata APIs under `/api/v1/tenants/{tenantId}/users`.
- Added active actor and active support-agent validation helpers using `findByTenantIdAndId` for tenant-boundary safety.
- Extended OpenAPI/context-load test coverage for the new operational user controller.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add tenant metadata update and active-tenant guard** - `a555640` (feat)
2. **Task 2: Add tenant-local operational user metadata** - `9d7bcf2` (feat)

**Plan metadata:** committed with this summary.

## Files Created/Modified

- `backend-spring/src/main/java/com/supportflow/tenant/TenantStatus.java` - Adds `INACTIVE`.
- `backend-spring/src/main/java/com/supportflow/tenant/TenantService.java` - Adds tenant update command and active-tenant guard.
- `backend-spring/src/main/java/com/supportflow/tenant/TenantController.java` - Adds tenant patch route and slug-free update request record.
- `backend-spring/src/main/java/com/supportflow/user/OperationalUser.java` - Adds Mongo document for tenant-local operational user metadata.
- `backend-spring/src/main/java/com/supportflow/user/OperationalUserRepository.java` - Adds tenant-scoped repository lookup methods.
- `backend-spring/src/main/java/com/supportflow/user/OperationalUserService.java` - Adds create/list/detail/status and workflow validation helpers.
- `backend-spring/src/main/java/com/supportflow/user/OperationalUserController.java` - Adds create/list/detail/status endpoints.
- `backend-spring/src/test/java/com/supportflow/tenant/TenantServiceTest.java` - Covers inactive status, updates, slug immutability, and inactive guard.
- `backend-spring/src/test/java/com/supportflow/tenant/TenantApiIntegrationTest.java` - Covers tenant patch route and request shape.
- `backend-spring/src/test/java/com/supportflow/user/OperationalUserServiceTest.java` - Covers tenant-scoped user behavior and validation helpers.
- `backend-spring/src/test/java/com/supportflow/user/OperationalUserApiIntegrationTest.java` - Covers operational user API behavior and no-auth-field request records.
- `backend-spring/src/test/java/com/supportflow/SupportFlowApplicationTests.java` - Adds broad context mock for the new service.
- `backend-spring/src/test/java/com/supportflow/common/OpenApiDocumentationTest.java` - Adds operational user route assertions.

## Decisions Made

- Tenant update DTOs intentionally omit slug to preserve existing slug identity and uniqueness assumptions.
- Inactive operational users remain readable but are rejected by validation helpers for new actor/assignee use.
- Role values remain metadata only; no authentication, sessions, Spring Security, or RBAC enforcement was added.

## Verification

- `cd backend-spring && ./mvnw test -Dtest=TenantServiceTest,TenantApiIntegrationTest` - PASSED, 11 tests.
- `cd backend-spring && ./mvnw test -Dtest=OperationalUserServiceTest,OperationalUserApiIntegrationTest` - PASSED, 11 tests.
- `cd backend-spring && ./mvnw verify` - PASSED, 37 tests run, 1 Testcontainers test skipped because Docker was unavailable and the test is annotated to skip without Docker.
- Acceptance checks passed for required symbols/routes and absence of slug/auth fields in update/user request surfaces.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added broad-test mocks for the new operational user service**
- **Found during:** Plan-level verification after Task 2
- **Issue:** `./mvnw verify` failed in context-load and OpenAPI tests that intentionally exclude Mongo repositories; the new controller/service needed a mock in those broad tests.
- **Fix:** Added `@MockitoBean OperationalUserService` to `SupportFlowApplicationTests` and `OpenApiDocumentationTest`; extended OpenAPI assertions for operational user routes.
- **Files modified:** `backend-spring/src/test/java/com/supportflow/SupportFlowApplicationTests.java`, `backend-spring/src/test/java/com/supportflow/common/OpenApiDocumentationTest.java`
- **Verification:** `cd backend-spring && ./mvnw verify` passed.
- **Committed in:** `9d7bcf2` (amended into Task 2 commit)

**Total deviations:** 1 auto-fixed (Rule 3)
**Impact on plan:** Test-only fix required for full-suite correctness; no production scope change.

## Known Stubs

None. Null checks in `TenantService.updateTenant(...)` are the intended partial-update behavior, not placeholder data.

## Issues Encountered

- Docker was not available during `./mvnw verify`; the Docker-backed Testcontainers test skipped as configured. Non-Docker unit, WebMvc, OpenAPI, and application context tests passed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Ready for `02-02`: ticket workflow metadata updates can call `TenantService.requireActiveTenant(...)`, `OperationalUserService.validateActiveActor(...)`, and `OperationalUserService.validateActiveSupportAgent(...)`.

## Self-Check: PASSED

- Created summary file exists: `.planning/phases/02-tenant-workflow-core/02-01-SUMMARY.md`.
- Task commits found: `a555640`, `9d7bcf2`.
- Key created files found under `backend-spring/src/main/java/com/supportflow/user/`.
- Required verification commands passed.

---
*Phase: 02-tenant-workflow-core*
*Completed: 2026-05-13*
