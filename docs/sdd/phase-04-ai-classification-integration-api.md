# Phase 04 AI Classification Integration API

This document captures the Phase 4 API contract for deterministic ticket classification. Phase 4 adds a separate FastAPI AI service and connects the Spring ticket workflow to it for tenant-scoped operational classification.

Phase 4 classifies tickets, stores append-only classification attempts, automatically applies successful category and priority predictions, and links AI-applied workflow history to the classification attempt that caused it.

## FastAPI AI Service

### GET /health

Returns HTTP 200 with:

- `status`: `ok`.

The health endpoint is used by Docker-backed tests and local service checks.

### POST /api/v1/classifications/tickets

Classifies one ticket payload.

Request fields:

- `tenantId`: required tenant id.
- `ticketId`: required ticket id.
- `subject`: required ticket subject, 1 to 200 characters.
- `customerMessage`: required customer message, 1 to 5000 characters.

Response fields:

- `category`: predicted category string, such as `billing`, `technical`, `account`, `cancellation`, or `general`.
- `urgency`: `LOW`, `NORMAL`, `HIGH`, or `CRITICAL`.
- `sentiment`: `NEGATIVE`, `NEUTRAL`, or `POSITIVE`.
- `priority`: `LOW`, `MEDIUM`, `HIGH`, or `URGENT`.
- `confidence`: numeric value from `0.0` to `1.0`.
- `classifierVersion`: classifier version, currently `rules-v1`.

The service uses deterministic local keyword rules. It is stateless and does not store tenants, tickets, users, or classification attempts.

## Spring Ticket Classification Behavior

### Ticket Create Auto-Classification

`POST /api/v1/tenants/{tenantId}/tickets` creates a ticket and synchronously requests classification from the FastAPI service.

Successful classification:

- Stores a classification attempt with status `SUCCESS`.
- Uses trigger `AUTO_ON_CREATE`.
- Does not invent a human actor; `actorUserId` is absent for automatic classification.
- Applies the attempt `category` to the ticket.
- Applies the attempt `priority` to the ticket.
- Appends an `AI_CLASSIFICATION_APPLIED` workflow history entry.
- Sets the history entry `classificationAttemptId` to the successful attempt id.
- Returns the ticket response with embedded `classificationAttempts`.

Failed classification:

- Stores a classification attempt with status `FAILED`.
- Stores `errorCode` and `errorMessage`.
- Returns the ticket response with embedded `classificationAttempts`.
- failed attempts do not change ticket category or priority.
- Failed attempts do not create `AI_CLASSIFICATION_APPLIED` workflow history.

### Ticket Detail Artifact Embedding

`GET /api/v1/tenants/{tenantId}/tickets/{ticketId}` returns the ticket only when it belongs to the requested tenant.

Ticket responses include:

- existing ticket fields.
- `history`: workflow history entries.
- `classificationAttempts`: append-only classification artifacts in insertion order.

### Manual Re-Analysis

`POST /api/v1/tenants/{tenantId}/tickets/{ticketId}/classification-attempts` manually re-analyzes one non-closed ticket.

Request fields:

- `actorUserId`: required active same-tenant operational user id.

Successful manual re-analysis:

- Stores a new classification attempt with status `SUCCESS`.
- Uses trigger `MANUAL_REANALYSIS`.
- Stores `actorUserId` on the attempt.
- Applies the successful `category` and `priority`.
- Appends `AI_CLASSIFICATION_APPLIED` workflow history linked by `classificationAttemptId`.
- Keeps prior attempts unchanged.

Failed manual re-analysis:

- Stores a new classification attempt with status `FAILED`.
- Stores `actorUserId`, `errorCode`, and `errorMessage`.
- Returns HTTP 200 with the ticket response and failed attempt artifact.
- failed attempts do not change ticket category or priority.
- Failed attempts do not create `AI_CLASSIFICATION_APPLIED` workflow history.

Closed tickets reject manual re-analysis with HTTP 400 and message `Closed tickets cannot be classified`.

## Classification Attempt Fields

Each attempt exposes:

- `id`: attempt id.
- `status`: `SUCCESS` or `FAILED`.
- `trigger`: `AUTO_ON_CREATE` or `MANUAL_REANALYSIS`.
- `actorUserId`: present only for manual re-analysis.
- `requestedAt`: timestamp before the AI call.
- `completedAt`: timestamp after success or failure.
- `category`: present on success.
- `urgency`: present on success.
- `sentiment`: present on success.
- `priority`: present on success.
- `confidence`: present on success.
- `classifierVersion`: present on success.
- `errorCode`: present on failure.
- `errorMessage`: present on failure.

## Tenant And Failure Semantics

Inactive tenant behavior:

- Ticket reads remain inspectable through existing read routes.
- Ticket create and manual re-analysis require an active tenant.

Cross-tenant behavior:

- Cross-tenant ticket detail returns HTTP 404.
- Cross-tenant actor ids return HTTP 404.
- Cross-tenant access cannot reveal classification attempts.

Timeout and unavailable AI behavior:

- The backend client uses `supportflow.ai.classification.base-url`.
- The default timeout is approximately 2 seconds.
- Unavailable AI service calls produce failed attempts with stable error metadata.
- Ticket creation still succeeds when automatic classification fails.
- Manual AI failure returns HTTP 200 with a failed attempt artifact.

## Phase 4 Exclusions

Phase 4 has no provider-backed LLM, no prompts, no RAG, no embeddings, no vector search, no draft generation, no async jobs, no frontend UI, no authentication, and no full RBAC.

Phase 4 also does not add customer-facing final responses, evidence retrieval, vector databases, Redis-backed classification orchestration, retries, queues, or ticket auto-closure.
