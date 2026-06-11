# Phase 4: AI Classification Integration - Research

**Researched:** 2026-06-11
**Status:** Ready for planning

## Research Summary

Phase 4 should be implemented as a deterministic structured-classification integration, not as an LLM or agent framework. The phase already has a locked AI-SPEC choosing FastAPI + Pydantic v2 for the AI service and a rule-based classifier with no provider credentials, no prompts, no RAG, no async queue, and no Redis dependency. The Spring backend remains the source of persistence and traceability.

## Scope Inputs

- `.planning/phases/04-ai-classification-integration/04-CONTEXT.md` locks automatic classification on ticket creation, manual re-analysis, append-only attempts, successful category/priority auto-apply, workflow history links, failed-attempt non-mutation, deterministic keyword rules, classifier versioning, and a short backend timeout.
- `.planning/phases/04-ai-classification-integration/04-AI-SPEC.md` locks FastAPI + Pydantic v2 direct service, deterministic in-process rules, code-first evals, pytest, Spring integration tests, and optional Phoenix tracing only.
- `.planning/REQUIREMENTS.md` maps Phase 4 to `AI-01`, `AI-02`, `AI-03`, `AI-04`, and `QUAL-04`.
- `.planning/ROADMAP.md` expects three Phase 4 plans: FastAPI service scaffold, backend classification workflow/artifact storage, and automated contract/integration tests.

## Official Documentation Findings

### FastAPI

FastAPI request bodies are declared with Pydantic models, and response models validate, document, serialize, and filter returned data. This fits the Phase 4 contract because `ClassificationRequest` and `ClassificationResponse` should be the source of truth for API shape. FastAPI `TestClient` can test the app directly without opening a real socket, which is appropriate for contract tests before Docker-backed integration.

Relevant docs:
- https://fastapi.tiangolo.com/tutorial/body/
- https://fastapi.tiangolo.com/tutorial/response-model/
- https://fastapi.tiangolo.com/reference/testclient/
- https://fastapi.tiangolo.com/deployment/versions/

### Pydantic v2

Pydantic v2 models define typed fields, validate incoming data into those types, and expose serialization/schema methods. Use `BaseModel`, `ConfigDict(extra="forbid")`, `Field(ge=0.0, le=1.0)`, and `StrEnum` to make classification inputs and outputs strict enough for contract tests.

Relevant docs:
- https://docs.pydantic.dev/latest/concepts/models/
- https://docs.pydantic.dev/latest/concepts/fields/

### pytest

Use parametrized tests for keyword examples and failure cases. The reference dataset should start small, with labeled examples for billing, technical, account access, cancellation, positive, neutral, fallback, and urgency/priority cases.

Relevant docs:
- https://docs.pytest.org/en/stable/how-to/parametrize.html

### Arize Phoenix

Phoenix can receive OpenTelemetry traces and the `arize-phoenix-otel` package offers tracing helpers. For Phase 4, tracing is optional because the local deterministic service and stored classification attempts are the required audit surface. If implemented, it must not require external credentials for local tests.

Relevant docs:
- https://arize.com/docs/phoenix/tracing/how-to-tracing/setup-tracing/instrument
- https://arize.com/docs/phoenix/tracing/how-to-tracing/setup-tracing/setup-using-phoenix-otel

## Package Versions Checked

Package availability was checked on 2026-06-11:

| Package | Version to use in plan |
|---------|------------------------|
| `fastapi` | `0.136.3` |
| `pydantic` | `2.13.4` |
| `pytest` | `9.0.3` |
| `arize-phoenix-otel` | `0.16.1` optional only |

## Existing Code Patterns

### Ticket Domain

- `Ticket` currently contains `tenantId`, inquiry fields, `category`, `priority`, `assigneeId`, timestamps, and embedded `history`.
- `TicketService.createTicket` is the insertion point for automatic classification after active tenant and assignee validation.
- `TicketService.updateWorkflowMetadata` shows the existing pattern for category/priority mutation and history field changes.
- `TicketHistoryEntry` currently stores `eventType`, `actorUserId`, `occurredAt`, and `changes`. Phase 4 needs an optional `classificationAttemptId` on AI-applied history entries.
- `TicketHistoryEventType` needs a new `AI_CLASSIFICATION_APPLIED` value.
- `TicketController.TicketResponse` is the response shape to extend with classification attempts.

### Backend Test Patterns

- Unit tests cover service rules directly.
- Web/API tests cover controller request/response validation.
- Mongo/Testcontainers tests use `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `TestRestTemplate`, `MongoDBContainer("mongo:7")`, and `@Testcontainers(disabledWithoutDocker = true)`.
- OpenAPI assertions live in `OpenApiDocumentationTest`.

### Compose Pattern

`docker-compose.yml` currently defines `mongodb`, `backend`, and `redis`. Phase 4 should add `ai-service` and configure the backend with a local AI service base URL. Redis remains unused for classification.

## Recommended Technical Design

### FastAPI Service

Create `ai-service-python` as a real Python service:

- `pyproject.toml` with pinned runtime and test dependencies.
- `Dockerfile` running uvicorn on port 8000.
- `app/models.py` with strict Pydantic request/response models and enums.
- `app/classifier.py` with pure deterministic keyword rules and `CLASSIFIER_VERSION = "rules-v1"`.
- `app/main.py` exposing `GET /health` and `POST /api/v1/classifications/tickets`.
- `tests/test_classifier_rules.py` and `tests/test_classification_api.py`.

The service should reject extra request fields, blank text, oversized text, invalid confidence, and missing required fields through Pydantic/FastAPI validation.

### Backend Integration

Add a backend classification package, likely `com.supportflow.ai`, and extend the ticket model:

- `AiClassificationClient` interface for backend service logic.
- `HttpAiClassificationClient` using Spring `RestClient` with configurable base URL and timeout.
- DTOs for request/response/error mapping.
- `AiClassificationProperties` for `supportflow.ai.classification.base-url` and `supportflow.ai.classification.timeout`.
- `TicketClassificationAttempt` embedded object or separate same-tenant collection. Embedded on `Ticket` is the simplest fit for Phase 4 because ticket detail must embed artifacts.
- Enums for attempt status and trigger: `SUCCESS` / `FAILED`, `AUTO_ON_CREATE` / `MANUAL_REANALYSIS`.
- Existing `TicketPriority` should be reused for priority.
- Add new urgency/sentiment enums for attempt result fields.
- Add optional `classificationAttemptId` to `TicketHistoryEntry`.
- Add `AI_CLASSIFICATION_APPLIED` to `TicketHistoryEventType`.

Backend behavior:

- Automatic classification runs synchronously during ticket creation.
- If automatic classification succeeds, store attempt, update ticket `category` and `priority`, and append AI history linked to the attempt.
- If automatic classification fails, store failed attempt and leave ticket fields/history unchanged except for the attempt artifact.
- Manual re-analysis endpoint requires active tenant, non-closed ticket, and active same-tenant `actorUserId`.
- Manual success creates a new attempt, reapplies category/priority, and appends linked AI history.
- Manual failure returns structured failure, stores failed attempt, and leaves ticket category/priority unchanged.

### Docker-Backed Integration

Use two levels of confidence:

1. Fast Python tests for the AI service contract and deterministic rules.
2. Spring tests with a fake client for service behavior and failure semantics.
3. Docker-backed Spring integration with Testcontainers `ImageFromDockerfile` or equivalent to run the actual FastAPI service and prove backend-to-AI HTTP contract.

`docker-compose.yml` should also expose local runtime wiring, but compose itself does not replace automated tests.

## Out Of Scope

- Real LLM/provider classification.
- Prompt templates, embeddings, RAG, vector search, evidence retrieval, and draft generation.
- Async queueing, Redis job state, retry workers, or background classification.
- Frontend UI, authentication, full RBAC, or customer-facing response generation.
- Automatic final response or ticket closure.

## Validation Architecture

Phase 4 has three validation layers:

1. **AI service contract tests** with pytest:
   - `python -m pytest` in `ai-service-python`.
   - Verify strict request/response schema, enum values, confidence bounds, classifier version, deterministic repeated results, keyword mapping, fallback confidence, health endpoint, and no external provider dependency.

2. **Backend unit/API tests** with Maven:
   - `./mvnw test -Dtest=TicketClassificationServiceTest,TicketClassificationControllerTest`.
   - Verify auto/manual triggers, successful auto-apply, failed non-mutation, actor validation, closed-ticket rejection, linked history, attempt ordering, and response shape.

3. **Docker-backed cross-service tests**:
   - `./mvnw test -Dtest=TicketClassificationMongoIntegrationTest`.
   - Use MongoDB Testcontainers plus an actual FastAPI container built from `ai-service-python`.
   - Verify ticket creation through Spring calls the FastAPI service and persists/applies the returned classification.

Full phase verification:

```bash
cd ai-service-python && python -m pytest
cd ../backend-spring && ./mvnw verify
```

## Planning Recommendation

Keep the roadmap's three-plan structure:

1. `04-01`: FastAPI AI service with contract-tested deterministic classifier.
2. `04-02`: Spring backend classification client, attempts, auto/manual workflow, and traceability.
3. `04-03`: Docker Compose wiring, Docker-backed integration tests, OpenAPI/SDD docs, and full verification.

This keeps the first slice independently callable and gives each subsequent plan a complete vertical behavior to verify.
