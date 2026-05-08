# Phase 1: Backend Foundation - Context

**Gathered:** 2026-05-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 1 delivers the backend-only operational foundation for SupportFlow AI. It establishes the Spring Boot backend, MongoDB connectivity, reproducible local Docker Compose setup, tenant workspace create/list/detail APIs, tenant-scoped ticket create/list/detail APIs, explicit ticket lifecycle validation, tenant isolation tests, backend API documentation, and automated unit/integration coverage.

This phase does not include frontend UI, authentication, AI service integration, Redis-backed async behavior, knowledge base workflows, draft review workflows, metrics, tenant metadata update workflows, or SaaS expansion features.

</domain>

<decisions>
## Implementation Decisions

### Tenant Identity and Tenant APIs
- **D-01:** Phase 1 tenant-scoped APIs must identify tenants through URL paths, not request headers. Use route shapes like `/api/v1/tenants/{tenantId}/tickets`.
- **D-02:** Tenant creation requires `name`, `slug`, and optional `description`. The backend generates the tenant ID and defaults tenant status to `ACTIVE`.
- **D-03:** Phase 1 exposes tenant create, list, and detail APIs: `POST /api/v1/tenants`, `GET /api/v1/tenants`, and `GET /api/v1/tenants/{tenantId}`.
- **D-04:** Tenant update is out of Phase 1 and remains Phase 2 scope.

### Ticket Lifecycle
- **D-05:** Ticket lifecycle states are `NEW`, `TRIAGED`, `IN_PROGRESS`, `ANSWERED`, and `CLOSED`.
- **D-06:** Phase 1 enforces a practical support loop: forward transitions are allowed, and `ANSWERED -> IN_PROGRESS` is allowed for rework.
- **D-07:** Reopening closed tickets is out of Phase 1. Do not allow `CLOSED -> IN_PROGRESS` or other reopen flows in this phase.

### Ticket Shape and Listing
- **D-08:** Ticket creation requires `subject`, `customerName`, `customerEmail`, and `customerMessage`.
- **D-09:** `category` and `priority` are not required at ticket creation in Phase 1.
- **D-10:** Ticket detail responses expose core fields plus nullable `category`, `priority`, and `assigneeId` placeholders.
- **D-11:** Full operational enrichment, urgency, sentiment, SLA-risk fields, and workflow history are out of Phase 1 unless needed internally for lifecycle validation.
- **D-12:** Ticket listing supports filters for `status`, `priority`, `assigneeId`, and created date so `TICK-02` is fully covered in Phase 1.

### Documentation and Verification
- **D-13:** Phase 1 API documentation uses Springdoc OpenAPI with generated `/v3/api-docs` and Swagger UI.
- **D-14:** Tenant isolation proof should use full HTTP API integration tests against Spring Boot with a MongoDB Testcontainer.
- **D-15:** Unit tests should still cover pure domain lifecycle rules, but cross-tenant read/write denial must be verified through HTTP endpoint tests.

### the agent's Discretion
- Exact Java package names, controller/service/repository layering, DTO naming, MongoDB collection names, validation annotation details, and test class organization are left to the researcher and planner, provided they preserve the locked API behavior and tenant isolation decisions above.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Scope and Requirements
- `.planning/PROJECT.md` - Defines backend-first sequencing, architectural constraints, tenant isolation expectations, and out-of-scope boundaries.
- `.planning/REQUIREMENTS.md` - Defines Phase 1 requirement IDs: `TEN-01`, `TICK-01`, `TICK-02`, `TICK-03`, `TICK-05`, `FLOW-01`, `FLOW-02`, `ISO-01`, `ISO-02`, `ISO-04`, `QUAL-01`, `QUAL-02`, `QUAL-03`, and `QUAL-06`.
- `.planning/ROADMAP.md` - Defines Phase 1 goal, success criteria, MVP mode, and planned plan breakdown.
- `.planning/STATE.md` - Records current project focus and initialization decisions.

### Project Research
- `.planning/research/SUMMARY.md` - Summarizes stack, architecture, roadmap implications, and critical pitfalls.
- `.planning/research/STACK.md` - Recommends Spring Boot 3.5.13, MongoDB, Springdoc OpenAPI, JUnit, Mockito, and Testcontainers.
- `.planning/research/ARCHITECTURE.md` - Defines split backend/AI architecture, tenant-scoped service boundary, explicit workflow state machine, and anti-patterns.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None yet. The repository currently contains placeholder service directories and planning artifacts, not implemented backend code.

### Established Patterns
- No application-level code patterns exist yet. Phase 1 should establish the initial Spring Boot project structure and backend testing conventions.

### Integration Points
- `backend-spring/` is the target for the Spring Boot operational backend.
- `docker-compose.yml` exists and should become the local reproducibility anchor for MongoDB and backend execution.
- `harness/api-tests/` exists as a placeholder for later API verification harnesses, but Phase 1 integration tests can live in the Spring test suite unless the planner decides a harness is needed.

</code_context>

<specifics>
## Specific Ideas

- Use explicit tenant-scoped URL paths before authentication exists to make tenant isolation visible and testable.
- Keep Phase 1 tenant administration intentionally narrow: create, list, and detail only.
- Preserve Phase 2 room for richer tenant metadata updates, workflow history, and operational assignment behavior.
- Use HTTP-level integration tests with Testcontainers as the main evidence for tenant isolation.

</specifics>

<deferred>
## Deferred Ideas

- Tenant update workflows remain Phase 2.
- Full ticket workflow history remains Phase 2.
- Ticket urgency, sentiment, SLA-risk fields, and richer prioritization behavior remain Phase 2 or later.
- Authentication and role enforcement remain post-v1 work unless roadmap changes.
- Frontend UI remains post-v1 work.

</deferred>

---

*Phase: 1-Backend Foundation*
*Context gathered: 2026-05-08*
