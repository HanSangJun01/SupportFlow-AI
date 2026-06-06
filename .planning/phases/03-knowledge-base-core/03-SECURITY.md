---
phase: 03
slug: knowledge-base-core
status: verified
threats_open: 0
asvs_level: 1
created: 2026-06-06
updated: 2026-06-06
register_authored_at_plan_time: true
---

# Phase 03 - Security

Per-phase security contract: threat register, accepted risks, and audit trail for tenant-scoped knowledge document APIs.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Tenant path identity to knowledge service | `/api/v1/tenants/{tenantId}/knowledge-documents` routes scope all document reads and mutations to the path tenant. | Tenant id, document id, actor id, knowledge metadata, document content |
| Knowledge service to Mongo repository | Service methods constrain route lookups with tenant-aware repository methods before returning or mutating documents. | Persisted knowledge document state and metadata |
| Mutation request actor to tenant-local user validation | Create, update, archive, and restore accept explicit `actorUserId` until authentication is added later. | Actor identity, tenant-local operational user status |
| Knowledge API contract to later retrieval phases | Phase 3 exposes retrieval-ready metadata while intentionally excluding search, RAG, embeddings, auth, delete, and revisions. | Source labels, tags, effective windows, content hashes, audit metadata |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-03-01-01 | Tampering | Knowledge document metadata and content hash | mitigate | `KnowledgeDocumentService` computes `contentHash` from LF-normalized stored content and exposes no request field for client-supplied hashes. Service tests verify generated hash, stable metadata-only updates, and recompute-on-content-change behavior. Evidence: `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:57`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:63`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:121`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:124`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:261`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentServiceTest.java:41`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentServiceTest.java:321`. | closed |
| T-03-01-02 | Information Disclosure | Tenant-scoped knowledge routes | mitigate | Routes are nested under tenant path identity, list uses `findByTenantId`, detail/update/archive/restore use `findByTenantIdAndId`, and no tenant route uses unscoped `findById`. Mongo integration tests verify cross-tenant detail, update, archive, restore, and list isolation. Evidence: `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentController.java:22`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentRepository.java:9`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentRepository.java:11`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:74`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:199`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentMongoIntegrationTest.java:43`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentMongoIntegrationTest.java:116`. | closed |
| T-03-01-03 | Tampering | Archived document mutation | mitigate | Archived content updates fail with HTTP 400, while metadata updates including type/title/source/tags/effective windows remain allowed. Service and Mongo integration tests cover both paths. Evidence: `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:97`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:106`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:128`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:140`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentServiceTest.java:259`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentServiceTest.java:289`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentMongoIntegrationTest.java:213`. | closed |
| T-03-02-01 | Information Disclosure | Cross-tenant document and actor IDs | mitigate | Docker-backed HTTP tests prove tenant B cannot read, update, archive, or restore tenant A documents, and tenant A cannot mutate using tenant B actors. Evidence: `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentMongoIntegrationTest.java:43`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentMongoIntegrationTest.java:68`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentMongoIntegrationTest.java:74`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentMongoIntegrationTest.java:102`, `docs/sdd/phase-03-knowledge-base-core-api.md:188`. | closed |
| T-03-02-02 | Tampering | Inactive tenant mutation behavior | mitigate | Reads call tenant lookup and remain available for inactive tenants; create, update, archive, and restore call `requireActiveTenant` and return HTTP 409 for inactive tenants. Mongo integration tests cover the read/mutation split. Evidence: `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:40`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:72`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:88`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:93`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:166`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java:184`, `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentMongoIntegrationTest.java:142`, `docs/sdd/phase-03-knowledge-base-core-api.md:174`. | closed |
| T-03-02-03 | Scope Creep | Retrieval/search API contract | mitigate | OpenAPI/SDD contract documents only exact metadata filters and explicitly excludes search, text indexes, embeddings, RAG, AI, frontend, auth, RBAC, delete, and revisions. Implementation exposes create/list/detail/update/archive/restore only. Evidence: `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentController.java:33`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentController.java:54`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentController.java:70`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentController.java:76`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentController.java:94`, `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentController.java:102`, `docs/sdd/phase-03-knowledge-base-core-api.md:5`, `docs/sdd/phase-03-knowledge-base-core-api.md:69`, `docs/sdd/phase-03-knowledge-base-core-api.md:201`. | closed |

Status: all plan-time threats are closed. No accepted risks were required.

---

## Accepted Risks Log

No accepted risks.

---

## Verification Evidence

| Date | Command | Result |
|------|---------|--------|
| 2026-06-04 | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentMongoIntegrationTest` | PASS: 5 tests, 0 failures, 0 errors, 0 skipped |
| 2026-06-04 | `cd backend-spring && ./mvnw verify` | PASS: 90 tests, 0 failures, 0 errors, 0 skipped |
| 2026-06-06 | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentServiceTest,KnowledgeDocumentMongoIntegrationTest` | PASS: 22 tests, 0 failures, 0 errors, 0 skipped |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-06-06 | 6 | 6 | 0 | Codex |

---

## Sign-Off

- [x] All threats have a disposition: mitigate.
- [x] Accepted risks documented in Accepted Risks Log.
- [x] `threats_open: 0` confirmed.
- [x] `status: verified` set in frontmatter.

Approval: verified 2026-06-06
