---
phase: 04
slug: ai-classification-integration
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-06-11
---

# Phase 04 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | pytest 9.0.3 for `ai-service-python`; JUnit Jupiter, Spring Boot Test, and Testcontainers for `backend-spring` |
| **Config file** | `ai-service-python/pyproject.toml`; `backend-spring/pom.xml` |
| **Quick run command** | `cd ai-service-python && python -m pytest` |
| **Full suite command** | `cd ai-service-python && python -m pytest && cd ../backend-spring && ./mvnw verify` |
| **Estimated runtime** | ~180 seconds with Docker available |

---

## Sampling Rate

- **After every task commit:** Run the task-level command listed in the plan.
- **After every plan wave:** Run `cd ai-service-python && python -m pytest` for AI-service changes and the relevant `./mvnw test -Dtest=...` command for backend changes.
- **Before `$gsd-verify-work`:** `cd ai-service-python && python -m pytest && cd ../backend-spring && ./mvnw verify` must be green.
- **Max feedback latency:** 180 seconds.

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 04-01 | 1 | AI-02, QUAL-04 | T-04-01-01 | AI service rejects invalid request/response shape and uses no provider secrets. | pytest | `cd ai-service-python && python -m pytest` | Yes | pending |
| 04-01-02 | 04-01 | 1 | AI-02, QUAL-04 | T-04-01-02 | Deterministic keyword rules produce stable enum outputs and confidence bounds. | pytest | `cd ai-service-python && python -m pytest` | Yes | pending |
| 04-01-03 | 04-01 | 1 | AI-01, AI-02, QUAL-04 | T-04-01-03 | FastAPI endpoint exposes a stable contract and Docker image starts locally. | pytest / docker | `cd ai-service-python && python -m pytest` | Yes | pending |
| 04-02-01 | 04-02 | 2 | AI-01, AI-03, AI-04 | T-04-02-01 | Classification attempts remain tenant-owned and append-only on tickets. | JUnit | `cd backend-spring && ./mvnw test -Dtest=TicketClassificationServiceTest` | Yes | pending |
| 04-02-02 | 04-02 | 2 | AI-01, AI-02, AI-03 | T-04-02-02 | Successful classifications apply category/priority and link history to attempt ID. | JUnit | `cd backend-spring && ./mvnw test -Dtest=TicketClassificationServiceTest` | Yes | pending |
| 04-02-03 | 04-02 | 2 | AI-03, AI-04 | T-04-02-03 | Failed classifications do not mutate ticket category/priority or workflow history. | JUnit / WebMvc | `cd backend-spring && ./mvnw test -Dtest=TicketClassificationServiceTest,TicketClassificationControllerTest` | Yes | pending |
| 04-03-01 | 04-03 | 3 | AI-01, AI-02, QUAL-04 | T-04-03-01 | Spring backend can call the real FastAPI service in Docker-backed tests. | Testcontainers | `cd backend-spring && ./mvnw test -Dtest=TicketClassificationMongoIntegrationTest` | Yes | pending |
| 04-03-02 | 04-03 | 3 | AI-03, AI-04 | T-04-03-02 | API docs and SDD expose inspectable classification artifacts and failure semantics. | JUnit / docs grep | `cd backend-spring && ./mvnw test -Dtest=OpenApiDocumentationTest,FoundationVerificationTest` | Yes | pending |
| 04-03-03 | 04-03 | 3 | AI-01, AI-02, AI-03, AI-04, QUAL-04 | T-04-03-03 | Full verification proves Python and Spring sides together. | full suite | `cd ai-service-python && python -m pytest && cd ../backend-spring && ./mvnw verify` | Yes | pending |

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. Phase 4 will add Python pytest configuration as part of `04-01-01`; Spring/JUnit/Testcontainers infrastructure already exists.

---

## Manual-Only Verifications

All Phase 4 behaviors have automated verification. Manual UAT can inspect create/detail/re-analysis JSON responses after execution, but it is not the only proof path.

---

## Validation Sign-Off

- [x] All tasks have automated verify commands or explicit Wave 0 coverage.
- [x] Sampling continuity: no 3 consecutive tasks without automated verify.
- [x] Wave 0 covers all missing references.
- [x] No watch-mode flags.
- [x] Feedback latency < 180s.
- [x] `nyquist_compliant: true` set in frontmatter.

**Approval:** approved 2026-06-11
