# Phase 2: Tenant Workflow Core - Research

**Date:** 2026-05-13
**Status:** Complete

## Research Question

What does the planner need to know to implement Phase 2 safely on top of the Phase 1 Spring Boot and MongoDB backend?

## Phase Scope Summary

Phase 2 extends the existing backend-only workflow core. The work stays inside the Spring Boot service and MongoDB persistence model. It adds tenant metadata updates, tenant-local operational users, workflow actor validation, ticket ownership/category/priority updates, immutable ticket history, inactive-tenant mutation blocking, and integration coverage that proves tenant isolation across the expanded model.

## Existing Architecture Findings

### Tenant Administration

- `Tenant` already stores `name`, `slug`, `description`, `status`, `createdAt`, and `updatedAt`.
- `TenantStatus` currently only contains `ACTIVE`; Phase 2 needs to add `INACTIVE`.
- `TenantService` owns tenant creation, list, detail, and existence checks.
- `TenantController` exposes `POST /api/v1/tenants`, `GET /api/v1/tenants`, and `GET /api/v1/tenants/{tenantId}`.
- Tenant slug uniqueness is enforced in service logic and by Mongo auto-index creation. Phase 2 should keep slug immutable and avoid update-time uniqueness edge cases.

### Ticket Workflow

- Tenant-scoped ticket routes already use `/api/v1/tenants/{tenantId}/tickets`.
- `Ticket` already stores `tenantId`, `status`, `category`, `priority`, and `assigneeId`.
- `TicketStatusTransitionPolicy` is the lifecycle rule source. Phase 2 should record status-transition history without replacing this policy.
- `TicketService.getTicket(tenantId, ticketId)` uses `findByTenantIdAndId`, which preserves cross-tenant non-disclosure by returning 404 for cross-tenant access.
- `TicketService.createTicket`, `updateStatus`, and the new workflow-metadata update path need an active-tenant guard. Ticket list/detail should continue working for inactive tenants.

### Tests And Verification

- Controller-level tests use `@WebMvcTest`, `MockMvc`, and `@MockitoBean`.
- Service tests use JUnit Jupiter, Mockito, and AssertJ.
- Mongo-backed tenant isolation coverage uses `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `TestRestTemplate`, `MongoDBContainer("mongo:7")`, and `@Testcontainers(disabledWithoutDocker = true)`.
- Full verification uses `cd backend-spring && ./mvnw verify`.
- Existing OpenAPI checks assert `/v3/api-docs` contains route paths. Phase 2 should extend this style for new/update routes.

## Recommended Implementation Approach

### 1. Tenant Update And Inactive-Tenant Guard

Add a tenant update command that accepts only `name`, `description`, and `status`. Keep `slug` out of the update request entirely. Add service-level helpers with distinct semantics:

- `getTenant(tenantId)` for read paths.
- `requireActiveTenant(tenantId)` or equivalent for tenant-scoped mutation paths.

This distinction implements the context decision that inactive tenants remain readable but reject ticket create/status/workflow mutations.

### 2. Tenant-Local Operational Users

Add an `operational-user` package or a `user` package under `com.supportflow`. The package should contain:

- `OperationalUser` Mongo document with `id`, `tenantId`, `displayName`, `email`, `role`, `status`, `createdAt`, and `updatedAt`.
- `OperationalUserRole` enum values `TENANT_ADMIN` and `SUPPORT_AGENT`.
- `OperationalUserStatus` enum values `ACTIVE` and `INACTIVE`.
- Repository queries constrained by tenant: `findByTenantId`, `findByTenantIdAndId`, and any email uniqueness query if chosen.
- Service methods for create/list/detail/status update and validation helpers used by ticket workflow:
  - validate active actor exists in same tenant.
  - validate assignee is an active support agent in same tenant.

Avoid adding credentials, passwords, sessions, Spring Security, or RBAC enforcement. These users are operational metadata only.

### 3. Ticket Workflow Metadata And History

Add embedded immutable history entries to `Ticket`. A simple Mongo-friendly shape is enough:

- `TicketHistoryEntry`
  - `eventType`
  - `actorUserId`
  - `occurredAt`
  - `changes`
- `TicketFieldChange`
  - `field`
  - `oldValue`
  - `newValue`

Event types should include at least `STATUS_CHANGED` and `WORKFLOW_METADATA_CHANGED`.

Update status operations to require `actorUserId` in the request body. Add a separate workflow metadata patch endpoint for `assigneeId`, `priority`, and `category`. Do not merge these fields into the status endpoint.

Closed tickets should reject workflow metadata edits. Inactive tenants should reject status changes and workflow metadata edits. Read paths still return ticket detail and history.

### 4. API And Documentation

Phase 2 should document and test these new routes:

- `PATCH /api/v1/tenants/{tenantId}` for tenant metadata/status updates.
- Tenant-local operational user routes under `/api/v1/tenants/{tenantId}/users` or a close equivalent.
- `PATCH /api/v1/tenants/{tenantId}/tickets/{ticketId}/workflow` or a close equivalent for assignee/priority/category updates.
- Existing `PATCH /status` request body extended to include `actorUserId`.

Create `docs/sdd/phase-02-tenant-workflow-core-api.md` to keep the Phase 2 contract separate from the Phase 1 contract.

## Risks And Mitigations

| Risk | Why It Matters | Mitigation |
|------|----------------|------------|
| Inactive-tenant behavior becomes inconsistent | Some tenant-scoped routes should remain readable while mutations are blocked | Centralize mutation guard in `TenantService` and test read vs mutation behavior |
| Actor IDs become meaningless strings | Phase 2 requires actor-attributed history | Validate actor IDs against active same-tenant operational users |
| Assignment crosses tenants or roles | Ownership fields are tenant-sensitive | Validate assignee is an active `SUPPORT_AGENT` in the same tenant |
| History is mutable or incomplete | Workflow traceability is a core Phase 2 success criterion | Append entries inside service methods; expose chronological entries in ticket detail |
| SLA/AI fields creep into Phase 2 | FLOW-05 excludes full SLA automation | Limit direct updates to assignee, priority, and category; defer urgency, sentiment, and SLA-risk |

## Validation Architecture

Phase 2 should use the existing Spring/JUnit validation stack.

### Automated Commands

- Quick controller/service slice checks: `cd backend-spring && ./mvnw test -Dtest=TenantApiIntegrationTest,OperationalUserApiIntegrationTest,TicketApiIntegrationTest,TicketServiceTest,OperationalUserServiceTest,TenantServiceTest`
- Mongo-backed isolation checks: `cd backend-spring && ./mvnw test -Dtest=TenantWorkflowMongoIntegrationTest`
- Full phase verification: `cd backend-spring && ./mvnw verify`
- Documentation route check: `cd backend-spring && ./mvnw test -Dtest=OpenApiDocumentationTest,FoundationVerificationTest`

### Required Test Evidence

- Tenant update changes `name`, `description`, `status`, and `updatedAt`; slug remains unchanged.
- Inactive tenant permits ticket list/detail reads and rejects ticket create, status update, and workflow metadata update.
- Operational users are tenant-scoped and support `TENANT_ADMIN`, `SUPPORT_AGENT`, `ACTIVE`, and `INACTIVE`.
- Ticket assignment rejects missing users, cross-tenant users, inactive users, and non-`SUPPORT_AGENT` users.
- Status changes and workflow metadata updates append immutable history entries with event type, actor, timestamp, field, old value, and new value.
- Cross-tenant actor/assignee attempts are rejected without leaking another tenant's resources.

## Planning Recommendations

Use the roadmap's three-plan split:

1. Tenant metadata updates plus operational users.
2. Ticket workflow metadata updates plus immutable history.
3. Integration, OpenAPI, docs, and cross-tenant verification expansion.

This keeps Phase 2 executable in sequence while preserving vertical backend slices: tenant/admin metadata first, ticket workflow behavior second, verification and documentation last.

## RESEARCH COMPLETE
