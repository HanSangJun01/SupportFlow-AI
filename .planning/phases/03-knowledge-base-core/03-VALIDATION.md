---
phase: 3
slug: knowledge-base-core
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-06-04
---

# Phase 3 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter, Spring Boot Test, MockMvc, Testcontainers MongoDB |
| **Config file** | `backend-spring/pom.xml` |
| **Quick run command** | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentServiceTest,KnowledgeDocumentApiIntegrationTest` |
| **Full suite command** | `cd backend-spring && ./mvnw verify` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run the task-specific command from the map below.
- **After every plan wave:** Run `cd backend-spring && ./mvnw verify`.
- **Before `$gsd-verify-work`:** Full suite must be green.
- **Max feedback latency:** 120 seconds for targeted tests; full verification before phase sign-off.

No watch-mode commands should be used in plans.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 03-01 | 1 | KNOW-01, KNOW-02, KNOW-04 | T-03-01-01 | Document model stores tenant id, metadata, audit actors, timestamps, and backend content hash | unit | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentServiceTest` | W0 | pending |
| 03-01-02 | 03-01 | 1 | KNOW-01, KNOW-02, KNOW-03 | T-03-01-02 | Tenant-scoped REST routes use active-tenant mutation guards and tenant-id lookups | unit + WebMvc | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentServiceTest,KnowledgeDocumentApiIntegrationTest` | W0 | pending |
| 03-01-03 | 03-01 | 1 | KNOW-02, KNOW-04 | T-03-01-03 | Archive/restore preserve traceability and reject archived content edits | unit + WebMvc | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentServiceTest,KnowledgeDocumentApiIntegrationTest` | W0 | pending |
| 03-02-01 | 03-02 | 2 | KNOW-03 | T-03-02-01 | Cross-tenant document ids and actor ids do not leak or mutate data | integration | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentMongoIntegrationTest` | W0 | pending |
| 03-02-02 | 03-02 | 2 | KNOW-01, KNOW-02, KNOW-04 | T-03-02-02 | Inactive tenant read/mutation split, filters, archive defaults, and restore behavior are proven over Mongo | integration | `cd backend-spring && ./mvnw test -Dtest=KnowledgeDocumentMongoIntegrationTest` | W0 | pending |
| 03-02-03 | 03-02 | 2 | QUAL-01 evidence | T-03-02-03 | OpenAPI and SDD document the API contract and exclusions | docs + full suite | `cd backend-spring && ./mvnw verify` | W0 | pending |

---

## Threat References

| Threat ID | STRIDE | Component | Mitigation |
|-----------|--------|-----------|------------|
| T-03-01-01 | Tampering | Knowledge document metadata and content hash | Backend computes content hash from stored normalized content and ignores any client hash. |
| T-03-01-02 | Information Disclosure | Tenant-scoped knowledge routes | Every lookup includes tenant id; cross-tenant access returns 404. |
| T-03-01-03 | Tampering | Archived document mutation | Archived records allow metadata/status updates but reject content changes. |
| T-03-02-01 | Information Disclosure | Cross-tenant document and actor ids | Mongo HTTP tests prove tenant A cannot access tenant B documents or use tenant B actors. |
| T-03-02-02 | Tampering | Inactive tenant mutation behavior | `requireActiveTenant` blocks create/update/archive/restore while reads still work. |
| T-03-02-03 | Scope Creep | Retrieval/search API contract | API docs explicitly exclude search, text indexes, embeddings, and RAG. |

---

## Wave 0 Requirements

- [ ] `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentServiceTest.java` - service/model tests for document shape, actor validation, hash generation, archive behavior, and filters.
- [ ] `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentApiIntegrationTest.java` - WebMvc tests for routes, request validation, response shape, and error mapping.
- [ ] `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentMongoIntegrationTest.java` - Docker-backed tests for tenant isolation, inactive-tenant behavior, archive/default-list behavior, restore, and cross-tenant actor denial.
- [ ] `backend-spring/src/test/java/com/supportflow/common/OpenApiDocumentationTest.java` - assertions for Phase 3 knowledge document routes.
- [ ] `docs/sdd/phase-03-knowledge-base-core-api.md` - API contract and explicit Phase 3 exclusions.

---

## Manual-Only Verifications

All Phase 3 behaviors have automated verification.

---

## Validation Sign-Off

- [ ] All tasks have automated verification commands.
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify.
- [ ] Wave 0 covers all missing test references.
- [ ] No watch-mode flags.
- [ ] Feedback latency under 120 seconds for targeted tests.
- [x] `nyquist_compliant: true` set in frontmatter.

**Approval:** pending
