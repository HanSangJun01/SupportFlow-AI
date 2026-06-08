# Phase 4: AI Classification Integration - Context

**Gathered:** 2026-06-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 4 introduces the separate FastAPI AI service and connects it to the Spring backend ticket workflow for tenant-scoped ticket classification. It delivers a defined inter-service classification contract, deterministic local classification behavior, backend invocation on ticket creation and manual re-analysis, append-only classification artifacts, automatic category and priority application from successful classifications, workflow traceability, API documentation, Docker Compose wiring, and automated verification.

This phase does not add evidence retrieval, embeddings, vector search, draft response generation, frontend UI, authentication, full RBAC, provider-backed LLM calls, async job orchestration, Redis-backed background processing, or human-readable AI rationale.

</domain>

<decisions>
## Implementation Decisions

### Classification Contract
- **D-01:** The AI service returns operational classification fields only: `category`, `urgency`, `sentiment`, `priority`, and `confidence`.
- **D-02:** `urgency`, `sentiment`, and `priority` use small fixed enums. `category` is a simple string.
- **D-03:** `confidence` is required and represented as a numeric `0.0` to `1.0` value.
- **D-04:** Phase 4 does not include a human-readable rationale or explanation field.
- **D-05:** The AI service response includes a classifier or model version, and the backend stores that version on classification attempts.

### Invocation Flow
- **D-06:** Classification is triggered both automatically on ticket creation and manually through a backend re-analysis endpoint.
- **D-07:** Ticket creation runs classification synchronously in Phase 4 so the create response can include the resulting successful or failed classification artifact.
- **D-08:** If automatic classification is unavailable or times out, ticket creation still succeeds and a failed classification attempt is stored.
- **D-09:** Manual re-analysis is allowed for existing tickets.
- **D-10:** Classification and re-analysis are allowed for any non-closed ticket. Closed tickets must reject classification mutations.
- **D-11:** Manual re-analysis requires `actorUserId` because it is an operational action and must remain attributable before authentication exists.
- **D-12:** Automatic ticket-creation classification has no human actor. Use trigger metadata such as `AUTO_ON_CREATE`; do not invent a fake system user.

### Artifact Storage And Traceability
- **D-13:** Classification attempts are stored as append-only history attached to the ticket.
- **D-14:** Ticket detail responses embed classification artifacts so operational consumers can inspect them alongside ticket data.
- **D-15:** Each classification attempt stores attempt id, status, trigger, actor if manual, timestamps, result fields on success, classifier version, and error code/message on failure.
- **D-16:** Successful AI predictions automatically apply predicted `category` and `priority` to the ticket.
- **D-17:** Failed classification attempts do not change ticket `category` or `priority`.
- **D-18:** Manual re-analysis creates a new classification attempt and updates ticket `category` and `priority` again if successful.
- **D-19:** Successful AI-applied ticket updates record a workflow history entry showing that category/priority were applied by AI classification.
- **D-20:** The AI-applied workflow history entry links to the related `classificationAttemptId`.
- **D-21:** Use a new AI classification history event type rather than reusing human workflow metadata change events.
- **D-22:** Failed classification attempts are recorded in classification artifacts only. Workflow history remains for applied workflow changes.

### Local AI Behavior
- **D-23:** The FastAPI AI service uses a deterministic rule-based classifier in Phase 4.
- **D-24:** The deterministic classifier maps ticket subject and customer message through transparent keyword rules.
- **D-25:** Confidence uses rule-derived bands so strong keyword matches and fallback classifications are distinguishable.
- **D-26:** Phase 4 does not require provider keys, external LLM calls, network-backed inference, or nondeterministic AI behavior.

### Failure And Timeout Behavior
- **D-27:** Manual re-analysis failures return a structured failure to the caller, store a failed classification attempt, and leave the ticket unchanged.
- **D-28:** The backend uses a short local AI service timeout, approximately 2 seconds by default.
- **D-29:** Phase 4 does not introduce async retry jobs. Synchronous failures should be explicit and testable.

### the agent's Discretion
- Exact FastAPI package layout, Python dependency manager, DTO names, Java client class names, Mongo document field names, enum names, endpoint suffixes, timeout property names, and test class organization are left to the researcher and planner.
- The planner may choose the exact fixed enum values for `urgency` and `sentiment`, provided they are small, documented, and contract-tested.
- The planner may choose whether classification attempts are embedded directly in `Ticket` or modeled as a separate tenant-scoped collection, provided ticket detail embeds the artifacts and tenant isolation is proven.
- The planner may choose the exact manual re-analysis route, provided it is tenant-scoped under the ticket API and requires `actorUserId`.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Scope and Requirements
- `.planning/PROJECT.md` - Defines backend-first sequencing, separate Spring and FastAPI services, local-first reproducibility, REST-first APIs, tenant isolation, traceability, and out-of-scope boundaries.
- `.planning/REQUIREMENTS.md` - Defines Phase 4 requirement IDs: `AI-01`, `AI-02`, `AI-03`, `AI-04`, and `QUAL-04`.
- `.planning/ROADMAP.md` - Defines Phase 4 goal, success criteria, dependency on Phase 3, and planned plan breakdown.
- `.planning/STATE.md` - Records current project focus and accumulated decisions.

### Prior Phase Context
- `.planning/phases/01-backend-foundation/01-CONTEXT.md` - Locks tenant-scoped URL identity, REST-first backend foundation, tenant isolation verification, and backend-only v1 discipline.
- `.planning/phases/02-tenant-workflow-core/02-CONTEXT.md` - Locks operational users, explicit `actorUserId`, workflow metadata rules, closed-ticket mutation rejection, and immutable ticket history.
- `.planning/phases/03-knowledge-base-core/03-CONTEXT.md` - Locks backend-only tenant-scoped API patterns and defers AI classification, retrieval, and drafts to later phases.

### Existing Backend Code
- `backend-spring/src/main/java/com/supportflow/ticket/Ticket.java` - Current ticket document includes `tenantId`, subject/message content, `category`, `priority`, `assigneeId`, timestamps, and embedded history.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java` - Current create, detail, status, and workflow metadata mutation rules; Phase 4 should connect classification here without weakening tenant or closed-ticket guards.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketController.java` - Current tenant-scoped ticket REST surface and response shape that Phase 4 should extend with classification artifacts.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketHistoryEntry.java` - Existing embedded workflow history structure to extend or align with AI-applied history events.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketHistoryEventType.java` - Existing history event enum that needs a distinct AI classification event type.
- `docker-compose.yml` - Currently defines MongoDB, backend, and Redis; Phase 4 should add the FastAPI AI service wiring without making Redis required for classification flow.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `TicketService.createTicket`: the automatic classification trigger should integrate after ticket creation data is available while preserving active-tenant and assignee validation.
- `TicketService.updateWorkflowMetadata`: existing category/priority mutation and field-change recording logic is the closest pattern for AI-applied category/priority updates, but AI updates need a distinct history event and classification attempt link.
- `OperationalUserService`: reuse active same-tenant actor validation for manual re-analysis `actorUserId`.
- Existing tenant-scoped repository and controller patterns: classification attempts must remain constrained by `tenantId` and should not expose cross-tenant resources.
- Existing Docker-backed integration tests: extend this style for backend-to-AI service integration and tenant isolation coverage.

### Established Patterns
- REST endpoints are versioned under `/api/v1`.
- Tenant-scoped APIs use path-based tenant identity: `/api/v1/tenants/{tenantId}/...`.
- Tenant-scoped reads call `getTenant`; tenant-scoped mutations call `requireActiveTenant`.
- Cross-tenant resource access should resolve as not found rather than leaking ownership.
- Mutations that represent human operational actions require explicit `actorUserId` until authentication exists.
- Ticket detail currently embeds workflow history; Phase 4 should similarly expose classification artifacts for operational inspection.

### Integration Points
- Add a new FastAPI AI service under the monorepo and wire it into `docker-compose.yml`.
- Add a backend AI classification client with a short configurable timeout defaulting around 2 seconds.
- Extend the ticket model or related persistence to store append-only classification attempts.
- Extend ticket detail responses to include classification artifacts.
- Add a manual re-analysis endpoint under the tenant-scoped ticket route.
- Add contract tests for the FastAPI classification endpoint and backend integration tests for successful classification, failure artifacts, auto-apply behavior, history traceability, and tenant isolation.

</code_context>

<specifics>
## Specific Ideas

- Use deterministic keyword rules as local AI behavior so tests remain repeatable and the project does not need external AI provider credentials in Phase 4.
- Keep AI rationale out of the Phase 4 contract; classification is operational metadata plus confidence and versioning.
- Treat successful AI classification as operational automation that reduces manual work, while preserving traceability through append-only attempts and linked workflow history.
- Keep failed attempts visible in classification artifacts without cluttering workflow history or changing ticket fields.

</specifics>

<deferred>
## Deferred Ideas

- Human-readable AI rationale can be revisited in later evidence or draft-generation phases.
- Real LLM/provider-backed classification is deferred until a later AI hardening phase.
- Async classification jobs, retries, queue inspection, and Redis-backed orchestration are deferred to later reliability work.
- Evidence retrieval, embeddings, vector search, RAG evidence bundles, and source citation behavior remain Phase 5 scope.
- Draft response generation, review, edit, approval, and final response controls remain Phase 6 scope.
- Frontend AI review UI remains post-v1 UI scope.
- Authentication and full RBAC remain post-v1 authentication scope.

</deferred>

---

*Phase: 4-AI Classification Integration*
*Context gathered: 2026-06-08*
