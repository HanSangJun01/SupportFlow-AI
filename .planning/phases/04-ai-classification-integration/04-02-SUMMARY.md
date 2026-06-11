---
phase: 04-ai-classification-integration
plan: 04-02
subsystem: api
tags: [spring-boot, restclient, ticket-workflow, classification-attempts, tenant-isolation]

requires:
  - phase: 04-ai-classification-integration
    provides: FastAPI ticket classification contract and deterministic response fields
  - phase: 02-tenant-workflow-core
    provides: active tenant guard, active actor validation, ticket workflow history pattern
provides:
  - Spring AI classification client contract and configurable RestClient implementation
  - embedded append-only ticket classification attempts
  - automatic ticket-create classification with successful category/priority auto-apply
  - manual actor-attributed re-analysis endpoint
  - linked AI_CLASSIFICATION_APPLIED workflow history entries
affects: [ai-classification-integration, docker-compose, openapi, ticket-workflow, tenant-isolation]

tech-stack:
  added: []
  patterns: [Spring RestClient boundary, embedded ticket artifacts, linked workflow history, non-mutating failure attempts]

key-files:
  created:
    - backend-spring/src/main/java/com/supportflow/ai/AiClassificationClient.java
    - backend-spring/src/main/java/com/supportflow/ai/AiClassificationProperties.java
    - backend-spring/src/main/java/com/supportflow/ai/HttpAiClassificationClient.java
    - backend-spring/src/main/java/com/supportflow/ai/TicketClassificationRequest.java
    - backend-spring/src/main/java/com/supportflow/ai/TicketClassificationResponse.java
    - backend-spring/src/main/java/com/supportflow/ai/TicketClassificationException.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketClassificationAttempt.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketClassificationAttemptStatus.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketClassificationTrigger.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketClassificationUrgency.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketClassificationSentiment.java
    - backend-spring/src/test/java/com/supportflow/ticket/TicketClassificationServiceTest.java
    - backend-spring/src/test/java/com/supportflow/ticket/TicketClassificationControllerTest.java
  modified:
    - backend-spring/src/main/java/com/supportflow/ticket/Ticket.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketHistoryEntry.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketHistoryEventType.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketPriority.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketService.java
    - backend-spring/src/main/java/com/supportflow/ticket/TicketController.java
    - backend-spring/src/test/java/com/supportflow/ticket/TicketServiceTest.java

key-decisions:
  - "Ticket creation saves once to obtain a ticket id, then stores the classification attempt and any successful AI-applied field changes in a second save."
  - "Failed classification attempts are visible artifacts but never mutate category, priority, or workflow history."
  - "Manual re-analysis requires an active same-tenant actor and rejects closed tickets before calling the AI client."

patterns-established:
  - "Classification attempts are embedded on Ticket and exposed in TicketResponse in insertion order."
  - "Successful AI changes use AI_CLASSIFICATION_APPLIED history with classificationAttemptId instead of human workflow metadata events."
  - "Precondition failures happen before AI client invocation."

requirements-completed: [AI-01, AI-02, AI-03, AI-04]

duration: 14min
completed: 2026-06-11
---

# Phase 04-02: Backend Classification Workflow Summary

**Spring ticket workflow classification with append-only attempts, successful AI category/priority auto-apply, and linked audit history**

## Performance

- **Duration:** 14 min
- **Started:** 2026-06-11T08:21:00Z
- **Completed:** 2026-06-11T08:34:57Z
- **Tasks:** 3
- **Files modified:** 20

## Accomplishments

- Added `com.supportflow.ai` client contract, properties, HTTP RestClient implementation, request/response DTOs, and stable classification exception codes.
- Extended tickets with append-only `classificationAttempts` and extended history entries with nullable `classificationAttemptId`.
- Wired ticket creation to synchronous automatic classification and manual re-analysis under `/classification-attempts`.
- Implemented success semantics: store attempt, apply category/priority, append `AI_CLASSIFICATION_APPLIED` history linked to the attempt id.
- Implemented failure semantics: store failed attempt with error metadata, leave category/priority/history unchanged, and return ticket response.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add backend AI client contract, config, and ticket attempt model** - `e158147` (feat)
2. **Task 2: Integrate automatic ticket-create classification and manual re-analysis workflow** - `32769cf` (feat)
3. **Task 3: Harden backend failure, tenant, and response semantics** - `f6f8d7b` (test)

**Plan metadata:** committed with this summary.

## Files Created/Modified

- `backend-spring/src/main/java/com/supportflow/ai/*` - AI classification client interface, properties, HTTP client, DTOs, and exception type.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketClassificationAttempt.java` - Embedded success/failure attempt artifact.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketClassificationAttemptStatus.java` - Attempt status enum.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketClassificationTrigger.java` - Automatic/manual trigger enum.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketClassificationUrgency.java` - AI urgency enum.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketClassificationSentiment.java` - AI sentiment enum.
- `backend-spring/src/main/java/com/supportflow/ticket/Ticket.java` - Adds null-safe classification attempt list.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketHistoryEntry.java` - Adds optional `classificationAttemptId`.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketHistoryEventType.java` - Adds `AI_CLASSIFICATION_APPLIED`.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketPriority.java` - Adds `MEDIUM` for compatibility with the Phase 4 AI service contract.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketService.java` - Adds automatic/manual classification workflow and failure handling.
- `backend-spring/src/main/java/com/supportflow/ticket/TicketController.java` - Adds manual re-analysis route and response artifacts.
- `backend-spring/src/test/java/com/supportflow/ticket/TicketClassificationServiceTest.java` - Service workflow and failure coverage.
- `backend-spring/src/test/java/com/supportflow/ticket/TicketClassificationControllerTest.java` - WebMvc route, validation, and response coverage.
- `backend-spring/src/test/java/com/supportflow/ticket/TicketServiceTest.java` - Existing ticket creation test adjusted for automatic classification saves.

## Decisions Made

- Classification attempts remain embedded on the ticket because ticket detail must expose artifacts alongside workflow history in Phase 4.
- Manual AI failure returns a successful ticket response containing a failed attempt rather than throwing a 5xx.
- Java `TicketPriority` now includes `MEDIUM` so FastAPI `Priority.MEDIUM` can deserialize and be applied without a translation layer.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added `TicketPriority.MEDIUM` to align Java and Python contracts**
- **Found during:** Task 1 (backend AI client contract)
- **Issue:** The Phase 4 FastAPI service returns `MEDIUM`, but the existing Java ticket priority enum only had `NORMAL`.
- **Fix:** Preserved existing `NORMAL` and added `MEDIUM` so backend HTTP deserialization can accept the AI service response.
- **Files modified:** `backend-spring/src/main/java/com/supportflow/ticket/TicketPriority.java`
- **Verification:** `cd backend-spring && ./mvnw test -Dtest=TicketClassificationServiceTest,TicketClassificationControllerTest` passed.
- **Committed in:** `e158147` (Task 1 commit)

**Total deviations:** 1 auto-fixed (blocking contract compatibility)
**Impact on plan:** No behavior outside priority enum compatibility; existing `NORMAL` usage remains valid.

## Issues Encountered

- Existing ticket creation tests expected one repository save. Automatic classification now intentionally saves a baseline ticket to obtain an id, then saves the attempt and successful field changes; the test was updated to expect two saves.
- Maven test runs emit Mockito dynamic-agent warnings on Java 23. Tests pass and the warning is from the test runtime.

## User Setup Required

None - no external service configuration required.

## Verification

- `cd backend-spring && ./mvnw test -Dtest=TicketClassificationServiceTest` - PASS, 3 tests after Task 1.
- `cd backend-spring && ./mvnw test -Dtest=TicketClassificationServiceTest,TicketClassificationControllerTest` - PASS, 12 tests after Task 2.
- `cd backend-spring && ./mvnw test -Dtest=TicketClassificationServiceTest,TicketClassificationControllerTest` - PASS, 15 tests after Task 3.
- `cd backend-spring && ./mvnw test -Dtest=TicketClassificationServiceTest,TicketClassificationControllerTest,TicketServiceTest` - PASS, 19 tests.

## Self-Check: PASSED

- Created summary file exists: `.planning/phases/04-ai-classification-integration/04-02-SUMMARY.md`.
- Task commits found: `e158147`, `32769cf`, and `f6f8d7b`.
- Ticket responses embed `classificationAttempts`.
- Successful attempts apply category/priority and append linked `AI_CLASSIFICATION_APPLIED` history.
- Failed attempts do not mutate category/priority and do not add workflow history.
- No async queues, Redis orchestration, RAG, embeddings, draft generation, frontend, auth, or provider-backed LLM behavior was added.

## Next Phase Readiness

Plan 04-03 can add Docker Compose wiring, Docker-backed backend-to-FastAPI integration tests, OpenAPI assertions, SDD docs, and full Phase 4 verification.

---
*Phase: 04-ai-classification-integration*
*Completed: 2026-06-11*
