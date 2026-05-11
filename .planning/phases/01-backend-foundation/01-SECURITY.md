---
phase: 1
slug: backend-foundation
status: verified
threats_open: 0
asvs_level: 1
created: 2026-05-11
---

# Phase 1 - Security

Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Local developer machine to Docker Compose services | Backend, MongoDB, and Redis are run locally through Docker Compose for Phase 1 development. | Local API traffic and development data |
| HTTP API client to Spring backend | Tenant and ticket API requests enter the Spring MVC controllers. | Tenant IDs, ticket/customer fields, status updates |
| Spring backend to MongoDB | Repositories persist tenants and tenant-scoped tickets. | Tenant records and ticket records |
| Spring backend to generated documentation endpoints | Springdoc exposes generated OpenAPI and Swagger UI locally. | API schemas and endpoint metadata |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-01-01 | Tampering | `application.yml` MongoDB configuration | mitigate | `spring.data.mongodb.uri` is explicit and environment-overridable; committed defaults use local MongoDB without credentials. Evidence: `backend-spring/src/main/resources/application.yml:6`, `docker-compose.yml:15`. | closed |
| T-01-02 | Denial of Service | Local Docker Compose backend/MongoDB path | accept | Accepted as Phase 1 local-only risk; no production exposure or rate limiting is claimed. Evidence: accepted risk `AR-01-01`; local commands in `README.md:30`, `README.md:36`. | closed |
| T-01-03 | Information Disclosure | Actuator and generated docs dependencies | mitigate | Actuator web exposure is limited to `health,info`. Evidence: `backend-spring/src/main/resources/application.yml:24`. | closed |
| T-02-01 | Tampering | Tenant create API | mitigate | Tenant create validates `name` and `slug`, relies on server-side Mongo IDs, and defaults status to `ACTIVE`. Evidence: `backend-spring/src/main/java/com/supportflow/tenant/TenantController.java:52`, `backend-spring/src/main/java/com/supportflow/tenant/TenantService.java:24`, `backend-spring/src/main/java/com/supportflow/tenant/TenantService.java:28`, `backend-spring/src/test/java/com/supportflow/tenant/TenantApiIntegrationTest.java:48`. | closed |
| T-02-02 | Information Disclosure | Ticket detail/list APIs | mitigate | Ticket routes require path `tenantId`; list and detail reads are constrained by repository methods using `tenantId`. Evidence: `backend-spring/src/main/java/com/supportflow/ticket/TicketController.java:23`, `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java:42`, `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java:59`, `backend-spring/src/main/java/com/supportflow/ticket/TicketRepository.java:14`. | closed |
| T-02-03 | Repudiation | API error responses | mitigate | Validation, response-status, and illegal-argument failures return structured `ApiErrorResponse` with timestamp, status, error, message, and path. Evidence: `backend-spring/src/main/java/com/supportflow/common/ApiErrorResponse.java:5`, `backend-spring/src/main/java/com/supportflow/common/GlobalExceptionHandler.java:39`. | closed |
| T-03-01 | Tampering | Ticket status update endpoint | mitigate | Status updates call `TicketStatusTransitionPolicy`; invalid transitions throw HTTP 400. Evidence: `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java:65`, `backend-spring/src/main/java/com/supportflow/ticket/TicketStatusTransitionPolicy.java:28`, `backend-spring/src/test/java/com/supportflow/ticket/TicketStatusTransitionPolicyTest.java:25`. | closed |
| T-03-02 | Information Disclosure | Cross-tenant ticket access | mitigate | Reads and mutations resolve tickets with `findByTenantIdAndId`; cross-tenant reads and status mutations return 404. Evidence: `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java:58`, `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java:64`, `backend-spring/src/test/java/com/supportflow/ticket/TenantIsolationIntegrationTest.java:46`, `backend-spring/src/test/java/com/supportflow/ticket/TenantIsolationMongoIntegrationTest.java:47`. | closed |
| T-03-03 | Elevation of Privilege | Tenant path parameter before authentication | mitigate | Phase 1 documents path `tenantId` as the tenant scope, defers user-level auth, and proves negative tenant-isolation paths with HTTP tests. Evidence: `docs/sdd/phase-01-backend-foundation-api.md:47`, `docs/sdd/phase-01-backend-foundation-api.md:126`, `backend-spring/src/test/java/com/supportflow/ticket/TenantIsolationIntegrationTest.java:46`, `backend-spring/src/test/java/com/supportflow/ticket/TenantIsolationMongoIntegrationTest.java:47`. | closed |
| T-04-01 | Information Disclosure | OpenAPI docs | mitigate | OpenAPI configuration documents Phase 1 tenant/ticket APIs without auth claims, secrets, or MongoDB internals; docs test checks Phase 1 routes. Evidence: `backend-spring/src/main/java/com/supportflow/common/OpenApiConfig.java:13`, `backend-spring/src/test/java/com/supportflow/common/OpenApiDocumentationTest.java:39`, `docs/sdd/phase-01-backend-foundation-api.md:135`. | closed |
| T-04-02 | Repudiation | Verification evidence | mitigate | Automated docs and backend test evidence exist; current audit ran `./mvnw test` successfully with 19 tests passing. Evidence: `backend-spring/src/test/java/com/supportflow/common/OpenApiDocumentationTest.java:33`, `backend-spring/src/test/java/com/supportflow/FoundationVerificationTest.java:41`. | closed |
| T-04-03 | Tampering | README/local run commands | mitigate | README commands are explicit and local-only, include test/verify commands, and do not instruct disabling tests or bypassing tenant isolation. Evidence: `README.md:30`, `README.md:36`, `README.md:42`. | closed |

Status: open or closed.
Disposition: mitigate (implementation required), accept (documented risk), or transfer (third-party).

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01-01 | T-01-02 | Phase 1 is local-only through Docker Compose and does not claim production exposure, public traffic handling, or rate limiting. This risk must be revisited before any production or shared-environment deployment. | Plan disposition in `01-01-PLAN.md` | 2026-05-11 |

Accepted risks do not resurface in future audit runs unless the phase scope changes.

---

## Unregistered Flags

No `## Threat Flags` sections were present in the Phase 1 summary artifacts.

---

## Security Audit 2026-05-11

| Metric | Count |
|--------|-------|
| Threats found | 12 |
| Closed | 12 |
| Open | 0 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-05-11 | 12 | 12 | 0 | Codex `gsd-secure-phase` |

Verification command: `cd backend-spring && ./mvnw test`

Result: 19 tests run, 0 failures, 0 errors, 0 skipped.

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

Approval: verified 2026-05-11
