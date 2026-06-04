# Phase 03 Knowledge Base Core API

This document captures the Phase 3 backend API contract for tenant-scoped knowledge documents. Phase 3 manages shared-shape `FAQ` and `POLICY` records for one tenant at a time and keeps retrieval behavior out of scope.

Phase 3 keeps tenant identity in URL paths and does not add authentication headers, auth claims, full RBAC, frontend UI, search, AI classification, RAG, embeddings, vector search, destructive delete, or immutable revisions. Mutations use explicit `actorUserId` fields until authentication is introduced later.

## Knowledge Document Shape

Knowledge documents are stored under one tenant and expose:

- `id`: document id.
- `tenantId`: owning tenant id.
- `type`: `FAQ` or `POLICY`.
- `status`: `ACTIVE` or `ARCHIVED`.
- `title`: required display title, max 200 characters.
- `content`: required document body, max 50000 characters.
- `sourceLabel`: required source label, max 200 characters.
- `sourceUrl`: optional source URL or reference.
- `tags`: optional normalized tags. Tags are trimmed, lower-cased, deduplicated, limited to 20 tags, and limited to 50 characters each.
- `effectiveFrom`: optional timestamp for policy/document applicability.
- `effectiveTo`: optional timestamp for policy/document applicability.
- `contentHash`: server-computed SHA-256 hash of the stored LF-normalized `content`.
- `createdByUserId`: tenant-local actor id that created the document.
- `updatedByUserId`: tenant-local actor id that last changed the document.
- `createdAt`: server timestamp.
- `updatedAt`: server timestamp.
- `archivedAt`: current archive timestamp, present only while archived.
- `archivedByUserId`: current archive actor id, present only while archived.

`effectiveFrom` must not be after `effectiveTo` when both are supplied. `contentHash` is never accepted from clients and changes only when stored `content` changes.

## Endpoints

### POST /api/v1/tenants/{tenantId}/knowledge-documents

Creates a tenant-scoped knowledge document.

Request fields:

- `type`: required `FAQ` or `POLICY`.
- `title`: required, non-blank, max 200 characters.
- `content`: required, non-blank, max 50000 characters.
- `sourceLabel`: required, non-blank, max 200 characters.
- `sourceUrl`: optional.
- `tags`: optional list normalized by the server.
- `effectiveFrom`: optional ISO timestamp.
- `effectiveTo`: optional ISO timestamp.
- `actorUserId`: required active same-tenant operational user id.

Response:

- HTTP 201 with the full knowledge document response.
- `Location` points to `/api/v1/tenants/{tenantId}/knowledge-documents/{documentId}`.
- New documents default to `ACTIVE`.
- `createdByUserId` and `updatedByUserId` are set to `actorUserId`.
- `createdAt`, `updatedAt`, and `contentHash` are computed by the server.

Validation:

- The tenant must be `ACTIVE`.
- `actorUserId` must identify an `ACTIVE` operational user in the same tenant.
- Cross-tenant actor ids return HTTP 404.
- Inactive tenants return HTTP 409.

### GET /api/v1/tenants/{tenantId}/knowledge-documents

Lists knowledge documents for one tenant.

Query filters:

- `type`: optional exact `FAQ` or `POLICY` filter.
- `status`: optional exact `ACTIVE` or `ARCHIVED` filter.
- `tag`: optional exact normalized tag filter.
- `activeAt`: optional ISO timestamp. A document matches when `effectiveFrom` is absent or at/before `activeAt`, and `effectiveTo` is absent or at/after `activeAt`.

Response:

- HTTP 200 with an array of document responses.
- By default, archived documents are excluded.
- Supplying `status=ARCHIVED` includes archived documents.

Tenant behavior:

- Reads call tenant lookup, not active-tenant mutation guards.
- Inactive tenants remain readable for list operations.
- Documents from other tenants are never included.

### GET /api/v1/tenants/{tenantId}/knowledge-documents/{documentId}

Returns one tenant-scoped knowledge document.

Response:

- HTTP 200 with the document when `tenantId` and `documentId` match.
- HTTP 404 when the document does not exist under that tenant.

Tenant behavior:

- Inactive tenants remain readable for detail operations.
- Cross-tenant document ids return HTTP 404.

### PATCH /api/v1/tenants/{tenantId}/knowledge-documents/{documentId}

Updates mutable document fields with overwrite semantics. Phase 3 does not create immutable revisions.

Request fields:

- `actorUserId`: required active same-tenant operational user id.
- `type`: optional replacement `FAQ` or `POLICY`.
- `title`: optional replacement, non-blank when supplied, max 200 characters.
- `content`: optional replacement, non-blank when supplied, max 50000 characters.
- `sourceLabel`: optional replacement, non-blank when supplied, max 200 characters.
- `sourceUrl`: optional replacement.
- `tags`: optional replacement list normalized by the server.
- `effectiveFrom`: optional replacement ISO timestamp.
- `effectiveTo`: optional replacement ISO timestamp.

Response:

- HTTP 200 with the updated document.
- Empty or no-op updates return the unchanged document.
- Metadata-only updates keep `contentHash` unchanged.
- Content updates recompute `contentHash`.

Validation:

- The tenant must be `ACTIVE`; inactive tenants return HTTP 409.
- `actorUserId` must identify an `ACTIVE` operational user in the same tenant.
- Cross-tenant document ids and cross-tenant actor ids return HTTP 404.
- Archived metadata updates are allowed, including changing `type` from `FAQ` to `POLICY`.
- Archived content updates return HTTP 400.

### PATCH /api/v1/tenants/{tenantId}/knowledge-documents/{documentId}/archive

Archives a tenant-scoped knowledge document without deleting it.

Request fields:

- `actorUserId`: required active same-tenant operational user id.

Response:

- HTTP 200 with the archived document.
- `status` becomes `ARCHIVED`.
- `archivedAt`, `archivedByUserId`, `updatedAt`, and `updatedByUserId` reflect the archive operation.

Validation:

- The tenant must be `ACTIVE`; inactive tenants return HTTP 409.
- Cross-tenant document ids and cross-tenant actor ids return HTTP 404.
- Archive and restore are idempotent. Archiving an already archived document returns HTTP 200 and leaves current archive metadata unchanged.

### PATCH /api/v1/tenants/{tenantId}/knowledge-documents/{documentId}/restore

Restores an archived tenant-scoped knowledge document.

Request fields:

- `actorUserId`: required active same-tenant operational user id.

Response:

- HTTP 200 with the active document.
- `status` becomes `ACTIVE`.
- Restore clears archivedAt and archivedByUserId.
- `updatedAt` and `updatedByUserId` reflect the restore operation when the document was archived.

Validation:

- The tenant must be `ACTIVE`; inactive tenants return HTTP 409.
- Cross-tenant document ids and cross-tenant actor ids return HTTP 404.
- Archive and restore are idempotent. Restoring an already active document returns HTTP 200 and leaves the document active.

## Inactive Tenant Behavior

Inactive tenants remain inspectable:

- Knowledge document list returns HTTP 200.
- Knowledge document detail returns HTTP 200 for same-tenant documents.

Inactive tenants reject knowledge document mutations with HTTP 409:

- Create.
- Update.
- Archive.
- Restore.

## Tenant Isolation Rules

Every knowledge document belongs to exactly one tenant. Every read or write lookup uses both `tenantId` and `documentId`.

Cross-tenant behavior:

- Tenant B cannot read tenant A documents.
- Tenant B cannot update tenant A documents.
- Tenant B cannot archive tenant A documents.
- Tenant B cannot restore tenant A documents.
- Tenant A cannot create, update, archive, or restore using tenant B's `actorUserId`.
- Cross-tenant denial returns HTTP 404 and does not mutate stored document state.

## Phase 3 Exclusions

Phase 3 intentionally adds no AI classification, no RAG, no embeddings, no vector search, no Mongo text search, no keyword contains search, no frontend UI, no authentication, no full RBAC, no destructive delete, and no immutable revisions.

There are no text-search endpoints in Phase 3. Exact metadata filters are the only list-query behavior: `type`, `status`, `tag`, and `activeAt`.
