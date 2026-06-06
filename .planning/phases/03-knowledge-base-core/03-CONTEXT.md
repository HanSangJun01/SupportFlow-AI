# Phase 3: Knowledge Base Core - Context

**Gathered:** 2026-06-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 3 adds backend-only tenant-scoped knowledge document management for FAQ and policy content. It delivers registration, list/detail retrieval, update, archive/restore behavior, tenant isolation, retrieval-ready metadata, API documentation, and automated verification.

This phase does not add AI classification, embeddings, vector search, RAG evidence retrieval, draft generation, frontend UI, authentication, full RBAC, destructive deletion, or text-search behavior.

</domain>

<decisions>
## Implementation Decisions

### Document Shape
- **D-01:** Knowledge document type is a validated enum with `FAQ` and `POLICY`.
- **D-02:** FAQ and policy documents share `title` and `content` fields rather than separate FAQ-specific and policy-specific content shapes.
- **D-03:** Knowledge document status is limited to `ACTIVE` and `ARCHIVED`.
- **D-04:** Knowledge document mutations require explicit `actorUserId` before authentication exists.
- **D-05:** `actorUserId` must validate against an active tenant-local operational user in the same tenant, reusing the Phase 2 actor validation concept.

### Update Semantics
- **D-06:** Updates overwrite the current document fields and refresh audit metadata. Phase 3 does not create immutable revisions.
- **D-07:** Knowledge documents track `createdByUserId` and `updatedByUserId`.
- **D-08:** Archived documents allow metadata and status updates, including restore, but reject content updates while archived.
- **D-09:** List responses exclude archived documents by default and support an optional status filter.

### Retrieval-Ready Metadata
- **D-10:** Knowledge documents require `sourceLabel` and allow optional `sourceUrl`.
- **D-11:** Knowledge documents support an optional normalized tags list.
- **D-12:** Knowledge documents support optional `effectiveFrom` and `effectiveTo` timestamps for policy traceability.
- **D-13:** The backend computes and stores `contentHash`; clients do not provide it.

### API Surface
- **D-14:** Tenant-scoped knowledge APIs use `/api/v1/tenants/{tenantId}/knowledge-documents`.
- **D-15:** Phase 3 exposes create, list, detail, update, archive, and restore operations.
- **D-16:** List filtering supports exact filters for `type`, `status`, `tag`, and active date window.
- **D-17:** Phase 3 does not add text search, Mongo text indexes, keyword contains search, embeddings, or RAG behavior. Retrieval semantics remain Phase 5 scope.

### the agent's Discretion
- Exact Java type names, DTO names, enum names, repository method names, validation annotation details, MongoDB collection name, route method names, and test class organization are left to the researcher and planner.
- The planner may choose whether archive/restore are separate endpoints or represented through a status update endpoint, provided deletion is not introduced and the API contract remains clear.
- The planner may choose the exact active date window filter names as long as they are documented and testable.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Scope and Requirements
- `.planning/PROJECT.md` - Defines backend-first sequencing, REST-first APIs, tenant isolation as a correctness rule, local-first reproducibility, and out-of-scope boundaries.
- `.planning/REQUIREMENTS.md` - Defines Phase 3 requirement IDs: `KNOW-01`, `KNOW-02`, `KNOW-03`, and `KNOW-04`.
- `.planning/ROADMAP.md` - Defines Phase 3 goal, success criteria, dependency on Phase 2, and planned plan breakdown.
- `.planning/STATE.md` - Records current project focus and accumulated decisions.

### Prior Phase Context
- `.planning/phases/01-backend-foundation/01-CONTEXT.md` - Locks tenant-scoped URL identity, REST-first backend foundation, and tenant isolation verification expectations.
- `.planning/phases/02-tenant-workflow-core/02-CONTEXT.md` - Locks tenant-local operational users, explicit actor attribution, inactive tenant mutation behavior, and Phase 2 workflow boundaries.
- `docs/sdd/phase-02-tenant-workflow-core-api.md` - Captures the current actor-attributed ticket and operational user API contract that Phase 3 should align with.

### Existing Backend Code
- `backend-spring/src/main/java/com/supportflow/tenant/TenantService.java` - Provides `getTenant` and `requireActiveTenant` patterns for read versus mutation behavior.
- `backend-spring/src/main/java/com/supportflow/user/OperationalUserService.java` - Provides active same-tenant actor validation helpers for `actorUserId`.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketController.java` - Shows established tenant-scoped REST route, DTO, validation, and response record style.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java` - Shows tenant-aware service behavior, active-tenant mutation guards, and explicit actor validation.
- `backend-spring/src/main/java/com/supportflow/ticket/Ticket.java` - Shows Mongo document style with indexed `tenantId`, timestamps, and enum fields.
- `backend-spring/src/test/java/com/supportflow/ticket/TenantWorkflowMongoIntegrationTest.java` - Shows Docker-backed Mongo integration coverage for active/inactive tenant behavior and cross-tenant denial.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `TenantService`: use `getTenant` for read/list/detail operations and `requireActiveTenant` for create/update/archive/restore mutations.
- `OperationalUserService`: reuse active same-tenant actor validation for knowledge document mutation requests.
- `GlobalExceptionHandler` and existing controller DTO style: keep validation and error behavior consistent with tenant, user, and ticket APIs.
- Existing Testcontainers Mongo tests: reuse the HTTP-level integration style to prove knowledge document tenant isolation and inactive-tenant mutation denial.
- Existing OpenAPI documentation test: expand route assertions so knowledge document APIs remain documented.

### Established Patterns
- REST endpoints are versioned under `/api/v1`.
- Tenant-scoped resources use path-based tenant identity: `/api/v1/tenants/{tenantId}/...`.
- Tenant-scoped reads call `getTenant`; tenant-scoped mutations call `requireActiveTenant`.
- Cross-tenant resource access should resolve as not found rather than leaking ownership.
- Mongo documents carry `tenantId`, timestamps, and enums with service-layer validation.
- Backend verification combines unit tests for rules with HTTP integration tests for tenant isolation.

### Integration Points
- Add a new knowledge package under the Spring backend, likely alongside `tenant`, `ticket`, and `user`.
- Connect knowledge document actor validation to Phase 2 operational users.
- Add OpenAPI-visible controller routes under `/api/v1/tenants/{tenantId}/knowledge-documents`.
- Add a Phase 3 SDD or API contract document before or during implementation so later Phase 5 retrieval planning has a stable source contract.

</code_context>

<specifics>
## Specific Ideas

- Treat Phase 3 as document management, not retrieval. Exact metadata filters are acceptable; search and relevance ranking are not.
- Use archive/restore instead of deletion so later evidence references can remain traceable.
- Store `contentHash` as a lightweight future-proofing mechanism without introducing revision history.
- Keep FAQ and policy documents on one model so Phase 5 retrieval can consume a consistent document contract.

</specifics>

<deferred>
## Deferred Ideas

- Immutable document revisions and full update history are deferred.
- Text search, keyword contains search, Mongo text indexes, embeddings, vector search, and RAG retrieval are deferred to later retrieval phases.
- AI classification, draft generation, and response approval remain Phase 4 through Phase 6 scope.
- Frontend knowledge management UI remains post-v1 UI scope.
- Authentication and full RBAC remain post-v1 authentication scope.
- Destructive deletion is not part of Phase 3.

</deferred>

---

*Phase: 3-Knowledge Base Core*
*Context gathered: 2026-06-04*
