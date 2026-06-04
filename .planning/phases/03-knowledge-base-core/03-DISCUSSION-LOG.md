# Phase 3: Knowledge Base Core - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md - this log preserves the alternatives considered.

**Date:** 2026-06-04
**Phase:** 3-Knowledge Base Core
**Areas discussed:** Document Shape, Update Semantics, Retrieval-Ready Metadata, API Surface

---

## Document Shape

### Document Type

| Option | Description | Selected |
|--------|-------------|----------|
| FAQ and POLICY enum | Matches the roadmap language and gives later retrieval/tests a stable contract. | yes |
| Freeform type string | More flexible, but weaker validation and easier to drift. | |
| Single DOCUMENT type | Simplest now, but pushes FAQ/policy meaning into loose metadata later. | |

**User's choice:** FAQ and POLICY enum
**Notes:** None.

### Content Model

| Option | Description | Selected |
|--------|-------------|----------|
| Shared title + content fields | One simple model works for both FAQ and policy documents; later RAG can chunk content. | yes |
| FAQ fields plus policy fields | Clearer but creates branching in the model and API. | |
| Flexible metadata map | Maximum flexibility, weaker contract and harder tests. | |

**User's choice:** Shared title and content fields
**Notes:** None.

### Publication State

| Option | Description | Selected |
|--------|-------------|----------|
| ACTIVE/ARCHIVED | Simple lifecycle; archived documents remain readable for audit but can be excluded from future retrieval. | yes |
| DRAFT/ACTIVE/ARCHIVED | Useful, but starts to look like a publishing workflow. | |
| No status field | Simplest, but makes stale policy handling harder later. | |

**User's choice:** ACTIVE/ARCHIVED
**Notes:** None.

### Actor Metadata

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit actorUserId in mutation requests | Consistent with Phase 2 workflow history and validates tenant-local actors. | yes |
| No actor fields for knowledge documents | Simpler, but less traceable. | |
| Plain author name/email fields | Human-readable, but not tied to tenant-local operational users. | |

**User's choice:** Explicit actorUserId in mutation requests
**Notes:** None.

---

## Update Semantics

### Main Record Updates

| Option | Description | Selected |
|--------|-------------|----------|
| Overwrite current fields and update audit metadata | Simple API and storage model; enough for Phase 3. | yes |
| Create immutable revisions on every update | Better auditability, but expands the model and API expectations. | |
| Reject content updates after creation | Safest but too rigid for FAQ/policy maintenance. | |

**User's choice:** Overwrite current fields and update audit metadata
**Notes:** None.

### Last Actor Tracking

| Option | Description | Selected |
|--------|-------------|----------|
| Track createdByUserId and updatedByUserId | Preserves traceability without full revision history. | yes |
| Track only timestamps | Simpler but weaker audit trail. | |
| Embed a full update history list | More traceable, but overlaps with revision/history scope. | |

**User's choice:** Track createdByUserId and updatedByUserId
**Notes:** None.

### Archived Document Updates

| Option | Description | Selected |
|--------|-------------|----------|
| Allow metadata/status updates, reject content updates | Archived records stay stable but can be restored or labeled. | yes |
| Reject all updates while archived | Simplest safety rule, but makes mistakes harder to fix. | |
| Allow all updates | Flexible, but archived content becomes less trustworthy. | |

**User's choice:** Allow metadata/status updates, reject content updates
**Notes:** None.

### Archived List Behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Exclude archived by default, optional status filter | Keeps active knowledge clean while preserving access. | yes |
| Include everything by default | Easiest to inspect, but stale policies may clutter normal use. | |
| Only expose archived documents by detail endpoint | Clean lists, but harder for admins to manage archives. | |

**User's choice:** Exclude archived by default, optional status filter
**Notes:** None.

---

## Retrieval-Ready Metadata

### Source Metadata

| Option | Description | Selected |
|--------|-------------|----------|
| Required sourceLabel, optional sourceUrl | Gives later evidence citations a stable human-readable source without requiring external URLs. | yes |
| No source metadata yet | Simpler, but Phase 5 evidence references will be weaker. | |
| Required sourceUrl for every document | Strong traceability, but unrealistic for internal policies/FAQs. | |

**User's choice:** Required sourceLabel, optional sourceUrl
**Notes:** None.

### Tags

| Option | Description | Selected |
|--------|-------------|----------|
| Optional normalized tags list | Useful for future filtering/retrieval and easy to validate now. | yes |
| No tags yet | Smallest model, but less retrieval-ready. | |
| Freeform metadata map instead of tags | Flexible, but weak contract and harder tenant-safe tests. | |

**User's choice:** Optional normalized tags list
**Notes:** None.

### Effective Dates

| Option | Description | Selected |
|--------|-------------|----------|
| Optional effectiveFrom and effectiveTo | Supports policy traceability without forcing every FAQ to behave like a policy. | yes |
| Required dates for all documents | Too heavy for FAQs. | |
| No effective dates | Simpler, but weaker for policy evidence later. | |

**User's choice:** Optional effectiveFrom and effectiveTo
**Notes:** None.

### Content Fingerprint

| Option | Description | Selected |
|--------|-------------|----------|
| Store a server-computed contentHash | Helps later evidence snapshots and change detection without adding version history. | yes |
| No content hash yet | Simpler, but later retrieval artifacts have less change traceability. | |
| Client-provided content hash | Flexible, but trust and validation are weaker. | |

**User's choice:** Store a server-computed contentHash
**Notes:** None.

---

## API Surface

### Route Shape

| Option | Description | Selected |
|--------|-------------|----------|
| /api/v1/tenants/{tenantId}/knowledge-documents | Explicit resource name and consistent tenant-scoped route pattern. | yes |
| /api/v1/tenants/{tenantId}/knowledge | Shorter, but less clear as the resource grows. | |
| /api/v1/knowledge-documents?tenantId=... | Weaker than the established tenant path pattern. | |

**User's choice:** /api/v1/tenants/{tenantId}/knowledge-documents
**Notes:** None.

### Operations

| Option | Description | Selected |
|--------|-------------|----------|
| Create, list, detail, update, archive/restore | Covers admin maintenance and stale policy handling without delete. | yes |
| Create, list, detail, update only | Simpler, but archive status becomes less useful. | |
| Full CRUD with delete | Familiar, but deletion weakens traceability. | |

**User's choice:** Create, list, detail, update, archive/restore
**Notes:** None.

### List Filtering

| Option | Description | Selected |
|--------|-------------|----------|
| Filter by type, status, tag, and active date window | Supports admin use and future retrieval tests without search/RAG. | yes |
| Filter only by type/status | Simpler, but less retrieval-ready. | |
| No filters yet | Smallest API, but weaker management surface. | |

**User's choice:** Filter by type, status, tag, and active date window
**Notes:** None.

### Text Search

| Option | Description | Selected |
|--------|-------------|----------|
| No text search/RAG yet; exact filters only | Keeps Phase 3 as document management and leaves retrieval semantics to Phase 5. | yes |
| Simple Mongo text search | Useful but starts pulling retrieval behavior forward. | |
| Keyword contains search only | Likely becomes a weak pseudo-retrieval contract. | |

**User's choice:** No text search/RAG yet; exact filters only
**Notes:** None.

## the agent's Discretion

- Exact Java type names, DTO names, enum names, repository method names, validation annotation details, MongoDB collection name, route method names, and test class organization.
- Exact archive/restore route shape, provided delete is not introduced and the API contract remains clear.
- Exact active date window filter names, provided they are documented and testable.

## Deferred Ideas

- Immutable document revisions and full update history.
- Text search, keyword contains search, Mongo text indexes, embeddings, vector search, and RAG retrieval.
- AI classification, draft generation, and response approval.
- Frontend knowledge management UI.
- Authentication and full RBAC.
- Destructive deletion.
