# Phase 2: Tenant Workflow Core - Pattern Map

**Date:** 2026-05-13
**Status:** Complete

## Purpose

Map Phase 2 planned files to the closest existing Phase 1 analogs so execution can reuse local conventions.

## Existing Patterns To Reuse

| New/Modified Area | Closest Existing Analog | Pattern To Preserve |
|-------------------|-------------------------|---------------------|
| Tenant metadata update | `TenantService`, `TenantController`, `TenantApiIntegrationTest` | Controller records nested in controller, service owns validation and repository calls, missing tenant returns 404 |
| Operational users | `Tenant`/`TenantRepository`/`TenantService` and `Ticket` tenant-scoped routes | Mongo document with `tenantId`, repository methods constrained by tenant, service validation before save |
| Ticket workflow update | `TicketService.updateStatus`, `TicketController.updateStatus` | Tenant-scoped path, request record, service fetches by `tenantId` and `ticketId`, invalid workflow returns 400 |
| Ticket history | `Ticket` timestamps and status policy tests | Keep state changes in service methods; test pure domain policy separately from HTTP integration |
| OpenAPI docs | `OpenApiConfig`, `OpenApiDocumentationTest` | Shared OpenAPI metadata plus route assertions through `/v3/api-docs` |
| Mongo integration | `TenantIsolationMongoIntegrationTest` | `@SpringBootTest`, `TestRestTemplate`, `MongoDBContainer("mongo:7")`, cross-tenant checks return 404 or empty lists |

## File-Level Guidance

### `backend-spring/src/main/java/com/supportflow/tenant/TenantStatus.java`
- Existing enum only has `ACTIVE`.
- Add `INACTIVE`.
- Do not rename `ACTIVE`, because Phase 1 tests and responses assert it.

### `backend-spring/src/main/java/com/supportflow/tenant/TenantService.java`
- Existing methods throw `ResponseStatusException`.
- Add update command and active-tenant guard here rather than duplicating status checks in controllers.
- Preserve `getTenant` as the read helper so inactive tenant reads remain possible.

### `backend-spring/src/main/java/com/supportflow/tenant/TenantController.java`
- Existing controller uses Java records for request and response types.
- Add a `PatchMapping("/{tenantId}")` update route with a request record containing `name`, `description`, and `status`.
- Response should continue to use `TenantResponse.from(tenant)`.

### Operational User Package
- Create a package such as `com.supportflow.user` or `com.supportflow.operationaluser`.
- Use the same Spring stereotypes and record style already used in tenant/ticket controllers.
- Every repository lookup that touches a user from a tenant route should include `tenantId`.

### `backend-spring/src/main/java/com/supportflow/ticket/Ticket.java`
- Add history as an initialized list so new tickets can append events without null checks.
- Keep existing response fields stable and add `history` to the response record.

### `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java`
- Preserve `updateStatus` transition-policy validation.
- Change status command shape to include `actorUserId`.
- Add a workflow metadata command with `actorUserId`, `assigneeId`, `priority`, and `category`.
- Call operational-user validation from service methods, not controller methods.

### Tests
- Unit tests should cover pure service validation and history append behavior.
- MockMvc tests should cover request/response contracts.
- Testcontainers integration should prove tenant isolation and inactive-tenant behavior through real HTTP calls.

## PATTERN MAPPING COMPLETE
