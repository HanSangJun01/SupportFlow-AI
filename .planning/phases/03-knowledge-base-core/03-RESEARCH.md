# Phase 3: Knowledge Base Core - Research

**Date:** 2026-06-04
**Status:** Complete

## Research Question

What does the planner need to know to implement Phase 3 well on top of the Phase 2 Spring Boot and MongoDB backend?

## Phase Scope Summary

Phase 3 adds backend-only tenant-scoped knowledge document management. It should deliver FAQ and policy document registration, update, retrieval, archive/restore behavior, tenant isolation, retrieval-ready metadata, API contract documentation, and automated verification.

The phase must not implement authentication, frontend UI, AI classification, embeddings, vector search, Mongo text search, keyword contains search, RAG retrieval, evidence ranking, immutable revisions, or destructive deletion. Later retrieval phases can consume the stored model, but Phase 3 is only the document registry and metadata foundation.

## 1. Existing Backend Patterns To Reuse

### Package And Layer Shape

Add a new package beside the current domain packages:

- `com.supportflow.knowledge`
- `KnowledgeDocument`
- `KnowledgeDocumentType`
- `KnowledgeDocumentStatus`
- `KnowledgeDocumentRepository`
- `KnowledgeDocumentService`
- `KnowledgeDocumentController`

This mirrors existing `tenant`, `ticket`, and `user` packages. Avoid introducing a separate module or shared abstraction unless implementation pressure proves it is needed.

### Controller Pattern

Reuse the controller style in `TicketController` and `OperationalUserController`:

- `@RestController`
- `@RequestMapping("/api/v1/tenants/{tenantId}/knowledge-documents")`
- `@Tag` and per-route `@Operation(summary = "...")`
- nested request and response records inside the controller
- `@Valid @RequestBody` for mutation requests
- `@PathVariable String tenantId`
- `@RequestParam(required = false)` filters
- `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)` for `Instant` query params
- `ResponseEntity.created(URI.create(...)).body(...)` for creates

Validation should initially use Jakarta bean validation annotations and service-layer checks, matching current code. Errors should flow through `GlobalExceptionHandler` as `ApiErrorResponse`.

### Service Pattern

Reuse the Phase 2 read-vs-mutation split:

- Read/list/detail methods call `tenantService.getTenant(tenantId)`.
- Create/update/archive/restore methods call `tenantService.requireActiveTenant(tenantId)`.
- Actor-attributed mutations call `operationalUserService.validateActiveActor(tenantId, actorUserId)`.
- Tenant-owned document lookup uses `findByTenantIdAndId(tenantId, documentId)` so cross-tenant access returns 404.
- Use `ResponseStatusException` with existing status semantics: 404 for missing/cross-tenant resources, 400 for invalid request state or bad model rules, 409 for inactive tenant or inactive actor conflicts.

Avoid binding knowledge permissions to operational roles in Phase 3 unless planning explicitly decides to add a role check. Phase 3 context only locks "active tenant-local operational user" validation, not real RBAC.

### Repository Pattern

Use Spring Data Mongo repositories like `TicketRepository` and `OperationalUserRepository`:

- `List<KnowledgeDocument> findByTenantId(String tenantId)`
- `Optional<KnowledgeDocument> findByTenantIdAndId(String tenantId, String id)`
- optional exact-query helpers such as `findByTenantIdAndStatus(...)` are acceptable, but current services mostly fetch by tenant and filter in Java for MVP scope.

For Phase 3 scale, Java-side filtering is consistent with `TicketService.listTickets`. Add an indexed `tenantId` field on the Mongo document. Consider indexes for `status`, `type`, and `tags` only if the planner wants retrieval-readiness at the storage level, but do not add text indexes.

### Domain Pattern

Follow the Mongo document style in `Ticket` and `OperationalUser`:

- `@Document("knowledge_documents")`
- `@Id String id`
- `@Indexed String tenantId`
- plain getters and setters
- enums stored as Java enum fields
- `Instant createdAt`
- `Instant updatedAt`

Do not use Lombok, builders, Kotlin data classes, JPA annotations, or custom mapping infrastructure. The existing code is explicit Java beans plus records at the API boundary.

### DTO Pattern

Expose response records instead of returning Mongo documents directly from controllers. Include only fields useful for client contracts and later evidence linking:

- ids and tenant id
- type and status
- title and content
- source metadata
- tags
- effective dates
- content hash
- audit actors
- timestamps

Keep command records inside `KnowledgeDocumentService`, matching `TicketService.CreateTicketCommand` and `TicketService.TicketFilters`.

### Test Pattern

Use the existing split:

- service unit tests with JUnit Jupiter, Mockito, AssertJ
- WebMvc controller tests with `@WebMvcTest(controllers = {KnowledgeDocumentController.class, GlobalExceptionHandler.class})`, `MockMvc`, and `@MockitoBean`
- Mongo-backed HTTP integration test with `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `TestRestTemplate`, `MongoDBContainer("mongo:7")`, and `@Testcontainers(disabledWithoutDocker = true)`
- OpenAPI documentation assertions in `OpenApiDocumentationTest`
- reflection route assertions in `FoundationVerificationTest` or a similarly named verification test

### Documentation Pattern

Create a new SDD/API contract doc:

- `docs/sdd/phase-03-knowledge-base-core-api.md`

Follow `docs/sdd/phase-02-tenant-workflow-core-api.md`: list endpoints, request fields, response fields, validation rules, inactive-tenant behavior, tenant isolation rules, and explicit exclusions.

## 2. Proposed Knowledge Document Model

### Enums

`KnowledgeDocumentType`:

- `FAQ`
- `POLICY`

`KnowledgeDocumentStatus`:

- `ACTIVE`
- `ARCHIVED`

Do not add `DRAFT`, `PUBLISHED`, `DELETED`, or `RETIRED` in Phase 3. The user decisions lock the status set to active/archive only.

### Document Fields

Recommended `KnowledgeDocument` fields:

- `String id`
- `@Indexed String tenantId`
- `KnowledgeDocumentType type`
- `KnowledgeDocumentStatus status`
- `String title`
- `String content`
- `String sourceLabel`
- `String sourceUrl`
- `List<String> tags`
- `Instant effectiveFrom`
- `Instant effectiveTo`
- `String contentHash`
- `String createdByUserId`
- `String updatedByUserId`
- `Instant createdAt`
- `Instant updatedAt`
- `Instant archivedAt`
- `String archivedByUserId`

`archivedAt` and `archivedByUserId` are not explicitly required in the context, but they are useful audit metadata for archive/restore and later evidence traceability. On restore, set `status = ACTIVE`, update `updatedByUserId`/`updatedAt`, and either clear `archivedAt`/`archivedByUserId` or preserve them as "last archive" metadata. Planning should choose one and document it. The simpler API behavior is to clear them on restore so current state is unambiguous.

### Validation Implications

Create request should require:

- `type`: non-null enum, only `FAQ` or `POLICY`
- `title`: non-blank
- `content`: non-blank
- `sourceLabel`: non-blank
- `actorUserId`: non-blank, active same-tenant operational user
- `sourceUrl`: optional, validate with `@URL` only if Hibernate Validator URL support is already available; otherwise use `@Pattern` or service-level URI parsing
- `tags`: optional list
- `effectiveFrom` and `effectiveTo`: optional Instants

Update request should require:

- `actorUserId`: non-blank
- all mutable fields optional
- at least one mutable field should be supplied, unless the planner chooses no-op updates; rejecting empty updates with 400 is cleaner

Service-level checks should enforce:

- `effectiveFrom` must be before or equal to `effectiveTo` when both are present.
- normalized tags should be lower-case, trimmed, de-duplicated, and empty tags removed.
- tag count and tag length should be bounded for MVP stability. A conservative default is max 20 tags and max 50 chars per tag.
- title and source label should be trimmed and bounded. A conservative default is title max 200 chars, source label max 200 chars.
- content should be non-blank and bounded to avoid oversized Mongo documents. A pragmatic MVP bound is 50,000 chars unless another project limit exists.
- clients must not provide `contentHash`; the backend computes it.

### Content Hash

Compute `contentHash` on create and whenever content changes while the document is active.

Recommended deterministic algorithm:

- Normalize content by replacing CRLF/CR with LF.
- Trim only line-ending representation, not meaningful leading/trailing content, unless the API already trims `content` before storage.
- Hash the exact stored normalized content bytes with UTF-8.
- Store hex SHA-256.

The critical planning choice is to hash exactly what is stored. If the service trims or normalizes content before saving, hash that normalized stored value. Otherwise, semantically identical API payloads can produce confusing hash drift.

## 3. API Contract Recommendations

### Routes

Use the path locked in context:

- `POST /api/v1/tenants/{tenantId}/knowledge-documents`
- `GET /api/v1/tenants/{tenantId}/knowledge-documents`
- `GET /api/v1/tenants/{tenantId}/knowledge-documents/{documentId}`
- `PATCH /api/v1/tenants/{tenantId}/knowledge-documents/{documentId}`
- `PATCH /api/v1/tenants/{tenantId}/knowledge-documents/{documentId}/archive`
- `PATCH /api/v1/tenants/{tenantId}/knowledge-documents/{documentId}/restore`

Separate archive/restore endpoints are clearer than generic status patching because archive has special content-update implications and no delete behavior. They also make OpenAPI and tests easier to read.

### Create Request

```json
{
  "type": "FAQ",
  "title": "Refund policy",
  "content": "Customers can request a refund within 30 days.",
  "sourceLabel": "Support policy handbook",
  "sourceUrl": "https://example.com/policies/refunds",
  "tags": ["billing", "refunds"],
  "effectiveFrom": "2026-06-01T00:00:00Z",
  "effectiveTo": null,
  "actorUserId": "tenant-local-user-id"
}
```

Create behavior:

- Requires active tenant.
- Requires active same-tenant actor.
- Creates document as `ACTIVE`.
- Sets `createdByUserId` and `updatedByUserId` to `actorUserId`.
- Computes `contentHash`.
- Sets `createdAt` and `updatedAt`.
- Returns HTTP 201 with Location header.

### Update Request

```json
{
  "title": "Updated refund policy",
  "content": "Updated content.",
  "sourceLabel": "Support policy handbook",
  "sourceUrl": "https://example.com/policies/refunds-v2",
  "tags": ["billing", "refunds", "policy"],
  "effectiveFrom": "2026-06-01T00:00:00Z",
  "effectiveTo": "2026-12-31T23:59:59Z",
  "actorUserId": "tenant-local-user-id"
}
```

Update behavior:

- Requires active tenant.
- Requires active same-tenant actor.
- Resolves the document through `tenantId` and `documentId`.
- Updates only supplied fields.
- Recomputes `contentHash` only when content changes.
- Refreshes `updatedByUserId` and `updatedAt` when a persisted change occurs.
- Rejects content updates while status is `ARCHIVED`.
- Allows metadata updates while archived if no content change is supplied.

The planner should decide whether unchanged no-op updates return 200 without save or reject as 400. Phase 2 ticket workflow returns the unchanged object for no-op metadata updates, so returning 200 without save is consistent.

### Archive Request

```json
{
  "actorUserId": "tenant-local-user-id"
}
```

Archive behavior:

- Requires active tenant.
- Requires active same-tenant actor.
- Resolves by `tenantId` and `documentId`.
- Sets `status = ARCHIVED`.
- Sets `archivedAt`, `archivedByUserId`, `updatedAt`, and `updatedByUserId`.
- Should be idempotent or reject already archived with 409. The simpler operational contract is idempotent 200 if already archived, but tests must lock whichever behavior is chosen.
- Does not delete content.

### Restore Request

```json
{
  "actorUserId": "tenant-local-user-id"
}
```

Restore behavior:

- Requires active tenant.
- Requires active same-tenant actor.
- Resolves by `tenantId` and `documentId`.
- Sets `status = ACTIVE`.
- Updates `updatedAt` and `updatedByUserId`.
- Clears archive metadata if the chosen response semantics treat archive metadata as current-state only.
- Should be idempotent or reject already active with 409. Match archive semantics.

### Response Shape

```json
{
  "id": "knowledge-document-id",
  "tenantId": "tenant-id",
  "type": "FAQ",
  "status": "ACTIVE",
  "title": "Refund policy",
  "content": "Customers can request a refund within 30 days.",
  "sourceLabel": "Support policy handbook",
  "sourceUrl": "https://example.com/policies/refunds",
  "tags": ["billing", "refunds"],
  "effectiveFrom": "2026-06-01T00:00:00Z",
  "effectiveTo": null,
  "contentHash": "sha256-hex",
  "createdByUserId": "tenant-local-user-id",
  "updatedByUserId": "tenant-local-user-id",
  "createdAt": "2026-06-04T00:00:00Z",
  "updatedAt": "2026-06-04T00:00:00Z",
  "archivedAt": null,
  "archivedByUserId": null
}
```

### List Filtering

Recommended query params:

- `type`: exact `FAQ` or `POLICY`
- `status`: exact `ACTIVE` or `ARCHIVED`
- `tag`: exact normalized tag match
- `activeAt`: returns documents whose effective window includes this instant

Default behavior:

- If `status` is absent, return only `ACTIVE` documents.
- If `status=ARCHIVED`, return archived documents.
- If `status=ACTIVE`, return active documents.
- Do not include archived documents in mixed results unless the planner adds a clear `status=ALL` enum or separate boolean. The context only requires optional status filter, so avoid adding `ALL` unless needed.

Active date window behavior:

- If `activeAt` is supplied, include documents where `effectiveFrom` is null or `<= activeAt`, and `effectiveTo` is null or `>= activeAt`.
- Do not infer active date windows from current clock when no date filter is supplied. That would silently hide records and complicate support operations.

Sorting:

- Sort by `createdAt` ascending to match `TicketService.listTickets`, or by `updatedAt` descending for operational usefulness. The planner should choose and document it. Existing code uses `createdAt` ascending, so that is the safest local pattern.

### Out-of-Scope Exclusions

Document these exclusions in the SDD:

- no authentication headers, sessions, Spring Security, or full RBAC
- no frontend knowledge management UI
- no text search or keyword contains search
- no Mongo text indexes
- no embeddings or vector indexes
- no RAG evidence retrieval
- no relevance ranking
- no immutable document revision history
- no destructive delete endpoint
- no AI ingestion, classification, summarization, or draft generation

## 4. Tenant Isolation And Inactive Tenant Rules

### Tenant Isolation

Every knowledge document must store `tenantId`. Every repository lookup used by tenant-scoped APIs must include `tenantId`.

Expected denial behavior:

- Reading tenant B's document through tenant A's path returns 404.
- Updating tenant B's document through tenant A's path returns 404 and does not mutate.
- Archiving or restoring tenant B's document through tenant A's path returns 404 and does not mutate.
- Using a tenant B actor for tenant A mutations returns 404 through `OperationalUserService.validateActiveActor`.
- Tenant A list never includes tenant B documents.

Do not create an endpoint that fetches knowledge documents by id without tenant id. That would violate the path-based tenant identity pattern.

### Inactive Tenant Behavior

Apply the same Phase 2 read-vs-mutation rule:

- Inactive tenant can list knowledge documents.
- Inactive tenant can view knowledge document detail.
- Inactive tenant rejects create.
- Inactive tenant rejects update.
- Inactive tenant rejects archive.
- Inactive tenant rejects restore.

Expected response status for inactive-tenant mutations should match `TenantService.requireActiveTenant`: HTTP 409 conflict with "Tenant is inactive".

### Archived Document Behavior

Archived documents remain readable by detail lookup and by list when `status=ARCHIVED`.

Mutation matrix:

- Active document metadata update: allowed.
- Active document content update: allowed, recompute hash.
- Active document archive: allowed.
- Archived document metadata update: allowed.
- Archived document content update: reject with 400 or 409. Prefer 400 because the document exists and the request violates a domain rule.
- Archived document restore: allowed.

The planner should define "metadata update" as fields other than `content`: title, source label, source URL, tags, effective dates, and possibly type. Consider whether `type` should be mutable. If type changes are allowed, they are metadata changes. If not, reject with 400 and document type immutability. For MVP simplicity, allowing type updates while active is acceptable, but changing type while archived should be treated as metadata and therefore allowed only if the context decision is interpreted broadly.

## 5. Verification Strategy

### Unit Tests

Create `KnowledgeDocumentServiceTest` with Mockito and AssertJ.

Exact behaviors to prove:

- create requires `tenantService.requireActiveTenant`.
- create validates active same-tenant actor before save.
- create sets `ACTIVE`, timestamps, audit actor ids, normalized tags, and backend-computed `contentHash`.
- list calls `tenantService.getTenant` and defaults to active-only results.
- list filters by type, status, normalized tag, and active date window.
- detail uses `findByTenantIdAndId`.
- update validates tenant and actor, updates only supplied fields, refreshes audit metadata, and recomputes hash only on content change.
- archived content update is rejected and repository `save` is not called.
- archive and restore validate actor and tenant, mutate status/audit metadata, and never delete content.
- cross-tenant document lookup failure returns 404 through repository miss.
- invalid effective date range returns 400.

### WebMvc Tests

Create `KnowledgeDocumentApiIntegrationTest` using `@WebMvcTest`.

Exact behaviors to prove:

- create returns 201 and the response contains tenant id, status, content hash, and audit actor ids.
- create request rejects missing `type`, `title`, `content`, `sourceLabel`, and `actorUserId`.
- list accepts `type`, `status`, `tag`, and `activeAt` query params.
- detail route returns one document.
- update route accepts partial metadata/content request plus actor.
- archive route requires `actorUserId`.
- restore route requires `actorUserId`.
- service `ResponseStatusException` maps to expected HTTP status through `GlobalExceptionHandler`.

### Mongo Testcontainers Tests

Create `KnowledgeDocumentMongoIntegrationTest` or `TenantKnowledgeDocumentMongoIntegrationTest` using the existing Testcontainers pattern.

Exact behaviors to prove through real HTTP calls:

- tenant A create/list/detail does not expose tenant B documents.
- tenant A cannot read, update, archive, or restore tenant B document ids through tenant A paths.
- cross-tenant actor cannot create/update/archive/restore a tenant A document.
- inactive actor cannot create/update/archive/restore.
- inactive tenant allows list/detail but rejects create/update/archive/restore.
- archive hides document from default list and includes it when `status=ARCHIVED`.
- restore puts the document back in the default active list.
- archived content update is rejected without changing content hash or content.
- filters combine correctly for type, status, tag, and activeAt.

### OpenAPI Documentation Checks

Extend `OpenApiDocumentationTest`:

- assert `/api/v1/tenants/{tenantId}/knowledge-documents`
- assert `/api/v1/tenants/{tenantId}/knowledge-documents/{documentId}`
- assert `/api/v1/tenants/{tenantId}/knowledge-documents/{documentId}/archive`
- assert `/api/v1/tenants/{tenantId}/knowledge-documents/{documentId}/restore`

Add a reflection route check in `FoundationVerificationTest` or a new focused verification test:

- controller exposes `@PostMapping`
- controller exposes `@GetMapping` list and detail
- controller exposes `@PatchMapping` update, archive, and restore
- Phase 3 integration test class exists

### Documentation Checks

Create `docs/sdd/phase-03-knowledge-base-core-api.md` and verify it contains:

- all route names
- request and response fields
- enum values
- content hash ownership by backend
- default active-only list behavior
- archive/restore semantics
- inactive tenant behavior
- tenant isolation behavior
- explicit retrieval/search/RAG exclusions

## 6. Plan Split Recommendation

Keep the roadmap's two-plan split. It is enough if Plan 03-02 owns tests, docs, and validation evidence.

### Plan 03-01: Implement Tenant-Scoped Knowledge Document APIs And Persistence Model

Wave: 1

Requirements:

- `KNOW-01`
- `KNOW-02`
- `KNOW-03`
- `KNOW-04`

Recommended tasks:

1. Add model, enums, repository, service commands, validation helpers, content hash computation, and tag/effective-date normalization.
2. Add controller routes for create, list, detail, update, archive, and restore.
3. Add focused service and WebMvc tests for model and API behavior.

Dependencies:

- depends on Phase 2 operational users and active-tenant guard
- no dependency on AI service or retrieval code

Plan 03-01 should create the functional backend slice and fast test coverage. It should not wait until Plan 03-02 to add all tests, because the model has enough validation complexity to need immediate feedback.

### Plan 03-02: Add Metadata Validation, Tenant-Isolation Tests, API Docs, And Phase Verification

Wave: 2

Requirements:

- `KNOW-01`
- `KNOW-02`
- `KNOW-03`
- `KNOW-04`
- quality evidence for `QUAL-01`, `QUAL-02`, `QUAL-03`, and later `QUAL-05` readiness

Recommended tasks:

1. Add Mongo-backed HTTP integration tests for tenant isolation, inactive tenant behavior, cross-tenant actor denial, archive/default-list behavior, restore behavior, and archived content-update rejection.
2. Add OpenAPI route assertions, reflection/verification assertions, Phase 3 SDD/API contract doc, and README verification note if local test instructions need updating.

Dependencies:

- depends on Plan 03-01 routes and service behavior

### Why Not More Plans

Three plans would be reasonable if API docs and integration tests grow large, but the roadmap already expects two plans and the implementation is a CRUD-style backend slice. Keep it at two unless planning discovers that validation architecture generation or docs work must be isolated.

## 7. Risks And Landmines

| Risk | Why It Matters | Planning Mitigation |
|------|----------------|---------------------|
| Content hash determinism | Later retrieval/evidence workflows may rely on stable content references | Define normalization before hashing; hash exactly the stored content; test CRLF vs LF behavior if normalization is implemented |
| Archived content update rejection | Context allows archived metadata/status updates but rejects content updates | Service must distinguish content from metadata; add unit and Mongo integration tests proving content/hash stay unchanged |
| Actor validation drift | Phase 3 has no auth, so actor ids are the audit trust boundary | Always validate `actorUserId` with `OperationalUserService.validateActiveActor` before mutation; reject cross-tenant and inactive actors |
| Status filter defaults | Accidentally returning archived docs by default can confuse clients and retrieval later | Default list to `ACTIVE`; require `status=ARCHIVED` to see archive; document and test this |
| Search/RAG scope creep | Adding text search or embeddings early creates premature contracts | Only exact filters in Phase 3; defer relevance/search/vector behavior to Phase 5 |
| Type mutability ambiguity | Changing FAQ to POLICY may alter downstream assumptions | Decide in PLAN.md. If mutable, test it. If immutable, reject updates with 400 and document it |
| Effective window semantics | Effective dates can be confused with status | Treat status and effective window as independent filters; no implicit date filtering without `activeAt` |
| Source URL validation | Strict URL validation can reject useful internal labels or relative links | Keep `sourceLabel` required; make `sourceUrl` optional and validate conservatively |
| Tag normalization mismatch | Clients may filter by `Refunds` while stored tag is `refunds` | Normalize on write and normalize the `tag` query param before filtering |
| Audit metadata overwrite | Updates must preserve creator while refreshing updater | Set `createdByUserId` only on create; update only `updatedByUserId` after mutations |
| Repository leakage | `findById` without tenant id would break isolation | Avoid service code paths that call document repository `findById` for tenant-scoped routes |

## 8. Validation Architecture

Phase 3 should use the existing Spring/JUnit validation stack.

### Test Infrastructure

| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter, Spring Boot Test, Mockito, AssertJ, Testcontainers |
| Config file | `backend-spring/pom.xml` |
| Quick service/API command | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentServiceTest,KnowledgeDocumentApiIntegrationTest` |
| Mongo isolation command | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentMongoIntegrationTest` |
| Documentation command | `cd backend-spring && ./mvnw test -Dtest=OpenApiDocumentationTest,FoundationVerificationTest` |
| Full suite command | `cd backend-spring && ./mvnw verify` |
| Estimated runtime | Similar to Phase 2: fast slice tests under 120 seconds with cached dependencies; Docker-backed tests may be slower on cold start |

### Sampling Rate

- After each Plan 03-01 model/service/controller task: run `KnowledgeDocumentServiceTest,KnowledgeDocumentApiIntegrationTest`.
- After Plan 03-01 completion: run `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentServiceTest,KnowledgeDocumentApiIntegrationTest`.
- After each Plan 03-02 integration/docs task: run the task-specific command.
- Before Phase 3 verification: run `cd backend-spring && ./mvnw verify`.
- No watch-mode commands should be used in plans.

### Per-Task Verification Map For Nyquist

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|
| 03-01-01 | 03-01 | 1 | KNOW-01, KNOW-02, KNOW-04 | T-03-01-01 | Document model stores tenant id, metadata, audit actors, timestamps, and backend content hash | unit | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentServiceTest` |
| 03-01-02 | 03-01 | 1 | KNOW-01, KNOW-02, KNOW-03 | T-03-01-02 | Tenant-scoped REST routes use active-tenant mutation guards and tenant-id lookups | unit + WebMvc | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentServiceTest,KnowledgeDocumentApiIntegrationTest` |
| 03-01-03 | 03-01 | 1 | KNOW-02, KNOW-04 | T-03-01-03 | Archive/restore preserve traceability and reject archived content edits | unit + WebMvc | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentServiceTest,KnowledgeDocumentApiIntegrationTest` |
| 03-02-01 | 03-02 | 2 | KNOW-03 | T-03-02-01 | Cross-tenant document ids and actor ids do not leak or mutate data | integration | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentMongoIntegrationTest` |
| 03-02-02 | 03-02 | 2 | KNOW-01, KNOW-02, KNOW-04 | T-03-02-02 | Inactive tenant read/mutation split, filters, archive defaults, and restore behavior are proven over Mongo | integration | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentMongoIntegrationTest` |
| 03-02-03 | 03-02 | 2 | QUAL-01 evidence | T-03-02-03 | OpenAPI and SDD document the API contract and exclusions | docs + full suite | `cd backend-spring && ./mvnw verify` |

### Threat References

| Threat ID | STRIDE | Component | Mitigation |
|-----------|--------|-----------|------------|
| T-03-01-01 | Tampering | Knowledge document metadata and content hash | Backend computes content hash from stored normalized content and ignores any client hash |
| T-03-01-02 | Information Disclosure | Tenant-scoped knowledge routes | Every lookup includes tenant id; cross-tenant access returns 404 |
| T-03-01-03 | Tampering | Archived document mutation | Archived records allow metadata/status updates but reject content changes |
| T-03-02-01 | Information Disclosure | Cross-tenant document and actor ids | Mongo HTTP tests prove tenant A cannot access tenant B documents or use tenant B actors |
| T-03-02-02 | Tampering | Inactive tenant mutation behavior | `requireActiveTenant` blocks create/update/archive/restore while reads still work |
| T-03-02-03 | Scope Creep | Retrieval/search API contract | API docs explicitly exclude search, text indexes, embeddings, and RAG |

### Required Evidence Before Phase Sign-Off

- `KnowledgeDocumentServiceTest` exists and proves model rules, actor validation, status defaults, hash generation, archived content rejection, and filters.
- `KnowledgeDocumentApiIntegrationTest` exists and proves controller routes, request validation, response shape, and error mapping.
- `KnowledgeDocumentMongoIntegrationTest` exists and proves tenant isolation, inactive-tenant behavior, archive/default-list behavior, restore, and cross-tenant actor denial.
- `OpenApiDocumentationTest` asserts Phase 3 route paths.
- `docs/sdd/phase-03-knowledge-base-core-api.md` exists and documents exact API behavior and exclusions.
- `cd backend-spring && ./mvnw verify` passes.

## Planning Checklist

Before writing PLAN.md, decide and lock:

- whether archived archive/restore endpoints are idempotent or reject same-state requests
- whether restore clears `archivedAt` and `archivedByUserId`
- whether `type` is mutable on update
- exact max lengths for title, source label, source URL, tags, and content
- exact no-op update behavior
- exact list sort order
- exact content normalization before hashing
- exact names for date filter params, preferably `activeAt`

## RESEARCH COMPLETE
