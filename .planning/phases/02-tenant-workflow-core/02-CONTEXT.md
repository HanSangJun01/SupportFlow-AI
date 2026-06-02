# Phase 2: Tenant Workflow Core - Context

**Gathered:** 2026-05-13
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 2 expands the backend-only operational core with tenant metadata update workflows, tenant-local operational user and role metadata, ticket ownership and prioritization fields, immutable ticket workflow history, and expanded tenant-isolation coverage.

This phase does not add frontend UI, authentication, login credentials, full RBAC, AI service integration, SLA automation, notifications, escalation scheduling, tenant self-registration, or response drafting.

</domain>

<decisions>
## Implementation Decisions

### Tenant Metadata Updates
- **D-01:** Phase 2 allows super admins to update tenant `name`, `description`, and `status`.
- **D-02:** Tenant `slug` remains stable after creation. Do not add slug update behavior in Phase 2.
- **D-03:** Tenant status is limited to `ACTIVE` and `INACTIVE`.
- **D-04:** Phase 2 does not add separate tenant metadata history. Tenant metadata updates should maintain `updatedAt`.
- **D-05:** `INACTIVE` tenants still allow tenant-scoped ticket reads such as list/detail, but block tenant-scoped mutations such as ticket create, status update, workflow metadata update, and ownership changes. Super admin tenant list/detail/update remains available so inactive tenants can be inspected and reactivated.

### Operational Users And Roles
- **D-06:** Phase 2 adds tenant-local operational user profiles for assignment and history attribution only. These are not authentication accounts and must not include passwords, sessions, login flows, or security enforcement.
- **D-07:** Operational roles are `TENANT_ADMIN` and `SUPPORT_AGENT`.
- **D-08:** Operational user status is limited to `ACTIVE` and `INACTIVE`.
- **D-09:** Active users can appear as workflow actors. Inactive users remain visible for historical references but cannot be newly assigned to tickets.
- **D-10:** Ticket assignee and workflow actor references must validate that the referenced operational user exists in the same tenant.

### Ticket Ownership And Prioritization
- **D-11:** Phase 2 allows direct updates to ticket `assignee`, `priority`, and `category`.
- **D-12:** Do not add direct user-editable `urgency`, `sentiment`, or SLA-risk fields in Phase 2. Those remain for later AI classification, prioritization enrichment, or metrics work unless the planner needs read-only placeholders already present in existing models.
- **D-13:** Use one workflow metadata patch endpoint for assignee/priority/category changes, with actor attribution in the request.
- **D-14:** Assignment is only allowed to active `SUPPORT_AGENT` users in the same tenant.
- **D-15:** Operational metadata edits are rejected when the ticket is `CLOSED`.

### Workflow History Shape
- **D-16:** Immutable ticket history records status changes and workflow metadata changes.
- **D-17:** Each history event includes event type, actor, timestamp, changed fields, and old/new values.
- **D-18:** Workflow operations supply explicit `actorUserId` in the request body before authentication exists.
- **D-19:** `actorUserId` is validated against tenant-local operational users in the same tenant.
- **D-20:** Ticket detail responses embed chronological history entries for the same tenant.

### the agent's Discretion
- Exact Java type names, DTO names, enum names, endpoint path suffixes, validation annotation details, MongoDB document structure for embedded history, repository query names, and test class organization are left to the researcher and planner.
- The planner may choose the precise patch route name, such as `/api/v1/tenants/{tenantId}/tickets/{ticketId}/workflow`, as long as lifecycle status transitions remain separate from workflow metadata updates.
- The planner may decide whether operational user APIs are full CRUD or a narrower create/list/detail/update-status surface, provided Phase 2 supports same-tenant validation for actor and assignee references.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Scope and Requirements
- `.planning/PROJECT.md` - Defines backend-first sequencing, tenant isolation as a core correctness rule, local-first reproducibility, REST-first APIs, and out-of-scope boundaries.
- `.planning/REQUIREMENTS.md` - Defines Phase 2 requirement IDs: `TEN-02`, `TEN-03`, `TEN-04`, `TICK-04`, `FLOW-03`, `FLOW-04`, `FLOW-05`, and `ISO-03`.
- `.planning/ROADMAP.md` - Defines Phase 2 goal, success criteria, dependency on Phase 1, and planned plan breakdown.
- `.planning/STATE.md` - Records current project focus and prior phase decisions affecting Phase 2.

### Prior Phase Context
- `.planning/phases/01-backend-foundation/01-CONTEXT.md` - Locks tenant URL identity, Phase 1 ticket lifecycle states, tenant isolation verification expectations, and Phase 2 deferred items.
- `docs/sdd/phase-01-backend-foundation-api.md` - Captures the existing Phase 1 API contract and explicitly lists deferred Phase 2 items.

### Existing Backend Code
- `backend-spring/src/main/java/com/supportflow/tenant/Tenant.java` - Existing tenant model with `name`, `slug`, `description`, `status`, `createdAt`, and `updatedAt`.
- `backend-spring/src/main/java/com/supportflow/tenant/TenantController.java` - Existing tenant create/list/detail API surface to extend with safe updates.
- `backend-spring/src/main/java/com/supportflow/ticket/Ticket.java` - Existing ticket model with tenant ID, status, category, priority, assignee ID, and timestamps.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketController.java` - Existing tenant-scoped ticket APIs and status update endpoint.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketStatusTransitionPolicy.java` - Existing lifecycle transition policy that Phase 2 history should record, not replace.
- `backend-spring/src/test/java/com/supportflow/ticket/TenantIsolationIntegrationTest.java` - Existing HTTP-level tenant isolation pattern to expand for Phase 2 workflow behavior.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `TenantService` and `TenantController`: already provide tenant create/list/detail and can be extended for name/description/status update behavior.
- `TenantStatus`: already exists and should remain the status enum anchor for `ACTIVE` / `INACTIVE` behavior.
- `TicketService`, `TicketController`, and `TicketRepository`: already enforce tenant-scoped ticket access through `tenantId` path parameters and `findByTenantIdAndId`.
- `TicketStatusTransitionPolicy`: already owns lifecycle transition validation and should remain the lifecycle rule source.
- Existing MockMvc and Testcontainers tests: provide the verification style for API contracts, tenant isolation, and Mongo-backed persistence behavior.

### Established Patterns
- REST endpoints are versioned under `/api/v1`.
- Tenant-scoped ticket APIs use `/api/v1/tenants/{tenantId}/tickets`.
- Missing or cross-tenant ticket access returns 404 rather than exposing resource ownership.
- Domain objects are Mongo documents with Java enums and explicit timestamps.
- HTTP-level integration tests are required for tenant-isolation proof; pure unit tests are still useful for domain rules.

### Integration Points
- Add tenant metadata update behavior under the existing tenant API surface.
- Add operational users under a tenant-scoped route, likely `/api/v1/tenants/{tenantId}/users` or equivalent.
- Add ticket workflow metadata updates under the existing tenant-scoped ticket route without merging them into the status transition endpoint.
- Embed or otherwise expose ticket history through ticket detail responses while preserving tenant-scoped lookup semantics.

</code_context>

<specifics>
## Specific Ideas

- Keep tenant slug immutable so existing URL identity and uniqueness assumptions stay stable.
- Treat operational users as assignment and audit metadata only until authentication work begins after v1.
- Keep lifecycle status changes and workflow metadata changes distinct, but record both in the same immutable ticket history.
- Preserve readable inactive-tenant data for investigation while preventing new operational mutations.

</specifics>

<deferred>
## Deferred Ideas

- Tenant slug update workflows are deferred.
- Full tenant metadata change history is deferred.
- Authentication accounts, passwords, sessions, and real RBAC enforcement are deferred to post-v1 authentication work.
- Direct user-editable urgency, sentiment, and SLA-risk fields are deferred to later AI classification, prioritization, or metrics phases.
- SLA policy automation, escalation rules, notifications, and scheduling are deferred.
- Separate ticket history endpoint is deferred unless planning discovers an implementation need.

</deferred>

---

*Phase: 2-Tenant Workflow Core*
*Context gathered: 2026-05-13*
